/**
 * $Id$
 */
#include "netcap_udp.h"

#include <stdlib.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <inttypes.h>
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
#include "netcap_queue.h"
#include "netcap_ip.h"
#include "netcap_pkt.h"


static int _udp_send_sock = -1;

static int _netcap_udp_sendto(int sock, void* buf, size_t len, int flags, netcap_pkt_t* pkt);

/**
 * Packets have the data pointer set to the start of the IP Header, 
 * this function advances the data pointer and ajusts the data length
 * so the data pointer is in the correct place 
 */
static int _process_queue_pkt( netcap_pkt_t* pkt, u_char** full_pkt, int* full_pkt_len );

struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size);


int  netcap_udp_init ()
{
    int one=1;

    /**
     * create the socket used to send/spoof outgoing udp packets
     */
    if(( _udp_send_sock = socket(AF_INET,SOCK_DGRAM,0)) < 0) {
        errlog( ERR_CRITICAL, "Unable to open udp send socket\n" );
        return perrlog ( "socket" );
    }

    /**
     * set all the needed socket options
     */
    if (setsockopt(_udp_send_sock, SOL_SOCKET, SO_BROADCAST,&one, sizeof(one)) < 0) {
        return perrlog ( "setsockopt" );
    }
    if (setsockopt(_udp_send_sock, SOL_IP, IP_TRANSPARENT,&one, sizeof(one)) < 0) {
        return perrlog ( "setsockopt" );
    }
    
    return 0;
}

int  netcap_udp_cleanup()
{
    if ( _udp_send_sock > 0 && ( close( _udp_send_sock ) < 0 )) perrlog("close");
    
    return 0;
}

int  netcap_udp_send (char* data, int data_len, netcap_pkt_t* pkt)
{
    if ( !data || !pkt ) return -1;
        
    return _netcap_udp_sendto( _udp_send_sock, data, data_len, 0, pkt ); 
}

int  netcap_udp_call_hooks (netcap_pkt_t* pkt, void* arg)
{
    netcap_session_t* session;
    u_char* full_pkt = NULL;
    int full_pkt_len;
    mailbox_t* mb = NULL;
    int call_hook = 0;
    netcap_intf_t intf;
    
    /* If the packet was queued (non-zero id), dequeue it */
    if ( pkt == NULL ) {
        return errlogargs();
    }
    
    if ( _process_queue_pkt( pkt, &full_pkt, &full_pkt_len ) < 0 ) {
        netcap_pkt_raze( pkt );
        return errlog( ERR_CRITICAL, "_process_queued_pkt\n" );
    }    

    debug( 10, "UDP: Intercepted packet ::  (%s:%-5i -> %s:%i) mark: %08x\n",
           unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port, unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port, pkt->nfmark);
    
    SESSTABLE_WRLOCK();

    // First check to see if the session already exists.
    // use the pre-NAT source and post-NAT dest (original src and reply src)
    session = netcap_nc_sesstable_get_tuple (!NC_SESSTABLE_LOCK, IPPROTO_UDP,
                                             pkt->nat_info.original.src_address,
                                             pkt->nat_info.original.dst_address,
                                             ntohs( pkt->nat_info.original.src_protocol_id ),
                                             ntohs( pkt->nat_info.original.dst_protocol_id ));
    
    // If it doesn't, intialize the session.
    if ( !session ) {
        // Create a UDP session
        session = netcap_udp_session_create( pkt );
        
        if ( !session ) {
            // Drop the packet
            netcap_pkt_action_raze( pkt, NF_DROP );
            SESSTABLE_UNLOCK();
            return perrlog("netcap_udp_session_create");
        }

        // Add the session to the table
        // use the pre-NAT source and post-NAT dest (original src and reply src)
        if ( netcap_nc_sesstable_add_tuple ( !NC_SESSTABLE_LOCK, session, IPPROTO_UDP,
                                             pkt->nat_info.original.src_address,
                                             pkt->nat_info.original.dst_address,
                                             ntohs( pkt->nat_info.original.src_protocol_id ),
                                             ntohs( pkt->nat_info.original.dst_protocol_id )) < 0 ) {
            netcap_udp_session_raze(!NC_SESSTABLE_LOCK, session);
            netcap_pkt_action_raze( pkt, NF_DROP );
            SESSTABLE_UNLOCK();
            return perrlog("netcap_nc_sesstable_add_tuple");
        }

        // Add the session to itself
        if ( netcap_nc_sesstable_add ( !NC_SESSTABLE_LOCK, session )) {
            netcap_udp_session_raze(!NC_SESSTABLE_LOCK, session);
            netcap_pkt_action_raze( pkt, NF_DROP );
            SESSTABLE_UNLOCK();
            return perrlog("netcap_sesstable_add");
        }

        call_hook = 1;
    }

    // Now either the session was already found or a new one has been created
    // Put the pkt in the correct mailbox
    
    // Figure out the correct mailbox
    if ( pkt->src.host.s_addr == session->cli.cli.host.s_addr ) {
        mb      = &session->cli_mb;
        intf    = session->cli.intf;
    } else if ( pkt->src.host.s_addr == session->srv.srv.host.s_addr ) {
        mb      = &session->srv_mb;
        intf    = session->srv.intf;
    } else {
        SESSTABLE_UNLOCK();
        errlog( ERR_CRITICAL, "Cannot determine correct mailbox: pkt %s, cli %s, srv %s\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), 
                unet_next_inet_ntoa( session->cli.cli.host.s_addr ), 
                unet_next_inet_ntoa( session->srv.srv.host.s_addr ));
        netcap_pkt_raze( pkt );
        return -1;
    }

    /* Verify the packet is from the same interface */
    if ( intf != pkt->src_intf ) {
        errlog( ERR_WARNING, "UDP: Packet from the incorrect interface expected %d actual %d. Dropping...\n", intf, pkt->src_intf );
        netcap_pkt_action_raze( pkt, NF_DROP );
        SESSTABLE_UNLOCK();
        return 0;
    }

    // Put the packet into the mailbox
    if (mailbox_size(mb) > MAX_MB_SIZE ) {
        errlog( ERR_WARNING,"Mailbox Full: Dropping Packet (%s:%i -> %s:%i)\n",
                unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port);
        netcap_pkt_action_raze( pkt, NF_DROP );
        full_pkt = NULL;
    } else {
        if ( mailbox_put( mb, (void*)pkt ) < 0 ) {
            netcap_pkt_action_raze( pkt, NF_DROP );
            perrlog("mailbox_put");
            full_pkt = NULL;
        }
    }
                
    SESSTABLE_UNLOCK();

    if ( call_hook ) {
        debug(10,"Calling UDP hook(s)\n");
        global_udp_hook(session,arg);
    }

    return 0;
}

void netcap_udp_cleanup_hook (netcap_session_t* netcap_sess, void *arg)
{
    /* Remove the session */
    netcap_session_raze( netcap_sess );
}

void netcap_udp_null_hook (netcap_session_t* netcap_sess, void *arg)
{
    errlog( ERR_WARNING, "netcap_udp_null_hook: No UDP hook registered\n" );
    
    netcap_udp_cleanup_hook( netcap_sess, arg );    
}

static int _netcap_udp_sendto (int sock, void* data, size_t data_len, int flags, netcap_pkt_t* pkt)
{
    struct msghdr      msg = {0};
    struct cmsghdr*    cmsg;
    struct iovec       iov[1];
    struct sockaddr_in dst;
    char               control[MAX_CONTROL_MSG] = {0};
    u_short            sport;
    int                ret;
    int                tos_len =0;
    int                ttl_len =0;
    u_int              tos_val =0;
    u_int              ttl_val =0;

    /**
     * mark packet with:  MARK_BYPASS + whatever packet marks are specified 
     */
    u_int              nfmark = ( MARK_BYPASS | ( pkt->is_marked ? pkt->nfmark : 0 )); 
    
    /* Setup the destination */
    memset(&dst, 0, sizeof(dst));
    memcpy( &dst.sin_addr, &pkt->dst.host , sizeof(struct in_addr) );
    dst.sin_port = htons( pkt->dst.port );
    dst.sin_family = AF_INET;

    msg.msg_name       = &dst;
    msg.msg_namelen    = sizeof( dst );
    msg.msg_iov        = iov;
    iov[0].iov_base    = data;
    iov[0].iov_len     = data_len;
    msg.msg_iovlen     = 1;
    msg.msg_flags      = 0;
    msg.msg_control    = control;
    msg.msg_controllen = MAX_CONTROL_MSG;
    
    /* tos ancillary */
    cmsg = CMSG_FIRSTHDR( &msg );
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }

    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TOS;
    if ( IS_NEW_KERNEL() >= 316 ) {
        tos_len   = CMSG_LEN(sizeof(int));
        tos_val   = pkt->tos;
        cmsg->cmsg_len   = tos_len;
        memcpy( CMSG_DATA(cmsg), &tos_val, tos_len );
    }
    else {
        cmsg->cmsg_len   = CMSG_LEN(sizeof(pkt->tos));
        memcpy( CMSG_DATA(cmsg), &pkt->tos, sizeof(pkt->tos) );
    }

    /* ttl ancillary */
    if ( IS_NEW_KERNEL() >= 316 ) {
        ttl_len   = CMSG_LEN(sizeof(int));
        cmsg = my__cmsg_nxthdr(&msg, cmsg, sizeof(int));
    }
    else {
        ttl_len   = CMSG_LEN(sizeof(pkt->ttl));
        cmsg = my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->ttl));
    }
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len   = ttl_len;
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TTL;

    if ( IS_NEW_KERNEL() >= 316 ) {
        ttl_val = pkt->ttl;
        memcpy( CMSG_DATA(cmsg), &ttl_val, sizeof(int) );
    }
    else
        memcpy( CMSG_DATA(cmsg), &pkt->ttl, sizeof(pkt->ttl) );


    /* src port ancillary */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->src.port));
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.port));
    cmsg->cmsg_level = SOL_UDP;
    // this options is available through custom extensions untangle kernel patch
    cmsg->cmsg_type  = UDP_SPORT;
    sport = htons(pkt->src.port);
    memcpy( CMSG_DATA(cmsg),&sport,sizeof(pkt->src.port) );

    /* nfmark */                                  
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, sizeof(nfmark));
    if ( cmsg == NULL ) {
        errlog( ERR_CRITICAL, "No more CMSG Room\n" );
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(nfmark));
    cmsg->cmsg_level = SOL_IP;
    // this options is available through custom extensions untangle kernel patch
    cmsg->cmsg_type  = IP_SENDNFMARK_VALUE();
    memcpy( CMSG_DATA( cmsg ), &nfmark, sizeof(nfmark) );

    /* src ip */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->src.host));
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.host));
    cmsg->cmsg_level = SOL_IP;
    // this options is available through custom extensions untangle kernel patch
    cmsg->cmsg_type  = IP_SADDR_VALUE();
    memcpy( CMSG_DATA(cmsg),&pkt->src.host,sizeof(pkt->src.host) );

    /* sanity check */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, 0);
    if ( ((char*)cmsg) > control + MAX_CONTROL_MSG)
        errlog(ERR_CRITICAL,"CMSG overrun");

    if ( IS_NEW_KERNEL() >= 316 ) 
        msg.msg_controllen =
            CMSG_SPACE(sizeof(int)) +
            CMSG_SPACE(sizeof(int)) +
            CMSG_SPACE(sizeof(pkt->src.port)) +
            CMSG_SPACE(sizeof(nfmark)) +
            CMSG_SPACE(sizeof(pkt->src.host));
    else
        msg.msg_controllen =
            CMSG_SPACE(sizeof(pkt->tos)) +
            CMSG_SPACE(sizeof(pkt->ttl)) +
            CMSG_SPACE(sizeof(pkt->src.port)) +
            CMSG_SPACE(sizeof(nfmark)) +
            CMSG_SPACE(sizeof(pkt->src.host));

    /* Send Packet */
    debug( 10, "sending UDP %s:%i -> %s:%i data_len:%i ttl:%i tos:%i nfmark:0x%08x\n",
           unet_next_inet_ntoa(pkt->src.host.s_addr), pkt->src.port,
           unet_next_inet_ntoa(pkt->dst.host.s_addr), pkt->dst.port,
           (int) data_len, pkt->ttl, pkt->tos, nfmark);

    if ( ( ret = sendmsg( sock, &msg, flags ) ) < 0 ) {
        /**
         * An error has occured.
         * Use the data length to fake that the packet was written.
         * This way the packet is just dropped.
         * This is to prevent spinning while retrying to write the packet (Bug #11624.)
         * Something really bad has happened so just give up and drop the packet.
         */
        ret = data_len;

        switch ( errno ) {
        case EPERM:
            /* Fallthrough */
        case ENETUNREACH:
            /* Fallthrough */
        case EHOSTUNREACH:
            /* Fallthrough */
            errlog( ERR_WARNING, "UDP: unable to send packet (errno: %i)\n", errno);
            break;

        case EINVAL: 
            errlog( ERR_WARNING, "sendmsg: %s | (%s:%i -> %s:%i) data_len:%i ttl:%i tos:%i nfmark:%#10x\n", 
                    errstr, 
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                    unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port,
                    (int) data_len, pkt->ttl, pkt->tos, nfmark );
            break;

        default:
            errlog( ERR_CRITICAL, "sendmsg: %s | (%s:%i -> %s:%i) data_len:%i ttl:%i tos:%i nfmark:%#10x\n", 
                    errstr, 
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                    unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port,
                    (int) data_len, pkt->ttl, pkt->tos, nfmark );
        }
    }
    
    goto out;

 err_out:
    errlog ( ERR_CRITICAL, "Unable to send packet\n" );
    ret = -1;
 out:
    return ret;
}

static int _process_queue_pkt( netcap_pkt_t* pkt, u_char** full_pkt, int* full_pkt_len )
{
    int offset;
    struct iphdr* iph = (struct iphdr*)pkt->data;

    /* Update the full packet */
    *full_pkt     = NULL;
    *full_pkt_len = 0;
    
    if (( iph = (struct iphdr*)pkt->data ) == NULL ) {
        return errlog( ERR_CRITICAL, "Queued UDP packet without IP header\n" );
    }
    
    /* Advance the data pointer past the header */
    /* Update the full packet */
    *full_pkt     = pkt->data;
    *full_pkt_len = pkt->data_len;

    offset = iph->ihl;

    if ( offset > 15 ) {
        return errlog( ERR_CRITICAL, "UDP: Invalid data offset - %d\n", offset );
    }
    
    /* Words to bytes */
    offset = sizeof( struct udphdr ) + ( offset << 2 );
    pkt->data_len = pkt->data_len - offset;
    
    if (( pkt->data_len < 0 ) || ( pkt->data_len > QUEUE_MAX_MESG_SIZE )) {
        return errlog( ERR_CRITICAL, "UDP: Invalid data size - %d\n", pkt->data_len );
    }

    /* Don't use memmove, since the ICMP message may need the whole packet including the header */
    if ( pkt->buffer == NULL ) {
        return errlog( ERR_CRITICAL, "pkt->buffer is null\n" );
    }

    pkt->data = &pkt->data[offset];

    return 0;
}

/**
 * this gets rid of the mess in libc (in bits/socket.h)
 */
struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size)
{
	struct cmsghdr * ptr;

	ptr = (struct cmsghdr*)(((unsigned char *) cmsg) +  CMSG_ALIGN(cmsg->cmsg_len));

    if ((((char*)ptr) + CMSG_LEN(size)) > ((char*)msg->msg_control + msg->msg_controllen)) {
		return (struct cmsghdr *)0;
    }

	return ptr;
}
