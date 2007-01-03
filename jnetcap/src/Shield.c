/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

#include <jni.h>

#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <fcntl.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include JH_Shield

// There is also a value for this inside of netcap_shield.c, make sure they are the same conflict.
#define BLESS_COUNT_MAX          128

#define _SHIELD_OBJ_STR          JP_BUILD_NAME( Shield )
#define _SHIELD_METHOD_REJ_NAME  "callRejectionEventListener"
#define _SHIELD_METHOD_REJ_DESC  "(JBDIIII)V"

#define _SHIELD_METHOD_STAT_NAME "callStatisticEventListener"
#define _SHIELD_METHOD_STAT_DESC "(IIIIIIII)V"

static struct
{
    int       call_hook;
    jclass    class;
    jmethodID call_listener_rejection_mid;
    jmethodID call_listener_statistic_mid;
    jobject   object;
} _shield = {
    .call_hook = 0,
    .class     = NULL,
    .call_listener_rejection_mid = NULL,
    .call_listener_statistic_mid = NULL,
    .object = NULL
};

static void _event_hook ( netcap_shield_event_data_t* data );

/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    config
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL JF_Shield( config )
    ( JNIEnv* env, jobject _this, jstring file_name )
{
    const char* file_str;

#ifdef _MCD_CHECK
    /*XXX This is only valid for the MCD checker */
    /* Dump out the memory logs */
    showMemStats( 0 );
#endif
    
    if (( file_str = (*env)->GetStringUTFChars( env, file_name, NULL )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    };

    debug( 5, "JNETCAP: Loading shield configuration file: %s\n", file_str );
    
    do {
        struct stat file_stat;

        if ( stat( file_str, &file_stat ) < 0 ) {
            jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "stat: %s", errstr );
            break;
        }

        if ( access( file_str, R_OK ) == 0 && S_ISREG( file_stat.st_mode )) {
            int fd;
            char buf[4096];
            int msg_len;

            /* Open and read the configuration file */
            if (( fd = open( file_str, O_RDONLY )) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "open: %s", errstr );
                break;
            }
            
            if (( msg_len = read( fd, buf, sizeof( buf ))) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "read: %s", errstr );                
                if ( close( fd ) < 0 )
                    perrlog( "close" );
                break;
            }
            
            /* Don't stop if there is an error, the data has already been read */
            if ( close( fd ) < 0 ) 
                perrlog( "close" );
            
            fd = -1;
            
            if ( msg_len == sizeof ( buf )) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Invalid shield configuration(size>=%d)\n", 
                               sizeof ( buf ));
                continue;
            }
                        
            /* Load the shield configuration */
            if ( msg_len != 0 && netcap_shield_cfg_load ( buf, msg_len ) < 0 ) {
                jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_load_configuration\n" );
                break;
            }
            
            debug( 5, "JNETCAP: Successfully loaded shield configuration\n" );
        } else {
            jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Unable to access file: '%s'", file_str );
        }
    } while ( 0 );

    (*env)->ReleaseStringUTFChars( env, file_name, file_str );
}

/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    dump
 * Signature: (LI);
 */
JNIEXPORT void JNICALL JF_Shield( status )
  (JNIEnv *env, jobject _this, jlong ip, jint port )
{
    struct sockaddr_in dst;
    int fd;
    
    memcpy( &dst.sin_addr, &ip, sizeof( struct in_addr ));
    dst.sin_port = htons((u_short)port );
    dst.sin_family = AF_INET;
    
    if (( fd = socket( AF_INET, SOCK_DGRAM, IPPROTO_UDP )) < 0 ) 
    {
        perrlog( "socket" );
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to open a UDP socket\n" );
    }

    do {
        if ( netcap_shield_status( fd, &dst ) < 0 ) {
            jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_status\n" );
            break;
        }
    } while ( 0 );
    
    if ( close( fd ) < 0 )
        perrlog( "close" );
}


/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    addChunk
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL JF_Shield( addChunk )
  (JNIEnv *env, jobject _this, jlong ip, jshort protocol, jint num_bytes )
{
    struct in_addr address = { .s_addr = (in_addr_t)JLONG_TO_UINT( ip ) };
    /* Could throw an error, but shield errors are currently ignored. */
    if ( netcap_shield_rep_add_chunk( &address, protocol, (u_short)num_bytes ) < 0 )
        errlog( ERR_WARNING, "netcap_shield_rep_add_chunk\n" );  
}

/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    registerEventListener
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Shield( registerEventListener )
  (JNIEnv *env, jobject _this )
{
    jclass local_ref;

    /* Indicate not to call the hook until the initialization is complete */
    _shield.call_hook = 0;

    /* Get the method from the object */
    if (( local_ref = (*env)->FindClass( env, _SHIELD_OBJ_STR )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->FindClass\n" );
    }
    
    _shield.class = (*env)->NewGlobalRef( env, local_ref );
    
    (*env)->DeleteLocalRef( env, local_ref );
    
    if ( _shield.class == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }
    
    _shield.call_listener_rejection_mid = (*env)->GetMethodID( env, _shield.class, _SHIELD_METHOD_REJ_NAME, 
                                                               _SHIELD_METHOD_REJ_DESC );

    if ( _shield.call_listener_rejection_mid == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetMethodID\n" );
    }

    _shield.call_listener_statistic_mid = (*env)->GetMethodID( env, _shield.class, _SHIELD_METHOD_STAT_NAME, 
                                                               _SHIELD_METHOD_STAT_DESC );

    if ( _shield.call_listener_statistic_mid == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetMethodID\n" );
    }

    
    if (( _shield.object = (*env)->NewGlobalRef( env, _this )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }    
    
    if ( netcap_shield_register_hook( _event_hook ) < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_register_hook\n" );
    }

    /* Indicate to call the hook now that the system is registered */
    _shield.call_hook = 1;
}

/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    removeEventListener
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Shield( removeEventListener )
  (JNIEnv *env, jobject _this )
{
    netcap_shield_unregister_hook();

    /* Don't call the hook anymore */
    _shield.call_hook = 0;

    /* To avoid synchronization issues, the global references are never removed
     * (the shield is a singleton anyway) */
}

/*
 * Class:     com_untangle_jnetcap_Shield
 * Method:    setNodeSettings
 * Signature: ([D[J[J)V
 */
JNIEXPORT void JNICALL JF_Shield( setNodeSettings )
( JNIEnv *env, jobject _this, jdoubleArray j_dividerArray, jlongArray j_addressArray, 
  jlongArray j_netmaskArray )
{
    if ( j_dividerArray == NULL || j_addressArray == NULL || j_netmaskArray == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "NULL argument\n" );
    }

    int length;

    if (( length = (*env)->GetArrayLength( env, j_dividerArray )) < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetArrayLength\n" );
    }
    
    jdouble* dividerArray;
    jlong*   addressArray;
    jlong*   netmaskArray;

    if (( dividerArray = (*env)->GetDoubleArrayElements( env, j_dividerArray, NULL )) == NULL ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetDoubleArrayElements\n" );
    }
    
    if (( addressArray = (*env)->GetLongArrayElements( env, j_addressArray, NULL )) == NULL ) {
        (*env)->ReleaseDoubleArrayElements( env, j_dividerArray, dividerArray, 0 );
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetDoubleArrayElements\n" );
    }

    if (( netmaskArray = (*env)->GetLongArrayElements( env, j_netmaskArray, NULL )) == NULL ) {
        (*env)->ReleaseDoubleArrayElements( env, j_dividerArray, dividerArray, 0 );
        (*env)->ReleaseLongArrayElements( env, j_addressArray, addressArray, 0 );
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetDoubleArrayElements\n" );
    }

    /* Just use some maximum */
    if ( length > BLESS_COUNT_MAX ) {
        errlog( ERR_WARNING, "Too many nodes in the blessing list, limiting to %d\n", BLESS_COUNT_MAX );
        length = BLESS_COUNT_MAX;
    }

    netcap_shield_bless_t data[length];
    int c;
    netcap_shield_bless_array_t bless_array = {
        .count = length,
        .d     = data
    };
    
    for ( c = 0 ; c < length ; c++ ) {
        data[c].divider = dividerArray[c];
        data[c].address.s_addr = (in_addr_t)JLONG_TO_UINT( addressArray[c] );
        data[c].netmask.s_addr = (in_addr_t)JLONG_TO_UINT( netmaskArray[c] );
    }
    
    (*env)->ReleaseDoubleArrayElements( env, j_dividerArray, dividerArray, 0 );
    (*env)->ReleaseLongArrayElements( env, j_addressArray, addressArray, 0 );
    (*env)->ReleaseLongArrayElements( env, j_netmaskArray, netmaskArray, 0 );
    
    if ( netcap_shield_bless_users( &bless_array ) < 0 ) {
        return jmvutil_error_void( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_shield_bless_users\n" );
    }    
}


static void _event_hook ( netcap_shield_event_data_t* event )
{
    JNIEnv* env = NULL;

    if ( event == NULL ) {
        errlogargs();
        return;
    }

    if ( _shield.call_hook != 1 ) return;

    if (( env = jmvutil_get_java_env()) == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        return;
    }
    
    if ( _shield.object == NULL || _shield.call_listener_rejection_mid == NULL ||
         _shield.call_listener_statistic_mid == NULL) {
        errlog( ERR_WARNING, "Shield hook never registered." );
        return;
    }

    switch ( event->type ) {
    case NC_SHIELD_EVENT_REJECTION:
        (*env)->CallVoidMethod( env, _shield.object, _shield.call_listener_rejection_mid, 
                                (jlong)event->data.rejection.ip,
                                (jbyte)event->data.rejection.client_intf,
                                (jdouble)event->data.rejection.reputation,
                                event->data.rejection.mode, event->data.rejection.limited, 
                                event->data.rejection.dropped, event->data.rejection.rejected );
        
        break;
    case NC_SHIELD_EVENT_STATISTIC:
        (*env)->CallVoidMethod( env, _shield.object, _shield.call_listener_statistic_mid,
                                event->data.statistic.counters.total.accepted,
                                event->data.statistic.counters.total.limited,
                                event->data.statistic.counters.total.dropped,
                                event->data.statistic.counters.total.rejected,
                                event->data.statistic.relaxed,
                                event->data.statistic.lax,
                                event->data.statistic.tight,
                                event->data.statistic.closed );
        break;
    default:
        errlog( ERR_CRITICAL, "Invalid shield event type: %d\n", event->type );
    }

    /* Clear out any exceptions */
    jmvutil_error_exception_clear();
}
