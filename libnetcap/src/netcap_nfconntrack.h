/**
 * $Id: netcap_nfconntrack.h,v 1.00 2013/08/08 11:34:10 dmorris Exp $
 */
#ifndef __NETCAP_NFCONNTRACK_H
#define __NETCAP_NFCONNTRACK_H

#include <libnetfilter_conntrack/libnetfilter_conntrack.h> 

/* 20 minutes is a long session timeout */
#define NETCAP_NFCONNTRACK_MAX_TIMEOUT 60 * 20

typedef enum
{
    NFCONNTRACK_DIRECTION_ORIG,
    NFCONNTRACK_DIRECTION_REPLY
} netcap_nfconntrack_direction_t;

typedef struct
{
    u_int8_t protocol;
    u_int32_t src_address;
    u_int16_t src_port;
    u_int32_t dst_address;
    u_int16_t dst_port;
} netcap_nfconntrack_ipv4_tuple_t;

/**
 * Initialize the netfilter conntrack library.
 */
int  netcap_nfconntrack_init( int num_handlers );

/**
 * Cleanup all of the filedescriptors associated with netfilter
 */
int  netcap_nfconntrack_cleanup( void );

/**
 * Get the file descriptor associated with the netfilter conntrack.
 */
int  netcap_nfconntrack_get_fd( void );

/**
 * Dump all of the expects
 */
int  netcap_nfconntrack_dump_expects( void );

void netcap_nfconntrack_print_entry( int level, struct nf_conntrack *ct );

/**
 * Retrieve a single conntrack entry using a tuple
 * @param tuple The Tuple to lookup
 * @param direction The direction the tuple should match.
 */
struct nf_conntrack *netcap_nfconntrack_get_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, 
                                                         netcap_nfconntrack_direction_t direction );

/**
 * Retrieve a single conntrack entry using its id.
 */
struct nfct_conntrack *netcap_nfconntrack_get_entry_id( u_int32_t id );

/**
 * Create a conntrack entry.
 * @param original The tuple for the original direction.
 * @param reply The tuple for the reply direction.
 * @param timeout Timeout to set for the conntrack entry.
 * @param ignore_exists Do not report if the conntrack entry already exist.
 */
int netcap_nfconntrack_create_entry( netcap_nfconntrack_ipv4_tuple_t* original, 
                                     netcap_nfconntrack_ipv4_tuple_t* reply, int timeout,
                                     int ignore_exists );

/**
 * Delete a conntrack entry based on its tuple.
 * @param ignore_noent Do not report if the conntrack entry doesn't exist.
 */
int netcap_nfconntrack_del_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, 
                                        netcap_nfconntrack_direction_t direction,
                                        int ignore_noent );

#endif // #ifndef __NETCAP_NFCONNTRACK_H

