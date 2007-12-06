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
#ifndef __NETCAP_PKT_
#define __NETCAP_PKT_

#include <netinet/in.h>

#include "libnetcap.h"

netcap_pkt_t*  netcap_pkt_malloc (void);
int            netcap_pkt_init   (netcap_pkt_t* pkt);
netcap_pkt_t*  netcap_pkt_create (void);

struct iphdr*  netcap_pkt_get_ip_hdr  ( netcap_pkt_t* pkt );
struct tcphdr* netcap_pkt_get_tcp_hdr ( netcap_pkt_t* pkt );


#endif
