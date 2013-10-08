/**
 * $Id: netcap_pkt.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_PKT_
#define __NETCAP_PKT_

#include <netinet/in.h>

#include "libnetcap.h"

netcap_pkt_t*  netcap_pkt_malloc (void);
int            netcap_pkt_init   (netcap_pkt_t* pkt);
netcap_pkt_t*  netcap_pkt_create (void);

struct iphdr*  netcap_pkt_get_ip_hdr  ( netcap_pkt_t* pkt );
struct tcphdr* netcap_pkt_get_tcp_hdr ( netcap_pkt_t* pkt );


#endif
