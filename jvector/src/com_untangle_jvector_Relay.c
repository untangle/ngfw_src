/**
 * $Id$
 */
#include <vector/relay.h>


#include "com_untangle_jvector_Relay.h"

/*
 * Class:     Relay
 * Method:    relay_create
 * Signature: ()I
 */
JNIEXPORT jlong JNICALL Java_com_untangle_jvector_Relay_relay_1create
    (JNIEnv * env, jobject _this)
{
    return (jlong)(uintptr_t)relay_create();
}

/*
 * Class:     Relay
 * Method:    relay_free
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_Relay_relay_1free
    (JNIEnv * env, jobject _this, jlong relay_ptr)
{
    relay_free((relay_t*)(uintptr_t)relay_ptr);
}

/*
 * Class:     Relay
 * Method:    relay_set_src
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_Relay_relay_1set_1src
    (JNIEnv * env, jobject _this, jlong relay_ptr, jlong src_ptr)
{
    relay_set_src((relay_t*)(uintptr_t)relay_ptr, (source_t*)(uintptr_t)src_ptr);

}

/*
 * Class:     Relay
 * Method:    relay_set_snk
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_com_untangle_jvector_Relay_relay_1set_1snk
    (JNIEnv * env, jobject _this, jlong relay_ptr, jlong snk_ptr)
{
    relay_set_snk((relay_t*)(uintptr_t)relay_ptr, (sink_t*)(uintptr_t)snk_ptr);
}

