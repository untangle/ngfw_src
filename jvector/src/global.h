/**
 * $Id$
 */
#include <jni.h>

typedef struct sq_event {
    event_t ev;
    jobject obj;
} sq_event_t;

JNIEnv* jvector_get_java_env( void );

#define SQ_EVENT_TYPE 133

