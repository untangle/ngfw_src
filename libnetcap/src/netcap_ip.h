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

/* $Id$ */
#ifndef __NETCAP_IP_H
#define __NETCAP_IP_H

#include "libnetcap.h"
#include "netinet/ip.h"
#include "netinet/udp.h"
#include "netinet/tcp.h"

/* Parse out the UDP header */
struct udphdr* netcap_ip_get_udp_header( struct iphdr* iph, int header_len );

/* Parse out the TCP header */
struct tcphdr* netcap_ip_get_tcp_header( struct iphdr* iph, int header_len );


#endif // __NETCAP_IP_H



