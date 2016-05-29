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

/**
 * NFConntrack has an interesting bug where sometimes the delete will
 * take more than 9 seconds to return.  Previously all sessions
 * completions would block on one mutex that was waiting for the entry
 * to be deleted.  The deletion must be synchronous, so the call must
 * block.  To get around this we splitup the connection to conntrack
 * across N connections.  This way only 1 / N connections will ever
 * block if the box is extemely loaded and this condition occurs.
 */
typedef struct 
{
    pthread_mutex_t mutex;
    struct nfct_handle* handle;
    struct nfct_handle* dump_handle;
} _conntrack_handle_t;

static struct
{
    pthread_key_t tls_key;    
    pthread_mutex_t handle_mutex;
    int num_handles;
    int current_handle;
    _conntrack_handle_t* handles;
} _netcap_nfconntrack = {
    .tls_key = -1,
    .handle_mutex = PTHREAD_MUTEX_INITIALIZER,
    .num_handles = 0,
    .handles = NULL,
    .current_handle = 0
};

static int _nfconntrack_tuple_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data );
static int _nfconntrack_dump_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data );
static int _initialize_handle( _conntrack_handle_t* handler );
static int _nfconntrack_update_mark( struct nf_conntrack* ct );
static _conntrack_handle_t* _get_handle( void );

/**
 * Initialize the netfilter conntrack library.
 */
int  netcap_nfconntrack_init( int num_handles )
{
    if ( _netcap_nfconntrack.num_handles != 0 ) return errlog( ERR_CRITICAL, "Already initialized" );

    if ( num_handles <= 0 ) return errlog( ERR_CRITICAL, "Invalid number of handles. %d\n", num_handles );

    debug( 2, "Initializing netcap_nfconntrack with %d handle[s].\n", num_handles );

    if ( pthread_key_create( &_netcap_nfconntrack.tls_key, NULL ) < 0 ) return perrlog( "pthread_key_create" );
    
    _netcap_nfconntrack.num_handles = 0;
    if (( _netcap_nfconntrack.handles = calloc( num_handles, sizeof( *_netcap_nfconntrack.handles ))) == NULL ) {
        return errlogmalloc();
    }
    int c;
    for ( c = 0 ; c< num_handles ; c++ ) {
        if ( _initialize_handle( &_netcap_nfconntrack.handles[c] ) < 0 ) {
            return errlog( ERR_CRITICAL, "_initialize_handler\n" );
        }
    }

    _netcap_nfconntrack.num_handles = num_handles;
    
    debug( 2, "Initialization completed.\n" );
    return 0;
}

/**
 * Cleanup all of the filedescriptors associated with the conntrack handle.
 */
int  netcap_nfconntrack_cleanup( void )
{
    debug( 2, "Cleanup\n" );

    int c = 0;

    for ( c = 0 ; c < _netcap_nfconntrack.num_handles ; c++ ) {
        _conntrack_handle_t* handle = &_netcap_nfconntrack.handles[c];
        
        if (( handle->handle != NULL ) && ( nfct_close( handle->handle ))) {
            perrlog( "nfct_close" );
        }
        if (( handle->dump_handle != NULL ) && ( nfct_close( handle->dump_handle ))) {
            perrlog( "nfct_close" );
        }
        
        handle->handle = NULL;
        handle->dump_handle = NULL;
    }

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

    /* Packet is post-NAT, lookup in reverse */
    _nfconntrack_update_mark(ct);

    if ( ct != NULL ) nfct_destroy( ct );
    
    return 0;
}

/**
 * Delete the specified conntrack entry
 */
int netcap_nfconntrack_del_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, int ignore_noent )
{
    struct nf_conntrack* ct = NULL;
    _conntrack_handle_t* handle = NULL;

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
        if ( nfct_query( handle->handle, NFCT_Q_DESTROY, ct ) < 0 ) {

            /* Report the error if the error is not EEXIST or always if ignore_exists is not set */
            if (( errno != ENOENT ) || ( ignore_noent == 0 ))
                return perrlog( "nfct_query" );

            debug( 4, "Unable to delete conntrack entry.  Conntrack entry does not exist.\n" );
        } 
        
        return 0;
    }

    if ( tuple == NULL ) return errlogargs();
    
    if (( handle = _get_handle()) == NULL )
        return errlog( ERR_CRITICAL, "_get_handle\n" );
    if (( ct = nfct_new()) == NULL )
        return errlog( ERR_CRITICAL, "nfct_new\n" );

    int ret;
    if ( pthread_mutex_lock( &handle->mutex ) < 0 ) {
        if ( ct != NULL ) nfct_destroy( ct );
        return perrlog( "pthread_mutex_lock" );
    }
    ret = _critical_section();
    if ( ct != NULL )
        nfct_destroy( ct );
    if ( pthread_mutex_unlock( &handle->mutex ) < 0 ) {
        return perrlog( "pthread_mutex_unlock" );
    }

    if ( ret < 0 )
        return errlog( ERR_CRITICAL, "_critical_section\n" );

    return 0;
}

/**
 * Dump all of conntrack
 */
int netcap_nfconntrack_dump( struct nf_conntrack** array, int limit )
{
    _conntrack_handle_t* handle = NULL;
    int ret;

    int _critical_section() {
        u_int32_t family = AF_INET;
        if ( nfct_query(handle->dump_handle, NFCT_Q_DUMP, &family) < 0 ) {
            return perrlog( "nfct_query");
        }
        return 0;
    }

    if (( handle = _get_handle()) == NULL )
        return errlog( ERR_CRITICAL, "_get_handle\n" );

    list_t* list = list_create(0);
    if ( list == NULL )
        return perrlog("list_create");

    if ( pthread_mutex_lock( &handle->mutex ) < 0 ) {
        list_raze( list );
        return perrlog( "pthread_mutex_lock" );
    }

    pthread_setspecific( _netcap_nfconntrack.tls_key, list );
    ret = _critical_section();
    pthread_setspecific( _netcap_nfconntrack.tls_key, NULL );

    if ( pthread_mutex_unlock( &handle->mutex ) < 0 ) {
        list_raze( list );
        return perrlog( "pthread_mutex_unlock" );
    }

    if ( ret < 0 ) {
        list_raze( list );
        return errlog( ERR_CRITICAL, "_critical_section\n" );
    }

    struct nf_conntrack* entry;
    int count = 0;
    int i = 0;
    for ( ; i < limit ; i++ ) {
        if ( list_pop_head( list, (void**)&entry ) < 0 )
            break;
        array[i] = entry;
        count++;
        if ( list_length( list ) < 1 )
            break;
    }

    return i;
}

/**
 * Initialize a handle
 */
static int _initialize_handle( _conntrack_handle_t* handler )
{
    bzero( handler, sizeof( *handler ));
    
    if (( handler->handle = nfct_open( CONNTRACK, 0 )) == NULL )
        return perrlog( "nfct_open" );
    if (( handler->dump_handle = nfct_open( CONNTRACK, 0 )) == NULL )
        return perrlog( "nfct_open" );
    if ( nfct_callback_register( handler->handle, NFCT_T_ALL, _nfconntrack_tuple_callback, NULL ) < 0 )
        return perrlog( "nfct_callback_register" );
    if ( nfct_callback_register( handler->dump_handle, NFCT_T_ALL, _nfconntrack_dump_callback, NULL ) < 0 )
        return perrlog( "nfct_callback_register" );
    if ( pthread_mutex_init( &handler->mutex, NULL ) < 0 )
        return perrlog( "pthread_mutex_init" );

    return 0;
}

/**
 * Get an available conntrack handler
 */
static _conntrack_handle_t* _get_handle( void )
{
    if ( _netcap_nfconntrack.num_handles <= 0 ) {
        return errlog_null( ERR_CRITICAL, "conntrack is not initialized.\n" );
    }

    _conntrack_handle_t* _critical_section() {
        _netcap_nfconntrack.current_handle++;

        if (( _netcap_nfconntrack.current_handle < 0 ) ||
            ( _netcap_nfconntrack.current_handle >= _netcap_nfconntrack.num_handles )) {
            _netcap_nfconntrack.current_handle = 0;
        }

        return &_netcap_nfconntrack.handles[_netcap_nfconntrack.current_handle];
    }
    
    _conntrack_handle_t* handle = NULL;
    if ( pthread_mutex_lock( &_netcap_nfconntrack.handle_mutex ) < 0 ) {
        return perrlog_null( "pthread_mutex_lock" );
    }
    
    handle = _critical_section();
    if ( pthread_mutex_unlock( &_netcap_nfconntrack.handle_mutex ) < 0 ) {
        return perrlog_null( "pthread_mutex_unlock" );
    }
    
    if ( handle == NULL ) return errlog_null( ERR_CRITICAL, "_critical_section\n" );

    if ( handle->handle == NULL || handle->dump_handle == NULL ) {
        return errlog_null( ERR_CRITICAL, "Handle is not initialized.\n" );
    }

    return handle;
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
    struct nf_conntrack **ct_result;
    if (( ct_result = pthread_getspecific( _netcap_nfconntrack.tls_key  )) == NULL ) {
        return errlog( ERR_CRITICAL, "null args\n" );
    }
    /* assuming it is the correct one for now, and assuming this is a get call */
    *ct_result = nfct_clone( conntrack );

    /* match on the first */
    return NFCT_CB_STOP;
}

/**
 * This is the callback used to dump all of conntrack
 */
static int _nfconntrack_dump_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack, void * user_data )
{
    list_t* list;
    
    if (( list = pthread_getspecific( _netcap_nfconntrack.tls_key )) == NULL )
        return errlog( ERR_CRITICAL, "null args\n" );

    struct nf_conntrack* ct = nfct_clone(conntrack);
    list_add_tail( list, ct );
    
    return NFCT_CB_CONTINUE;
}

/**
 * Update the specified conntrack
 */
static int _nfconntrack_update_mark( struct nf_conntrack* ct )
{
    struct nf_conntrack* ct_result = NULL;
    _conntrack_handle_t* handle = NULL;

    int _critical_section() {

        // netcap_nfconntrack_print_entry( 10, ct );

        /* Actually make the query */
        errno = 0;
        if ( nfct_query( handle->handle, NFCT_Q_UPDATE, ct ) < 0 ) {
            return perrlog( "nfct_query" );
        }
        
        return 0;
    }

    if (( handle = _get_handle()) == NULL ) return errlog( ERR_CRITICAL, "_get_handle\n" );

    int ret;
    /* save a pointer to ct_result to TLS */
    if ( pthread_mutex_lock( &handle->mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    pthread_setspecific( _netcap_nfconntrack.tls_key, &ct_result );
    ret = _critical_section();
    pthread_setspecific( _netcap_nfconntrack.tls_key, NULL );

    if ( pthread_mutex_unlock( &handle->mutex ) < 0 ) {
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
