#ifndef _JNETCAP_ERROR_H_
#define _JNETCAP_ERROR_H_

#include <jni.h>

typedef enum
{
    JVECTOR_ERROR_ARGS,
    JVECTOR_ERROR_STT
} jvector_err_t;

#define THROW_MSG_COUNT    4

int  jvector_error_init( void );

/**
 * Usage: jvector_error( env, type, "Format", args... )
 * This uses the arguments twice, so make sure there are no ++, or -- statements.
 * lpszFmt must be a constant string, it cannot be a variable.
 *   You can throw at most THROW_MSG_COUNT exceptions before you start reusing buffers
 *   Also, if there is a pending exception, a new exception is not thrown.
 */

#define jvector_error(type,MV_ERR,lpszFmt,...) \
        errlog(MV_ERR,jvector_error_throw((type),lpszFmt, ## __VA_ARGS__), ## __VA_ARGS__)


#define jvector_error_null(type,MV_ERR,lpszFmt,...) \
        errlog_null(MV_ERR,jvector_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

#define jvector_error_void(type,MV_ERR,lpszFmt,...) \
        (void)errlog(MV_ERR,jvector_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

/** Exception handling, both functions return 0 on no exceptions and -1 otherwise */

/** This just checks for an exception, but it will not clear it */
int jvector_exception( void );

/** This checks for an exception, describes it and then clears it */
int jvector_exception_clear( void );

/**
 * Usage: Don't, use one of the jvector_error functions 
 */
char* jvector_error_throw( jvector_err_t type, const char* format, ... );

#endif 
