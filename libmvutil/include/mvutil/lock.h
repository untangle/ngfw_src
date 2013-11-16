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
#ifndef __LOCK_H
#define __LOCK_H

#include <pthread.h>
#include <semaphore.h>
#include "hash.h"

/**
 * Lock is a simple (pthread_rwlock,thread_id) tuple.
 * This gives several advantages.
 * You can see who the owner is, and also sometimes prevent deadlocks
 *
 * There are two modes, keeping track of the readers and not keep track
 * If you specify keeping track of the readers a hash table will be used
 * with slight overhead, but will catch the following deadlocks:
 * calling wrlock() when you hold a read lock
 * calling rdlock() when you hold a write lock
 * also calling unlock() on a lock you don't rd own will return an error
 *
 * When not keeping track of the readers, those conditions will not be detected.
 *
 * All other behavior is inherited from pthread_rwlock
 */

#define LOCK_FLAG_NOTRACK_READERS  1

typedef struct lock {
 
    pthread_rwlock_t rwlock;

    pthread_t wr_owner;

    char flags;

    struct hash_table rd_owners;
    
} lock_t;

/**
 * create a new lock 
 * still requires initialization
 * needs to be freed after being destroyed
 * @returns the new lock or NULL on error
 */
lock_t* lock_create (void);

/**
 * free's the memory a lock uses after call lock_destroy()
 * @returns void
 */
void    lock_free (lock_t* lock);

/**
 * initialize the lock 
 * see sem_init
 * @variable lock      the lock to be initialized
 * @variable pshared   see sem_init
 * @returns  0 or -1 on error
 */
int lock_init (lock_t* lock, int flags);

/**
 * destroys the lock 
 * see sem_init
 * @variable lock      the lock to be initialized
 * @returns  0 or -1 on error
 */
int lock_destroy (lock_t* lock);

/**
 * acquires a readlock 
 * if the current thread already owns it -2 is returned
 * otherwise it will block 
 * @variable lock      the lock to be acquired
 * @returns  0 or -1 on error
 */
int lock_rdlock (lock_t* lock);

/**
 * acquires a lock 
 * if the current thread already owns it -2 is returned
 * otherwise it will block 
 * @variable lock      the lock to be acquired
 * @returns  0 or -1 on error
 */
int lock_wrlock (lock_t* lock);

/**
 * trys to obtain a read lock 
 * on failure to obtain the lock -1 is returned
 * if you already own the lock -2 is returned
 * @variable lock      the lock to be acquired
 * @returns  0 or -1 on error
 */
int lock_try_rdlock (lock_t* lock);

/**
 * trys to obtain a write lock 
 * on failure to obtain the lock -1 is returned
 * if you already own the lock -2 is returned
 * @variable lock      the lock to be acquired
 * @returns  0 or -1 on error
 */
int lock_try_wrlock (lock_t* lock);

/**
 * release the lock
 * returns -1 on if you don't own the lock or other errors
 * @variable lock      the lock to be released
 * @returns  0 or -1 on error
 */
int lock_unlock (lock_t* lock);

/**
 * tests if the current thread owns the lock 
 * see sem_trywait
 * @variable lock      the lock 
 * @return  1 if owned by the current thread, 0 otherwise
 * @returns 0 if it is not owned by anyone
 */
int lock_wr_own (lock_t* lock);

/**
 * checks to see if the lock is claimed
 * @variable lock      the lock to be checked
 * @return 1 if claimed 0 if not, -1 on error
 */
int lock_wr_claimed (lock_t* lock);

/**
 * loops around a sem wait until succesful
 * returns sem_waits return value
 */
int lock_sem_wait_loop (sem_t* sem);



#endif
