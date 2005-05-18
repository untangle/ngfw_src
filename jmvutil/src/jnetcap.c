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

#include "jnetcap.h"
#include JH_Netcap

#include "jerror.h"

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

__thread JNIEnv* thread_env;

static struct {
    int mvutil;
    int scheduler;
    int netcap;
    pthread_mutex_t mutex;
    JavaVM* jvm;
    struct {
        jclass    hook_class;     /* Class ID for the hook */
        jmethodID hook_method_id; /* Method identifier for the hook */
        jobject   tcp_hook;       /* TCP Hook */
        jobject   udp_hook;       /* UDP hook */
    } java;
} _jnetcap = 
{
    .mvutil    _UNINITIALIZED, 
    .scheduler _UNINITIALIZED, 
    .netcap    _UNINITIALIZED, 
    .mutex     PTHREAD_MUTEX_INITIALIZER,
    .jvm       NULL,
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

static void              _command_port_guard( JNIEnv* env, jint gate, jint protocol, jstring ports, 
                                              jstring guard, int if_add );

static int _get_interface_address ( char* interface_name, in_addr_t* address, in_addr_t* netmask );

static __inline__ int    _attach_thread( JavaVM* jvm, JNIEnv **env ) 
{
    jint res = -1;
    
    if ( jvm == NULL ) return errlogargs();

#ifdef JNI_VERSION_1_2
    res = (*jvm)->AttachCurrentThread( jvm, (void**)env, NULL );
#else
    res = (*jvm)->AttachCurrentThread( jvm, env, NULL );
#endif // JNI_VERSION_1_2
    
    if ( res < 0 ) return errlog( ERR_CRITICAL, "AttachCurrentThread\n" );

    return 0;
}

static __inline__ void     _detach_thread( JavaVM* jvm ) 
{
    (*jvm)->DetachCurrentThread(jvm);
}

int jnetcap_initialized( void )
{
    return _jnetcap.netcap & _jnetcap.mvutil;
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
        if ( _jnetcap.jvm == NULL ) { 
            if ( (*env)->GetJavaVM( env, &_jnetcap.jvm ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "GetJavaVM\n" );
                break;
            }
        }        

        if ( _jnetcap.mvutil == _UNINITIALIZED ) {
            if ( libmvutil_init() < 0 ) {
                ret = errlog ( ERR_CRITICAL, "libmvutil_init\n" );
                break;
            }
            
            /* Set the initialization flag */
            _jnetcap.mvutil = _INITIALIZED;
        }
        
        if ( _jnetcap.netcap == _UNINITIALIZED ) {
            /* Initialize the error handling library */
            if ( jnetcap_error_init() < 0 ) {
                ret = errlog( ERR_CRITICAL, "jnetcap_error_init\n" );
                break;
            }

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
 * Method:    getHostLong
 * Signature: ();
 */
JNIEXPORT jlong JNICALL JF_Netcap( getHostLong )
  (JNIEnv *env, jclass _class)
{
    /* May want to one day make the interface name an argument, but for now
     * that is not necessary */
    in_addr_t address;
    
    
    if ( _get_interface_address( "br0", &address, NULL ) < 0 )
        address = 0;
    
    return (jlong)address;
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    getNetmask
 * Signature: ()Z;
 */
JNIEXPORT jlong JNICALL JF_Netcap( getNetmaskLong )
    (JNIEnv *env, jclass _class )
{
    /* May want to one day make the interface name an argument, but for now
     * that is not necessary */
    in_addr_t netmask;
    
    
    if ( _get_interface_address( "br0", NULL, &netmask ) < 0 )
        netmask = 0;
    
    return (jlong)netmask;
}


/*
 * Class:     Netcap
 * Method:    cleanup
 * Signature: ()V
 */
JNIEXPORT void JNICALL JF_Netcap( cleanup )
( JNIEnv *env, jclass _class )
{
    if ( _jnetcap.netcap == _INITIALIZED ) {
        /* Unregister all of the hooks */
        _unregister_hook( env, IPPROTO_UDP );
        _unregister_hook( env, IPPROTO_TCP );
        
        netcap_cleanup();
        _jnetcap.netcap = 0;
    }

    if ( _jnetcap.mvutil == _INITIALIZED ) {
        libmvutil_cleanup();
        _jnetcap.mvutil = 0;
    }
    
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
    
    // muntrace();
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
        return jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    };

    do {
        if ( netcap_interface_string_to_intf( (char*)str, &intf ) < 0 ) {
            ret = jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Invalid interface: '%s'\n", intf );
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
        return jnetcap_error_null( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Invalid interface: %d\n", intf );
    }

    return (*env)->NewStringUTF( env, buf );    
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    blockIncomingTraffic
 * Signature: (ZIIII)V
 */
JNIEXPORT void JNICALL Java_com_metavize_jnetcap_Netcap_blockIncomingTraffic
  (JNIEnv *env, jclass _class, jboolean if_add, jint protocol, jint intf, jint low, jint high )
{
    if_add = ( if_add == JNI_TRUE ) ? 1 : 0;

    if ( netcap_subscription_block_incoming( if_add, protocol, intf, low, high ) < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_subscription_block_incoming\n" );
    }
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    limitSubnet
 * Signature: (Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL JF_Netcap( limitSubnet )
    (JNIEnv *env, jclass _class, jstring inside, jstring outside )
{
    const char* inside_str = NULL;
    const char* outside_str = NULL;

    if (( inside_str = (*env)->GetStringUTFChars( env, inside, NULL )) == NULL ) {
        return jnetcap_error_void( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
    };

    do {
        if (( outside_str = (*env)->GetStringUTFChars( env, outside, NULL )) == NULL ) {
            jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
            break;
        };

        if ( netcap_interface_limit_subnet( (char*)inside_str, (char*)outside_str ) < 0 ) {
            jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "netcap_interface_limit_subnet\n" );
            break;
        }
        
    } while ( 0 );
    
    (*env)->ReleaseStringUTFChars( env, inside, inside_str );
    if ( outside_str != NULL ) (*env)->ReleaseStringUTFChars( env, outside, outside_str );    
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    stationTcpGuard
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Netcap( stationTcpGuard )
    (JNIEnv *env, jclass _class, jint gate, jstring ports, jstring guests )
{
    _command_port_guard( env, gate, IPPROTO_TCP, ports, guests, _STATION_GUARD );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    stationUdpGuard
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Netcap( stationUdpGuard )
    (JNIEnv *env, jclass _class, jint gate, jstring ports, jstring guests )
{
    _command_port_guard( env, gate, IPPROTO_UDP, ports, guests, _STATION_GUARD );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    relieveTcpGuard
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Netcap( relieveTcpGuard )
    (JNIEnv *env, jclass _class, jint gate, jstring ports, jstring guests )
{
    _command_port_guard( env, gate, IPPROTO_TCP, ports, guests, _RELIEVE_GUARD );
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    relieveUdpGuard
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Netcap( relieveUdpGuard )
    (JNIEnv *env, jclass _class, jint gate, jstring ports, jstring guests )
{
    _command_port_guard( env, gate, IPPROTO_UDP, ports, guests, _RELIEVE_GUARD );
}

static void _command_port_guard( JNIEnv* env, jint gate, jint protocol, jstring ports, jstring guests, 
                                 int if_add )
{
    const char* ports_str  = NULL;
    const char* guests_str = NULL;
    int ret = 0;

    if ( ports == NULL ) {
        return jnetcap_error_void( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "NULL ports" );
    } else {
        if (( ports_str = (*env)->GetStringUTFChars( env, ports, NULL )) == NULL ) {
            return jnetcap_error_void( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
        };
    }

    do {
        if ( guests != NULL ) {
            if (( guests_str = (*env)->GetStringUTFChars( env, guests, NULL )) == NULL ) {
                jnetcap_error_void( JNETCAP_ERROR_STT, ERR_CRITICAL, "(*env)->GetStringUTFChars\n" );
                break;
            }
        }
        
        if ( if_add == _STATION_GUARD ) {
            ret = netcap_interface_station_port_guard((netcap_intf_t)gate, protocol, (char*)ports_str, 
                                                     (char*)guests_str );
            if ( ret < 0 ) {
                jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Unable to station guard\n" );
            }
        } else { 
            ret = netcap_interface_relieve_port_guard((netcap_intf_t)gate, protocol, (char*)ports_str,
                                                      (char*)guests_str );
            if ( ret < 0 ) {
                jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "Unable to relieve guard\n" );
            }
        }
    } while( 0 );

    if ( ports_str != NULL ) (*env)->ReleaseStringUTFChars( env, ports, ports_str );
    if ( guests_str != NULL ) (*env)->ReleaseStringUTFChars( env, guests, guests_str );
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
        jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "debugLevel: %d\n", debug_level );
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
            return jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "jnetcap_thread_create\n" );
        }
                
        if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, arg )) {
            perrlog( "pthread_create" );
            return jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "pthread_create\n" );
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
        return jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "NULL icmp mailbox\n" );
    }

    /* Convert the byte array */
    if (( data = (*env)->GetByteArrayElements( env, _data, NULL )) == NULL ) {
        return jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "GetByteArrayElements\n" );
    }
    
    do {
        data_lim = (*env)->GetArrayLength( env, _data );
        
        if ( data_len > data_lim ) {
            ret = jnetcap_error( JNETCAP_ERROR_ARGS, ERR_CRITICAL, "ICMP: size > byte array length\n" );
            break;
        }
        
        if (( ret = netcap_icmp_update_pkt( data, data_len, data_lim, icmp_type, icmp_code, icmp_pid, icmp_mb )) < 0 ) {
            ret = jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_icmp_fix_pkt\n" );
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
    ( JNIEnv* env, jclass _class, jint inside, jint outside )
{
    if ( netcap_update_address( inside, outside ) < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_update_address\n" );
    }
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    disableLocalAntisubscribe
 * Signature: ()V;
 */
JNIEXPORT void JNICALL JF_Netcap( disableLocalAntisubscribe )
    ( JNIEnv* env, jclass _class )
{
    if ( netcap_subscription_enable_local() < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_subscription_enable_local\n" );
    }
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    enableLocalAntisubscribe
 * Signature: ()V;
 */
JNIEXPORT void JNICALL JF_Netcap( enableLocalAntisubscribe )
    ( JNIEnv* env, jclass _class )
{
    if ( netcap_subscription_disable_local() < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_subscription_disable_local\n" );
    }
}
/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    disableDhcpForwarding
 * Signature: ()V;
 */
JNIEXPORT void JNICALL JF_Netcap( disableDhcpForwarding )
    ( JNIEnv* env, jclass _class )
{
    if ( netcap_subscription_disable_dhcp_forwarding() < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_subscription_disable_dhcp_forwarding\n" );
    }
}

/*
 * Class:     com_metavize_jnetcap_Netcap
 * Method:    enableDhcpForwarding
 * Signature: ()V;
 */
JNIEXPORT void JNICALL JF_Netcap( enableDhcpForwarding )
    ( JNIEnv* env, jclass _class )
{
    if ( netcap_subscription_enable_dhcp_forwarding() < 0 ) {
        jnetcap_error( JNETCAP_ERROR_STT, ERR_CRITICAL, "netcap_subscription_enable_dhcp_forwarding\n" );
    }
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
    JNIEnv* env = thread_env;
    u_int session_id;
    netcap_session_t* netcap_sess_cleanup;
    
    if ( _jnetcap.netcap != _INITIALIZED || _jnetcap.java.hook_class == NULL || 
         _jnetcap.java.hook_method_id == NULL) { 
        errlog( ERR_CRITICAL, "_hook: unintialized\n" );
        netcap_session_raze( netcap_sess);
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
    if ( jnetcap_exception_clear() < 0 ) {
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

    if ( _arg == NULL ) return errlogargs_null();

    if ( ((jnetcap_thread_t*)_arg)->thread_func == NULL ) return errlogargs_null();

    memcpy( &arg, _arg, sizeof( jnetcap_thread_t ));
    
    jnetcap_thread_raze( _arg );

    /* Attach the thread to the VM */
    if ( _attach_thread( _jnetcap.jvm, &thread_env ) < 0 ) {
        return errlog_null( ERR_CRITICAL, "_attach_thread\n" );
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
    
    _detach_thread( _jnetcap.jvm );

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

JNIEnv* jnetcap_get_java_env( void )
{
    static __thread JNIEnv* env = NULL;
    int res  = 0;

    /* This shouldn't happen because it is initialized in JNI_OnLoad */
    if ( _jnetcap.jvm == NULL ) {
        jsize num_jvms;
        if ( JNI_GetCreatedJavaVMs( &_jnetcap.jvm, 1, &num_jvms ) < 0 ) {
            return errlog_null( ERR_CRITICAL, "JNI_GetCreatedJavaVMs\n" );
        }
        if ( num_jvms > 1 ) return errlog_null( ERR_CRITICAL, "MULTIPLE JVMS\n" );
    }

    if ( env == NULL ) {
#ifdef JNI_VERSION_1_2
        res = (*_jnetcap.jvm)->AttachCurrentThread( _jnetcap.jvm, (void**)&env, NULL );
#else
        res = (*_jnetcap.jvm)->AttachCurrentThread( _jnetcap.jvm, &env, NULL );
#endif // JNI_VERSION_1_2
        
        if ( res < 0 ) return errlog_null( ERR_CRITICAL, "AttachCurrentThread\n" );
    }

    return env;
}


/* Retrieves the address of br0 */
static int _get_interface_address ( char* interface_name, in_addr_t* address, in_addr_t* netmask )
{
    int sockfd, ret = 0;
    struct ifreq ifr;
    
    if (( sockfd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) 
        return perrlog( "socket" );

    do {
        strncpy( ifr.ifr_name, interface_name, sizeof( ifr.ifr_name ));

        /* Get the address */
        if ( address != NULL ) {
            if ( ioctl( sockfd, SIOCGIFADDR, &ifr ) < 0 ) { ret = perrlog( "ioctl" ); break; }
            *address = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr.s_addr;
        }

        /* Get the netmask */
        if ( netmask != NULL ) {
            if ( ioctl( sockfd, SIOCGIFNETMASK, &ifr ) < 0 ) { ret = perrlog( "ioctl" ); break; }
            *netmask = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr.s_addr;
        }
    } while ( 0 );

    if ( close ( sockfd )  < 0 ) 
        return perrlog( "close" );
    
    return ret;
}
