#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>

#include <libnetcap.h>

#include "netcap_session.h"
#include "netcap_sesstable.h"

int netcap_udp_session_init(netcap_session_t* netcap_sess, netcap_pkt_t* pkt) 
{
    netcap_endpoints_t endpoints;
    
    if ( pkt == NULL ) return errlogargs();

    netcap_endpoints_bzero( &endpoints );
        
    endpoints.protocol    = IPPROTO_UDP;
    endpoints.cli.port    = pkt->src.port;
    endpoints.srv.port    = pkt->dst.port;

    memcpy(&endpoints.cli.host,&pkt->src.host,sizeof(in_addr_t));
    memcpy(&endpoints.srv.host,&pkt->dst.host,sizeof(in_addr_t));

    netcap_sess->cli_intf = endpoints.cli.intf = pkt->src.intf;
    netcap_sess->srv_intf = endpoints.srv.intf = pkt->dst.intf;

    /* Set alive to true */
    netcap_sess->alive = 1;

    /* Set the protocol */
    netcap_sess->protocol = IPPROTO_UDP;

    /* Set the TTL and TOS value */
    netcap_sess->ttl      = pkt->ttl;
    netcap_sess->tos      = pkt->tos;

    if ( netcap_session_init(netcap_sess, &endpoints, NC_SESSION_IF_MB) < 0 ) {
        return errlog(ERR_CRITICAL, "netcap_session_create");
    }

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
            errlog(ERR_CRITICAL,"netcap_udp_session_free");
        }

        return errlog_null(ERR_CRITICAL,"netcap_udp_session_init");        
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
        if ( netcap_shield_rep_end_session( netcap_sess->cli.cli.host.s_addr ) < 0 ) {
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
