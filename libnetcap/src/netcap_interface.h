/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef _NETCAP_INTERFACE_H_
#define _NETCAP_INTERFACE_H_

#include "libnetcap.h"
#include <netinet/if_ether.h>
#include <linux/if_packet.h>

#include "netcap_route.h"
#include "netcap_intf_db.h"

int            netcap_interface_init         ( void );

/* blocking on configuration */
int           netcap_interface_cleanup       ( void );

int           netcap_interface_mark_to_intf  ( int nfmark, netcap_intf_t* intf );

netcap_intf_t netcap_interface_index_to_intf ( int index );

netcap_intf_db_t* netcap_interface_get_db    ( void );

/* Retrieve the other interface */
int           netcap_interface_other_intf    ( netcap_intf_t* intf, netcap_intf_t src );

int           netcap_interface_intf_to_index ( netcap_intf_t intf );


#endif // _NETCAP_INTERFACE_H_
