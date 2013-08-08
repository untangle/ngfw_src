/**
 * $Id$
 */
#ifndef __NETCAP_SESSION_H
#define __NETCAP_SESSION_H

#include <pthread.h>
#include <netinet/in.h>

#include <mvutil/mailbox.h>
#include <mvutil/hash.h>
#include <mvutil/lock.h>

#include <libnetcap.h>


#define NC_SESSION_IF_MB 1

typedef enum {
    SESS_STATE_ESTABLISHING,
    SESS_STATE_ESTABLISHED,
    SESS_STATE_CLOSING,
    SESS_STATE_DEFUNCT,
    SESS_STATE_ERROR
} session_state_t;

typedef struct {
    char output_buf[64];
} session_tls_t;

int netcap_sessions_init   ( void );
int netcap_sessions_cleanup( void );
int netcap_session_tls_init( session_tls_t* tls );

netcap_session_t* netcap_session_malloc ( void );
int netcap_session_init                 ( netcap_session_t* netcap_sess, netcap_endpoints_t* endpoints, 
                                          netcap_intf_t srv_intf, int if_mb );
netcap_session_t* netcap_session_create ( netcap_endpoints_t* endpoints, netcap_intf_t srv_intf, int if_mb );

int netcap_session_free(netcap_session_t* session);
int netcap_session_destroy(netcap_session_t* netcap_sess);
int netcap_nc_session_destroy(int if_lock, netcap_session_t* session);
int netcap_nc_session_raze(int if_lock, netcap_session_t* session);

int netcap_nc_session__destroy (netcap_session_t* netcap_sess, int if_mb);

#define netcap_udp_session_malloc() netcap_session_malloc()

int netcap_udp_session_init(netcap_session_t* netcap_sess, netcap_pkt_t* pkt);
netcap_session_t* netcap_udp_session_create(netcap_pkt_t* pkt);

#define netcap_udp_session_free(netcap_sess) netcap_session_free((netcap_sess))
int netcap_udp_session_destroy(int if_lock, netcap_session_t* session);
int netcap_udp_session_raze(int if_lock, netcap_session_t* netcap_sess);

#define netcap_tcp_session_malloc() netcap_session_malloc()


int netcap_tcp_session_init( netcap_session_t* netcap_sess, 
                             in_addr_t client_addr, u_short client_port,
                             int client_sock, in_addr_t server_addr, 
                             u_short server_port, int server_sock,
                             netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int seq );

netcap_session_t* netcap_tcp_session_create(in_addr_t client_addr, u_short client_port,
                                            int client_sock, in_addr_t server_addr,
                                            u_short server_port, int server_sock,
                                            netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int seq);

void netcap_tcp_session_debug(netcap_session_t* netcap_sess, int level, char *msg);


#define netcap_tcp_session_free(netcap_sess) netcap_session_free((netcap_sess))
int netcap_tcp_session_destroy(int if_lock, netcap_session_t* session);
int netcap_tcp_session_close(netcap_session_t* session);
int netcap_tcp_session_raze(int if_lock, netcap_session_t* netcap_sess);

#endif
