/**
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
#include <limits.h>
#include <inttypes.h>
#include <netinet/ip.h>
#include <netinet/ip_icmp.h>
#define __FAVOR_BSD   // DUDE! 
#include <netinet/tcp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/unet.h>
#include <mvutil/mailbox.h>
#include <linux/netfilter_ipv4.h>

#include "libnetcap.h"
#include "netcap_session.h"
#include "netcap_pkt.h"
#include "netcap_globals.h"
#include "netcap_icmp.h"
#include "netcap_queue.h"
#include "netcap_nfconntrack.h"
#include "netcap_virtual_interface.h"
/* Maximum size for an ICMP response, these are responses to SYNs so they shouldn't be that large */
/* This goes on the stack, so any of the max packet sizes would probably exceed that size. */
#define _ICMP_MAX_RESPONSE_SIZE 1024

/**
 * Number of SYNs to reset in response
 */
#define _SYN_REJECT_COUNT 1

/**
 * Timeout (in seconds) for waiting for a SYN from a reset or ICMP packet response
 */
#define _SYN_RESP_TIMEOUT 2

int _netcap_tcp_cli_send_reset( netcap_pkt_t* pkt );

/* Util functions */
int _netcap_tcp_setsockopt_cli( int sock , u_int mark );

static int  _retrieve_and_reject( netcap_session_t* netcap_sess, netcap_callback_action_t action );
static int  _send_icmp_response ( netcap_session_t* netcap_sess, netcap_pkt_t* syn );
static int  _forward_rejection  ( netcap_session_t* netcap_sess, netcap_pkt_t* syn );

int  _netcap_tcp_callback_cli_complete( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    int fd;
    tcp_msg_t* msg;
    if ( netcap_sess == NULL ) return errlogargs();

    debug( 6, "TCP: (%"PRIu64") CLI_COMPLETE %s\n", netcap_sess->session_id,
           netcap_session_cli_tuple_print( netcap_sess ));

    if ( netcap_sess->cli_state == CONN_STATE_COMPLETE )
        return errlog(ERR_CRITICAL,"Invalid state (%i), Can't perform action (%i).\n",netcap_sess->cli_state, action);

    /* Grab the first SYN out, (older SYNs are disregarded) */
    if (( msg = mailbox_timed_get( &netcap_sess->tcp_mb, 1 )) == NULL ) {
        if ( errno == ETIMEDOUT )
            return errlog( ERR_CRITICAL,"TCP: Missing SYN :: %s\n",netcap_session_tuple_print(netcap_sess));
        else
            return perrlog("mailbox_timed_get");
    }
    
    if ( msg->type != TCP_MSG_SYN || !msg->pkt ) {
        errlog( ERR_CRITICAL, "TCP: Invalid message: %i %i 0x%016"PRIxPTR"\n", msg->type, msg->fd, (uintptr_t) msg->pkt );
        netcap_tcp_msg_raze( msg );
        return -1;
    }


    struct iphdr *ip_header = (struct iphdr *) msg->pkt->data;
    struct tcphdr *tcp_header = (struct tcphdr*) ( msg->pkt->data + ( 4 * ip_header->ihl ));
    /**
     * Delete the conntrack entry
     */
    netcap_nfconntrack_ipv4_tuple_t tuple;
    netcap_ip_tuple nat_info = msg->pkt->nat_info.original;
    tuple.protocol = ip_header->protocol;
    tuple.src_address = nat_info.src_address;
    tuple.src_port = (u_int16_t)nat_info.src_protocol_id;
    tuple.dst_address = nat_info.dst_address;
    tuple.dst_port = (u_int16_t)nat_info.dst_protocol_id;

    /* This will automatically ignore the error of the entry not existing. */
    if( netcap_nfconntrack_del_entry_tuple( &tuple, 1 ) < 0 ) {
        return errlog( ERR_WARNING,"netcap_nfconntrack_del_entry_tuple\n");
    }

    /**
     * Undo any NATing...
     */

    /* Update the packet to contain the original source and destination tuple */
    ip_header->saddr = msg->pkt->nat_info.original.src_address;
    tcp_header->th_sport = (u_int16_t)msg->pkt->nat_info.original.src_protocol_id;
    ip_header->daddr = msg->pkt->nat_info.original.dst_address;
    tcp_header->th_dport = (u_int16_t)msg->pkt->nat_info.original.dst_protocol_id;

    int tcp_len = ntohs(ip_header->tot_len) - (ip_header->ihl * 4);
    tcp_header->th_sum = 0;
    tcp_header->th_sum = unet_tcp_sum_calc( tcp_len, (u_int8_t*)&ip_header->saddr, (u_int8_t*)&ip_header->daddr, (u_int8_t*)tcp_header );
                                            
    ip_header->check = 0;
    ip_header->check = unet_in_cksum((u_int16_t *) ip_header, sizeof(struct iphdr));


    /**
     * Reinject the SYN packet
     */
    u_int nfmark = msg->pkt->nfmark;
    netcap_virtual_interface_send_pkt( msg->pkt );
    netcap_pkt_action_raze( msg->pkt, NF_DROP );
    msg->pkt = NULL;
    netcap_tcp_msg_raze( msg );
    msg = NULL;

    debug( 6, "TCP: (%"PRIu64") Released SYN :: %s\n", netcap_sess->session_id, netcap_session_cli_tuple_print(netcap_sess));
    
    /* 
     * To test the ACCEPT_MSG replacement, do the following:
     * insert a sleep( 10 ) here, and turn up netcap debugging
     * Run the command nc -p <port> <host on other side> 80
     * Run the command nmap -sS -g <port> <host on other side> -p 80
     * Inside of the logs the following message should appear:
     *   TCP: (1128327673) Dropping new SYN, ACCEPT is already in mb.
     * This means that ACCEPT message replacement has occured.
     * In the nc window, type a string such as GET index.html, and the connection
     * should complete.
     */

    /**
     * wait on connection complete message
     * it is possible to get more syn/ack's during this time, ignore them
     */
    while (1) {
        if (!(msg = mailbox_timed_get(&netcap_sess->tcp_mb,5))) {
            if (errno == ETIMEDOUT) {
                debug(6,"TCP: (%"PRIu64") Missed ACCEPT message\n",netcap_sess->session_id);
                return -1;
            }
            else
                return perrlog("mailbox_timed_get");
        }

        if (msg->type != TCP_MSG_ACCEPT || !msg->fd) {
            if (msg->type == TCP_MSG_SYN && msg->pkt) {
                debug(8,"TCP: (%"PRIu64") DUP syn message, passing\n",netcap_sess->session_id);
                netcap_virtual_interface_send_pkt( msg->pkt );
                netcap_pkt_action_raze( msg->pkt, NF_DROP );
                msg->pkt = NULL;
            } else {
                errlog(ERR_WARNING,"TCP: Invalid message: %i %i 0x%016"PRIxPTR"\n", msg->type, msg->fd, (uintptr_t) msg->pkt);
            }
            netcap_tcp_msg_raze( msg );
            msg = NULL;
            continue;
        }

        break;
    }
        
    fd = msg->fd;
    msg->fd = -1; /* msg no longer owns the fd */
    netcap_tcp_msg_raze( msg );
    msg = NULL;

    netcap_sess->cli_state = CONN_STATE_COMPLETE;
    netcap_sess->client_sock = fd;

    netcap_nfconntrack_update_mark( netcap_sess, nfmark & 0x00ffffff ); // first two bytes unused in connmark currently
                                    
    if (_netcap_tcp_setsockopt_cli(fd,nfmark&0x00ffffff)<0)
        perrlog("_netcap_tcp_setsockopt_cli");

    return 0;
}

int  _netcap_tcp_callback_cli_reject( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    debug( 6, "TCP: (%"PRIu64") Client Reject(%d) %s\n", netcap_sess->session_id, 
           action, netcap_session_cli_tuple_print( netcap_sess ));

    switch ( netcap_sess->cli_state ) {
    case CONN_STATE_INCOMPLETE:
        debug( 6, "TCP: (%"PRIu64") Rejecting Client  :: %s\n", netcap_sess->session_id, 
               netcap_session_tuple_print( netcap_sess ));

        if ( _retrieve_and_reject( netcap_sess, action ) < 0 ) { 
            return errlog( ERR_CRITICAL, "_retrieve_and_reject\n" );
        }
        break;

    case CONN_STATE_COMPLETE:
        /**
         * If already accepted, close then send reset anyway
         */
        debug( 6, "TCP: (%"PRIu64") Client completed, close and reseting  :: %s\n", netcap_sess->session_id, 
               netcap_session_tuple_print( netcap_sess ));

        if ( unet_reset_and_close( netcap_sess->client_sock ) < 0 )
            perrlog( "unet_reset_and_close" );
        netcap_sess->client_sock = -1;
        break;

    case CONN_STATE_NULL:
        /* fallthrough */
    default:
        return errlog( ERR_CRITICAL, "Invalid state (%i), Can't perform action (%i).\n",
                       netcap_sess->cli_state, action );        
    }

    return 0;
}

int  _netcap_tcp_setsockopt_cli( int sock , u_int mark )
{
    int one        = 1;
    int thirty     = 30;
    int sixhundo   = 600;
    int nine       = 9;
    
    if (setsockopt(sock, SOL_SOCKET, SO_KEEPALIVE, &one, sizeof(one))<0)
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_NODELAY,&one,sizeof(one))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_LINGER2,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPIDLE,&sixhundo,sizeof(sixhundo)) < 0 )
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPINTVL,&thirty,sizeof(thirty))<0) 
        perrlog("setsockopt");
    if (setsockopt(sock,SOL_TCP,TCP_KEEPCNT,&nine,sizeof(nine)) < 0 )
        perrlog("setsockopt");

    // we have to swap the src and dst interfaces on the client side
    u_char lo = (mark & 0x000000ff);
    u_char hi = ((mark & 0x0000ff00) >> 16);
    u_int fixer = mark & 0xffff0000;
    fixer |= (lo << 16);
    fixer |= hi;

    struct ip_sendnfmark_opts nfmark = {
        .on = 1,
        .mark = fixer
    };

    if ( setsockopt(sock,SOL_IP,IP_SENDNFMARK_VALUE(),&nfmark,sizeof(nfmark)) < 0 )
        return perrlog( "setsockopt" );

    return 0;
}

int _netcap_tcp_cli_send_reset( netcap_pkt_t* pkt )
{
    struct iphdr*  iph;
    struct tcphdr* tcph;
    u_int16_t tmp16;
    u_int32_t tmp32;
    u_char extra_len = 0;
    u_char tcp_len   = 0;

    if ( pkt == NULL || pkt->data == NULL ) return errlogargs();

    /* XXX Probably want to validate the data length */
    tcph = netcap_pkt_get_tcp_hdr( pkt );
    iph  = netcap_pkt_get_ip_hdr( pkt );

    if ( iph->protocol != IPPROTO_TCP ) {
        return errlog( ERR_CRITICAL, "Attempt to reset non-tcp packet %d\n", iph->protocol );
    }
    
    extra_len = pkt->data_len - ( iph->ihl * 4 ) - sizeof(struct tcphdr);
    tcp_len   = sizeof(struct tcphdr);
    
    /**
     * swap src, dst, src.port, dst.port
     */
    tmp16 = ntohs( iph->tot_len );
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
    tcph->th_ack   = htonl(ntohl(tcph->th_seq)+1);
    tcph->th_flags = TH_RST|TH_ACK;
    tcph->th_seq   = 0;
    tcph->th_win   = 0;
    tcph->th_sum   = 0;
    tcph->th_urp   = 0;
    tcph->th_off   = tcp_len/4;

    /**
     * compute checksum
     */
    tcph->th_sum = unet_tcp_sum_calc( tcp_len, (u_int8_t*)&iph->saddr, (u_int8_t*)&iph->daddr, (u_int8_t*)tcph );
    
    /* send the packet out a raw socket */    
    if ( netcap_raw_send( pkt->data, pkt->data_len-extra_len ) < 0 )
        return perrlog( "netcap_raw_send" );
    
    return 0;
}


static int  _retrieve_and_reject( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    tcp_msg_t* msg;
    int ret = 0;
    int c;
    int drop_pkt;

    for ( c = 0 ; c < _SYN_REJECT_COUNT ; c++ ) {
        drop_pkt = 0xF00D;

        /**
         * read syn message
         */
        if (( msg = mailbox_timed_get( &netcap_sess->tcp_mb, _SYN_RESP_TIMEOUT )) == NULL ) {
            if ( errno == ETIMEDOUT ) {
                if ( c != 0 ) break;
                
                return errlog( ERR_CRITICAL, "TCP: (%"PRIu64") Missed SYN :: %s\n", 
                               netcap_sess->session_id, netcap_session_tuple_print( netcap_sess ));
            } else {
                return errlog( ERR_CRITICAL, "mailbox_timed_get\n" );
            }
        }
        
        if (( msg->type != TCP_MSG_SYN ) || ( msg->pkt == NULL )) {
            errlog( ERR_WARNING, "TCP: (%"PRIu64") Invalid message: %i %i 0x%016"PRIxPTR"\n",
                    netcap_sess->session_id, msg->type, msg->fd, (uintptr_t) msg->pkt );
            netcap_tcp_msg_raze( msg );
            msg = NULL;
            return -1;
        }
        
        switch ( action ) {
        case CLI_RESET:
            debug( 8, "TCP: (%"PRIu64") Resetting Client  :: %s\n", netcap_sess->session_id, 
                   netcap_session_tuple_print( netcap_sess ));
            
            if ( _netcap_tcp_cli_send_reset( msg->pkt ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "_netcap_tcp_cli_send_reset\n" );
            }
            break;
            
        case CLI_ICMP:
            debug( 8, "TCP: (%"PRIu64") Sending ICMP response to client  :: %s\n", netcap_sess->session_id, 
                   netcap_session_tuple_print( netcap_sess ));
            
            if ( _send_icmp_response( netcap_sess, msg->pkt ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "_send_icmp_response\n" );
            }
            break;

        case CLI_FORWARD_REJECT:
            debug( 8, "TCP: (%"PRIu64") Forwarding server rejection to client :: %s\n", netcap_sess->session_id,
                   netcap_session_tuple_print( netcap_sess ));
            if ( _forward_rejection( netcap_sess, msg->pkt ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "_forward_rejection\n" );
            }
            break;
            
        case CLI_DROP:
            break;

        default:
            errlog( ERR_WARNING, "Invalid action(%d), dropping\n", action );
            break;
        }

        /* Only set the verdict if that is necessary */
        if ( drop_pkt == 0 ) netcap_pkt_raze( msg->pkt );
        else                 netcap_pkt_action_raze( msg->pkt, NF_DROP );

        msg->pkt = NULL;
        netcap_tcp_msg_raze( msg );
        msg = NULL;
    }

    return ret;
}

static int  _send_icmp_response( netcap_session_t* netcap_sess, netcap_pkt_t* syn )
{
    u_char data[_ICMP_MAX_RESPONSE_SIZE];
    struct icmp* icmp_pkt;
    int len;
    in_addr_t host;
        
    bzero( &data, sizeof( data ));

    icmp_pkt = (struct icmp*)&data;

    if ( netcap_icmp_verify_type_and_code( netcap_sess->dead_tcp.type, netcap_sess->dead_tcp.code ) < 0 ) {
        return errlog( ERR_WARNING, "netcap_icmp_verify_type_and_code\n" );
    }
    
    /* Build the ICMP message */
    switch( netcap_sess->dead_tcp.type ) {
    case ICMP_REDIRECT:
        icmp_pkt->icmp_gwaddr.s_addr = netcap_sess->dead_tcp.redirect;
        debug( 10, "TCP: ICMP redirect packet to %s\n", unet_next_inet_ntoa( icmp_pkt->icmp_gwaddr.s_addr ));

        /* fallthrough */
    case ICMP_DEST_UNREACH:
        /* fallthrough */
    case ICMP_SOURCE_QUENCH:
        /* fallthrough */
    case ICMP_TIME_EXCEEDED:
        /* fallthrough */
        icmp_pkt->icmp_type = netcap_sess->dead_tcp.type;
        icmp_pkt->icmp_code = netcap_sess->dead_tcp.code;
        /* Copy in the SYN packet */
        len = syn->data_len;

        if ( len + sizeof( struct icmphdr ) > sizeof( data )) {
            len = sizeof( data ) - sizeof( struct icmphdr );
            debug( 10, "TCP: Truncating SYN packet %d -> %d bytes\n", syn->data_len, len );
        }

        debug( 10, "TCP: Updating icmp packet(%d/%d): copying in %d bytes\n", 
               icmp_pkt->icmp_type, icmp_pkt->icmp_code, len );
        memcpy( &icmp_pkt->icmp_ip, syn->data, len );
        
        len += sizeof( struct icmphdr );
        
        /* Updating the checksum */
        icmp_pkt->icmp_cksum = 0;
        icmp_pkt->icmp_cksum = unet_in_cksum((u_int16_t*)icmp_pkt, len );
        
        /* Clear out any marks */
        syn->is_marked = 0;
        syn->nfmark = 0;

        /* Swap the source and dest */
        host = syn->src.host.s_addr;
        syn->src.host.s_addr = syn->dst.host.s_addr;
        
        /* Use the source address from the ICMP packet on the outside if it
         * is relevant */
        if ( netcap_sess->dead_tcp.use_src && ( netcap_sess->dead_tcp.src != (in_addr_t)0 )) {
            syn->src.host.s_addr = netcap_sess->dead_tcp.src;
        }

        syn->dst.host.s_addr = host;

        /* Zero out the ports */
        syn->dst.port = 0;
        syn->src.port = 0;
        
        netcap_icmp_send((char*)icmp_pkt, len, syn );
        
        break;

    default:
        return errlog( ERR_CRITICAL, "Unable to handle ICMP type: %d\n", netcap_sess->dead_tcp.type );
    }

    return 0;
}

static int  _forward_rejection  ( netcap_session_t* netcap_sess, netcap_pkt_t* syn )
{
    switch ( netcap_sess->dead_tcp.exit_type ) {
    case TCP_CLI_DEAD_DROP:
        /* Do nothing */
        break;

    case TCP_CLI_DEAD_RESET:
        if ( _netcap_tcp_cli_send_reset( syn ) < 0 ) {
            return errlog( ERR_CRITICAL, "_netcap_tcp_cli_send_reset\n" );
        }
        break;

    case TCP_CLI_DEAD_ICMP:
        if ( _send_icmp_response( netcap_sess, syn ) < 0 ) {
            return errlog( ERR_CRITICAL, "_netcap_tcp_cli_send_reset\n" );
        }
        break;

    case TCP_CLI_DEAD_NULL:
        /* fallthrough */
    default:
        return errlog( ERR_WARNING, "Invalid server rejection response: %d, dropping\n", 
                       netcap_sess->dead_tcp.exit_type );
    }
    
    return 0;
}
