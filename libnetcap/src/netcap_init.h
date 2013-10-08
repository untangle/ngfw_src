/**
 * $Id: netcap_init.h 35571 2013-08-08 18:37:27Z dmorris $
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

