/**
 * $Id: global.h 35567 2013-08-08 07:47:12Z dmorris $
 */
#include <jni.h>

typedef struct sq_event {
    event_t ev;
    jobject obj;
} sq_event_t;

JNIEnv* jvector_get_java_env( void );

#define SQ_EVENT_TYPE 133

