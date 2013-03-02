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

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <signal.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>
#include <mvutil/unet.h>
#include <jmvutil.h>

#include <jni.h>

#include "jnetcap.h"
#include "jsession.h"

#include JH_Session
#include JH_TCPSession
#include JH_UDPSession


/**
 * XXXX All of the get functions should throw errors
 */

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getSession
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL JF_Session( getSession )
    ( JNIEnv *env, jclass _class, jlong session_id, jshort protocol )
{
    netcap_session_t* session;

    if ( session_id == 0 ) {
        /* Throw an error */
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session id(0)\n" );
        return 0;
    }

    if (( session = netcap_sesstable_get( (u_int64_t)session_id )) == NULL ) {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "netcap_sesstable_get(%10u)\n", session_id );
        return 0;
    }
    
    
    /* If necessary, verify that the protocol matches */
    int session_protocol = session->protocol;

    /* XXX This is kind of a mess access wise, because only certain classes should 
     * be able to create sessions with unverified protocols */
    if ( protocol != 0 && session_protocol != protocol ) {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Mismatched protocol: expected %d actual %d\n",
                       protocol, session->protocol );
        return 0;
    }
    
    return UINT_TO_JLONG( session );
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
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

    int flag = req_id & JN_Session( FLAG_MASK );
    switch( flag ) {
    case JN_Session( FLAG_HOST ): return UINT_TO_JLONG( endpoint->host.s_addr );
    case JN_Session( FLAG_NAT_FROM_HOST ):
      debug(10,"FLAG: FLAG_NAT_FROM_HOST = %s\n",unet_next_inet_ntoa(session->nat_info.reply.dst_address ));
      return session->nat_info.reply.dst_address;
    case JN_Session( FLAG_ID ): return session->session_id;
    case JN_Session( FLAG_NAT_TO_HOST   ):
      debug(10,"FLAG: FLAG_NAT_TO_HOST = %s\n",unet_next_inet_ntoa(session->nat_info.reply.src_address ));
      return session->nat_info.reply.src_address;
    }

    return (jlong)errlog(ERR_CRITICAL,"Invalid arguments: flag %i\n", flag);    
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getIntValue
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL JF_Session( getIntValue )
  (JNIEnv *env, jclass _this, jint req_id, jlong session_ptr )
{
    netcap_session_t* session;
    netcap_endpoint_t* endpoint;
    netcap_endpoints_t* endpoints;
    JLONG_TO_SESSION( session, session_ptr );

    switch( req_id & JN_Session( FLAG_MASK )) { 

    case JN_Session( FLAG_NAT_FROM_PORT ):
      debug(10,"FLAG: FLAG_NAT_FROM_PORT = %u\n",ntohs(session->nat_info.reply.dst_protocol_id ));
      return ntohs(session->nat_info.reply.dst_protocol_id);
    case JN_Session( FLAG_NAT_TO_PORT   ):
      debug(10,"FLAG: FLAG_NAT_TO_PORT = %u\n",ntohs(session->nat_info.reply.src_protocol_id ));      
      return ntohs(session->nat_info.reply.src_protocol_id);

    case JN_Session( FLAG_PROTOCOL ): return session->protocol;
    case JN_TCPSession( FLAG_FD ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        
        if ( req_id & JN_Session( FLAG_IF_CLIENT_MASK )) return session->client_sock;
        return session->server_sock;

    case JN_TCPSession( FLAG_ACKED ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        return (!session->syn_mode) & 1;

    case JN_UDPSession( FLAG_TTL ): 
        if ( session->protocol != IPPROTO_UDP ) {
            return errlog( ERR_CRITICAL, "Expecting UDP\n" );
        }
        return session->ttl;

    case JN_UDPSession( FLAG_TOS ): 
        if ( session->protocol != IPPROTO_UDP ) {
            return errlog( ERR_CRITICAL, "Expecting UDP\n" );
        }
        return session->ttl;

    }
    
    endpoint  = _get_endpoint( session, req_id );
    endpoints = _get_endpoints( session, req_id );
    if ( NULL == endpoint || NULL == endpoints ) return errlog( ERR_CRITICAL, "_get_endpoint(s)" );
    
    switch( req_id & JN_Session( FLAG_MASK )) {
    case JN_Session( FLAG_PORT ): return endpoint->port;
    case JN_Session( FLAG_INTERFACE ): return endpoints->intf;
    }

    return errlogargs();
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getClientMark
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL JF_Session( getClientMark )
(JNIEnv* env, jclass _this, jlong session_ptr )
{
    netcap_session_t* session;

    JLONG_TO_SESSION( session, session_ptr );

    if (!session)
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session (null)\n");

    if (session->protocol == IPPROTO_TCP) {
        return netcap_tcp_get_client_mark( session );
    } else {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "getClientMark unsupported protocol (%i)\n",session->protocol);
    }

    return -1;
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    setClientMark
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_Session( setClientMark )
(JNIEnv* env, jclass _this, jlong session_ptr, jint mark )
{
    netcap_session_t* session;

    JLONG_TO_SESSION_VOID( session, session_ptr );

    if (!session)
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session (null)\n");

    if (session->protocol == IPPROTO_TCP) {
        netcap_tcp_set_client_mark( session, mark );
        netcap_nfconntrack_update_mark( session, mark );
    } else {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "setClientMark unsupported protocol (%i)\n",session->protocol);
    }

    return;
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getServerMark
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL JF_Session( getServerMark )
(JNIEnv* env, jclass _this, jlong session_ptr )
{
    netcap_session_t* session;

    JLONG_TO_SESSION( session, session_ptr );

    if (!session)
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session (null)\n");

    if (session->protocol == IPPROTO_TCP) {
        return netcap_tcp_get_server_mark( session );
    } else {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "setServerMark unsupported protocol (%i)\n",session->protocol);
    }

    return -1;
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    setServerMark
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_Session( setServerMark )
(JNIEnv* env, jclass _this, jlong session_ptr, jint mark )
{
    netcap_session_t* session;

    JLONG_TO_SESSION_VOID( session, session_ptr );

    if (!session)
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session (null)\n");

    if (session->protocol == IPPROTO_TCP) {
        netcap_tcp_set_server_mark( session, mark );
        netcap_nfconntrack_update_mark( session, mark );
    } else {
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "setServerMark unsupported protocol (%i)\n",session->protocol);
    }

    return;
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    setServerIntf
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_Session( setServerIntf )
(JNIEnv* env, jclass _this, jlong session_ptr, jint intf )
{
    netcap_session_t* session;

    JLONG_TO_SESSION_VOID( session, session_ptr );

    if (!session)
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid session (null)\n");

    session->srv.intf = intf;

    return;
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
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

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    toString
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL JF_Session( toString )
  (JNIEnv *env, jclass _this, jlong session_ptr, jboolean ifClient )
{
    netcap_session_t* session = NULL;
    netcap_endpoints_t* endpoints;
    /* Just leaving some slack in case */
    char buf[sizeof( "[cc]xxx.xxx.xxx.xxx:ppppp -> [cc]xxx.xxx.xxx.xxx:ppppp" ) + 16];
    
    JLONG_TO_SESSION_NULL( session, session_ptr );

    if ( ifClient == JNI_TRUE ) endpoints = &session->cli;
    else                        endpoints = &session->srv;
    
    bzero( buf, sizeof( buf ));

    snprintf( buf, sizeof( buf ), "[%d]%s:%d -> [%d]%s:%d",
              session->cli.intf, unet_next_inet_ntoa( endpoints->cli.host.s_addr ), endpoints->cli.port,
              session->srv.intf, unet_next_inet_ntoa( endpoints->srv.host.s_addr ), endpoints->srv.port );

    return (*env)->NewStringUTF( env, buf );
}
