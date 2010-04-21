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

#include "netcap_udp.h"

#include <stdlib.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
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
#include "netcap_icmp.h"
#include "netcap_icmp_msg.h"
#include "netcap_ip.h"
#include "netcap_pkt.h"


/* Cleanup at most 2 UDP packets per iteration */
#define _ICMP_CACHE_CLEANUP_MAX 2

static struct {
    int send_sock;
} _udp = {
    .send_sock   = -1,
};

static int _netcap_udp_sendto(int sock, void* buf, size_t len, int flags, netcap_pkt_t* pkt);

/**
 * Packets have the data pointer set to the start of the IP Header, 
 * this function advances the data pointer and ajusts the data length
 * so the data pointer is in the correct place 
 */
static int _process_queue_pkt( netcap_pkt_t* pkt, u_char** full_pkt, int* full_pkt_len );

/**
 * Parse an UDP/IP header and set the received port.
 */
static int _parse_udp_ip_header( netcap_pkt_t* pkt, char* header, int header_len, int buf_len );

/**
 * Cache a packet inside of a ICMP mailbox, this is used to respond to ICMP error messages
 */
static int _cache_packet( u_char* full_pkt, int full_pkt_len, mailbox_t* icmp_mb );

/**
 * This will release the first packet with a zero length header in order to
 * establish the conntrack session.
 */
static int _insert_first_pkt( netcap_session_t* session, netcap_pkt_t* pkt );


struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size);

int  netcap_udp_init ()
{
    int one=1;

    /**
     * create the socket used to send/spoof outgoing udp packets
     */
    if(( _udp.send_sock = socket(AF_INET,SOCK_DGRAM,0)) < 0) {
        errlog( ERR_CRITICAL, "Unable to open udp send socket\n" );
        return perrlog ( "socket" );
    }

    /**
     * set all the needed socket options
     */
    if (setsockopt(_udp.send_sock, SOL_SOCKET, SO_BROADCAST,&one, sizeof(one)) < 0) {
        return perrlog ( "setsockopt" );
    }

    return 0;
}

int  netcap_udp_cleanup()
{
    if ( _udp.send_sock > 0 && ( close( _udp.send_sock ) < 0 )) perrlog("close");
    
    return 0;
}

int  netcap_udp_send (char* data, int data_len, netcap_pkt_t* pkt)
{
    if ( !data || !pkt ) return -1;
    if ( !data_len ) return 0;
        
    return _netcap_udp_sendto( _udp.send_sock, data, data_len, 0, pkt ); 
}

int  netcap_udp_recvfrom (int sock, void* buf, size_t len, int flags, netcap_pkt_t* pkt)
{
    struct msghdr      msg;
    struct iovec       iov[1];
    struct cmsghdr*    cmsg;
    char               control[MAX_CONTROL_MSG];
    struct sockaddr_in  cli;
    int numread;

    char  ip_header_len = 0;
 
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

    pkt->data     = buf;
    pkt->data_len = numread;

    if ( msg.msg_flags & MSG_CTRUNC ) {
        errlog( ERR_WARNING, "UDP: Message truncated\n" );
    }

    if ( msg.msg_flags & MSG_TRUNC ) {
        errlog( ERR_WARNING, "UDP: Data truncated\n" );
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
            case IP_RECVNFMARK:  pkt->nfmark = *(u_int*)CMSG_DATA(cmsg); break;
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
            case UDP_RECVDHDR:
                ip_header_len = cmsg->cmsg_len - CMSG_LEN(0);
                debug( 10, "Received UDP header of size: %d\n", ip_header_len );
                if ( _parse_udp_ip_header( pkt, (char*)(CMSG_DATA(cmsg)), ip_header_len, len ) < 0 ) {
                    return errlog( ERR_CRITICAL, "_parse_udp_ip_haeder\n" );
                }                
                break;
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

    if ( ip_header_len == 0 ) {
        return errlog( ERR_CRITICAL, "Header was not updated\n" );
    }
    
    /* Set the source interface for the packet */
    /* XXXX Should never catch packets with a mark of 0 */
    if ( pkt->nfmark == 0 ) {
        debug( 4, "Packet with mark: %#10x\n", pkt->nfmark );
    }
    if ( netcap_interface_mark_to_intf( pkt->nfmark,&pkt->src_intf ) < 0 ) {
        errlog( ERR_WARNING, "Unable to determine the source interface from mark[%s:%d -> %s:%d]\n",
                unet_next_inet_ntoa( cli.sin_addr.s_addr ), cli.sin_port,
                unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port );
    }
    
    /* Clear the output interface */
    pkt->dst_intf = NC_INTF_UNK;

    memcpy(&pkt->src.host,&cli.sin_addr,sizeof(struct in_addr));
    pkt->src.port = ntohs(cli.sin_port);
    pkt->proto = IPPROTO_UDP;
    pkt->packet_id = 0;
    
    return pkt->data_len;
}

int  netcap_udp_call_hooks (netcap_pkt_t* pkt, void* arg)
{
    netcap_session_t* session;
    u_char* full_pkt = NULL;
    int full_pkt_len;
    mailbox_t* mb = NULL;
    mailbox_t* icmp_mb = NULL;

    /* If the packet was queued (non-zero id), dequeue it */
    if ( pkt == NULL ) {
        return errlogargs();
    }
    
    if ( _process_queue_pkt( pkt, &full_pkt, &full_pkt_len ) < 0 ) {
        netcap_pkt_raze( pkt );
        return errlog( ERR_CRITICAL, "_process_queued_pkt\n" );
    }    

    SESSTABLE_WRLOCK();

    // First check to see if the session already exists.
    session = netcap_nc_sesstable_get_tuple (!NC_SESSTABLE_LOCK, IPPROTO_UDP,
                                             pkt->src.host.s_addr, pkt->dst.host.s_addr,
                                             pkt->src.port,pkt->dst.port, 0);
    
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
        if ( netcap_nc_sesstable_add_tuple ( !NC_SESSTABLE_LOCK, session, IPPROTO_UDP,
                                             pkt->src.host.s_addr, pkt->dst.host.s_addr,
                                             pkt->src.port,pkt->dst.port,0 ) ) {
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

        // Dump the packet into the mailbox

        // Put the packet into the mailbox
        if (mailbox_size(&session->cli_mb) > MAX_MB_SIZE ) {
            errlog( ERR_WARNING,"Mailbox Full: Dropping Packet (%s:%i -> %s:%i)\n",
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                    unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port);
            netcap_pkt_action_raze( pkt, NF_DROP );
            full_pkt = NULL;
        } else if (mailbox_put(&session->cli_mb,(void*)pkt)<0) {
            netcap_pkt_action_raze( pkt, NF_DROP );
            perrlog("mailbox_put");
            full_pkt = NULL;
        }
        
        /* Caching order is not significant since the other thread/session doesn't exist yet */
        
        // Put a copy of the full packet into the mailbox
        if ( full_pkt != NULL && ( _cache_packet( full_pkt, full_pkt_len, &session->icmp_cli_mb ) < 0 )) {
            errlog( ERR_CRITICAL, "_cache_packet\n" );
        }

        if ( _insert_first_pkt( session, pkt ) < 0 ) {
            netcap_udp_session_raze( !NC_SESSTABLE_LOCK, session );
            //netcap_pkt_action_raze( pkt, NF_DROP );
            SESSTABLE_UNLOCK();
            return errlog( ERR_CRITICAL, "_insert_first_pkt\n" );
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
        /* Session has already started, so drop the packet (only need to accept the first one) */
        int packet_id;
        if (( packet_id = pkt->packet_id ) == 0 ) {
            debug( 10, "UDP packet was already dropped or had zero id\n" );
        } else {
            pkt->packet_id = 0;
            if ( netcap_set_verdict( packet_id, NF_DROP, NULL, 0 ) < 0 ) {
                errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
                return -1;
            }
        }
        
        netcap_intf_t intf;


        // Figure out the correct mailbox
        if ( pkt->src.host.s_addr == session->cli.cli.host.s_addr ) {
            mb      = &session->cli_mb;
            icmp_mb = &session->icmp_cli_mb;
            intf    = session->cli.intf;
        } else if ( pkt->src.host.s_addr == session->srv.srv.host.s_addr ) {
            mb      = &session->srv_mb;
            icmp_mb = &session->icmp_srv_mb;
            intf    = session->srv.intf;
        } else {
            netcap_pkt_raze( pkt );
            SESSTABLE_UNLOCK();
            return errlog( ERR_CRITICAL, "Cannot determine correct mailbox: pkt %s, cli %s, srv %s\n",
                           unet_next_inet_ntoa( pkt->src.host.s_addr ), 
                           unet_next_inet_ntoa( session->cli.cli.host.s_addr ), 
                           unet_next_inet_ntoa( session->srv.srv.host.s_addr ));
        }

        /* Verify the packet is from the same interface */
        if ( intf != pkt->src_intf ) {
            debug( 5, "UDP: Packet from the incorrect interface expected %d actual %d\n", 
                   intf, pkt->src_intf );
            
            netcap_pkt_raze( pkt );
            SESSTABLE_UNLOCK();
            return 0;
        }
                
        // Put the packet into the mailbox
        if (mailbox_size(mb) > MAX_MB_SIZE ) {
            errlog(ERR_WARNING,"Mailbox Full: Dropping Packet (from %s:%i)\n",
                   inet_ntoa(pkt->src.host),pkt->src.port);
            netcap_pkt_raze(pkt);
            full_pkt = NULL;
        } else {
            /* The caching of the packet must occur before the packet is put into the mailbox,
             * because if it isn't the data could be removed from the mailbox and then the
             * memory would be invalid
             * (to test, swap the order, and then add a sleep one in between the two
             * once it is in there, run echospam through the box and then send a few
             * UDP packets through, eventually a crash will occur.)
             */
            if (( full_pkt != NULL ) && _cache_packet( full_pkt, full_pkt_len, icmp_mb ) < 0 ) {
                errlog( ERR_CRITICAL, "_cache_packet\n" );
            }

            if ( mailbox_put( mb, (void*)pkt ) < 0 ) {
                netcap_pkt_raze(pkt);
                perrlog("mailbox_put");
                full_pkt = NULL;
            }
        }
                
        SESSTABLE_UNLOCK();
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
    struct msghdr      msg;
    struct cmsghdr*    cmsg;
    struct iovec       iov[1];
    struct sockaddr_in dst;
    char               control[MAX_CONTROL_MSG];
    u_short            sport;
    int                ret;
    u_int              nfmark = ( MARK_ANTISUB | MARK_NOTRACK | ( pkt->is_marked ? pkt->nfmark : 0 )); 
    /* mark is  antisub + notrack + whatever packet marks are specified */

    /* if the caller uses the force flag, then override the default bits of the mark */
    if ( pkt->is_marked == IS_MARKED_FORCE_FLAG ) nfmark = pkt->nfmark;

    if ( pkt->dst_intf != NC_INTF_UNK ) errlog(ERR_CRITICAL,"NC_INTF_UNK Unsupported (IP_DEVICE)\n");

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
    cmsg->cmsg_len   = CMSG_LEN(sizeof(pkt->tos));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TOS;
    memcpy( CMSG_DATA(cmsg), &pkt->tos, sizeof(pkt->tos) );

    /* ttl ancillary */
    cmsg = my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->ttl));
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len   = CMSG_LEN(sizeof(pkt->ttl));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_TTL;
    memcpy( CMSG_DATA(cmsg), &pkt->ttl, sizeof(pkt->ttl) );

    /* src port ancillary */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->src.port));
    if( !cmsg ) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.port));
    cmsg->cmsg_level = SOL_UDP;
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
    cmsg->cmsg_type  = IP_SENDNFMARK;
    memcpy( CMSG_DATA( cmsg ), &nfmark, sizeof(nfmark) );

    /* src ip */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, sizeof(pkt->src.host));
    if(!cmsg) {
        errlog(ERR_CRITICAL,"No more CMSG Room\n");
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(pkt->src.host));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SADDR;
    memcpy( CMSG_DATA(cmsg),&pkt->src.host,sizeof(pkt->src.host) );

    /* sanity check */
    cmsg =  my__cmsg_nxthdr(&msg, cmsg, 0);
    if ( ((char*)cmsg) > control + MAX_CONTROL_MSG)
        errlog(ERR_CRITICAL,"CMSG overrun");

    msg.msg_controllen =
        CMSG_SPACE(sizeof(pkt->tos)) +
        CMSG_SPACE(sizeof(pkt->ttl)) +
        CMSG_SPACE(sizeof(pkt->src.port)) +
        CMSG_SPACE(sizeof(nfmark)) +
        CMSG_SPACE(sizeof(pkt->src.host));

    /* Send Packet */
    debug( 10, "sending UDP %s:%i -> %s:%i data_len:%i ttl:%i tos:%i nfmark:%#10x\n",
           unet_next_inet_ntoa(pkt->src.host.s_addr), pkt->src.port,
           unet_next_inet_ntoa(pkt->dst.host.s_addr),pkt->dst.port,
           data_len, pkt->ttl, pkt->tos, nfmark);

    
    if (( ret = sendmsg( sock, &msg, flags )) < 0 ) {
        switch ( errno ) {
        case EPERM:
            /* Fallthrough */
        case ENETUNREACH:
            /* Fallthrough */
        case EHOSTUNREACH:
            /* Fallthrough */
            errlog( ERR_WARNING, "UDP: unable to send packet(%s), innocuous response code\n",
                    strerror(errno));
            /* Use the data length to fake that the packet was written. This way the packet is consumed */
            ret = data_len;
            break;

        case EINVAL: 
            errlog( ERR_WARNING, "sendmsg: %s | (%s:%i -> %s:%i) data_len:%i ttl:%i tos:%i nfmark:%#10x\n", 
                    errstr, 
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                    unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port,
                    data_len, pkt->ttl, pkt->tos, nfmark );
            
            /* Use the data length to fake that the packet was written. This way the packet is consumed */
            /* XXX This should not be here, the packets should be dropped before reaching the MVVM, 
               see bug(827) for more information */
            ret = data_len;
            break;

        default:
            errlog( ERR_CRITICAL, "sendmsg: %s | (%s:%i -> %s:%i) data_len:%i ttl:%i tos:%i nfmark:%#10x\n", 
                    errstr, 
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), pkt->src.port,
                    unet_next_inet_ntoa( pkt->dst.host.s_addr ), pkt->dst.port,
                    data_len, pkt->ttl, pkt->tos, nfmark );
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

static int _cache_packet( u_char* full_pkt, int full_pkt_len, mailbox_t* icmp_mb )
{
    netcap_icmp_msg_t* msg;
    netcap_icmp_msg_t* old_msg;
    int c;

    if ( full_pkt == NULL )
        return 0;
    
    if (( msg = netcap_icmp_msg_create( full_pkt, full_pkt_len )) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_icmp_msg_create\n" );
    }

    /* Try to fetch some packages out */
    for ( c = 0 ; c < _ICMP_CACHE_CLEANUP_MAX ; c++ ) {
        if (( old_msg = mailbox_try_get( icmp_mb )) != NULL ) {
            debug( 10, "UDP: Removing cached ICMP message\n" );
            if ( netcap_icmp_msg_raze( old_msg ) < 0 ) errlog( ERR_CRITICAL, "netcap_icmp_msg_raze\n" );
        }
    }
    
    if ( mailbox_put( icmp_mb, (void*)msg ) < 0 ) {
        netcap_icmp_msg_raze( msg );
        return perrlog( "mailbox_put\n" );
    }
    
    return 0;
}

/**
 * Parse an UDP/IP header and return the received port.
 */
static int _parse_udp_ip_header( netcap_pkt_t* pkt, char* header, int header_len, int buf_len )
{
    struct iphdr* ip_header  = (struct iphdr*)header;
    struct udphdr* udphdr;
    
    if ( buf_len < ( pkt->data_len + header_len )) {
        return errlog( ERR_CRITICAL, "Data buffer is too small: %d < %d\n", 
                       buf_len, pkt->data_len + header_len );
    }
    
    if (( udphdr = netcap_ip_get_udp_header( ip_header, header_len )) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_ip_get_udp_header\n" );
    }

    /* Get the destination of the packet */
    pkt->dst.port = ntohs( udphdr->dest );

    /* Save the buffer so you know where the packet started when the address is updated in
     * process packet */
    pkt->buffer = pkt->data;
    
    /* Move the data and then copy in the header */
    memmove( &pkt->data[header_len], pkt->data, pkt->data_len );
    
    /* Copy the header in */
    memcpy( pkt->data, header, header_len );
    
    /* Update the packet length */
    pkt->data_len += header_len;
    
    return 0;
}

static int _insert_first_pkt( netcap_session_t* session, netcap_pkt_t* pkt )
{
    /* Copy the packet id into the first_pkt structure */
    if (( session->first_pkt_id = pkt->packet_id ) == 0 ) {
        return errlog( ERR_CRITICAL, "UDP SESSION: First packet already dropped.\n" );
    }

    pkt->packet_id = 0;

    return 0;
}

/* this gets rid of the mess in libc (in bits/socket.h) */
struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size)
{
	struct cmsghdr * ptr;

	ptr = (struct cmsghdr*)(((unsigned char *) cmsg) +  CMSG_ALIGN(cmsg->cmsg_len));

    if ((((char*)ptr) + CMSG_LEN(size)) > ((char*)msg->msg_control + msg->msg_controllen)) {
		return (struct cmsghdr *)0;
    }

	return ptr;
}
