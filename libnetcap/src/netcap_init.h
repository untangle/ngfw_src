/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef __NETCAP_INIT_
#define __NETCAP_INIT_

#include "netcap_session.h"
#include "netcap_shield.h"

typedef struct {
    shield_tls_t  shield;
    session_tls_t session;
} netcap_tls_t;

int           netcap_is_initialized( void );

netcap_tls_t* netcap_tls_get       ( void );

#endif

