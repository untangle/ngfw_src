/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_icmp.c,v 1.2 2004/12/06 21:15:46 rbscott Exp $
 */
#include "netcap_icmp.h"

#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <netinet/ip_icmp.h>
#include <netdb.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/unet.h>
#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_queue.h"
#include "netcap_globals.h"


static int raw_sock;		

int  netcap_icmp_init()
{
    struct protoent *proto = getprotobynumber(IPPROTO_ICMP);
    if (!proto) 
        return perrlog("getprotobyname");

    if ((raw_sock = socket(AF_INET, SOCK_RAW, proto->p_proto))<0)
        return perrlog("socket");

    return 0;
}

int  netcap_icmp_cleanup()
{
    if (close(raw_sock)<0)
        perrlog("close");

    return 0;
}

int  netcap_icmp_send (char *data, int data_len, netcap_pkt_t* pkt)
{
	struct		icmp *icp;
	struct		in_addr in;
	int                ret;
	struct msghdr      msg;
	struct iovec       iov[1];
	struct cmsghdr*    cmsg;
	char               control[MAX_CONTROL_MSG];
    struct sockaddr_in whereto;


	icp = (struct icmp*)data;
//	icp->icmp_type = ICMP_ECHOREPLY;
//	icp->icmp_code = 0;
//	icp->icmp_cksum = 0;
//	icp->icmp_seq = 0;
//	icp->icmp_id = 0;
//printf("------> %08x %08x %08x\n", icp->icmp_code, icp->icmp_seq, icp->icmp_id);

	/* compute ICMP checksum here */
	icp->icmp_cksum = unet_in_cksum((u_short *)data, data_len);

	memset(&whereto, 0, sizeof(whereto));
	whereto.sin_family = AF_INET;
	whereto.sin_addr.s_addr = pkt->dst.host.s_addr;
    
    msg.msg_name       = &whereto;
    msg.msg_namelen    = sizeof(whereto);
    msg.msg_iov        = iov;
    iov[0].iov_base    = data;
    iov[0].iov_len     = data_len;
    msg.msg_iovlen     = 1;
    msg.msg_flags      = 0;
    msg.msg_control    = control;
    msg.msg_controllen = MAX_CONTROL_MSG;

    in.s_addr = pkt->src.host.s_addr;
    cmsg = CMSG_FIRSTHDR(&msg);
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(struct in_addr));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SADDR;
    memcpy(CMSG_DATA(cmsg), &in, sizeof(struct in_addr));
    msg.msg_controllen = CMSG_SPACE(sizeof(struct in_addr));
    

    if ((ret = sendmsg(raw_sock,&msg,0))<0)
        errlog(ERR_CRITICAL,"sendmsg: %s \n",strerror(errno));
    
    return 0;
}
