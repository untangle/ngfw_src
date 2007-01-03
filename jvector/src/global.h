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

#include <jni.h>

typedef struct sq_event {
    event_t ev;
    jobject obj;
} sq_event_t;

JNIEnv* jvector_get_java_env( void );

#define SQ_EVENT_TYPE 133

