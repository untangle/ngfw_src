/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <libmvutil.h>
#include <libvector.h>
#include <mvutil/debug.h>
#include <mvutil/mvpoll.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <vector/event.h>
#include <vector/source.h>
#include <vector/sink.h>

#include "jvector.h"

#include "jni_header.h"

#include JH_SocketQueue
#include JH_OutgoingSocketQueue

/*
 * Class:     OutgoingSocketQueue
 * Method:    create
 * Signature: ()I
 */
JNIEXPORT jint JNICALL JF_OutgoingSocketQueue( create )
  (JNIEnv* env, jobject _this )
{
    jvector_source_t* src;
    mvpoll_key_t* key;

    if (( key = socket_queue_key_create( _this )) == NULL ) { 
        return (jint)errlog_null( ERR_CRITICAL, "socket_queue_create\n" );   
    }
        
    if (( src = jvector_source_create( _this, key )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "jvector_source_create\n" );
    }

    return (jint)src;
}

/*
 * Class:     OutgoingSocketQueue
 * Method:    mvpollKey
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_OutgoingSocketQueue( mvpollKey )
  (JNIEnv* env, jclass _this, jint pointer )
{
    jvector_source_t* jv_src = (jvector_source_t*)pointer;
    if ( jv_src == NULL ) return (jint)errlogargs_null();
    
    return (jint)jv_src->key;
}
