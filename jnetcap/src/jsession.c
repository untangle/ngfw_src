/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: jsession.c,v 1.12 2005/01/31 01:15:12 rbscott Exp $
 */

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

#include <jni.h>

#include "jnetcap.h"
#include "jsession.h"
#include "jerror.h"

#include JH_Session
#include JH_TCPSession
#include JH_UDPSession

/*
 * Class:     com_metavize_jnetcap_NetcapSession
 * Method:    getSession
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL JF_Session( getSession )
( JNIEnv *env, jclass _this, jint session_id, jshort protocol )
{
    netcap_session_t* session;

    if ( session_id == 0 ) {
        /* Throw an error */
        jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Invalid session id(0)\n" );
        return 0;
    }

    if (( session = netcap_sesstable_get( session_id )) == NULL ) {
        jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "netcap_sesstable_get(%10u)\n", session_id );
        return 0;
    }
    
    
    /* If necessary, verify that the protocol matches */
    /* XXX This is kind of a mess access wise, because only certain classes should 
     * be able to create sessions with unverified protocols */
    if ( protocol != 0 && session->protocol != protocol ) {
        jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Mismatched protocol: expected %d actual %d\n",
                       protocol, session->protocol );
        return 0;
    }
    
    return UINT_TO_JLONG( session );
}

/*
 * Class:     com_metavize_jnetcap_NetcapSession
 * Method:    getLongValue
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL JF_Session( getLongValue )
  (JNIEnv *env, jclass _this, jint req_id, jlong session_ptr )
{
    netcap_session_t* session;
    netcap_endpoint_t* endpoint;
    JLONG_TO_SESSION( session, session_ptr );

    endpoint = _get_endpoint( session, req_id );
    if ( endpoint == NULL ) return (jlong)errlog( ERR_CRITICAL, "_get_endpoint" );

    switch( req_id & JN_Session( FLAG_MASK )) {
    case JN_Session( FLAG_HOST ): return UINT_TO_JLONG( endpoint->host.s_addr );
        /* Coded this way to put other items below if necessary */
    }

    return (jlong)errlogargs();    
}

/*
 * Class:     com_metavize_jnetcap_NetcapSession
 * Method:    getIntValue
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL JF_Session( getIntValue )
  (JNIEnv *env, jclass _this, jint req_id, jlong session_ptr )
{
    netcap_session_t* session;
    netcap_endpoint_t* endpoint;
    JLONG_TO_SESSION( session, session_ptr );

    switch( req_id & JN_Session( FLAG_MASK )) { 
    case JN_Session( FLAG_ID ): return session->session_id;
    case JN_Session( FLAG_PROTOCOL ): return session->protocol;
    case JN_TCPSession( FLAG_FD ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        
        if ( req_id & JN_Session( FLAG_IF_CLIENT_MASK )) return session->client_sock;
        return session->server_sock;

    case JN_TCPSession( FLAG_ACKED ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        return (!session->syn_mode) & 1;

    case JN_UDPSession( FLAG_TTL ): 
        if ( session->protocol != IPPROTO_UDP ) return errlog( ERR_CRITICAL, "Expecting UDP\n" );
        return session->ttl;

    case JN_UDPSession( FLAG_TOS ): 
        if ( session->protocol != IPPROTO_UDP ) return errlog( ERR_CRITICAL, "Expecting UDP\n" );
        return session->ttl;
    }
    
    endpoint = _get_endpoint( session, req_id );
    if ( endpoint == NULL ) return errlog( ERR_CRITICAL, "_get_endpoint" );
    
    switch( req_id & JN_Session( FLAG_MASK )) {
    case JN_Session( FLAG_PORT ): return endpoint->port;
    case JN_Session( FLAG_INTERFACE ): return endpoint->intf;
    }

    return errlogargs();
}

/*
 * Class:     com_metavize_jnetcap_NetcapSession
 * Method:    getStringValue
 * Signature: (IJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL JF_Session( getStringValue )
  (JNIEnv* env, jclass _this, jint req_id, jlong session_ptr )
{
    netcap_session_t* session;
    netcap_endpoint_t* endpoint;
    char buf[NETCAP_MAX_IF_NAME_LEN]; /* XXX Update to the longest possible string returned */
    JLONG_TO_SESSION_NULL( session, session_ptr );
    
    endpoint = _get_endpoint( session, req_id );
    if ( endpoint == NULL ) return (jstring)errlog_null( ERR_CRITICAL, "_get_endpoint" );

    switch( req_id & JN_Session( FLAG_MASK )) {
    case JN_Session( FLAG_INTERFACE ):
        if ( endpoint->intf == NC_INTF_UNK ) return (*env)->NewStringUTF( env, "" );
        
        if ( netcap_interface_intf_to_string( endpoint->intf, buf, sizeof( buf )) < 0 ) {
            return errlog_null( ERR_CRITICAL, "netcap_intf_to_string\n" );
        }

        return (*env)->NewStringUTF( env, buf );
    }

    return (jstring)errlogargs_null();
}

/*
 * Class:     com_metavize_jnetcap_NetcapSession
 * Method:    raze
 * Signature: (J)I
 */
JNIEXPORT void JNICALL JF_Session( raze )
(JNIEnv* env, jclass _this, jlong session_ptr )
{
    netcap_session_t* session;

    JLONG_TO_SESSION_VOID( session, session_ptr );
    
    if ( netcap_session_raze( session ) < 0 ) {
        errlog( ERR_CRITICAL, "netcap_session_raze\n" );
    }
}




