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
