/* $Id: netcap_traffic.h,v 1.1 2004/11/09 19:40:00 dmorris Exp $ */
#ifndef __NETCAP_TRAFFIC_H_
#define __NETCAP_TRAFFIC_H_

#include <sys/types.h>
#include <netinet/in.h>
#include <libnetcap.h>

netcap_traffic_t* netcap_traffic_malloc(void);

int netcap_traffic_init (netcap_traffic_t* traf,
                         int proto, 
                         netcap_intf_t cli_intf, 
                         netcap_intf_t srv_intf,
                         in_addr_t* src, in_addr_t* shost_netmask, 
                         u_short src_port_min, u_short src_port_max,
                         in_addr_t* dst, in_addr_t* dhost_netmask, 
                         u_short dst_port_min, u_short dst_port_max);

netcap_traffic_t* netcap_traffic_create (int proto, 
                                         netcap_intf_t cli_intf, 
                                         netcap_intf_t srv_intf,
                                         in_addr_t* src, in_addr_t* shost_netmask, 
                                         u_short src_port_min, u_short src_port_max,
                                         in_addr_t* dst, in_addr_t* dhost_netmask, 
                                         u_short dst_port_min, u_short dst_port_max);


int netcap_traffic_destroy (netcap_traffic_t* descr);
int netcap_traffic_free (netcap_traffic_t * descr);
int netcap_traffic_copy (netcap_traffic_t* dst, netcap_traffic_t* src);

#endif /* __NETCAP_TRAFFIC_H_ */

