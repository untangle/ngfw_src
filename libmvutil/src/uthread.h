/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
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

void* uthread_tls_get ( pthread_key_t tls_key, size_t size, int(*init)( void *buf, size_t size ));

#endif
