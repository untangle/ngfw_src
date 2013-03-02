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
#include "netcap_interface.h"

#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <net/if.h>
#include <pthread.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sysfs/libsysfs.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include <mvutil/utime.h>

#include "libnetcap.h"

#include "netcap_globals.h"

#define NETCAP_MARK_INTF_MAX    255
#define NETCAP_MARK_INTF_CLI_MASK   0x000000FF
#define NETCAP_MARK_INTF_SRV_MASK   0x0000FF00

/**
 * Convert an interface mark to a netcap interface
 */
int  netcap_interface_mark_to_cli_intf(int nfmark, netcap_intf_t* intf)
{
    if ( intf == NULL ) return errlogargs();

    *intf = 0;

    nfmark &= NETCAP_MARK_INTF_CLI_MASK;

    if ( nfmark <= 0 || nfmark > NETCAP_MARK_INTF_MAX ) {
        return errlog( ERR_CRITICAL, "Invalid interface mark[%d]\n", nfmark );
    }

    /* Map the marking to the corresponding interface */
    *intf = nfmark;

    return 0;
}

/*
 * Convert an interface mark to a netcap srv interface
 */
int  netcap_interface_mark_to_srv_intf(int nfmark, netcap_intf_t* intf)
{
    if ( intf == NULL ) return errlogargs();

    *intf = 0;

    nfmark &= NETCAP_MARK_INTF_SRV_MASK;
    nfmark = nfmark >> 8;
    
    if ( nfmark <= 0 || nfmark > NETCAP_MARK_INTF_MAX ) {
        //return errlog( ERR_CRITICAL, "Invalid interface mark[%d]\n", nfmark );
        return -1;
    }

    /* Map the marking to the corresponding interface */
    *intf = nfmark;

    return 0;
}

