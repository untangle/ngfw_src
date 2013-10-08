/**
 * $Id: com_untangle_jvector_UDPSink.c 33807 2013-01-17 19:58:05Z dmorris $
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
      jint ttl, jint tos, jbyteArray options, jlong src_address )
{
    jbyte* data;
    int number_bytes = 0;
    int data_len;
    netcap_pkt_t* pkt = (netcap_pkt_t*)(uintptr_t)pointer;

    /* Save these values for later */
    int prev_ttl = pkt->ttl;
    int prev_tos = pkt->tos;

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
    
    /* If first packet hasn't been sent */
    if (( number_bytes = netcap_udp_send( (char*)data, data_len, pkt )) < 0 ) {
        perrlog( "netcap_udp_send" );
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



