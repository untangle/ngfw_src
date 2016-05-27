/*
 * $Id$
 */

#ifndef _JSESSION_H_
#define _JSESSION_H_

#include "jnetcap.h"
#include JH_Session

#define JLONG_TO_SESSION( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_ULONG(( session_ptr )); \
  } while (0)

#define JLONG_TO_SESSION_NULL( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error_null( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_ULONG(( session_ptr )); \
  } while (0)

#define JLONG_TO_SESSION_VOID( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_ULONG(( session_ptr )); \
  } while (0)

#endif // _JSESSION_H_
