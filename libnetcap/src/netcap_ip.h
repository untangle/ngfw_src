/**
 * $Id: netcap_ip.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_IP_H
#define __NETCAP_IP_H

#include "libnetcap.h"
#include "netinet/ip.h"
#include "netinet/udp.h"
#include "netinet/tcp.h"

/* Parse out the UDP header */
struct udphdr* netcap_ip_get_udp_header( struct iphdr* iph, int header_len );

/* Parse out the TCP header */
struct tcphdr* netcap_ip_get_tcp_header( struct iphdr* iph, int header_len );


#endif // __NETCAP_IP_H



