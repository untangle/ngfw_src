/**
 * $Id$
 */
#ifndef __NETCAP_INIT_
#define __NETCAP_INIT_

#include "netcap_session.h"

typedef struct {
    session_tls_t session;
} netcap_tls_t;

int           netcap_is_initialized( void );

netcap_tls_t* netcap_tls_get       ( void );

#endif

