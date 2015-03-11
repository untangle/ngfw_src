/**
 * $Id: netcap_arp.h,v 1.00 2015/03/10 14:05:39 dmorris Exp $
 */
#ifndef __NETCAP_ARP_H
#define __NETCAP_ARP_H

#include <netinet/in.h>
#include <netinet/if_ether.h>
#include "libnetcap.h"

int netcap_arp_init ();

int netcap_arp_lookup ( const char* ip, char* mac, int maclength );

#endif // __NETCAP_ARP_H_


