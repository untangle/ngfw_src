/**
 * $Id: netcap_tcp.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_TCP_H_
#define __NETCAP_TCP_H_

#include "libnetcap.h"

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
    int fd;            /* for TCP_ACCEPT */
} tcp_msg_t;

int  netcap_tcp_init();
int  netcap_tcp_cleanup();

int  netcap_tcp_redirect_port_range( int* base_port, int* count );

/* socket_array is updated to point to an array of sockets that are currently opened for 
 * redirection.
 * Returns the number of sockets that were opened for redirect.
 */
int  netcap_tcp_redirect_socks( int** socket_array );

int  netcap_tcp_syn_hook ( netcap_pkt_t* pkt );
int  netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client );

int  netcap_tcp_callback ( netcap_session_t* tcp_sess, netcap_callback_action_t action );

void netcap_tcp_null_hook         ( netcap_session_t* netcap_sess, void *arg );
int  netcap_tcp_syn_null_hook     ( netcap_pkt_t* pkt );

void netcap_tcp_cleanup_hook      ( netcap_session_t* netcap_sess, void *arg );
int  netcap_tcp_syn_cleanup_hook  ( netcap_pkt_t* pkt );


/* Helper functions for TCP messages */
tcp_msg_t* netcap_tcp_msg_malloc  ( void );
int        netcap_tcp_msg_init    ( tcp_msg_t* msg, tcp_msg_type_t type, netcap_pkt_t* pkt, int fd );
tcp_msg_t* netcap_tcp_msg_create  ( tcp_msg_type_t type, int fd, netcap_pkt_t* pkt );

int        netcap_tcp_msg_free    ( tcp_msg_t* msg );
int        netcap_tcp_msg_destroy ( tcp_msg_t* msg );
int        netcap_tcp_msg_raze    ( tcp_msg_t* msg );

#endif

