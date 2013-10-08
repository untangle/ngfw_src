/* $HeadURL: svn://chef/work/src/jnetcap/src/jnetcap.c $ */
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
#include <libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/uthread.h>
#include <jmvutil.h>

#include "jnetcap.h"
#include "com_untangle_jnetcap_Netcap.h"

/* Too hard to include kernel include files, so we just define it again here. XXX */
#ifndef SCHED_NORMAL
#define SCHED_NORMAL		0
#endif

#define _verify_mvutil_initialized()    if ( _jnetcap.mvutil != _INITIALIZED ) return -1
#define _verify_netcap_initialized()    if ( _jnetcap.netcap != _INITIALIZED ) return -1
#define _verify_scheduler_initialized() if ( _jnetcap.scheduler != _INITIALIZED ) return -1

/* Verify that the MAX_INTERFACES constant is set properly */
#if JN_Netcap( MAX_INTERFACE ) != NETCAP_MAX_INTERFACES
#error MAX_INTERFACES
#endif

#define _HOOK_OBJ_STR     JP_BUILD_NAME( NetcapCallback )
#define _HOOK_METHOD_NAME "event"
#define _HOOK_METHOD_DESC "(J)V"

/* default session limit ( 0 means no limit ) */
#define _SESSION_LIMIT_DEFAULT 0

/* WARN: These are overriden by the netcap property, so ignore them */
#define _NEW_SESSION_SCHED_POLICY_DEFAULT   SCHED_NORMAL
#define _SESSION_SCHED_POLICY_DEFAULT       SCHED_NORMAL

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
    
    int session_limit;

    int new_session_sched_policy;

    int session_sched_policy;
    
    int session_count;

    /* Used to guarantee that session_limit increases and decreases atomically */
    pthread_mutex_t session_mutex; 
    
    /* For outgoing interface lookups, don't want to wait the whole time. */
    unsigned long delay_array[];
} _jnetcap = 
{
    .scheduler = _UNINITIALIZED, 
    .netcap    = _UNINITIALIZED, 
    .mutex     = PTHREAD_MUTEX_INITIALIZER,
    .java = { 
        .hook_class     = NULL,
        .hook_method_id = NULL,
        .tcp_hook       = NULL,
        .udp_hook       = NULL
    },

    .session_limit = _SESSION_LIMIT_DEFAULT,
    .new_session_sched_policy = _NEW_SESSION_SCHED_POLICY_DEFAULT,
    .session_sched_policy = _SESSION_SCHED_POLICY_DEFAULT,
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

static jnetcap_thread_t* jnetcap_thread_malloc();

static void              _udp_hook( netcap_session_t* netcap_session, void* arg );
static void              _tcp_hook( netcap_session_t* netcap_session, void* arg );

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

static int _increment_session_count();
static int _decrement_session_count();

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

            /* Set the method identifier */
            _jnetcap.java.hook_method_id = (*env)->GetMethodID( env, _jnetcap.java.hook_class, _HOOK_METHOD_NAME, _HOOK_METHOD_DESC );

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
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    setNewSessionSchedPolicy
 * Signature: (I)V
 */
JNIEXPORT void JNICALL JF_Netcap( setNewSessionSchedPolicy )
  (JNIEnv *env, jobject _this, jint policy )
{
    debug( 0, "JNETCAP: Setting new session sched policy to %d\n", policy );

    _jnetcap.new_session_sched_policy = policy;
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    setSessionSchedPolicy
 * Signature: (I)V
 */
JNIEXPORT void JNICALL JF_Netcap( setSessionSchedPolicy )
  (JNIEnv *env, jobject _this, jint policy )
{
    debug( 0, "JNETCAP: Setting session sched policy to %d\n", policy );

    _jnetcap.session_sched_policy = policy;
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
        showMemStats( 1 );
#endif
    } while( 0 );

    // muntrace();

    if ( pthread_mutex_unlock ( &_jnetcap.mutex ) < 0 )  perrlog ( "pthread_mutex_unlock" );    
}

/*
 * Class:     com_untangle_jnetcap_Netcap
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
 * Class:     com_untangle_jnetcap_Netcap
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
        arg = jnetcap_thread_create( netcap_thread_donate, NULL, _jnetcap.new_session_sched_policy, NULL );
        
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
 * Class:     com_untangle_jnetcap_Netcap
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
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    registerUDPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerUDPHook )
  (JNIEnv *env, jclass _class, jobject udp_hook )
{
    return _register_hook( env, IPPROTO_UDP, udp_hook );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    registerTCPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( registerTCPHook )
  (JNIEnv *env, jclass _class, jobject tcp_hook )
{
    return _register_hook( env, IPPROTO_TCP, tcp_hook );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    unregisterUDPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterUDPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_hook( env, IPPROTO_UDP );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    unregisterTCPHook
 * Signature: (Lcom/untangle/jnetcap/Hook;)I
 */
JNIEXPORT jint JNICALL JF_Netcap( unregisterTCPHook )
(JNIEnv *env, jclass _class )
{
    return _unregister_hook( env, IPPROTO_TCP );
}

/*
 * Class:     com_untangle_jnetcap_Netcap
 * Method:    cTcpRedirectPorts
 * Signature: ()V;
 */
JNIEXPORT jintArray JNICALL JF_Netcap( cTcpRedirectPorts )
    ( JNIEnv* env, jobject _this )
{
    int ports[2];
    jintArray j_ports;

    if ( netcap_tcp_redirect_ports( &ports[0], &ports[1] ) < 0 ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "netcap_tcp_redirect_ports\n" );
    }

    /** Make an array to return the two values */
    if (( j_ports = (*env)->NewIntArray( env, 2 )) == NULL ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "(*env)->NewByteArray\n" );
    }
    
    (*env)->SetIntArrayRegion( env, j_ports, 0, 2, (jint*)ports );

    return j_ports;
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
    jnetcap_thread_t* thread_arg = NULL;
    debug(10, "FLAG _udp_hook\n");
    if ( netcap_sess == NULL ) { 
        errlogargs();
        return;
    }

    int _critical_section() {
        pthread_t id;
        
        thread_arg = jnetcap_thread_create( _udp_run_thread, netcap_sess, _jnetcap.session_sched_policy, NULL );
        if ( thread_arg == NULL  ) return errlog( ERR_CRITICAL, "jnetcap_thread_create\n" );
        
        if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, thread_arg )) {
            return perrlog( "pthread_create" );
        }

        return 0;
    }

    if ( _increment_session_count() < 0 ) {
        netcap_session_raze( netcap_sess );
        errlog( ERR_CRITICAL, "Hit session limit %d\n", _jnetcap.session_limit );
        return;
    }

    if ( _critical_section() < 0 ) {
        if ( thread_arg != NULL ) jnetcap_thread_raze( thread_arg );
        thread_arg = NULL;
        netcap_session_raze( netcap_sess );
        _decrement_session_count();
    }
}

static void              _tcp_hook( netcap_session_t* netcap_sess, void* arg )
{
    jnetcap_thread_t* thread_arg = NULL;

    if ( netcap_sess == NULL ) { 
        errlogargs();
        return;
    }
    
    int _critical_section() {
        pthread_t id;

        thread_arg = jnetcap_thread_create( _tcp_run_thread, netcap_sess, _jnetcap.session_sched_policy, NULL );
        if ( thread_arg == NULL ) return errlog( ERR_CRITICAL, "jnetcap_thread_create" );

        if ( pthread_create( &id, &uthread_attr.other.medium, _run_thread, thread_arg )) {
            return perrlog( "pthread_create" );
        }

        return 0;
    }

    if ( _increment_session_count() < 0 ) {
        netcap_session_raze( netcap_sess );
        errlog( ERR_CRITICAL, "Hit session limit %d\n", _jnetcap.session_limit );
        return;
    }

    if ( _critical_section() < 0 ) {
        if ( thread_arg != NULL ) jnetcap_thread_raze( thread_arg );
        thread_arg = NULL;
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

        if (( _jnetcap.netcap != _INITIALIZED ) || ( NULL == _jnetcap.java.hook_class ) ||
            ( NULL == _jnetcap.java.hook_method_id )) { 
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
        (*env)->CallVoidMethod( env, global_hook, _jnetcap.java.hook_method_id, netcap_sess->session_id );

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
                errlog( ERR_CRITICAL, "Session (%10u) left in table after hook\n", session_id );
                if ( netcap_sess_cleanup == netcap_sess ) {
                    /* Since the thread was created in C, there is no way that another
                     * thread could possible raze this session */
                    return errlog( ERR_CRITICAL, "Session (%10u) not razed in hook, razing in cleanup\n", session_id );
                } else {
                    netcap_sess = NULL;
                    return errlog( ERR_CRITICAL, "Session (%10u) replaced, assuming previous was razed\n", session_id );
                }
            }
        }

        /* Assume the session has been razed */
        netcap_sess = NULL;
        return 0;
    }
    int ret;
    
    ret = _critical_section();
    
    if (ret < 0 && ( netcap_sess != NULL )) netcap_session_raze( netcap_sess );
    
    /* Make sure to always decrement the session count */
    _decrement_session_count();        
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
    default: return errlog( ERR_CRITICAL, "_register_hook: invalid getProtocol(%d)\n", protocol );
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
        break;

    case IPPROTO_TCP:
        ret = netcap_tcp_hook_register( _tcp_hook );
        break;

    default:
        ret = -1; /* IMPOSSIBLE */

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
        
    default: return errlog( ERR_CRITICAL, "_register_hook: invalid getProtocol(%d)\n", protocol );
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
    struct sched_param empty;
    // int curpolicy = -1;

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
            if ( pthread_setschedparam( id, arg.policy, arg.param ) < 0 ) perrlog( "pthread_setschedparam(param)" );
        } else if ( arg.policy != SCHED_NORMAL ) {
            // This has to happen here, for some reason it doesn't work if we
            // do it at pthread create time. jdi
            id = pthread_self();
            bzero(&empty, sizeof(struct sched_param));
            // pthread_getschedparam(id, &curpolicy, &empty);
            // printf("%d: setting policy from %d to %d\n", id, curpolicy, arg.policy);
            if ( pthread_setschedparam( id, arg.policy, &empty ) < 0 )
                perrlog( "pthread_setschedparam(policy)" ); /* Not fatal */
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

