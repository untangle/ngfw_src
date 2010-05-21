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
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_OutgoingSocketQueue_create
  (JNIEnv* env, jobject _this )
{
    jvector_source_t* src;
    mvpoll_key_t* key;

    if (( key = socket_queue_key_create( _this )) == NULL ) { 
        return (uintptr_t)errlog_null( ERR_CRITICAL, "socket_queue_create\n" );   
    }
        
    if (( src = jvector_source_create( _this, key )) == NULL ) {
        return (uintptr_t)errlog_null( ERR_CRITICAL, "jvector_source_create\n" );
    }

    return (uintptr_t)src;
}

/*
 * Class:     OutgoingSocketQueue
 * Method:    mvpollNotifyObservers
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_OutgoingSocketQueue_mvpollNotifyObservers
  (JNIEnv* env, jobject _this, jlong pointer, jint eventmask )
{
    jvector_source_t* jv_src = (jvector_source_t*)(uintptr_t)pointer;

    if (( jv_src == NULL ) || ( jv_src->key == NULL )) {
        return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL sink or key" );
    }
    
    mvpoll_key_notify_observers( jv_src->key, (eventmask_t)eventmask );
}
