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
#include "libmvutil.h"

#include <pthread.h>
#include "debug.h"
#include "errlog.h"
#include "uthread.h"

static pthread_mutex_t init_mutex = PTHREAD_MUTEX_INITIALIZER;
static int             inited = 0;

int  libmvutil_init (void)
{
    int ret = 0;
    
    if ( pthread_mutex_lock ( &init_mutex ) < 0 )
        return -1;

    if ( !inited ) {
        if ( _debug_init() < 0 ) ret--;
        if ( _errlog_init() < 0 ) ret--;
        if ( uthread_init() < 0 ) ret--;
        if ( ret == 0 )
            inited = 1;
    }

    if ( pthread_mutex_unlock ( &init_mutex ) < 0 )
        return -1;

    return ret;
}

void libmvutil_cleanup(void)
{
    _errlog_cleanup();
}
