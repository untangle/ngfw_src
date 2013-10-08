/**
 * $Id: netcap_session.c 35571 2013-08-08 18:37:27Z dmorris $
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
    u_int32_t epoch = (u_int32_t)time(NULL);

    session_index = 0;

    // highest 16 bits are zero
    // middle  32 bits should be epoch
    // lowest  16 bits are zero
    // this means that session_indexs should be ever increasing despite restarts
    // (unless there are more than 16 bits or 65000 sessions per second on average)
    session_index |= ( (u_int64_t)epoch & 0xFFFFFFFF ) << 16;

    // double check that top bit is zero to avoid signedness issues with java
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



