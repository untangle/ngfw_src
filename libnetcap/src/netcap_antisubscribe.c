/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_antisubscribe.c,v 1.1 2004/11/09 19:39:58 dmorris Exp $
 */

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include <libnetcap.h>
#include "netcap_subscriptions.h"
#include "netcap_antisubscribe.h"

/**
 * This stores the number of subscriptions with the include-local flag set
 * When this goes from 0->1 we remove the marking of local connections so they will be caught
 * When it goes from 1->0 we add the marking back
 */
static int _netcap_local_sub_count = 0;

/**
 * This stores the antisubscribe ID's used to anti subscribe to local traffic
 */
static int _netcap_local_sub_id[NETCAP_MAX_INTERFACES];


int netcap_local_antisubscribe_init ( void ) {
    int c;

    for ( c = NETCAP_MAX_INTERFACES ; c-- ; ) {
        _netcap_local_sub_id[c] = 0;
    }
    
    _netcap_local_sub_count = 1;
    if ( netcap_local_antisubscribe_add () < 0 ) {
        return errlog (ERR_CRITICAL, "netcap_local_antisubscribe_add");
    }

    return 0;
}

int netcap_local_antisubscribe_cleanup ( void ) {
    _netcap_local_sub_count = 0;
    if ( netcap_local_antisubscribe_remove () < 0) {
        return perrlog("netcap_local_antisubscribe_remove");
    } 

    return 0;
}

int netcap_local_antisubscribe_add (void)
{
    int i;
    in_addr_t* if_addrs = netcap_interface_addrs();
    int if_count = netcap_interface_count();

    _netcap_local_sub_count--;

    /**
     * if there are zero subscriptions for local traffic, antisubscribe to it
     */
    if (_netcap_local_sub_count <= 0) {
        _netcap_local_sub_count = 0;

        for (i=0;i<if_count;i++) {
            in_addr_t mask = (in_addr_t)0xffffffff;
            if (_netcap_local_sub_id[i] <= 0) {
                _netcap_local_sub_id[i] = 
                    netcap_subscribe(NETCAP_FLAG_ANTI_SUBSCRIBE, NULL,IPPROTO_ALL,
                                     NC_INTF_UNK, NC_INTF_UNK,
                                     NULL,NULL,0,0,&if_addrs[i], &mask,0,0);

                if (_netcap_local_sub_id[i] < 0 ) perrlog("netcap_subscribe");

                debug(7,"SUBMANAGER: Adding local antisubscribe for '%s' (%i)\n",
                      unet_inet_ntoa(if_addrs[i]),_netcap_local_sub_id[i]);
            }
        }
    }
    return 0;
}

int netcap_local_antisubscribe_remove (void)
{
    int i;
    _netcap_local_sub_count++;

    /**
     * if this is the first subscription to local traffic, remove the antisubscribes
     */
    if (_netcap_local_sub_count <= 1) {
        _netcap_local_sub_count = 1;
        
        for ( i=0 ; i <netcap_interface_count();i++) {
            if (_netcap_local_sub_id[i] > 0) {
                debug(7,"SUBMANAGER: Removing local interface antisubscribe (%i)\n",
                      _netcap_local_sub_id[i]);
                _netcap_local_sub_id[i] = netcap_unsubscribe(_netcap_local_sub_id[i]);
                if (_netcap_local_sub_id[i] < 0)
                    perrlog("netcap_unsubscribe");
            }
        }
    }
    return 0;
}

