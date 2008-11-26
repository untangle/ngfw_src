/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ip.h 8515 2007-01-03 00:13:24Z rbscott $
 */

#include <pthread.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <stdlib.h>

#include <libnetfilter_queue/libnetfilter_queue.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/unet.h>


#include "netcap_nfconntrack.h"

static struct
{
    pthread_key_t tls_key;    
    struct nfct_handle *handle;
    struct nfct_handle *exp_handle;
    pthread_mutex_t mutex;
} _netcap_nfconntrack =
{
    .handle = NULL,
    .exp_handle = NULL,
    .tls_key = -1,
    .mutex = PTHREAD_MUTEX_INITIALIZER
};


/* This is the callback used to query things about the conntrack */
static int _nf_check_tuple_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack,
                                     void * user_data );

static int _nf_check_expect_callback( enum nf_conntrack_msg_type type, struct nf_expect *exp, 
                                      void *user_data );

/**
 * Initialize the netfilter conntrack library.
 */
int  netcap_nfconntrack_init( void )
{
    if ( _netcap_nfconntrack.handle != NULL ) return errlog( ERR_CRITICAL, "Already initialized" );

    debug( 2, "Initializing netcap_nfconntrack.\n" );

    if ( pthread_key_create( &_netcap_nfconntrack.tls_key, NULL ) < 0 ) return perrlog( "pthread_key_create" );
    
    if (( _netcap_nfconntrack.handle = nfct_open( CONNTRACK, 0 )) == NULL ) return perrlog( "nfct_open" );
    if (( _netcap_nfconntrack.exp_handle = nfct_open( EXPECT, 0 )) == NULL ) return perrlog( "nfct_open" );

    if ( nfct_callback_register( _netcap_nfconntrack.handle, NFCT_T_ALL, 
                                 _nf_check_tuple_callback, NULL ) < 0 ) {
        return perrlog( "nfct_callback_register" );
    }
    
    if ( nfexp_callback_register( _netcap_nfconntrack.exp_handle, NFCT_T_ALL,
                                  _nf_check_expect_callback, NULL ) < 0 ) {
        return perrlog( "nfexp_callback_register" );
    }
    
    debug( 2, "Initialization completed.\n" );
    
    return 0;
}

/**
 * Cleanup all of the filedescriptors associated with the conntrack handle.
 */
int  netcap_nfconntrack_cleanup( void )
{
    debug( 2, "Cleanup\n" );

    if (( _netcap_nfconntrack.handle != NULL ) && ( nfct_close( _netcap_nfconntrack.handle ))) perrlog( "nfct_close" );

    if (( _netcap_nfconntrack.exp_handle != NULL ) && ( nfct_close( _netcap_nfconntrack.exp_handle ))) {
        perrlog( "nfct_close" );
    }
    
    _netcap_nfconntrack.handle = NULL;
    _netcap_nfconntrack.exp_handle = NULL;

    return 0;
}

/**
 * Get the file descriptor associated with the netfilter conntrack.
 */
int  netcap_nfconntrack_get_fd( void )
{
    int fd;
    
    if ( _netcap_nfconntrack.handle == NULL ) return errlog( ERR_CRITICAL, "Uninitialized\n" );

    if (( fd = nfct_fd( _netcap_nfconntrack.handle )) < 0 ) return perrlog( "nfct_fd" );

    return fd;
}

/**
 * Dump all of the expects
 */
int  netcap_nfconntrack_dump_expects( void )
{
    int family = AF_INET;
    if ( _netcap_nfconntrack.handle == NULL ) return errlog( ERR_CRITICAL, "Uninitialized\n" );
    
    if ( nfexp_query( _netcap_nfconntrack.exp_handle, NFCT_Q_DUMP, &family ) < 0 ) {
        return perrlog( "nfexp_query" );
    }

    return 0;
}


/**
 * Retrieve a single conntrack entry using a tuple
 * @param tuple The Tuple to lookup
 * @param direction The direction the tuple should match.
 */
struct nf_conntrack *netcap_nfconntrack_get_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, 
                                                  netcap_nfconntrack_direction_t direction )
{
    struct nf_conntrack* ct = NULL;
    struct nf_conntrack* ct_result = NULL;

    int _critical_section() {
        if ( direction == NFCONNTRACK_DIRECTION_ORIG ) {
            debug( 4, "Checking for original direction\n" );
            /* assuming IPV4 */
            nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, tuple->protocol );

            nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, tuple->protocol );
            
            /* set the source information */
            nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, tuple->src_address );
            nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, tuple->src_port );

            /* Set the destination information */
            nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, tuple->dst_address );
            nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, tuple->dst_port );
        } else {
            /* There is a bug that prevents this from always working */
            // return errlog( ERR_CRITICAL, "not working\n" );
            debug( 4, "Checking for reply direction\n" );

            /* assuming IPV4 */
            nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, tuple->protocol );
            
            /* set the source information */
            nfct_set_attr_u32( ct, ATTR_REPL_IPV4_SRC, tuple->src_address );
            nfct_set_attr_u16( ct, ATTR_REPL_PORT_SRC, tuple->src_port );

            /* Set the destination information */
            nfct_set_attr_u32( ct, ATTR_REPL_IPV4_DST, tuple->dst_address );
            nfct_set_attr_u16( ct, ATTR_REPL_PORT_DST, tuple->dst_port );
        }

        netcap_nfconntrack_print_entry( 10, ct );

	
        /* Actually make the query */
        errno = 0;
        errlog( ERR_WARNING, "Errno: %d\n", errno );
        if ( nfct_query( _netcap_nfconntrack.handle, NFCT_Q_GET, ct ) < 0 ) {
            errlog( ERR_WARNING, "Errno: %d\n", errno );
            return perrlog( "nfct_query" );
        }
        
        return 0;
    }

    if ( tuple == NULL ) return errlogargs_null();
    
    if ( _netcap_nfconntrack.handle == NULL ) return errlog_null( ERR_CRITICAL, "Unitialized\n" );

    if (( direction != NFCONNTRACK_DIRECTION_ORIG ) && ( direction != NFCONNTRACK_DIRECTION_REPLY )) {
        return errlog_null( ERR_CRITICAL, "Invalid direction %d\n", direction );
    }

    /* assume the fields are zerod */
    if (( ct = nfct_new()) == NULL ) return errlog_null( ERR_CRITICAL, "nfct_new\n" );

    int ret;
    /* save a pointer to ct_result to TLS */
    pthread_setspecific( _netcap_nfconntrack.tls_key, &ct_result );
    ret = _critical_section();
    pthread_setspecific( _netcap_nfconntrack.tls_key, NULL );

    if ( ct != NULL ) nfct_destroy( ct );

    if ( ret < 0 ) return errlog_null( ERR_CRITICAL, "_critical_section\n" );

    /* This will just return NULL if it doesn't find one */
    return ct_result;
}

/**
 * Retrieve a single conntrack entry using its id.
 */
struct nfct_conntrack *netcap_nfconntrack_get_entry_id( u_int32_t id )
{
    struct nfct_tuple tuple;

    bzero( &tuple, sizeof( tuple ));
    
    return errlog_null ( ERR_CRITICAL, "implement me\n" );
}

/**
 * Create a conntrack entry.
 * @param original The tuple for the original direction.
 * @param reply The tuple for the reply direction.
 * @param timeout Timeout to set for the conntrack entry.
 * @param ignore_exists Do not report if the conntrack entry already exists.
 */
int netcap_nfconntrack_create_entry( netcap_nfconntrack_ipv4_tuple_t* original, 
                                     netcap_nfconntrack_ipv4_tuple_t* reply, int timeout,
                                     int ignore_exists )
{
    struct nf_conntrack* ct = NULL;

    int _critical_section() {
        int status = IPS_CONFIRMED | IPS_SRC_NAT_DONE | IPS_DST_NAT_DONE | IPS_SEEN_REPLY;

        /* assuming IPV4 */
        nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
        nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, reply->protocol );
        
        nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
        nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, reply->protocol );
        
        /* set the source information */
        nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, original->src_address );
        nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, original->src_port );
        
        /* Set the destination information */
        nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, original->dst_address );
        nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, original->dst_port );

        /* set the source information */
        nfct_set_attr_u32( ct, ATTR_REPL_IPV4_SRC, reply->src_address );
        nfct_set_attr_u16( ct, ATTR_REPL_PORT_SRC, reply->src_port );
        
        /* Set the destination information */
        nfct_set_attr_u32( ct, ATTR_REPL_IPV4_DST, reply->dst_address );
        nfct_set_attr_u16( ct, ATTR_REPL_PORT_DST, reply->dst_port );

        /* setup the NAT flags */
        if ( original->src_address != reply->dst_address ) {
            status |= IPS_SRC_NAT;
        }

        if ( original->src_port != reply->dst_port ) {
            status |= IPS_SRC_NAT;
        }

        if ( original->dst_address != reply->src_address ) {
            status |= IPS_DST_NAT;
        }

        if ( original->dst_port != reply->src_port ) {
            status |= IPS_DST_NAT;
        }

        nfct_set_attr_u32( ct, ATTR_STATUS, status );
        nfct_set_attr_u32( ct, ATTR_TIMEOUT, timeout );

        debug( 10, "Creating a new conntrack entry\n" );
        netcap_nfconntrack_print_entry( 10, ct );
        
        /* Actually make the query */
        errno = 0;
        if ( nfct_query( _netcap_nfconntrack.handle, NFCT_Q_CREATE, ct ) < 0 ) {
            /* Report the error if the error is not EEXIST or always if ignore_exists is not set */
            if (( errno != EEXIST ) || ( ignore_exists == 0 )) return perrlog( "nfct_query" );
        }
                
        return 0;
    }

    if (( original == NULL ) || ( reply == NULL )) return errlogargs();

    if (( timeout < 0 ) || ( timeout > NETCAP_NFCONNTRACK_MAX_TIMEOUT )) {
        return errlog( ERR_CRITICAL, "Invalid timeout %d\n", timeout );
    }
    
    if ( _netcap_nfconntrack.handle == NULL ) return errlog( ERR_CRITICAL, "Unitialized\n" );

    /* assume the fields are zerod */
    if (( ct = nfct_new()) == NULL ) return errlog( ERR_CRITICAL, "nfct_new\n" );

    int ret;
    ret = _critical_section();

    if ( ct != NULL ) nfct_destroy( ct );

    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_critical_section\n" );

    return 0;
}

/**
 * Retrieve a single conntrack entry using a tuple
 * @param tuple The Tuple to lookup
 * @param direction The direction the tuple should match.
 */
int netcap_nfconntrack_del_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, 
                                        netcap_nfconntrack_direction_t direction,
                                        int ignore_noent )
{
    struct nf_conntrack* ct = NULL;

    int _critical_section() {
        if ( direction == NFCONNTRACK_DIRECTION_ORIG ) {
            debug( 4, "Checking for original direction\n" );
            /* assuming IPV4 */
            nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, tuple->protocol );

            nfct_set_attr_u8( ct, ATTR_ORIG_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_ORIG_L4PROTO, tuple->protocol );
            
            /* set the source information */
            nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_SRC, tuple->src_address );
            nfct_set_attr_u16( ct, ATTR_ORIG_PORT_SRC, tuple->src_port );

            /* Set the destination information */
            nfct_set_attr_u32( ct, ATTR_ORIG_IPV4_DST, tuple->dst_address );
            nfct_set_attr_u16( ct, ATTR_ORIG_PORT_DST, tuple->dst_port );
        } else {
#if 1
            /* There is a bug that prevents this from always working */
            return errlog( ERR_CRITICAL, "not working\n" );
#else
#error "THE REPLY DIRECTORY IS BROKEN IN CONNTRACK"
            debug( 4, "Checking for reply direction\n" );

            /* assuming IPV4 */
            nfct_set_attr_u8( ct, ATTR_REPL_L3PROTO, AF_INET );
            nfct_set_attr_u8( ct, ATTR_REPL_L4PROTO, tuple->protocol );
            
            /* set the source information */
            nfct_set_attr_u32( ct, ATTR_REPL_IPV4_SRC, tuple->src_address );
            nfct_set_attr_u16( ct, ATTR_REPL_PORT_SRC, tuple->src_port );

            /* Set the destination information */
            nfct_set_attr_u32( ct, ATTR_REPL_IPV4_DST, tuple->dst_address );
            nfct_set_attr_u16( ct, ATTR_REPL_PORT_DST, tuple->dst_port );
#endif
        }

        netcap_nfconntrack_print_entry( 10, ct );

        /* Actually make the query */
        errno = 0;
        if ( nfct_query( _netcap_nfconntrack.handle, NFCT_Q_DESTROY, ct ) < 0 ) {
            /* Report the error if the error is not EEXIST or always if ignore_exists is not set */
            if (( errno != ENOENT ) || ( ignore_noent == 0 )) return perrlog( "nfct_query" );

            debug( 4, "Unable to delete conntrack entry.  Conntrack entry does not exist.\n" );
        }
        
        return 0;
    }

    if ( tuple == NULL ) return errlogargs();
    
    if ( _netcap_nfconntrack.handle == NULL ) return errlog( ERR_CRITICAL, "Unitialized\n" );

    if (( direction != NFCONNTRACK_DIRECTION_ORIG ) && ( direction != NFCONNTRACK_DIRECTION_REPLY )) {
        return errlog( ERR_CRITICAL, "Invalid direction %d\n", direction );
    }

    /* assume the fields are zerod */
    if (( ct = nfct_new()) == NULL ) return errlog( ERR_CRITICAL, "nfct_new\n" );

    int ret;
    if ( pthread_mutex_lock( &_netcap_nfconntrack.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }
    ret = _critical_section();
    if ( pthread_mutex_unlock( &_netcap_nfconntrack.mutex ) < 0 ) {
        return perrlog( "pthread_mutex_unlock" );
    }
    if ( ct != NULL ) nfct_destroy( ct );

    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_critical_section\n" );

    return 0;
}

void netcap_nfconntrack_print_entry( int level, struct nf_conntrack *ct )
{
    if ( ct == NULL ) return (void)errlog( ERR_CRITICAL, "Invalid arguments\n" );

    if ( debug_get_mylevel() < level ) return;

    debug( level,"ATTR_ORIG_IPV4_SRC        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_ORIG_IPV4_SRC)));
    debug( level,"ATTR_ORIG_IPV4_DST        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_ORIG_IPV4_DST)));
    debug( level,"ATTR_REPL_IPV4_SRC        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_REPL_IPV4_SRC)));
    debug( level,"ATTR_REPL_IPV4_DST        = %s\n",unet_next_inet_ntoa(nfct_get_attr_u32  (ct, ATTR_REPL_IPV4_DST)));
/*     debug( level,"ATTR_ORIG_IPV6_SRC        = %d\n",nfct_get_attr_u128 (ct, ATTR_ORIG_IPV6_SRC)); */
/*     debug( level,"ATTR_ORIG_IPV6_DST        = %d\n",nfct_get_attr_u128 (ct, ATTR_ORIG_IPV6_DST)); */
/*     debug( level,"ATTR_REPL_IPV6_SRC        = %d\n",nfct_get_attr_u128 (ct, ATTR_REPL_IPV6_SRC)); */
/*     debug( level,"ATTR_REPL_IPV6_DST        = %d\n",nfct_get_attr_u128 (ct, ATTR_REPL_IPV6_DST)); */
    debug( level,"ATTR_ORIG_PORT_SRC        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_ORIG_PORT_SRC)));
    debug( level,"ATTR_ORIG_PORT_DST        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_ORIG_PORT_DST)));
    debug( level,"ATTR_REPL_PORT_SRC        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_REPL_PORT_SRC)));
    debug( level,"ATTR_REPL_PORT_DST        = %d\n",ntohs(nfct_get_attr_u16  (ct, ATTR_REPL_PORT_DST)));
/*     debug( level,"ATTR_ICMP_TYPE            = %d\n",nfct_get_attr_u8   (ct, ATTR_ICMP_TYPE)); */
/*     debug( level,"ATTR_ICMP_CODE            = %d\n",nfct_get_attr_u8   (ct, ATTR_ICMP_CODE)); */
/*     debug( level,"ATTR_ICMP_ID              = %d\n",nfct_get_attr_u16  (ct, ATTR_ICMP_ID)); */
/*     debug( level,"ATTR_ORIG_L3PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_ORIG_L3PROTO)); */
/*     debug( level,"ATTR_REPL_L3PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_REPL_L3PROTO)); */
/*     debug( level,"ATTR_ORIG_L4PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_ORIG_L4PROTO)); */
/*     debug( level,"ATTR_REPL_L4PROTO         = %d\n",nfct_get_attr_u8   (ct, ATTR_REPL_L4PROTO)); */
/*     debug( level,"ATTR_TCP_STATE            = %d\n",nfct_get_attr_u8   (ct, ATTR_TCP_STATE)); */
/*     debug( level,"ATTR_SNAT_IPV4            = %d\n",nfct_get_attr_u32  (ct, ATTR_SNAT_IPV4)); */
/*     debug( level,"ATTR_DNAT_IPV4            = %d\n",nfct_get_attr_u32  (ct, ATTR_DNAT_IPV4)); */
/*     debug( level,"ATTR_SNAT_PORT            = %d\n",nfct_get_attr_u16  (ct, ATTR_SNAT_PORT)); */
/*     debug( level,"ATTR_DNAT_PORT            = %d\n",nfct_get_attr_u16  (ct, ATTR_DNAT_PORT)); */
/*     debug( level,"ATTR_TIMEOUT              = %d\n",nfct_get_attr_u32  (ct, ATTR_TIMEOUT));  */
/*     debug( level,"ATTR_MARK                 = %d\n",nfct_get_attr_u32  (ct, ATTR_MARK)); */
/*     debug( level,"ATTR_ORIG_COUNTER_PACKETS = %d\n",nfct_get_attr_u32  (ct, ATTR_ORIG_COUNTER_PACKETS)); */
/*     debug( level,"ATTR_REPL_COUNTER_PACKETS = %d\n",nfct_get_attr_u32  (ct, ATTR_REPL_COUNTER_PACKETS)); */
/*     debug( level,"ATTR_ORIG_COUNTER_BYTES   = %d\n",nfct_get_attr_u32  (ct, ATTR_ORIG_COUNTER_BYTES));  */
/*     debug( level,"ATTR_REPL_COUNTER_BYTES   = %d\n",nfct_get_attr_u32  (ct, ATTR_REPL_COUNTER_BYTES)); */
/*     debug( level,"ATTR_USE                  = %d\n",nfct_get_attr_u32  (ct, ATTR_USE)); */
/*     //printf("ATTR_ID                   = %d\n",nfct_get_attr_u32  (ct, ATTR_ID)); */
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

void netcap_nfconntrack_print_expect( int level, struct nf_expect* expect )
{
    if ( expect == NULL ) return (void)errlog( ERR_CRITICAL, "Invalid arguments\n" );

    struct nf_conntrack *ct;
    char buf[1024];
    debug( level, "MASTER\n" );
    
    nfexp_snprintf( buf, sizeof( buf ), expect, NFCT_T_UNKNOWN, NFCT_O_DEFAULT, 0 );
    
    debug( level, "nfct_sprintf_expect: %s\n", buf );
    
    if (( ct = (struct nf_conntrack *)nfexp_get_attr( expect, ATTR_EXP_MASTER )) != NULL ) {
        nfct_snprintf( buf, sizeof( buf ), ct, NFCT_T_UNKNOWN, NFCT_O_DEFAULT, 0 );
        debug( level, "expected: %s\n", buf );
        
        // netcap_nfconntrack_print_entry( level, ct );
    }

    debug( level, "EXPECTED\n" );
    if (( ct = (struct nf_conntrack *)nfexp_get_attr( expect, ATTR_EXP_EXPECTED )) != NULL ) {
        nfct_snprintf( buf, sizeof( buf ), ct, NFCT_T_UNKNOWN, NFCT_O_DEFAULT, 0 );
        debug( level, "expected: %s\n", buf );
        
        // netcap_nfconntrack_print_entry( level, ct );
    }

    debug( level, "MASK\n" );
    if (( ct = (struct nf_conntrack *)nfexp_get_attr( expect, ATTR_EXP_MASK )) != NULL ) {
        nfct_snprintf( buf, sizeof( buf ), ct, NFCT_T_UNKNOWN, NFCT_O_DEFAULT, 0 );
        debug( level, "mask: %s\n", buf );
        
        // netcap_nfconntrack_print_entry( level, ct );
    }

    debug( level, "TIMEOUT                  = %d\n", nfexp_get_attr_u32( expect, ATTR_EXP_TIMEOUT ));
} 

static int _nf_check_tuple_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *conntrack,
                                     void * user_data )
{
    if ( conntrack == NULL ) return errlogargs();

    debug( 6, "Found a conntrack entry %#010x, matching on first\n", conntrack );
    struct nf_conntrack **ct_result;
    if (( ct_result = pthread_getspecific( _netcap_nfconntrack.tls_key  )) == NULL ) {
        return errlog( ERR_CRITICAL, "null args\n" );
    }
    /* assuming it is the correct one for now, and assuming this is a get call */
    *ct_result = nfct_clone( conntrack );

    /* match on the first */
    return NFCT_CB_STOP;
}


static int _nf_check_expect_callback( enum nf_conntrack_msg_type type, struct nf_expect *exp, 
                                      void *user_data )
{
    if ( exp == NULL ) return errlogargs();
    netcap_nfconntrack_print_expect( 9, exp );
    return NFCT_CB_CONTINUE;
}
