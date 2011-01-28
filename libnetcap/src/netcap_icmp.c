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
#include "netcap_icmp_msg.h"

/* Cleanup at most 2 UDP packets per iteration */
#define _ICMP_CACHE_CLEANUP_MAX 2

static struct {
    int send_sock;
} _icmp = {
    .send_sock = -1
};

typedef enum
{
    /* an error occured while finding the session */
    _FIND_ERROR = -1,

    /* Session exists, and packet was placed into the correct mailbox. */
    _FIND_EXIST = 0,

    /* new session was created and the packet was placed into its client mailbox */
    _FIND_NEW   = 1,

    /* packet cannot be associated with a session, and should be dealt with individually */
    _FIND_NONE  = 2,

    /* packet cannot be associated with a session and should be dropped */
    _FIND_DROP  = 3,

    /* packet cannot be associated with a session and should be accepted */
    _FIND_ACCEPT= 4
} _find_t;

static int _restore_cached_msg( mailbox_t* mb, netcap_icmp_msg_t* msg );

/**
 * Retrieve a UDP or TCP session using the information from an error message as the key
 * This also updates mb with the correct value.
 */
static netcap_session_t* _icmp_get_error_session( netcap_pkt_t* pkt, mailbox_t** mb )
{
    struct ip*     ip_header;
    struct tcphdr* tcp_header;
    struct udphdr* udp_header;
    struct icmp*   icmp_header;
    netcap_session_t* netcap_sess;
    int ping_id = 0;
    int protocol = -1;

    in_addr_t src_host;
    in_addr_t dst_host;
    u_short src_port;
    u_short dst_port;
    
    /* Default to NULL */
    *mb = NULL;

    /* XXX MOVE ALL OF THIS LOGIC TO A COMMON PLACE */
    if ( pkt->data_len < ICMP_ADVLENMIN ) {
        return errlog_null ( ERR_WARNING, "Invalid ICMP error packet, %d < %d\n", 
                             pkt->data_len, ICMP_ADVLENMIN );
    }

    ip_header  = &((struct icmp*)pkt->data)->icmp_ip;
    
    if ( ip_header->ip_hl > 15  || ip_header->ip_hl < ( sizeof(struct ip) >> 2)) {
        errlog( ERR_WARNING,"Illogical IP header length (%d), Assuming 5.\n", ip_header->ip_hl );
        tcp_header = (struct tcphdr*) ( (char*)ip_header + sizeof(struct iphdr));
    }
    else if ( pkt->data_len >= ( ICMP_ADVLENMIN - sizeof(struct ip) + ( ip_header->ip_hl << 2 ))) {
        tcp_header = (struct tcphdr*) ( (char*)ip_header + ( 4 * ip_header->ip_hl ));
    } else {
        return errlog_null( ERR_WARNING, "Invalid ICMP error packet, %d < %d\n",
                            pkt->data_len, 
                            ( ICMP_ADVLENMIN - sizeof( struct ip ) + ( ip_header->ip_hl << 2 )));
    }
    
    udp_header  = (struct udphdr*)tcp_header;
    icmp_header = (struct icmp*)tcp_header;
    
    /* Host and dest are swapped since this is the packet that was sent out */
    src_host = ip_header->ip_dst.s_addr;
    dst_host = ip_header->ip_src.s_addr;
    
    switch ( ip_header->ip_p ) {
    case IPPROTO_TCP:
        protocol = IPPROTO_TCP;
        src_port = ntohs ( tcp_header->dest );
        dst_port = ntohs ( tcp_header->source );
        ping_id  = 0;
        break;

    case IPPROTO_UDP:
        protocol = IPPROTO_UDP;
        src_port = ntohs ( udp_header->dest );
        dst_port = ntohs ( udp_header->source );
        ping_id  = 0;
        break;

    case IPPROTO_ICMP:
        protocol = IPPROTO_UDP;
        
        if ( netcap_icmp_verify_type_and_code( icmp_header->icmp_type, icmp_header->icmp_code ) < 0 ) {
            return errlog_null( ERR_WARNING, "netcap_icmp_verify_type_and_code[%d,%d]\n",
                                icmp_header->icmp_type, icmp_header->icmp_code );
        }
        
        if ( icmp_header->icmp_type == ICMP_ECHO || icmp_header->icmp_type == ICMP_ECHOREPLY ) {
            src_port = 0;
            dst_port = 0;
            ping_id  = ntohs( icmp_header->icmp_id );
        } else {
            debug( 5, "ICMP: Unable to lookup ICMP Error session for icmp type %d, code %d\n", 
                   icmp_header->icmp_type, icmp_header->icmp_code );
            return NULL;
        }
        break;

    default:
        return errlog_null( ERR_WARNING, "ICMP: Unable to lookup session for protocol %d\n", ip_header->ip_p );
                            
    }
    
    debug( 10, "ICMP: Looking up packet %s:%d -> %s:%d (%d)\n", 
           unet_next_inet_ntoa( src_host ), src_port, unet_next_inet_ntoa( dst_host ), dst_port, ping_id );
    
    netcap_sess = netcap_nc_sesstable_get_tuple( !NC_SESSTABLE_LOCK, protocol,
                                                 src_host, dst_host, src_port, dst_port, ping_id );

    if ( netcap_sess != NULL ) {
        netcap_intf_t intf = -1;
        
        // Figure out the correct mailbox (TCP only has a server mailbox, no client mailbox)
        if ( src_host == netcap_sess->srv.srv.host.s_addr ) {
            /* Error packet from the server wrg packet from the client */
            debug( 10, "ICMP: Server mailbox\n" );
            *mb  =  &netcap_sess->srv_mb;
            intf = netcap_sess->srv.intf;
        } else if ( src_host == netcap_sess->cli.cli.host.s_addr ) {
            /* Error packet from the client wrg to a packet from the server */
            debug( 10, "ICMP: Client mailbox\n" );
            if ( ip_header->ip_p == IPPROTO_TCP ) {
                debug( 4, "ICMP: Received ICMP message from client for TCP session\n" );
                netcap_sess = NULL;
                *mb = NULL;
            } else {
                *mb  = &netcap_sess->cli_mb;
                intf = netcap_sess->cli.intf;
            }
        } else {
            *mb = NULL;
            return errlog_null( ERR_CRITICAL, "Cannot determine correct mailbox: msg %s, cli %s, srv %s\n",
                                unet_next_inet_ntoa( src_host ), 
                                unet_next_inet_ntoa( netcap_sess->cli.cli.host.s_addr ),
                                unet_next_inet_ntoa( netcap_sess->srv.srv.host.s_addr ));
        }

        if (( *mb != NULL ) && ( pkt->src_intf != intf )) {
            *mb = NULL;
            debug( 5, "ICMP: Packet from the incorrect interface expected %d actual %d\n", 
                    intf, pkt->src_intf );
            return NULL;
        }
    } else {
        debug( 5, "ICMP: No session for packet with protocol %d from %s\n", protocol,
               unet_next_inet_ntoa( src_host ));
    }
    
    return netcap_sess;
}

/**
 * Put a packet into the mailbox for a session */
static int _icmp_put_mailbox( mailbox_t* mb, netcap_pkt_t* pkt )
{
    if ( netcap_set_verdict( pkt->packet_id, NF_DROP, NULL, 0 ) < 0 ) {
      pkt->packet_id = 0;
      return errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
    }
    pkt->packet_id = 0;

    if ( mailbox_size( mb ) > MAX_MB_SIZE ) {
        return errlog( ERR_WARNING, "ICMP: Mailbox Full - Dropping Packet (from %s)\n", 
                       inet_ntoa( pkt->src.host ));
    } else if ( mailbox_put( mb, (void*)pkt ) < 0 ) {
        return perrlog("mailbox_put");
    }
    
    return 0;
}

static int  _netcap_icmp_send( char *data, int data_len, netcap_pkt_t* pkt, int flags );

/* Move the data pointer so that it points to the correct location inside of the packet, 
 * rather than starting at the header of the packet
 */
static int  _icmp_fix_packet( netcap_pkt_t* pkt, u_char** full_pkt, int* full_pkt_len );

/**
 * Determine how a session should be handled.
 */
static _find_t _icmp_find_session( netcap_pkt_t* pkt, netcap_session_t** netcap_sess, 
                                   u_char* full_pkt, int full_pkt_len );

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

void netcap_icmp_null_hook( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg)
{
    errlog( ERR_WARNING, "ICMP: NULL HOOK, razing packet(%#10x), session(%#10x)\n", pkt, netcap_sess );

    netcap_icmp_cleanup_hook( netcap_sess, pkt, arg );
}

void netcap_icmp_cleanup_hook( netcap_session_t* netcap_sess, netcap_pkt_t* pkt, void* arg)
{
    if ( pkt != NULL ) netcap_pkt_raze( pkt );

    if ( netcap_sess != NULL ) netcap_session_raze( netcap_sess );
}

int  netcap_icmp_call_hook( netcap_pkt_t* pkt )
{
    netcap_session_t* netcap_sess = NULL;
    int ret = -1;
    if ( pkt == NULL ) return errlogargs();

    if ( pkt->data == NULL ) return errlogargs();
    
    int critical_section(void) {
        int ret = -1;
        u_char* full_pkt;
        int full_pkt_len;

        if ( _icmp_fix_packet( pkt, &full_pkt, &full_pkt_len ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "_icmp_fix_packet\n" );
            return ret;
        }
        
        switch( _icmp_find_session( pkt, &netcap_sess, full_pkt, full_pkt_len )) {
        case _FIND_EXIST:
            /* Packets in mailbox, nothing left to do */
            pkt = NULL;
            ret = 0;
            break;
            
            /* Call the hooks */
        case _FIND_NEW:
            /* Packet has already been put into the session mailbox */
            pkt = NULL;
            /* fallthrough */
        case _FIND_NONE:
            debug( 10, "ICMP: Calling global icmp hook\n" );
            global_icmp_hook( netcap_sess, pkt, NULL ); /* XXX NULL arg */
            ret = 0;
            break;

        case _FIND_DROP:
            debug( 10, "ICMP: Dropping packet\n" );
            netcap_pkt_action_raze( pkt, NF_DROP );
            pkt = NULL;
            ret = 0;
            return 0;

        case _FIND_ACCEPT:
            debug( 10, "ICMP: Accepting packet\n" );
            netcap_set_verdict_mark(pkt->packet_id, NF_REPEAT, NULL, 0, 1, MARK_DUPE|MARK_ANTISUB);
            pkt->packet_id = 0;
            netcap_pkt_raze( pkt );
            pkt = NULL;
            ret = 0;
            return 0;

        case _FIND_ERROR:
        default:
        {
            int icmp_type = -1;
            int icmp_code = -1;
            struct icmp *packet = (struct icmp*)pkt->data;
            if ( packet != NULL ) {
                icmp_type = packet->icmp_type;
                icmp_code = packet->icmp_code;
            }
            
            if ( pkt != NULL ) {
                ret = errlog ( ERR_CRITICAL, "_icmp_find_session (%s:%d -> %s:%d), type %d code %d\n",
                               unet_next_inet_ntoa ( pkt->src.host.s_addr ), pkt->src.port, 
                               unet_next_inet_ntoa ( pkt->dst.host.s_addr ), pkt->dst.port,
                               icmp_type, icmp_code );
            }
        }
        }

        /* Drop the packet, but hold onto the data. */
        if ( pkt != NULL ) {
            debug( 10, "ICMP: Dropping packet (%#10x) and passing data\n", pkt->packet_id );
            if ( pkt->packet_id != 0 && netcap_set_verdict( pkt->packet_id, NF_DROP, NULL, 0 ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
            }
            /* Clear out the packet id */
            pkt->packet_id = 0;
        }
        return ret;
    }

    ret = critical_section();
    if ( ret < 0 ) {
        if ( pkt != NULL ) {
            netcap_pkt_raze( pkt );
        }
    }
    return ret;
}

int  netcap_icmp_send( char *data, int data_len, netcap_pkt_t* pkt )
{
    return _netcap_icmp_send( data, data_len, pkt, 0 );
}

int  netcap_icmp_update_pkt( char* data, int data_len, int data_lim,
                             int icmp_type, int icmp_code, int id, mailbox_t* icmp_mb )
{
    /* Length of the data that is copied in */
    int len;

    /* New packet length */
    int new_len = data_len;

    struct icmp*   icmp_header;
    netcap_icmp_msg_t* msg;
    char* reply_data;
    int mb_size = 0;

    if ( data == NULL || icmp_mb == NULL )
        return errlogargs();

    if ( data_len < data_lim ) {
        return errlog( ERR_CRITICAL, "Data is larger than the buffer\n" );
    }

    if ( netcap_icmp_verify_type_and_code( icmp_type, icmp_code ) < 0 ) {
        return errlog( ERR_WARNING, "netcap_icmp_verify_type_and_code[%d,%d]\n", icmp_type, icmp_code );
    }


    /* By default do not modify the length of the packet */
    new_len = data_len;
    
    switch ( icmp_type ) {
    case ICMP_ECHO:
        /* fallthrough */
    case ICMP_ECHOREPLY:
        if ( data_lim < ICMP_MINLEN ) {
            return errlog( ERR_WARNING, "Not enough room, %d < %d\n", data_lim, ICMP_MINLEN );
        }
        
        icmp_header = (struct icmp*)data;

        if ( id > 0 ) {
            /* id is a 16 bit field */
            id = id & 0xFFFF;
            /* Convert to a network short */
            id = htons( id );
            if ( icmp_header->icmp_id != id ) {
                debug( 9, "ICMP: change id, %d to %d\n", ntohs( icmp_header->icmp_id ), ntohs( id ));
                       
                icmp_header->icmp_id = id;

                /* Update the checksum */
                icmp_header->icmp_cksum = 0;
                icmp_header->icmp_cksum = unet_in_cksum( (u_int16_t*)data, new_len );
            }
        }
        break;

    case ICMP_PARAMETERPROB:
        errlog( ERR_WARNING, "ICMP: parameter problem packet\n" );
        /* fallthrough */
        /* XXX Doesn't change the code for packets that do not fit one of the error conditions */

    case ICMP_DEST_UNREACH:
        /* fallthrough */
    case ICMP_SOURCE_QUENCH:
        /* fallthrough */
    case ICMP_REDIRECT:
        /* fallthrough */
    case ICMP_TIME_EXCEEDED:
        /* fallthrough */
        /* Fix the packet */

        /* XXX May need the ID of the last packet received */
        if ( data_lim < ICMP_ADVLENMIN ) {
            return errlog( ERR_WARNING, "Not enough room, %d < %d\n", data_lim, ICMP_ADVLENMIN );
        }
                
        if (( mb_size = mailbox_size( icmp_mb )) < 0 ) return errlog( ERR_WARNING, "ICMP: mailbox_size\n" );

        /* This will happen if the client sends an ICMP error packet before the server has sent anything. */
        if ( mb_size < 1 ) {
            errlog( ERR_WARNING, "ICMP: No packet available to update ICMP message.\n" );
            return 0;
        }

        if (( msg = mailbox_timed_get( icmp_mb, 1 )) == NULL ) {
            return errlog( ERR_CRITICAL, "mailbox_timed_get\n" );
        }
        
        reply_data = &msg->data;
        
        icmp_header = (struct icmp*)data;

        if ( icmp_header->icmp_type != icmp_type ) {
            debug( 4, "ICMP: Modifying type on packet (%d->%d)\n", icmp_header->icmp_type, icmp_type );
            icmp_header->icmp_type = icmp_type;
        }

        if ( icmp_header->icmp_code != icmp_code ) {
            debug( 4, "ICMP: Modifying code on packet (%d->%d)\n", icmp_header->icmp_code, icmp_code );
            icmp_header->icmp_code = icmp_code;
        }

        len = msg->data_len;
        if ( data_lim < ( sizeof( struct icmphdr ) + len )) {
            debug( 9, "ICMP: Not enough space to put entire cached packet, truncated to %d bytes\n",
                   data_lim - sizeof( struct icmphdr ));
            len = data_lim - sizeof( struct icmphdr );
        }
        
        /* Copy in the data packet */
        debug( 10, "ICMP: Updating packet: copying in %d bytes\n", len );

        memcpy( &icmp_header->icmp_ip, reply_data, len );

        /* Update the length of the packet */
        new_len = sizeof( struct icmphdr ) + len;

        if ( _restore_cached_msg( icmp_mb, msg ) < 0 ) {
            errlog( ERR_CRITICAL, "restore_cached_msg\n" );
        }
               
        /* Update the checksum */
        icmp_header->icmp_cksum = 0;
        icmp_header->icmp_cksum = unet_in_cksum( (u_int16_t*)data, new_len );

    default:
        break;
    }        
    
    return new_len;
}

int  netcap_icmp_get_source( char* data, int data_len, netcap_pkt_t* pkt, struct in_addr* source )
{
    struct icmp* icmp_pkt;

    if ( data == NULL || pkt == NULL || source == NULL ) return errlogargs();
    
    if ( data_len < ICMP_MINLEN ) {
        errlog( ERR_WARNING, "ICMP Packet is too short\n" );
        return 0;
    }

    icmp_pkt = (struct icmp*)data;

    if ( netcap_icmp_verify_type_and_code( icmp_pkt->icmp_type, icmp_pkt->icmp_code ) < 0 ) {
        errlog( ERR_WARNING, "netcap_icmp_verify_type_and_code[%d,%d]\n", 
                icmp_pkt->icmp_type, icmp_pkt->icmp_code );
    }
    
    if ( ICMP_INFOTYPE( icmp_pkt->icmp_type )) {
        return 0;
    }
    
    if ( data_len <  ICMP_ADVLENMIN ) {
        errlog( ERR_WARNING, "ICMP Packet is too short" );
        return 0;
    }
        
    if ( icmp_pkt->icmp_ip.ip_dst.s_addr != pkt->src.host.s_addr ) {
        memcpy( source, &pkt->src.host, sizeof( struct in_addr ));
        return 1;
    }
    
    return 0;
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
    cmsg->cmsg_type  = IP_SADDR;
    memcpy( CMSG_DATA(cmsg), &pkt->src.host, sizeof( struct in_addr ));

    /* nfmark */
    cmsg = my__cmsg_nxthdr( &msg, cmsg, sizeof(pkt->ttl) );
    if( !cmsg ) {
        errlog( ERR_CRITICAL, "No more CMSG Room\n" );
        goto err_out;
    }
    cmsg->cmsg_len = CMSG_LEN(sizeof(nfmark));
    cmsg->cmsg_level = SOL_IP;
    cmsg->cmsg_type  = IP_SENDNFMARK;
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
        if ( errno == EINVAL && ( netcap_interface_is_broadcast( pkt->src.host.s_addr, 0 ))) {
            /* This is an attempt to send a message from a broadcast address, which is obviously,
             * not allowed */
            debug( 4, "ICMP: (%s -> %s) attempt to send ICMP message from a broadcast address\n" );
            /* Update to the data len so the packet is consumed */
            ret = data_len;
        } else {
            errlog( ERR_CRITICAL, "sendmsg: %s | (%s -> %s) len:%i ttl:%i tos:%i nfmark:%#10x\n", errstr,
                    unet_next_inet_ntoa( pkt->src.host.s_addr ), unet_next_inet_ntoa( pkt->dst.host.s_addr ),
                    data_len, pkt->ttl, pkt->tos, nfmark );            
        }
    }
    
    goto out;

 err_out:
    errlog( ERR_WARNING, "ICMP: Unable to send packet\n" );
    ret = -1;
 out:
    return ret;
}

static int  _icmp_fix_packet( netcap_pkt_t* pkt, u_char** full_pkt, int* full_pkt_len )
{
    int offset;

    struct iphdr* iph = (struct iphdr*)pkt->data;

    /* Have to move the data pointer, since right now it points to the whole packet */
    
    /* Get the length of the ip header */
    offset = iph->ihl;
    
    /* Validate the offset is valid XXX Magic numbers */
    if ( offset > 20 ) {
        return errlog( ERR_CRITICAL, "ICMP: Invalid data offset - %d\n", offset );
    }
    
    /* Words to bytes */
    offset = offset << 2;
    *full_pkt_len = pkt->data_len;
    pkt->data_len = pkt->data_len - offset;
    
    if (( pkt->data_len < 0 ) || ( pkt->data_len > QUEUE_MAX_MESG_SIZE )) {
        return errlog( ERR_CRITICAL, "ICMP: Invalid data size - %d\n", pkt->data_len );
    }
    
    /* Remove the header from the data buffer, and just move in the data */
    if ( pkt->buffer == NULL ) {
        return errlog( ERR_CRITICAL, "pkt->buffer is null\n" );
    }
    
    *full_pkt = pkt->data;
    pkt->data = &pkt->data[offset];

    return 0;
}

static _find_t _icmp_find_session( netcap_pkt_t* pkt, netcap_session_t** netcap_sess, 
                                   u_char* full_pkt, int full_pkt_len )
{
    /* Lookup the session information */
    struct icmp *packet = (struct icmp*)pkt->data;
    int ret = -1;

    netcap_session_t* session;
    mailbox_t* mb      = NULL;
    
    int critical_section(void) {
        int ret = -1;
        switch( packet->icmp_type ) {
            /* These both are treated as ICMP sessions with port 0 */
        case ICMP_ECHOREPLY:
            /* fallthrough */
        case ICMP_ECHO:
            return _FIND_ACCEPT;

        case ICMP_REDIRECT:
        case ICMP_SOURCE_QUENCH:
        case ICMP_TIME_EXCEEDED:
        case ICMP_DEST_UNREACH:
            /* Lookup the session, if it doesn't exist, then drop it */
            /* Here is the reasoning: ICMP DEST unreachable can come from two sources
             * 1. Packet is for the local host, if this is true, then the session should be
             *    in the conntrack table, and all sessions in the conntrack table are antisubscribed.
             * 2. Packet is not for the local host, if this is true, then the sessino should
             *    be in the session table, if this is true, then we put it in that sessions mb
             * Otherwise, this packet has no business being here, hence it is dropped
             */
            if (( session = _icmp_get_error_session( pkt, &mb )) == NULL ) {
                //if we failed to find it assume its for a session we are not 
                // watching.
                debug(10,"Could not find an ICMP session for %d\n",pkt->packet_id);
                ret = _FIND_ACCEPT;
                break;
            }
            
            if ( mb == NULL ) {
                ret = errlog( ERR_CRITICAL, "_icmp_get_error_session\n" );
                break;
            }
            
            /* Put the packet into the mailbox and drop the packet */
            if ( _icmp_put_mailbox( mb, pkt ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "_icmp_put_mailbox\n" );
                break;
            }
            
            ret = _FIND_EXIST;
            break;

        case ICMP_PARAMETERPROB:
        case ICMP_TIMESTAMP:
        case ICMP_TIMESTAMPREPLY:
        case ICMP_INFO_REQUEST:
        case ICMP_INFO_REPLY:
        case ICMP_ADDRESS:
        case ICMP_ADDRESSREPLY:
            /* We don't really care about these, these packets should be dropped */
            *netcap_sess = NULL;
            ret = _FIND_DROP;
            break;

            /* Just in case this is another type of packet */
        default:
            errlog( ERR_WARNING, "Unknown icmp packet type: %d, code: %d, dropping.\n", 
                    packet->icmp_type, packet->icmp_code );
            ret = _FIND_DROP;
            break;
        }
        return ret;
    }
    SESSTABLE_WRLOCK();
    ret = critical_section();
    SESSTABLE_UNLOCK();

    if ( ret < 0 ) ret = _FIND_ERROR;

    return ret;
}

static int _restore_cached_msg( mailbox_t* mb, netcap_icmp_msg_t* msg )
{
    int mb_size;
    
    if (( mb_size = mailbox_size( mb )) < 0 ) {
        netcap_icmp_msg_raze( msg );
        return errlog( ERR_CRITICAL, "mailbox_size\n" );
    }

    /* Only restore the packet if the size is zero */
    if ( mb_size == 0 ) {
        debug( 10, "ICMP: Restoring cached msg\n" );
        if ( mailbox_put( mb, (void*)msg ) < 0 ) {
            netcap_icmp_msg_raze( msg );
            return errlog( ERR_CRITICAL, "mailbox_put\n" );
        }
    } else {
        debug( 10, "ICMP: Dropping cached msg\n" );
        netcap_icmp_msg_raze( msg );
    }
    
    return 0;
}

/* this gets rid of the mess in libc (in bits/socket.h) */
static struct cmsghdr * my__cmsg_nxthdr(struct msghdr *msg, struct cmsghdr *cmsg, int size)
{
	struct cmsghdr * ptr;

	ptr = (struct cmsghdr*)(((unsigned char *) cmsg) +  CMSG_ALIGN(cmsg->cmsg_len));

    if ((((char*)ptr) + CMSG_LEN(size)) > ((char*)msg->msg_control + msg->msg_controllen)) {
		return (struct cmsghdr *)0;
    }

	return ptr;
}
