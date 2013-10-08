/**
 * $Id: jvector.c 35609 2013-08-13 06:19:27Z dmorris $
 */
#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <libvector.h>
#include <mvutil/debug.h>
#include <mvutil/mvpoll.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <jmvutil.h>

#include <vector/event.h>
#include <vector/source.h>
#include <vector/sink.h>

#include "jvector.h"

#include "com_untangle_jvector_Vector.h"
#include "com_untangle_jvector_Crumb.h"

static struct
{
    JavaVM* jvm;
    
    struct 
    {
        jclass    class;
        jmethodID get_event;
        jmethodID shutdown;
        jmethodID raze;        
    } src;

    struct 
    {
        jclass    class;
        jmethodID send_event;
        jmethodID shutdown;
        jmethodID raze;        
    } snk;

    struct
    {
        jclass    class;
        jmethodID raze;
        jmethodID type;
    } event;

    jobject shutdown_crumb;
    jobject reset_crumb;
} _jvector = {
    .shutdown_crumb = NULL,
    .reset_crumb = NULL
};

#define J_SRC                  "com/untangle/jvector/Source"
#define J_SRC_GET_EVENT        "get_event"
#define J_SRC_GET_EVENT_SIG    "()Lcom/untangle/jvector/Crumb;"
#define J_SRC_SHUTDOWN         "shutdown"
#define J_SRC_SHUTDOWN_SIG     "()I"
#define J_SRC_RAZE             "sourceRaze"
#define J_SRC_RAZE_SIG         "()V"

#define J_SNK                  "com/untangle/jvector/Sink"
#define J_SNK_SEND_EVENT       "send_event"
#define J_SNK_SEND_EVENT_SIG   "(Lcom/untangle/jvector/Crumb;)I"
#define J_SNK_SHUTDOWN         "shutdown"
#define J_SNK_SHUTDOWN_SIG     "()I"
#define J_SNK_RAZE             "sinkRaze"
#define J_SNK_RAZE_SIG         "()V"

#define J_EVENT                "com/untangle/jvector/Crumb"
#define J_EVENT_RAZE           "raze"
#define J_EVENT_RAZE_SIG       "()V"
#define J_EVENT_TYPE           "type"
#define J_EVENT_TYPE_SIG       "()I"

#define J_SHUTDOWN_CRUMB       "com/untangle/jvector/ShutdownCrumb"
#define J_RESET_CRUMB          "com/untangle/jvector/ResetCrumb"

#define J_GET_INSTANCE        "getInstance"
#define J_GET_INSTANCE_SIG    "()"


/* XXX Probably want to move this to jmvutil */
static jmethodID _get_global_java_mid( jclass class, const char* method_name, const char* method_sig );

/* This returns a global reference */
static jclass _get_global_java_class( const char* name );


static event_action_t    _sink_send_event     ( sink_t* snk, event_t* event );
static mvpoll_key_t*     _sink_get_event_key  ( sink_t* snk );
static int               _sink_shutdown       ( sink_t* snk );
static void              _sink_raze           ( sink_t* snk );

static void              _source_raze         ( source_t* src );
static int               _source_shutdown     ( source_t* src );
static mvpoll_key_t*     _source_get_event_key( source_t* snk );
static event_t*          _source_get_event    ( source_t* src );

static void              _event_raze          ( event_t* ev );

static __inline__ int _init_java_call( void* input, JNIEnv** env, jmethodID mid, const char *msg )
{
    if ( input == NULL ) return errlogargs();

    if ( (*env = jmvutil_get_java_env()) == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );

    debug( 10,"JVECTOR: '%s'\n", msg );

    if ( mid == NULL ) return errlog(ERR_CRITICAL,"Method not found\n");
    
    return 0;
}

int               jvector_load                ( JNIEnv* env )
{
    bzero( &_jvector, sizeof( _jvector ));

    /* temp for getting the shutdown and reset crumbs */
    jclass class;
    jmethodID mid;
    jobject tmp;
    JavaVM* jvm;

    /* JMVUTIL is protected against being initialized multiple times and also initializes libmvutil */
    if ( jmvutil_init() < 0 ) return errlog( ERR_CRITICAL, "jmvutil_init\n" );
    
    debug_set_mylevel( 0 );
    debug_set_level( UTIL_DEBUG_PKG, 0 );
    debug_set_level( VECTOR_DEBUG_PKG, 0 );

    debug( 8, "JVECTOR: Loading.\n" );

    if ( env == NULL ) return errlogargs();

    if (( jvm = jmvutil_get_java_vm()) == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_vm\n" );

    /* Initialize the error routines */
    
    /* Initialize all of the class and method ids */
    if (( _jvector.src.class = _get_global_java_class( J_SRC )) == NULL ) return JNI_ERR;

    _jvector.src.get_event = _get_global_java_mid( _jvector.src.class, J_SRC_GET_EVENT, J_SRC_GET_EVENT_SIG );

    _jvector.src.shutdown = _get_global_java_mid( _jvector.src.class, J_SRC_SHUTDOWN, J_SRC_SHUTDOWN_SIG );

    _jvector.src.raze = _get_global_java_mid( _jvector.src.class, J_SRC_RAZE, J_SRC_RAZE_SIG );

    /* If any of the MIDs are NULL, return an error */
    if (( _jvector.src.get_event == NULL ) || ( _jvector.src.shutdown == NULL ) || 
        ( _jvector.src.raze == NULL )) {
        return JNI_ERR;
    }

    if (( _jvector.snk.class = _get_global_java_class( J_SNK )) == NULL ) return JNI_ERR;

    _jvector.snk.send_event = _get_global_java_mid( _jvector.snk.class, J_SNK_SEND_EVENT, J_SNK_SEND_EVENT_SIG );

    _jvector.snk.shutdown = _get_global_java_mid( _jvector.snk.class, J_SNK_SHUTDOWN, J_SNK_SHUTDOWN_SIG );

    _jvector.snk.raze = _get_global_java_mid( _jvector.snk.class, J_SNK_RAZE, J_SNK_RAZE_SIG );

    /* If any of the MIDs are NULL, return an error */
    if (( _jvector.snk.send_event == NULL ) || ( _jvector.snk.shutdown == NULL ) || 
        ( _jvector.snk.raze == NULL )) {
        return JNI_ERR;
    }
    
    if (( _jvector.event.class = _get_global_java_class( J_EVENT )) == NULL ) return JNI_ERR;

    _jvector.event.raze = _get_global_java_mid( _jvector.event.class, J_EVENT_RAZE, J_EVENT_RAZE_SIG );
    _jvector.event.type = _get_global_java_mid( _jvector.event.class, J_EVENT_TYPE, J_EVENT_TYPE_SIG );

    if (( _jvector.event.raze == NULL ) || ( _jvector.event.type == NULL )) return JNI_ERR;

    /* Get global references to the shutdown and reset crumbs */
    if (( class =(*env)->FindClass( env, J_SHUTDOWN_CRUMB )) == NULL ) {
        errlog( ERR_CRITICAL, "FindClass\n" );
        return JNI_ERR;
    }

    mid = (*env)->GetStaticMethodID( env, class, J_GET_INSTANCE,  "()L" J_SHUTDOWN_CRUMB ";" );

    if ( mid == NULL ) {
        errlog( ERR_CRITICAL, "GetMethodID\n" );
        return JNI_ERR;
    }

    tmp = (*env)->CallStaticObjectMethod( env, class, mid );

    if (( jmvutil_error_exception_clear() < 0 ) || ( tmp == NULL )) {
        errlog( ERR_CRITICAL, "CallStaticObjectMethod\n" );
        return JNI_ERR;
    }

    if (( _jvector.shutdown_crumb = (*env)->NewGlobalRef( env, tmp )) == NULL) {
        errlog( ERR_CRITICAL, "NewGlobalRef\n" );
        return JNI_ERR;
    }

    if (( class =(*env)->FindClass( env, J_RESET_CRUMB )) == NULL ) {
        errlog( ERR_CRITICAL, "FindClass\n" );
        return JNI_ERR;
    }

    mid = (*env)->GetStaticMethodID( env, class, J_GET_INSTANCE,  "()L" J_RESET_CRUMB ";" );
    if ( mid == NULL ) {
        errlog( ERR_CRITICAL, "GetMethodID\n" );
        return JNI_ERR;
    }

    tmp = (*env)->CallStaticObjectMethod( env, class, mid );

    if (( jmvutil_error_exception_clear() < 0 ) || ( tmp == NULL )) {
        errlog( ERR_CRITICAL, "CallStaticObjectMethod\n" );
        return JNI_ERR;
    }

    if (( _jvector.reset_crumb = (*env)->NewGlobalRef( env, tmp )) == NULL) {
        errlog( ERR_CRITICAL, "NewGlobalRef\n" );
        return JNI_ERR;
    }
   
    debug( 8, "JVECTOR: Loaded\n" );

    return JNI_VERSION_1_2;
}

jvector_sink_t*   jvector_sink_malloc         ( void )
{
    jvector_sink_t* snk;
    if (( snk = malloc( sizeof( jvector_sink_t ))) == NULL ) return errlogmalloc_null();
    return snk;
}

int               jvector_sink_init           ( jvector_sink_t* snk, jobject this, mvpoll_key_t* key )
{
    JNIEnv* env = jmvutil_get_java_env();
    jclass  class;

    if ( env == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );

    if ( snk == NULL || key == NULL ) return errlogargs();

    /* Retrieve all of the method ids */
    if (( class = (*env)->GetObjectClass( env, this )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "GetObjectClass\n" );
    }

    snk->mid.send_event = (*env)->GetMethodID( env, class, J_SNK_SEND_EVENT, J_SNK_SEND_EVENT_SIG );
    snk->mid.shutdown = (*env)->GetMethodID( env, class, J_SNK_SHUTDOWN, J_SNK_SHUTDOWN_SIG );
    snk->mid.raze = (*env)->GetMethodID( env, class, J_SNK_RAZE, J_SNK_RAZE_SIG );
    
    if (( snk->mid.send_event == NULL ) || ( snk->mid.shutdown == NULL ) || ( snk->mid.raze == NULL )) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to locate a method id\n" );
    }
    
    snk->snk.send_event    = _sink_send_event;
    snk->snk.get_event_key = _sink_get_event_key;
    snk->snk.shutdown      = _sink_shutdown;
    snk->snk.raze          = _sink_raze;
    
    /* Create a reference for this, this could be a weak global reference which means that
     * it will automatically be garbage collected, we use a global reference and explicitly
     * delete it in the raze function */
    snk->this = (*env)->NewGlobalRef( env, this );
    snk->key  = key;

    (*env)->DeleteLocalRef( env, this );

    return 0;
}

jvector_sink_t*   jvector_sink_create         ( jobject this, mvpoll_key_t* key )
{
    jvector_sink_t* snk;

    if (( snk = jvector_sink_malloc()) == NULL ) return errlog_null( ERR_CRITICAL, "jvector_sink_malloc\n" );
    
    if ( jvector_sink_init( snk, this, key ) < 0 ) {
        free( snk );
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jvector_sink_init\n" );
    }
    
    return snk;
}

jvector_source_t* jvector_source_malloc       ( void )
{
    jvector_source_t* src;

    if (( src = malloc( sizeof( jvector_source_t ))) == NULL ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_FATAL, "malloc\n" );
    }
    return src;
}

int               jvector_source_init         ( jvector_source_t* src, jobject this, mvpoll_key_t* key )
{
    JNIEnv* env = jmvutil_get_java_env();
    jclass  class;

    if ( env == NULL ) return errlog( ERR_CRITICAL, "jmvutil_get_java_env\n" );

    if ( src == NULL || key == NULL ) return errlogargs();

    /* Retrieve all of the method ids */
    if (( class = (*env)->GetObjectClass( env, this )) == NULL ) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "GetObjectClass\n" );
    }

    src->mid.get_event = (*env)->GetMethodID( env, class, J_SRC_GET_EVENT, J_SRC_GET_EVENT_SIG );
    src->mid.shutdown = (*env)->GetMethodID( env, class, J_SRC_SHUTDOWN, J_SRC_SHUTDOWN_SIG );
    src->mid.raze = (*env)->GetMethodID( env, class, J_SRC_RAZE, J_SRC_RAZE_SIG );
    
    if (( src->mid.get_event == NULL ) || ( src->mid.shutdown == NULL ) || ( src->mid.raze == NULL )) {
        return jmvutil_error( JMVUTIL_ERROR_STT, ERR_CRITICAL, "Unable to locate a method id\n" );
    }
    
    src->src.get_event     = _source_get_event;
    src->src.get_event_key = _source_get_event_key;
    src->src.shutdown      = _source_shutdown;
    src->src.raze          = _source_raze;

    /* See the comment above about global references */
    /* XX Possible add a check to make sure that the class implements the event method */
    src->this = (*env)->NewGlobalRef( env, this );
    src->key  = key;

    (*env)->DeleteLocalRef( env, this );

    return 0;
}

jvector_source_t* jvector_source_create       ( jobject this, mvpoll_key_t* key )
{
    jvector_source_t* src;
        
    if (( src = jvector_source_malloc()) == NULL ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jvector_source_malloc\n" );
    }
    
    if ( jvector_source_init( src, this, key ) < 0 ) {
        free( src );
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jvector_source_init\n" );
    }
    
    return src;
}

jvector_event_t* jvector_event_malloc       ( void )
{
    jvector_event_t* event;
    if (( event = malloc( sizeof( jvector_event_t ))) == NULL ) return errlogmalloc_null();
    return event;
}

int               jvector_event_init         ( jvector_event_t* jv_event )
{
    if ( jv_event == NULL ) return errlogargs();

    jv_event->ev.type = JV_EVENT_TYPE;
    jv_event->ev.raze = _event_raze;
    
    return 0;
}

jvector_event_t*  jvector_event_create       ( void )
{
    jvector_event_t* jv_event;

    if (( jv_event = jvector_event_malloc()) == NULL ) {
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jvector_event_malloc\n" );
    }
    
    if ( jvector_event_init( jv_event ) < 0 ) {
        free( jv_event );
        return jmvutil_error_null( JMVUTIL_ERROR_STT, ERR_CRITICAL, "jvector_event_init\n" );
    }
    
    return jv_event;
}

static event_action_t    _sink_send_event     ( sink_t* snk, event_t* event )
{
    jvector_sink_t*  jv_snk = (jvector_sink_t*)snk;
    jvector_event_t* jv_event = (jvector_event_t*)event;
    event_action_t action;
    JNIEnv* env;
    jmethodID mid = NULL;

    if ( jv_snk == NULL ) return EVENT_ACTION_ERROR;
    mid = jv_snk->mid.send_event;
    if ( _init_java_call( jv_snk, &env, mid, "sink.send_event" ) < 0 ) return EVENT_ACTION_ERROR;
    
    if ( jv_event == NULL ) {
        errlog( ERR_CRITICAL, "NULL Event" );
        return EVENT_ACTION_ERROR;
    }

    /**
     * XXX arg? ??? ??? *
     * RBS - Not quite sure what this is about
     */
    switch ( jv_event->ev.type & EVENT_TYPE_MASK ) {
    case JV_EVENT_TYPE:
        action = (*env)->CallIntMethod( env, jv_snk->this, mid, jv_event->obj );

        if ( jmvutil_error_exception_clear() < 0 ) {
            return (event_action_t)errlog( ERR_CRITICAL, "Exception occured while sending data crumb\n" );
        }

        return action;

    case EVENT_BASE_TYPE:
        if (( jv_event->ev.type & EVENT_SHUTDOWN_ERROR_MASK ) == EVENT_SHUTDOWN_ERROR_MASK ) {
            /* Send a reset crumb */
            (*env)->CallIntMethod( env, jv_snk->this, mid, _jvector.reset_crumb );
            
            if ( jmvutil_error_exception_clear() < 0 ) {
                errlog( ERR_CRITICAL, "Exception occured while sending reset crumb\n" );
            }
            
            action = EVENT_ACTION_SHUTDOWN;
        } else if ( jv_event->ev.type & EVENT_SHUTDOWN_MASK ) {
            /* Send a shutdown crumb */
            (*env)->CallIntMethod( env, jv_snk->this, mid, _jvector.shutdown_crumb );

            if ( jmvutil_error_exception_clear() < 0 ) {
                errlog( ERR_CRITICAL, "Exception occured while sending shutdown crumb\n" );
            }

            action = EVENT_ACTION_SHUTDOWN;
        } else {
            errlog( ERR_CRITICAL, "Invalid event type: %d\n", jv_event->ev.type );
            action = EVENT_ACTION_ERROR;
        }
        
        return action;

    default:
        errlog( ERR_CRITICAL, "Invalid event type: %d\n", jv_event->ev.type );
    }

    return EVENT_ACTION_ERROR;
}

static mvpoll_key_t*     _sink_get_event_key  ( sink_t* snk )
{
    if ( snk == NULL ) return errlogargs_null();

    return ((jvector_sink_t*)snk)->key;
}

static int               _sink_shutdown       ( sink_t* snk )
{
    jvector_sink_t*  jv_snk = (jvector_sink_t*)snk;
    JNIEnv* env;
    int ret;
    jmethodID mid;

    if ( jv_snk == NULL ) return EVENT_ACTION_ERROR;
    mid = jv_snk->mid.shutdown;

    if ( _init_java_call( jv_snk, &env, mid, "sink.shutdown" ) < 0 ) return EVENT_ACTION_ERROR;
    
    ret = (int)(*env)->CallIntMethod( env, jv_snk->this, mid );

    if ( jmvutil_error_exception_clear() < 0 ) {
        return errlog( ERR_CRITICAL, "Exception occured in sink.shutdown\n" );
    }

    return ret;
}

static void              _sink_raze           ( sink_t* snk )
{
    jvector_sink_t*  jv_snk = (jvector_sink_t*)snk;
    JNIEnv* env;
    jmethodID mid;

    if ( jv_snk == NULL ) return;
    
    mid = jv_snk->mid.raze;

    if ( _init_java_call( jv_snk, &env, mid, "sink.sinkRaze" ) < 0 ) return;

    (*env)->CallVoidMethod( env, jv_snk->this, mid );

    jmvutil_error_exception_clear();

    /* Remove the global references */
    if ( jv_snk->this != NULL ) 
        (*env)->DeleteGlobalRef( env, (jobject)jv_snk->this );

    jv_snk->this = NULL;

    /* Remove the key */
    if ( jv_snk->key != NULL )
        mvpoll_key_raze( jv_snk->key );

    jv_snk->key = NULL;
    
    /* Free the sink */
    free( snk );
}

static void              _source_raze         ( source_t* src )
{
    jvector_source_t*  jv_src = (jvector_source_t*)src;
    JNIEnv* env;
    jmethodID mid;

    if ( jv_src == NULL ) return;

    mid = jv_src->mid.raze;

    if ( _init_java_call( jv_src, &env, mid, "source.sourceRaze" ) < 0 ) return;
    
    /* ??? Could also restructure so that a CallVoidMethod wrapper would handle 
     * clearing the exception based on the user input */
    (*env)->CallVoidMethod( env, jv_src->this, mid );

    jmvutil_error_exception_clear();

    /* Remove the global references */
    if ( jv_src->this != NULL ) 
        (*env)->DeleteGlobalRef( env, jv_src->this );

    /* Remove the key */
    if ( jv_src->key != NULL ) {
        mvpoll_key_raze( jv_src->key );
        jv_src->key = NULL;
    }
        
    /* Free the source */
    free( src );
}

static mvpoll_key_t*     _source_get_event_key( source_t* src )
{
    if ( src == NULL ) return errlogargs_null();

    return ((jvector_source_t*)src)->key;
}

static int               _source_shutdown     ( source_t* src )
{
    jvector_source_t*  jv_src = (jvector_source_t*)src;
    JNIEnv* env;
    int ret;
    jmethodID mid;

    if ( jv_src == NULL ) return EVENT_ACTION_ERROR;
    mid = jv_src->mid.shutdown;
    if ( _init_java_call( jv_src, &env, mid, "source.shutdown" ) < 0 ) return EVENT_ACTION_ERROR;
    
    ret = (int)(*env)->CallIntMethod( env, jv_src->this, mid );
    if ( jmvutil_error_exception_clear() < 0 ) {
        return errlog( ERR_CRITICAL, "Exception occured in source.shutdonw" );
    }

    return ret;
}

static event_t*          _source_get_event    ( source_t* src )
{
    jvector_source_t* jv_src = (jvector_source_t*)src;
    jvector_event_t*  jv_event;
    jmethodID mid;
    jobject   crumb;
    jobject   crumb_local;
    jint type;
    event_type_t event_type = 0;

    JNIEnv* env;
    
    if ( jv_src == NULL ) return NULL;
    mid = jv_src->mid.get_event;
    if ( _init_java_call( jv_src, &env, mid, "source.get_event" ) < 0 ) return NULL;
    
    crumb = (*env)->CallObjectMethod( env, jv_src->this, mid );

    /* Check if there was an exception */
    if ( jmvutil_error_exception_clear() < 0 ) {
        return errlog_null( ERR_CRITICAL, "Exception occured while getting crumb\n" );
    }
    
    if (( mid = _jvector.event.type ) == NULL ) return errlog_null( ERR_CRITICAL, "Missing get_type MID\n" );

    /* Get the type of the crumb */
    type = (*env)->CallIntMethod( env, crumb, mid );

    if ( jmvutil_error_exception_clear() < 0 ) {
        return errlog_null( ERR_CRITICAL, "Exception occured while geting crumb type\n" );
    }

    event_type = 0;

    switch ( type ) {
    case com_untangle_jvector_Crumb_TYPE_RESET:    event_type |= EVENT_SHUTDOWN_ERROR_MASK; /*fallthrough*/
    case com_untangle_jvector_Crumb_TYPE_SHUTDOWN: event_type |= EVENT_SHUTDOWN_MASK;       /*fallthrough*/

    case com_untangle_jvector_Crumb_TYPE_UDP_PACKET: /*fallthrough*/
    case com_untangle_jvector_Crumb_TYPE_ICMP_PACKET: /*fallthrough*/        
    case com_untangle_jvector_Crumb_TYPE_DATA:
        if (( jv_event = jvector_event_create()) == NULL )
            return errlog_null( ERR_CRITICAL, "jvector_event_create\n" );

        /* ORR in the possible shutdown masks */
        jv_event->ev.type |= event_type;
        
        crumb_local = crumb;
        
        if (( crumb = (*env)->NewGlobalRef( env, crumb_local )) == NULL )
            return errlog_null( ERR_CRITICAL, "NewGlobalRef\n" );

        (*env)->DeleteLocalRef( env, crumb_local );

        jv_event->obj = crumb;
        break;

    default:
        return errlog_null( ERR_CRITICAL, "Unknown crumb type: %d\n", type );
    }
    
    return (event_t*)jv_event;
}


static void              _event_raze          ( event_t* event )
{
    jvector_event_t* jv_event = (jvector_event_t*)event;
    jmethodID        mid = _jvector.event.raze;
    JNIEnv* env = NULL;

    if ( _init_java_call( jv_event, &env, mid, "event.raze" ) < 0 ) return;

    if (( jv_event->ev.type & EVENT_TYPE_MASK ) != JV_EVENT_TYPE ) {
        return (void)errlog( ERR_CRITICAL, "Invalid event type: 0x%08x\n", jv_event->ev.type );
    }
    
    (*env)->CallVoidMethod( env, jv_event->obj, mid );

    jmvutil_error_exception_clear();

    /* Remove the global references */
    if ( jv_event->obj != NULL )
        (*env)->DeleteGlobalRef( env, jv_event->obj );

    free( jv_event );
}

static jmethodID _get_global_java_mid( jclass class, const char* method_name, const char* method_sig )
{
    JNIEnv* env;
    jmethodID mid;

    if (( env = jmvutil_get_java_env()) == NULL ) return errlog_null( ERR_CRITICAL, "jmvutil_get_java_env\n" );

    if (( mid = (*env)->GetMethodID( env, class, method_name, method_sig )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "GetMethodID: %s.%s", method_name, method_sig );
    }
    
    return mid;
}

/* This returns a global reference */
static jclass _get_global_java_class( const char* name )
{
    jclass tmp, global;
    JNIEnv* env;
    if (( env = jmvutil_get_java_env()) == NULL ) return errlog_null( ERR_CRITICAL, "jmvutil_get_java_env\n" );
    
    if ((tmp = (*env)->FindClass( env, name )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "FindClass: %s\n", name );
    }
    
    if ((global = (*env)->NewGlobalRef( env, tmp )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "NewGlobalRef\n" );
    }

    (*env)->DeleteLocalRef( env, tmp );
    
    return global;
}

