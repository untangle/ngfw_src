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

#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include <vector/vector.h>
#include <vector/sink.h>
#include <libvector.h>



#include "jvector.h"

#include "com_untangle_jvector_Relay.h"
#include "com_untangle_jvector_Vector.h"


#if com_untangle_jvector_Vector_ACTION_ERROR != _EVENT_ACTION_ERROR
#error ACTION_ERROR
#endif

#if com_untangle_jvector_Vector_ACTION_NOTHING != _EVENT_ACTION_NOTHING
#error ACTION_NOTHING
#endif

#if com_untangle_jvector_Vector_ACTION_DEQUEUE != _EVENT_ACTION_DEQUEUE
#error ACTION_DEQUEUE
#endif

#if com_untangle_jvector_Vector_ACTION_SHUTDOWN != _EVENT_ACTION_SHUTDOWN
#error ACTION_SHUTDOWN
#endif

#if com_untangle_jvector_Vector_MSG_SHUTDOWN != _VECTOR_MSG_SHUTDOWN
#error MSG_SHUTDOWN
#endif


/*
 * Class:     Vector
 * Method:    cLoad
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_Vector_cLoad
  ( JNIEnv *env, jclass _class )
{    
    if ( jvector_load( env ) < 0 ) return errlog( ERR_CRITICAL, "jvector_load\n" );
    
    return 0;
}

/*
 * Class:     Vector
 * Method:    vector_create
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_Vector_vector_1create
    (JNIEnv* env, jobject _this, jlong list_ptr)
{
    return (uintptr_t)vector_create((list_t*)(uintptr_t)list_ptr);
}

/*
 * Class:     Vector
 * Method:    vector_raze
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_Vector_vector_1raze
    (JNIEnv* env, jobject _this, jlong vec_ptr)
{
    return (uintptr_t)vector_raze((vector_t*)(uintptr_t)vec_ptr);
}

/*
 * Class:     Vector
 * Method:    vector_send_msg
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_Vector_vector_1send_1msg
    ( JNIEnv* env, jobject _this, jlong vec_ptr, jint vec_msg, jlong arg )
{
    return (uintptr_t)vector_send_msg((vector_t*)(uintptr_t)vec_ptr,(vector_msg_t)vec_msg,(void*)(uintptr_t)arg);
}

/*
 * Class:     Vector
 * Method:    vector_set_timeout
 * Signature: (II)I
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_Vector_vector_1set_1timeout
    (JNIEnv* env, jobject _this, jlong vec_ptr, jint time)
{
    return vector_set_timeout((vector_t*)(uintptr_t)vec_ptr,(int)time);
}

/*
 * Class:     Vector
 * Method:    vector
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_Vector_vector
    ( JNIEnv* env, jobject _this, jlong vec_ptr )
{
    return (jint)vector((vector_t*)(uintptr_t)vec_ptr);
}

/*
 * Class:     Vector
 * Method:    list_create
 * Signature: (I)I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_Vector_list_1create 
    (JNIEnv* env, jobject _this, jint flags)
{
    return (uintptr_t)list_create((u_int)flags);
}

/*
 * Class:     Vector
 * Method:    list_add_tail
 * Signature: (II)I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_Vector_list_1add_1tail
    (JNIEnv* env, jobject _this, jlong list_ptr, jlong arg)
{
    return (uintptr_t)list_add_tail((list_t*)(uintptr_t)list_ptr,(void*)(uintptr_t)arg);
}

/*
 * Class:     Vector
 * Method:    list_raze
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_untangle_jvector_Vector_list_1raze
    (JNIEnv* env, jobject _this, jlong list_ptr)
{
    return (jint)list_raze((list_t*)(uintptr_t)list_ptr);
}

/*
 * Class:     Vector
 * Method:    debugLevel
 * Signature: (I)I
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_Vector_debugLevel
  (JNIEnv* env, jclass _class, jint type, jint level )
{
    if ( level < 0 ) {
        errlog( ERR_WARNING, "Invalid debug level: %d", level );
        return;
    }

    switch ( type ) {
    case com_untangle_jvector_Vector_JVECTOR_DEBUG:
        debug_set_mylevel( level );
        break;

    case com_untangle_jvector_Vector_MVUTIL_DEBUG:
        debug_set_level( UTIL_DEBUG_PKG, level );
        break;

    case com_untangle_jvector_Vector_VECTOR_DEBUG:
        debug_set_level( VECTOR_DEBUG_PKG, level );
        break;
        
    default: errlog( ERR_WARNING, "Invalid type: %d\n", type );
    }
    
    return;
}

