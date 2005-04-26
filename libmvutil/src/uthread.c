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
#include "uthread.h"

#include <pthread.h>
#include "errlog.h"

#define SMALL_STACK_SIZE 512*1024

uthread_attr_t uthread_attr;

pthread_attr_t small_detached_attr;

struct sched_param rr_high_priority;
struct sched_param rr_medium_priority;
struct sched_param rr_low_priority;

struct sched_param other_high_priority;
struct sched_param other_medium_priority;
struct sched_param other_low_priority;

int uthread_init (void)
{
    int c;
    unsigned int min, max;

    if (pthread_attr_init(&small_detached_attr)<0)
        return perrlog("pthread_attr_init");

    if (pthread_attr_setstacksize(&small_detached_attr,SMALL_STACK_SIZE)<0)
        return perrlog("pthread_attr_setstacksize");
    if (pthread_attr_setdetachstate(&small_detached_attr,PTHREAD_CREATE_DETACHED)<0)
        return perrlog("pthread_attr_setdetachstate");
/*     if (pthread_attr_setschedpolicy(&small_detached_attr,SCHED_RR)<0) */
/*         return perrlog("pthread_attr_setschedpolicy"); */

    min = sched_get_priority_min(SCHED_RR);
    max = sched_get_priority_max(SCHED_RR);
    rr_medium_priority.sched_priority = (min+max)/2;
    rr_high_priority.sched_priority = rr_medium_priority.sched_priority + 1;
    rr_low_priority.sched_priority = rr_medium_priority.sched_priority - 1;

    /* XXX According to the man page, Priority doesn't matter for SCHED_OTHER threads */
    min = sched_get_priority_min(SCHED_OTHER);
    max = sched_get_priority_max(SCHED_OTHER);
    other_medium_priority.sched_priority = (min+max)/2;
    other_high_priority.sched_priority = other_medium_priority.sched_priority + 1;
    other_low_priority.sched_priority  = other_medium_priority.sched_priority - 1;

    // We do this even though it apparently has no effect with NPTL -- be sure to
    // call pthread_setschedparam() manually after creating each new thread. XX
/*     if (pthread_attr_setschedparam(&small_detached_attr,&rr_medium_priority)<0) */
/*         return perrlog("pthread_attr_setschedparam"); */

    for ( c = 0 ; c < ( sizeof ( uthread_attr) / sizeof ( pthread_attr_t ) ) ; c++ ) {
        memcpy(&(((pthread_attr_t*)&uthread_attr)[c]), &small_detached_attr, sizeof ( pthread_attr_t ) );
    }

    if ( pthread_attr_setschedpolicy( &uthread_attr.rr.low, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.rr.medium, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.rr.high, SCHED_RR ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.low, SCHED_OTHER ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.medium, SCHED_OTHER ) < 0 ||
         pthread_attr_setschedpolicy( &uthread_attr.other.high, SCHED_OTHER ) < 0 ) {
        return perrlog( "pthread_attr_setschedpolicy" );
    }
    
    if ( pthread_attr_setschedparam ( &uthread_attr.rr.low, &rr_low_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.rr.medium, &rr_medium_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.rr.high, &rr_high_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.low, &other_low_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.medium, &other_medium_priority ) < 0 ||
         pthread_attr_setschedparam ( &uthread_attr.other.high, &other_high_priority ) < 0 )
    {
        return perrlog ( "pthread_attr_setschedpolicy" );
    }

    return 0;
}
