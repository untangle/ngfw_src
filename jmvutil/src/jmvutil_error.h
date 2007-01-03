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

#ifndef _JMVUTIL_ERROR_H_
#define _JMVUTIL_ERROR_H_

#include "jmvutil.h"

#define THROW_MSG_MAX_LEN 64

typedef struct {
    int current;
    char buffer[THROW_MSG_COUNT][THROW_MSG_MAX_LEN];
} jerror_tls_t;

int _jmvutil_error_init();

int _jmvutil_error_tls_init( jerror_tls_t* tls );

#endif 
