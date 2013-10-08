/**
 * $Id: netcap_interface.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef _NETCAP_INTERFACE_H_
#define _NETCAP_INTERFACE_H_

#include "libnetcap.h"
#include <netinet/if_ether.h>
#include <linux/if_packet.h>

int netcap_interface_mark_to_cli_intf( int nfmark, netcap_intf_t* intf );

int netcap_interface_mark_to_srv_intf( int nfmark, netcap_intf_t* intf );

#endif // _NETCAP_INTERFACE_H_
