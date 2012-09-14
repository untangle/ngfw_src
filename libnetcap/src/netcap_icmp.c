/* $HeadURL$ */
#include <stdlib.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <arpa/inet.h>
#include <string.h>
#include <netinet/ip_icmp.h>
#include <netinet/ip.h>
#include <unistd.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/unet.h>
#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_sesstable.h"
#include "netcap_session.h"
#include "netcap_icmp.h"


static struct {
    int send_sock;
} _icmp = {
    .send_sock = -1
};


static int  _netcap_icmp_send( char *data, int data_len, netcap_pkt_t* pkt, int flags );

static struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size);



int  netcap_icmp_init()
{
    int one = 1;

    if (( _icmp.send_sock = socket( AF_INET, SOCK_RAW, IPPROTO_ICMP )) < 0 ) return perrlog( "socket" );

    if ( setsockopt( _icmp.send_sock, SOL_SOCKET, SO_BROADCAST, &one, sizeof(one)) < 0) {
        perrlog( "setsockopt" );
        if ( close( _icmp.send_sock ) < 0 ) perrlog( "close\n" );
        return -1;
    }

    return 0;
}

int  netcap_icmp_cleanup()
{
    int send_sock = _icmp.send_sock;
    _icmp.send_sock = -1;

    if (( send_sock > 0 ) && close( send_sock ) < 0 ) perrlog( "close" );

    return 0;
}

int  netcap_icmp_send( char *data, int data_len, netcap_pkt_t* pkt )
{
    return _netcap_icmp_send( data, data_len, pkt, 0 );
}

int  netcap_icmp_verify_type_and_code( u_int type, u_int code )
{
    if ( type > NR_ICMP_TYPES ) 
        return -1;

    switch ( type ) {
    case ICMP_DEST_UNREACH:
        if ( code > NR_ICMP_UNREACH ) return -1;
        break;

    case ICMP_REDIRECT:
        if ( code > ICMP_REDIRECT_TOSHOST ) return -1;
        break;
    case ICMP_TIME_EXCEEDED:
        if ( code > ICMP_TIMXCEED_REASS ) return -1;
        break;
    case ICMP_PARAMETERPROB:
        if ( code > ICMP_PARAMPROB_OPTABSENT ) return -1;
        break;
    case ICMP_SOURCE_QUENCH:
        /* fallthrough */
    case ICMP_ECHO:
        /* fallthrough */
    case ICMP_ECHOREPLY:
        /* fallthrough */
    case ICMP_TIMESTAMP:
        /* fallthrough */
    case ICMP_TIMESTAMPREPLY:
        /* fallthrough */
    case ICMP_INFO_REQUEST:
        /* fallthrough */
    case ICMP_INFO_REPLY:
        /* fallthrough */
    case ICMP_ADDRESS:
        /* fallthrough */
    case ICMP_ADDRESSREPLY:
        /* fallthrough */
        if ( code != 0 ) return -1;
    }
    
    return 0;
}



static int  _netcap_icmp_send( char *data, int data_len, netcap_pkt_t* pkt, int flags )
{
    struct msghdr      msg;
    struct cmsghdr*    cmsg;
    struct iovec       iov[1];
    struct sockaddr_in dst;
    char               control[4096];
    int                ret;
    u_int              nfmark = ( MARK_ANTISUB | MARK_NOTRACK | (pkt->is_marked ? pkt->nfmark : 0 )); 
    /* mark is  antisub + notrack + whatever packet marks are specified */

    /* if the caller uses the force flag, then override the default bits of the mark */
    if ( pkt->is_marked == IS_MARKED_FORCE_FLAG ) nfmark = pkt->nfmark;

    if ( pkt->dst_intf != NF_INTF_UNKNOWN ) debug( 1, "NETCAP_ICMP: !NC_INTF_UNK Unsupported (IP_DEVICE)\n" );

    /* Setup the destination */
    memset(&dst, 0, sizeof(dst));
    memcpy( &dst.sin_addr, &pkt->dst.host, sizeof(struct in_addr));
    dst.sin_port = 0; /* ICMP does not use ports */
    dst.sin_family = AF_INET;

    msg.msg_name       = &dst;
    msg.msg_namelen    = sizeof( dst );
    msg.msg_iov        = iov;
    iov[0].iov_base    = data;
    iov[0].iov_len     = data_len;
    msg.msg_iovlen     = 1;
    msg.msg_flags      = 0;
    msg.msg_control    = control;
    msg.msg_controllen = 4096;

    /* tos ancillary */
    cmsg = CMSG_FIRSTHDR( &msg );
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->tos));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TOS;
    memcpy( CMSG_DATA(cmsg), &pkt->tos, sizeof(pkt->tos) );

    /* ttl ancillary */
    cmsg = my__cmsg_nxthdr( &msg, cmsg, sizeof(pkt->ttl) );
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len   = CMSG_LEN(sizeof(pkt->ttl));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TTL;
    memcpy( CMSG_DATA(cmsg), &pkt->ttl, sizeof(pkt->ttl) );
    
    /* Source IP ancillary data */
    cmsg = my__cmsg_nxthdr( &msg, cmsg, sizeof(pkt->ttl) );
    if( !cmsg ) {
        errlog( ERR_CRITICAL, "No more CMSG Room\n" );
        goto err_out;
    }
    cmsg->cmsg_len   = CMSG_LEN(sizeof( struct in_addr ));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SADDR_VALUE();
    memcpy( CMSG_DATA(cmsg), &pkt->src.host, sizeof( struct in_addr ));

    /* nfmark */
    cmsg = my__cmsg_nxthdr( &msg, cmsg, sizeof(pkt->ttl) );
    if( !cmsg ) {
        errlog( ERR_CRITICAL, "No more CMSG Room\n" );
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(nfmark));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SENDNFMARK_VALUE();
    memcpy( CMSG_DATA( cmsg ), &nfmark, sizeof(nfmark));

    /* sanity check */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, 0);
    if ( ((char*)cmsg) > control + MAX_CONTROL_MSG)
        errlog(ERR_CRITICAL,"CMSG overrun");

    msg.msg_controllen =
        CMSG_SPACE(sizeof(pkt->src.host)) +
        CMSG_SPACE(sizeof(pkt->tos)) +
        CMSG_SPACE(sizeof(pkt->ttl)) + 
        CMSG_SPACE(sizeof(nfmark));

    /* Send Packet */
    debug( 10, "sending ICMP %s -> %s  data_len:%i ttl:%i tos:%i nfmark:%#10x\n",
           unet_next_inet_ntoa(pkt->src.host.s_addr), 
           unet_next_inet_ntoa(pkt->dst.host.s_addr),
           data_len, pkt->ttl, pkt->tos, nfmark);

    
    if (( ret = sendmsg( _icmp.send_sock, &msg, flags )) < 0 ) {
        errlog( ERR_CRITICAL, "sendmsg: %s | (%s -> %s) len:%i ttl:%i tos:%i nfmark:%#10x\n", errstr,
                unet_next_inet_ntoa( pkt->src.host.s_addr ), unet_next_inet_ntoa( pkt->dst.host.s_addr ),
                data_len, pkt->ttl, pkt->tos, nfmark );            
    }
    
    goto out;

 err_out:
    errlog( ERR_WARNING, "ICMP: Unable to send packet\n" );
    ret = -1;
 out:
    return ret;
}

/**
 * this gets rid of the mess in libc (in bits/socket.h)
 */
static struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size)
{
	struct cmsghdr * ptr;

	ptr = (struct cmsghdr*)(((unsigned char *) cmsg) +  CMSG_ALIGN(cmsg->cmsg_len));

    if ((((char*)ptr) + CMSG_LEN(size)) > ((char*)msg->msg_control + msg->msg_controllen)) {
		return (struct cmsghdr *)0;
    }

	return ptr;
}
