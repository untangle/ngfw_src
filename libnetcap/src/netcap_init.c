/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_init.c,v 1.4 2005/01/24 03:59:01 rbscott Exp $
 */
#include "netcap_init.h"

#include <stdio.h>
#include <stdlib.h>
#include <netdb.h>
#include <unistd.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include "netcap_globals.h"
#include "netcap_subscriptions.h"
#include "netcap_server.h"
#include "netcap_queue.h"
#include "netcap_hook.h"
#include "netcap_tcp.h"
#include "netcap_udp.h"
#include "netcap_interface.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_antisubscribe.h"
#include "netcap_shield.h"
#include "netcap_sched.h"

int netcap_inited = 0;

/*! \brief must be called before netcap is used 
 *  XXX how can this be done automatically? _init doesnt work
 */
int netcap_init( int shield_enable )
{
    /* Due to structuing of the iptables rules, netcap_interface_init must go before
     * netcap_subscription_init */
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
    if (netcap_server_init()<0) 
        return perrlog("netcap_server_init");
    if (netcap_sched_init()<0)
        return perrlog("netcap_sched_init");
    if ( shield_enable == NETCAP_SHIELD_ENABLE && ( netcap_shield_init() < 0 ))
        return perrlog("netcap_shield_init");

    debug(1,"Netcap %s Initialized\n",netcap_version());
    netcap_inited = 1;

    /* This must happen after netcap is initialized */
    if (netcap_local_antisubscribe_init()<0)
        return perrlog("netcap_local_antisubscribe_init");

    return 0;
}

int netcap_cleanup()
{
    TEST_INIT();
    
    debug(1,"NETCAP: Netcap Cleaning up...\n");
    
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
    if (netcap_shield_cleanup()<0)
        perrlog("netcap_shield_cleanup");
    if (netcap_sched_cleanup_z ( NULL ) < 0 )
        perrlog("netcap_sched_cleanup");
    
    debug(1,"NETCAP: Cleaned.\n");
    netcap_inited = 0;

    return 0;
}
