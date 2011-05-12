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

#ifndef _NETCAP_ARP_H_
#define _NETCAP_ARP_H_

#include <netinet/in.h>
#include <net/ethernet.h>

#include "libnetcap.h"

/* Maximum number of arps to try before giving, This guarantees that
 * the delay array eventually ends  */
#define NETCAP_ARP_MAX     15

#define NETCAP_ARP_SUCCESS  1
#define NETCAP_ARP_NOERROR  0

int netcap_arp_init            ( void );

int netcap_arp_cleanup         ( void );

/**
 * Request the outgoing interface for an IP address using the default delay array.
 * @param intf     - The interface index that the packet is going out on.
 * @param src_intf - The interface that the session came in on. (unused, but may be important in the future)
 * @param src_ip   - The source IP address. (unused, but may be important in the future).
 * @param dst_ip   - The ip address to lookup the outgoing interface for.
 * @return       NETCAP_ARP_SUCCESS: found interface
 *               NETCAP_ARP_NOERROR: no errors, but unable to find the interface.
 *               < 0               : an error occured.
 */
int netcap_arp_dst_intf       ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip );


#endif // _NETCAP_ARP_H_
