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
#ifndef __NETCAP_GLOBALS_
#define __NETCAP_GLOBALS_

#define NETCAP_SYNACK_MARK 0x20

/**
 * maximum queue packet size
 */
#define QUEUE_BUFSIZE 65535
#define UDP_MAX_MESG_SIZE   65536
#define QUEUE_MAX_MESG_SIZE 65536

/**
 * size of the subscription table
 */
#define SUBSCRIPTION_TABLE_SIZE 1025

/**
 * max control massge size
 * WARNING: fragile
 */
#define MAX_CONTROL_MSG 200

/**
 * max length of an iptables command
 */
#define MAX_CMD_LEN 500

/**
 * max iptables commands per redirect
 */
#define MAX_CMD_IPTABLES_PER_RDR 8

/**
 * max fd in the epoll server
 */
#define EPOLL_MAX_EVENT 4096

/**
 * Maximum number of messages inside of a mailbox for UDP/ICMP
 */
#define MAX_MB_SIZE    16

/**
 * Returns IP_TRANSPARENT constant (varies by kernel version)
 * Returns 0 if the kernel does not support IP_TRANSPARENT
 */
int IP_TRANSPARENT_VALUE ( );

/**
 * Returns IP_NONLOCAL constant (varies by kernel version)
 */
int IP_NONLOCAL_VALUE ( );

/**
 * Returns IP_SADDR constant (varies by kernel version)
 */
int IP_SADDR_VALUE ( );

/**
 * Returns IP_RECVNFMARK constant (varies by kernel version)
 */
int IP_RECVNFMARK_VALUE ( );

/**
 * Returns IP_SENDNFMARK constant (varies by kernel version)
 */
int IP_SENDNFMARK_VALUE ( );

/**
 * Returns IP_FIRSTNFMARK constant (varies by kernel version)
 */
int IP_FIRSTNFMARK_VALUE ( );

struct ip_sendnfmark_opts {
    u_int32_t on;
    u_int32_t mark;
};

/* Bits for the Netfilter marks */
#define MARK_BYPASS   0x01000000

#ifndef SOL_UDP /* missing from early kernels */
#define SOL_UDP 17
#endif
#ifndef UDP_RECVDPORT
#define UDP_RECVDPORT 2
#endif
#ifndef UDP_RECVDHDR
#define UDP_RECVDHDR 2
#endif
#ifndef UDP_SPORT
#define UDP_SPORT 1 
#endif


#endif
