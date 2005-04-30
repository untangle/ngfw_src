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
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
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

static struct ipq_handle *ipq_h;
static int    rawsock;

int  netcap_queue_init (void)
{
    int one = 1;
    
    if ((rawsock = socket(PF_INET, SOCK_RAW, IPPROTO_RAW))<0)  
        return perrlog("socket");
    if (setsockopt(rawsock, IPPROTO_IP, IP_HDRINCL, (char *) &one, sizeof(one)) < 0)
        return perrlog("setsockopt");
    
    if (!(ipq_h = ipq_create_handle(0, PF_INET)))
        return perrlog("ipq_create_handle");

    if (ipq_set_mode(ipq_h, IPQ_COPY_PACKET, QUEUE_BUFSIZE)<0) {
        ipq_destroy_handle(ipq_h);
        return perrlog("ipq_set_mode");
    }

    return 0;
}

int  netcap_queue_cleanup (void)
{
    if (ipq_destroy_handle(ipq_h)<0)
        perrlog("ipq_destroy_handle");

    if (close(rawsock)<0)
        perrlog("close");
    
    return 0;
}

int  netcap_queue_get_sock (void)
{
    return ipq_h->fd;
}

int  netcap_set_verdict (u_long packet_id, int verdict, char *buffer, int len)
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
        return errlog(ERR_CRITICAL,"Invalid verdict.\n");
    }
    
    if (ipq_set_verdict(ipq_h, packet_id, nf_verdict, len, buffer)<0)
        return perrlog("ipq_set_verdict\n");

    return 0;
}

int  netcap_queue_read (char *buffer, int max, netcap_pkt_t* pkt)
{
    int status;
    ipq_packet_msg_t* msg;
    char* p;
    struct iphdr* iph;
    struct tcphdr* tcph;
    struct udphdr* udph;
        
    status = ipq_read(ipq_h, buffer, max, 0 );

    if ( status == 0 ) {
        errlog(ERR_WARNING,"ipq_read: %s\n", strerror(ipq_get_msgerr(buffer)));
        return 0;
    }
    
    if (status < 0) 
        return errlog(ERR_CRITICAL,"ipq_read: %s\n", strerror(ipq_get_msgerr(buffer)));

    switch (ipq_message_type(buffer)) {

    case IPQM_PACKET: 
        msg = ipq_get_packet(buffer);
        p = msg->payload;
        break;

    case NLMSG_ERROR:
        return errlog(ERR_CRITICAL,"ipq_message_type: %s\n", strerror(ipq_get_msgerr(buffer)));
        break;

    default:
        return errlog(ERR_CRITICAL,"ipq_message_type: Unknown ret: %s\n", strerror(ipq_get_msgerr((buffer))));
        break;
    }

    iph  = (struct iphdr*)  p;

    /* XXX correct? can't be larger than 16 */
    if (iph->ihl > 20) {
        errlog(ERR_WARNING,"Illogical IP header length (%i), Assuming 5.\n",ntohs(iph->ihl));
        tcph = (struct tcphdr*) (p+sizeof(struct iphdr));
        udph = (struct udphdr*) (p+sizeof(struct iphdr));
    }
    else {
        tcph = (struct tcphdr*) (p+(4*iph->ihl));
        udph = (struct udphdr*) (p+(4*iph->ihl));
    }

    if (msg->data_len < ntohs(iph->tot_len))
        return errlogcons();
    
    pkt->src.host.s_addr = iph->saddr;
    pkt->dst.host.s_addr = iph->daddr;
    
    /* Set the ttl, tos and option values */
    pkt->ttl = iph->ttl;
    pkt->tos = iph->tos;

    /* XXX If nececssary, options should be copied out here */

    if (iph->protocol == IPPROTO_TCP) {
        if (msg->data_len < (sizeof(struct iphdr)+sizeof(struct tcphdr)))
            return errlogcons();
        pkt->src.port = ntohs(tcph->source);
        pkt->dst.port = ntohs(tcph->dest);
        if(tcph->ack)  pkt->th_flags |= TH_ACK;
        if(tcph->syn)  pkt->th_flags |= TH_SYN;
        if(tcph->fin)  pkt->th_flags |= TH_FIN;	    
        if(tcph->rst)  pkt->th_flags |= TH_RST;	    
        if(tcph->urg)  pkt->th_flags |= TH_URG;	    
        if(tcph->psh)  pkt->th_flags |= TH_PUSH;	
    }
    else if (iph->protocol == IPPROTO_UDP) {
        if (msg->data_len < (sizeof(struct iphdr)+sizeof(struct udphdr)))
            return errlogcons();
        pkt->src.port = ntohs(udph->source);
        pkt->dst.port = ntohs(udph->dest);
    }
    else {
        pkt->src.port = 0;
        pkt->dst.port = 0;
    }

    pkt->packet_id = msg->packet_id;
    pkt->buffer = buffer;
    pkt->data = msg->payload;
    pkt->data_len = msg->data_len;
    pkt->proto = iph->protocol;
    pkt->nfmark  = (u_int)msg->mark;
    
    netcap_interface_mark_to_intf(msg->mark,&pkt->src.intf);
    pkt->dst.intf = NC_INTF_UNK;

    return msg->data_len;
}

int  netcap_raw_send (char* pkt, int len)
{
    struct sockaddr_in to;
    struct iphdr* iph = (struct iphdr*)pkt;

    to.sin_family = AF_INET;
    to.sin_port = 0;
    to.sin_addr.s_addr = iph->daddr;
    
    if (sendto(rawsock, pkt, len, 0, (struct sockaddr*)&to, sizeof(struct sockaddr))<0)
        return perrlog("sendto");

    return 0;
}
