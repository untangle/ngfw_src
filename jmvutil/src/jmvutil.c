/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "jmvutil.h"
#include "jerror.h"

static struct {
    int mvutil;
    int jmvutil;
    pthread_mutex_t mutex;
    JavaVM* jvm;
} _jmvutil = 
{
    .mvutil    _UNINITIALIZED,
    .jmvutil   _UNINITIALIZED,
    .mutex     PTHREAD_MUTEX_INITIALIZER,
    .jvm       NULL
};

int jmvutil_init       ( void )
{
    int ret = 0;
    jclass local;
    
    if ( pthread_mutex_lock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );

    do {
        if ( _jmvutil.jvm == NULL ) { 
            if ( (*env)->GetJavaVM( env, &_jmvutil.jvm ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "GetJavaVM\n" );
                break;
            }
        } 

        if ( _jmvutil.mvutil == _UNINITIALIZED ) {
            if ( libmvutil_init() < 0 ) {
                ret = errlog ( ERR_CRITICAL, "libmvutil_init\n" );
                break;
            }
            
            /* Set the initialization flag */
            _jmvutil.mvutil = _INITIALIZED;
        }
        
        if ( _jmvutil.jmvutil != _INITIALIZED ) {
        }
        
        _jmvutil.jmvutil = _INITIALIZED;
        
    } while ( 0 );
    
    if ( pthread_mutex_unlock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );
    
    return ret;
}

JavaVM* jmvutil_get_java_vm ( void )
{
    if ( _jmvutil.jvm == NULL ) {
        jsize num_jvms;
        if ( JNI_GetCreatedJavaVMs( &_jmvutil.jvm, 1, &num_jvms ) < 0 ) {
            return errlog_null( ERR_CRITICAL, "JNI_GetCreatedJavaVMs\n" );
        }
        if ( num_jvms > 1 ) return errlog_null( ERR_CRITICAL, "MULTIPLE JVMS\n" );
    }

    return _jmvutil.jvm;
}

JNIEnv* jmvutil_get_java_env( void )
{
    static __thread JNIEnv* env = NULL;
    int res  = 0;
    JavaVM* jvm;
    
    if (( jvm = jmvutil_get_java_vm()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "jmvutil_get_java_vm\n" );
    }

    if ( env == NULL ) {
#ifdef JNI_VERSION_1_2
        res = (*jvm)->AttachCurrentThread( jvm, (void**)&env, NULL );
#else
        res = (*jvm)->AttachCurrentThread( jvm, &env, NULL );
#endif // JNI_VERSION_1_2
        
        if ( res < 0 ) return errlog_null( ERR_CRITICAL, "AttachCurrentThread\n" );
    }

    return env;
}
