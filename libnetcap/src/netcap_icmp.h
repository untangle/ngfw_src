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
#ifndef __NETCAP_ICMP_H_
#define __NETCAP_ICMP_H_

#include <netinet/ip_icmp.h>

#include "libnetcap.h"



int  netcap_icmp_init         ( void );
int  netcap_icmp_cleanup      ( void );
int  netcap_icmp_send         ( char *data, int data_len, netcap_pkt_t* pkt );
int  netcap_icmp_call_hook    ( netcap_pkt_t* pkt );
void netcap_icmp_null_hook    ( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg);
void netcap_icmp_cleanup_hook ( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg);

int  netcap_icmp_verify_type_and_code( u_int type, u_int code );

#endif
