#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "jmvutil.h"
#include "jerror.h"

#define _ILLEGAL_ARG_CLS_STR "java/lang/IllegalArgumentException"
#define _ILLEGAL_STT_CLS_STR "java/lang/IllegalStateException"

static struct {
    jclass illegal_arg;    /* An illegal argument exception */
    jclass illegal_stt;    /* An illegal state exception */
    pthread_key_t  tls;    /* Key to the thread local storage */
} _jerror = {
    .illegal_arg NULL,
    .illegal_stt NULL,
    .tls         -1
};

#define THROW_MSG_MAX_LEN 64

typedef struct {
    int current;
    char buffer[THROW_MSG_COUNT][THROW_MSG_MAX_LEN];
} tls_jerror_t;

/* Initializer for thread local storage (only run once when TLS is allocated) */
static int _tls_init( void* buf, size_t size );

int   jmvutil_error_init( void )
{
    JNIEnv* env = jmvutil_get_java_env();
    jclass local;

    if ( env == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
    
    /* Allocate the TLS key */
    if ( pthread_key_create( &_jerror.tls, uthread_tls_free ) < 0 ) {
        return errlog( ERR_CRITICAL, "pthread_key_create\n" );
    }

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
int   jmvutil_exception( void )
{
    JNIEnv* env = jmvutil_get_java_env();

    if ( env == NULL ) return errlogargs();

    if ((*env)->ExceptionCheck( env ) == JNI_TRUE ) return -1;
    return 0;
}

/** This checks for an exception, describes it and then clears it */
int   jmvutil_exception_clear( void )
{
    JNIEnv* env = jmvutil_get_java_env();

    if ( env == NULL ) return errlogargs();

    if ( jmvutil_exception() < 0 ) {
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
    tls_jerror_t* _exception;

    if ( env == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        return (char*)format;
    }
    
    if (( _exception = uthread_tls_get( _jerror.tls, sizeof( tls_jerror_t ), _tls_init )) == NULL ) {
        errlog( ERR_CRITICAL, "uthread_get_tls\n" );
        return (char*)format;
    }

    /* Only throw an exception if there is not a pending exception */
    if ( jmvutil_exception() < 0 ) {
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
        (*env)->ThrowNew( env, exception, "Netcap error!!!" );
    } else {
        (*env)->ThrowNew( env, exception, (const char*)_exception->buffer[_exception->current] );
    }

    va_end( argptr );

    return (char*)format;
}

static int _tls_init( void* buf, size_t size )
{
    tls_jerror_t* _exception;

    if ( size != sizeof( tls_jerror_t )) return errlogargs();
    
    _exception = buf;
    
    _exception->current = 0;

    return 0;
}
