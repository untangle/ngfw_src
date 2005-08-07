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
#include <netinet/in.h>
#include <signal.h>
#include <unistd.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <mcheck.h>

#include <libnetcap.h>
#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include JH_Netcap

#define _verify_mvutil_initialized()    if ( _jnetcap.mvutil != _INITIALIZED ) return -1
#define _verify_netcap_initialized()    if ( _jnetcap.netcap != _INITIALIZED ) return -1
#define _verify_scheduler_initialized() if ( _jnetcap.scheduler != _INITIALIZED ) return -1

/* Verify that the MAX_INTERFACES constant is set properly */
#if JN_Netcap( MAX_INTERFACE ) != NETCAP_MAX_INTERFACES
#error MAX_INTERFACES
#endif

#define _HOOK_OBJ_STR     JP_BUILD_NAME( NetcapHook )
#define _HOOK_METHOD_NAME "event"
#define _HOOK_METHOD_DESC "(I)V"

#define _STATION_GUARD  0x53AD4332
#define _RELIEVE_GUARD  0xDEADD00D

typedef struct {
    void* (*thread_func)(void* arg);
    void* arg;

    /* Thread settings */
    int policy;
    struct sched_param* param;
} jnetcap_thread_t;

static struct {
    int scheduler;
    int netcap;
    pthread_mutex_t mutex;
    struct {
        jclass    hook_class;     /* Class ID for the hook */
        jmethodID hook_method_id; /* Method identifier for the hook */
        jobject   tcp_hook;       /* TCP Hook */
        jobject   udp_hook;       /* UDP hook */
    } java;
} _jnetcap = 
{
    .scheduler _UNINITIALIZED, 
    .netcap    _UNINITIALIZED, 
    .mutex     PTHREAD_MUTEX_INITIALIZER,
    .java { 
        .hook_class     NULL,
        .hook_method_id NULL,
        .tcp_hook       NULL,
        .udp_hook       NULL
    }
};

static void*             _run_thread( void* arg );

static jnetcap_thread_t* jnetcap_thread_malloc();

static void              _udp_hook( netcap_session_t* netcap_session, void* arg );
static void              _tcp_hook( netcap_session_t* netcap_session, void* arg );
static void              _icmp_hook( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg);

/* shared hook between the UDP and TCP hooks, these just get the program into java */
static void              _hook( int protocol, netcap_session_t* netcap_session, void* arg );

/* Register/unregistering hooks */
static int               _register_hook( JNIEnv *env, int protocol, jobject hook );
static int               _unregister_hook( JNIEnv *env, int protocol );

static int               jnetcap_thread_init( jnetcap_thread_t* input, void* (*thread_func)(void*), 
                                              void* arg, int policy, struct sched_param* param );

static jnetcap_thread_t* jnetcap_thread_create( void* (*thread_func)(void*), void* arg, 
                                                int policy, struct sched_param* param );

static void              jnetcap_thread_free    ( jnetcap_thread_t* input );
static void              jnetcap_thread_destroy ( jnetcap_thread_t* input );
static void              jnetcap_thread_raze    ( jnetcap_thread_t* input );

static __inline__ void     _detach_thread( JavaVM* jvm ) 
{
    (*jvm)->DetachCurrentThread(jvm);
}

int jnetcap_initialized( void )
{
    return _jnetcap.netcap;
}

/*
 * Class:     Netcap
 * Method:    cInit
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Netcap( init )
( JNIEnv *env, jclass _class, jboolean shield_enable, jint netcap_debug_level, jint jnetcap_debug_level )
{
    int ret = 0;
    jclass local;
    
    if ( pthread_mutex_lock ( &_jnetcap.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );

//  mtrace();
    
    do {
        /* JMVUTIL guarantees that it is only initialized once. */
        if ( jmvutil_init() < 0 ) {
            ret = errlog ( ERR_CRITICAL, "jvmutil_init\n" );
            break;
        }
        
        if ( _jnetcap.netcap == _UNINITIALIZED ) {
            /* Set the netcap debugging level */
            netcap_debug_set_level ( netcap_debug_level );

            /* Set the jnetcap debugging level */
            debug_set_mylevel( jnetcap_debug_level );
            
            if ( netcap_init(( shield_enable == JNI_TRUE ) ? NETCAP_SHIELD_ENABLE : 0 ) < 0 ) {
                ret = errlog ( ERR_CRITICAL, "netcap_init\n" );
                break;
            }

            /* XXX Consider moving this to a helper function */
            /* Set the class identifiers */
            if (( local = (*env)->FindClass( env, _HOOK_OBJ_STR )) == NULL ) {
                ret = errlog( ERR_CRITICAL, "(*env)->FindClass\n" );
                break;
            }
            _jnetcap.java.hook_class = (*env)->NewGlobalRef( env, local );
            
            (*env)->DeleteLocalRef( env, local );

            if ( _jnetcap.java.hook_class == NULL ) {
                ret = errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
                break;
            }

            /* Set the method identifier */
            _jnetcap.java.hook_method_id = (*env)->GetMethodID( env, _jnetcap.java.hook_class,
                                                                _HOOK_METHOD_NAME, _HOOK_METHOD_DESC );

            if ( _jnetcap.java.hook_method_id == NULL ) {
                ret = errlog( ERR_CRITICAL, "(*env)->GetMethodID" );
                break;
            }

            _jnetcap.netcap = _INITIALIZED;
        }
    } while ( 0 );
    
    if ( pthread_mutex_unlock ( &_jnetcap.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );
    
    return ret;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    getInterfaceAddressLong
 * Signature: ();
 */
JNIEXPORT jlong JNICALL JF_Netcap( getInterfaceAddressLong )
  (JNIEnv *env, jobject _this, jstring j_interface_string )
{
    const char* str;
    struct in_addr address;
    int ret;
    
    if (( str = (*env)->GetStringUTFChars( env, j_interface_string, NULL )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    }
    
    ret = netcap_interface_get_address((char*)str, &address );

    (*env)->ReleaseStringUTFChars( env, j_interface_string, str );

    if ( ret < 0 ) {
        jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to retrieve interface address.\n" );
        return (jlong)-1L;
    }
        
    return (jlong)address.s_addr;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    getInterfaceNetmaskLong
 * Signature: ()Z;
 */
JNIEXPORT jlong JNICALL JF_Netcap( getInterfaceNetmaskLong )
    (JNIEnv *env, jobject _this, jstring j_interface_string )
{
    const char* str;
    struct in_addr netmask;
    int ret;
    
    if (( str = (*env)->GetStringUTFChars( env, j_interface_string, NULL )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    }

    ret = netcap_interface_get_netmask((char*)str, &netmask );
    
    (*env)->ReleaseStringUTFChars( env, j_interface_string, str );

    if ( ret < 0 ) {
        jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to netmask for interface.\n" );
        return (jlong)-1L;
    }
        
    return (jlong)netmask.s_addr;
}


/*
 * Class:     Netcap
 * Method:    cleanup
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Netcap( cleanup )
( JNIEnv *env, jclass _class )
{
    if ( pthread_mutex_lock ( &_jnetcap.mutex ) < 0 ) perrlog ( "pthread_mutex_lock" );
    
    do {
        if ( _jnetcap.netcap == _INITIALIZED ) {
            /* Unregister all of the hooks */
            _unregister_hook( env, IPPROTO_UDP );
            _unregister_hook( env, IPPROTO_TCP );
            
            netcap_cleanup();
        }
        _jnetcap.netcap = 0;
        
        if ( jmvutil_cleanup() < 0 ) errlog( ERR_WARNING, "jmvutil_cleanup\n" );
        
        /* Exit the scheduler */
        if ( _jnetcap.scheduler == _INITIALIZED ) {
            /* The scheduler is exited by netcap_cleanup */
        }
        
        /* Delete any of the global references */
        if ( _jnetcap.java.hook_class != NULL ) {
            (*env)->DeleteGlobalRef( env, _jnetcap.java.hook_class );
            _jnetcap.java.hook_class = NULL;
        }
        
#ifdef _MCD_CHECK
        /*XXX This is only valid for the MCD checker */
        /* Dump out the memory logs */
        showMemStats();
#endif
    } while( 0 );

    // muntrace();

    if ( pthread_mutex_unlock ( &_jnetcap.mutex ) < 0 )  perrlog ( "pthread_mutex_unlock" );    
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    convertStringToIntf
 * Signature: (Ljava/lang/String;)B
 */
JNIEXPORT jbyte JNICALL JF_Netcap( convertStringToIntf )
    (JNIEnv *env, jclass _class, jstring string_intf )
{
    const char* str;
    netcap_intf_t intf;
    int ret = 0;

    if (( str = (*env)->GetStringUTFChars( env, string_intf, NULL )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    }

    do {
        if ( netcap_interface_string_to_intf( (char*)str, &intf ) < 0 ) {
            ret = jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid interface: '%s'\n", intf );
        }
    } while ( 0 );

    (*env)->ReleaseStringUTFChars( env, string_intf, str );

    return ( ret < 0 ) ? ret : intf;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    convertIntfToString
 * Signature: (I)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL JF_Netcap( convertIntfToString )
    (JNIEnv *env, jclass _class, jint intf )
{
    char buf[NETCAP_MAX_IF_NAME_LEN];

    if ( intf == NC_INTF_UNK ) return (*env)->NewStringUTF( env, "" );
    
    if ( netcap_interface_intf_to_string( intf, buf, sizeof( buf )) < 0 ) {
        return jmvutil_error_null( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "Invalid interface: %d\n", intf );
    }

    return (*env)->NewStringUTF( env, buf );    
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    debugLevel
 * Signature: (I)V
 */
JNIEXPORT void JNICALL JF_Netcap( debugLevel )
    (JNIEnv* env, jclass _class, jint type, jint debug_level)
{
    switch ( type ) {
    case JN_Netcap( NETCAP_DEBUG ):  
        netcap_debug_set_level( debug_level ); 
        break;

    case JN_Netcap( JNETCAP_DEBUG ): 
        debug_set_mylevel( debug_level ); 
        break;
        
    default:
        jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "debugLevel: %d\n", debug_level );
        errlog( ERR_WARNING, "Invalid debug type: %d\n", type );
    }    
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    donateThreads
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Netcap( donateThreads )
  ( JNIEnv *env, jclass _class, jint num_threads )
{
    pthread_t id;
    jnetcap_thread_t* arg;

    _verify_netcap_initialized ();

    // if (( num_threads < 0 ) || ( num_threads > JN_Netcap( MAX_THREADS ))) return errlogargs();

    /* Donate a few threads */
    for ( ; num_threads > 0 ; num_threads-- ) {
        arg = jnetcap_thread_create( netcap_thread_donate, NULL, SCHED_OTHER, NULL );
        
        if ( arg == NULL ) {
            return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jnetcap_thread_create\n" );
        }
                
        if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, arg )) {
            perrlog( "pthread_create" );
            return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "pthread_create\n" );
        }
    }
    
    return 0;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    startScheduler
 * Signature: ()I
 */
JNIEXPORT jint JNICALL JF_Netcap( startScheduler )
  (JNIEnv *env, jclass _class )
{
    pthread_t id;

    int ret = 0;
    
    if ( pthread_mutex_lock ( &_jnetcap.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );

    do {
        if ( _jnetcap.scheduler == _UNINITIALIZED ) {
            if ( pthread_create( &id, &uthread_attr.other.medium, netcap_sched_donate, NULL )) {
                ret = perrlog( "pthread_create" );
                break;
            }
            _jnetcap.scheduler = _INITIALIZED;
        } else {
            ret = errlog ( ERR_CRITICAL, "Scheduler is already initialized\n" );
        }        
    } while ( 0 );
    
    if ( pthread_mutex_unlock ( &_jnetcap.mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    return ret;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    registerUDPHook
 * Signature: (Lcom/metavize/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerUDPHook )
  (JNIEnv *env, jclass _class, jobject udp_hook )
{
    return _register_hook( env, IPPROTO_UDP, udp_hook );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    registerTCPHook
 * Signature: (Lcom/metavize/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerTCPHook )
  (JNIEnv *env, jclass _class, jobject tcp_hook )
{
    return _register_hook( env, IPPROTO_TCP, tcp_hook );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    unregisterUDPHook
 * Signature: (Lcom/metavize/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterUDPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_hook( env, IPPROTO_UDP );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    unregisterTCPHook
 * Signature: (Lcom/metavize/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterTCPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_hook( env, IPPROTO_TCP );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    isBroadcast
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL JF_Netcap( isBroadcast )
    ( JNIEnv* env, jclass _class, jlong address )
{
    in_addr_t addr = (in_addr_t)JLONG_TO_UINT( address );
    
    if ( netcap_interface_is_broadcast( addr ))
        return JNI_TRUE;
        
    return JNI_FALSE;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    isBridgeAlive
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL JF_Netcap( isBridgeAlive )
    ( JNIEnv* env, jclass _class )
{    
    if ( netcap_interface_bridge_exists() == 1 )
        return JNI_TRUE;
        
    return JNI_FALSE;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    updateIcmpPacket
 * Signature: ([BIII)V
 */
JNIEXPORT jint JNICALL JF_Netcap( updateIcmpPacket )
( JNIEnv *env, jobject _this, jbyteArray _data, jint data_len, jint icmp_type, jint icmp_code, jint icmp_pid, jlong _icmp_mb )
{
    mailbox_t* icmp_mb = (mailbox_t*)JLONG_TO_UINT( _icmp_mb );
    jbyte* data;
    int data_lim;
    int ret = -1;
    
    if ( icmp_mb == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL icmp mailbox\n" );
    }

    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "GetByteArrayElements\n" );
    }
    
    do {
        data_lim = (*env)->GetArrayLength( env, _data );
        
        if ( data_len > data_lim ) {
            ret = jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "ICMP: size > byte array length\n" );
            break;
        }
        
        if (( ret = netcap_icmp_update_pkt( data, data_len, data_lim, icmp_type, icmp_code, icmp_pid, icmp_mb )) < 0 ) {
            ret = jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_icmp_fix_pkt\n" );
            break;
        }        
    } while ( 0 );

    (*env)->ReleaseByteArrayElements( env, _data, data, 0 );
    
    return ret;
}


/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    isMulticast
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL JF_Netcap( isMulticast )
    ( JNIEnv* env, jclass _class, jlong address )
{
    in_addr_t addr = (in_addr_t)JLONG_TO_UINT( address );
    
    if ( netcap_interface_is_multicast( addr ))
        return JNI_TRUE;
        
    return JNI_FALSE;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    updateAddress
 * Signature: ()V;
 */
JNIEXPORT void JNICALL JF_Netcap( updateAddress )
    ( JNIEnv* env, jclass _class )
{
    if ( netcap_update_address() < 0 ) {
        jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_update_address" );
    }
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    cTcpRedirectPorts
 * Signature: ()V;
 */
JNIEXPORT jintArray JNICALL JF_Netcap( cTcpRedirectPorts )
    ( JNIEnv* env, jobject _this )
{
    int ports[2];
    jintArray j_ports;

    if ( netcap_tcp_redirect_ports( &ports[0], &ports[1] ) < 0 ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_tcp_redirect_ports" );
    }

    /** Make an array to return the two values */
    if (( j_ports = (*env)->NewIntArray( env, 2 )) == NULL ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewByteArray" );
    }
    
    (*env)->SetIntArrayRegion( env, j_ports, 0, 2, (jint*)ports );

    return j_ports;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    cUdpDivertPort
 * Signature: ()I;
 */
JNIEXPORT jint JNICALL JF_Netcap( cUdpDivertPort )
    ( JNIEnv* env, jobject _this )
{
    int port = -1;

    if (( port = netcap_udp_divert_port()) < 0 ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_udp_divert_port" );
    }

    return port;
}


static void*             _tcp_run_thread( void* arg )
{
    netcap_session_t* netcap_sess = arg;
    
    if ( netcap_sess == NULL ) {
        return errlogargs_null();
    }
    
    _hook( IPPROTO_TCP, netcap_sess, NULL );
        
    return NULL;
}

static void*             _udp_run_thread( void* arg )
{
    netcap_session_t* netcap_sess = arg;
    
    if ( netcap_sess == NULL ) {
        return errlogargs_null();
    }
    
    _hook( IPPROTO_UDP, netcap_sess, NULL );
        
    return NULL;
}

static void*             _icmp_run_thread( void* arg )
{
    netcap_session_t* netcap_sess = arg;
    
    if ( netcap_sess == NULL ) {
        return errlogargs_null();
    }
    
    _hook( IPPROTO_ICMP, netcap_sess, NULL );
        
    return NULL;
}

static void              _udp_hook( netcap_session_t* netcap_sess, void* arg )
{
    pthread_t id;
    jnetcap_thread_t* thread_arg;
    
    if (( thread_arg = jnetcap_thread_create( _udp_run_thread, netcap_sess, SCHED_OTHER, NULL )) == NULL ) {
        /* FIXME XXX Possible leak */
        errlog( ERR_CRITICAL, "jnetcap_thread_create" );
        return;
    }
    
    if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, thread_arg )) {
        perrlog( "pthread_create" );
        return;
        /* XXX FIXME Possible leak of the netcap session */
    }

    //_hook( IPPROTO_UDP, netcap_sess, arg );
}

static void              _icmp_hook( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg)
{
    pthread_t id;
    jnetcap_thread_t* thread_arg;
    
    if (( thread_arg = jnetcap_thread_create( _icmp_run_thread, netcap_sess, SCHED_OTHER, NULL )) == NULL ) {
        /* FIXME XXX Possible leak */
        errlog( ERR_CRITICAL, "jnetcap_thread_create" );
        return;
    }
    
    if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, thread_arg )) {
        perrlog( "pthread_create" );
        return;
        /* XXX FIXME Possible leak of the netcap session */
    }
/*     if ( pkt != NULL ) { */
/*         errlog( ERR_CRITICAL, "_icmp_hook: Unable to handle packet" ); */
/*         netcap_pkt_raze( pkt ); */
/*     } */

/*     if ( netcap_sess != NULL ) { */
/*         _hook( IPPROTO_UDP, netcap_sess, arg ); */
/*     } */
}

static void              _tcp_hook( netcap_session_t* netcap_sess, void* arg )
{
    pthread_t id;
    jnetcap_thread_t* thread_arg;
    
    if (( thread_arg = jnetcap_thread_create( _tcp_run_thread, netcap_sess, SCHED_OTHER, NULL )) == NULL ) {
        /* FIXME XXX Possible leak */
        errlog( ERR_CRITICAL, "jnetcap_thread_create" );
        return;
    }
    
    if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, thread_arg )) {
        perrlog( "pthread_create" );
        return;
        /* XXX FIXME Possible leak of the netcap session */
    }
    
//    _hook( IPPROTO_TCP, netcap_sess, arg );
}

/* shared hook between the UDP and TCP hooks, these just get the program into java */
static void              _hook( int protocol, netcap_session_t* netcap_sess, void* arg )
{
    jobject global_hook = NULL;
    JNIEnv* env;
    u_int session_id;
    netcap_session_t* netcap_sess_cleanup;

    if (( env = jmvutil_get_java_env()) == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        netcap_session_raze( netcap_sess );
        return;
    }

    if ( _jnetcap.netcap != _INITIALIZED || _jnetcap.java.hook_class == NULL || 
         _jnetcap.java.hook_method_id == NULL) { 
        errlog( ERR_CRITICAL, "_hook: unintialized\n" );
        netcap_session_raze( netcap_sess );
        return;
    }
    
    switch( protocol ) {
    case IPPROTO_ICMP: global_hook = _jnetcap.java.udp_hook; break;
    case IPPROTO_UDP:  global_hook = _jnetcap.java.udp_hook; break;
    case IPPROTO_TCP:  global_hook = _jnetcap.java.tcp_hook; break;
    default: 
        errlog( ERR_CRITICAL, "_hook: invalid protocol(%d)\n", protocol );
    }

    if ( global_hook == NULL ) {
        errlog( ERR_CRITICAL, "_hook: invalid hook\n" );
        /* Remove the session */
        netcap_session_raze( netcap_sess);
        return;
    }

    debug( 10, "jnetcap.c: Calling hook\n" );

    /* cache the session_id */
    session_id = netcap_sess->session_id;
    
    /* Call the global method */
    (*env)->CallVoidMethod( env, global_hook, _jnetcap.java.hook_method_id, netcap_sess->session_id );

    debug( 10, "JNETCAP: Exiting hook\n" );

    /* Check to see if there was an exception, this assumes that a new
     * thread was not spawned if there was an exception.  If there was
     * an exception, make sure that the session is taken out of the
     * sesion table */
    if ( jmvutil_error_exception_clear() < 0 ) {
        /* Get the session out from the session table !!! If this
         * happens, it is pretty bad, this really should NEVER happen
         */
        if (( netcap_sess_cleanup = netcap_sesstable_get( session_id )) != NULL ) {
            errlog( ERR_CRITICAL, "Session (%10u) left in table after hook\n", session_id );
            if ( netcap_sess_cleanup != netcap_sess ) {
                errlog( ERR_CRITICAL, "NetcapSession replaced, assuming previous was razed\n" );
                return;
            }

            /* XXX Do not know how to handle this, the session still may need to be used,
             * if there was an exception and a new thread */ 
           netcap_session_raze( netcap_sess_cleanup );
        }
    }
}

/* XXX Probably want a lock around this function */
/* Consider: PTHREAD_RWLOCK_INITIALIZER */
static int              _register_hook( JNIEnv* env, int protocol, jobject hook )
{
    jobject *global_hook;
    int ret;

    _verify_netcap_initialized();
    
    if ( hook == NULL ) return errlogargs();

    switch( protocol ) {
    case IPPROTO_UDP: global_hook = &_jnetcap.java.udp_hook; break;
    case IPPROTO_TCP: global_hook = &_jnetcap.java.tcp_hook; break;
    default: return errlog( ERR_CRITICAL, "_register_hook: invalid protocol(%d)\n", protocol );
    }
        
    /* If necessary, remove the previous global reference */
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }

    if (( *global_hook = (*env)->NewGlobalRef( env, hook )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }
    
    switch ( protocol ) {
    case IPPROTO_UDP: 
        ret = netcap_udp_hook_register( _udp_hook ); 
        
        if ( ret < 0 ) 
            break;
        
        ret = netcap_icmp_hook_register( _icmp_hook );
        break;
    case IPPROTO_TCP: ret = netcap_tcp_hook_register( _tcp_hook ); break;
    default: ret = -1; /* IMPOSSIBLE */
    }
    
    if ( ret < 0 ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        return errlog( ERR_CRITICAL, "netcap_*_hook_register\n" );
    }
    
    return 0;
}

static int               _unregister_hook( JNIEnv* env, int protocol )
{
    jobject *global_hook;
    _verify_netcap_initialized();

    switch( protocol ) {
    case IPPROTO_UDP: 
        global_hook = &_jnetcap.java.udp_hook;
        netcap_udp_hook_unregister();
        break;

    case IPPROTO_TCP: 
        global_hook = &_jnetcap.java.tcp_hook;
        netcap_tcp_hook_unregister();
        break;
        
    default: return errlog( ERR_CRITICAL, "_register_hook: invalid protocol(%d)\n", protocol );
    }
    
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }
    
    return 0;
}

static void*             _run_thread( void* _arg )
{
    jnetcap_thread_t arg;
    pthread_t id;
    void* ret;
    JavaVM* jvm;

    if ( _arg == NULL ) return errlogargs_null();

    if ( ((jnetcap_thread_t*)_arg)->thread_func == NULL ) return errlogargs_null();

    memcpy( &arg, _arg, sizeof( jnetcap_thread_t ));
    
    jnetcap_thread_raze( _arg );

    if (( jvm = jmvutil_get_java_vm()) == NULL ) return errlog_null( ERR_CRITICAL, "jmvutil_get_java_vm\n" );

    /* Attach the thread to the VM */
    if ( jmvutil_get_java_env() == NULL ) {
        return errlog_null( ERR_CRITICAL, "jmvutil_get_java_env\n" );
    }
    
    do {
        /* If necessary, set the scheduling parameters */
        if ( arg.param != NULL ) {
            id = pthread_self();

            /* Not a fatal error */
            if ( pthread_setschedparam( id, arg.policy, arg.param ) < 0 ) perrlog( "pthread_setschedparam" );
        }
        
        /* Run the required function */
        ret = arg.thread_func( arg.arg );

    } while ( 0 );
    
    _detach_thread( jvm );

    jnetcap_thread_destroy( &arg );

    return ret;
}

static jnetcap_thread_t* jnetcap_thread_malloc()
{
    jnetcap_thread_t* jnetcap_thread;
    if (( jnetcap_thread = calloc ( 1, sizeof( jnetcap_thread_t ))) == NULL ) return errlogmalloc_null();
    
    return jnetcap_thread;
}

static int               jnetcap_thread_init( jnetcap_thread_t* input, void* (*thread_func)(void*), 
                                              void* arg, int policy, struct sched_param* param )
{
    if ( input == NULL || thread_func == NULL ) return errlogargs();
    
    
    input->thread_func = thread_func;
    input->arg = arg;
    /* XXX Change */
    input->policy = policy; 
    input->param  = param;

    return 0;
}

static jnetcap_thread_t* jnetcap_thread_create( void* (*thread_func)(void*), void* arg, 
                                                int policy, struct sched_param* param )
{
    jnetcap_thread_t* jnetcap_thread;
    
    if (( jnetcap_thread = jnetcap_thread_malloc()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "jnetcap_thread_malloc\n" );
    }
        
    if ( jnetcap_thread_init( jnetcap_thread, thread_func, arg, policy, param ) < 0 ) {
        return errlog_null( ERR_CRITICAL, "jnetcap_thread_init\n" );
    }
    
    return jnetcap_thread;
}


static void              jnetcap_thread_free    ( jnetcap_thread_t* input )
{
    if ( input == NULL ) return (void)errlogargs();
    
    free ( input );
}

static void              jnetcap_thread_destroy ( jnetcap_thread_t* input )
{
    if ( input == NULL ) return (void)errlogargs();
}

static void              jnetcap_thread_raze    ( jnetcap_thread_t* input )
{
    if ( input == NULL ) return (void)errlogargs();

    jnetcap_thread_destroy(input);
    jnetcap_thread_free(input);
}
