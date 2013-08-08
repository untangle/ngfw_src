/**
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

#include "com_untangle_jvector_IncomingSocketQueue.h"

/*
 * Class:     IncomingSocketQueue
 * Method:    create
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_IncomingSocketQueue_create
  (JNIEnv* env, jobject _this )
{
    jvector_sink_t* snk;
    mvpoll_key_t*   key;

    if (( key = socket_queue_key_create( _this )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "socket_queue_key_create\n" );
    }
    
    if (( snk = jvector_sink_create( _this, key )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "jvector_sink_create\n" );
    }

    return (uintptr_t)snk;
}

/*
 * Class:     IncomingSocketQueue
 * Method:    mvpollNotifyObservers
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_IncomingSocketQueue_mvpollNotifyObservers
  (JNIEnv* env, jclass _this, jlong pointer, jint eventmask )
{
    jvector_sink_t* jv_snk = (jvector_sink_t*)(uintptr_t)pointer;
    
    if (( jv_snk == NULL ) || ( jv_snk->key == NULL )) {
        return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL sink or key\n" );
    }
    
    mvpoll_key_notify_observers( jv_snk->key, (eventmask_t)eventmask );
}

