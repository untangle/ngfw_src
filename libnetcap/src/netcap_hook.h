/* $Id: netcap_hook.h,v 1.1 2004/11/09 19:39:59 dmorris Exp $ */
#ifndef __NETCAP_HOOK_H
#define __NETCAP_HOOK_H

#include "libnetcap.h"
#include <mvutil/list.h>

typedef int (*netcap_tcp_syn_hook_t)  (netcap_pkt_t* pkt );

extern netcap_tcp_hook_t global_tcp_hook;
extern netcap_tcp_syn_hook_t global_tcp_syn_hook;
extern netcap_udp_hook_t global_udp_hook;
extern netcap_icmp_hook_t global_icmp_hook;

int  netcap_hooks_init (void);
int  netcap_hooks_cleanup (void);

#endif
