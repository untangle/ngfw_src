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
#include JH_IncomingSocketQueue


/*
 * Class:     IncomingSocketQueue
 * Method:    create
 * Signature: ()I
 */
JNIEXPORT jint JNICALL JF_IncomingSocketQueue( create )
  (JNIEnv* env, jobject _this )
{
    jvector_sink_t* snk;
    mvpoll_key_t*   key;

    if (( key = socket_queue_key_create( _this )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "socket_queue_key_create\n" );
    }
    
    if (( snk = jvector_sink_create( _this, key )) == NULL ) {
        return (jint)errlog_null( ERR_CRITICAL, "jvector_sink_create\n" );
    }

    return (jint)snk;
}

/*
 * Class:     IncomingSocketQueue
 * Method:    mvpollKey
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_IncomingSocketQueue( mvpollKey )
  (JNIEnv* env, jclass _this, jint pointer )
{
    jvector_sink_t* jv_snk = (jvector_sink_t*)pointer;
    if ( jv_snk == NULL ) return (jint)errlogargs_null();
    
    return (jint)jv_snk->key;
}

