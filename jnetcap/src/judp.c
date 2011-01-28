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
#include <arpa/inet.h>

#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>

#include <linux/netfilter.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/utime.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include "jsession.h"

#include JH_IPTraffic
#include JH_UDPSession

#define VERIFY_PKT_SESSION(session) if ((session)->protocol != IPPROTO_UDP) \
   return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, \
                         "JPKT: Expecting a UDP session: %d\n", (session)->protocol )

#define VERIFY_PKT_SESSION_VOID(session) if ((session)->protocol != IPPROTO_UDP ) \
   return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, \
                              "JPKT: Expecting a UDP Session: %d\n", (session)->protocol )

#define JLONG_TO_PACKET( packet, packet_ptr )   do { \
    if (( packet_ptr ) == 0 ) return errlogargs(); \
    (packet) = (netcap_pkt_t*)JLONG_TO_ULONG(( packet_ptr )); \
  } while (0)

#define JLONG_TO_PACKET_NULL( packet, packet_ptr )   do { \
    if (( packet_ptr ) == 0 ) return errlogargs_null(); \
    (packet) = (netcap_pkt_t*)JLONG_TO_ULONG(( packet_ptr )); \
  } while (0)

#define JLONG_TO_PACKET_VOID( packet, packet_ptr )   do { \
    if (( packet_ptr ) == 0 ) return (void)errlogargs(); \
    (packet) = (netcap_pkt_t*)JLONG_TO_ULONG(( packet_ptr )); \
  } while (0)


static void _udp_callback( jlong session_ptr, netcap_callback_action_t action, jint _flags );

static netcap_endpoint_t* _get_pkt_endpoint( netcap_pkt_t* pkt, int req_id )
{
    if (( req_id & JN_IPTraffic( FLAG_SRC_MASK )) == JN_IPTraffic( FLAG_SRC )) return &pkt->src;
    
    return &pkt->dst;
}


/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    createIPTraffic
 * Signature: (JJJJ)I
 */
JNIEXPORT jlong JNICALL JF_IPTraffic( createIPTraffic )
  (JNIEnv* env, jclass _class, jlong src, jint src_port, jlong dst, jint dst_port )
{
    netcap_pkt_t* pkt;

    /* XXX This should go to a netcap_pkt_create, like function */
    if (( pkt = calloc( 1, sizeof( netcap_pkt_t ))) == NULL ) return errlogmalloc();
    
    pkt->proto           = IPPROTO_UDP;
    pkt->src.host.s_addr = JLONG_TO_UINT( src );
    pkt->src.port        = (u_short)src_port;
    pkt->src_intf        = NF_INTF_UNKNOWN;

    pkt->dst.host.s_addr = JLONG_TO_UINT( dst );
    pkt->dst.port        = (u_short)dst_port;
    pkt->dst_intf        = NF_INTF_UNKNOWN;

    pkt->ttl             = 255;
    pkt->tos             = 0;
    pkt->opts            = NULL;
    pkt->opts_len        = 0;
    pkt->data            = NULL;
    pkt->data_len        = 0;

    return UINT_TO_JLONG(pkt);
}


/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    send
 * Signature: (J[B)I
 */
JNIEXPORT jint JNICALL JF_IPTraffic( send )
  (JNIEnv* env, jclass _class, jlong _pkt, jbyteArray _data )
{
    jbyte* data;
    int ret = 0;
    int data_len;
    netcap_pkt_t* pkt = (netcap_pkt_t*)JLONG_TO_ULONG(_pkt);
    if ( pkt == NULL ) return errlogargs();

    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) {
        return errlogmalloc();
    }
    
    data_len = (*env)->GetArrayLength( env, _data );
    
    do { 
        if ( netcap_udp_send( (char*)data, data_len, pkt ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "netcap_udp_send\n" );
        }
    } while ( 0 );

    (*env)->ReleaseByteArrayElements( env, _data, data, 0 );
    
    return ret;
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    getLongValue
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL JF_IPTraffic( getLongValue )
  (JNIEnv* env, jclass _class, jlong pkt_ptr, jint req )
{
    netcap_pkt_t* pkt;
    netcap_endpoint_t* endpoint;

    /* XXX What happens on the long return */
    JLONG_TO_PACKET( pkt, pkt_ptr );

    endpoint = _get_pkt_endpoint( pkt, req );
    
    switch( req & JN_IPTraffic( FLAG_MASK )) {
    case JN_IPTraffic( FLAG_HOST ): return UINT_TO_JLONG((uint)endpoint->host.s_addr );
    }

    return (jlong)errlogargs();    
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    getIntValue
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL JF_IPTraffic( getIntValue )
  ( JNIEnv* env, jclass _class, jlong pkt_ptr, jint req )
{
    netcap_pkt_t* pkt;

    netcap_endpoint_t* endpoint   = NULL;

    /* XXX What happens on the long return */
    JLONG_TO_PACKET( pkt, pkt_ptr );

    

    if (( endpoint  = _get_pkt_endpoint( pkt, req )) == NULL ) return errlogargs();

    switch( req & JN_IPTraffic( FLAG_MASK )) {
    case JN_IPTraffic( FLAG_PORT ): return endpoint->port;
    case JN_IPTraffic( FLAG_TTL ): return pkt->ttl;
    case JN_IPTraffic( FLAG_TOS ): return pkt->tos;
    case JN_IPTraffic( FLAG_MARK_EN ): return pkt->is_marked;
    case JN_IPTraffic( FLAG_MARK ): return pkt->nfmark;
    case JN_IPTraffic( FLAG_INTERFACE ):
        if (( req & JN_IPTraffic( FLAG_SRC_MASK )) == JN_IPTraffic( FLAG_SRC )) return pkt->src_intf;
        return pkt->dst_intf;

    case JN_IPTraffic( FLAG_PROTOCOL ): return pkt->proto;
    }

    return errlogargs();
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    getStringValue
 * Signature: (JI)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL JF_IPTraffic( getStringValue )
  (JNIEnv* env, jclass _class, jlong pkt_ptr, jint req )
{
    netcap_pkt_t* pkt;
    char buf[NETCAP_MAX_IF_NAME_LEN]; /* XXX Update to the longest possible string returned */
    netcap_intf_t intf;

    JLONG_TO_PACKET_NULL( pkt, pkt_ptr );
    
    switch( req & JN_IPTraffic( FLAG_MASK )) {
    case JN_IPTraffic( FLAG_INTERFACE ): 
        intf = (( req & JN_IPTraffic( FLAG_SRC_MASK )) == JN_IPTraffic( FLAG_SRC )) ? pkt->src_intf : pkt->dst_intf;
        break;
    default: return errlogargs_null();
    }
    
    if ( intf == NF_INTF_UNKNOWN ) return (*env)->NewStringUTF( env, "" );
        
    if ( netcap_interface_intf_to_string( intf, buf, sizeof( buf )) < 0 ) {
        return errlog_null( ERR_CRITICAL, "netcap_intf_to_string\n" );
    }

    return (*env)->NewStringUTF( env, buf );
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    setLongValue
 * Signature: (JIJ)I
 */
JNIEXPORT jint JNICALL JF_IPTraffic( setLongValue )
  (JNIEnv* env, jclass _class, jlong pkt_ptr, jint req, jlong value )
{
    netcap_pkt_t* pkt;
    netcap_endpoint_t* endpoint;

    /* XXX What happens on the long return */
    JLONG_TO_PACKET( pkt, pkt_ptr );
    endpoint = _get_pkt_endpoint( pkt, req );

    switch( req & JN_IPTraffic( FLAG_MASK )) {
    case JN_IPTraffic( FLAG_HOST ): endpoint->host.s_addr = UINT_TO_JLONG(value); break;
    default:
        return errlogargs();
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    setIntValue
 * Signature: (JII)I
 */
JNIEXPORT jint JNICALL JF_IPTraffic( setIntValue )
  (JNIEnv* env, jclass _class, jlong pkt_ptr, jint req, jint value )
{
    netcap_pkt_t* pkt;
    netcap_endpoint_t* endpoint;

    JLONG_TO_PACKET( pkt, pkt_ptr );
    endpoint = _get_pkt_endpoint( pkt, req );

    switch( req & JN_IPTraffic( FLAG_MASK ) ) {
    case JN_IPTraffic( FLAG_PORT ): endpoint->port = value; break;
    case JN_IPTraffic( FLAG_TTL ):  pkt->ttl = value; break;
    case JN_IPTraffic( FLAG_TOS ):  pkt->tos = value; break;
    case JN_IPTraffic( FLAG_MARK_EN ): pkt->is_marked = value; break;
    case JN_IPTraffic( FLAG_MARK ): pkt->nfmark = value; break;
    case JN_IPTraffic( FLAG_INTERFACE ): 
        /* Verify that this is a valid interface */
        if ( netcap_interface_intf_verify( value ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_interface_intf_verify\n" );
        }
        if (( req & JN_IPTraffic( FLAG_SRC_MASK )) == JN_IPTraffic( FLAG_SRC )) {
            pkt->src_intf = value;
        } else {
            pkt->dst_intf = value; 
        }
        break;
    default:
        return errlogargs();        
    }

    return 0;
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    setStringValue
 * Signature: (JILjava/lang/String;)I
 */
JNIEXPORT jint JNICALL JF_IPTraffic( setStringValue )
  (JNIEnv* env, jclass _class, jlong pkt_ptr, jint req, jstring value )
{
    netcap_pkt_t* pkt;
    const char* str;
    netcap_intf_t* intf;
    int ret;

    JLONG_TO_PACKET( pkt, pkt_ptr );
    if (( req & JN_IPTraffic( FLAG_SRC_MASK )) == JN_IPTraffic( FLAG_SRC )) {
        intf = &pkt->src_intf;
    } else {
        intf = &pkt->dst_intf;
    }
    
    switch( req & JN_IPTraffic( FLAG_MASK ) ) {
    case JN_IPTraffic( FLAG_INTERFACE ): break;
    default: 
        return errlogargs();
    }
    
    if (( str = (*env)->GetStringUTFChars( env, value, NULL )) == NULL ) {
        return errlogmalloc();
    };
    
    do {
        if ( netcap_interface_string_to_intf( (char*)str, intf ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "netcap_string_to_intf\n" );
        }
    } while ( 0 );
    
    (*env)->ReleaseStringUTFChars( env, value, str );
    
    return ret;
}

/*
 * Class:     com_untangle_jnetcap_IPTraffic
 * Method:    raze
 * Signature: (J)V
 */
JNIEXPORT void JNICALL JF_IPTraffic( raze )
  (JNIEnv* env, jclass _class, jlong _pkt )
{
    netcap_pkt_t* pkt = (netcap_pkt_t*)JLONG_TO_ULONG(_pkt);
    if ( pkt == NULL ) return (void)errlogargs();

    /* Remove the packet */
    debug(10, "FLAG: IPTraffic.raze packet %d\n", pkt->packet_id);
        
    /* This will occasionally happen if a session is dropped after being accepted.  In this
     * case we must drop the packet */
    if ( pkt->packet_id != 0 ) debug( 3, "IPTraffic object with nonzero packet id.\n" );

    netcap_pkt_action_raze( pkt, NF_DROP );

    return;
}

/*
 * Class:     com_untangle_jnetcap_UDPSession
 * Method:    read
 * Signature: (JZI)J
 */
JNIEXPORT jlong JNICALL JF_UDPSession( read )
  (JNIEnv* env, jclass _class, jlong session_ptr, jboolean if_client, jint timeout )
{
    struct timeval tv;
    netcap_pkt_t *pkt = NULL;
    mailbox_t*    mb = NULL;
    netcap_session_t* netcap_sess = (netcap_session_t*)JLONG_TO_ULONG( session_ptr );
    if ( netcap_sess == NULL ) return UINT_TO_JLONG(errlogargs_null());

    if ( if_client == JNI_TRUE ) mb = &netcap_sess->cli_mb; 
    else                         mb = &netcap_sess->srv_mb; 
    
    if ( utime_msec_add_now( &tv, timeout ) < 0 ) {
        return UINT_TO_JLONG(errlog_null( ERR_CRITICAL, "utime_msec_add_now\n" ));
    }
    pkt = (netcap_pkt_t *) mailbox_utimed_get( mb, &tv );
    /*
    if (( pkt != NULL ) && ( pkt->packet_id != 0 )) {
        debug(10, "pulled intact UDP packet %d from mailbox, droping\n",pkt->packet_id);
        netcap_set_verdict( pkt->packet_id, NF_DROP, NULL, 0 );
        pkt->packet_id = 0;
    }
    */
    return  UINT_TO_JLONG( pkt );
}

/*
 * Class:     com_untangle_jnetcap_UDPSession
 * Method:    data
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL JF_UDPSession( data )
  (JNIEnv* env, jclass _class, jlong packet_ptr )
{
    netcap_pkt_t* pkt = (netcap_pkt_t*)JLONG_TO_ULONG( packet_ptr );
    jbyteArray bytes = NULL;
    
    if ( pkt == NULL ) return errlogargs_null();

    bytes = (*env)->NewByteArray( env, pkt->data_len );

    if ( bytes == NULL ) return errlogmalloc_null();

    if ( pkt->data_len > 0 ) {
        (*env)->SetByteArrayRegion( env, bytes, 0, pkt->data_len, (jbyte*)pkt->data );
    }
    
    return bytes;
}

/*
 * Class:     com_untangle_jnetcap_UDPSession
 * Method:    getData
 * Signature: (J)[B
 */
JNIEXPORT int JNICALL JF_UDPSession( getData )
  (JNIEnv* env, jclass _class, jlong packet_ptr, jbyteArray buffer )
{
    netcap_pkt_t* pkt = (netcap_pkt_t*)JLONG_TO_ULONG( packet_ptr );
    int buffer_len;

    if ( pkt == NULL || buffer == NULL ) return errlogargs();

    buffer_len = (*env)->GetArrayLength( env, buffer );
    if ( pkt->data_len > buffer_len )
        return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL,
                              "UDP: Buffer is to small for packet: %d < %d", buffer_len, pkt->data_len );


    if ( pkt->data_len > 0 ) {
        (*env)->SetByteArrayRegion( env, buffer, 0, pkt->data_len, (jbyte*)pkt->data );
    }
    
    return pkt->data_len;
}


/*
 * Class:     com_untangle_jnetcap_UDPSession
 * Method:    send
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL JF_UDPSession( send )
  (JNIEnv* env, jclass _class, jlong packet_ptr )
{
    netcap_pkt_t* pkt = (netcap_pkt_t*)JLONG_TO_ULONG( packet_ptr );
    if ( pkt == NULL ) return errlogargs();
    
    if ( pkt->data == NULL && pkt->data_len != 0 ) {
        return errlog( ERR_CRITICAL, "NULL packet, non-zero length\n" );
    }
    
    if ( pkt->data == NULL || pkt->data_len == 0 ) errlog( ERR_WARNING, "Sending a zero length packet\n" );

    if ( netcap_udp_send( (char*)pkt->data, pkt->data_len, pkt ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_udp_send\n" );
    }

    return 0;
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    merge
 * Signature: (JJJII)I
 */
JNIEXPORT jint JNICALL JF_UDPSession( merge )
  ( JNIEnv *env, jclass _class, jlong pointer, jlong src_addr, jint src_port, 
    jlong dst_addr, jint dst_port, jint intf )
{
    int ret;
    in_addr_t src = (in_addr_t)JLONG_TO_UINT( src_addr );
    in_addr_t dst = (in_addr_t)JLONG_TO_UINT( dst_addr );

    netcap_session_t* session = (netcap_session_t*)JLONG_TO_ULONG( pointer );

    if ( session == NULL ) return errlogargs();

    ret = netcap_sesstable_merge_udp_tuple( session, src, dst, src_port, dst_port, intf );

    if ( ret < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_sesstable_merge_udp\n" );
    } else if ( ret > 0 ) {
        return JN_UDPSession( MERGED_DEAD );
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    icmpMerge
 * Signature: (JJJII)I
 */
JNIEXPORT jint JNICALL JF_UDPSession( icmpMerge )
( JNIEnv *env, jclass _class, jlong pointer, jint icmp_pid, jlong src_addr, jlong dst_addr, jbyte intf )
{
    int ret;
    in_addr_t src = (in_addr_t)JLONG_TO_UINT( src_addr );
    in_addr_t dst = (in_addr_t)JLONG_TO_UINT( dst_addr );

    netcap_session_t* session = (netcap_session_t*)JLONG_TO_ULONG( pointer );

    if ( session == NULL ) return errlogargs();
    
    ret = netcap_sesstable_merge_icmp_tuple( session, src, dst, intf, icmp_pid );

    if ( ret < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_sesstable_merge_icmp_tuple\n" );
    } else if ( ret > 0 ) {
        return JN_UDPSession( MERGED_DEAD );
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    mailboxPointer
 * Signature: (JZ)I
 */
JNIEXPORT jlong JNICALL JF_UDPSession( mailboxPointer )
  ( JNIEnv *env, jclass _class, jlong pointer, jboolean if_client )
{
    netcap_session_t* session = (netcap_session_t*)JLONG_TO_ULONG( pointer );
    if ( session == NULL ) return errlogargs();

    return (jlong)(long)(( if_client == JNI_TRUE ) ? &session->cli_mb : &session->srv_mb);
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    liberate
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_UDPSession( liberate )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _udp_callback( session_ptr, LIBERATE, flags );
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    transferFirstPacketID
 * Signature: (JII)I
 */
JNIEXPORT void JNICALL JF_UDPSession( transferFirstPacketID )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jlong server_traffic_ptr )
{
    if ( session_ptr == 0 || server_traffic_ptr == 0 ) {
        return (void)jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid Arguments\n" );
    }

    netcap_session_t* session = (netcap_session_t*)JLONG_TO_ULONG( session_ptr );
    netcap_pkt_t* server_traffic = (netcap_pkt_t*)JLONG_TO_ULONG( server_traffic_ptr );

    debug( 10, "NetcapUDPSession: Transferring the first packet id\n" );

    if ( session->first_pkt_id == 0 ) {
        return (void)jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "First packet ID is zero\n" );
    }

    if ( server_traffic->packet_id != 0 ) {
        return (void)jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Packet ID is non-zero\n" );
    }

    /* Transfer the first packet id */
    server_traffic->packet_id = session->first_pkt_id;
    session->first_pkt_id = 0;
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    setSessionMark
 * Signature: (JJI)I
 */
JNIEXPORT void JNICALL JF_UDPSession( setSessionMark )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jlong server_traffic_ptr, jint mark )
{
    netcap_session_t* session = (netcap_session_t*)JLONG_TO_ULONG( session_ptr );
    netcap_pkt_t* server_traffic = (netcap_pkt_t*)JLONG_TO_ULONG( server_traffic_ptr );
    int packet_id = 0;
    
    if (server_traffic != NULL)
        packet_id = server_traffic->packet_id;
    
    /**
     * This updates the mark in the conntrack table directly
     * However if the first packet ID still exists then it hasn't been released
     * As such the conntrack entry doesn't exist yet so there is nothing to be done
     */
    if (session != NULL && session->first_pkt_id == 0 && packet_id == 0) 
        netcap_nfconntrack_update_mark( session, mark );
}

/*
 * Class:     com_untangle_jnetcap_NetcapUDPSession
 * Method:    serverComplete
 * Signature: (JI)I
 */
JNIEXPORT void JNICALL JF_UDPSession( serverComplete )
    ( JNIEnv *env, jclass _class, jlong session_ptr, jint flags )
{
    _udp_callback( session_ptr, SRV_COMPLETE, flags );
}

static void _udp_callback( jlong session_ptr, netcap_callback_action_t action, jint _flags )
{
    netcap_session_t* netcap_sess;
    int flags = 0;

    JLONG_TO_SESSION_VOID( netcap_sess, session_ptr );
    VERIFY_PKT_SESSION_VOID( netcap_sess );    
        
    if ( netcap_sess->callback == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "JPKT: null callback %d\n", action );
    }

    if ( netcap_sess->callback( netcap_sess, action, flags ) < 0 ) {
        debug( 2, "JPKT: callback failed=%d\n", action );

        /* Throw an error, but don't print an error message */
        jmvutil_error_throw( JMVUTIL_ERROR_STT, "JPKT: callback failed action=%d", action );
    }
}
