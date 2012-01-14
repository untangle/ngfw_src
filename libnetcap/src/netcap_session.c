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
#include "netcap_session.h"

#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <time.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/lock.h>
#include <mvutil/mailbox.h>
#include <mvutil/unet.h>

#include "libnetcap.h"

#include "netcap_globals.h"
#include "netcap_init.h"
#include "netcap_icmp.h"
#include "netcap_queue.h"
#include "netcap_sesstable.h"
#include "netcap_tcp.h"

static u_int64_t session_index = 1;
static lock_t session_index_lock;

static u_int64_t netcap_session_next_id( void );

static session_tls_t* _tls_get( void );

int netcap_sessions_init ( void )
{
    int c;

    // Set the session index to a random value
    /* XXX This isn't a particularly good seed */
    srand(getpid() ^ time( NULL ));
    
    /* Calculate a 31-bit random value */
    session_index = 0;

    for ( c = 0 ; ( session_index == 0 ) && ( c < 7 ) ; c++ ) {
        session_index |= ( (u_int64_t)rand() & 0xFFFF );
        session_index |= ( (u_int64_t)rand() & 0xFFFF ) << 16; 
        session_index |= ( (u_int64_t)rand() & 0xFFFF ) << 32; 
        session_index |= ( (u_int64_t)rand() & 0xFFFF ) << 48; 
    }
    
    if ( session_index == 0 ) {
        return errlog( ERR_CRITICAL, "Unable to generate a non-zero random number in %d attempts\n", c );
    }

    /* Down to 63 bit - avoid signedness issues with java*/
    session_index &= 0x7FFFFFFFFFFFFFFFULL;

    // Initialize the locks on the index
    if (lock_init(&session_index_lock,0)<0) {
        return errlog(ERR_CRITICAL,"lock_init\n");
    }
    
    return 0;
}

int netcap_sessions_cleanup ( void )
{
    if (lock_destroy(&session_index_lock)<0)
        return errlog(ERR_CRITICAL,"lock_destroy\n");

    return 0;
}

int netcap_session_tls_init( session_tls_t* tls )
{
    if ( tls == NULL ) return errlogargs();

    /* Nothing to do here, tls is just an output buffer */
    return 0;
}

char* netcap_session_tuple_print ( netcap_session_t* sess )
{
    session_tls_t* tls;
    
    if ( sess == NULL ) return errlogargs_null();
    
    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(%s:%-5i -> %s:%-5i)", 
              unet_next_inet_ntoa( sess->cli.cli.host.s_addr ), sess->cli.cli.port,
              unet_next_inet_ntoa( sess->srv.srv.host.s_addr ), sess->srv.srv.port );
    
    return tls->output_buf;
}

char* netcap_session_srv_tuple_print ( netcap_session_t* sess )
{
    session_tls_t* tls;

    if (!sess) return errlogargs_null();

    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(%s:%-5i)",
              unet_next_inet_ntoa( sess->srv.srv.host.s_addr ), sess->srv.srv.port );

    return tls->output_buf;
}

char* netcap_session_cli_tuple_print ( netcap_session_t* sess )
{
    session_tls_t* tls;

    if (!sess) return errlogargs_null();

    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(%s:%-5i)",
              unet_next_inet_ntoa( sess->cli.cli.host.s_addr ), sess->cli.cli.port );
    
    return tls->output_buf;
}

char* netcap_session_srv_endp_print ( netcap_session_t* sess )
{
    session_tls_t* tls;

    if (!sess) return errlogargs_null();

    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(%s:%-5i) -> (%s:%-5i)",
              unet_next_inet_ntoa( sess->srv.cli.host.s_addr ), sess->srv.cli.port,
              unet_next_inet_ntoa( sess->srv.srv.host.s_addr ), sess->srv.srv.port );
    
    return tls->output_buf;
}

char* netcap_session_cli_endp_print ( netcap_session_t* sess )
{
    session_tls_t* tls;

    if (!sess) return errlogargs_null();

    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(%s:%-5i) -> (%s:%-5i)",
              unet_next_inet_ntoa( sess->cli.cli.host.s_addr ), sess->cli.cli.port,
              unet_next_inet_ntoa( sess->cli.srv.host.s_addr ), sess->cli.srv.port );

    return tls->output_buf;
}
    
char* netcap_session_fd_tuple_print ( netcap_session_t* sess )
{
    session_tls_t* tls;

    if ( !sess ) return errlogargs_null();

    if (( tls = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );

    snprintf( tls->output_buf, sizeof( tls->output_buf ), "(fd: %i,%i)", 
              sess->client_sock, sess->server_sock );

    return tls->output_buf;
}

u_int64_t netcap_session_next_id ( void )
{
    if (lock_wrlock(&session_index_lock)<0) {
        errlog(ERR_CRITICAL,"lock_wrlock\n");
        return 0;
    }
    
    session_index++;

    /* Down to 63 bit - avoid signedness issues with java*/
    session_index &= 0x7FFFFFFFFFFFFFFFULL;
    
    if ( session_index == 0 ) session_index++; /* session id 0 not allowed */
    
    if (lock_unlock(&session_index_lock)<0) {
        errlog(ERR_CRITICAL,"lock_unlock\n");
        return 0;
    }

    return session_index;
}

netcap_session_t* netcap_session_malloc(void)
{
    netcap_session_t* netcap_sess = NULL;
    
    if (( netcap_sess = calloc( 1, sizeof(netcap_session_t))) == NULL ) return errlogmalloc_null();

    return netcap_sess;
 }

int netcap_session_init( netcap_session_t* netcap_sess, netcap_endpoints_t *endpoints, netcap_intf_t srv_intf, int if_mb )
{
    if ( endpoints == NULL ) return errlog(ERR_CRITICAL, "Invalid arguments");

    // Clear out the app_data variable
    netcap_sess->app_data = NULL;
    
    // Set the id
    netcap_sess->session_id = netcap_session_next_id();

    /* Indicate to remove endpoints */
    netcap_sess->remove_tuples = 1;

    // Set the traffic structures
    netcap_endpoints_copy( &netcap_sess->srv, endpoints );
    netcap_endpoints_copy( &netcap_sess->cli, endpoints );

    netcap_sess->srv.intf = srv_intf;

    if ( netcap_sess->session_id == 0 ) return errlog( ERR_CRITICAL, "netcap_session_next_id\n" );
    
    // If needed, Create the two mail boxes
    if ( if_mb ) {
        if (mailbox_init(&netcap_sess->srv_mb)<0) {
            return errlog(ERR_CRITICAL,"mailbox_init\n");
        }
        
        if (mailbox_init(&netcap_sess->cli_mb)<0) {
            if ( mailbox_destroy(&netcap_sess->srv_mb) < 0 ) {
                perrlog("mailbox_destroy");
            }
            
            return errlog(ERR_CRITICAL,"mailbox_init\n");
        }
    }

    /* Clear out the ICMP identifiers */
    netcap_sess->icmp.client_id = 0;
    netcap_sess->icmp.server_id = 0;

    /* Clear out the dead TCP session flags */
    netcap_sess->dead_tcp.exit_type = TCP_CLI_DEAD_NULL;
    netcap_sess->dead_tcp.type      = 255;
    netcap_sess->dead_tcp.code      = 255;
    netcap_sess->dead_tcp.use_src   = 0;
    netcap_sess->dead_tcp.redirect  = (in_addr_t)0;
    netcap_sess->dead_tcp.src       = (in_addr_t)0;

    /* null out the callback */
    netcap_sess->callback           = NULL;

    netcap_sess->first_pkt_id = 0;

    return 0;
}

netcap_session_t* netcap_session_create( netcap_endpoints_t *endpoints, netcap_intf_t srv_intf, int if_mb )
{
    netcap_session_t* netcap_sess = NULL;

    netcap_sess = netcap_session_malloc();

    if ( !netcap_sess ) {
        return errlog_null(ERR_CRITICAL,"netcap_session_malloc");
    }

    if ( netcap_session_init( netcap_sess, endpoints, srv_intf, if_mb ) < 0 ) {
        free ( netcap_sess );
        return errlog_null( ERR_CRITICAL, "netcap_session_init" );
    }

    return (netcap_sess);
}

int netcap_session_free(netcap_session_t* netcap_sess) {
    if ( !netcap_sess ) {
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    }

    free(netcap_sess);
    return 0;
}

int netcap_session_destroy(netcap_session_t* netcap_sess)
{
    return netcap_nc_session_destroy(NC_SESSTABLE_LOCK,netcap_sess);
}

int netcap_nc_session_destroy(int if_lock, netcap_session_t* netcap_sess)
{
    if ( !netcap_sess ) {
        return errlogargs();
    }

    switch ( netcap_sess->protocol ) {
    case IPPROTO_TCP:
        return netcap_tcp_session_destroy(if_lock,netcap_sess);

    case IPPROTO_ICMP:
    case IPPROTO_UDP:
        return netcap_udp_session_destroy(if_lock,netcap_sess);

    default:
        return errlog( ERR_CRITICAL, "Unable to determine session protocol %d\n", netcap_sess->protocol );
    }
}

int netcap_nc_session__destroy (netcap_session_t* netcap_sess, int if_mb)
{
    netcap_pkt_t* pkt;

    if ( !netcap_sess ) return errlogargs();

    if ( netcap_sess->first_pkt_id != 0 ) {
        if ( netcap_set_verdict( netcap_sess->first_pkt_id, NF_DROP, NULL, 0 ) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_set_verdict\n" );
        }
    }

    netcap_sess->first_pkt_id = 0;

    // Clear out the two mailboxes
    if ( if_mb ) {
        while((pkt = (netcap_pkt_t*)mailbox_try_get(&netcap_sess->cli_mb))) {
            netcap_pkt_action_raze( pkt, NF_DROP );
        }

        while((pkt = (netcap_pkt_t*)mailbox_try_get(&netcap_sess->srv_mb))) {
            netcap_pkt_action_raze( pkt, NF_DROP );
        }

        if (mailbox_destroy(&netcap_sess->cli_mb)<0) {
            errlog(ERR_WARNING,"mailbox_destroy failed\n");
        }
        
        if (mailbox_destroy(&netcap_sess->srv_mb)<0) {
            errlog(ERR_WARNING,"mailbox_destroy failed\n");
        }
    }

    return 0;
}

int netcap_session_raze (netcap_session_t* netcap_sess)
{
    return netcap_nc_session_raze(NC_SESSTABLE_LOCK,netcap_sess);
}

int netcap_nc_session_raze(int if_lock, netcap_session_t* netcap_sess)
{
    if ( !netcap_sess ) {
        return errlogargs();
    }

    switch ( netcap_sess->protocol ) {
    case IPPROTO_TCP:
        return netcap_tcp_session_raze(if_lock,netcap_sess);

    case IPPROTO_UDP:
    case IPPROTO_ICMP:
        return netcap_udp_session_raze(if_lock,netcap_sess);

    default:
        return errlog( ERR_CRITICAL, "Unable to determine session protocol: %d\n", netcap_sess->protocol );
    }
}

static session_tls_t* _tls_get( void )
{
    netcap_tls_t* netcap_tls;
    if (( netcap_tls = netcap_tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "netcap_tls_get\n" );
    
    return &netcap_tls->session;
}



