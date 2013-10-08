/**
 * $Id: netcap_hook.c 35571 2013-08-08 18:37:27Z dmorris $
 */
#include "netcap_hook.h"

#include <errno.h>
#include <stdlib.h>
#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_udp.h"
#include "netcap_tcp.h"

netcap_tcp_hook_t     global_tcp_hook     = netcap_tcp_null_hook;
netcap_tcp_syn_hook_t global_tcp_syn_hook = netcap_tcp_syn_null_hook;
netcap_udp_hook_t     global_udp_hook     = netcap_udp_null_hook;

int  netcap_hooks_init           ( void )
{
    /* These are all initialized statically above */
    return 0;
}

int  netcap_hooks_cleanup        ( void )
{
    netcap_udp_hook_unregister();
    netcap_tcp_hook_unregister();
    return 0;
}

int  netcap_tcp_hook_register    ( netcap_tcp_hook_t hook )
{
    if ( hook == NULL ) return errlogargs();
    global_tcp_hook = hook;
    global_tcp_syn_hook = netcap_tcp_syn_hook;
    return 0;
}

int  netcap_tcp_hook_unregister  ( void )
{
    global_tcp_hook     = netcap_tcp_cleanup_hook;
    global_tcp_syn_hook = netcap_tcp_syn_cleanup_hook;
    return 0;
}

int  netcap_udp_hook_register    ( netcap_udp_hook_t hook )
{
    if ( hook == NULL ) return errlogargs();
    global_udp_hook = hook;
    return 0;
}

int  netcap_udp_hook_unregister  ( void )
{
    global_udp_hook = netcap_udp_cleanup_hook;
    return 0;
}
