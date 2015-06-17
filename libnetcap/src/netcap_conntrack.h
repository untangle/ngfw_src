/**
 * $Id: netcap_conntrackd.h 37267 2014-02-26 23:42:19Z dmorris $
 */
#ifndef __NETCAP_CONNTRACKD_H_
#define __NETCAP_CONNTRACKD_H_

#include "libnetcap.h"

int   netcap_conntrack_init();
void* netcap_conntrack_listen ( void* arg );
void  netcap_conntrack_null_hook ( int type, long mark, long conntrack_id, long session_id, 
                                   int l3_proto, int l4_proto,
                                   long c_client_addr, long c_server_addr,
                                   int  c_client_port, int c_server_port,
                                   long s_client_addr, long s_server_addr,
                                   int  s_client_port, int s_server_port,
                                   int c2s_packets, int c2s_bytes,
                                   int s2c_packets, int s2c_bytes,
                                   long timestamp_start, long timestamp_stop );

#endif

