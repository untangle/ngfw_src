/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

