/*
 * $Id: jconntrack.h,v 1.00 2016/05/27 10:17:46 dmorris Exp $
 */

#ifndef _JCONNTRACK_H_
#define _JCONNTRACK_H_

#include "jnetcap.h"
#include JH_Conntrack

#define JLONG_TO_CONNTRACK( conntrack, conntrack_ptr )   do { \
    if (( conntrack_ptr ) == 0 ) return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (conntrack) = (struct nf_conntrack*)JLONG_TO_ULONG(( conntrack_ptr )); \
  } while (0)

#define JLONG_TO_CONNTRACK_NULL( conntrack, conntrack_ptr )   do { \
    if (( conntrack_ptr ) == 0 ) return jmvutil_error_null( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (conntrack) = (struct nf_conntrack*)JLONG_TO_ULONG(( conntrack_ptr )); \
  } while (0)

#define JLONG_TO_CONNTRACK_VOID( conntrack, conntrack_ptr )   do { \
    if (( conntrack_ptr ) == 0 ) return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (conntrack) = (struct nf_conntrack*)JLONG_TO_ULONG(( conntrack_ptr )); \
  } while (0)

#endif // _JCONNTRACK_H_
