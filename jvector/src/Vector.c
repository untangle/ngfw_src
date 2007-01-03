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

#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include <vector/vector.h>
#include <vector/sink.h>
#include <libvector.h>


#include "jni_header.h"

#include "jvector.h"

#include JH_Relay
#include JH_Vector


#if JN_Vector( ACTION_ERROR ) != _EVENT_ACTION_ERROR
#error ACTION_ERROR
#endif

#if JN_Vector( ACTION_NOTHING ) != _EVENT_ACTION_NOTHING
#error ACTION_NOTHING
#endif

#if JN_Vector( ACTION_DEQUEUE ) != _EVENT_ACTION_DEQUEUE
#error ACTION_DEQUEUE
#endif

#if JN_Vector( ACTION_SHUTDOWN ) != _EVENT_ACTION_SHUTDOWN
#error ACTION_SHUTDOWN
#endif

#if JN_Vector( MSG_SHUTDOWN ) != _VECTOR_MSG_SHUTDOWN
#error MSG_SHUTDOWN
#endif


/*
 * Class:     Vector
 * Method:    cLoad
 * Signature: ()I
 */
JNIEXPORT jint JNICALL JF_Vector( cLoad )
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
JNIEXPORT jint JNICALL JF_Vector( vector_1create )
    (JNIEnv* env, jobject _this, jint list_ptr)
{
    return (jint)vector_create((list_t*)list_ptr);
}

/*
 * Class:     Vector
 * Method:    vector_raze
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Vector( vector_1raze )
    (JNIEnv* env, jobject _this, jint vec_ptr)
{
    return (jint)vector_raze((vector_t*)vec_ptr);
}

/*
 * Class:     Vector
 * Method:    vector_send_msg
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL JF_Vector( vector_1send_1msg )
    ( JNIEnv* env, jobject _this, jint vec_ptr, jint vec_msg, jint arg )
{
    return (jint)vector_send_msg((vector_t*)vec_ptr,(vector_msg_t)vec_msg,(void*)arg);
}

/*
 * Class:     Vector
 * Method:    vector_set_timeout
 * Signature: (II)I
 */
JNIEXPORT void JNICALL JF_Vector( vector_1set_1timeout )
    (JNIEnv* env, jobject _this, jint vec_ptr, jint time)
{
    return vector_set_timeout((vector_t*)vec_ptr,(int)time);
}

/*
 * Class:     Vector
 * Method:    vector
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Vector( vector )
    ( JNIEnv* env, jobject _this, jint vec_ptr )
{
    return (jint)vector((vector_t*)vec_ptr);
}

/*
 * Class:     Vector
 * Method:    list_create
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Vector( list_1create ) 
    (JNIEnv* env, jobject _this, jint flags)
{
    return (jint)list_create((u_int)flags);
}

/*
 * Class:     Vector
 * Method:    list_add_tail
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL JF_Vector( list_1add_1tail )
    (JNIEnv* env, jobject _this, jint list_ptr, jint arg)
{
    return (jint)list_add_tail((list_t*)list_ptr,(void*)arg);
}

/*
 * Class:     Vector
 * Method:    list_raze
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Vector( list_1raze )
    (JNIEnv* env, jobject _this, jint list_ptr)
{
    return (jint)list_raze((list_t*)list_ptr);
}

/*
 * Class:     Vector
 * Method:    debugLevel
 * Signature: (I)I
 */
JNIEXPORT void JNICALL JF_Vector( debugLevel )
  (JNIEnv* env, jclass _class, jint type, jint level )
{
    if ( level < 0 ) {
        errlog( ERR_WARNING, "Invalid debug level: %d", level );
        return;
    }

    switch ( type ) {
    case JN_Vector( JVECTOR_DEBUG ):
        debug_set_mylevel( level );
        break;

    case JN_Vector( MVUTIL_DEBUG ):
        debug_set_level( UTIL_DEBUG_PKG, level );
        break;

    case JN_Vector( VECTOR_DEBUG ):
        debug_set_level( VECTOR_DEBUG_PKG, level );
        break;
        
    default: errlog( ERR_WARNING, "Invalid type: %d\n", type );
    }
    
    return;
}

