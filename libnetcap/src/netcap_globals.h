/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_globals.h,v 1.1 2004/11/09 19:39:58 dmorris Exp $
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
#define MAX_CONTROL_MSG (64 + NETCAP_MAX_IF_NAME_LEN + 10)

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
 * XXX should be in kernel config 
 * if not, make good guesses
 */

#ifndef IP_NONLOCAL
#define IP_NONLOCAL 18
#endif
#ifndef IP_SADDR
#define IP_SADDR	20
#endif
#ifndef IP_DEVICE
#define IP_DEVICE	21
#endif
#ifndef IP_RECVNFMARK
#define IP_RECVNFMARK	22
#endif
#ifndef IP_SENDNFMARK
struct ip_sendnfmark_opts {
    u_int32_t on;
    u_int32_t mark;
};
#define IP_SENDNFMARK	23
#endif
#ifndef IP_FIRSTNFMARK
#define IP_FIRSTNFMARK	24
#endif


#ifndef SOL_UDP /* missing from early kernels */
#define SOL_UDP 17
#endif
#ifndef UDP_RECVDPORT
#define UDP_RECVDPORT 2
#endif
#ifndef UDP_SPORT
#define UDP_SPORT 1 
#endif

#endif
