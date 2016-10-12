/**
 * $Id: netcap_conntrackd.h 37267 2014-02-26 23:42:19Z dmorris $
 */
#ifndef __NETCAP_CONNTRACKD_H_
#define __NETCAP_CONNTRACKD_H_

#include "libnetcap.h"

int   netcap_conntrack_init();
int   netcap_conntrack_cleanup( void );
void* netcap_conntrack_listen ( void* arg );
void netcap_conntrack_null_hook ( struct nf_conntrack* ct, int type );

#endif

