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
#include "netcap_init.h"

#include <stdio.h>
#include <stdlib.h>
#include <netdb.h>
#include <unistd.h>
#include <pthread.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "netcap_globals.h"
#include "netcap_server.h"
#include "netcap_queue.h"
#include "netcap_hook.h"
#include "netcap_tcp.h"
#include "netcap_udp.h"
#include "netcap_icmp.h"
#include "netcap_interface.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_sched.h"
#include "netcap_nfconntrack.h"
#include "netcap_virtual_interface.h"

#define NETCAP_TUN_DEVICE_NAME "utun"


enum {
    STATUS_UNINITIALIZED,
    STATUS_INITIALIZED,
    STATUS_CLEANING,
    STATUS_CLEAN,
    STATUS_ERROR
};

static struct {
    int status;
    pthread_mutex_t mutex;
    pthread_key_t   tls_key;
} _init = {
    .status = STATUS_UNINITIALIZED,
    .mutex  = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP
};

static int _netcap_init();
static int _tls_init   ( void* buf, size_t size );


int netcap_init()
{
    int ret;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    if (( ret = _netcap_init()) == 0 ) {
        _init.status = STATUS_INITIALIZED;
    } else {
        _init.status = STATUS_ERROR;
    }

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }
    
    return ret;
    
}

/*! \brief must be called before netcap is used 
 *  XXX how can this be done automatically? _init doesnt work
 */
static int _netcap_init()
{
    int num_handles = 15;
    char *val = getenv("CONNTRACK_NUM_HANDLES");
    if ( val == NULL || (( num_handles = atoi( val )) < 1 )) num_handles = 15;

    if ( pthread_key_create( &_init.tls_key, uthread_tls_free ) != 0 )
        return perrlog( "pthread_key_create\n" );

    if (netcap_sesstable_init()<0)
        return perrlog("netcap_sesstable_init");
    if (netcap_sessions_init()<0)
        return perrlog("netcap_sessions_init");
    if (netcap_hooks_init()<0) 
        return perrlog("netcap_hooks_init");
    if (netcap_queue_init()<0) 
        return perrlog("netcap_queue_init");
    if (netcap_tcp_init()<0) 
        return perrlog("netcap_tcp_init");
    if (netcap_udp_init()<0) 
        return perrlog("netcap_udp_init");
    if (netcap_icmp_init()<0) 
        return perrlog("netcap_icmp_init");    
    if (netcap_server_init()<0) 
        return perrlog("netcap_server_init");
    if (netcap_sched_init()<0)
        return perrlog("netcap_sched_init");
    if (netcap_nfconntrack_init(num_handles)<0)
        return errlog( ERR_CRITICAL, "netcap_nfconntrack_init\n" );
    if (netcap_virtual_interface_init( NETCAP_TUN_DEVICE_NAME ) < 0 )
        return errlog( ERR_CRITICAL, "netcap_virtual_interface_init\n" );
    
    debug(1,"NETCAP %s Initialized\n",netcap_version());

    return 0;
}

int netcap_cleanup()
{
    debug(1,"NETCAP: Netcap Cleaning up...\n");

    int ret = 0;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 )
        perrlog( "pthread_mutex_lock" );

    if ( _init.status == STATUS_CLEAN ) {
        ret = errlog( ERR_CRITICAL, "NETCAP: Already cleaned\n" );
    }
    
    _init.status = STATUS_CLEANING;

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 )
        perrlog( "pthread_mutex_lock" );

    if ( ret < 0 ) {
        _init.status = STATUS_CLEAN;
        return errlog( ERR_CRITICAL, "NETCAP: Skipping second clean\n" );
    }

    if (netcap_server_shutdown()<0)
        perrlog("netcap_server_shutdown");
    if (netcap_queue_cleanup()<0)
        perrlog("netcap_queue_cleanup");
    if (netcap_tcp_cleanup()<0)
        perrlog("netcap_tcp_cleanup");
    if (netcap_udp_cleanup()<0)
        perrlog("netcap_udp_cleanup");
    if (netcap_hooks_cleanup()<0) 
        perrlog("netcap_hooks_cleanup");
    if (netcap_sessions_cleanup()<0)
        perrlog("netcap_sessions_cleanup");
    if (netcap_sesstable_cleanup()<0)
        perrlog("netcap_sesstable_cleanup");
    if (netcap_sched_cleanup_z ( NULL ) < 0 )
        perrlog("netcap_sched_cleanup");
    netcap_virtual_interface_destroy();
    
    debug(1,"NETCAP: Cleaned.\n");

    /* No need to lock since it has already moved out of the initialized state. */
    _init.status = STATUS_CLEAN;

    return 0;
}

int netcap_is_initialized()
{
    int ret;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 )
        return perrlog( "pthread_mutex_lock" );

    if ( _init.status == STATUS_INITIALIZED || _init.status == STATUS_CLEANING ) {
        ret = 1;
    } else {
        ret = 0;
    }

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 )
        return perrlog( "pthread_mutex_unlock" );

    return ret;
}

netcap_tls_t* netcap_tls_get( void )
{
    netcap_tls_t* tls = NULL;

    if (( tls = uthread_tls_get( _init.tls_key, sizeof( netcap_tls_t ), _tls_init )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "uthread_get_tls\n" );
    }
    
    return tls;
}

static int _tls_init( void* buf, size_t size )
{
    netcap_tls_t* tls = buf;

    if (( size != sizeof( netcap_tls_t )) || ( tls == NULL )) return errlogargs();
        
    if (( netcap_session_tls_init( &tls->session )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_session_tls_init\n" );
    }

    return 0;
}
