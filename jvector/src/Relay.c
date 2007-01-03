/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

#include <vector/relay.h>

#include "jni_header.h"

#include JH_Relay

/*
 * Class:     Relay
 * Method:    relay_create
 * Signature: ()I
 */
JNIEXPORT jint JNICALL JF_Relay( relay_1create )
    (JNIEnv * env, jobject _this)
{
    return (jint)relay_create();
}

/*
 * Class:     Relay
 * Method:    relay_free
 * Signature: (I)V
 */
JNIEXPORT void JNICALL JF_Relay( relay_1free )
    (JNIEnv * env, jobject _this, jint relay_ptr)
{
    relay_free((relay_t*) relay_ptr);
}

/*
 * Class:     Relay
 * Method:    relay_set_src
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Relay( relay_1set_1src )
    (JNIEnv * env, jobject _this, jint relay_ptr, jint src_ptr)
{
    relay_set_src((relay_t*) relay_ptr, (source_t*)src_ptr);

}

/*
 * Class:     Relay
 * Method:    relay_set_snk
 * Signature: (II)V
 */
JNIEXPORT void JNICALL JF_Relay( relay_1set_1snk )
    (JNIEnv * env, jobject _this, jint relay_ptr, jint snk_ptr)
{
    relay_set_snk((relay_t*) relay_ptr, (sink_t*)snk_ptr);
}

