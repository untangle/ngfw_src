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
#ifndef __NETCAP_UDP_H
#define __NETCAP_UDP_H

#include "libnetcap.h"

int  netcap_udp_init         ( void );
int  netcap_udp_cleanup      ( void );

int netcap_udp_divert_sock   ( void );
int netcap_udp_divert_port   ( void );

int  netcap_udp_recvfrom     ( int s, void* buf, size_t len, int flags, netcap_pkt_t* pkt );
int  netcap_udp_call_hooks   ( netcap_pkt_t* pkt, void* arg );

void netcap_udp_null_hook    ( netcap_session_t* netcap_sess, void *arg );
void netcap_udp_cleanup_hook ( netcap_session_t* netcap_sess, void *arg );


#endif
