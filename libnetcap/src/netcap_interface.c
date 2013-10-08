/**
 * $Id: netcap_interface.c 35760 2013-08-31 02:39:23Z dmorris $
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

