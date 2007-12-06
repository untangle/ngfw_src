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
#ifndef __NETCAP_QUEUE_H
#define __NETCAP_QUEUE_H

#include <linux/netfilter.h>
#include "libnetcap.h"

int  netcap_queue_init       ( void );
int  netcap_queue_cleanup    ( void );
int  netcap_nfqueue_get_sock ( void );
int  netcap_nfqueue_read     ( u_char* buffer, int max, netcap_pkt_t* props );
int  netcap_set_verdict_mark ( u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, 
                               u_int32_t mark );
int  netcap_raw_send         ( u_char* pkt, int len );


#endif
