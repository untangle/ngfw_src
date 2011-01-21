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

#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#include <signal.h>
#include <unistd.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include "jsession.h"
#include JH_TCPSession

#define VERIFY_TCP_SESSION(session) if ( (session)->protocol != IPPROTO_TCP ) \
   return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, \
                         "JTCP: Expecting a TCP session: %d\n", (session)->protocol )

#define VERIFY_TCP_SESSION_VOID(session) if ( (session)->protocol != IPPROTO_TCP ) \
   return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, \
                              "JTCP: Expecting a TCP Session: %d\n", (session)->protocol )

static void _tcp_callback( jlong session_ptr, netcap_callback_action_t type, jint flags );

static __inline__ int* _get_sock_ptr( netcap_session_t* session, int if_client )
{
    return ( if_client == JNI_TRUE ) ? &session->client_sock : &session->server_sock;
}

static __inline__ int _get_sock( netcap_session_t* session, int if_client )
{
    return *(_get_sock_ptr( session, if_client ));
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    setServerEndpoint
 * Signature: (JIJI)I
 */
JNIEXPORT jint JNICALL JF_TCPSession( setServerEndpoint )
  (JNIEnv *env, jclass _class, jlong session_ptr, jlong client_addr, jint client_port, 
   jlong server_addr,  jint server_port, jint intf )
{
    netcap_session_t* session;

    JLONG_TO_SESSION( session, session_ptr );
    
    VERIFY_TCP_SESSION( session );

    if ( session->srv_state == CONN_STATE_COMPLETE ) {
        return errlog( ERR_CRITICAL, "Cannot modify server endpoint after completing the connection\n" );
    }

    session->srv.cli.host.s_addr = JLONG_TO_UINT( client_addr );
    /* XXX Should verify that the port is between 0 and 0xFFFF */
    session->srv.cli.port        = (u_short)client_port;
    session->srv.srv.host.s_addr = JLONG_TO_UINT( server_addr );
    session->srv.srv.port        = (u_short)server_port;

    if ( netcap_interface_intf_verify( intf ) < 0 ) {
        /* XXXX Consider making this a warning */
        debug( 5, "Invalid interface: %d\n", intf );
    } else {
        session->srv.intf     = intf;
    }

    return 0;
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    serverComplete
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( serverComplete )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, SRV_COMPLETE, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientComplete
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientComplete )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, CLI_COMPLETE, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientReset
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientReset )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, CLI_RESET, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientDrop
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientDrop )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, CLI_DROP, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientForwardReject
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientForwardReject )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, CLI_FORWARD_REJECT, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    liberate
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( liberate )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, LIBERATE, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientSendIcmp
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientSendIcmp )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _tcp_callback( session_ptr, CLI_ICMP, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    clientSendIcmpDestUnreach
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( clientSendIcmpDestUnreach )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags, jbyte icmp_code )
{
    netcap_session_t* session;

    JLONG_TO_SESSION_VOID( session, session_ptr );
    VERIFY_TCP_SESSION_VOID( session );    
        
    if ( icmp_code < 0 || icmp_code > NR_ICMP_UNREACH ) {
        return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid ICMP code: %d\n", icmp_code );
    }

    session->dead_tcp.exit_type = TCP_CLI_DEAD_ICMP;
    session->dead_tcp.type      = ICMP_DEST_UNREACH;
    session->dead_tcp.code      = icmp_code;
    
    _tcp_callback( session_ptr, CLI_ICMP, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    close
 * Signature: (JZI)I
 */
JNIEXPORT void JNICALL JF_TCPSession( close )
  (JNIEnv *env, jclass _class, jlong session_ptr, jboolean if_client )
{
    netcap_session_t* session;
    int* sock_ptr = NULL;
    int  sock;

    JLONG_TO_SESSION_VOID( session, session_ptr );
    
    VERIFY_TCP_SESSION_VOID( session );
    
    sock_ptr = _get_sock_ptr( session, if_client );
    sock = *sock_ptr;
    *sock_ptr = -1;

    /* XXX Check state here? */
    if ( sock > 0 && close( sock ) < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "close: %s", errstr );
    }        
}

/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    read
 * Signature: (JZ[B)I
 */
JNIEXPORT int JNICALL JF_TCPSession( read )
  (JNIEnv *env, jclass _class, jlong session_ptr, jboolean if_client, jbyteArray _data )
{
    netcap_session_t* session;
    jbyte* data;
    int ret = 0;
    int data_len;
    int sock;

    JLONG_TO_SESSION( session, session_ptr );
    
    VERIFY_TCP_SESSION( session );

    sock = _get_sock( session, if_client );

    if ( sock < 0 ) return errlog( ERR_CRITICAL, "Unable to read from an uninitialized socket\n" );
    
    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) return errlogmalloc();
    
    data_len = (*env)->GetArrayLength( env, _data );
    
    do { 
        if (( ret = read( sock, (char*)data, data_len )) < 0 ) ret = perrlog( "read" );
    } while ( 0 );

    (*env)->ReleaseByteArrayElements( env, _data, data, 0 );
    
    return ret;
}


/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    write
 * Signature: (JZ[B)I
 */
JNIEXPORT int JNICALL JF_TCPSession( write )
  (JNIEnv *env, jclass _class, jlong session_ptr, jboolean if_client, jbyteArray _data )
{
    netcap_session_t* session;
    jbyte* data;
    int ret = 0;
    int data_len;
    int sock;

    JLONG_TO_SESSION( session, session_ptr );
    
    VERIFY_TCP_SESSION( session );

    sock = _get_sock( session, if_client );
    
    if ( sock < 0 ) return errlog( ERR_CRITICAL, "Unable to write to uninitialized socket\n" );
    
    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) return errlogmalloc();
    
    data_len = (*env)->GetArrayLength( env, _data );
    
    do { 
        if ( write( sock, (char*)data, data_len ) < 0 ) ret = perrlog( "write" );
    } while ( 0 );

    (*env)->ReleaseByteArrayElements( env, _data, data, 0 );
    
    return ret;
}


/*
 * Class:     com_untangle_jnetcap_NetcapTCPSession
 * Method:    blocking
 * Signature: (JZZ)I
 */
JNIEXPORT void JNICALL JF_TCPSession( blocking )
  (JNIEnv *env, jclass _class, jlong session_ptr, jboolean if_client, jboolean mode )
{
    netcap_session_t* session;
    int ret;
    int sock = -1;

    JLONG_TO_SESSION_VOID( session, session_ptr );
    VERIFY_TCP_SESSION_VOID( session );
    
    sock = _get_sock( session, if_client );

    if ( mode == JNI_TRUE ) ret = unet_blocking_enable( sock );
    else                    ret = unet_blocking_disable( sock );
    
    if ( ret < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, 
                                   "JTCP: Unable to change blocking flags for fd: %d\n", sock );
    }
}

static void _tcp_callback( jlong session_ptr, netcap_callback_action_t action, jint _flags )
{
    netcap_session_t* session;
    int flags = 0;

    JLONG_TO_SESSION_VOID( session, session_ptr );
    VERIFY_TCP_SESSION_VOID( session );    
    
    if ( _flags & JN_TCPSession( NON_LOCAL_BIND )) flags |= SRV_COMPLETE_NONLOCAL_BIND;
    
    /* Verify that the callback is non-null */
    if ( session->callback == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "JTCP: null callback %d\n", action );
    }

    if ( session->callback( session, action, flags ) < 0 ) {
        debug( 2, "JTCP: callback failed=%d\n", action );

        /* Throw an error, but don't print an error message */
        jmvutil_error_throw( JMVUTIL_ERROR_STT, "JTCP: callback failed action=%d", action );
    }
}

