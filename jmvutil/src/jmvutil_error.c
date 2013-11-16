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

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "jmvutil.h"
#include "jmvutil_base.h"
#include "jmvutil_error.h"

#define _ILLEGAL_ARG_CLS_STR "java/lang/IllegalArgumentException"
#define _ILLEGAL_STT_CLS_STR "java/lang/IllegalStateException"

static struct {
    jclass illegal_arg;    /* An illegal argument exception */
    jclass illegal_stt;    /* An illegal state exception */
} _jerror = {
    .illegal_arg = NULL,
    .illegal_stt = NULL,
};

static jerror_tls_t* _tls_get( void );

int   _jmvutil_error_init( void )
{
    JNIEnv* env = jmvutil_get_java_env();
    jclass local;

    if ( env == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
    
    if (( local = (*env)->FindClass( env, _ILLEGAL_ARG_CLS_STR )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->FindClass\n" );
    }

    _jerror.illegal_arg = (*env)->NewGlobalRef( env, local );
    
    (*env)->DeleteLocalRef( env, local );
    
    if ( _jerror.illegal_arg == NULL ) return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );

    if (( local = (*env)->FindClass( env, _ILLEGAL_STT_CLS_STR )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->FindClass\n" );
    }

    _jerror.illegal_stt = (*env)->NewGlobalRef( env, local );
    
    (*env)->DeleteLocalRef( env, local );
    
    if ( _jerror.illegal_stt == NULL ) return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );

    return 0;
}

/** This just checks for an exception, but it will not clear it */
int   jmvutil_error_exception( void )
{
    JNIEnv* env = jmvutil_get_java_env();

    if ( env == NULL ) return errlogargs();

    if ((*env)->ExceptionCheck( env ) == JNI_TRUE ) return -1;
    return 0;
}

/** This checks for an exception, describes it and then clears it */
int   jmvutil_error_exception_clear( void )
{
    JNIEnv* env = jmvutil_get_java_env();

    if ( env == NULL ) return errlogargs();

    if ( jmvutil_error_exception() < 0 ) {
        (*env)->ExceptionDescribe( env );
        (*env)->ExceptionClear( env );
        return -1;
    }

    return 0;
}

char* jmvutil_error_throw( jmvutil_err_t type, const char* format, ... )
{
    jobject exception = NULL;
    va_list argptr;
    JNIEnv* env = jmvutil_get_java_env();    
    jerror_tls_t* _exception;

    if ( env == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        return (char*)format;
    }
    
    if (( _exception = _tls_get()) == NULL ) {
        errlog( ERR_CRITICAL, "_tls_get\n" );
        return (char*)format;
    }

    /* Only throw an exception if there is not a pending exception */
    if ( jmvutil_error_exception() < 0 ) {
        errlog( ERR_WARNING, "Exception pending\n" );
        return (char*)format;
    }
    
    switch ( type ) {
    case JMVUTIL_ERROR_ARGS: exception = _jerror.illegal_arg; break;
    case JMVUTIL_ERROR_STT:  exception = _jerror.illegal_stt; break;
    default:
        errlog( ERR_WARNING, "Invalid error type, defaulting to state(%d)\n", type );
        exception = _jerror.illegal_stt;
    }

    if ( exception == NULL ) {
        errlog ( ERR_CRITICAL, "jmvutil_error is unititalized\n" );
        return (char*)format;
    }

    _exception->current++;
    if ( _exception->current >= THROW_MSG_COUNT ) _exception->current = 0;
    
    va_start( argptr, format );

    if ( vsnprintf( _exception->buffer[_exception->current], THROW_MSG_MAX_LEN, format, argptr ) < 0 ) {
        (*env)->ThrowNew( env, exception, "mvutil error!!!" );
    } else {
        (*env)->ThrowNew( env, exception, (const char*)_exception->buffer[_exception->current] );
    }

    va_end( argptr );

    return (char*)format;
}

int _jmvutil_error_tls_init( jerror_tls_t* tls  )
{    
    tls->current = 0;

    return 0;
}

static jerror_tls_t* _tls_get( void )
{
    jmvutil_global_tls_t* tls;

    if (( tls = _jmvutil_tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_jmvutil_tls_get\n" );
    
    return &tls->error;
}
