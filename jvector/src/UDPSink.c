/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPSink.c,v 1.4 2005/01/27 04:56:29 rbscott Exp $
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/socket.h>
#include <unistd.h>

#include <libmvutil.h>
#include <libvector.h>
#include <libnetcap.h>

#include <mvutil/debug.h>
#include <mvutil/mvpoll.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <vector/event.h>
#include <vector/source.h>

#include "jni_header.h"
#include "jvector.h"

#include JH_UDPSink


/**
 * UDP Poll is always writeable 
 */
static eventmask_t   _poll ( mvpoll_key_t* key );

/*
 * Class:     UDPSink
 * Method:    create
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_UDPSink( create )
( JNIEnv *env, jobject _this, jint pointer )
{
    jvector_sink_t* snk;
    mvpoll_key_t* key;
    

    /* XXX What is this about */
    if (( key = malloc( sizeof( mvpoll_key_t ))) == NULL ) return (jint)errlogmalloc_null();

    if ( mvpoll_key_base_init( key ) < 0 ) return (jint)errlog( ERR_CRITICAL, "mvpoll_key_base_init\n" );

    key->type            = JV_UDP_KEY_TYPE;
    key->arg             = NULL;
    key->special_destroy = NULL;
    key->data            = NULL;
    key->poll            = _poll;
    
    if (( snk = jvector_sink_create( _this, key )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "jvector_sink_create\n" );
    }
    
    return (jint)snk;    
}

JNIEXPORT jint JNICALL JF_UDPSink( write )
    ( JNIEnv *env, jobject _this, jint pointer, jbyteArray _data, jint offset, jint size,
      jint ttl, jint tos, jbyteArray options )
{
    jbyte* data;
    int number_bytes = 0;
    int data_len;
    netcap_pkt_t* pkt = (netcap_pkt_t*)pointer;

    /* Save these values for later */
    int prev_ttl = pkt->ttl;
    int prev_tos = pkt->tos;

    /* XXX options */
    

    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) return errlogmalloc();
    
    data_len = (*env)->GetArrayLength( env, _data );
    
    if ( size > data_len ) {
        /* XXX Should these errors return */
        errlog( ERR_WARNING, "Requested %d write with a buffer of size %d\n", size, data_len );
    } else if ( offset > size ) {
        errlog( ERR_WARNING, "Requesetd %d offset with a buffer of size %d\n", offset, size );
    }  else { 
        data_len = size - offset;
    }

    if ( ttl != JN_UDPSink( DISABLED )) pkt->ttl = ttl;
    if ( tos != JN_UDPSink( DISABLED )) pkt->tos = tos;

    /* XXX options */

    if (( number_bytes = netcap_udp_send( data, data_len, pkt )) < 0 ) perrlog( "netcap_udp_send" );

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
JNIEXPORT jint JNICALL JF_UDPSink( shutdown )
  (JNIEnv *env, jclass _this, jint pointer)
{
    /* Not much to do here */
    return 0;
}

/* Always writeable, never readable */
static eventmask_t   _poll ( mvpoll_key_t* key )
{
    return MVPOLLOUT;
}

