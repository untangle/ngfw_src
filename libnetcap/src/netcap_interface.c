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

#include "netcap_arp.h"
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

/**
 * Detect the destination interface for a given session
 */
int  netcap_interface_dst_intf       ( netcap_session_t* session, char* intf_name )
{
    if ( session == NULL ) return errlogargs();

    netcap_intf_t server_intf_index;
    
    if ( session->srv.intf != NF_INTF_UNKNOWN ) {
        debug( 10, "INTERFACE: (%10u) Destination interface is already known %d\n", 
               session->session_id, session->srv.intf );
        return 0;
    }

    /* Need to determine the redirected destination interface, not the
     * original destination interface */
    /* From the reply, the source is where this session is heading, and the destination is
     * where it is coming from, the source is unused in this function, so it doesn't really
     * matter. */
    struct in_addr dst = { .s_addr = session->nat_info.reply.src_address };
    struct in_addr src = { .s_addr = session->nat_info.reply.dst_address };

    if ( netcap_arp_dst_intf( &server_intf_index, session->cli.intf, &src, &dst ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_arp_dst_intf (%s -> %s)\n", unet_next_inet_ntoa( src.s_addr ), unet_next_inet_ntoa( dst.s_addr ) );
    }

    if (server_intf_index == 0) {
        return errlog(ERR_WARNING,"Unable to determine destination interface: (%s -> %s)\n", unet_next_inet_ntoa( src.s_addr ), unet_next_inet_ntoa( dst.s_addr ));
    }
    
    debug( 10, "INTERFACE: (%10u) Session (%s -> %s) is going out %d\n", 
           session->session_id, unet_next_inet_ntoa( src.s_addr ), unet_next_inet_ntoa( dst.s_addr ), server_intf_index );

    if (if_indextoname(server_intf_index, intf_name) == NULL) {
        return errlog(ERR_WARNING,"if_indextoname(%i) = \"%s\"\n", server_intf_index, strerror(errno));
    }
        
    return 0;
}
