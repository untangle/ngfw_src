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

#include <libnetfilter_queue/libnetfilter_queue.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_interface.h"

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
    int nfq_fd;
    int    raw_sock;
} _queue = 
{
    .nfq_h    = NULL,
    .nfq_qh   = NULL,
    .raw_sock = -1,
    .tls_key  = -1
};

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
        if ( nfq_unbind_pf( _queue.nfq_h, PF_INET ) < 0 ) return perrlog( "nfq_unbind_pf" );
        
        /* Bind queue */
        if ( nfq_bind_pf( _queue.nfq_h, PF_INET ) < 0 ) return perrlog( "nfq_bind_pf" );
        
        /* Bind the socket to a queue */
        if (( _queue.nfq_qh = nfq_create_queue( _queue.nfq_h,  0, &_nf_callback, NULL )) == NULL ) {
            return perrlog( "nfq_create_queue" );
        }
        
        /* set the copy mode */
        if ( nfq_set_mode( _queue.nfq_qh, NFQNL_COPY_PACKET, 0xFFFF ) < 0) return perrlog( "nfq_set_mode" );

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
        if ( nfq_unbind_pf( _queue.nfq_h, AF_INET) < 0 ) perrlog( "nfq_unbind_pf" );
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

int  netcap_set_verdict_mark( u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, 
                              u_int32_t mark )
{
    int nf_verdict = -1;

    switch(verdict) {
    case NF_DROP:
        nf_verdict = NF_DROP;
        break;
    case NF_ACCEPT:
        nf_verdict = NF_ACCEPT;
        break;
    case NF_STOLEN:
        nf_verdict = NF_STOLEN;
        break;
    default:
        errlog(ERR_CRITICAL, "Invalid verdict, dropping packet %d\n", verdict );
        nf_verdict = NF_DROP;
    }
    
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

        if ( nfq_handle_packet( _queue.nfq_h, buf, pkt_len ) < 0 ) perrlog( "nfq_handle_packet" );

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
    struct tcphdr* tcp_header = NULL;
    struct udphdr* udp_header = NULL;
    
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
    if ( ip_header->ihl > 20 ) {
        errlog(ERR_WARNING,"Illogical IP header length (%i), Assuming 5.\n",ntohs(ip_header->ihl));
        tcp_header = (struct tcphdr*) (data + sizeof(struct iphdr));
        udp_header = (struct udphdr*) (data + sizeof(struct iphdr));
    }
    else {
        tcp_header = (struct tcphdr*) ( data + ( 4 * ip_header->ihl ));
        udp_header = (struct udphdr*) ( data + ( 4 * ip_header->ihl ));
    }

    if ( data_len < ntohs( ip_header->tot_len )) return errlogcons();
    
    pkt->src.host.s_addr = ip_header->saddr;
    pkt->dst.host.s_addr = ip_header->daddr;
    
    /* Set the ttl, tos and option values */
    pkt->ttl = ip_header->ttl;
    pkt->tos = ip_header->tos;

    /* XXX If nececssary, options should be copied out here */
    /* XXX The flags should always be initialized to zero has to be cleared out */
    pkt->th_flags = 0;
    if ( ip_header->protocol == IPPROTO_TCP ) {
        if ( data_len < ( sizeof( struct iphdr ) + sizeof( struct tcphdr ))) return errlogcons();
            
        pkt->src.port = ntohs(tcp_header->source);
        pkt->dst.port = ntohs(tcp_header->dest);
        if ( tcp_header->ack ) pkt->th_flags |= TH_ACK;
        if ( tcp_header->syn ) pkt->th_flags |= TH_SYN;
        if ( tcp_header->fin ) pkt->th_flags |= TH_FIN;
        if ( tcp_header->rst ) pkt->th_flags |= TH_RST;
        if ( tcp_header->urg ) pkt->th_flags |= TH_URG;
        if ( tcp_header->psh ) pkt->th_flags |= TH_PUSH;
    }
    else if ( ip_header->protocol == IPPROTO_UDP ) {
        if ( data_len < (sizeof(struct iphdr) + sizeof( struct udphdr ))) return errlogcons();
            
        pkt->src.port = ntohs( udp_header->source );
        pkt->dst.port = ntohs( udp_header->dest );
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
    
    if ( netcap_interface_mark_to_intf( pkt->nfmark, &pkt->src_intf ) < 0 ) {
        errlog( ERR_WARNING, "Unable to determine the source interface from mark[%s:%d -> %s:%d]\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port );
    }

    /* Verify that the marks match, eventually, it should just go to this other method */
    /* First lookup the physdev in */
    u_int32_t dev = nfq_get_physindev( nfa );
    if ( dev == 0 ) dev = nfq_get_indev( nfa );

    debug( 10, "NFQUEUE Input device %d\n", dev );

    /* Verify that the device matches the device indicated from the mark */
    if (( dev == 0 ) || ( netcap_interface_index_to_intf( dev ) != pkt->src_intf )) {
        errlog( ERR_WARNING, "Unable to determine the source interface from nfq [%s:%d -> %s:%d]\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port );
        
    }
    pkt->dst_intf = NC_INTF_UNK;
    
    return 0;
}
