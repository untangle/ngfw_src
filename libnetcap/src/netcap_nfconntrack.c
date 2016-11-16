/**
 * $Id$
 */
#include <pthread.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <stdlib.h>
#include <inttypes.h>

#include <libnetfilter_queue/libnetfilter_queue.h>

#include <mvutil/list.h>
#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_nfconntrack.h"

pthread_mutex_t query_mutex = PTHREAD_MUTEX_INITIALIZER;
struct nfct_handle* query_handle = NULL;
pthread_mutex_t dump_mutex = PTHREAD_MUTEX_INITIALIZER;
struct nfct_handle* dump_handle = NULL;

/**
 * This is the list used to store the results of the dump callback.
 * It can only be manipulated and accessed while holding dump_mutex
 */
list_t* dump_list = NULL;

/**
 * This stores the results of the query from query_handle
 * It can only be manipulated and accessed while holding query_mutex
 */
struct nf_conntrack* query_result = NULL;

static int _nfconntrack_tuple_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data );
static int _nfconntrack_dump_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data );
static int _nfconntrack_query( struct nf_conntrack* ct, const enum nf_conntrack_query query );

/**
 * Initialize the netfilter conntrack library.
 */
int  netcap_nfconntrack_init()
{
    debug( 2, "Initializing netcap_nfconntrack...\n" );

   if (( query_handle = nfct_open( CONNTRACK, 0 )) == NULL )
        return perrlog( "nfct_open" );
    if (( dump_handle = nfct_open( CONNTRACK, 0 )) == NULL )
        return perrlog( "nfct_open" );
    if ( nfct_callback_register( query_handle, NFCT_T_ALL, _nfconntrack_tuple_callback, NULL ) < 0 )
        return perrlog( "nfct_callback_register" );
    if ( nfct_callback_register( dump_handle, NFCT_T_ALL, _nfconntrack_dump_callback, NULL ) < 0 )
        return perrlog( "nfct_callback_register" );
    if ( pthread_mutex_init( &query_mutex, NULL ) < 0 )
        return perrlog( "pthread_mutex_init" );
    if ( pthread_mutex_init( &dump_mutex, NULL ) < 0 )
        return perrlog( "pthread_mutex_init" );

    debug( 2, "Initialization completed.\n" );

    return 0;
}

/**
 * Cleanup all of the filedescriptors associated with the conntrack handle.
 */
int  netcap_nfconntrack_cleanup( void )
{
    debug( 2, "Cleanup\n" );

    if (( query_handle != NULL ) && ( nfct_close( query_handle ))) {
        perrlog( "nfct_close" );
    }
    query_handle = NULL;
    if (( dump_handle != NULL ) && ( nfct_close( dump_handle ))) {
        perrlog( "nfct_close" );
    }
    dump_handle = NULL;

    return 0;
}

/**
 * Update the connmark of the session
 */
int  netcap_nfconntrack_update_mark( netcap_session_t* session, u_int32_t mark)
{
    struct nf_conntrack* ct = NULL;
    if (( ct = nfct_new()) == NULL ) return errlog( ERR_CRITICAL, "nfct_new\n" );
    
    /* set protocol info */
    nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
    nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, session->protocol );
    nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
    nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, session->protocol );
            
    /* set the client side (orig) info */
    nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, session->cli.cli.host.s_addr);
    nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, htons(session->cli.cli.port) );
    nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, session->cli.srv.host.s_addr );
    nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, htons(session->cli.srv.port) );
            
    /* set the server side (reply) info */
    nfct_set_attr_u32( ct, ATTR_REPL_IPV4_SRC, session->srv.srv.host.s_addr);
    nfct_set_attr_u16( ct, ATTR_REPL_PORT_SRC, htons(session->srv.srv.port) );
    nfct_set_attr_u32( ct, ATTR_REPL_IPV4_DST, session->srv.cli.host.s_addr );
    nfct_set_attr_u16( ct, ATTR_REPL_PORT_DST, htons(session->srv.cli.port) );

    /* set the mark to set */
    nfct_set_attr_u32( ct, ATTR_MARK, mark );

    /* update the mark */
    _nfconntrack_query(ct, NFCT_Q_UPDATE);

    if ( ct != NULL )
        nfct_destroy( ct );
    
    return 0;
}

/**
 * destroy the contrack of the session
 */
int  netcap_nfconntrack_destroy_conntrack( const u_int32_t protocol, const char* c_client_addr, const u_int32_t c_client_port, const char* c_server_addr, const u_int32_t c_server_port )
{
    struct nf_conntrack* ct = NULL;
    if (( ct = nfct_new()) == NULL ) return errlog( ERR_CRITICAL, "nfct_new\n" );
    
    /* set protocol info */
    nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
    nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, (u_int8_t) protocol );
    nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
    nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, (u_int8_t) protocol );

    in_addr_t cli = inet_addr( c_client_addr );
    in_addr_t srv = inet_addr( c_server_addr );

    /* set the client side (orig) info */
    nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, cli);
    nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, htons((u_short)c_client_port) );
    nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, srv );
    nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, htons((u_short)c_server_port) );
            
    /* destroy the conntrack entry */
    _nfconntrack_query(ct, NFCT_Q_DESTROY);

    if ( ct != NULL )
        nfct_destroy( ct );
    
    return 0;
}

/**
 * Delete the specified conntrack entry
 */
int netcap_nfconntrack_del_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, int ignore_noent )
{
    struct nf_conntrack* ct = NULL;

    int _critical_section() {
        /* assuming IPV4 */
        nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
        nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, tuple->protocol );
        nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
        nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, tuple->protocol );
            
        /* set the source information */
        nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, tuple->src_address );
        nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, tuple->src_port );

        /* Set the destination information */
        nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, tuple->dst_address );
        nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, tuple->dst_port );

        // netcap_nfconntrack_print_entry( 10, ct );

        /* Actually make the query */
        errno = 0;
        if ( nfct_query( query_handle, NFCT_Q_DESTROY, ct ) < 0 ) {

            /* Report the error if the error is not EEXIST or always if ignore_exists is not set */
            if (( errno != ENOENT ) || ( ignore_noent == 0 ))
                return perrlog( "nfct_query" );

            debug( 4, "Unable to delete conntrack entry.  Conntrack entry does not exist.\n" );
        } 
        
        return 0;
    }

    if ( tuple == NULL ) return errlogargs();
    
    if (( ct = nfct_new()) == NULL )
        return errlog( ERR_CRITICAL, "nfct_new\n" );

    int ret;
    if ( pthread_mutex_lock( &query_mutex ) < 0 ) {
        if ( ct != NULL ) nfct_destroy( ct );
        return perrlog( "pthread_mutex_lock" );
    }

    ret = _critical_section();

    if ( ct != NULL )
        nfct_destroy( ct );

    if ( pthread_mutex_unlock( &query_mutex ) < 0 ) {
        return perrlog( "pthread_mutex_unlock" );
    }

    if ( ret < 0 )
        return errlog( ERR_CRITICAL, "_critical_section\n" );

    return 0;
}

/**
 * Dump all of conntrack
 */
list_t* netcap_nfconntrack_dump( struct nf_conntrack** array, int limit )
{
    int ret;

    int _critical_section() {
        u_int32_t family = AF_INET;
        if ( nfct_query( dump_handle, NFCT_Q_DUMP, &family ) < 0 ) {
            return perrlog( "nfct_query");
        }
        return 0;
    }

    list_t* list = list_create(0);
    if ( list == NULL )
        return perrlog_null("list_create");

    if ( pthread_mutex_lock( &dump_mutex ) < 0 ) {
        list_raze( list );
        return perrlog_null( "pthread_mutex_lock" );
    }

    dump_list = list;
    ret = _critical_section();
    dump_list = NULL;

    if ( pthread_mutex_unlock( &dump_mutex ) < 0 ) {
        list_raze( list );
        return perrlog_null( "pthread_mutex_unlock" );
    }

    if ( ret < 0 ) {
        list_raze( list );
        return errlog_null( ERR_CRITICAL, "_critical_section\n" );
    }

    return list;
}

/**
 * This is the callback used to query things about the conntrack
 */
static int _nfconntrack_tuple_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data )
{
    // this is never actually used
    errlog( ERR_WARNING, "_nf_check_tuple_callback() called\n" );

    if ( conntrack == NULL )
        return errlogargs();

    debug( 6, "Found a conntrack entry 0x%016"PRIxPTR", matching on first\n", (uintptr_t) conntrack );

    /* assuming it is the correct one for now, and assuming this is a get call */
    query_result = nfct_clone( conntrack );

    /* match on the first */
    return NFCT_CB_STOP;
}

/**
 * This is the callback used to dump all of conntrack
 */
static int _nfconntrack_dump_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data )
{
    list_t* list;
    if (( list = dump_list ) == NULL )
        return errlog( ERR_CRITICAL, "null args\n" );

    /**
     * Ignore sessions from 127.0.0.1 to 127.0.0.1
     */
    u_int32_t client = nfct_get_attr_u32( conntrack, ATTR_ORIG_IPV4_SRC );
    u_int32_t server = nfct_get_attr_u32( conntrack, ATTR_ORIG_IPV4_DST );
    if ( client == 0x0100007f && server == 0x0100007f ) 
        return NFCT_CB_CONTINUE;
    
    struct nf_conntrack* ct = nfct_clone(conntrack);
    list_add_tail( list, ct );
    
    return NFCT_CB_CONTINUE;
}

/**
 * Run the specific conntrack query
 */
static int _nfconntrack_query( struct nf_conntrack* ct, const enum nf_conntrack_query query )
{
    int _critical_section() {
        /* Actually make the query */
        errno = 0;
        if ( nfct_query( query_handle, query, ct ) < 0 ) {
            // if the conntrack entry doesn't exist - do not print error
            if ( errno == ENOENT )
                return -1;
            return errlog( ERR_WARNING, "nfct_query(%i): %s\n", query, strerror(errno) );
        }
        
        return 0;
    }

    if ( pthread_mutex_lock( &query_mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    int ret = _critical_section();

    if ( pthread_mutex_unlock( &query_mutex ) < 0 ) {
        return perrlog( "pthread_mutex_unlock" );
    }

    return ret;
}

#if 0
static void _nfconntrack_print_entry( int level, struct nf_conntrack *ct )
{
    if ( ct == NULL ) return (void)errlog( ERR_CRITICAL, "Invalid arguments\n" );

    if ( debug_get_mylevel() < level ) return;

    debug( level,"ATTR_ORIG_L3PROTO        = %d\n",nfct_get_attr_u8( ct, ATTR_ORIG_L3PROTO ) );
    debug( level,"ATTR_ORIG_L4PROTO        = %d\n",nfct_get_attr_u8( ct, ATTR_ORIG_L4PROTO ) );
    debug( level,"ATTR_REPL_L3PROTO        = %d\n",nfct_get_attr_u8( ct, ATTR_REPL_L3PROTO ) );
    debug( level,"ATTR_REPL_L4PROTO        = %d\n",nfct_get_attr_u8( ct, ATTR_REPL_L4PROTO ) );

    debug( level,"ATTR_ORIG_IPV4_SRC        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_ORIG_IPV4_SRC)));
    debug( level,"ATTR_ORIG_IPV4_DST        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_ORIG_IPV4_DST)));
    debug( level,"ATTR_REPL_IPV4_SRC        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_REPL_IPV4_SRC)));
    debug( level,"ATTR_REPL_IPV4_DST        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_REPL_IPV4_DST)));
    /* debug( level,"ATTR_ORIG_IPV6_SRC        = %d\n",nfct_get_attr_u128 (ct, ATTR_ORIG_IPV6_SRC)); */
    /* debug( level,"ATTR_ORIG_IPV6_DST        = %d\n",nfct_get_attr_u128 (ct, ATTR_ORIG_IPV6_DST)); */
    /* debug( level,"ATTR_REPL_IPV6_SRC        = %d\n",nfct_get_attr_u128 (ct, ATTR_REPL_IPV6_SRC)); */
    /* debug( level,"ATTR_REPL_IPV6_DST        = %d\n",nfct_get_attr_u128 (ct, ATTR_REPL_IPV6_DST)); */
    debug( level,"ATTR_ORIG_PORT_SRC        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_ORIG_PORT_SRC)));
    debug( level,"ATTR_ORIG_PORT_DST        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_ORIG_PORT_DST)));
    debug( level,"ATTR_REPL_PORT_SRC        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_REPL_PORT_SRC)));
    debug( level,"ATTR_REPL_PORT_DST        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_REPL_PORT_DST)));
    /* debug( level,"ATTR_ICMP_TYPE            = %d\n",nfct_get_attr_u8   (ct, ATTR_ICMP_TYPE)); */
    /* debug( level,"ATTR_ICMP_CODE            = %d\n",nfct_get_attr_u8   (ct, ATTR_ICMP_CODE)); */
    /* debug( level,"ATTR_ICMP_ID              = %d\n",nfct_get_attr_u16  (ct, ATTR_ICMP_ID)); */
    /* debug( level,"ATTR_ORIG_L3PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_ORIG_L3PROTO)); */
    /* debug( level,"ATTR_REPL_L3PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_REPL_L3PROTO)); */
    /* debug( level,"ATTR_ORIG_L4PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_ORIG_L4PROTO)); */
    /* debug( level,"ATTR_REPL_L4PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_REPL_L4PROTO)); */
    /* debug( level,"ATTR_TCP_STATE            = %d\n",nfct_get_attr_u8   (ct, ATTR_TCP_STATE)); */
    /* debug( level,"ATTR_SNAT_IPV4            = %d\n",nfct_get_attr_u32  (ct, ATTR_SNAT_IPV4)); */
    /* debug( level,"ATTR_DNAT_IPV4            = %d\n",nfct_get_attr_u32  (ct, ATTR_DNAT_IPV4)); */
    /* debug( level,"ATTR_SNAT_PORT            = %d\n",nfct_get_attr_u16  (ct, ATTR_SNAT_PORT)); */
    /* debug( level,"ATTR_DNAT_PORT            = %d\n",nfct_get_attr_u16  (ct, ATTR_DNAT_PORT)); */
    /* debug( level,"ATTR_TIMEOUT              = %d\n",nfct_get_attr_u32  (ct, ATTR_TIMEOUT)); */
    debug( level,"ATTR_MARK                 = %d\n",nfct_get_attr_u32  (ct, ATTR_MARK)); 
    /* debug( level,"ATTR_ORIG_COUNTER_PACKETS = %d\n",nfct_get_attr_u32  (ct, ATTR_ORIG_COUNTER_PACKETS)); */
    /* debug( level,"ATTR_REPL_COUNTER_PACKETS = %d\n",nfct_get_attr_u32  (ct, ATTR_REPL_COUNTER_PACKETS)); */
    /* debug( level,"ATTR_ORIG_COUNTER_BYTES   = %d\n",nfct_get_attr_u32  (ct, ATTR_ORIG_COUNTER_BYTES)); */
    /* debug( level,"ATTR_REPL_COUNTER_BYTES   = %d\n",nfct_get_attr_u32  (ct, ATTR_REPL_COUNTER_BYTES)); */
    /* debug( level,"ATTR_USE                  = %d\n",nfct_get_attr_u32  (ct, ATTR_USE)); */

    int status = nfct_get_attr_u32  (ct, ATTR_STATUS);
    debug( level,"STATUS:IPS_EXPECTED = %d\n",	  status&IPS_EXPECTED?1:0 );
    debug( level,"STATUS:IPS_SEEN_REPLY = %d\n",   status&IPS_SEEN_REPLY?1:0 );
    debug( level,"STATUS:IPS_ASSURED = %d\n",	  status&IPS_ASSURED?1:0 );
    debug( level,"STATUS:IPS_CONFIRMED = %d\n",	  status&IPS_CONFIRMED?1:0 );
    debug( level,"STATUS:IPS_SRC_NAT = %d\n",	  status&IPS_SRC_NAT?1:0 );
    debug( level,"STATUS:IPS_DST_NAT = %d\n",	  status&IPS_DST_NAT?1:0 );
    debug( level,"STATUS:IPS_SEQ_ADJUST = %d\n",   status&IPS_SEQ_ADJUST?1:0 );
    debug( level,"STATUS:IPS_SRC_NAT_DONE = %d\n", status&IPS_SRC_NAT_DONE?1:0 );
    debug( level,"STATUS:IPS_DST_NAT_DONE = %d\n", status&IPS_DST_NAT_DONE?1:0 );
    debug( level,"STATUS:IPS_DYING = %d\n",	  status&IPS_DYING?1:0 );
    debug( level,"STATUS:IPS_FIXED_TIMEOUT = %d\n",status&IPS_FIXED_TIMEOUT?1:0);
}	
#endif
