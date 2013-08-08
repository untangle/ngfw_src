/**
 * $Id: netcap_virtual_interface.h,v 1.00 2013/08/08 11:32:06 dmorris Exp $
 */
#ifndef __NETCAP_VIRTUAL_INTERFACE_H
#define __NETCAP_VIRTUAL_INTERFACE_H

#include <linux/if.h>
#include "libnetcap.h"

/* This initialises the tun interface */
int netcap_virtual_interface_init( char *name );

/* Send pkt out the virtual interface */
int netcap_virtual_interface_send_pkt( netcap_pkt_t* pkt );

/* Clean up the virtual interface */
void netcap_virtual_interface_destroy( void );

#endif // __NETCAP_VIRTUAL_INTERFACE_H_


