/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: global.h,v 1.3 2005/01/05 21:26:19 rbscott Exp $
 */

#include <jni.h>

typedef struct sq_event {
    event_t ev;
    jobject obj;
} sq_event_t;

JNIEnv* jvector_get_java_env( void );

#define SQ_EVENT_TYPE 133

