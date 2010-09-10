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
#include <stdlib.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

#include <netinet/ip.h>
#include <netinet/udp.h>

#include <linux/netfilter.h>

#include <libmvutil.h>
#include <libvector.h>
#include <libnetcap.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/mvpoll.h>
#include <mvutil/unet.h>
#include <jmvutil.h>

#include <vector/event.h>
#include <vector/source.h>

#include "jvector.h"


#include "com_untangle_jvector_UDPSink.h"


/**
 * UDP Poll is always writeable 
 */
static eventmask_t   _poll ( mvpoll_key_t* key );

/**
 * Accept the first packet that is in the session.
 */
static int _accept_packet( char* data, int data_len, netcap_pkt_t* pkt );

/*
 * Class:     UDPSink
 * Method:    create
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_UDPSink_create
( JNIEnv *env, jobject _this, jlong pointer )
{
    jvector_sink_t* snk;
    mvpoll_key_t* key;
    

    /* XXX What is this about */
    if (( key = malloc( sizeof( mvpoll_key_t ))) == NULL ) return (uintptr_t)errlogmalloc_null();

    if ( mvpoll_key_base_init( key ) < 0 ) return (uintptr_t)errlog_null( ERR_CRITICAL, "mvpoll_key_base_init\n" );

    key->type            = JV_UDP_KEY_TYPE;
    key->arg             = NULL;
    key->special_destroy = NULL;
    key->data.ptr        = NULL;
    key->poll            = _poll;
    
    if (( snk = jvector_sink_create( _this, key )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "jvector_sink_create\n" );
    }
    
    return (uintptr_t)snk;    
}

JNIEXPORT jint JNICALL Java_com_untangle_jvector_UDPSink_write
    ( JNIEnv *env, jobject _this, jlong pointer, jbyteArray _data, jint offset, jint size,
      jint ttl, jint tos, jbyteArray options, jboolean is_udp, jlong src_address )
{
    jbyte* data;
    int number_bytes = 0;
    int data_len;
    netcap_pkt_t* pkt = (netcap_pkt_t*)(uintptr_t)pointer;

    /* Save these values for later */
    int prev_ttl = pkt->ttl;
    int prev_tos = pkt->tos;

    /* XXX options */
    
    
    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) return errlogmalloc();
    
    data_len = (*env)->GetArrayLength( env, _data );
    
    if ( size > data_len ) {
        return errlog( ERR_WARNING, "Requested %d write with a buffer of size %d\n", size, data_len );
    } else if ( offset > size ) {
        return errlog( ERR_WARNING, "Requested %d offset with a buffer of size %d\n", offset, size );
    }  else { 
        data_len = size - offset;
    }

    /* Once these values have changed make sure not to return before fixing them */
    if ( ttl != com_untangle_jvector_UDPSink_DISABLED) pkt->ttl = ttl;
    if ( tos != com_untangle_jvector_UDPSink_DISABLED) pkt->tos = tos;
    
    /* XXX options */
    if  ( is_udp == JNI_TRUE ) {
        if ( pkt->packet_id != 0 ) {
            if (( number_bytes = _accept_packet( (char*)data, data_len, pkt )) < 0 ) {
                errlog( ERR_CRITICAL, "_accept_packet" );
            }
        } else {
            if (( number_bytes = netcap_udp_send( (char*)data, data_len, pkt )) < 0 ) {
                perrlog( "netcap_udp_send" );
            }
        }
    } else {
        in_addr_t prev_addr = pkt->src.host.s_addr;
        if ( src_address != 0 ) {
            /* Some ICMP packets may come from a different source, eg timeout expired */
            pkt->src.host.s_addr = (in_addr_t)( src_address & 0xFFFFFFFF );
        }
        
        if (( number_bytes = netcap_icmp_send( (char*)data, data_len, pkt )) < 0 ) {
            perrlog( "netcap_icmp_send" );
        }
        
        /* Return to the original address */
        pkt->src.host.s_addr = prev_addr;
    }

    (*env)->ReleaseByteArrayElements( env, _data, data, 0 );

    /* Set the values back */
    pkt->opts     = NULL;
    pkt->opts_len = 0;
    pkt->ttl      = prev_ttl;
    pkt->tos      = prev_tos;

    return number_bytes;
}

/*
 * Class:     UDPSink
 * Method:    shutdown
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_UDPSink_shutdown
  (JNIEnv *env, jclass _this, jlong pointer)
{
    /* Not much to do here */
    return 0;
}

/* Always writeable, never readable */
static eventmask_t   _poll ( mvpoll_key_t* key )
{
    return MVPOLLOUT;
}

/**
 * Accept the first packet that is in the session.
 */
static int _accept_packet( char* data, int data_len, netcap_pkt_t* pkt )
{
    int packet_id = pkt->packet_id;
    pkt->packet_id = 0;

    struct iphdr* ip_header = NULL;
    struct udphdr* udp_header = NULL;

    u_int16_t ip_len = sizeof( struct iphdr ) + sizeof( struct udphdr ) + data_len;

    int _critical_section() {
        udp_header = (struct udphdr*)(((char*)ip_header) + sizeof( struct iphdr ));
        
        /* construct a zero length packet */
        /* This is the size in words */
        ip_header->ihl = sizeof( struct iphdr ) / 4;
        ip_header->version = 4;
        ip_header->tos = 0;
        ip_header->tot_len = htons( ip_len );
        /* xxxx this is bad */
        ip_header->id = (u_int16_t)(packet_id * 67343 % 0x10000);
        ip_header->frag_off = 0;
        
        ip_header->ttl = pkt->ttl - 1;
        
        ip_header->protocol = IPPROTO_UDP;
        ip_header->check = 0;
        ip_header->saddr = pkt->src.host.s_addr;
        ip_header->daddr = pkt->dst.host.s_addr;
        
        /* Recalculate the checksum */
        ip_header->check = unet_in_cksum((u_int16_t *)ip_header, sizeof( struct iphdr ));
        
        /* Fill in the UDP packet */
        u_int16_t udp_len = sizeof( struct udphdr ) + data_len;
        udp_header->source = (u_int16_t)htons( pkt->src.port );
        udp_header->dest = (u_int16_t)htons( pkt->dst.port );
        udp_header->len = htons( udp_len );
        udp_header->check = 0;
        /* Copy in the data */
        memcpy( udp_header + 1, data, data_len );

        udp_header->check = unet_udp_sum_calc( udp_len, (u_int8_t*)&ip_header->saddr, (u_int8_t*)&ip_header->daddr, (u_int8_t*)udp_header );
        
        debug( 10, "UDPSink: Sending packet using queue\n" );
        
        if ( netcap_set_verdict( packet_id, NF_ACCEPT, (u_char*)ip_header, ip_len )) {
            return errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
        }

        return 0;
    }

    if (( ip_header = malloc( ip_len )) == NULL ) {
        return errlog( ERR_CRITICAL, "UDP SESSION: malloc.\n" );
    }
    int ret = 0;
    
    if (( ret = _critical_section()) < 0 ) {
        errlog( ERR_CRITICAL, "_critical_section\n" );
    }

    free( ip_header );
    
    return ret;
}


