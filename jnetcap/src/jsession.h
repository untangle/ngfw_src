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

#ifndef _JSESSION_H_
#define _JSESSION_H_

#include "jnetcap.h"
#include JH_Session

#define JLONG_TO_SESSION( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_UINT(( session_ptr )); \
  } while (0)

#define JLONG_TO_SESSION_NULL( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error_null( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_UINT(( session_ptr )); \
  } while (0)

#define JLONG_TO_SESSION_VOID( session, session_ptr )   do { \
    if (( session_ptr ) == 0 ) return jmvutil_error_void( JMVUTIL_ERROR_ARGS, ERR_CRITICAL, "NULL\n" ); \
    (session) = (netcap_session_t*)JLONG_TO_UINT(( session_ptr )); \
  } while (0)

static __inline__ netcap_endpoints_t* _get_endpoints( netcap_session_t* session, int req_id )
{
    if ( session == NULL ) return errlogargs_null();
    if ( req_id & JN_Session( FLAG_IF_CLIENT_MASK )) return &session->cli;
    return &session->srv;    
}

static __inline__ netcap_endpoint_t* _get_endpoint( netcap_session_t* session, int req_id )
{
    netcap_endpoints_t* endpoints = NULL;

    if ( session == NULL ) return errlogargs_null();

    if (( endpoints = _get_endpoints( session, req_id )) == NULL ) return NULL;

    if ( req_id & JN_Session( FLAG_IF_SRC_MASK )) return &endpoints->cli;
    
    return &endpoints->srv;
}

#endif // _JSESSION_H_
