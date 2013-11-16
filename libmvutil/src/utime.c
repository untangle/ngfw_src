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

#include <sys/types.h>
#include <unistd.h>
#include "mvutil/errlog.h"

#include "mvutil/utime.h"

unsigned long utime_usec_diff (struct timeval* earlier, struct timeval* later)
{
    u_long usec_diff = 0;
    
    if (!earlier || !later) return 0;
    
    usec_diff  = (later->tv_sec - earlier->tv_sec)*1000000;
    usec_diff += (later->tv_usec) - (earlier->tv_usec);
    
    return usec_diff;
}

unsigned long utime_usec_diff_now (struct timeval* earlier)
{
    struct timeval tv;

    if ( gettimeofday( &tv, NULL ) < 0 )
        return 0;
    
    return utime_usec_diff(earlier,&tv);
}

int utime_usec_add (struct timeval* tv, long microsec )
{
    if ( tv == NULL ) return -1;

    tv->tv_sec  += USEC_TO_SEC( microsec );
    tv->tv_usec += microsec % U_SEC;

    /* Handle overflow from adding */
    if ( tv->tv_usec >= U_SEC ) {
        tv->tv_sec  += USEC_TO_SEC( tv->tv_usec );
        tv->tv_usec  = tv->tv_usec % U_SEC;
    } else if ( tv->tv_usec < 0 ) {
        /* just in case?*/
        tv->tv_usec = 0;
    }
    
    return 0;
}

int utime_usec_add_now( struct timeval* tv, long microsec )
{
    if ( tv == NULL )
        return errlogargs();

    if ( gettimeofday( tv, NULL ) < 0 )
        return perrlog( "gettimeofday" );

    return utime_usec_add( tv, microsec );
}

int utime_msec_add ( struct timeval* tv, long millisec )
{
    return utime_usec_add( tv, MSEC_TO_USEC( millisec ));
}

int utime_msec_add_now( struct timeval* tv, long millisec )
{
    return utime_usec_add_now( tv, MSEC_TO_USEC( millisec ));
}

void*         utime_timer_start_sem(void* utime_timer_struct)
{
    struct utime_timer* t = (struct utime_timer*) utime_timer_struct;
    
    usleep(t->usec);
    sem_post(t->sem_to_post);
    return NULL;
}
