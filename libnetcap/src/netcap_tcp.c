/**
 * $Id$
 */
#include "netcap_tcp.h"

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <unistd.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <limits.h>
#include <inttypes.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#define __FAVOR_BSD
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
#include <mvutil/mailbox.h>
#include <linux/netfilter_ipv4.h>

#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_session.h"
#include "netcap_pkt.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_sesstable.h"

/* The number of sockets to listen on for TCP */
#define RDR_TCP_LOCALS_SOCKS 128

static struct {
    int base_port;
    int redirect_socks[RDR_TCP_LOCALS_SOCKS];
} _tcp = {
    .base_port = -1
};

extern int  _netcap_tcp_callback_cli_complete( netcap_session_t* netcap_sess, netcap_callback_action_t action );
extern int  _netcap_tcp_callback_srv_complete( netcap_session_t* netcap_sess, netcap_callback_action_t action );
extern int  _netcap_tcp_callback_cli_reject  ( netcap_session_t* netcap_sess, netcap_callback_action_t action );

/* Initialization and cleanup routines */
static int  _redirect_ports_open( void );
static void _redirect_ports_close( void );

/* Mailbox functions */
static int _session_put_syn         (netcap_session_t* netcap_sess, netcap_pkt_t* syn );
static int _session_put_complete_fd (netcap_session_t* netcap_sess, int client_fd );

/* Hook functions */
static int  _netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client );
static int  _netcap_tcp_syn_hook    ( netcap_pkt_t* syn );

/* Util functions */
extern int _netcap_tcp_setsockopt_cli ( int sock );
extern int _netcap_tcp_cli_send_reset ( netcap_pkt_t* pkt );

static int _netcap_tcp_get_mark ( int sock );
static int _netcap_tcp_set_mark ( int sock, int mark );

static netcap_session_t* _netcap_get_or_create_sess ( int* created_flag,
                                                      in_addr_t c_cli_addr, u_short c_cli_port, 
                                                      in_addr_t c_srv_addr, u_short c_srv_port, 
                                                      in_addr_t s_cli_addr, u_short s_cli_port, 
                                                      in_addr_t s_srv_addr, u_short s_srv_port, 
                                                      netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int nfmark );

int  netcap_tcp_init ( void )
{
    if ( _redirect_ports_open() < 0 ) return errlog( ERR_CRITICAL, "_redirect_ports_open\n" );

    return 0;
}

int  netcap_tcp_cleanup ( void )
{
    _redirect_ports_close();
    return 0;
}

int  netcap_tcp_redirect_ports( int* port_low, int* port_high )
{
    if (( port_low == NULL ) || ( port_high == NULL )) return errlogargs();

    if ( _tcp.base_port < 0 ) return errlog( ERR_CRITICAL, "TCP Redirect ports are uninitialized\n" );

    *port_low  = _tcp.base_port;
    *port_high = _tcp.base_port + RDR_TCP_LOCALS_SOCKS - 1;
    
    return 0;
}

int  netcap_tcp_redirect_socks( int** sock_array )
{
    if ( sock_array == NULL ) return errlogargs();

    if ( _tcp.base_port < 0 ) return errlog( ERR_CRITICAL, "TCP Redirect ports are uninitialized\n" );

    *sock_array = _tcp.redirect_socks;

    return RDR_TCP_LOCALS_SOCKS;
}

int  netcap_tcp_set_server_mark ( netcap_session_t* netcap_sess , int mark )
{
    if ( !netcap_sess )
        return errlogargs();

    if ( netcap_sess->protocol != IPPROTO_TCP ) {
        return errlogargs();
    }

    return _netcap_tcp_set_mark( netcap_sess->server_sock, mark );
}

int  netcap_tcp_set_client_mark ( netcap_session_t* netcap_sess , int mark )
{
    if ( !netcap_sess )
        return errlogargs();

    if ( netcap_sess->protocol != IPPROTO_TCP ) {
        return errlogargs();
    }

    return _netcap_tcp_set_mark( netcap_sess->client_sock, mark);
}

int  netcap_tcp_get_server_mark ( netcap_session_t* netcap_sess )
{
    if ( !netcap_sess )
        return errlogargs();

    return _netcap_tcp_get_mark( netcap_sess->server_sock );
}

int  netcap_tcp_get_client_mark ( netcap_session_t* netcap_sess )
{
    if ( !netcap_sess )
        return errlogargs();

    return _netcap_tcp_get_mark( netcap_sess->client_sock );
}
    
static int _netcap_tcp_get_mark ( int sock )
{
    struct ip_sendnfmark_opts nfmark = {
        .on = 0,
        .mark = 0
    };
    u_int size = sizeof(nfmark);
    
    if ( getsockopt(sock,SOL_IP,IP_SENDNFMARK_VALUE(),&nfmark,&size) < 0 )
        return perrlog( "getsockopt" );

    return nfmark.mark;
}

static int _netcap_tcp_set_mark ( int sock, int mark )
{
    if ( sock < 1 )
        return errlogargs();

    struct ip_sendnfmark_opts nfmark = {
        .on = 1,
        .mark = mark
    };

    if ( setsockopt(sock,SOL_IP,IP_SENDNFMARK_VALUE(),&nfmark,sizeof(nfmark)) < 0 )
        return perrlog( "setsockopt" );

    return 0;
}

int  netcap_tcp_callback ( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    if ( netcap_sess == NULL ) return errlogargs();

    switch ( action ) {
    case CLI_COMPLETE: 
        return _netcap_tcp_callback_cli_complete( netcap_sess, action  );
    case SRV_COMPLETE: 
        return _netcap_tcp_callback_srv_complete( netcap_sess, action );
    case CLI_RESET:
        /* fallthrough */
    case CLI_DROP:
        /* fallthrough */
    case CLI_ICMP:
        /* fallthrough */
    case CLI_FORWARD_REJECT:
        return _netcap_tcp_callback_cli_reject( netcap_sess, action );

    default:
        return errlog( ERR_CRITICAL, "Unknown action: %i\n", action );
    }

    return errlogcons();
}

int  netcap_tcp_syn_hook ( netcap_pkt_t* syn )
{
    if ( syn == NULL )
        return errlogargs();

    if (!( syn->th_flags & TH_SYN )) {
        errlog( ERR_CRITICAL,"Caught non SYN packet\n" );
        return netcap_pkt_action_raze( syn, NF_ACCEPT );
    }
    if (( syn->th_flags & TH_SYN ) && ( syn->th_flags & TH_ACK )) {
        errlog( ERR_CRITICAL,"Caught SYN/ACK\n" );
        return netcap_pkt_action_raze( syn, NF_ACCEPT);
    }
        
    if ( _netcap_tcp_syn_hook( syn ) < 0 ) {
        errlog( ERR_CRITICAL, "_netcap_tcp_syn_hook\n" );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }

    return 0;
}

int  netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client )
{
    if ( cli_sock <= 0 )
        return errlogargs();

    return _netcap_tcp_accept_hook( cli_sock, client );
}

void netcap_tcp_null_hook ( netcap_session_t* netcap_sess, void *arg )
{
    errlog( ERR_WARNING, "netcap_tcp_null_hook: No TCP hook registered\n" );
    
    netcap_tcp_cleanup_hook( netcap_sess, arg );
}

void netcap_tcp_cleanup_hook ( netcap_session_t* netcap_sess, void *arg )
{
    /* Remove the session */
    netcap_session_raze( netcap_sess );
}

tcp_msg_t* netcap_tcp_msg_malloc  ( void )
{
    tcp_msg_t* msg;
    if (( msg = malloc( sizeof(tcp_msg_t))) == NULL ) return errlogmalloc_null();
    return msg;
}

int        netcap_tcp_msg_init    ( tcp_msg_t* msg, tcp_msg_type_t type, netcap_pkt_t* pkt, int fd )
{
    if ( msg == NULL ) return errlogargs();

    msg->type = type;
    msg->pkt  = pkt;
    msg->fd   = fd;
    
    return 0;
}

tcp_msg_t* netcap_tcp_msg_create  ( tcp_msg_type_t type, int fd, netcap_pkt_t* pkt )
{
    tcp_msg_t* msg;
    if (( msg = netcap_tcp_msg_malloc()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "netcap_tcp_msg_malloc\n" );
    }

    if ( netcap_tcp_msg_init( msg, type, pkt, fd ) < 0 ) {
        netcap_tcp_msg_free( msg );
        return errlog_null( ERR_CRITICAL, "netcap_tcp_msg_init\n" );
    }
    
    return msg;
}

int        netcap_tcp_msg_free    ( tcp_msg_t* msg )
{
    if ( msg == NULL ) return errlogargs();
    else free( msg );
    return 0;
}

int        netcap_tcp_msg_destroy ( tcp_msg_t* msg )
{
    if ( msg == NULL ) return errlogargs();
    if ( msg->pkt != NULL ) netcap_pkt_action_raze( msg->pkt, NF_DROP );
    if (( msg->fd > 0 ) && ( close( msg->fd  ) < 0 )) perrlog( "close" );
    return 0;
}

int        netcap_tcp_msg_raze    ( tcp_msg_t* msg )
{
    if ( netcap_tcp_msg_destroy( msg ) < 0 ) errlog( ERR_CRITICAL, "netcap_tcp_msg_destroy\n" );
    if ( netcap_tcp_msg_free( msg ) < 0 ) errlog( ERR_CRITICAL, "netcap_tcp_msg_free\n" );
    return 0;
}

static int  _redirect_ports_open( void )
{
    int c;
    u_short base_port;

    _tcp.base_port = -1;

    /* Clear out all of the ports */
    for ( c = 0 ; c < RDR_TCP_LOCALS_SOCKS ; c++ ) {
        /* XXX May want to try and close the one before */
        _tcp.redirect_socks[c] = -1;
    }

    if ( unet_startlisten_on_portrange( RDR_TCP_LOCALS_SOCKS, &base_port, _tcp.redirect_socks, "0.0.0.0" ) < 0 ) {
        return errlog( ERR_CRITICAL, "unet_startlisten_on_portrange\n" );
    }
    
    _tcp.base_port = base_port;
    
    return 0;
}

static void _redirect_ports_close( void )
{
    int c;
    
    /* Clear out all of the ports */
    for ( c = 0 ; c < RDR_TCP_LOCALS_SOCKS ; c++ ) {
        if (( _tcp.redirect_socks[c] > 0 ) && ( close( _tcp.redirect_socks[c] ) < 0 )) {
            perrlog( "close" );
        }

        _tcp.redirect_socks[c] = -1;
    }

    _tcp.base_port = -1;
}

int  netcap_tcp_syn_null_hook ( netcap_pkt_t* syn )
{
    errlog( ERR_CRITICAL, "netcap_tcp_syn_null_hook: No TCP SYN hook registered\n" );
    
    netcap_tcp_syn_cleanup_hook( syn );
    return 0;
}

int  netcap_tcp_syn_cleanup_hook ( netcap_pkt_t* syn )
{
    netcap_pkt_action_raze( syn, NF_DROP );
    return 0;
}

static int  _netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client )
{
    in_addr_t cli_addr,srv_addr;
    u_short   cli_port,srv_port;
    struct sockaddr_in server;
    u_int server_len = sizeof(server);
    netcap_session_t* sess = NULL;

    /**
     * fill in src, dst, src.port, and dst.port
     */
    if ( getsockopt( cli_sock, SOL_IP, SO_ORIGINAL_DST, &server, &server_len ) < 0 )
        return perrlog("getsockopt");
    memcpy( &cli_addr, &client.sin_addr.s_addr, sizeof(client.sin_addr.s_addr));
    memcpy( &srv_addr, &server.sin_addr.s_addr, sizeof(server.sin_addr.s_addr));
    cli_port = ntohs(client.sin_port);
    srv_port = ntohs(server.sin_port);
    
    unet_reset_inet_ntoa();
    debug( 5, "TCP:         Connection Accepted :: (%s:%-5i) -> (%s:%-5i)\n",
           unet_next_inet_ntoa( cli_addr ), cli_port,
           unet_next_inet_ntoa( srv_addr ), srv_port );

    SESSTABLE_WRLOCK();

    sess = netcap_nc_sesstable_get_tuple( !NC_SESSTABLE_LOCK, IPPROTO_TCP, cli_addr, srv_addr, cli_port, srv_port);
                                          
    SESSTABLE_UNLOCK();
    
    if (!sess) {
        return errlog( ERR_WARNING, "TCP: Could not find session for accepted connection :: (%s:%-5i) -> (%s:%-5i)\n",
           unet_next_inet_ntoa( cli_addr ), cli_port,
           unet_next_inet_ntoa( srv_addr ), srv_port );
    }
    
    _session_put_complete_fd( sess, cli_sock );

    return 0;
}

static int  _netcap_tcp_syn_hook ( netcap_pkt_t* syn )
{
    netcap_intf_t cli_intf;
    netcap_intf_t srv_intf;
    in_addr_t c_cli_addr,c_srv_addr,s_cli_addr,s_srv_addr;
    u_short   c_cli_port,c_srv_port,s_cli_port,s_srv_port;
    int   new_sess_flag = 0;
    netcap_session_t* sess = NULL;
    
    cli_intf = syn->src_intf;
    srv_intf = syn->dst_intf;

    c_cli_addr = syn->nat_info.original.src_address;
    c_cli_port = ntohs(syn->nat_info.original.src_protocol_id);
    c_srv_addr = syn->nat_info.original.dst_address;
    c_srv_port = ntohs(syn->nat_info.original.dst_protocol_id);
    s_cli_addr = syn->nat_info.reply.dst_address;
    s_cli_port = ntohs(syn->nat_info.reply.dst_protocol_id);
    s_srv_addr = syn->nat_info.reply.src_address;
    s_srv_port = ntohs(syn->nat_info.reply.src_protocol_id);
    
    debug( 8, "SYN: Intercepted packet ::  (%s:%-5i -> %s:%i) (intf:%d,%d) (syn:%i ack:%i)\n",
           unet_next_inet_ntoa( c_cli_addr ), c_cli_port, unet_next_inet_ntoa( s_srv_addr ), s_srv_port, 
           cli_intf, srv_intf, !!( syn->th_flags & TH_SYN ), !!( syn->th_flags & TH_ACK ));

    SESSTABLE_WRLOCK();

    sess = _netcap_get_or_create_sess( &new_sess_flag,
                                       c_cli_addr, c_cli_port,
                                       c_srv_addr, c_srv_port,
                                       s_cli_addr, s_cli_port,
                                       s_srv_addr, s_srv_port,
                                       cli_intf, srv_intf,
                                       syn->nfmark );

    /**
     * First, put the SYN into the session mailbox.
     * Next, if this is a new session, call the hook.
     */
    _session_put_syn( sess, syn );

    SESSTABLE_UNLOCK();
    
    if ( new_sess_flag ) {
        debug( 8, "TCP: (%"PRIu64") Calling TCP hook\n", sess->session_id );
        global_tcp_hook( sess, NULL );
    }

    return 0;
}

static int  _session_put_syn      ( netcap_session_t* netcap_sess, netcap_pkt_t* syn )
{
    /* XXX 
     * the errors for this function are not pushed to its top-most function.  If they were,
     * we could get rid of all of the netcap_pkt_action_raze's calls after each error and
     * instead just return an error flag.  As it is now, errors in this function are ignored.
     */
    tcp_msg_t* msg;

    if ( netcap_sess == NULL ) {
        debug( 2, "TCP: (%"PRIu64") Dropping SYN (Connection not found)\n", netcap_sess->session_id );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }
    
    if ( netcap_sess->cli_state == CONN_STATE_COMPLETE ) {
        debug( 2, "TCP: (%"PRIu64") Dropping SYN (Connection already complete)\n", netcap_sess->session_id );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }

    if ( syn->src_intf != netcap_sess->cli.intf ) {
        errlog( ERR_WARNING, "TCP: (%"PRIu64") Dropping SYN (incorrect side)\n", netcap_sess->session_id );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }

    /* Try to remove the first message in the mailbox */
    if (( msg = mailbox_try_get( &netcap_sess->tcp_mb )) != NULL ) {
        /* There is a message in the mailbox */
        switch( msg->type ) {
        case TCP_MSG_SYN:
            debug( 7, "TCP: (%"PRIu64") Dropping old SYN.\n", netcap_sess->session_id );
            /* Drop the current SYN in the mailbox */
            if ( netcap_pkt_action_raze( msg->pkt, NF_DROP ) < 0 ) {
                errlog ( ERR_CRITICAL, "netcap_pkt_action_raze\n" );
            }
            msg->pkt = NULL;
            netcap_tcp_msg_raze( msg );
            break;

        case TCP_MSG_ACCEPT:
            debug( 7, "TCP: (%"PRIu64") Dropping new SYN, ACCEPT is already in mb.\n", netcap_sess->session_id );

            /* Have an accept already, drop the current syn and then place the accept back
             * into the mailbox */
            if ( netcap_pkt_action_raze( syn, NF_DROP ) < 0 ) {
                errlog ( ERR_CRITICAL, "netcap_pkt_action_raze\n" );
            }
            
            /* Put the accept message back into the mailbox */
            if ( mailbox_put( &netcap_sess->tcp_mb, msg ) < 0 ) {
                return errlog( ERR_CRITICAL, "mailbox_put" );
            }

            return 0;

        default:
            errlog( ERR_CRITICAL, "Invalid TCP message type: %d\n", msg->type );
            free( msg );
            break;
        }
    }

    debug(5,"TCP: (%"PRIu64") Putting SYN in mailbox\n", netcap_sess->session_id);
    
    /* Send a session a SYN */
    if (( msg = netcap_tcp_msg_create( TCP_MSG_SYN, -1, syn )) == NULL ) {
        errlog( ERR_CRITICAL, "netcap_tcp_msg_create\n" );
        return netcap_pkt_action_raze(syn,NF_DROP);
    }
        
    if ( mailbox_put( &netcap_sess->tcp_mb, msg ) < 0 ) {
        perrlog( "mailbox_put" );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }
    
    return 0;
}

static int  _session_put_complete_fd ( netcap_session_t* netcap_sess, int client_fd ) 
{
    tcp_msg_t* msg;

    debug(5,"TCP: (%"PRIu64") Putting session complete in mailbox\n", netcap_sess->session_id);
    
    /* Send a session a client connection */
    if ( client_fd < 0 ) return errlogargs();

    if ( netcap_sess == NULL ) {
        if ( close ( client_fd ) < 0 ) perrlog("close");
        return errlogargs();
    }
    
    if ( netcap_sess->client_sock > 0 || netcap_sess->cli_state == CONN_STATE_COMPLETE ) {
        if ( close ( client_fd ) < 0 ) perrlog("close");
        return errlog(ERR_CRITICAL,"Client connection opened twice\n");
    }
    
    if (( msg = netcap_tcp_msg_create( TCP_MSG_ACCEPT, client_fd, NULL )) == NULL ) {
        errlogmalloc();
        if ( close( client_fd )) perrlog("close");
        return -1;
    }
    
    client_fd = -1; /* The MSG now "owns" the fd, and it will close it if there is an error */
    
    if ( mailbox_put( &netcap_sess->tcp_mb, msg ) < 0 ) {
        perrlog("mailbox_put");
        netcap_tcp_msg_raze( msg );
        return -1;
    }
    
    return 0;
}

/**
 * Must hold the SESSTABLE LOCK to call this function
 */
static netcap_session_t* _netcap_get_or_create_sess ( int* created_flag,
                                                      in_addr_t c_cli_addr, u_short c_cli_port, 
                                                      in_addr_t c_srv_addr, u_short c_srv_port, 
                                                      in_addr_t s_cli_addr, u_short s_cli_port, 
                                                      in_addr_t s_srv_addr, u_short s_srv_port, 
                                                      netcap_intf_t cli_intf, netcap_intf_t srv_intf, u_int nfmark )
{
    netcap_session_t* sess;

    if (!created_flag)
        return errlogargs_null();
    
    sess = netcap_nc_sesstable_get_tuple( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                          c_cli_addr, c_srv_addr, c_cli_port, c_srv_port);
                                          
    
#if 0
    debug(2,"LOOKUP: (%s:%i ->",unet_next_inet_ntoa(c_cli_addr),c_cli_port);
    debug_nodate(2," %s:%i) -> ",unet_next_inet_ntoa(s_srv_addr),s_srv_port);
    if (sess)
        debug_nodate(2,"FOUND\n");
    else
        debug_nodate(2,"not found, creating new session...\n");
#endif
    
    if (sess) {
        *created_flag = 0;
        return sess;
    }

    *created_flag = 1;

    sess = netcap_tcp_session_create( c_cli_addr, c_cli_port,
                                      c_srv_addr, c_srv_port,
                                      s_cli_addr, s_cli_port,
                                      s_srv_addr, s_srv_port,                                      
                                      -1, -1,
                                      cli_intf, srv_intf );

    // CITRIX - preserve the source and destination interfaces
    sess->initial_mark = nfmark & 0x0000ffff;
    
    if ( netcap_nc_sesstable_add_tuple( !NC_SESSTABLE_LOCK, sess, IPPROTO_TCP,
                                        c_cli_addr, c_srv_addr,
                                        c_cli_port, c_srv_port ) < 0 ) {
        netcap_tcp_session_raze(!NC_SESSTABLE_LOCK,sess);
        return perrlog_null("netcap_nc_sesstable_add_tuple\n");
    }

    if ( netcap_nc_sesstable_add ( !NC_SESSTABLE_LOCK, sess )) {
        netcap_tcp_session_raze ( !NC_SESSTABLE_LOCK, sess );
        return perrlog_null("netcap_nc_sesstable_add");
    }

    return sess;
}
