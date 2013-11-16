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
#include "libmvutil.h"

#include <pthread.h>
#include "mvutil/debug.h"
#include "mvutil/errlog.h"
#include "mvutil/uthread.h"

static pthread_mutex_t init_mutex = PTHREAD_MUTEX_INITIALIZER;
static int             inited = 0;

extern int     unet_init        ( void );


int  libmvutil_init (void)
{
    int ret = 0;
    
    if ( pthread_mutex_lock ( &init_mutex ) < 0 )
        return -1;

    if ( !inited ) {
        if ( _debug_init() < 0 ) ret--;
        if ( _errlog_init() < 0 ) ret--;
        if ( uthread_init() < 0 ) ret--;
        if ( unet_init() < 0 ) ret--;
        if ( ret == 0 )
            inited = 1;
    }

    if ( pthread_mutex_unlock ( &init_mutex ) < 0 )
        return -1;

    return ret;
}

void libmvutil_cleanup(void)
{
}
