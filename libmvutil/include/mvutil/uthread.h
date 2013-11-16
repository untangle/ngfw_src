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
#ifndef __UTHREAD_H
#define __UTHREAD_H

#include <pthread.h>

typedef struct {
    struct {
        pthread_attr_t high;
        pthread_attr_t medium;
        pthread_attr_t low;
    } rr, other;
} uthread_attr_t;

extern uthread_attr_t uthread_attr;

extern pthread_attr_t small_detached_attr;

extern struct sched_param rr_high_priority;
extern struct sched_param rr_medium_priority;
extern struct sched_param rr_low_priority;

extern struct sched_param other_high_priority;
extern struct sched_param other_medium_priority;
extern struct sched_param other_low_priority;

int   uthread_init    ( void );

void  uthread_tls_free( void* buf );

/**
 * Get the TLS for a specific key.
 * If necessary this will allocate memory and then call init (if non-null) to initialize the address.
 * tls_key: Key to retrieve data for.
 * size:    If necessary size of the memory to allocate, this is also passed into init for verification.
 * init:    Function pointer to call to initialize newly allocated memory, this is only called if
 *          a new value is being created.  This function should not call function that utilize TLS.
 */
void* uthread_tls_get ( pthread_key_t tls_key, size_t size, int(*init)( void *buf, size_t size ));

#endif
