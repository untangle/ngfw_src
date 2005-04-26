/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef __NETCAP_TCP_H_
#define __NETCAP_TCP_H_

#include "libnetcap.h"
#include "netcap_subscriptions.h"

typedef struct tcp_tuple {
    in_addr_t shost;
    in_addr_t dhost;
    u_short   sport;
    u_short   dport;
} tcp_tuple_t;

typedef netcap_tcp_conn_state_t conn_state_t;

typedef enum {
    TCP_MSG_SYN    = 1,
    /* TCP_MSG_SYNACK = 1, Deprecated */
    TCP_MSG_ACCEPT,
    TCP_MSG_NULL
} tcp_msg_type_t;

typedef struct tcp_msg {
    tcp_msg_type_t type;
    netcap_pkt_t* pkt; /* for TCP_SYNACK */
    int fd; /* for TCP_ACCEPT */
} tcp_msg_t;

int  netcap_tcp_init();
int  netcap_tcp_cleanup();

int  netcap_tcp_syn_hook ( netcap_pkt_t* pkt );
int  netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client, netcap_sub_t* sub );

int  netcap_tcp_callback ( netcap_session_t* tcp_sess, netcap_callback_action_t action, netcap_callback_flag_t flags );

void netcap_tcp_null_hook ( netcap_session_t* netcap_sess, void *arg );
int  netcap_tcp_syn_null_hook ( netcap_pkt_t* pkt );

int  netcap_packet_action_free ( netcap_pkt_t* pkt, int action);


#endif

