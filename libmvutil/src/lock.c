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
#include "mvutil/lock.h"

#include <stdlib.h>
#include <pthread.h>
#include <string.h>
#include <errno.h>
#include <semaphore.h>
#include "mvutil/hash.h"
#include "mvutil/errlog.h"
#include "mvutil/debug.h"

#define ERR_ALREADY_OWN -2

#define _lock_wr_own(lock)  ((lock)->wr_owner==pthread_self() ? 1 : 0)
#define _lock_rd_own(lock)  (ht_lookup(&(lock)->rd_owners,(void*)pthread_self()) ? 1 : 0)


lock_t* lock_create ()
{
    lock_t* nl = calloc(1,sizeof(lock_t));
    if (!nl) return errlogmalloc_null();

    return nl;
}

int  lock_init(lock_t* lock, int flags)
{
    int num;
    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    lock->wr_owner  = 0;
    lock->flags     = flags;

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS))
        if (ht_init(&lock->rd_owners,17,int_hash_func,int_equ_func,HASH_FLAG_ALLOW_DUPS)<0)
            return perrlog("ht_create_and_init");
    
    num = pthread_rwlock_init(&lock->rwlock,NULL);
    if (num)
        return perrlog("pthread_rwlock_init");
    
    return 0;
}

int  lock_destroy (lock_t* lock)
{
    int num;
    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if ((num = pthread_rwlock_destroy(&lock->rwlock)))
        perrlog("pthread_rwlock_destroy");

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS)) {
        if (ht_destroy(&lock->rd_owners)<0)
            perrlog("ht_destroy");
    }
    
    return 0;
}

void lock_free (lock_t* lock)
{
    free(lock);
}

int  lock_rdlock (lock_t* lock)
{
    long num;

    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}
    
    if (_lock_wr_own(lock)) {
        errlog(ERR_CRITICAL,"Trying to lock a lock you already own\n");
        return ERR_ALREADY_OWN;
    }
    
    if ((num = pthread_rwlock_rdlock(&lock->rwlock))!=0) 
        return perrlog("pthread_rwlock_rdlock");

    num = pthread_self();

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS))
        if (ht_add(&lock->rd_owners,(void*)num,(void*)num)<0)
            return perrlog("ht_add");

    return 0;
}

int  lock_try_rdlock (lock_t* lock)
{
    long num;

    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if (_lock_wr_own(lock)) {
        errlog(ERR_CRITICAL,"Trying to lock a lock you already own\n");
        return ERR_ALREADY_OWN;
    }

    if ((num = pthread_rwlock_tryrdlock(&lock->rwlock))!=0) {
        if (num != EBUSY) 
            perrlog("pthread_rwlock_tryrdlock");
        return -1;
    }

    num = pthread_self();

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS))
        if (ht_add(&lock->rd_owners,(void*)num,(void*)num)<0)
            return perrlog("ht_add");
    
    return 0;
}

int  lock_wrlock (lock_t* lock)
{
    int num;

    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS))
        if (_lock_rd_own(lock)) {
            errlog(ERR_CRITICAL,"Trying to wrlock a lock you already rd_own\n");
            return ERR_ALREADY_OWN;
        }
    
    if (_lock_wr_own(lock)) {
        errlog(ERR_CRITICAL,"Trying to wrlock a lock you already wr_own\n");
        return ERR_ALREADY_OWN;
    }
    
    if ((num = pthread_rwlock_wrlock(&lock->rwlock))!=0) 
        return perrlog("pthread_rwlock_wrlock");

    lock->wr_owner   = pthread_self();

    return 0;
}

int  lock_try_wrlock (lock_t* lock)
{
    int num;
    
    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS))
        if (_lock_rd_own(lock)) {
            errlog(ERR_CRITICAL,"Trying to wrlock a lock you already rd_own\n");
            return ERR_ALREADY_OWN;
        }
    
    if (_lock_wr_own(lock)) {
        errlog(ERR_WARNING,"Trying to lock a lock you already own\n");
        return ERR_ALREADY_OWN;
    }
    
    if ((num = pthread_rwlock_trywrlock(&lock->rwlock))!=0) {
        if (num != EBUSY) 
            perrlog("pthread_rwlock_trywrlock");
        return -1;
    }

    lock->wr_owner  = pthread_self();

    return 0;
}

int  lock_unlock (lock_t* lock)
{
    int num;

    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if (_lock_wr_own(lock)) 
        lock->wr_owner  = 0;
    else { /* must be a reader */
        if (!(lock->flags & LOCK_FLAG_NOTRACK_READERS)) {
            if (!_lock_rd_own(lock))
                return errlog(ERR_CRITICAL,"Trying to unlock a lock you don't own\n");
            else if (ht_remove(&lock->rd_owners,(void*)pthread_self())<0)
                return errlog(ERR_CRITICAL,"ht_remove failed\n");
        }
    }
    
    if ((num = pthread_rwlock_unlock(&lock->rwlock))!=0) 
        return perrlog("pthread_rwlock_unlock");

    return 0;
}

int  lock_wr_own (lock_t* lock)
{
    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}
    
    return _lock_wr_own(lock);
}

int  lock_wr_claimed (lock_t* lock)
{
    if (!lock) {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    if (lock->wr_owner == 0)
        return 0;
    return 1;
}

int  lock_sem_wait_loop (sem_t* sem)
{
    int n;
    do {
        if ((n = sem_wait(sem))!=0)
            perrlog("sem_wait");
    } while (n);

    return n;
}
