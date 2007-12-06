/*
 * $HeadURL$
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
#ifndef _NETCAP_INTERFACE_H_
#define _NETCAP_INTERFACE_H_

#include "libnetcap.h"
#include <netinet/if_ether.h>
#include <linux/if_packet.h>

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
