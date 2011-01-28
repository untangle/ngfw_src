
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

