/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ip.h 8515 2007-01-03 00:13:24Z rbscott $
 */

#ifndef __NETCAP_VIRTUAL_INTERFACE_H
#define __NETCAP_VIRTUAL_INTERFACE_H

#include <linux/if.h>
#include "libnetcap.h"

/* This initialises the tun interface */
int netcap_virtual_interface_init( char* name );

/* Send pkt out the virtual interface */
int netcap_virtual_interface_send_pkt( netcap_pkt_t* pkt );

/* Clean up the virtual interface */
void netcap_virtual_interface_destroy( void );

#endif // __NETCAP_VIRTUAL_INTERFACE_H_


