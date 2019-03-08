/* $HeadURL$ */
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
#include <libnetfilter_conntrack/libnetfilter_conntrack.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_interface.h"
#include "netcap_nfconntrack.h"

#define NFQNL_COPY_UNTANGLE_MODE 0x10
#define NETCAP_CTINFO 11
#define NETCAP_CT_DIR_ORIGINAL 12
#define NETCAP_CT_DIR_REPLY 13
#define NETCAP_CT_TUPLE_L3SIZE      4

/* The l3 protocol-specific manipulable parts of the tuple: always in
   network order! */
union netcap_conntrack_address {
        u_int32_t all[NETCAP_CT_TUPLE_L3SIZE];
        __be32 ip;
        __be32 ip6[4];
};

/* The protocol-specific manipulable parts of the tuple: always in
   network order! */
union netcap_conntrack_man_proto
{
        /* Add other protocols here. */
        u_int16_t all;

        struct {
                __be16 port;
        } tcp;
        struct {
                __be16 port;
        } udp;
        struct {
                __be16 id;
        } icmp;
        struct {
                __be16 port;
        } sctp;
        struct {
                __be16 key;     /* GRE key is 32bit, PPtP only uses 16bit */
        } gre;
};

/* The manipulable part of the tuple. */
struct netcap_conntrack_man
{
        union netcap_conntrack_address u3;
        union netcap_conntrack_man_proto u;
        /* Layer 3 protocol */
        u_int16_t l3num;
};

/* This contains the information to distinguish a connection. */
struct netcap_conntrack_tuple
{
        struct netcap_conntrack_man src;

        /* These are the parts of the tuple which are fixed. */
        struct {
                union netcap_conntrack_address u3;
                union {
                        /* Add other protocols here. */
                        u_int16_t all;

                        struct {
                                __be16 port;
                        } tcp;
                        struct {
                                __be16 port;
                        } udp;
                        struct {
                                u_int8_t type, code;
                        } icmp;
                        struct {
                                __be16 port;
                        } sctp;
                        struct {
                                __be16 key;
                        } gre;
                } u;

                /* The protocol. */
                u_int8_t protonum;

                /* The direction (for tuplehash) */
                u_int8_t dir;
        } dst;
};

struct nfq_data {
        struct nfattr **data;
};

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

    struct nfq_handle*  nfq_udp_h;
    struct nfq_handle*  nfq_tcp_h;

    struct nfq_q_handle* nfq_udp_qh;
    int nfq_udp_fd;
    struct nfq_q_handle* nfq_tcp_qh;
    int nfq_tcp_fd;

    int    raw_sock;

} _queue = 
{
    .nfq_udp_h    = NULL,
    .nfq_tcp_h    = NULL,

    .nfq_udp_qh   = NULL,
    .nfq_udp_fd = -1,
    .nfq_tcp_qh   = NULL,
    .nfq_tcp_fd = -1,
    
    .raw_sock = -1,
    .tls_key  = -1
};

/* This is a helper function to retrieve the ctinfo*/
static int _nfq_get_conntrack_info( struct nfq_data *nfad, netcap_pkt_t* pkt, int l3num );
static int _nfq_get_conntrack( struct nfq_data *nfad, netcap_pkt_t* pkt );

/* This is the callback for netfilter queuing */
static int _nf_callback( struct nfq_q_handle *qh, struct nfgenmsg *nfmsg, struct nfq_data *nfa, void *data );

static int _init_nfq( struct nfq_handle** nfq_h );
static int _init_nfqh( struct nfq_q_handle** nfq_qh, int* nfq_fd, int queue_num, struct nfq_handle* nfq_h );

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

        if ( _init_nfq( &_queue.nfq_tcp_h ) < 0 )
            return perrlog( "_init_nfq" );
        if ( _init_nfq( &_queue.nfq_udp_h ) < 0 )
            return perrlog( "_init_nfq" );
        if ( _init_nfqh( &_queue.nfq_tcp_qh, &_queue.nfq_tcp_fd, 1981, _queue.nfq_tcp_h ) < 0 )
            return perrlog( "_init_nfqh" );
        if ( _init_nfqh( &_queue.nfq_udp_qh, &_queue.nfq_udp_fd, 1982, _queue.nfq_udp_h ) < 0 )
            return perrlog( "_init_nfqh" );
        
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
    if (( _queue.nfq_udp_qh != NULL ) && ( nfq_destroy_queue( _queue.nfq_udp_qh ) < 0 )) {
        perrlog( "nfq_destroy_queue" );
    }
    if (( _queue.nfq_tcp_qh != NULL ) && ( nfq_destroy_queue( _queue.nfq_tcp_qh ) < 0 )) {
        perrlog( "nfq_destroy_queue" );
    }
    
    /* close the queue */
    if ( _queue.nfq_udp_h != NULL ) {
        // Don't unbind on shutdown, if you do other processes
        // that use nfq will stop working.
        // if ( nfq_unbind_pf( _nfqueue.nfq_udp_h, AF_INET) < 0 ) perrlog( "nfq_unbind_pf" );
        if ( nfq_close( _queue.nfq_udp_h ) < 0 ) perrlog( "nfq_close" );
    }
    if ( _queue.nfq_tcp_h != NULL ) {
        // Don't unbind on shutdown, if you do other processes
        // that use nfq will stop working.
        // if ( nfq_unbind_pf( _nfqueue.nfq_udp_h, AF_INET) < 0 ) perrlog( "nfq_unbind_pf" );
        if ( nfq_close( _queue.nfq_tcp_h ) < 0 ) perrlog( "nfq_close" );
    }

    
    /* close the raw socket */
    if (( _queue.raw_sock > 0 ) && ( close( _queue.raw_sock ) < 0 )) perrlog("close");
    
    /* null out everything */
    _queue.raw_sock = -1;        
    _queue.nfq_udp_qh = NULL;
    _queue.nfq_tcp_qh = NULL;
    _queue.nfq_udp_h = NULL;
    _queue.nfq_tcp_h = NULL;
    
    return 0;
}

int  netcap_nfqueue_get_udp_sock (void)
{
    if ( _queue.nfq_udp_h == NULL ) return errlog( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_udp_fd;
}

struct nfq_handle*   netcap_nfqueue_get_udp_nfq ( void )
{
    if ( _queue.nfq_udp_h == NULL ) return errlog_null( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_udp_h;
}

struct nfq_q_handle* netcap_nfqueue_get_udp_nfqh ( void )
{
    if ( _queue.nfq_udp_h == NULL ) return errlog_null( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_udp_qh;
}

int  netcap_nfqueue_get_tcp_sock (void)
{
    if ( _queue.nfq_tcp_h == NULL ) return errlog( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_tcp_fd;
}

struct nfq_handle*   netcap_nfqueue_get_tcp_nfq ( void )
{
    if ( _queue.nfq_tcp_h == NULL ) return errlog_null( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_tcp_h;
}

struct nfq_q_handle* netcap_nfqueue_get_tcp_nfqh ( void )
{
    if ( _queue.nfq_tcp_h == NULL ) return errlog_null( ERR_CRITICAL, "QUEUE is not initialized\n" );
    return _queue.nfq_tcp_qh;
}

int  netcap_set_verdict ( struct nfq_q_handle* nfq_qh, u_int32_t packet_id, int verdict, u_char* buf, int len)
{
    return netcap_set_verdict_mark( nfq_qh, packet_id, verdict, buf, len, 0, 0 );
}

int  netcap_set_verdict_mark( struct nfq_q_handle* nfq_qh, u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, u_int32_t mark )
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
    case NF_REPEAT:
        nf_verdict = NF_REPEAT;
        break;	
    default:
        errlog(ERR_CRITICAL, "Invalid verdict, dropping packet %d\n", verdict );
        nf_verdict = NF_DROP;
    }

    if ( packet_id == 0 ) return errlog( ERR_CRITICAL, "Unable to set the verdict on packet id 0\n" );
    
    if ( set_mark == 0 ) {
        if ( nfq_set_verdict( nfq_qh, packet_id, nf_verdict, len, buf ) < 0 ) {
            return perrlog("nfq_set_verdict");
        }
    } else {
        debug( 10, "setting mark to: %#010x\n", mark );
        /* Convert to the proper byte order */
        if ( nfq_set_verdict2( nfq_qh, packet_id, nf_verdict, mark, len, buf ) < 0 ) {
            return perrlog("nfq_set_verdict_mark");
        }
    }

    return 0;    
}

/* The netfiler version of the queue reading function */
int  netcap_nfqueue_read( struct nfq_handle*  nfq_h, struct nfq_q_handle* nfq_qh, int nfq_fd, u_char* buf, int buf_len, netcap_pkt_t* pkt )
{
    int _critical_section( void )
    {
        int pkt_len = 0;
        
        if (( pkt_len = recv( nfq_fd, buf, buf_len, 0 )) < 0 ) return perrlog( "recv" );
        pkt->nfq_h = nfq_h;
        pkt->nfq_qh = nfq_qh;
        
        debug( 11, "NFQUEUE Received %d bytes.\n", pkt_len );
        if ( nfq_handle_packet( nfq_h, (char*)buf, pkt_len ) < 0 ) {
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
    int l3num = 0;
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
    if ((( data_len = nfq_get_payload( nfa, (unsigned char**)&data )) < 0 ) || ( data == NULL )) {
        return perrlog( "nfq_get_payload" );
    }

    ip_header = (struct iphdr*)data;
    
    if (( ip_header->ihl < 5 ) || ( ip_header->ihl > 16 )) {
        return errlog( ERR_WARNING, "Dropping illegal IP header length (%i).\n", ip_header->ihl );
    }

    if ( data_len < ntohs( ip_header->tot_len )) return errlogcons();

    if ( IS_NEW_KERNEL() ) {  
        /*Get the original and reply tuple from conntrack */  
        l3num = nfmsg->nfgen_family; 
    
        if ( _nfq_get_conntrack_info( nfa, pkt, l3num ) < 0 ) {
            netcap_set_verdict( pkt->nfq_qh, pkt->packet_id, NF_DROP, NULL, 0 );
            pkt->packet_id = 0;
            return errlog( ERR_WARNING, "DROPPING PACKET because it has no conntrack info.\n" );
        }
    }
    else { 
        if ( _nfq_get_conntrack( nfa, pkt ) < 0 ) {
            netcap_set_verdict( pkt->nfq_qh, pkt->packet_id, NF_DROP, NULL, 0 );
            pkt->packet_id = 0;
            return errlog( ERR_WARNING, "DROPPING PACKET because it has no conntrack info.\n" );
        }
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

    debug( 10, "packet info src= %s -> %s\n",
            unet_next_inet_ntoa(ip_header->saddr),
            unet_next_inet_ntoa(ip_header->daddr));
       
    /**
     * undo any NATing.
     */
    if (( ip_header->saddr == pkt->nat_info.reply.dst_address ) &&
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
        tcp_header->check = 0;
        tcp_header->check = unet_tcp_sum_calc( tcp_len,(u_int8_t*)&ip_header->saddr,(u_int8_t*)&ip_header->daddr, (u_int8_t*)tcp_header );
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
        debug( 12, "unet_udp_sum_calc\n    len_tcp = %d\n    src_addr = %s\n    dst_addr = %s\n",
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

    if ( pkt->nfmark & MARK_BYPASS) {
        netcap_set_verdict( pkt->nfq_qh, pkt->packet_id, NF_DROP, NULL, 0 );
        pkt->packet_id = 0;
        return errlog( ERR_WARNING, "Queued a bypassed packet\n");
    }

    if ( netcap_interface_mark_to_cli_intf( pkt->nfmark, &pkt->src_intf ) < 0 ) {
        errlog( ERR_WARNING, "Unable to determine the source interface from mark[%s:%d -> %s:%d] mark:%08x\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port, pkt->nfmark );
    } else {
        debug( 10, "NFQUEUE Input device %d\n", pkt->src_intf );
    }    

    if ( netcap_interface_mark_to_srv_intf( pkt->nfmark, &pkt->dst_intf ) < 0 ) {
        /* this is best effort - the session may not be marked. don't complain if it isnt */
        /*         errlog( ERR_WARNING, "Unable to determine the dest interface from mark[%s:%d -> %s:%d]\n", */
        /*                 unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port, */
        /*                 unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port ); */
    } else {
        debug( 10, "NFQUEUE Output device %d\n", pkt->dst_intf );
    }    

    return 0;
}

/*Helper function to get conntrack related info in kernel version >= 3.10*/
int nfq_get_ct_info(struct nfq_data *nfad, unsigned char **data)
{
    *data = (unsigned char *)
        nfnl_get_pointer_to_data(nfad->data, NFQA_CT, struct nf_conntrack );

    if (*data)
        return NFA_PAYLOAD(nfad->data[NFQA_CT-1]);


    return errlog( ERR_CRITICAL, "nfnl_get_pointer_to_data(NFQA_CT) returned NULL\n" );
}

/*This function is used to get conntrack info in kernel version >= 3.10*/
static int _nfq_get_conntrack_info( struct nfq_data *nfad, netcap_pkt_t* pkt, int l3num )
{
    struct nf_conntrack *ct;
    int ct_len =0;
    unsigned char *ct_data;
    
    ct_len = nfq_get_ct_info(nfad, &ct_data);
    if ( ct_len <= 0 ) {
        return errlog( ERR_WARNING, "nfq_get_ct_info returned error.\n" );
    }

    ct = nfct_new();
    if ( !ct ) {
        return errlog( ERR_WARNING, "nfct_new failed\n" );
    }

    if (nfct_payload_parse((void *)ct_data, ct_len, l3num, ct ) < 0) {
        nfct_destroy( ct );
        return errlog( ERR_WARNING, "nfq_payload_parse returned error.\n" );
    }

    pkt->nat_info.original.src_address     = nfct_get_attr_u32(ct,ATTR_ORIG_IPV4_SRC);
    pkt->nat_info.original.src_protocol_id = nfct_get_attr_u16(ct,ATTR_ORIG_PORT_SRC);
    pkt->nat_info.original.dst_address     = nfct_get_attr_u32(ct,ATTR_ORIG_IPV4_DST); 
    pkt->nat_info.original.dst_protocol_id = nfct_get_attr_u16(ct,ATTR_ORIG_PORT_DST); 
    
    pkt->nat_info.reply.src_address     = nfct_get_attr_u32(ct,ATTR_REPL_IPV4_SRC);
    pkt->nat_info.reply.src_protocol_id = nfct_get_attr_u16(ct,ATTR_REPL_PORT_SRC);
    pkt->nat_info.reply.dst_address     = nfct_get_attr_u32(ct,ATTR_REPL_IPV4_DST);
    pkt->nat_info.reply.dst_protocol_id = nfct_get_attr_u16(ct,ATTR_REPL_PORT_DST);

    nfct_destroy( ct );
    return 0;
}

/*This function is used to get conntrack info in kernel version <= 3.2 */
int nfq_get_conntrack(struct nfq_data *nfad, struct netcap_conntrack_tuple** original, struct netcap_conntrack_tuple** reply )
{
       *original = nfnl_get_pointer_to_data(nfad->data, NETCAP_CT_DIR_ORIGINAL, struct netcap_conntrack_tuple);
       if (*original==NULL) return -1;
       *reply = nfnl_get_pointer_to_data(nfad->data, NETCAP_CT_DIR_REPLY, struct netcap_conntrack_tuple);
       if (*reply==NULL) return -1;

       return 0;
}

/*This function is used to get conntrack info in kernel version <= 3.2 */
static int _nfq_get_conntrack( struct nfq_data *nfad, netcap_pkt_t* pkt )
{
    struct netcap_conntrack_tuple* original;
    struct netcap_conntrack_tuple* reply;

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

static int _init_nfq( struct nfq_handle** nfq_h )
{
    /* initialize the netfilter queue */
    if (( *nfq_h = nfq_open()) == NULL ) return perrlog( "nfq_open" );
        
    /* Unbind any existing queue handlers */
    /* In > 2.6.22, EINVAL is returned if the queue handler isn't registered.  So
       we just ignore it. */
    if ( nfq_unbind_pf( *nfq_h, PF_INET ) < 0 && errno != EINVAL ) perrlog( "nfq_unbind_pf" );
        
    /* Bind queue */
    if ( nfq_bind_pf( *nfq_h, PF_INET ) < 0 ) perrlog( "nfq_bind_pf" );

    return 0;
}

static int _init_nfqh( struct nfq_q_handle** nfq_qh, int* nfq_fd, int queue_num, struct nfq_handle* nfq_h )
{
        
    /* Bind the socket to a queue */
    if (( *nfq_qh = nfq_create_queue( nfq_h,  queue_num, &_nf_callback, NULL )) == NULL ) {
        return perrlog( "nfq_create_queue" );
    }
        
    /* set the copy mode */
    if ( IS_NEW_KERNEL() ){  
        if ( nfq_set_mode( *nfq_qh, NFQNL_COPY_PACKET, 0xFFFF ) < 0){
            return perrlog( "nfq_set_mode" );
        }
    
        /*NFQA_CFG_F_FAIL_OPEN is not supported in buffalo 3.10 kernel*/
        if ( IS_NEW_KERNEL() >= 316 ){  
            if ( nfq_set_queue_flags( *nfq_qh, NFQA_CFG_F_FAIL_OPEN,  NFQA_CFG_F_FAIL_OPEN ) ) {
                return perrlog( "nfq_set_queue_flags NFQA_CFG_F_FAIL_OPEN" );
            }
        }

        if ( nfq_set_queue_flags( *nfq_qh, NFQA_CFG_F_CONNTRACK,  NFQA_CFG_F_CONNTRACK ) ) {
            return perrlog( "nfq_set_queue_flags NFQA_CFG_F_CONNTRACK" );
        }
    }
    else { 
        /* set untangle copy mode to include conntrack info */
        if ( nfq_set_mode( *nfq_qh, NFQNL_COPY_PACKET|NFQNL_COPY_UNTANGLE_MODE, 0xFFFF ) < 0 ) {
            return perrlog( "nfq_set_mode" );
        }
    }

    /* Retrieve the file descriptor for the netfilter queue */
    if (( *nfq_fd = nfnl_fd( nfq_nfnlh( nfq_h ))) <= 0 ) {
        return errlog( ERR_CRITICAL, "nfnl_fd/nfq_nfnlh\n" );
    }

    int bufsize = 1048576*2; /* 2 meg */
    if ( setsockopt( *nfq_fd, SOL_SOCKET, SO_RCVBUF, &bufsize, sizeof( bufsize )) < 0 ) {
        perrlog("setsockopt");
    }

    return 0;
}
