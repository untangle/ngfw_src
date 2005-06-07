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
#include "netcap_tcp.h"

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
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
#include "netcap_shield.h"

static int syn_mode = 1;

/* Callback functions */
/* XX May make more sense to call extern functions that are not global netcap functions
 * just _tcp rather than prefixing with netcap.
 */
extern int  _netcap_tcp_callback_cli_complete( netcap_session_t* netcap_sess, 
                                               netcap_callback_action_t action, 
                                               netcap_callback_flag_t flags );

extern int  _netcap_tcp_callback_srv_complete( netcap_session_t* netcap_sess, 
                                               netcap_callback_action_t action, 
                                               netcap_callback_flag_t flags );

extern int  _netcap_tcp_callback_cli_reject  ( netcap_session_t* netcap_sess, 
                                               netcap_callback_action_t action, 
                                               netcap_callback_flag_t flags );

/* Mailbox functions */
static int _session_put_syn         (netcap_session_t* netcap_sess, netcap_pkt_t* syn );
static int _session_put_complete_fd (netcap_session_t* netcap_sess, int client_fd );

/* Hook functions */
static int  _netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client, netcap_sub_t* sub );
static int  _netcap_tcp_syn_hook    ( netcap_pkt_t* syn );

/* Util functions */
extern int _netcap_tcp_setsockopt_cli( int sock );
extern int _netcap_tcp_cli_send_reset( netcap_pkt_t* pkt );

static netcap_session_t* _netcap_get_or_create_sess( int* created_flag,
                                                     in_addr_t cli_addr, u_short cli_port, int cli_sock,
                                                     in_addr_t srv_addr, u_short srv_port, int srv_sock,
                                                     int protocol,
                                                     netcap_intf_t cli_intf, netcap_intf_t srv_intf, 
                                                     int flags, u_int seq );

int  netcap_tcp_init ( void )
{
    return 0;
}

int  netcap_tcp_cleanup ( void )
{
    return 0;
}

int  netcap_tcp_syn_mode ( int toggle )
{
    syn_mode = toggle;
    return 0;
}

int  netcap_tcp_callback ( netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags )
{
    if ( netcap_sess == NULL )
        return errlogargs();

    switch ( action ) {
    case CLI_COMPLETE: 
        return _netcap_tcp_callback_cli_complete( netcap_sess,action, flags );
    case SRV_COMPLETE: 
        return _netcap_tcp_callback_srv_complete( netcap_sess,action, flags );
    case CLI_RESET:
        /* fallthrough */
    case CLI_DROP:
        /* fallthrough */
    case CLI_ICMP:
        /* fallthrough */
    case CLI_FORWARD_REJECT:
        return _netcap_tcp_callback_cli_reject( netcap_sess,action,flags );

    default:
        return errlog( ERR_CRITICAL, "Unknown action: %i\n", action );
    }

    return errlogcons();
}

int  netcap_tcp_syn_hook ( netcap_pkt_t* syn )
{
    netcap_shield_response_t* ans;

    if ( syn == NULL )
        return errlogargs();

    if (!syn_mode)
        return netcap_pkt_action_raze( syn, NF_ACCEPT );

    if (!( syn->th_flags & TH_SYN )) {
        errlog( ERR_CRITICAL,"Caught non SYN packet\n" );
        return netcap_pkt_action_raze( syn, NF_ACCEPT );
    }
    if (( syn->th_flags & TH_SYN ) && ( syn->th_flags & TH_ACK )) {
        errlog( ERR_CRITICAL,"Caught SYN/ACK\n" );
        return netcap_pkt_action_raze( syn, NF_ACCEPT);
    }

    /**
     * Indicate that the user sent a syn
     */
    if ( netcap_shield_rep_add_request ( syn->src.host.s_addr ) < 0 ) {
        errlog( ERR_CRITICAL, "netcap_shield_rep_add_request\n" );
    }

    /**
     * Check the reputation
     */
    if (( ans = netcap_shield_rep_check ( syn->src.host.s_addr )) == NULL ) {
        errlog( ERR_WARNING, "netcap_shield_rep_check\n" );
        return netcap_pkt_action_raze( syn, NF_ACCEPT );
    }
    
    switch( ans->tcp ) {
    case NC_SHIELD_YES:
        break;

    case NC_SHIELD_LIMITED:
        if ( ans->if_print ) {
            errlog( ERR_WARNING, "TCP: Session in opaque mode: %s:%d -> %s:%d\n", 
                    unet_next_inet_ntoa ( syn->src.host.s_addr ), syn->src.port,
                    unet_next_inet_ntoa ( syn->dst.host.s_addr ), syn->dst.port );
        }
        return netcap_pkt_action_raze( syn, NF_ACCEPT );
        
    case NC_SHIELD_RESET:
        if ( _netcap_tcp_cli_send_reset( syn ) < 0 ) {
            errlog( ERR_CRITICAL, "_netcap_tcp_cli_send_reset\n" );
        }
        /* fallthrough */
    case NC_SHIELD_DROP:
        if ( ans->if_print ) {
            errlog( ERR_WARNING, "TCP: SYN packet %s: %s:%d -> %s:%d\n",
                    ( ans->tcp == NC_SHIELD_RESET ) ? "reset" : "dropped",
                    unet_next_inet_ntoa( syn->src.host.s_addr ), syn->src.port,
                    unet_next_inet_ntoa( syn->dst.host.s_addr ), syn->dst.port );
        }
        return netcap_pkt_action_raze( syn, NF_DROP );
        
    default:
        errlog( ERR_WARNING, "netcap_shield_rep_check: invalid verdict: %d\n", ans );
        return netcap_pkt_action_raze( syn, NF_ACCEPT );
    }
    
    if ( _netcap_tcp_syn_hook( syn ) < 0 ) {
        errlog( ERR_CRITICAL, "_netcap_tcp_syn_hook\n" );
        return netcap_pkt_action_raze( syn, NF_DROP );
    }

    return 0;
}

int  netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client, netcap_sub_t* sub )
{
    if (!sub || cli_sock<=0)
        return errlogargs();

    return _netcap_tcp_accept_hook(cli_sock,client,sub);
}

void netcap_tcp_null_hook ( netcap_session_t* netcap_sess, void *arg )
{
    errlog( ERR_CRITICAL, "netcap_tcp_null_hook: No TCP hook registered\n" );

    /* Remove the session */
    netcap_tcp_session_raze(1, netcap_sess);
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
    if ( msg->pkt != NULL ) netcap_pkt_raze( msg->pkt );
    if (( msg->fd > 0 ) && ( close( msg->fd  ) < 0 )) perrlog( "close" );
    return 0;
}

int        netcap_tcp_msg_raze    ( tcp_msg_t* msg )
{
    if ( netcap_tcp_msg_destroy( msg ) < 0 ) errlog( ERR_CRITICAL, "netcap_tcp_msg_destroy\n" );
    if ( netcap_tcp_msg_free( msg ) < 0 ) errlog( ERR_CRITICAL, "netcap_tcp_msg_free\n" );
    return 0;
}

int  netcap_tcp_syn_null_hook ( netcap_pkt_t* syn )
{
    errlog( ERR_CRITICAL, "netcap_tcp_syn_null_hook: No TCP SYN hook registered\n" );

    netcap_pkt_action_raze(syn,NF_DROP);
    return 0;    
}

static int  _netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client, netcap_sub_t* sub )
{
    netcap_intf_t cli_intf_idx;
    in_addr_t cli_addr,srv_addr;
    u_short   cli_port,srv_port;
    struct sockaddr_in server;
    int server_len = sizeof(server);
    void* arg;
    int   flags;
    int   new_sess_flag = 0;
    netcap_session_t* sess = NULL;
    int nfmark;
    int nfmark_len = sizeof(nfmark);
    /**
     * Get the mark
     * and convert it to and interface index
     */
    if ( getsockopt(cli_sock, SOL_IP, IP_FIRSTNFMARK, &nfmark, &nfmark_len) < 0 )
        return perrlog("getsockopt");
    if ( netcap_interface_mark_to_intf(nfmark,&cli_intf_idx) < 0 )
        return perrlog("netcap_interface_mark_to_intf");

    /**
     * fill in src,dst,src.port, and dst.port
     */
    if ( getsockopt( cli_sock, SOL_IP, SO_ORIGINAL_DST, &server, &server_len ) < 0 )
        return perrlog("getsockopt");

    memcpy( &cli_addr, &client.sin_addr.s_addr, sizeof(client.sin_addr.s_addr));
    memcpy( &srv_addr, &server.sin_addr.s_addr, sizeof(server.sin_addr.s_addr));
    cli_port = ntohs(client.sin_port);
    srv_port = ntohs(server.sin_port);
    
    /**
     * Get misc info, sub, flags
     */
    arg   = sub->arg;
    flags = sub->rdr.flags;

    unet_reset_inet_ntoa();
    debug( 5, "TCP:         Connection Accepted :: (%s:%-5i) -> (%s:%-5i)\n",
           unet_next_inet_ntoa( cli_addr ), cli_port,
           unet_next_inet_ntoa( srv_addr ), srv_port );

    sess = _netcap_get_or_create_sess(&new_sess_flag,
                                      cli_addr,cli_port,cli_sock,
                                      srv_addr,srv_port,-1,
                                      IPPROTO_TCP,cli_intf_idx,NC_INTF_UNK,
                                      flags,0);

    if (!sess)
        return errlog(ERR_CRITICAL,"Could not find or create new session\n");

    /**
     * If this is a new session, call the hook
     * Otherwise, put the fd in the mailbox
     */
    if (new_sess_flag) {
        debug(8,"TCP: (%05d) Calling TCP hook\n", sess->session_id);

        /* Since this is a new session, it must be in opaque mode */
        sess->syn_mode = 0;

        /* Client has already completed */
        sess->cli_state = CONN_STATE_COMPLETE;
        
        if (_netcap_tcp_setsockopt_cli(cli_sock)<0)
            perrlog("_netcap_tcp_setsockopt_cli");

        global_tcp_hook( sess,arg );
    }
    else {
        _session_put_complete_fd( sess, cli_sock );
    }
    
    return 0;
}

static int  _netcap_tcp_syn_hook ( netcap_pkt_t* syn )
{
    netcap_intf_t cli_intf_idx;
    in_addr_t cli_addr,srv_addr;
    u_short   cli_port,srv_port;
    int   flags = 0;
    void* arg = NULL;
    int   new_sess_flag = 0;
    netcap_session_t* sess = NULL;
    
    cli_addr = syn->src.host.s_addr;
    cli_port = syn->src.port;
    srv_addr = syn->dst.host.s_addr;
    srv_port = syn->dst.port;
    cli_intf_idx = syn->src.intf;

    arg   = NULL; /* XXX */
    flags = 0;    /* XXX */

    debug( 8, "SYN: Intercepted packet ::  (%s:%-5i -> %s:%i) (src.intf:%d) (syn:%i ack:%i)\n",
           unet_next_inet_ntoa( cli_addr ), cli_port, unet_next_inet_ntoa( srv_addr ), srv_port, 
           cli_intf_idx,!!( syn->th_flags & TH_SYN ), !!( syn->th_flags & TH_ACK ));

    /* XXXXX Need to hold a lock between calling the get and putting the SYN in the mailbox
     * so the mailbox cannot be deleted from underneath the function */
    sess = _netcap_get_or_create_sess( &new_sess_flag, cli_addr, cli_port, -1, srv_addr, srv_port, -1,
                                       IPPROTO_TCP, cli_intf_idx, NC_INTF_UNK, flags, 0 );

    if ( sess == NULL ) {
        return errlog( ERR_CRITICAL, "Could not find or create new session\n" );
    }
    
    /**
     * First, put the SYN into the session mailbox.
     * Next, if this is a new session, call the hook.
     */
    
    _session_put_syn( sess, syn );

    if ( new_sess_flag ) {
        debug( 8, "TCP: (%10u) Calling TCP hook\n", sess->session_id );
        global_tcp_hook( sess, arg );
    }
    
    return 0;
}


/* XXX 
 * the errors for this function are not pushed to its top-most function.  If they were,
 * we could get rid of all of the netcap_pkt_action_raze's calls after each error and
 * instead just return an error flag.  As it is now, errors in this function are ignored.
 */
static int  _session_put_syn      ( netcap_session_t* netcap_sess, netcap_pkt_t* syn )
{
    tcp_msg_t* msg;
    
    if ( netcap_sess->cli_state == CONN_STATE_COMPLETE ) {
        debug(5,"TCP: (%10u) Dropping SYN\n", netcap_sess->session_id);
        return netcap_pkt_action_raze( syn, NF_DROP );
    }

    /* Try to remove the first message in the mailbox */
    if (( msg = mailbox_try_get( &netcap_sess->tcp_mb )) != NULL ) {
        /* There is a message in the mailbox */
        switch( msg->type ) {
        case TCP_MSG_SYN:
            debug( 7, "TCP: (%10u) Dropping old SYN.\n", netcap_sess->session_id );
            /* Drop the current SYN in the mailbox */
            if ( netcap_pkt_action_raze( msg->pkt, NF_DROP ) < 0 ) {
                errlog ( ERR_CRITICAL, "netcap_pkt_action_raze\n" );
            }
            msg->pkt = NULL;
            netcap_tcp_msg_raze( msg );
            break;

        case TCP_MSG_ACCEPT:
            debug( 7, "TCP: (%10u) Dropping new SYN, ACCEPT is already in mb.\n", netcap_sess->session_id );

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

    debug(5,"TCP: (%10u) Putting SYN in mailbox\n", netcap_sess->session_id);
    
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

    debug(5,"TCP: (%10u) Putting session complete in mailbox\n", netcap_sess->session_id);
    
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

static netcap_session_t* _netcap_get_or_create_sess ( int* created_flag,
                                                      in_addr_t cli_addr, u_short cli_port, int cli_sock,
                                                      in_addr_t srv_addr, u_short srv_port, int srv_sock,
                                                      int protocol,
                                                      netcap_intf_t cli_intf, netcap_intf_t srv_intf, 
                                                      int flags, u_int seq )
{
    netcap_session_t* sess;

    if (!created_flag)
        return errlogargs_null();
    
    SESSTABLE_WRLOCK();

    sess = netcap_nc_sesstable_get_tuple( !NC_SESSTABLE_LOCK, IPPROTO_TCP,
                                          cli_addr,srv_addr, cli_port,srv_port,seq);
                                          
    
#if 0
    debug(2,"LOOKUP: (%s:%i ->",unet_inet_ntoa(cli_addr),cli_port);
    debug_nodate(2," %s:%i seq:%i) -> ",unet_inet_ntoa(srv_addr),srv_port,seq);
    if (sess)
        debug_nodate(2,"FOUND\n");
    else
        debug_nodate(2,"not found, creating new session...\n");
#endif
    
    if (sess) {
        SESSTABLE_UNLOCK();
        *created_flag = 0;
        return sess;
    }

    *created_flag = 1;

    sess = netcap_tcp_session_create( cli_addr, cli_port, cli_sock,
                                      srv_addr, srv_port, srv_sock,
                                      protocol,
                                      cli_intf, srv_intf, flags, seq );

    if ( netcap_nc_sesstable_add_tuple(!NC_SESSTABLE_LOCK,sess,protocol,
                                       cli_addr,srv_addr,
                                       cli_port,srv_port,seq) < 0) {
        netcap_tcp_session_raze(!NC_SESSTABLE_LOCK,sess);
        SESSTABLE_UNLOCK();
        return perrlog_null("netcap_nc_sesstable_add_tuple\n");
    }

    if ( netcap_nc_sesstable_add ( !NC_SESSTABLE_LOCK, sess )) {
        netcap_tcp_session_raze ( !NC_SESSTABLE_LOCK, sess );
        SESSTABLE_UNLOCK();
        return perrlog_null("netcap_nc_sesstable_add");
    }
    
    SESSTABLE_UNLOCK();

    /* Update their reputation */
    netcap_shield_rep_add_session( cli_addr );

    return sess;
}
