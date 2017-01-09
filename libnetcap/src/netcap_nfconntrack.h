/**
 * $Id$
 */
#ifndef __NETCAP_NFCONNTRACK_H
#define __NETCAP_NFCONNTRACK_H

#include <libnetfilter_conntrack/libnetfilter_conntrack.h> 
#include <libnetfilter_queue/libnetfilter_queue.h>

/* 20 minutes is a long session timeout */
#define NETCAP_NFCONNTRACK_MAX_TIMEOUT 60 * 20

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
int  netcap_nfconntrack_init();

/**
 * Cleanup all of the filedescriptors associated with netfilter
 */
int  netcap_nfconntrack_cleanup( void );

/**
 * Delete a conntrack entry based on its tuple.
 */
int netcap_nfconntrack_del_entry_tuple( netcap_nfconntrack_ipv4_tuple_t* tuple, int ignore_noent );

#endif // #ifndef __NETCAP_NFCONNTRACK_H

