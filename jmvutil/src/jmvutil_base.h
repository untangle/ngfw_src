
/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

#ifndef __JMVUTIL_BASE_H_
#define __JMVUTIL_BASE_H_

#include "jmvutil_error.h"

typedef struct {
    JNIEnv* env;
} jmvutil_tls_t;

typedef struct {
    jmvutil_tls_t base;
    jerror_tls_t  error;
} jmvutil_global_tls_t;

jmvutil_global_tls_t* _jmvutil_tls_get( void );
 
#endif  // __JMVUTIL_H_

