#ifndef _JNETCAP_ERROR_H_
#define _JNETCAP_ERROR_H_

#include <jni.h>

typedef enum
{
    JNETCAP_ERROR_ARGS,
    JNETCAP_ERROR_STT
} jnetcap_err_t;

#define THROW_MSG_COUNT    4

int  jnetcap_error_init( void );

/**
 * Usage: jnetcap_error( env, type, "Format", args... )
 * This uses the arguments twice, so make sure there are no ++, or -- statements.
 * lpszFmt must be a constant string, it cannot be a variable.
 *   You can throw at most THROW_MSG_COUNT exceptions before you start reusing buffers
 *   Also, if there is a pending exception, a new exception is not thrown.
 */

#define jnetcap_error(type,MV_ERR,lpszFmt,...) \
        errlog(MV_ERR,jnetcap_error_throw((type),lpszFmt, ## __VA_ARGS__), ## __VA_ARGS__)


#define jnetcap_error_null(type,MV_ERR,lpszFmt,...) \
        errlog_null(MV_ERR,jnetcap_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

#define jnetcap_error_void(type,MV_ERR,lpszFmt,...) \
        (void)errlog(MV_ERR,jnetcap_error_throw((type),(lpszFmt), ## __VA_ARGS__), ## __VA_ARGS__)

/** Exception handling, both functions return 0 on no exceptions and -1 otherwise */

/** This just checks for an exception, but it will not clear it */
int jnetcap_exception( void );

/** This checks for an exception, describes it and then clears it */
int jnetcap_exception_clear( void );

/**
 * Usage: Don't, use one of the jnetcap_error functions 
 */
char* jnetcap_error_throw( jnetcap_err_t type, const char* format, ... );

#endif 
