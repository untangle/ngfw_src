
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

#ifndef __JMVUTIL_H_
#define __JMVUTIL_H_

#include <jni.h>
#define JMVUTIL_UNINITIALIZED  0xDEADD00D
#define JMVUTIL_INITIALIZED    ~JMVUTIL_UNINITIALIZED

#define THROW_MSG_COUNT        4

int     jmvutil_init        ( void );

int     jmvutil_cleanup     ( void );

/* Returns INITIALIZED if jmvutil and mvutil are unitialized */
int     jmvutil_initialized ( void );

JavaVM* jmvutil_get_java_vm ( void );

JNIEnv* jmvutil_get_java_env( void );

typedef enum
{
    JMVUTIL_ERROR_ARGS,
    JMVUTIL_ERROR_STT
} jmvutil_err_t;

/**
 * Usage: jmvutil_error( env, type, "Format", args... )
 * This uses the arguments twice, so make sure there are no ++, or -- statements.
 * lpszFmt must be a constant string, it cannot be a variable.
 *   You can throw at most THROW_MSG_COUNT exceptions before you start reusing buffers
 *   Also, if there is a pending exception, a new exception is not thrown.
 */
#define jmvutil_error(type,MV_ERR,lpszFmt,...) \
        errlog(MV_ERR,jmvutil_error_throw((type),lpszFmt, ## __VA_ARGS__), ## __VA_ARGS__)


#define jmvutil_error_null(type,MV_ERR,lpszFmt,...) \
        errlog_null(MV_ERR,jmvutil_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

#define jmvutil_error_void(type,MV_ERR,lpszFmt,...) \
        (void)errlog(MV_ERR,jmvutil_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

/** Exception handling, both functions return 0 on no exceptions and -1 otherwise */

/** This just checks for an exception, but it will not clear it */
int jmvutil_error_exception      ( void );

/** This checks for an exception, describes it and then clears it */
int jmvutil_error_exception_clear( void );

/**
 * Usage: Don't, use one of the jmvutil_error functions.
 *        Use this function only if you want to throw an error without printing an error message.
 */
char* jmvutil_error_throw        ( jmvutil_err_t type, const char* format, ... );

#endif  // __JMVUTIL_H_

