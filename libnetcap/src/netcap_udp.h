/* $Id: netcap_udp.h,v 1.1 2004/11/09 19:40:00 dmorris Exp $ */
#ifndef __NETCAP_PKT_H
#define __NETCAP_PKT_H

#include "libnetcap.h"

int  netcap_udp_init (void);
int  netcap_udp_cleanup (void);

int  netcap_udp_recvfrom (int s, void* buf, size_t len, int flags, netcap_pkt_t* props);
int  netcap_udp_call_hooks (netcap_pkt_t* pkt, void* arg);

void netcap_udp_null_hook ( netcap_session_t* netcap_sess, void *arg);

#endif
