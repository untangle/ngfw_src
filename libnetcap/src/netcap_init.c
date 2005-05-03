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
#include "netcap_init.h"

#include <stdio.h>
#include <stdlib.h>
#include <netdb.h>
#include <unistd.h>
#include <pthread.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include "netcap_globals.h"
#include "netcap_subscriptions.h"
#include "netcap_server.h"
#include "netcap_queue.h"
#include "netcap_hook.h"
#include "netcap_tcp.h"
#include "netcap_udp.h"
#include "netcap_icmp.h"
#include "netcap_interface.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_antisubscribe.h"
#include "netcap_shield.h"
#include "netcap_sched.h"

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
} _init = {
    .status STATUS_UNINITIALIZED,
    .mutex PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP
};

static int _netcap_init( int shield_enable );


int netcap_init( int shield_enable )
{
    int ret;

    if ( pthread_mutex_lock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    if (( ret = _netcap_init( shield_enable )) == 0 ) {
        _init.status = STATUS_INITIALIZED;
    } else {
        _init.status = STATUS_ERROR;
    }

    if ( pthread_mutex_unlock( &_init.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    /* This must happen after netcap is initialized */
    if (netcap_local_antisubscribe_init()<0)
        return perrlog("netcap_local_antisubscribe_init");
    
    return ret;
    
}

/*! \brief must be called before netcap is used 
 *  XXX how can this be done automatically? _init doesnt work
 */
static int _netcap_init( int shield_enable )
{    
    /* Due to structuring of the iptables rules, netcap_interface_init must go before
     * netcap_subscription_init, now netcap_redirect_tables_init is the only thing that
     * must go before netcap_subscription_init and netcap_interface_init. */
    if (netcap_redirect_tables_init()<0)
        return perrlog("netcap_redirect_tables_init");
    if (netcap_interface_init()<0) 
        return perrlog("netcap_interface_init");
    if (netcap_sesstable_init()<0)
        return perrlog("netcap_sesstable_init");
    if (netcap_sessions_init()<0)
        return perrlog("netcap_sessions_init");
    if (netcap_hooks_init()<0) 
        return perrlog("netcap_hooks_init");
    if (netcap_subscriptions_init()<0) 
        return perrlog("netcap_subscriptions_init");
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
    if ( shield_enable == NETCAP_SHIELD_ENABLE && ( netcap_shield_init() < 0 ))
        return perrlog("netcap_shield_init");

    debug(1,"Netcap %s Initialized\n",netcap_version());

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

    if ( ret < 0 )
        return errlog( ERR_CRITICAL, "NETCAP: Skipping second clean\n" );;

    if (netcap_local_antisubscribe_cleanup()<0)
        perrlog("netcap_local_antisubscribe_cleanup");
    if (netcap_unsubscribe_all()<0) 
        perrlog("netcap_unsubscribe_all");
    if (netcap_server_shutdown()<0)
        perrlog("netcap_server_shutdown");
    if (netcap_subscriptions_cleanup()<0)
        perrlog("netcap_subscriptions_cleanup");
    if (netcap_queue_cleanup()<0)
        perrlog("netcap_queue_cleanup");
    if (netcap_tcp_cleanup()<0)
        perrlog("netcap_tcp_cleanup");
    if (netcap_udp_cleanup()<0)
        perrlog("netcap_udp_cleanup");
    if (netcap_hooks_cleanup()<0) 
        perrlog("netcap_hooks_cleanup");
    /* XXX Should these return */
    if (netcap_sessions_cleanup()<0)
        return perrlog("netcap_sessions_cleanup");
    if (netcap_sesstable_cleanup()<0)
        return perrlog("netcap_sesstable_cleanup");
    if (netcap_interface_cleanup()<0) 
        return perrlog("netcap_interface_cleanup");
    if (netcap_redirect_tables_cleanup()<0)
        return perrlog("netcap_redirect_tables_init");
    if (netcap_shield_cleanup()<0)
        perrlog("netcap_shield_cleanup");
    if (netcap_sched_cleanup_z ( NULL ) < 0 )
        perrlog("netcap_sched_cleanup");
    
    debug(1,"NETCAP: Cleaned.\n");

    /* No need to lock since it has already moved out of the initialized state. */
    _init.status = STATUS_CLEAN;

    if ( ret < 0 )
        return errlog( ERR_CRITICAL, "NETCAP: Skipping second clean\n" );;


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

int netcap_update_address( int inside, int outside )
{
    if ( _init.status == STATUS_INITIALIZED ) {
        if ( netcap_interface_update_address( inside, outside ) < 0 )
            return errlog( ERR_CRITICAL, "netcap_interface_update_address\n" );
    } else {
        debug( 1, "NETCAP: Not updating address because netcap is not initialized\n" );
    }

    return 0;
}


