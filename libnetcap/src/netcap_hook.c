/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

/* $Id$ */
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
