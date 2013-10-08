/*
 * $HeadURL: svn://chef/work/src/jnetcap/src/jsession.h $
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
