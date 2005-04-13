/* $Id$ */
#include "netcap_udp.h"

#include <stdlib.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/uthread.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_hook.h"
#include "netcap_globals.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"
#include "netcap_interface.h"
#include "netcap_shield.h"

#define MAX_MB_SIZE    16

static int _udpsend_fd;

static int _netcap_udp_sendto(int sock, void* buf, size_t len, int flags, netcap_pkt_t* pkt);

static struct cmsghdr * my__cmsg_nxthdr(void *__ctl, size_t __size, struct cmsghdr *__cmsg);

int  netcap_udp_init ()
{
    int one=1;

    /**
     * create the socket used to send/spoof outgoing udp packets
     */
    if(( _udpsend_fd = socket(AF_INET,SOCK_DGRAM,0)) < 0) {
        errlog( ERR_CRITICAL, "Unable to open udp send socket\n" );
        return perrlog ( "socket" );
    }

    /**
     * set all the needed socket options
     */
    if (setsockopt(_udpsend_fd, SOL_SOCKET, SO_BROADCAST,&one, sizeof(one)) < 0) {
        return perrlog ( "setsockopt" );
    }

    return 0;
}

int  netcap_udp_cleanup()
{
    if ( _udpsend_fd > 0 && ( close( _udpsend_fd ) < 0 )) perrlog("close");

    return 0;
}

int  netcap_udp_send (char* data, int data_len, netcap_pkt_t* pkt)
{
    if ( !data || !pkt ) return -1;
    if ( !data_len ) return 0;
        
    return _netcap_udp_sendto(_udpsend_fd, data, data_len, 0, pkt); 
}

int  netcap_udp_recvfrom (int sock, void* buf, size_t len, int flags, netcap_pkt_t* pkt)
{
    struct msghdr      msg;
    struct iovec       iov[1];
    struct cmsghdr*    cmsg;
    char               control[MAX_CONTROL_MSG];
    struct sockaddr_in  cli;
    int numread;
 
    if(!buf || !pkt) return -1;

    /* fill in the members of the msghdr stucture */
    msg.msg_iov     = iov;
    iov[0].iov_base = buf;
    iov[0].iov_len  = len;
    msg.msg_iovlen  = 1;
    msg.msg_name    = &cli;
    msg.msg_namelen = sizeof(struct sockaddr);
    msg.msg_flags   = 0;
    msg.msg_control    = control;
    msg.msg_controllen = sizeof(control);

    if ((numread = recvmsg(sock,&msg,0))<0) {
        fprintf(stderr,"recvmsg error: %s\n",strerror(errno));
        return -1;
    }
        
    for (cmsg = CMSG_FIRSTHDR(&msg); cmsg != NULL;cmsg = CMSG_NXTHDR(&msg, cmsg)) {
        int unknown;
        struct in_pktinfo* pkti;

        unknown = 0;
        if ( cmsg->cmsg_level == SOL_IP ) {
            switch ( cmsg->cmsg_type ) {
            case IP_PKTINFO:
                pkti = (struct in_pktinfo*) CMSG_DATA(cmsg);
                memcpy(&pkt->dst.host,&pkti->ipi_addr,sizeof(struct in_addr));
                break;

            case IP_TTL: pkt->ttl = *(u_char*)CMSG_DATA(cmsg); break;
            case IP_TOS: pkt->tos = *(u_char*)CMSG_DATA(cmsg); break;
            case IP_RECVNFMARK: pkt->nfmark = *(u_int*)CMSG_DATA(cmsg); break;
            case IP_RETOPTS:
                pkt->opts_len = cmsg->cmsg_len-CMSG_LEN(0);
                pkt->opts = malloc(pkt->opts_len);
                if( !pkt->opts ) return errlogmalloc();
                memcpy(pkt->opts,CMSG_DATA(cmsg),pkt->opts_len);
                break;
            default: unknown = 1;
            }
        } else if  ( cmsg->cmsg_level == SOL_UDP ) {
            switch ( cmsg->cmsg_type ) {
            case UDP_RECVDPORT: pkt->dst.port = ntohs(*(u_short*)CMSG_DATA(cmsg)); break;
            default: unknown = 1;
            }
        } else {
            unknown = 1;
        }
        
        if ( unknown ) {
            errlog(ERR_WARNING,"unknown ancillary data, level=%i, type=%i, len=%i\n",
                   cmsg->cmsg_level, cmsg->cmsg_type, cmsg->cmsg_len);
        }
    }
    
    /* Set the source interface for the packet */
    if ( netcap_interface_mark_to_intf(pkt->nfmark,&pkt->src.intf) < 0) {
        errlog(ERR_WARNING,"Unable to determine the source interface from mark\n");
    }
    
    /* Clear the output interface */
    pkt->dst.intf = NC_INTF_UNK;

    memcpy(&pkt->src.host,&cli.sin_addr,sizeof(struct in_addr));
    pkt->src.port = ntohs(cli.sin_port);
    pkt->proto = IPPROTO_UDP;
    
    return numread;
}

int  netcap_udp_call_hooks (netcap_pkt_t* pkt, void* arg)
{
    netcap_session_t* session;
    mailbox_t* mb = NULL;
    char address[20];

    SESSTABLE_WRLOCK();

    // First check to see if the session already exists.
    session = netcap_nc_sesstable_get_tuple (!NC_SESSTABLE_LOCK, IPPROTO_UDP,
                                             pkt->src.host.s_addr,
                                             pkt->dst.host.s_addr,
                                             pkt->src.port,pkt->dst.port, 0);
    
    // If it doesn't, intialize the session.
    if ( !session ) {
        /* XXX Are shield errors considered catastrophic */
        if ( netcap_shield_rep_add_request  ( pkt->src.host.s_addr ) < 0 ) {
            errlog ( ERR_CRITICAL, "netcap_shield_rep_add_session\n" );
        }
        
        switch ( netcap_shield_rep_check ( pkt->src.host.s_addr )  ) {
        case NC_SHIELD_DROP:
        case NC_SHIELD_RESET:
            netcap_pkt_raze ( pkt );
            SESSTABLE_UNLOCK();
            strncpy ( address, unet_inet_ntoa ( pkt->src.host.s_addr ), sizeof ( address ) );
            /* XXX Change the debug level */
            errlog ( ERR_WARNING, "UDP: Shield rejected session: %s:%d -> %s:%d\n", 
                     address, pkt->src.port, unet_inet_ntoa ( pkt->dst.host.s_addr ), pkt->dst.port );
            return 0;
        case NC_SHIELD_YES:
        case NC_SHIELD_LIMITED:
            break;

        default:
            errlog ( ERR_CRITICAL, "netcap_shield_rep_check\n" );
        }

        /* XXX Check if you should always end UDP sessions */
        if ( netcap_shield_rep_add_session ( pkt->src.host.s_addr ) < 0 ) {
            errlog ( ERR_CRITICAL, "netcap_shield_rep_add_session\n" );
        }

        // Create a UDP session
        session = netcap_udp_session_create (pkt);
        
        if ( !session ) {
            // Drop the packet
            netcap_pkt_raze(pkt);
            SESSTABLE_UNLOCK();
            return perrlog("netcap_udp_session_create");
        }

        // Add the session to the table
        if ( netcap_nc_sesstable_add_tuple ( !NC_SESSTABLE_LOCK, session, IPPROTO_UDP,
                                             pkt->src.host.s_addr,
                                             pkt->dst.host.s_addr,
                                             pkt->src.port,pkt->dst.port,0 ) ) {
            netcap_udp_session_raze(!NC_SESSTABLE_LOCK, session);
            netcap_pkt_raze(pkt);
            SESSTABLE_UNLOCK();
            return perrlog("netcap_sesstable_add_tuple");
        }

        // Add the session to itself
        if ( netcap_nc_sesstable_add ( !NC_SESSTABLE_LOCK, session )) {
            netcap_udp_session_raze(!NC_SESSTABLE_LOCK, session);
            netcap_pkt_raze(pkt);
            SESSTABLE_UNLOCK();
            return perrlog("netcap_sesstable_add_tuple");
        }

        // Dump the packet into the mailbox

        // Put the packet into the mailbox
        if (mailbox_size(&session->cli_mb) > MAX_MB_SIZE ) {
            errlog(ERR_WARNING,"Mailbox Full: Dropping Packet (from %s:%i)\n",
                   inet_ntoa(pkt->src.host),pkt->src.port);
            netcap_pkt_raze(pkt);
        } else if (mailbox_put(&session->cli_mb,(void*)pkt)<0) {
            netcap_pkt_raze(pkt);
            perrlog("mailbox_put");
        }

        SESSTABLE_UNLOCK();

        // XXX Right here if a packet comes through that matches the reverse
        // rule than this will be create a disconnected session which would be
        // invalid 
        // SOL Use a merge operation that merges two sessions
        // together if one comes in on the reverse

        // Call the UDP hooks
        debug(10,"Calling UDP hook(s)\n");
        global_udp_hook(session,arg);
    } else {
        // Figure out the correct mailbox
        if ( pkt->src.host.s_addr == session->cli.cli.host.s_addr ) {
            mb = &session->cli_mb;
        } else if ( pkt->src.host.s_addr == session->srv.srv.host.s_addr ) {
            mb = &session->srv_mb;
        } else {
            char pkt_str[INET_ADDRSTRLEN];
            char cli_str[INET_ADDRSTRLEN];
            char srv_str[INET_ADDRSTRLEN];
                
            strncpy( pkt_str, unet_inet_ntoa( pkt->src.host.s_addr ), sizeof( pkt_str ));
            strncpy( cli_str, unet_inet_ntoa( session->cli.cli.host.s_addr ), sizeof( cli_str ));
            strncpy( srv_str, unet_inet_ntoa( session->srv.srv.host.s_addr ), sizeof( srv_str ));

            netcap_pkt_raze( pkt );

            return errlog( ERR_CRITICAL, "Cannot determine correct mailbox: pkt %s, cli %s, srv %s\n",
                           pkt_str, cli_str, srv_str );
        }

        /* Add this chunk against the client reputation */
        netcap_shield_rep_add_chunk ( session->cli.cli.host.s_addr, IPPROTO_UDP, pkt->data_len );

        switch ( netcap_shield_rep_check( session->cli.cli.host.s_addr )) {
        case NC_SHIELD_DROP:
        case NC_SHIELD_RESET:
        case NC_SHIELD_LIMITED:
            netcap_pkt_raze ( pkt );
            SESSTABLE_UNLOCK();
            /* XXX Change the debug level */
            strncpy ( address, unet_inet_ntoa ( session->cli.cli.host.s_addr ), sizeof ( address ) );
            /* XXX Change the debug level */
            errlog ( ERR_WARNING, "UDP: Shield rejected packet: %s:%d -> %s:%d\n", 
                     address, session->cli.cli.port, unet_inet_ntoa ( session->cli.srv.host.s_addr ),
                     session->cli.srv.port );
            return 0;
        case NC_SHIELD_YES:
            break;

        default:
            errlog ( ERR_CRITICAL, "netcap_shield_rep_check\n" );
        }
        
        // Put the packet into the mailbox
        if (mailbox_size(mb) > MAX_MB_SIZE ) {
            errlog(ERR_WARNING,"Mailbox Full: Dropping Packet (from %s:%i)\n",
                   inet_ntoa(pkt->src.host),pkt->src.port);
            netcap_pkt_raze(pkt);
        } else if (mailbox_put(mb,(void*)pkt)<0) {
            netcap_pkt_raze(pkt);
            perrlog("mailbox_put");
        }
        
        SESSTABLE_UNLOCK();
    }

    return 0;
}

void netcap_udp_null_hook (netcap_session_t* netcap_sess, void *arg)
{
    errlog(ERR_WARNING,"netcap_udp_null_hook: No UDP hook registered\n");

    /* Remove the session */
    netcap_udp_session_raze(NC_SESSTABLE_LOCK, netcap_sess);
}


static int _netcap_udp_sendto (int sock, void* data, size_t data_len, int flags, netcap_pkt_t* pkt)
{
    struct msghdr      msg;
    struct cmsghdr*    cmsg;
    struct iovec       iov[1];
    struct sockaddr_in dst;
    int                dstlen = sizeof(dst);
    char               control[MAX_CONTROL_MSG];
    u_short            sport;
    int                ret;
    int                dst_intf_len = 0;
    netcap_intf_t      dst_intf;
    char               dst_intf_str[NETCAP_MAX_IF_NAME_LEN];

    /* Antisubscribe all outgoing UDP packets */
    u_int              mark = 0x10;

    dst_intf_str[0] = '\0';

#ifdef DEBUG_ON
    bzero(control,MAX_CONTROL_MSG);
    bzero(&dst,sizeof(struct sockaddr_in));
    bzero(&msg,sizeof(struct msghdr));
#endif

    memcpy(&dst.sin_addr,&pkt->dst.host,sizeof(struct in_addr));
    dst.sin_port = htons(pkt->dst.port);
    dst.sin_family = AF_INET;

    msg.msg_name       = &dst;
    msg.msg_namelen    = dstlen;
    msg.msg_iov        = iov;
    iov[0].iov_base    = data;
    iov[0].iov_len     = data_len;
    msg.msg_iovlen     = 1;
    msg.msg_flags      = 0;
    msg.msg_control    = control;
    msg.msg_controllen = MAX_CONTROL_MSG;
    
    /* ttl ancillary */
    cmsg = CMSG_FIRSTHDR(&msg);
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->ttl));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TTL;
    memcpy(CMSG_DATA(cmsg),&pkt->ttl,sizeof(pkt->ttl));
    
    /* tos ancillary */
	 cmsg =  my__cmsg_nxthdr(msg.msg_control, msg.msg_controllen, cmsg);
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->tos));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TOS;
    memcpy(CMSG_DATA(cmsg),&pkt->tos,sizeof(pkt->tos));

    /* src ip ancillary */
	 cmsg =  my__cmsg_nxthdr(msg.msg_control, msg.msg_controllen, cmsg);
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.host));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SADDR;
    memcpy(CMSG_DATA(cmsg),&pkt->src.host,sizeof(pkt->src.host));

    /* src port ancillary */
	 cmsg =  my__cmsg_nxthdr(msg.msg_control, msg.msg_controllen, cmsg);
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.port));
    cmsg->cmsg_level = SOL_UDP;
    cmsg->cmsg_type  = UDP_SPORT;
    sport = htons(pkt->src.port);
    memcpy(CMSG_DATA(cmsg),&sport,sizeof(pkt->src.port));

    /* destination interface ancillary */
    dst_intf = pkt->dst.intf;
    
    if ( dst_intf != NC_INTF_UNK ) {
        if ( netcap_interface_intf_to_string( dst_intf, dst_intf_str, sizeof( dst_intf_str )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_interface_intf_to_string\n" );
            goto err_out;
        }

        cmsg =  my__cmsg_nxthdr(msg.msg_control, msg.msg_controllen, cmsg);
        if(!cmsg) {
            errlog(ERR_CRITICAL,"No more CMSG Room\n");
            goto err_out;
        }

        dst_intf_len = strnlen ( dst_intf_str, sizeof ( dst_intf_str )) + 1;
        /* XXX This is what should be here cmsg->cmsg_len = CMSG_LEN( dst_intf_len ); */
        cmsg->cmsg_len   = 13;  /* CMSG_LEN(dst_intf_len); (have to fix bug in kernel first) */
        cmsg->cmsg_level = SOL_IP;
        cmsg->cmsg_type  = IP_DEVICE;
        memcpy(CMSG_DATA(cmsg), dst_intf_str, dst_intf_len );

        debug ( 10, "UDP: Sending to interface to '%s'\n", dst_intf_str );
    }

    /* The bit for antisubscribe is reserved, and is always set on outgoing packets */
    if ( pkt->is_marked ) {
        mark |= pkt->nfmark;
    }

    cmsg = my__cmsg_nxthdr( msg.msg_control, msg.msg_controllen, cmsg );
    if ( cmsg == NULL ) { 
        errlog( ERR_CRITICAL, "No more CMSG Room\n" );
        goto err_out;
    }
    
    cmsg->cmsg_len = CMSG_LEN(sizeof(unsigned long));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SENDNFMARK;
    memcpy( CMSG_DATA( cmsg ), &mark, sizeof(pkt->nfmark));
    
    debug( 10, "UDP: Sending packet with mark: %#10x\n", mark );

    /* XXX add options support ( XXX is_marked ) */
    msg.msg_controllen = CMSG_SPACE(sizeof(pkt->src.host)) + CMSG_SPACE(sizeof(pkt->src.port)) + 
        CMSG_SPACE(sizeof(pkt->tos)) + CMSG_SPACE(sizeof(pkt->ttl)) + 
        CMSG_SPACE(sizeof(mark)) +
        (( dst_intf_len )   ? CMSG_SPACE( dst_intf_len ) : 0 );
        


    debug( 10, "sending udp %s:%i -> ", inet_ntoa(pkt->src.host), pkt->src.port );
    debug_nodate( 10, "%s:%i data_len:%i ttl:%i tos:%i\n",inet_ntoa(pkt->dst.host),pkt->dst.port,
                  data_len, pkt->ttl, pkt->tos);
    
    if (( ret = sendmsg( sock, &msg, flags )) < 0 ) {
        if ( errno == EPERM ) {
            debug( 10, "UDP: EPERM sending a UDP packet\n" );
        } else {
            perrlog( "sendmsg" );
        }
    }
    
    goto out;

 err_out:
    errlog ( ERR_CRITICAL, "Unable to send packet\n" );
    ret = -1;
 out:
    return ret;
}

/* this gets rid of a libc bug */
static struct cmsghdr * my__cmsg_nxthdr(void *__ctl, size_t __size, struct cmsghdr *__cmsg)
{
	struct cmsghdr * __ptr;

	__ptr = (struct cmsghdr*)(((unsigned char *) __cmsg) +  CMSG_ALIGN(__cmsg->cmsg_len));
	if ((unsigned long)((char*)(__ptr+1) - (char *) __ctl) > __size)
		return (struct cmsghdr *)0;

	return __ptr;
}


