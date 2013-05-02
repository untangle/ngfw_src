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

#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>

#include "jmvutil.h"
#include "jmvutil_base.h"
#include "jmvutil_error.h"

static struct {
    int mvutil;
    int jmvutil;
    pthread_mutex_t mutex;
    pthread_key_t   tls_key;
    JavaVM* jvm;
} _jmvutil = 
{
    .mvutil    = JMVUTIL_UNINITIALIZED,
    .jmvutil   = JMVUTIL_UNINITIALIZED,
    .mutex     = PTHREAD_MUTEX_INITIALIZER,
    .jvm       = NULL,
    .tls_key   = -1
};

static jmvutil_tls_t* _tls_get ( void );
static int            _tls_init( void* buf, size_t size );


int jmvutil_init       ( void )
{
    int ret = 0;
    
    if ( pthread_mutex_lock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );

    do {
        if ( _jmvutil.jvm == NULL ) { 
            if ( jmvutil_get_java_vm() == NULL ) {
                ret = errlog( ERR_CRITICAL, "GetJavaVM\n" );
                break;
            }
        } 

        if ( _jmvutil.mvutil == JMVUTIL_UNINITIALIZED ) {
            if ( libmvutil_init() < 0 ) {
                ret = errlog ( ERR_CRITICAL, "libmvutil_init\n" );
                break;
            }            
        }
        
        /* Set the initialization flag */
        _jmvutil.mvutil = JMVUTIL_INITIALIZED;
        
        if ( _jmvutil.jmvutil == JMVUTIL_UNINITIALIZED ) {
            /* Allocate the TLS key */
            if ( pthread_key_create( &_jmvutil.tls_key, uthread_tls_free ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "pthread_key_create\n" );
                break;
            }
            
            if ( _jmvutil_error_init() < 0 ) {
                ret = errlog( ERR_CRITICAL, "_jmvutil_error_init\n" );
                break;
            }
        }
        
        _jmvutil.jmvutil = JMVUTIL_INITIALIZED;
    } while ( 0 );
    
    if ( pthread_mutex_unlock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );
    
    return ret;
}

int     jmvutil_cleanup     ( void )
{
    if ( pthread_mutex_lock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );
    
    if ( _jmvutil.mvutil == JMVUTIL_INITIALIZED ) {
        libmvutil_cleanup();
    }
    
    /* XXX Insert any cleanup for jmvutil here */    
    _jmvutil.mvutil = 0;
    
    if ( pthread_mutex_unlock ( &_jmvutil.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    return 0;
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
    int res  = 0;
    JavaVM* jvm;
    jmvutil_tls_t* tls;
    
    if (( jvm = jmvutil_get_java_vm()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "jmvutil_get_java_vm\n" );
    }
    
    if (( tls = _tls_get()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "_tls_get\n" );
    }

    if ( tls->env == NULL ) {
        res = (*jvm)->AttachCurrentThread( jvm, (void**)&tls->env, NULL );
        
        if ( res < 0 )
            return errlog_null( ERR_CRITICAL, "AttachCurrentThread\n" );
    }

    return tls->env;
}

jmvutil_global_tls_t* _jmvutil_tls_get( void )
{
    jmvutil_global_tls_t* tls;
    if (( tls = uthread_tls_get( _jmvutil.tls_key, sizeof( jmvutil_global_tls_t ), _tls_init )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "uthread_get_tls\n" );
    }

    return tls;
}

static jmvutil_tls_t* _tls_get ( void )
{
    jmvutil_global_tls_t* tls;
    
    if (( tls = _jmvutil_tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_jmvutil_tls_get\n" );
    
    return &tls->base;
}

static int            _tls_init( void* buf, size_t size )
{
    JavaVM* jvm;
    int ret;
    jmvutil_global_tls_t* tls = buf;

    if (( size != sizeof( jmvutil_global_tls_t )) || ( buf == NULL )) return errlogargs();

    if (( jvm = jmvutil_get_java_vm() ) == NULL ) {
        return errlog( ERR_CRITICAL, "jmvutil_get_java_vm\n" );
    }

    ret = (*jvm)->AttachCurrentThread( jvm, (void**)&tls->base.env, NULL );

    if ( ret < 0 )
        return errlog( ERR_CRITICAL, "AttachCurrentThread: 0x%08x\n", ret );
    
    if ( _jmvutil_error_tls_init( &tls->error ) < 0 ) 
        return errlog( ERR_CRITICAL, "_jmvutil_error_tls_init\n" );

    return 0;
}
