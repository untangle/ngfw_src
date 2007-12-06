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

#include <netinet/ip.h>
#include <netinet/udp.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>

#include <libnetcap.h>

#include "netcap_globals.h"
#include "netcap_queue.h"
#include "netcap_nfconntrack.h"
#include "netcap_session.h"
#include "netcap_sesstable.h"

/* 30 second timeout for the initial conntrack entry */
#define UDP_SESSION_TIMEOUT  30

/* callback for a UDP session */
static int _callback    ( netcap_session_t* netcap_sess, netcap_callback_action_t action, 
                          netcap_callback_flag_t flags );

/* liberate a session from being caught by netcap */
static int  _liberate   ( netcap_session_t* netcap_sess, netcap_callback_action_t action,
                          netcap_callback_flag_t flags );

/* Liberate an individual packet */
static int _liberate_pkt( netcap_pkt_t* pkt );

int netcap_udp_session_init( netcap_session_t* netcap_sess, netcap_pkt_t* pkt ) 
{
    netcap_endpoints_t endpoints;
    
    if ( pkt == NULL ) return errlogargs();

    /* XXX ICMP Hack */
    if (( pkt->proto != IPPROTO_UDP ) && ( pkt->proto != IPPROTO_ICMP )) {
        return errlog( ERR_CRITICAL, "non-udp and icmp packet for udp session: %d.\n", pkt->proto );
    }

    netcap_endpoints_bzero( &endpoints );
        
    memcpy( &endpoints.cli, &pkt->src, sizeof( endpoints.cli ));
    memcpy( &endpoints.srv, &pkt->dst, sizeof( endpoints.srv ));
    
    endpoints.intf = pkt->src_intf;

    if ( netcap_session_init( netcap_sess, &endpoints, pkt->dst_intf, NC_SESSION_IF_MB ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_session_init\n" );
    }

    /* Set alive to true */
    netcap_sess->alive = 1;

    /* Set the protocol */
    netcap_sess->protocol = pkt->proto;

    /* Set the TTL and TOS value */
    netcap_sess->ttl      = pkt->ttl;
    netcap_sess->tos      = pkt->tos;

    /* Set the callback, for most actions this doesn't do anything */
    netcap_sess->callback = _callback;

    debug(7,"UDP: FLAG Copying NAT info from packet to session\n");
    netcap_sess->nat_info = pkt->nat_info;

    return 0;
}

// Create a new session
netcap_session_t* netcap_udp_session_create(netcap_pkt_t* pkt)
{
    netcap_session_t* netcap_sess;

    if ((netcap_sess = netcap_udp_session_malloc()) == NULL) {
        return errlog_null(ERR_CRITICAL,"netcap_udp_session_malloc");
    }

    if ( netcap_udp_session_init(netcap_sess,pkt) < 0) {
        if ( netcap_udp_session_free(netcap_sess)) {
            errlog( ERR_CRITICAL, "netcap_udp_session_free\n" );
        }

        return errlog_null( ERR_CRITICAL, "netcap_udp_session_init\n" );
    }

    return netcap_sess;
}

int netcap_udp_session_destroy(int if_lock, netcap_session_t* netcap_sess) {
    int err = 0;

    if ( netcap_sess == NULL ) {
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    }
    
    /* Remove the session from the endpoints first */
    netcap_sesstable_remove_session(if_lock, netcap_sess);

    /* If you removed the endpoints, then you ended an actual session */
    if ( netcap_sess->remove_tuples ) {
        if ( netcap_shield_rep_end_session( &netcap_sess->cli.cli.host ) < 0 ) {
            err -= errlog(ERR_CRITICAL,"netcap_shield_rep_end_session\n");
        }
    }

    // Free the session and its mailboxes
    if ( netcap_nc_session__destroy(netcap_sess,NC_SESSION_IF_MB) ) {
        err -= errlog(ERR_CRITICAL,"netcap_session_raze");
    }

    return err;
}

int netcap_udp_session_raze(int if_lock, netcap_session_t* netcap_sess)
{
    int err = 0;

    if ( netcap_sess == NULL ) {
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    }

    if ( netcap_udp_session_destroy(if_lock, netcap_sess) < 0 ) {
        err -= 1;
        errlog(ERR_CRITICAL,"netcap_udp_session_destroy");
    }

    if ( netcap_udp_session_free(netcap_sess) < 0 ) {
        err -= 2;
        errlog(ERR_CRITICAL,"netcap_udp_session_free");
    }

    return err;
}

/**************************************** STATIC ****************************************/

static int _callback ( netcap_session_t* netcap_sess, netcap_callback_action_t action, 
                       netcap_callback_flag_t flags )
{
    if ( netcap_sess == NULL ) return errlogargs();
        
    switch ( action ) {
    case SRV_COMPLETE: 
        /* fallthrough */
    case CLI_COMPLETE: 
        /* fall through */
    case CLI_DROP:
        /* fallthrough */
        return 0;
    case CLI_ICMP:
        /* XXXX Should do something here */
        /* fallthrough */
    case CLI_RESET:
        /* XXXX Should do something here */
    case CLI_FORWARD_REJECT:
        /* XXXX Should do something here */
        errlog( ERR_WARNING, "_udp_rejection type %d is not implemented, ignoring\n", action );
        return 0;
        
    case LIBERATE:
        return _liberate( netcap_sess, action, flags );

    default:
        return errlog( ERR_CRITICAL, "Unknown action: %i\n", action );
    }

    return errlogcons();
}

/* liberate a session from being caught by netcap */
static int  _liberate( netcap_session_t* netcap_sess, netcap_callback_action_t action,
                       netcap_callback_flag_t flags )
{
    netcap_pkt_t* pkt = NULL;
    int count = 0;

    /* this has to release any of the queued packets with a special mark indicating that they
     * are liberated, this applies to both ICMP and UDP packets. */

    /* Iterate through all of the packets grabbing them out of the mailbox */
    while (( pkt = (netcap_pkt_t*)mailbox_try_get( &netcap_sess->cli_mb )) != NULL ) {
        if ( _liberate_pkt( pkt ) < 0 ) errlog( ERR_CRITICAL, "_liberate_pkt\n" );
        netcap_pkt_raze( pkt );
        count++;
    }

    /* If there weren't any packets liberated and the session was alive, then print an error message */
    if (  netcap_sess->alive && ( 0 == count )) {
        errlog( ERR_WARNING, "_liberated a session that contained no packet\n" );
    }

    return 0;
}

static int _liberate_pkt( netcap_pkt_t* pkt )
{
    if ( pkt == NULL ) return errlogargs();
    
    /* decrement the ttl on outgoing packets to discourage floods */
    if ( 0 == pkt->ttl ) {
        debug( 10, "UDP_SESSION: dropping packet with TTL of zero.\n" );
    } else {
        debug( 10, "UDP_SESSION: liberating a packet with TTL %d.\n", pkt->ttl );
        
        /* decrement the ttl */
        pkt->ttl--;
        
        /* Set the mark on the packet this guarantees that it is connmarked */
        pkt->is_marked = IS_MARKED_FORCE_FLAG;
        pkt->nfmark    = MARK_ANTISUB | MARK_LIBERATE;
        
        /* Actually release the packet */
        /* have to determine which type of packet this is */
        
        /* !!!! ICMP or UDP !!!! */
        switch ( pkt->proto ) {
        case IPPROTO_UDP:
	  if ( netcap_set_verdict_mark( pkt->packet_id, NF_ACCEPT, NULL, 0, 1,  
					pkt->nfmark | MARK_ANTISUB ) < 0 ) {  
	    errlog( ERR_CRITICAL, "netcap_set_verdict_mark\n" );  
	  } 
	  //netcap_udp_send( pkt->data, pkt->data_len, pkt );
	  break;
                
        case IPPROTO_ICMP:
            netcap_icmp_send( pkt->data, pkt->data_len, pkt );
            break;
                
        default:
            return errlog( ERR_WARNING, "Unable to liberate packet of unknown protocol %d\n", pkt->proto );
        }
    }

    return 0;
}
