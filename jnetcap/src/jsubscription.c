/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: jsubscription.c,v 1.7 2005/01/21 01:10:03 rbscott Exp $
 */

#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <signal.h>

#include <libnetcap.h>
#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "jnetcap.h"

#include JH_Netcap
#include JH_Subscription
#include JH_SubscriptionGen

#define _verify_netcap_initialized()    if ( jnetcap_initialized() != _INITIALIZED ) return -1

static int _parse_flags( int flags );

/*
 * Class:     Subscription
 * Method:    createSubscription
 * Signature: (III[B[BIII[B[BII)J
 */
JNIEXPORT jint JNICALL JF_SubscriptionGen( createSubscription )
( JNIEnv *env, jobject _this, jint flags, jint protocol, 
  jint src_interface_set, jlong _src_address, jlong __src_netmask, 
  jint src_port_low, jint src_port_high, 
  jint dst_interface_set, jlong _dst_address, jlong __dst_netmask, 
  jint dst_port_low, jint dst_port_high )
{
    _verify_netcap_initialized();
    
    jint sub_id;
    
    in_addr_t src, src_netmask;
    in_addr_t dst, dst_netmask;
    
    in_addr_t* _src = &src;
    in_addr_t* _src_netmask = &src_netmask;
    in_addr_t* _dst = &dst;
    in_addr_t* _dst_netmask = &dst_netmask;

    /* Configure protocol */
    if ( protocol == JN_SubscriptionGen( PROTOCOL_ALL )) {
        protocol = IPPROTO_ALL;
    }

    src         = JLONG_TO_UINT( _src_address );
    src_netmask = JLONG_TO_UINT( __src_netmask );

    dst         = JLONG_TO_UINT( _dst_address );
    dst_netmask = JLONG_TO_UINT( __dst_netmask );

    if ( src == 0 ) _src = NULL;
    if ( src_netmask == 0 ) _src_netmask = NULL;

    if ( dst == 0 ) _dst = NULL;
    if ( dst_netmask == 0 ) _dst_netmask = NULL;

    /* Cannot use the arg parameter */
    sub_id = netcap_subscribe ( _parse_flags( flags ), NULL, protocol, src_interface_set, dst_interface_set, 
                                _src, _src_netmask, (u_short)src_port_low, (u_short)src_port_high,
                                _dst, _dst_netmask, (u_short)dst_port_low, (u_short)dst_port_high );

    if ( sub_id < 0 ) return errlog ( ERR_CRITICAL, "netcap_subscribe\n" );

    return sub_id;
}

/*
 * Class:     SubscriptionGenerator
 * Method:    unsubscribe
 * Signature: (I)J
 */
JNIEXPORT jint JNICALL JF_SubscriptionGen( unsubscribe )
  ( JNIEnv *env, jobject _this, jint sub_id )
{
    _verify_netcap_initialized();

    if ( sub_id < 0 ) return errlogargs();
        
    if ( netcap_unsubscribe ( sub_id ) < 0 ) return errlog ( ERR_CRITICAL, "netcap_unsubscribe\n" );
    
    return 0;
}

JNIEXPORT jint JNICALL JF_SubscriptionGen( unsubscribeAll )
  (JNIEnv *env, jclass _this)
{
    _verify_netcap_initialized();

    /* Iterate through the linked list and subscribe to each rule */
    
    if ( netcap_unsubscribe_all () < 0 ) return errlog ( ERR_CRITICAL, "netcap_unsubscribe_all\n" );
    
    return 0;
}

static int _parse_flags( int _flags )
{
    int flags = 0;

    if ( _flags & JN_SubscriptionGen( SERVER_UNFINISHED    )) flags |= NETCAP_FLAG_SRV_UNFINI;
    if ( _flags & JN_SubscriptionGen( CLIENT_UNFINISHED    )) flags |= NETCAP_FLAG_CLI_UNFINI;
    if ( _flags & JN_SubscriptionGen( ANTI_SUBSCRIBE       )) flags |= NETCAP_FLAG_ANTI_SUBSCRIBE;
    if ( _flags & JN_SubscriptionGen( BLOCK_CURRENT        )) flags |= NETCAP_FLAG_BLOCK_CURRENT;
    if ( _flags & JN_SubscriptionGen( LOCAL_ANTI_SUBSCRIBE )) flags |= NETCAP_FLAG_LOCAL_ANTI_SUBSCRIBE;
    if ( _flags & JN_SubscriptionGen( IS_FAKE              )) flags |= NETCAP_FLAG_IS_FAKE;

    return flags;

}

