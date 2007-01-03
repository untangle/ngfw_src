/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include "mvutil/mvsem.h"

#include <stdlib.h>
#include <semaphore.h>
#include "mvutil/list.h"
#include "mvutil/errlog.h"
#include "mvutil/debug.h"

static eventmask_t _poll (mvpoll_key_t* key);

int      mvsem_init   (mvsem_t* mvsem, int share, int value)
{
    if (sem_init(&mvsem->sem,share,value)<0)
        return perrlog("sem_init");

    mvsem->key = mvpoll_key_base_create();
    mvsem->key->type = MVSEM_MVPOLL_KEY_TYPE;
    mvsem->key->poll = _poll;
    mvsem->key->special_destroy = NULL;

    return 0;
}

mvsem_t* mvsem_create (int share, int value)
{
    mvsem_t* mvsem = mvsem_malloc();

    if (!mvsem)
        return NULL;

    if (mvsem_init(mvsem,share,value)<0) {
        free(mvsem);
        return NULL;
    }

    return mvsem;
}

mvsem_t* mvsem_malloc (void)
{
    mvsem_t* mvsem = malloc(sizeof(mvsem_t));

    if (!mvsem)
        return errlogmalloc_null();

    return mvsem;
}

int      mvsem_destroy (mvsem_t* mvsem)
{
    if (!mvsem)
        return errlogargs();

    if (sem_destroy(&mvsem->sem)<0)
        perrlog("sem_destroy");
    if (mvpoll_key_raze(mvsem->key)<0)
        perrlog("mvpoll_key_raze");
    
    return 0;
}

int      mvsem_raze (mvsem_t* mvsem)
{
    if (!mvsem)
        return errlogargs();

    if (mvsem_destroy(mvsem)<0)
        perrlog("mvsem_destroy");

    mvsem_free(mvsem);

    return 0;
}

void     mvsem_free (mvsem_t* mvsem)
{
    if (!mvsem) {
        errlogargs();
        return;
    }

    free(mvsem);
}


int      mvsem_post (mvsem_t* mvsem)
{
    int ret;
    
    if (!mvsem)
        return errlogargs();

    if ((ret = sem_post(&mvsem->sem))<0)
        perrlog("sem_post");
    else
        mvpoll_key_notify_observers(mvsem->key, MVPOLLIN | MVPOLLOUT);

    return ret;
}

int      mvsem_wait (mvsem_t* mvsem)
{
    int ret;
    
    if (!mvsem)
        return errlogargs();

    if ((ret = sem_wait(&mvsem->sem))<0)
        perrlog("sem_wait");
    else {
        int value = 0;
        
        if (sem_getvalue(&mvsem->sem,&value)<0)
            perrlog("sem_getvalue");
        else {
            if (value <= 0)
                mvpoll_key_notify_observers(mvsem->key, MVPOLLOUT);
        }
    }

    return ret;
}

int      mvsem_trywait (mvsem_t* mvsem)
{
    int ret;
    
    if (!mvsem)
        return errlogargs();

    if ((ret = sem_trywait(&mvsem->sem))<0)
        perrlog("sem_trywait");
    else {
        int value = 0;
        
        if (sem_getvalue(&mvsem->sem,&value)<0)
            perrlog("sem_getvalue");
        else {
            if (value <= 0)
                mvpoll_key_notify_observers(mvsem->key, MVPOLLOUT);
        }
    }

    return ret;
}

static eventmask_t _poll (mvpoll_key_t* key)
{
    int value;
    eventmask_t mask = 0;
    mvsem_t* mvsem = (mvsem_t*) key;

    if (!key)
        return 0;
    
    if (sem_getvalue(&mvsem->sem,&value)<0)
        return 0;

    /**
     * semaphore is always postable
     */
    mask |= MVPOLLOUT;

    if (value > 0)
        mask |= MVPOLLIN;
    
    debug(10,"MVSEM: Poll 0x%08x (value:%i)\n",mask,value);

    return mask;
}
