
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

#ifndef __JMVUTIL_H_
#define __JMVUTIL_H_

#include <jni.h>

#define JMVUTIL_UNINITIALIZED  0xDEADD00D
#define JMVUTIL_INITIALIZED    ~_UNINITIALIZED

int     jmvutil_init        ( void );

/* Returns INITIALIZED if jmvutil and mvutil are unitialized */
int     jmvutil_initialized ( void );

JavaVM* jmvutil_get_java_vm ( void );

JNIEnv* jmvutil_get_java_env( void );

 
#endif  // __JMVUTIL_H_

