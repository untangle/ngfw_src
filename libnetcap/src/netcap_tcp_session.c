/**
 * $Id: netcap_tcp_session.c 35571 2013-08-08 18:37:27Z dmorris $
 */
#include <stdlib.h>
#include <unistd.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/unet.h>

#include <libnetcap.h>

#include "netcap_queue.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_tcp.h"
#include "netcap_pkt.h"

int netcap_tcp_session_init( netcap_session_t* netcap_sess,
                             in_addr_t client_addr, u_short client_port, int client_sock,
                             in_addr_t server_addr, u_short server_port, int server_sock,
                             netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int seq )
{
    netcap_endpoints_t endpoints;

    netcap_endpoints_bzero(&endpoints);

    endpoints.cli.port = client_port;
    endpoints.srv.port = server_port;

    memcpy( &endpoints.cli.host, &client_addr, sizeof( in_addr_t ));
    memcpy( &endpoints.srv.host, &server_addr, sizeof( in_addr_t ));

    endpoints.intf = cli_intf;
    
    // Create a new session without mailboxes
    if ( netcap_session_init( netcap_sess, &endpoints, srv_intf, !NC_SESSION_IF_MB ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_session_init");
    }
    
    // Initialize the tcp mailbox
    if (mailbox_init(&netcap_sess->tcp_mb)<0) {
        netcap_nc_session__destroy(netcap_sess, !NC_SESSION_IF_MB);
        return errlog(ERR_CRITICAL,"mailbox_init\n");
    }
    
    /* client*/
    netcap_sess->client_sock = client_sock;

    /* server */
    netcap_sess->server_sock = server_sock;

    netcap_sess->protocol = IPPROTO_TCP;

    if (netcap_sess->client_sock > 0) {
        netcap_sess->cli_state = CONN_STATE_COMPLETE;
    } else {
        netcap_sess->cli_state = CONN_STATE_INCOMPLETE;
    }
    
    if (netcap_sess->server_sock > 0) {
        netcap_sess->srv_state = CONN_STATE_COMPLETE;
    } else {
        netcap_sess->srv_state = CONN_STATE_INCOMPLETE;
    }
    
    netcap_sess->callback = netcap_tcp_callback;

    /* Create it in SYN mode by default */
    netcap_sess->syn_mode = 1;
    
    return 0;
}

netcap_session_t* netcap_tcp_session_create(in_addr_t client_addr, u_short client_port, int client_sock,
                                            in_addr_t server_addr, u_short server_port, int server_sock,
                                            netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int seq )
{
    int ret;
    netcap_session_t* netcap_sess;
    
    if ((netcap_sess = netcap_tcp_session_malloc()) == NULL) {
        return errlog_null( ERR_CRITICAL, "netcap_udp_session_malloc\n" );
    }

    ret = netcap_tcp_session_init ( netcap_sess, client_addr, client_port,
                                    client_sock, server_addr, server_port,
                                    server_sock, cli_intf, srv_intf, seq );

    if ( ret < 0) {
        if ( netcap_tcp_session_free(netcap_sess)) {
            errlog( ERR_CRITICAL, "netcap_udp_session_free\n" );
        }

        return errlog_null( ERR_CRITICAL, "netcap_tcp_session_init\n" );
    }

    return netcap_sess;
}

int netcap_tcp_session_destroy(int if_lock,netcap_session_t* netcap_sess)
{
    tcp_msg_t* msg;
    int err=0;
    
    if ( netcap_sess == NULL ) {
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    }

    netcap_sesstable_remove_session(if_lock, netcap_sess);

    if ( netcap_tcp_session_close ( netcap_sess) ) {
        errlog(ERR_CRITICAL,"netcap_tcp_session_close\n");
        err--;
    }

    // Clear out the tcp mailbox
    while((msg = (tcp_msg_t*)mailbox_try_get(&netcap_sess->tcp_mb))) {
        switch ( msg->type ) {
        case TCP_MSG_SYN:
            if ( msg->pkt != NULL ) {
                if ( netcap_pkt_action_raze( msg->pkt, NF_DROP ) < 0 ) {
                    errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
                }
            }
            break;

        case TCP_MSG_ACCEPT:
            if ( msg->fd > 0 ) {
                if ( close (msg->fd )  < 0 ) perrlog("close");
            }
            break;

        default:
            errlog( ERR_WARNING, "Found invalid TCP message while cleaning mailbox %d\n", msg->type );
        }
        
        free( msg );
    }
    
    if (mailbox_destroy(&netcap_sess->tcp_mb)<0) {
        errlog(ERR_WARNING,"mailbox_destroy failed\n");
        err--;
    }

    // Destroy the session
    if ( netcap_nc_session__destroy(netcap_sess,!NC_SESSION_IF_MB) ) {
        errlog(ERR_CRITICAL,"netcap_session_destroy");
        err--;
    }

    return err;
}

int netcap_tcp_session_raze(int if_lock, netcap_session_t* netcap_sess)
{
    int err = 0;

    if ( netcap_sess == NULL ) {
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    }

    if ( netcap_tcp_session_destroy(if_lock, netcap_sess) < 0 ) {
        err -= 1;
        errlog(ERR_CRITICAL,"netcap_tcp_session_destroy");
    }

    if ( netcap_tcp_session_free(netcap_sess) < 0 ) {
        err -= 2;
        errlog(ERR_CRITICAL,"netcap_tcp_session_free");
    }

    return err;
}


int netcap_tcp_session_close(netcap_session_t* netcap_sess)
{
    if ( !netcap_sess ) {
        errlog(ERR_CRITICAL,"Invalid Arguments\n");
        return -1;
    }

    if (netcap_sess->client_sock >=0 && (close(netcap_sess->client_sock) < 0)) {
        errlog(ERR_WARNING,"Problem closing socket (%i): %s \n",netcap_sess->client_sock,strerror(errno));
    }

    if (netcap_sess->server_sock >= 0 && (close(netcap_sess->server_sock) < 0)) {
        errlog(ERR_WARNING,"Problem closing socket (%i): %s \n",netcap_sess->server_sock,strerror(errno));
    }

    netcap_sess->client_sock = -1;
    netcap_sess->server_sock = -1;

    return 0;
}

void netcap_tcp_session_debug(netcap_session_t* netcap_sess, int level, char *msg)
{    
    if ( netcap_sess == NULL ) {
        debug( level, "NULL Session!!" );
        return;
    }

    if ( msg == NULL ) {
        errlog( ERR_WARNING, "netcap_tcp_session_debug: NULL msg argument" );
        msg = "";
    }

    debug_nodate(level, "%s :: (%s:%-5i -> %s:%-5i)\n", msg, 
                 unet_next_inet_ntoa( netcap_sess->cli.cli.host.s_addr ), netcap_sess->cli.cli.port,
                 unet_next_inet_ntoa( netcap_sess->srv.srv.host.s_addr ), netcap_sess->srv.srv.port );
}
