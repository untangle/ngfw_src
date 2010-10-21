/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
#include "netcap_queue.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <sys/wait.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <linux/netfilter.h>
#include <ctype.h>
#include <errno.h>
#include <pthread.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <netinet/ip_icmp.h>

#include <libnfnetlink/libnfnetlink.h>
#include <libnetfilter_queue/libnetfilter_queue.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_nfconntrack.h"


/*
 * input buffer
 *
 * inpack contains:
 *         ip header    (20 octets)
 *         ip options   (0-40 octets (ipoptlen))
 *         icmp header  (8 octets)
 *         timeval      (8 or 12 octets, only if timing==1)
 *         other data
 */

/* maximum length of IP header (including options) */
#define	MAXIPLEN	60
/* max packet contents size */
#define	MAXPAYLOAD	(IP_MAXPACKET - MAXIPLEN - ICMP_MINLEN)

/* This is passed to the nf_callback using TLS */
typedef struct
{
    u_char* buf;
    int buf_len;
    netcap_pkt_t* pkt;
} _nf_callback_args_t;

static struct
{
    pthread_key_t tls_key;
    struct nfq_handle*  nfq_h;
    struct nfq_q_handle* nfq_qh;
    /* socket to accept connections on */
    int nfq_fd;
    int    raw_sock;
} _queue = 
{
    .nfq_h    = NULL,
    .nfq_qh   = NULL,
    .raw_sock = -1,
    .tls_key  = -1
};

/* This is a helper function to retrieve the ctinfo
 */
static int _nfq_get_conntrack( struct nfq_data *nfad, netcap_pkt_t* pkt );


/* This is the callback for netfilter queuing */
static int _nf_callback( struct nfq_q_handle *qh, struct nfgenmsg *nfmsg, struct nfq_data *nfa, void *data );

/* Since this is a callback, have to use tls to pass data back */

int  netcap_queue_init (void)
{
    int _critical_section( void )
    {
        int one = 1;
        
        /* Initialize the TLS key */
        /* no need to use a destruction function, because the value is unset after the 
         * netfilter callback is called */
        if ( pthread_key_create( &_queue.tls_key, NULL ) < 0 ) return perrlog( "pthread_key_create" );

        /* Initialize the raw socket */
        if (( _queue.raw_sock = socket(PF_INET, SOCK_RAW, IPPROTO_RAW )) < 0 ) return perrlog("socket");
        
        if ( setsockopt( _queue.raw_sock, IPPROTO_IP, IP_HDRINCL, (char *) &one, sizeof( one )) < 0 )
            return perrlog( "setsockopt" );
        
        /* initialize the netfilter queue */
        if (( _queue.nfq_h = nfq_open()) == NULL ) return perrlog( "nfq_open" );
        
        /* Unbind any existing queue handlers */
        /* In > 2.6.22, EINVAL is returned if the queue handler isn't register.  So
           we just ignore it. */
        if ( nfq_unbind_pf( _queue.nfq_h, PF_INET ) < 0 && errno != EINVAL ) perrlog( "nfq_unbind_pf" );
        
        /* Bind queue */
        if ( nfq_bind_pf( _queue.nfq_h, PF_INET ) < 0 ) perrlog( "nfq_bind_pf" );
        
        /* Bind the socket to a queue */
        if (( _queue.nfq_qh = nfq_create_queue( _queue.nfq_h,  0, &_nf_callback, NULL )) == NULL ) {
            return perrlog( "nfq_create_queue" );
        }
        
        /* set the copy mode */
        /* set untangle copy mode to include conntrack info */
        if ( nfq_set_mode( _queue.nfq_qh, NFQNL_COPY_PACKET|NFQNL_COPY_UNTANGLE_MODE, 0xFFFF ) < 0){
            return perrlog( "nfq_set_mode" );
        }

        /* Retrieve the file descriptor for the netfilter queue */
        if (( _queue.nfq_fd = nfnl_fd( nfq_nfnlh( _queue.nfq_h ))) <= 0 ) {
            return errlog( ERR_CRITICAL, "nfnl_fd/nfq_nfnlh\n" );
        }

        return 0;
    }
    
    if ( _critical_section() < 0 ) {
        netcap_queue_cleanup();
        return errlog( ERR_CRITICAL, "_critical_section\n" );
    }
    
    return 0;
}

int  netcap_queue_cleanup (void)
{
    /* Cleanup */    
    /* close the queue handler */
    if (( _queue.nfq_qh != NULL ) && ( nfq_destroy_queue( _queue.nfq_qh ) < 0 )) {
        perrlog( "nfq_destroy_queue" );
    }
    
    /* close the queue */
    if ( _queue.nfq_h != NULL ) {
        // Don't unbind on shutdown, if you do other processes
        // that use nfq will stop working.
        // if ( nfq_unbind_pf( _nfqueue.nfq_h, AF_INET) < 0 ) perrlog( "nfq_unbind_pf" );
        if ( nfq_close( _queue.nfq_h ) < 0 ) perrlog( "nfq_close" );
    }

    /* close the raw socket */
    if (( _queue.raw_sock > 0 ) && ( close( _queue.raw_sock ) < 0 )) perrlog("close");
    
    /* null out everything */
    _queue.raw_sock = -1;        
    _queue.nfq_qh = NULL;
    _queue.nfq_h = NULL;
    
    return 0;
}

int  netcap_nfqueue_get_sock (void)
{
    if ( _queue.nfq_h == NULL ) return errlog( ERR_CRITICAL, "QUEUE is not initialized\n" );

    debug( 10, "Handle queue sock: %d\n", _queue.nfq_fd );

    return _queue.nfq_fd;
}

int  netcap_set_verdict ( u_int32_t packet_id, int verdict, u_char* buf, int len)
{
    return netcap_set_verdict_mark( packet_id, verdict, buf, len, 0, 0 );
}

int  netcap_set_verdict_mark( u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, u_int32_t mark )
{
    int nf_verdict = -1;

    switch(verdict) {
    case NF_DROP:
        nf_verdict = NF_DROP;
        break;
    case NF_ACCEPT:
        debug(10, "FLAG: NF_ACCEPTing packet %d with mark %d\n", packet_id, mark);
        nf_verdict = NF_ACCEPT;
        break;
    case NF_STOLEN:
        nf_verdict = NF_STOLEN;
        break;
    case NF_REPEAT:
        nf_verdict = NF_REPEAT;
        break;	
    default:
        errlog(ERR_CRITICAL, "Invalid verdict, dropping packet %d\n", verdict );
        nf_verdict = NF_DROP;
    }

    if ( packet_id == 0 ) return errlog( ERR_CRITICAL, "Unable to set the verdict on packet id 0\n" );
    
    if ( set_mark == 0 ) {
        if ( nfq_set_verdict( _queue.nfq_qh, packet_id, nf_verdict, len, buf ) < 0 ) {
            return perrlog("nfq_set_verdict");
        }
    } else {
        debug( 10, "setting mark to: %#010x\n", mark );
        /* Convert to the proper byte order */
        mark = htonl( mark );
        if ( nfq_set_verdict_mark( _queue.nfq_qh, packet_id, nf_verdict, mark, len, buf ) < 0 ) {
            return perrlog("nfq_set_verdict_mark");
        }
    }

    return 0;    
}

/* The netfiler version of the queue reading function */
int  netcap_nfqueue_read( u_char* buf, int buf_len, netcap_pkt_t* pkt )
{
    int _critical_section( void )
    {
        int pkt_len = 0;
        
        if (( pkt_len = recv( _queue.nfq_fd, buf, buf_len, 0 )) < 0 ) return perrlog( "recv" );
        
        debug( 11, "NFQUEUE Received %d bytes.\n", pkt_len );
        
        if ( nfq_handle_packet( _queue.nfq_h, (char*)buf, pkt_len ) < 0 ) {
            return errlog(ERR_WARNING, "nfq_handle_packet\n" );
        }
        
        debug( 11, "NFQUEUE Packet ID: %#010x.\n", pkt->packet_id );
        
        return 0;
    }
    
    /* Build the arguments for the queuing callback */
    
    /* XXX This is kind of dirty since it is on the stack. */
    _nf_callback_args_t args =
        {
            .buf = buf,
            .buf_len = buf_len,
            .pkt = pkt
        };

    int ret;

    /* set the tls key */
    pthread_setspecific( _queue.tls_key, &args );

    ret = _critical_section();
    
    /* unset the tls key */
    pthread_setspecific( _queue.tls_key, NULL );
    
    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_critical_section\n" );
    return 0;
    
}

int  netcap_raw_send (u_char* pkt, int len)
{
    struct sockaddr_in to;
    struct iphdr* iph = (struct iphdr*)pkt;

    to.sin_family = AF_INET;
    to.sin_port = 0;
    to.sin_addr.s_addr = iph->daddr;

    if (sendto( _queue.raw_sock, pkt, len, 0, (struct sockaddr*)&to, sizeof(struct sockaddr)) < 0 ) {
        return perrlog("sendto");
    }

    return 0;
}

/* This is the callback for netfilter queuing */
static int _nf_callback( struct nfq_q_handle *qh, struct nfgenmsg *nfmsg, struct nfq_data *nfa, void *unused )
{
    u_char* data = NULL;
    int data_len = 0;
    struct iphdr* ip_header = NULL;    
    struct nfqnl_msg_packet_hdr *ph = NULL;
    netcap_pkt_t* pkt = NULL;
    _nf_callback_args_t* args = NULL;

    debug( 10, "Entering callback.\n" );
    
    if (( args = pthread_getspecific( _queue.tls_key  )) == NULL ) {
        return errlog( ERR_CRITICAL, "null args\n" );
    }
    
    if (( ph = nfq_get_msg_packet_hdr( nfa )) == NULL ) return perrlog( "nfq_get_msg_packet_hdr" );

    if (( pkt = args->pkt ) == NULL ) return errlogargs();
    
    pkt->packet_id = ntohl( ph->packet_id );
    
    /* Fill in the values for a packet */
    if ((( data_len = nfq_get_payload( nfa, (char**)&data )) < 0 ) || ( data == NULL )) {
        return perrlog( "nfq_get_payload" );
    }

    ip_header = (struct iphdr*)data;
    
    /* XXX correct? can't be larger than 16 */
    if (( ip_header->ihl < 5 ) || ( ip_header->ihl > 16 )) {
        return errlog( ERR_WARNING, "Dropping illegal IP header length (%i).\n", ip_header->ihl );
    }

    if ( data_len < ntohs( ip_header->tot_len )) return errlogcons();

    if (ip_header->protocol == IPPROTO_ICMP) { 
        debug(10, "not looking up conntrack for ICMP packet\n");
    } else {
        debug(10, "FLAG: Try to get the conntrack information\n");
        if ( _nfq_get_conntrack( nfa, pkt ) < 0 ) {
            netcap_set_verdict(pkt->packet_id, NF_DROP, NULL, 0);
            pkt->packet_id = 0;
            return errlog( ERR_WARNING, "DROPPING PACKET because it has no conntrack info.\n" );
        }

        debug( 10, "Conntrack original info: %s:%d -> %s:%d\n",
               unet_next_inet_ntoa( pkt->nat_info.original.src_address ), 
               ntohs( pkt->nat_info.original.src_protocol_id ),
               unet_next_inet_ntoa( pkt->nat_info.original.dst_address ), 
               ntohs( pkt->nat_info.original.dst_protocol_id ));
    
        debug( 10, "Conntrack reply info: %s:%d -> %s:%d\n",
               unet_next_inet_ntoa( pkt->nat_info.reply.src_address ), 
               ntohs( pkt->nat_info.reply.src_protocol_id ),
               unet_next_inet_ntoa( pkt->nat_info.reply.dst_address ), 
               ntohs( pkt->nat_info.reply.dst_protocol_id ));
    } 
    /**
     * if we are not ICMP, undo any NATing.
     */
    if (ip_header->protocol == IPPROTO_ICMP) { 
        debug(10, "caught ICMP packet\n");
    } else if (( ip_header->saddr == pkt->nat_info.reply.dst_address ) &&
               ( ip_header->daddr == pkt->nat_info.reply.src_address )) {
        /* This is a packet from the original side that has been NATd */
        debug( 10, "QUEUE: Packet from client post NAT.\n");
        ip_header->saddr = pkt->nat_info.original.src_address;
        ip_header->daddr = pkt->nat_info.original.dst_address;
        pkt->queue_type = NETCAP_QUEUE_CLIENT_POST_NAT;
    } else if (( ip_header->saddr == pkt->nat_info.original.dst_address ) &&
               ( ip_header->daddr == pkt->nat_info.original.src_address )) {
        debug( 10, "QUEUE: Packet from server post NAT.\n");
        ip_header->saddr = pkt->nat_info.reply.src_address;
        ip_header->daddr = pkt->nat_info.reply.dst_address;
        pkt->queue_type = NETCAP_QUEUE_SERVER_POST_NAT;
    } else if (( ip_header->saddr == pkt->nat_info.original.src_address ) &&
               ( ip_header->daddr == pkt->nat_info.original.dst_address )) {
        debug( 10, "QUEUE: Packet from client pre NAT.\n");
        pkt->queue_type = NETCAP_QUEUE_CLIENT_PRE_NAT;
    } else if (( ip_header->saddr == pkt->nat_info.reply.src_address ) &&
               ( ip_header->daddr == pkt->nat_info.reply.dst_address )) {
        debug( 10, "QUEUE: Packet from server pre NAT.\n");
        pkt->queue_type = NETCAP_QUEUE_SERVER_PRE_NAT;
    } else {
        return errlog( ERR_CRITICAL, "Packet doesn't match either side of the conntrack data\n" );
    }
    
    ip_header->check = 0;
    ip_header->check = unet_in_cksum((u_int16_t *) ip_header, sizeof(struct iphdr));

    pkt->src.host.s_addr = ip_header->saddr;
    pkt->dst.host.s_addr = ip_header->daddr;
    
    /* Set the ttl, tos and option values */
    pkt->ttl = ip_header->ttl;
    pkt->tos = ip_header->tos;

    /* XXX If necessary, options should be copied out here */
    /* XXX The flags should always be initialized to zero has to be cleared out */
    pkt->th_flags = 0;
    if ( ip_header->protocol == IPPROTO_TCP ) {
        struct tcphdr* tcp_header = (struct tcphdr*) ( data + ( 4 * ip_header->ihl ));
        if ( data_len < ( sizeof( struct iphdr ) + sizeof( struct tcphdr ))) return errlogcons();
        
        switch ( pkt->queue_type ) {
        case NETCAP_QUEUE_CLIENT_POST_NAT:
            tcp_header->dest = (u_int16_t)pkt->nat_info.original.dst_protocol_id;
            tcp_header->source = (u_int16_t)pkt->nat_info.original.src_protocol_id;
            break;
        case NETCAP_QUEUE_SERVER_POST_NAT:
            tcp_header->dest = (u_int16_t)pkt->nat_info.reply.dst_protocol_id;
            tcp_header->source = (u_int16_t)pkt->nat_info.reply.src_protocol_id;
            break;
        default:
            break;
        }

        pkt->src.port = ntohs(tcp_header->source);
        pkt->dst.port = ntohs(tcp_header->dest);
        if ( tcp_header->ack ) pkt->th_flags |= TH_ACK;
        if ( tcp_header->syn ) pkt->th_flags |= TH_SYN;
        if ( tcp_header->fin ) pkt->th_flags |= TH_FIN;
        if ( tcp_header->rst ) pkt->th_flags |= TH_RST;
        if ( tcp_header->urg ) pkt->th_flags |= TH_URG;
        if ( tcp_header->psh ) pkt->th_flags |= TH_PUSH;

        int tcp_len = ntohs(ip_header->tot_len) - (ip_header->ihl * 4);
        debug( 12, "FLAG unet_tcp_sum_calc\n    len_tcp = %d\n    src_addr = %s\n    dst_addr = %s\n",
               tcp_len, unet_next_inet_ntoa(ip_header->saddr),
               unet_next_inet_ntoa(ip_header->daddr));
        tcp_header->check = 0;
        tcp_header->check = unet_tcp_sum_calc( tcp_len,(u_int8_t*)&ip_header->saddr,
                                               (u_int8_t*)&ip_header->daddr, (u_int8_t*)tcp_header );
    }
    else if ( ip_header->protocol == IPPROTO_UDP ) {
        struct udphdr* udp_header = (struct udphdr*) ( data + ( 4 * ip_header->ihl ));
        if ( data_len < (sizeof(struct iphdr) + sizeof( struct udphdr ))) return errlogcons();

        switch ( pkt->queue_type ) {
        case NETCAP_QUEUE_CLIENT_POST_NAT:
            udp_header->dest = (u_int16_t)pkt->nat_info.original.dst_protocol_id;
            udp_header->source = (u_int16_t)pkt->nat_info.original.src_protocol_id;
            break;
        case NETCAP_QUEUE_SERVER_POST_NAT:
            udp_header->dest = (u_int16_t)pkt->nat_info.reply.dst_protocol_id;
            udp_header->source = (u_int16_t)pkt->nat_info.reply.src_protocol_id;
            break;
        default:
            break;
        }
            
        pkt->src.port = ntohs( udp_header->source );
        pkt->dst.port = ntohs( udp_header->dest );

        int udp_len = ntohs(ip_header->tot_len) - (ip_header->ihl * 4);
        debug( 12, "FLAG unet_udp_sum_calc\n    len_tcp = %d\n    src_addr = %s\n    dst_addr = %s\n",
               udp_len, unet_next_inet_ntoa( ip_header->saddr ),
               unet_next_inet_ntoa( ip_header->daddr ));
        udp_header->check = 0;
        udp_header->check = unet_udp_sum_calc( udp_len,(u_int8_t*)&ip_header->saddr,
                                               (u_int8_t*)&ip_header->daddr, (u_int8_t*)udp_header );
    }
    else {
        pkt->src.port = 0;
        pkt->dst.port = 0;
    }

    pkt->buffer = args->buf;
    pkt->data = data;
    pkt->data_len = data_len;
    pkt->proto = ip_header->protocol;
    pkt->nfmark  = nfq_get_nfmark( nfa );

    if ( pkt->nfmark & (MARK_DUPE | MARK_ANTISUB)){
      netcap_set_verdict(pkt->packet_id, NF_DROP, NULL, 0);
      pkt->packet_id = 0;
      return errlog( ERR_WARNING, "Queued a REINJECTED or ANTISUBSCRIBED packet\n");
    }

    if ( netcap_interface_mark_to_intf( pkt->nfmark, &pkt->src_intf ) < 0 ) {
        errlog( ERR_WARNING, "Unable to determine the source interface from mark[%s:%d -> %s:%d]\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port );
    } else {
        debug( 10, "NFQUEUE Input device %d\n", pkt->src_intf );
    }    

    /* First lookup the physdev_out */
    /* if that fails, check the out_dev */
    u_int32_t out_dev = 0;
    if (ip_header->protocol == IPPROTO_ICMP) { 
        debug(10, "ICMP packets dont have outdev info\n");
        pkt->dst_intf = NC_INTF_UNK;
    }else{
        out_dev = nfq_get_physoutdev( nfa );
        if ( out_dev == 0 ) out_dev = nfq_get_outdev( nfa );
        if ( out_dev == 0 ) {
            errlog( ERR_WARNING, "Unable to determine the destination interface with nfq_get_physoutdev\n");
        }
        if (( pkt->dst_intf = netcap_interface_index_to_intf( out_dev )) == 0 ) {
            /* This occurs when the interface is a bridge */
            pkt->dst_intf = NC_INTF_UNK;
        }
    }

    debug( 10, "NFQUEUE Output device %d\n", pkt->dst_intf );

    return 0;
}


static int _nfq_get_conntrack( struct nfq_data *nfad, netcap_pkt_t* pkt )
{
    struct nf_conntrack_tuple* original;
    struct nf_conntrack_tuple* reply;

    if ( nfq_get_conntrack( nfad, &original,  &reply ) < 0 ) {
        errlog( ERR_WARNING, "nfq_get_conntrack could not find conntrack info\n" );
        return -1;
    }

    /* using the union from the nfqueue structure, doesn't matter if
     * this is TCP, UDP, whatever, but it is kind of filthy. */
    pkt->nat_info.original.src_address     = original->src.u3.ip;
    pkt->nat_info.original.src_protocol_id = original->src.u.tcp.port;
    pkt->nat_info.original.dst_address     = original->dst.u3.ip;
    pkt->nat_info.original.dst_protocol_id = original->dst.u.tcp.port;
    
    /* using the union from the nfqueue structure, doesn't matter if
     * this is TCP, UDP, whatever, but it is kind of filthy. */
    pkt->nat_info.reply.src_address     = reply->src.u3.ip;
    pkt->nat_info.reply.src_protocol_id = reply->src.u.tcp.port;
    pkt->nat_info.reply.dst_address     = reply->dst.u3.ip;
    pkt->nat_info.reply.dst_protocol_id = reply->dst.u.tcp.port;

    return 0;
}

