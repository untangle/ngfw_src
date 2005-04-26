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
    _netcap_local_sub_count--;

    /**
     * if there are zero subscriptions for local traffic, antisubscribe to it
     */
    if (_netcap_local_sub_count <= 0) {
        _netcap_local_sub_count = 0;
        
        if ( _netcap_local_sub_id[0] <= 0 ) {
            _netcap_local_sub_id[0] = 
                netcap_subscribe(( NETCAP_FLAG_ANTI_SUBSCRIBE | NETCAP_FLAG_LOCAL ),
                                 NULL,IPPROTO_ALL, NC_INTF_UNK, NC_INTF_UNK,
                                 NULL,NULL, 0, 0, NULL, NULL, 0, 0 );
            
            if ( _netcap_local_sub_id[0] < 0 ) perrlog( "netcap_subscribe" );
            
            debug( 7, "SUBMANAGER: Added local antisubscribes\n" );
        }        
    }
    return 0;
}

int netcap_local_antisubscribe_remove (void)
{
    _netcap_local_sub_count++;

    /**
     * if this is the first subscription to local traffic, remove the antisubscribes
     */
    if (_netcap_local_sub_count <= 1) {
        _netcap_local_sub_count = 1;
        
        if (_netcap_local_sub_id[0] > 0) {
            debug(7,"SUBMANAGER: Removing local interface antisubscribe (%i)\n",
                  _netcap_local_sub_id[0]);
            _netcap_local_sub_id[0] = netcap_unsubscribe(_netcap_local_sub_id[0]);
            if (_netcap_local_sub_id[0] < 0)
                perrlog("netcap_unsubscribe");
            }
    }
    return 0;
}

