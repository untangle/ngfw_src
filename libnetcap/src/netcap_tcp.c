/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_tcp.c,v 1.9 2005/01/27 04:55:08 rbscott Exp $
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
#define __FAVOR_BSD
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
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

#define TUPLE_TABLE_SIZE 1337

static int syn_mode = 1;

/* Callback functions */
static int  _netcap_tcp_callback_cli_reset (netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags );
static int  _netcap_tcp_callback_cli_complete (netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags );
static int  _netcap_tcp_callback_srv_complete (netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags );
static int  _netcap_tcp_callback_srv_start_complete (netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags );
/* Mailbox functions */
static int _session_put_syn         (netcap_session_t* netcap_sess, netcap_pkt_t* syn );
static int _session_put_complete_fd (netcap_session_t* netcap_sess, int client_fd );
/* Hook functions */
static int  _netcap_tcp_accept_hook ( int cli_sock, struct sockaddr_in client, netcap_sub_t* sub );
static int  _netcap_tcp_syn_hook ( netcap_pkt_t* syn );
/* Util functions */
static int  _netcap_tcp_setsockopt_cli ( int sock );
static int  _netcap_tcp_setsockopt_srv ( int sock );
static int  _netcap_packet_action_free ( netcap_pkt_t* pkt, int action);
static netcap_session_t* _netcap_get_or_create_sess ( int* created_flag,
                                                      in_addr_t client_addr, u_short client_port, int client_sock,
                                                      in_addr_t server_addr, u_short server_port, int server_sock,
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
    if (!netcap_sess)
        return errlogcons();

    switch (action) {
    case CLI_RESET: 
        return _netcap_tcp_callback_cli_reset(netcap_sess,action,flags );
    case CLI_COMPLETE: 
        return _netcap_tcp_callback_cli_complete(netcap_sess,action,flags );
    case SRV_START_COMPLETE: 
        return _netcap_tcp_callback_srv_start_complete(netcap_sess,action,flags );
    case SRV_COMPLETE: 
        return _netcap_tcp_callback_srv_complete(netcap_sess,action,flags );
    default:
        return errlog(ERR_CRITICAL,"Unknown action: %i\n",action);
    }

    return errlogcons();
}

int  netcap_tcp_syn_hook ( netcap_pkt_t* syn )
{
    netcap_shield_ans_t ans;
    char address[20];

    if (!syn)
        return errlogargs();

    if (!syn_mode)
        return _netcap_packet_action_free(syn,NF_ACCEPT);

    if (!(syn->th_flags & TH_SYN)) {
        errlog(ERR_CRITICAL,"Caught non SYN packet\n");
        return _netcap_packet_action_free(syn,NF_ACCEPT);
    }
    if (syn->th_flags & TH_SYN && (syn->th_flags & TH_ACK)) {
        errlog(ERR_CRITICAL,"Caught SYN/ACK\n");
        return _netcap_packet_action_free(syn,NF_ACCEPT);
    }

    /**
     * Indicate that the user sent a syn
     */
    if ( netcap_shield_rep_add_request ( syn->src.host.s_addr ) < 0 ) {
        perrlog("netcap_shield_rep_add_syn\n");
    }

    /**
     * Check the reputation
     */
    /* XXX Debugging output should be cleaned up */
    if ( (ans = netcap_shield_rep_check ( syn->src.host.s_addr )) < 0 ) {
        errlog(ERR_WARNING,"netcap_shield_rep_check\n");
        return _netcap_packet_action_free(syn,NF_ACCEPT);
    }
    else if ( ans == NC_SHIELD_LIMITED ) {
        strncpy ( address, inet_ntoa ( syn->src.host ), sizeof ( address ) );
        errlog( ERR_WARNING, "TCP: Session in opaque mode: %s:%d -> %s:%d\n", address, syn->src.port,
                inet_ntoa ( syn->dst.host ), syn->dst.port );
        return _netcap_packet_action_free( syn, NF_ACCEPT );
    }
    else if ( ans == NC_SHIELD_RESET || ans == NC_SHIELD_DROP ) {
        strncpy ( address, inet_ntoa ( syn->src.host ), sizeof ( address ) );
        errlog( ERR_WARNING, "TCP: Session rejected: %s:%d -> %s:%d\n", address, syn->src.port,
                inet_ntoa ( syn->dst.host ), syn->dst.port );
        return _netcap_packet_action_free( syn, NF_DROP );
    }
    else if ( ans != NC_SHIELD_YES ) {
        errlog(ERR_WARNING,"netcap_shield_rep_check: invalid verdict: %d\n", ans);
        return _netcap_packet_action_free(syn,NF_ACCEPT);
    }
        
    
    if (_netcap_tcp_syn_hook(syn)<0) {
        perrlog("_netcap_tcp_syn_hook");
        return _netcap_packet_action_free(syn,NF_DROP);
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

int  netcap_tcp_syn_null_hook ( netcap_pkt_t* syn )
{
    errlog( ERR_CRITICAL, "netcap_tcp_syn_null_hook: No TCP SYN hook registered\n" );

    _netcap_packet_action_free(syn,NF_DROP);
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
    if (getsockopt(cli_sock,SOL_IP,SO_ORIGINAL_DST,&server,&server_len)<0)
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

    debug( 5, "TCP:         Connection Accepted :: (%s:%-5i) -> (%s:%-5i)\n",
          unet_inet_ntoa( cli_addr ), cli_port,
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
    u_int seq;
    int   flags = 0;
    void* arg = NULL;
    int   new_sess_flag = 0;
    netcap_session_t* sess = NULL;
    struct tcphdr* tcp_hdr; 
    
    cli_addr = syn->src.host.s_addr;
    cli_port = syn->src.port;
    srv_addr = syn->dst.host.s_addr;
    srv_port = syn->dst.port;
    cli_intf_idx = syn->src.intf;

    if (!(tcp_hdr = netcap_pkt_get_tcp_hdr ( syn ))) 
        return errlog(ERR_CRITICAL,"netcap_pkt_get_tcp_hdr");
    seq = ntohl(tcp_hdr->th_seq);

    arg   = NULL; /* XXX */
    flags = 0;    /* XXX */

    debug(8,"SYN: Intercepted packet ::  ");
    debug_nodate(8,"(%s:%-5i -> ",unet_inet_ntoa(cli_addr),cli_port);
    debug_nodate(8,"%s:%i) (src.intf:%d) (syn:%i ack:%i)\n",unet_inet_ntoa(srv_addr),
                 srv_port,cli_intf_idx,!!(syn->th_flags&TH_SYN),!!(syn->th_flags&TH_ACK));

    sess = _netcap_get_or_create_sess(&new_sess_flag,
                                      cli_addr,cli_port,-1,
                                      srv_addr,srv_port,-1,
                                      IPPROTO_TCP,cli_intf_idx,NC_INTF_UNK,
                                      flags,0);

    if (!sess)
        return errlog(ERR_CRITICAL,"Could not find or create new session\n");

    /**
     * If this is a new session, call the hook
     * Otherwise, put the fd in the mailbox
     */
    _session_put_syn(sess,syn);

    if (new_sess_flag) {
        debug(8,"TCP: (%10u) Calling TCP hook\n", sess->session_id);
        global_tcp_hook(sess,arg);
    }
    
    return 0;
}

static int  _netcap_tcp_callback_cli_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags )
{
    int fd;
    tcp_msg_t* msg;
    tcp_msg_t* prev_msg;
    
    debug(6,"TCP: (%10u) CLI_COMPLETE %s\n",netcap_sess->session_id,netcap_session_cli_tuple_print(netcap_sess));

    if (netcap_sess->cli_state == CONN_STATE_COMPLETE)
        return errlog(ERR_CRITICAL,"Invalid state (%i), Can't perform action (%i).\n",netcap_sess->cli_state, action);

    if ( mailbox_size( &netcap_sess->tcp_mb ) <= 0 )
        return errlogcons(); /* SYN must be present */

    /**
     * "wait" for the SYN arrival.              
     * The SYN should already be in the mailbox 
     */
    for ( prev_msg = NULL; mailbox_size(&netcap_sess->tcp_mb) > 0; prev_msg = msg ) {
        if (!(msg = mailbox_timed_get(&netcap_sess->tcp_mb,1))) {
            if ( errno == ETIMEDOUT ) 
                return errlog(ERR_WARNING,"TCP: Missed SYN :: %s\n",netcap_session_tuple_print(netcap_sess));
            else
                return perrlog("mailbox_timed_get");
        }
        
        if ( msg->type != TCP_MSG_SYN || !msg->pkt ) {
            errlog(ERR_WARNING,"TCP: Invalid message: %i %i 0x%08x\n", msg->type, msg->fd, msg->pkt);
            free(msg); 
            return -1;
        }
        
        /* Clear out every SYN except for the last one */
        if ( prev_msg != NULL ) {
            _netcap_packet_action_free(prev_msg->pkt,NF_DROP);
            free(prev_msg);
        }
    }

    /**
     * Release the SYN
     */
    netcap_sess->cli_state = CONN_STATE_COMPLETING;
    _netcap_packet_action_free(msg->pkt,NF_ACCEPT);
    free(msg);

    debug( 6, "TCP: (%10u) Released SYN :: %s\n",netcap_sess->session_id,
           netcap_session_cli_tuple_print(netcap_sess));
    
    /**
     * wait on connection complete message
     * it is possible to get more syn/ack's during this time, ignore them
     */
    while (1) {
        if (!(msg = mailbox_timed_get(&netcap_sess->tcp_mb,5))) {
            if (errno == ETIMEDOUT) {
                debug(6,"TCP: (%10u) Missed Final ACK\n",netcap_sess->session_id);
                /**
                 * This is bad behavior, the host used up the resources, but 
                 * client did not respond (Typically a SYN flood)
                 * Although this happens if the client decides not to connect before the server connection completes
                 * (e.g. the person presses the stop button in their browser before connected)
                 */
                netcap_shield_rep_blame( netcap_sess->cli.cli.host.s_addr, NC_SHIELD_ERR_3 );
                return -1;
            }
            else
                return perrlog("mailbox_timed_get");
        }

        if (msg->type != TCP_MSG_ACCEPT || !msg->fd) {
            if (msg->type == TCP_MSG_SYN && msg->pkt) {
                debug(8,"TCP: (%10u) DUP syn message, passing\n",netcap_sess->session_id);
                _netcap_packet_action_free(msg->pkt,NF_ACCEPT);
            }
            else
                errlog(ERR_WARNING,"TCP: Invalid message: %i %i 0x%08x\n", msg->type, msg->fd, msg->pkt);
            free(msg);
            continue;
        }

        break;
    }
        
    fd = msg->fd;
    free(msg);

    netcap_sess->cli_state = CONN_STATE_COMPLETE;
    netcap_sess->client_sock = fd;

    if (_netcap_tcp_setsockopt_cli(fd)<0)
        perrlog("_netcap_tcp_setsockopt_cli");

    return 0;
}

static int  _netcap_tcp_callback_cli_reset ( netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags )
{
    tcp_msg_t* msg;
        
    debug(6,"TCP: (%10u) CLI_RESET %s\n", netcap_sess->session_id, netcap_session_cli_tuple_print(netcap_sess));

    if (netcap_sess->cli_state != CONN_STATE_COMPLETE && netcap_sess->cli_state != CONN_STATE_INCOMPLETE)
        return errlog(ERR_CRITICAL,"Invalid state (%i), Can't perform action (%i).\n",netcap_sess->cli_state, action);

    /**
     * If already accepted, send reset anyway, then close
     */
    if (netcap_sess->cli_state == CONN_STATE_COMPLETE) {
        if (unet_reset_and_close(netcap_sess->client_sock)<0)
            perrlog("unet_reset_and_close");
        netcap_sess->client_sock = -1;
        netcap_sess->cli_state = CONN_STATE_INCOMPLETE;
        return 0;
    }

    /**
     * read syn message
     */
    if (!(msg = mailbox_timed_get(&netcap_sess->tcp_mb,1))) {
        if (errno == ETIMEDOUT)
            return errlog(ERR_WARNING,"TCP: (%10u) Missed SYN/ACK :: %s\n", netcap_sess->session_id, netcap_session_tuple_print(netcap_sess));
        else
            return perrlog("mailbox_get");
    }

    if (msg->type != TCP_MSG_SYN || !msg->pkt) {
        errlog(ERR_WARNING,"TCP: (%10u) Invalid message: %i %i 0x%08x\n", netcap_sess->session_id, msg->type, msg->fd, msg->pkt);
        free(msg); 
        return -1;
    }

    debug(6,"TCP: (%10u) Reseting Client  :: %s\n", netcap_sess->session_id, netcap_session_tuple_print(netcap_sess));

    {
        struct iphdr*  iph;
        struct tcphdr* tcph;
        u_int16_t tmp16;
        u_int32_t tmp32;
        u_char extra_len = 0;
        u_char tcp_len   = 0;
        
        /**
         * I didnt write this gobbly gook, if anyone asks, robert wrote it!
         */
        tcph = netcap_pkt_get_tcp_hdr( msg->pkt );
        iph  = netcap_pkt_get_ip_hdr( msg->pkt );

        extra_len = msg->pkt->data_len - iph->ihl*4 - sizeof(struct tcphdr);
        tcp_len   = sizeof(struct tcphdr);
        
        /**
         * swap src, dst, src.port, dst.port
         */
        tmp16 = ntohs(iph->tot_len);
        tmp16 -= extra_len;
        iph->tot_len = htons(tmp16);

        tmp16 = ntohs(iph->check);
        tmp16 += extra_len;
        iph->check = htons(tmp16);
        
        tmp32      = iph->saddr;
        iph->saddr = iph->daddr;
        iph->daddr = tmp32;

        tmp16          = tcph->th_sport;
        tcph->th_sport = tcph->th_dport;
        tcph->th_dport = tmp16;
        
        /**
         * set flags, etc
         */
        tcph->th_ack = htonl(ntohl(tcph->th_seq)+1);
        tcph->th_flags = TH_RST|TH_ACK;
        tcph->th_seq = 0;
        tcph->th_win = 0;
        tcph->th_sum = 0;
        tcph->th_urp = 0;
        tcph->th_off = tcp_len/4;


        /**
         * compute checksum
         */
        tcph->th_sum = htons( unet_tcp_sum_calc( tcp_len, (u_int8_t*)&iph->saddr, (u_int8_t*)&iph->daddr, (u_int8_t*)tcph ) );


        /**
         * send the packet out a raw socket, drop the packet in ipq
         * and free all resources
         */
        netcap_sess->cli_state = CONN_STATE_INCOMPLETE;

        if (netcap_raw_send(msg->pkt->data,msg->pkt->data_len-extra_len)<0)
            perrlog("netcap_raw_send");

        _netcap_packet_action_free(msg->pkt,NF_DROP);
        free(msg);
    }

    return 0;
}

static int  _netcap_tcp_callback_srv_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags )
{
    struct sockaddr_in dest_addr;

    if (netcap_sess->srv_state == CONN_STATE_COMPLETE) return 0;

    if (netcap_sess->srv_state == CONN_STATE_INCOMPLETE) {
        if (_netcap_tcp_callback_srv_start_complete(netcap_sess, action, flags)<0)
            return -1;
    }

    if (netcap_sess->srv_state != CONN_STATE_COMPLETING) {
        return errlog(ERR_CRITICAL,"Invalid state (%i), Can't perform action (%i).\n",
                      netcap_sess->srv_state, action);
    }

    debug(6,"TCP: (%10u) SRV_COMPLETE %s\n",netcap_sess->session_id,netcap_session_srv_tuple_print(netcap_sess));

    dest_addr.sin_family = AF_INET;
    dest_addr.sin_port   = htons(netcap_sess->srv.srv.port);
    memcpy(&dest_addr.sin_addr,&netcap_sess->srv.srv.host,sizeof(in_addr_t));
    
    debug(8,"TCP: (%10u) Connect %i to %s\n",netcap_sess->session_id,netcap_sess->server_sock,netcap_session_srv_tuple_print(netcap_sess));
    
    if (connect(netcap_sess->server_sock, (struct sockaddr*)&dest_addr, sizeof(dest_addr))<0) {
        debug(8,"TCP: connect failed: %s\n",errstr);
        /* Increment the number of failed server connection attempts */
        netcap_shield_rep_add_srv_fail( netcap_sess->cli.cli.host.s_addr );
        return -1;
    }

    debug(8,"TCP: (%10u) Connection completed successfully. (fd:%i)\n",netcap_sess->session_id,netcap_sess->server_sock);
    /* Increment the number of succesful server connection attempts */
    netcap_shield_rep_add_srv_conn( netcap_sess->cli.cli.host.s_addr );
    netcap_sess->srv_state = CONN_STATE_COMPLETE;
    
    return 0;
}

static int  _netcap_tcp_callback_srv_start_complete ( netcap_session_t* netcap_sess, netcap_callback_action_t action, netcap_callback_flag_t flags )
{
    struct sockaddr_in dest_addr;
    struct sockaddr_in local_addr;
    int newsocket;

    debug(7,"TCP: (%10u) SRV_START_COMPLETE %s\n",netcap_sess->session_id,netcap_session_srv_tuple_print(netcap_sess));

    if (netcap_sess->srv_state != CONN_STATE_INCOMPLETE) 
        return errlog(ERR_CRITICAL,"Invalid state (%i), Can't perform action (%i).\n",netcap_sess->srv_state, action);
            
    if ((newsocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP))<0) 
        return perrlog("socket");

    netcap_sess->server_sock = newsocket;

    if (_netcap_tcp_setsockopt_srv(newsocket)<0)
        perrlog("_netcap_tcp_setsockopt_srv");
        
    local_addr.sin_family = AF_INET;
    local_addr.sin_port   = htons(netcap_sess->srv.cli.port);
    memcpy(&local_addr.sin_addr,&netcap_sess->srv.cli.host,sizeof(in_addr_t));
    dest_addr.sin_family = AF_INET;
    dest_addr.sin_port   = htons(netcap_sess->srv.srv.port);
    memcpy(&dest_addr.sin_addr,&netcap_sess->srv.srv.host,sizeof(in_addr_t));

    debug(8,"TCP: (%10u) Completing connection to %s\n",netcap_sess->session_id,
          netcap_session_srv_endp_print( netcap_sess ));

    if (flags & SRV_COMPLETE_NONLOCAL_BIND) {
        debug(8,"TCP: (%10u) Binding %i to %s:%i\n",netcap_sess->session_id,newsocket,
              unet_inet_ntoa( local_addr.sin_addr.s_addr ), ntohs( local_addr.sin_port ));
        if (bind(newsocket, (struct sockaddr *)&local_addr, sizeof(local_addr))<0)
            return perrlog("bind"); 
    }
    else 
        debug(8,"TCP: (%10u) Skipping binding\n",netcap_sess->session_id);

    /**
     * set non-blocking
     */
    if ((flags = fcntl(newsocket,F_GETFL))<0)  perrlog("fcntl");

    if (fcntl(newsocket,F_SETFL,flags | O_NONBLOCK)<0) perrlog("fcntl");
    
    debug( 8,"TCP: (%10u) Connect %i to %s:%i\n",netcap_sess->session_id,newsocket,
           unet_inet_ntoa( dest_addr.sin_addr.s_addr ), ntohs( dest_addr.sin_port ));

    if (connect(newsocket, (struct sockaddr*)&dest_addr, sizeof(dest_addr))<0) {
        if (errno != EINPROGRESS) {
            if ((flags = fcntl(newsocket,F_GETFL))<0) perrlog("fcntl");
            if (fcntl(newsocket,F_SETFL,flags & ~O_NONBLOCK)<0) perrlog("fcntl");
            return perrlog("connect");
        }
    }

    /**
     * reset blocking mode
     */
    if ((flags = fcntl(newsocket,F_GETFL))<0) perrlog("fcntl");
    if (fcntl(newsocket,F_SETFL,flags & ~O_NONBLOCK)<0) perrlog("fcntl");

    netcap_sess->srv_state = CONN_STATE_COMPLETING;
    debug(8,"TCP: (%10u) Connection completion started. (fd:%i)\n",netcap_sess->session_id,newsocket);
    
    return 0;

}

static int  _netcap_tcp_setsockopt_cli ( int sock )
{
    int one = 1;
    int thirty = 30;
    int threehundo  = 300;

    if (setsockopt(sock, SOL_SOCKET, SO_KEEPALIVE, &one, sizeof(one))<0)
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_NODELAY,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_LINGER2,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPINTVL,&threehundo,sizeof(threehundo))<0) 
        perrlog("setsockopt");

    return 0;
}

static int  _netcap_tcp_setsockopt_srv ( int sock )
{
    int one = 1;
    int thirty = 30;
    int threehundo  = 300;
    
    if (setsockopt(sock,SOL_IP,IP_NONLOCAL,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_NODELAY,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_LINGER2,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock, SOL_SOCKET, SO_KEEPALIVE, &one, sizeof(one))<0)
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPINTVL,&threehundo,sizeof(threehundo))<0) 
        perrlog("setsockopt");

    return 0;
}

static int  _session_put_syn      ( netcap_session_t* netcap_sess, netcap_pkt_t* syn )
{
    tcp_msg_t* msg;
    
    if ( netcap_sess->cli_state == CONN_STATE_COMPLETE ) {
        debug(5,"TCP: (%10u) Dropping SYN\n", netcap_sess->session_id);
        return _netcap_packet_action_free(syn,NF_DROP);
    }

    debug(5,"TCP: (%10u) Putting SYN in mailbox\n", netcap_sess->session_id);

    /* Send a session a SYN */
    if (( msg = malloc(sizeof(tcp_msg_t))) == NULL ) {
        errlogmalloc();
        return _netcap_packet_action_free(syn,NF_DROP);
    }
        
    msg->type = TCP_MSG_SYN;
    msg->fd   = -1;
    msg->pkt  = syn;
    if ( mailbox_put(&netcap_sess->tcp_mb, msg) < 0 ) {
        perrlog("mailbox_put");
        return _netcap_packet_action_free(syn,NF_DROP);
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
    
    if (( msg = malloc(sizeof(tcp_msg_t))) == NULL ) {
        errlogmalloc();
        if ( close( netcap_sess->client_sock ) ) perrlog("close");
        return -1;
    }
    
    msg->type = TCP_MSG_ACCEPT;
    msg->fd   = client_fd;
    msg->pkt  = NULL;

    if ( mailbox_put(&netcap_sess->tcp_mb, msg) < 0 ) {
        perrlog("mailbox_put");
        if ( close ( netcap_sess->client_sock ) ) perrlog("close");
        return -1;
    }
    
    return 0;
}

static int  _netcap_packet_action_free ( netcap_pkt_t* pkt, int action)
{
    if (!pkt)
        return errlogargs();
       
    if ( netcap_set_verdict(pkt->packet_id,action,NULL,0) < 0 ) {
        perrlog("netcap_set_verdict");
        netcap_pkt_raze(pkt);
        return -1;
    }

    netcap_pkt_raze(pkt);
    return 0;
}

static netcap_session_t* _netcap_get_or_create_sess ( int* created_flag,
                                                      in_addr_t client_addr, u_short client_port, int client_sock,
                                                      in_addr_t server_addr, u_short server_port, int server_sock,
                                                      int protocol,
                                                      netcap_intf_t cli_intf, netcap_intf_t srv_intf, 
                                                      int flags, u_int seq )
{
    netcap_session_t* sess;

    if (!created_flag)
        return errlogargs_null();
    
    SESSTABLE_WRLOCK();

    sess = netcap_nc_sesstable_get_tuple(!NC_SESSTABLE_LOCK,IPPROTO_TCP,
                                         client_addr,server_addr,
                                         client_port,server_port,seq);

#if 0
    debug(2,"LOOKUP: (%s:%i ->",unet_inet_ntoa(client_addr),client_port);
    debug_nodate(2," %s:%i seq:%i) -> ",unet_inet_ntoa(server_addr),server_port,seq);
    if (sess)
        debug_nodate(2,"FOUND\n");
    else
        debug_nodate(2,"not found, creating new session...\n");
#endif
    
    if (sess) {
        SESSTABLE_UNLOCK();
        return sess;
    }

    *created_flag = 1;

    sess = netcap_tcp_session_create(client_addr,client_port,client_sock,
                                     server_addr,server_port,server_sock,
                                     protocol,
                                     cli_intf,srv_intf,flags,seq);

    if ( netcap_nc_sesstable_add_tuple(!NC_SESSTABLE_LOCK,sess,protocol,
                                       client_addr,server_addr,
                                       client_port,server_port,seq) < 0) {
        netcap_tcp_session_raze(!NC_SESSTABLE_LOCK,sess);
        SESSTABLE_UNLOCK();
        return perrlog_null("netcap_nc_sesstable_add_tuple\n");
    }

    if ( netcap_nc_sesstable_add ( !NC_SESSTABLE_LOCK, sess ) ) {
        netcap_tcp_session_raze ( !NC_SESSTABLE_LOCK, sess );
        SESSTABLE_UNLOCK();
        return perrlog_null("netcap_nc_sesstable_add");
    }
    
    SESSTABLE_UNLOCK();

    /* Update their reputation */
    netcap_shield_rep_add_session(client_addr);

    return sess;
}


