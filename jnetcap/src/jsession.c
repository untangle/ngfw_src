/*
 * $HeadURL$
 */

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <signal.h>
#include <unistd.h>
#include <inttypes.h>

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

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getSession
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL JF_Session( getSession )
    ( JNIEnv *env, jclass _class, jlong session_id )
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
    
    return UINT_TO_JLONG( session );
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getLongValue
 * Signature: (IJ)J
 */
JNIEXPORT jlong JNICALL JF_Session( getLongValue )
  (JNIEnv *env, jclass _this, jint flag, jlong session_ptr )
{
    netcap_session_t* session;
    JLONG_TO_SESSION( session, session_ptr );

    netcap_endpoints_t* clientSide = &session->cli;
    netcap_endpoints_t* serverSide = &session->srv;
    if ( clientSide == NULL || serverSide == NULL ) {
        return (jlong)errlog( ERR_CRITICAL, "Unexpected NULL: 0x%016"PRIxPTR" 0x%016"PRIxPTR"\n", (uintptr_t)clientSide, (uintptr_t)serverSide);
    }

    switch( flag ) {
    case JN_Session( FLAG_CLIENTSIDE_CLIENT_HOST ): return UINT_TO_JLONG( clientSide->cli.host.s_addr );
    case JN_Session( FLAG_CLIENTSIDE_SERVER_HOST ): return UINT_TO_JLONG( clientSide->srv.host.s_addr );
    case JN_Session( FLAG_SERVERSIDE_CLIENT_HOST ): return UINT_TO_JLONG( serverSide->cli.host.s_addr );
    case JN_Session( FLAG_SERVERSIDE_SERVER_HOST ): return UINT_TO_JLONG( serverSide->srv.host.s_addr );
    case JN_Session( FLAG_ID ): return session->session_id;
    }

    return errlog(ERR_CRITICAL, "Unknown flag: %i\n", flag); 
}

/*
 * Class:     com_untangle_jnetcap_NetcapSession
 * Method:    getIntValue
 * Signature: (IJ)I
 */
JNIEXPORT jint JNICALL JF_Session( getIntValue )
  (JNIEnv *env, jclass _this, jint flag, jlong session_ptr )
{
    netcap_session_t* session;
    JLONG_TO_SESSION( session, session_ptr );

    switch( flag ) { 

    case JN_Session( FLAG_PROTOCOL ): return session->protocol;

    case JN_TCPSession( FLAG_CLIENT_FD ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        return session->client_sock;

    case JN_TCPSession( FLAG_SERVER_FD ):
        if ( session->protocol != IPPROTO_TCP ) return errlog( ERR_CRITICAL, "Expecting TCP\n" );
        return session->server_sock;

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

    netcap_endpoints_t* clientSide = &session->cli;
    netcap_endpoints_t* serverSide = &session->srv;
    if ( clientSide == NULL || serverSide == NULL ) {
        return (jlong)errlog( ERR_CRITICAL, "Unexpected NULL: 0x%016"PRIxPTR" 0x%016"PRIxPTR"\n", (uintptr_t)clientSide, (uintptr_t)serverSide);
    }
    
    switch( flag ) {
    case JN_Session( FLAG_CLIENTSIDE_CLIENT_PORT ): return clientSide->cli.port;
    case JN_Session( FLAG_CLIENTSIDE_SERVER_PORT ): return clientSide->srv.port;
    case JN_Session( FLAG_SERVERSIDE_CLIENT_PORT ): return serverSide->cli.port;
    case JN_Session( FLAG_SERVERSIDE_SERVER_PORT ): return serverSide->srv.port;
    case JN_Session( FLAG_CLIENT_INTERFACE ): return clientSide->intf;
    case JN_Session( FLAG_SERVER_INTERFACE ): return serverSide->intf;
    }

    return errlog(ERR_CRITICAL, "Unknown flag: %i\n", flag); 
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
