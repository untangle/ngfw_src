/* $HeadURL$ */
#include <jni.h>

#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <netinet/in.h>
#include <signal.h>
#include <unistd.h>
#include <inttypes.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/ioctl.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <mcheck.h>

#include <libnetcap.h>
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>
#include <mvutil/list.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include "com_untangle_jnetcap_Netcap.h"

#define _verify_mvutil_initialized()    if ( _jnetcap.mvutil != _INITIALIZED ) return -1
#define _verify_netcap_initialized()    if ( _jnetcap.netcap != _INITIALIZED ) return -1

#define _HOOK_OBJ_STR     JP_BUILD_NAME( NetcapCallback )
#define _HOOK_METHOD_NAME "event"
#define _HOOK_METHOD_DESC "(J)V"
#define _CONNTRACK_HOOK_METHOD_DESC "(JI)V"

/* default session limit ( 0 means no limit ) */
#define _SESSION_LIMIT_DEFAULT 0

typedef struct {
    void* (*thread_func)(void* arg);
    void* arg;
} pthread_params_t;

static struct {
    int netcap;
    pthread_mutex_t mutex;
    struct {
        jclass    hook_class;     /* Class ID for the hook */
        jmethodID event_method_id; /* Method identifier for the "event" hook */
        jmethodID conntrack_event_method_id; /* Method identifier for the "event" hook with args */
        jobject   tcp_hook;       /* TCP Hook */
        jobject   udp_hook;       /* UDP hook */
        jobject   conntrack_hook; /* Conntrack Hook */
    } java;
    
    int session_limit;
    int session_count;

    /* Used to guarantee that session_limit increases and decreases atomically */
    pthread_mutex_t session_mutex; 
    
    /* For outgoing interface lookups, don't want to wait the whole time. */
    unsigned long delay_array[];
} _jnetcap = 
{
    .netcap    = _UNINITIALIZED, 
    .mutex     = PTHREAD_MUTEX_INITIALIZER,
    .java = { 
        .hook_class     = NULL,
        .event_method_id = NULL,
        .conntrack_event_method_id = NULL,
        .tcp_hook       = NULL,
        .udp_hook       = NULL
    },

    .session_limit = _SESSION_LIMIT_DEFAULT,
    .session_count = 0,
    .session_mutex = PTHREAD_MUTEX_INITIALIZER,

    .delay_array  = {
        3000,
        6000,
        20000,
        60000,
        250000,
        700000,
        10000,
        0
    }
};

static void*             _run_thread( void* arg );

static void              _udp_hook( netcap_session_t* netcap_session, void* arg );
static void              _tcp_hook( netcap_session_t* netcap_session, void* arg );
static void              _conntrack_hook( struct nf_conntrack* ct, int type );

/* shared hook between the UDP and TCP hooks, these just get the program into java */
static void              _hook( int protocol, netcap_session_t* netcap_session, void* arg );

static int _my_pthread_create ( pthread_t* thread, const pthread_attr_t* attr, void* (*func)(void*), void* arg);

static pthread_params_t* _pthread_params_malloc();
static int               _pthread_params_init    ( pthread_params_t* input, void* (*thread_func)(void*), void* arg );
static pthread_params_t* _pthread_params_create  ( void* (*thread_func)(void*), void* arg );
static void              _pthread_params_free    ( pthread_params_t* input );
static void              _pthread_params_destroy ( pthread_params_t* input );
static void              _pthread_params_raze    ( pthread_params_t* input );

static int _increment_session_count();
static int _decrement_session_count();

static int               _unregister_udp_hook( JNIEnv* env );
static int               _unregister_tcp_hook( JNIEnv* env );
static int               _unregister_conntrack_hook( JNIEnv* env );

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
( JNIEnv *env, jclass _class, jint netcap_debug_level, jint jnetcap_debug_level )
{
    int ret = 0;
    jclass local;
    
    if ( pthread_mutex_lock ( &_jnetcap.mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );

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
            
            if ( netcap_init() < 0 ) {
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

            /* Set the method identifier(s) */

            _jnetcap.java.event_method_id = (*env)->GetMethodID( env, _jnetcap.java.hook_class, _HOOK_METHOD_NAME, _HOOK_METHOD_DESC );
            if ( _jnetcap.java.event_method_id == NULL ) {
                ret = errlog( ERR_CRITICAL, "(*env)->GetMethodID" );
                break;
            }

            _jnetcap.java.conntrack_event_method_id = (*env)->GetMethodID( env, _jnetcap.java.hook_class, _HOOK_METHOD_NAME, _CONNTRACK_HOOK_METHOD_DESC );
            if ( _jnetcap.java.conntrack_event_method_id == NULL ) {
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
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    setSessionLimit
 * Signature: (I)V
 */
JNIEXPORT void JNICALL JF_Netcap( setSessionLimit )
  (JNIEnv *env, jobject _this, jint limit )
{
    debug( 0, "JNETCAP: Setting session limit to %d\n", limit );

    _jnetcap.session_limit = limit;
}

/*
 * Class:     Netcap
 * Method:    nextSessionId
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL JF_Netcap( nextSessionId )
( JNIEnv *env, jclass _class )
{
    return netcap_session_next_id();
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
            _unregister_udp_hook( env );
            _unregister_tcp_hook( env );
            _unregister_conntrack_hook( env );
            
            netcap_cleanup();
        }
        _jnetcap.netcap = 0;
        
        if ( jmvutil_cleanup() < 0 ) errlog( ERR_WARNING, "jmvutil_cleanup\n" );
        
        /* Delete any of the global references */
        if ( _jnetcap.java.hook_class != NULL ) {
            (*env)->DeleteGlobalRef( env, _jnetcap.java.hook_class );
            _jnetcap.java.hook_class = NULL;
        }
        
#ifdef _MCD_CHECK
        /*XXX This is only valid for the MCD checker */
        /* Dump out the memory logs */
        showMemStats( 1 );
#endif
    } while( 0 );

    if ( pthread_mutex_unlock ( &_jnetcap.mutex ) < 0 )  perrlog ( "pthread_mutex_unlock" );    
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    debugLevel
 * Signature: (II)V
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
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    donateThreads
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL JF_Netcap( donateThreads )
  ( JNIEnv *env, jclass _class, jint num_threads )
{
    pthread_t id;

    _verify_netcap_initialized ();

    // if (( num_threads < 0 ) || ( num_threads > JN_Netcap( MAX_THREADS ))) return errlogargs();

    /* Donate a few threads */
    for ( ; num_threads > 0 ; num_threads-- ) {
        if ( _my_pthread_create( &id, &uthread_attr.other.medium, netcap_thread_donate, NULL )) {
            perrlog( "pthread_create" );
            return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "pthread_create\n" );
        }
    }
    
    /* Donate a thread to conntrack */
    if ( pthread_create( &id, &uthread_attr.other.medium, netcap_conntrack_listen, NULL )) {
        perrlog( "pthread_create" );
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "pthread_create\n" );
    }

    return 0;
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    registerUDPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerUDPHook )
  (JNIEnv *env, jclass _class, jobject hook )
{
    if ( hook == NULL ) return errlogargs();
    _verify_netcap_initialized();
    int ret;
    jobject *global_hook = &_jnetcap.java.udp_hook;
        
    /* If necessary, remove the previous global reference */
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }

    if (( *global_hook = (*env)->NewGlobalRef( env, hook )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }
    
    ret = netcap_udp_hook_register( _udp_hook ); 
    
    if ( ret < 0 ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        return errlog( ERR_CRITICAL, "netcap_*_hook_register\n" );
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    registerTCPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerTCPHook )
  (JNIEnv *env, jclass _class, jobject hook )
{
    if ( hook == NULL ) return errlogargs();
    _verify_netcap_initialized();
    int ret;
    jobject *global_hook = &_jnetcap.java.tcp_hook;
        
    /* If necessary, remove the previous global reference */
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }

    if (( *global_hook = (*env)->NewGlobalRef( env, hook )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }
    
    ret = netcap_tcp_hook_register( _tcp_hook ); 
    
    if ( ret < 0 ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        return errlog( ERR_CRITICAL, "netcap_*_hook_register\n" );
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    registerConntrackHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerConntrackHook )
  (JNIEnv *env, jclass _class, jobject hook )
{
    if ( hook == NULL ) return errlogargs();
    _verify_netcap_initialized();
    int ret;
    jobject *global_hook = &_jnetcap.java.conntrack_hook;
        
    /* If necessary, remove the previous global reference */
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }

    if (( *global_hook = (*env)->NewGlobalRef( env, hook )) == NULL ) {
        return errlog( ERR_CRITICAL, "(*env)->NewGlobalRef\n" );
    }

    ret = netcap_conntrack_hook_register( _conntrack_hook ); 
    
    if ( ret < 0 ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        return errlog( ERR_CRITICAL, "netcap_*_hook_register\n" );
    }
    
    return 0;
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    unregisterUDPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterUDPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_udp_hook( env );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    unregisterTCPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterTCPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_tcp_hook( env );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    unregisterTCPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterConntrackHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_conntrack_hook( env );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    arpLookup
 * Signature: (S)S
 */
JNIEXPORT jstring JNICALL JF_Netcap( arpLookup )
(JNIEnv *env, jclass _class, jstring ipAddress )
{
    char mac[20];
    const char *ipAddressStr = (*env)->GetStringUTFChars(env, ipAddress, NULL);
    
    int ret = netcap_arp_lookup( ipAddressStr, mac, 20 );
    (*env)->ReleaseStringUTFChars(env, ipAddress, ipAddressStr);

    if ( ret != 0 ) {
        // -1 does not mean an error, it just means it was not found
        //jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_arp_lookup\n" );
        return (*env)->NewStringUTF(env, "");
    }

    return (*env)->NewStringUTF(env, mac);
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    arpLookup
 * Signature: ([JI)I
 */
JNIEXPORT jint JNICALL JF_Netcap( conntrackDump )
(JNIEnv *env, jclass _class, jlongArray arr, jint arr_length )
{
    int count = 0;
    jlong* arr_body = (*env)->GetLongArrayElements(env, arr, 0);
    
    list_t* list = netcap_nfconntrack_dump( (struct nf_conntrack**)arr_body, arr_length );

    struct nf_conntrack* entry;
    while ( list_length( list ) > 0 && count < arr_length ) {
        if ( list_pop_head( list, (void**)&entry ) < 0 )
            break;
        if ( entry == NULL ) {
            errlog(ERR_WARNING, "NULL entry pulled from conntrack list.");
            continue;
        }
        arr_body[count] = (jlong)(intptr_t)entry;
        count++;
    }

    if ( list_raze( list ) < 0 )
        perrlog("list_raze");
    
    (*env)->ReleaseLongArrayElements(env, arr, arr_body, 0);
    return count;
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

static void              _udp_hook( netcap_session_t* netcap_sess, void* arg )
{
    if ( netcap_sess == NULL ) { 
        errlogargs();
        return;
    }

    int _critical_section() {
        pthread_t id;
        
        if ( _my_pthread_create( &id, &uthread_attr.other.medium, _udp_run_thread, netcap_sess )) {
            return perrlog( "pthread_create" );
        }

        return 0;
    }

    if ( _increment_session_count() < 0 ) {
        errlog( ERR_CRITICAL, "Hit session limit %d\n", _jnetcap.session_limit );
        netcap_session_raze( netcap_sess );
        return;
    }

    if ( _critical_section() < 0 ) {
        errlog(ERR_WARNING, "Error occurred. Killing session (%"PRIu64").\n", netcap_sess->session_id);
        netcap_session_raze( netcap_sess );
        _decrement_session_count();
    }
}

static void              _conntrack_hook( struct nf_conntrack* ct, int type )
{
    JNIEnv* env = jmvutil_get_java_env();
    if ( env == NULL ) {
        errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );
        return;
    }

    if (( _jnetcap.netcap != _INITIALIZED ) ||
        ( _jnetcap.java.hook_class == NULL ) ||
        ( _jnetcap.java.conntrack_event_method_id == NULL )) { 
        errlog( ERR_CRITICAL, "_hook: unintialized\n" );
        return;
    }
        
    jobject global_hook = _jnetcap.java.conntrack_hook; 

    if ( global_hook == NULL ) {
        errlog( ERR_CRITICAL, "_hook: invalid hook\n" );
        return;
    }

    debug( 10, "jnetcap: Calling hook\n" );
    
    /* Call the global method */
    (*env)->CallVoidMethod( env, global_hook, _jnetcap.java.conntrack_event_method_id, ((jlong)(intptr_t)ct), ((jint)type));

    debug( 10, "jnetcap: Exiting hook\n" );

    return;
}

static void              _tcp_hook( netcap_session_t* netcap_sess, void* arg )
{
    if ( netcap_sess == NULL ) { 
        errlogargs();
        return;
    }
    
    int _critical_section() {
        pthread_t id;

        if ( _my_pthread_create( &id, &uthread_attr.other.medium, _tcp_run_thread, netcap_sess )) {
            return perrlog( "pthread_create" );
        }

        return 0;
    }

    if ( _increment_session_count() < 0 ) {
        errlog( ERR_CRITICAL, "Hit session limit %d\n", _jnetcap.session_limit );
        netcap_session_raze( netcap_sess );
        return;
    }

    if ( _critical_section() < 0 ) {
        errlog(ERR_WARNING, "Error occurred. Killing session (%"PRIu64").\n", netcap_sess->session_id);
        netcap_session_raze( netcap_sess );
        _decrement_session_count();
    }
}

/* shared hook between the UDP and TCP hooks, this is executed in its own thread, so it
 * must decrement the session count */
static void              _hook( int protocol, netcap_session_t* netcap_sess, void* arg )
{
    jobject global_hook = NULL;

    int _critical_section( void ) {
        JNIEnv* env = jmvutil_get_java_env();
        u_int64_t session_id;

        if ( NULL == env ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );

        if (( _jnetcap.netcap != _INITIALIZED ) ||
            ( _jnetcap.java.hook_class == NULL ) ||
            ( _jnetcap.java.event_method_id == NULL )) { 
            return errlog( ERR_CRITICAL, "_hook: unintialized\n" );
        }
        
        switch( protocol ) {
        case IPPROTO_UDP:  global_hook = _jnetcap.java.udp_hook; break;
        case IPPROTO_TCP:  global_hook = _jnetcap.java.tcp_hook; break;
        default: 
            return errlog( ERR_CRITICAL, "_hook: invalid getProtocol(%d)\n", protocol );
        }

        if ( NULL == global_hook ) return errlog( ERR_CRITICAL, "_hook: invalid hook\n" );

        debug( 10, "jnetcap.c: Calling hook\n" );

        /* cache the session_id for cleanup */
        session_id = netcap_sess->session_id;
        
        /* Call the global method */
        (*env)->CallVoidMethod( env, global_hook, _jnetcap.java.event_method_id, ((jlong)netcap_sess->session_id) );

        debug( 10, "JNETCAP: Exiting hook\n" );

        /* Check to see if there was an exception, this assumes that a new
         * thread was not spawned if there was an exception.  If there was
         * an exception, make sure that the session is taken out of the
         * sesion table */
        if ( jmvutil_error_exception_clear() < 0 ) {
            netcap_session_t* netcap_sess_cleanup;

            /* Get the session out from the session table !!! If this
             * happens, it is pretty bad, this really should NEVER happen
             */
            if (( netcap_sess_cleanup = netcap_sesstable_get( session_id )) != NULL ) {
                errlog( ERR_CRITICAL, "Session (%"PRIu64") left in table after hook\n", session_id );
                if ( netcap_sess_cleanup == netcap_sess ) {
                    /* Since the thread was created in C, there is no way that another
                     * thread could possible raze this session */
                    return errlog( ERR_CRITICAL, "Session (%"PRIu64") not razed in hook, razing in cleanup\n", session_id );
                } else {
                    netcap_sess = NULL;
                    return errlog( ERR_CRITICAL, "Session (%"PRIu64") replaced, assuming previous was razed\n", session_id );
                }
            }
        }

        /* Assume the session has been razed */
        netcap_sess = NULL;
        return 0;
    }
    int ret;
    
    ret = _critical_section();
    
    if (ret < 0 && ( netcap_sess != NULL )) {
        errlog(ERR_WARNING, "Error occurred. Killing session (%"PRIu64").\n", netcap_sess->session_id);
        netcap_session_raze( netcap_sess );
    }
    
    /* Make sure to always decrement the session count */
    _decrement_session_count();        
}

static int               _unregister_udp_hook( JNIEnv* env )
{
    _verify_netcap_initialized();

    jobject *global_hook = &_jnetcap.java.udp_hook;
    netcap_udp_hook_unregister();
    
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }
    
    return 0;
}

static int               _unregister_tcp_hook( JNIEnv* env )
{
    _verify_netcap_initialized();

    jobject *global_hook = &_jnetcap.java.tcp_hook;
    netcap_tcp_hook_unregister();
    
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }
    
    return 0;
}

static int               _unregister_conntrack_hook( JNIEnv* env )
{
    _verify_netcap_initialized();

    jobject *global_hook = &_jnetcap.java.conntrack_hook;
    netcap_conntrack_hook_unregister();
    
    if ( *global_hook != NULL ) {
        (*env)->DeleteGlobalRef( env, *global_hook );
        *global_hook = NULL;
    }
    
    return 0;
}

/**
 * This is a wrapper function that just attaches/deattaches before/after calling the specified funciton
 */
static void*             _run_thread( void* _arg )
{
    pthread_params_t arg;
    void* ret;
    JavaVM* jvm;

    if ( _arg == NULL ) return errlogargs_null();

    if ( ((pthread_params_t*)_arg)->thread_func == NULL ) return errlogargs_null();

    memcpy( &arg, _arg, sizeof( pthread_params_t ));
    
    _pthread_params_raze( _arg );

    /* Attach the thread to the VM */
    if (( jvm = jmvutil_get_java_vm()) == NULL )
        return errlog_null( ERR_CRITICAL, "jmvutil_get_java_vm\n" );

    /* Run the required function */
    ret = arg.thread_func( arg.arg );

    /* Detach the thread from the VM */
    _detach_thread( jvm );

    _pthread_params_destroy( &arg );

    return ret;
}

static int _my_pthread_create( pthread_t* thread, const pthread_attr_t* attr, void* (*func)(void*), void* argument)
{
    pthread_params_t* arg = _pthread_params_create( func, argument );
    int ret;
    
    if ( arg == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "pthread_params_create\n" );
    }
                
    if ( (ret = pthread_create( thread, attr, _run_thread, arg ))) {
        perrlog( "pthread_create" );
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "pthread_create\n" );
    }

    return ret;
}

static pthread_params_t* _pthread_params_malloc()
{
    pthread_params_t* pthread_params;
    if (( pthread_params = calloc ( 1, sizeof( pthread_params_t ))) == NULL ) return errlogmalloc_null();
    
    return pthread_params;
}

static int               _pthread_params_init( pthread_params_t* input, void* (*thread_func)(void*), void* arg )
{
    if ( input == NULL || thread_func == NULL ) return errlogargs();
    
    input->thread_func = thread_func;
    input->arg = arg;

    return 0;
}

static pthread_params_t* _pthread_params_create( void* (*thread_func)(void*), void* arg )
{
    pthread_params_t* pthread_params;
    
    if (( pthread_params = _pthread_params_malloc()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "pthread_params_malloc\n" );
    }
        
    if ( _pthread_params_init( pthread_params, thread_func, arg ) < 0 ) {
        return errlog_null( ERR_CRITICAL, "pthread_params_init\n" );
    }
    
    return pthread_params;
}

static void              _pthread_params_free    ( pthread_params_t* input )
{
    if ( input == NULL ) return (void)errlogargs();
    
    free ( input );
}

static void              _pthread_params_destroy ( pthread_params_t* input )
{
    if ( input == NULL ) return (void)errlogargs();
}

static void              _pthread_params_raze    ( pthread_params_t* input )
{
    if ( input == NULL ) return (void)errlogargs();

    _pthread_params_destroy(input);
    _pthread_params_free(input);
}

static int _increment_session_count()
{
    int ret = 0;
    if ( pthread_mutex_lock( &_jnetcap.session_mutex ) < 0 ) perrlog( "pthread_mutex_lock" );
    
    /**
     * if there is no session_limit, or we have no yet hit the session limit
     * accept and return updated session count
     * otherwise return -1
     */
    if ( _jnetcap.session_limit < 1 || _jnetcap.session_count < _jnetcap.session_limit )
        _jnetcap.session_count++;
    else
        ret = -1;
                                                              
    if ( pthread_mutex_unlock( &_jnetcap.session_mutex ) < 0 ) perrlog( "pthread_mutex_unlock" );
    return ret;
}

static int _decrement_session_count()
{
    if ( pthread_mutex_lock( &_jnetcap.session_mutex ) < 0 ) perrlog( "pthread_mutex_lock\n" );
    
    if ( _jnetcap.session_count < 0 ) {
        errlog( ERR_CRITICAL, "_jnetcap.session_count < 0\n" );
        _jnetcap.session_count = 0;
    } else _jnetcap.session_count--;
    
    if ( pthread_mutex_unlock( &_jnetcap.session_mutex ) < 0 ) perrlog( "pthread_mutex_unlock\n" );
    return 0;
}

