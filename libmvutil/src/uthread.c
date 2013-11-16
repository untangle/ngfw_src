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
#include "mvutil/uthread.h"

#include <pthread.h>
#include <stdlib.h>

#include "mvutil/errlog.h"

#define SMALL_STACK_SIZE 96*1024

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
    //other_low_priority.sched_priority  = 0;

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

void  uthread_tls_free( void* buf )
{
    if ( buf != NULL ) free( buf );
}


void* uthread_tls_get( pthread_key_t tls_key, size_t size, int(*init)(void *buf, size_t size ))
{
    void* buf;
    void* verify;
    
    if ( size < 0 )
        return errlogargs_null();
        
    
    if (( buf = pthread_getspecific( tls_key )) == NULL ) {
        /* Buffer is not set yet, allocate a new buffer */
        if (( buf = malloc( size )) == NULL ) {
            return errlogmalloc_null();
        }
        
        /* Set the data on the key */
        if ( pthread_setspecific( tls_key, buf ) != 0 ) {
            free( buf );
            return perrlog_null( "pthread_setspecific" );
        }
        
        /* Just a sanity check to make sure the correct value is returned */
        if (( verify = pthread_getspecific( tls_key )) != buf ) {
            free( buf );
            return errlog_null( ERR_CRITICAL, "pthread_getspecific returned different val: %#10x->%#10x",
                                buf, verify );
        }
        
        /* If necessary, call the initializer function, call this last so if the initializer
         * allocates more memory it doesn't have to be freed if one of the previous errors occured */
        if (( init != NULL ) && ( init( buf, size ) < 0 )) {
            free( buf );
            pthread_setspecific( tls_key, NULL );
            return errlog_null( ERR_CRITICAL, "init: size %d\n", size );
        }

    }

    return buf;
}
