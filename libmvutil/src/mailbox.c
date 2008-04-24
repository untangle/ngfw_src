/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#include "mvutil/mailbox.h"

#include <features.h>
#include <unistd.h>
#include <time.h>
#include <sys/time.h>
#include <semaphore.h>
#include <stdlib.h>

#include "mvutil/errlog.h"
#include "mvutil/debug.h"
#include "mvutil/list.h"
#include "mvutil/unet.h"
#include "mvutil/utime.h"
#include "mvutil/mvpoll.h"

#define MB_LOCK(mb)       if (lock_wrlock(&(mb)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to lock mailbox\n")
#define MB_UNLOCK(mb)     if (lock_unlock(&(mb)->lock)<0) \
                                    errlog(ERR_CRITICAL,"Unable to unlock mailbox\n")
#define MB_LOCK_NULL(mb)       if (lock_wrlock(&(mb)->lock)<0) \
                                    return errlog_null(ERR_CRITICAL,"Unable to lock mailbox\n")
#define MB_UNLOCK_NULL(mb)     if (lock_unlock(&(mb)->lock)<0) \
                                    errlog_null(ERR_CRITICAL,"Unable to unlock mailbox\n")


typedef struct mb_mvpoll_key
{
    mvpoll_key_t key;
    
    mailbox_t* mb;
} mb_mvpoll_key_t;

typedef int (*wait_func_t) (sem_t* sem);

static void*   _mailbox_get (mailbox_t* mb, wait_func_t wfunc);
static void*   _mailbox_timed_get ( mailbox_t* mb, struct timespec* ts );

static eventmask_t   _src_poll_wrapper  ( mvpoll_key_t* key );
static int           _src_key_destroy   ( mvpoll_key_t* key ); 

int          mailbox_init (mailbox_t* mb)
{
    if (sem_init(&mb->list_size_sem,0,0)<0)
        return perrlog("sem_init");
    if (list_init(&mb->list,0)<0)
        return perrlog("list_init");
    if (lock_init(&mb->lock,0)<0)
        return perrlog("lock_init");
    mb->pipe[0] = -1;
    mb->pipe[1] = -1;
    mb->mv_key  = NULL;
    mb->size    = 0;
    return 0;
}

int          mailbox_destroy (mailbox_t* mb)
{
    if (sem_destroy(&mb->list_size_sem)<0)
        perrlog("sem_destroy");
    if (list_destroy(&mb->list)<0)
        perrlog("list_destroy");
    if (lock_destroy(&mb->lock)<0)
        perrlog("lock_destroy");
    if (mb->mv_key != NULL ) {
        errlog( ERR_WARNING, "Destroying registered key\n" );
        free( mb->mv_key );
        mb->mv_key = NULL;
    }

    if ((mb->pipe[0] != -1) && (close(mb->pipe[0])<0))
        perrlog("close");
    if ((mb->pipe[1] != -1) && (close(mb->pipe[1])<0))
        perrlog("close");
    
    return 0;
}

void*        mailbox_get (mailbox_t* mb)
{
    if (!mb) 
        return errlog_null(ERR_WARNING,"Invalid arguments\n");
    
    return _mailbox_get(mb,sem_wait);
}

void*        mailbox_timed_get (mailbox_t* mb, int sec)
{
    struct timespec ts;
    struct timeval  tv;

    if (!mb) 
        return errlog_null(ERR_WARNING,"Invalid arguments\n");

    if ( gettimeofday ( &tv, NULL ) < 0 ) return perrlog_null ( "gettimeofday" );

    ts.tv_sec  = tv.tv_sec + sec;
    ts.tv_nsec = USEC_TO_NSEC( tv.tv_usec );
    
    return _mailbox_timed_get ( mb, &ts );
}

void*        mailbox_utimed_get ( mailbox_t* mb, struct timeval* tv )
{
    struct timespec ts;
    
    if ( mb == NULL || tv == NULL )
        return errlogargs_null();

    ts.tv_sec  = tv->tv_sec;
    ts.tv_nsec = USEC_TO_NSEC( tv->tv_usec );
    
    if ( ts.tv_nsec >= N_SEC ) {
        ts.tv_sec  += NSEC_TO_SEC( ts.tv_nsec );
        ts.tv_nsec  = ts.tv_nsec % N_SEC;
    } else if ( ts.tv_nsec < 0 ) {
        /* Just in case */
        ts.tv_nsec = 0;
    }
    
    return _mailbox_timed_get( mb, &ts );
}

void*        mailbox_ntimed_get ( mailbox_t* mb, struct timespec* ts )
{
    if ( mb == NULL || ts == NULL )
        return errlogargs_null();
    
    return _mailbox_timed_get( mb, ts );
}

void*        mailbox_try_get (mailbox_t* mb)
{
    if (!mb) 
        return errlog_null( ERR_WARNING, "Invalid arguments\n" );
    
    return _mailbox_get( mb, sem_trywait );
}

int          mailbox_put (mailbox_t* mb, void* mail)
{
    if (!mb) 
        return errlog(ERR_WARNING,"Invalid arguments\n");

    MB_LOCK(mb);
    
    mb->size++;
    
    if (sem_post(&mb->list_size_sem)<0) {
        MB_UNLOCK(mb);
        return perrlog("sem_post");
    }
    
    if (list_add_tail(&mb->list,mail)<0) {
        MB_UNLOCK(mb);
        return errlog(ERR_WARNING,"list_add_tail failed\n");
    }

    /* post pollable event if in use */
    if (mb->pipe[1] != -1) {
        if (write(mb->pipe[1],".",1)<1) 
            perrlog("write");
    }

    /* Notify any observers if the key is non-null */
    if ( mb->mv_key != NULL ) mvpoll_key_notify_observers( mb->mv_key, MVPOLLIN );

    MB_UNLOCK(mb);

    return 0;
}

int          mailbox_get_pollable_event(mailbox_t* mb)
{
    if (!mb)
        return errlogargs();

    MB_LOCK(mb);
    
    if (mb->pipe[0] != -1) {
        MB_UNLOCK(mb);
        return mb->pipe[0];
    }        
    
    if (pipe(mb->pipe)<0) {
        MB_UNLOCK(mb);
        return perrlog("pipe");
    }

    if (mb->size>0) {
        char buf[mb->size];
        size_t num = mb->size;
        
        memset(buf,0,mb->size); /* needed only to shut up debuggers */
        
        unet_write_loop(mb->pipe[1],buf,&num,5);
        if (num < 0) 
            perrlog("write");
        if (num < mb->size) 
            errlog(ERR_CRITICAL,"write truncated (%i/%i)\n",num,mb->size);
    }

    MB_UNLOCK(mb);
    return mb->pipe[0];
}

int          mailbox_clear_pollable_event(mailbox_t* mb)
{
    if (!mb) 
        return errlogargs();

    MB_LOCK(mb);

    if (mb->pipe[0] < 0) {
        MB_UNLOCK(mb);
        return 0;
    }        

    {
        char buf[50];
        while(read(mb->pipe[0],buf,50)>0);
    }

    MB_UNLOCK(mb);
    return 0;
}

/* Create a mvpoll key for a source. (sinks aren't really necessary right now) */
mvpoll_key_t* mailbox_get_mvpoll_src_key( mailbox_t* mb )
{
    if ( mb == NULL ) return errlogargs_null();
    
    int _critical_section( mailbox_t* mb ) {
        if ( mb->mv_key != NULL ) return 0;
        
        if (( mb->mv_key = malloc( sizeof( mb_mvpoll_key_t ))) == NULL ) return errlogmalloc();

        mb_mvpoll_key_t* key = (mb_mvpoll_key_t*)mb->mv_key;
        
        if ( mvpoll_key_base_init( mb->mv_key ) < 0 ) {
            return errlog( ERR_CRITICAL, "mvpoll_key_base_init\n" );
        }
        
        mb->mv_key->type            = MB_SRC_KEY_TYPE;
        mb->mv_key->poll            = _src_poll_wrapper;
        mb->mv_key->special_destroy = _src_key_destroy;
        
        /* Loop the key back to its owning mailbox */
        key->mb = mb;

        return 0;
    }
    
    int ret = 0;

    MB_LOCK_NULL( mb );
    ret = _critical_section( mb );
    MB_UNLOCK( mb );

    if ( ret < 0 ) return errlog_null( ERR_CRITICAL, "_critical_section\n" );

    return (mvpoll_key_t*)(mb->mv_key);
}

int          mailbox_size (mailbox_t* mb)
{
    if (!mb) 
        return errlog(ERR_WARNING,"Invalid arguments\n");

    //MB_LOCK(mb);
    return mb->size;
    //MB_UNLOCK(mb);
}

static void*   _mailbox_timed_get ( mailbox_t* mb, struct timespec* ts )
{
    void* mail;
    list_node_t* node;
    char buf[2];
    
    if ( sem_timedwait( &mb->list_size_sem, ts ) != 0 ) {
        if (errno != ETIMEDOUT) {
            debug_backtrace( 0,  "sem_timedwait\n" );
            perrlog("sem_timedwait");
        }
        return NULL;
    }

    MB_LOCK_NULL(mb);

    node = list_head(&mb->list);
    if (!node) {
        MB_UNLOCK_NULL(mb);
        return errlog_null(ERR_CRITICAL,"mailbox empty. constraint failed\n");
    }

    if (mb->pipe[0] != -1) { 
        if (read(mb->pipe[0],buf,1)<1) 
           perrlog("read");
    }

    /* Update MVPOLL if necessary */
    if ( mb->mv_key != NULL ) mvpoll_key_notify_observers( mb->mv_key, ( mb->size > 1 ) ? MVPOLLIN : 0 );

    mail = list_node_val(node);
    
    if(list_remove(&mb->list,node)<0) {
        MB_UNLOCK_NULL(mb);
        return errlog_null(ERR_CRITICAL,"list remove failed\n");
    }
    mb->size--;

    MB_UNLOCK_NULL(mb);
    return mail;
}

static void*   _mailbox_get (mailbox_t* mb, wait_func_t wfunc)
{
    void* mail;
    list_node_t* node;
    char buf[2];
    
    if (wfunc(&mb->list_size_sem)<0) {
        if (errno != EAGAIN) 
            perrlog("wfunc");
        return NULL;
    }

    MB_LOCK_NULL(mb);

    node = list_head(&mb->list);
    if (!node) {
        MB_UNLOCK_NULL(mb);
        errlog_null(ERR_CRITICAL,"mailbox empty. constraint failed\n");
        return NULL;
    }

    /* empty pollable event if in use */
    if (mb->pipe[0] != -1) { 
        if (read(mb->pipe[0],buf,1)<1) 
            perrlog("read");
    }
    
    mail = list_node_val(node);
    
    if(list_remove(&mb->list,node)<0) {
        MB_UNLOCK_NULL(mb);
        return errlog_null(ERR_CRITICAL,"list remove failed\n");
    }
    mb->size--;
    
    MB_UNLOCK_NULL(mb);
    return mail;
}

static eventmask_t   _src_poll_wrapper  ( mvpoll_key_t* key )
{
    mb_mvpoll_key_t* mb_key = (mb_mvpoll_key_t*)key;
    int size;
    
    if (( mb_key == NULL ) || ( mb_key->mb == NULL )) {
        errlogargs();
        return MVPOLLERR;
    }

    if ( key->type != MB_SRC_KEY_TYPE ) {
        errlog( ERR_CRITICAL, "Incorrect key type[%d], expecting %d\n", key->type, MB_SRC_KEY_TYPE );
        return MVPOLLERR;
    }
    
    if ( sem_getvalue( &mb_key->mb->list_size_sem, &size ) < 0 ) {
        perrlog( "sem_getvalue" );
        return MVPOLLERR;
    }
    
    if ( size < 0 ) {
        errlog( ERR_CRITICAL, "mailbox size is less than zero[%d]\n", size );
        return MVPOLLERR;
    } else if ( size > 0 ) {
        return MVPOLLIN;
    }

    return 0;
}

static int           _src_key_destroy   ( mvpoll_key_t* key )
{
    if ( key == NULL ) return errlogargs();
    
    if ( key->type != MB_SRC_KEY_TYPE ) return errlog( ERR_CRITICAL, "Invalid key type: %d\n", key->type );
    mb_mvpoll_key_t* mb_key = (mb_mvpoll_key_t*)key;
    
    mailbox_t* mb = mb_key->mb;
    if  ( mb  == NULL ) errlog( ERR_CRITICAL, "NULL mailbox for the mailbox key\n" );
    else {
        if ( mb->mv_key != key ) errlog( ERR_CRITICAL, "Key is pointing to the incorrect mailbox\n" );
        else  mb->mv_key = NULL;
    }
    
    mb_key->mb = NULL;
    
    return 0;
}


