/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
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

#include <jmvutil.h>

#include "jvector.h"


#include "com_untangle_jvector_OutgoingSocketQueue.h"

/*
 * Class:     OutgoingSocketQueue
 * Method:    create
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_OutgoingSocketQueue_create
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
 * Method:    mvpollNotifyObservers
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_OutgoingSocketQueue_mvpollNotifyObservers
  (JNIEnv* env, jobject _this, jint pointer, jint eventmask )
{
    jvector_source_t* jv_src = (jvector_source_t*)pointer;

    if (( jv_src == NULL ) || ( jv_src->key == NULL )) {
        return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL sink or key" );
    }
    
    mvpoll_key_notify_observers( jv_src->key, (eventmask_t)eventmask );
}
