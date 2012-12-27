/**
 * $Id$
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

/* callback for a UDP session */
static int _callback    ( netcap_session_t* netcap_sess, netcap_callback_action_t action );

/* liberate a session from being caught by netcap */
static int  _liberate   ( netcap_session_t* netcap_sess, netcap_callback_action_t action );

/* Liberate an individual packet */
static int _liberate_pkt( netcap_session_t* netcap_sess, netcap_pkt_t* pkt );


int netcap_udp_session_init( netcap_session_t* netcap_sess, netcap_pkt_t* pkt ) 
{
    netcap_endpoints_t endpoints;
    
    if ( pkt == NULL ) return errlogargs();

    if ( pkt->proto != IPPROTO_UDP ) {
        return errlog( ERR_CRITICAL, "non-udp packet for udp session: %d.\n", pkt->proto );
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

static int _callback ( netcap_session_t* netcap_sess, netcap_callback_action_t action )
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
        return _liberate( netcap_sess, action );

    default:
        return errlog( ERR_CRITICAL, "Unknown action: %i\n", action );
    }

    return errlogcons();
}

/* liberate a session from being caught by netcap */
static int  _liberate( netcap_session_t* netcap_sess, netcap_callback_action_t action )
{
    netcap_pkt_t* pkt = NULL;
    int count = 0;

    /* Iterate through all of the packets grabbing them out of the mailbox */
    while (( pkt = (netcap_pkt_t*)mailbox_try_get( &netcap_sess->cli_mb )) != NULL ) {
        if ( _liberate_pkt( netcap_sess, pkt ) < 0 ) errlog( ERR_CRITICAL, "_liberate_pkt\n" );

        /* It will only be dropped if there was an error in liberate
         * that didn't clear out the packet_id */
        netcap_pkt_action_raze( pkt, NF_DROP );
        count++;
    }
    
    /* If there weren't any packets liberated and the session was alive, then print an error message */
    if (  netcap_sess->alive && ( 0 == count )) {
        errlog( ERR_WARNING, "_liberated a session that contained no packet\n" );
    }

    return 0;
}

static int _liberate_pkt( netcap_session_t* netcap_sess, netcap_pkt_t* pkt )
{
    if ( pkt == NULL ) return errlogargs();

    /* decrement the ttl on outgoing packets to discourage floods */
    if ( pkt->ttl == 0 ) {
        debug( 10, "UDP_SESSION: dropping packet with TTL of zero.\n" );
    } else {
        debug( 10, "UDP_SESSION: liberating a packet with TTL %d.\n", pkt->ttl );
        
        /* decrement the ttl */
        pkt->ttl--;
        
        /* Set the mark on the packet this guarantees that it is connmarked */
        pkt->is_marked = IS_MARKED_FORCE_FLAG;
        pkt->nfmark    = MARK_ANTISUB | MARK_LIBERATE | MARK_ANTISUB | MARK_DUPE;
       
        /* XXX In order to make this work, the NATd address must be restored. XXXX */
        if ( netcap_udp_send( (char*)pkt->data, pkt->data_len, pkt ) < 0 )  {
            errlog( ERR_CRITICAL, "netcap_udp_send\n" );   
        } 
    }

    return 0;
}
