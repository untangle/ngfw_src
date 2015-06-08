/**
 * $Id: netcap_conntrackd.h 37267 2014-02-26 23:42:19Z dmorris $
 */
#ifndef __NETCAP_CONNTRACKD_H_
#define __NETCAP_CONNTRACKD_H_

#include "libnetcap.h"

void* netcap_conntrack_listen( void* arg );

void netcap_conntrack_null_hook    ( /* dhan FIXME args ? */ void *arg );
void netcap_conntrack_cleanup_hook ( /* dhan FIXME args ? */ void *arg );

#endif

