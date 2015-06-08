/**
 * $Id: netcap_conntrackd.c 37443 2014-03-27 19:17:19Z dmorris $
 */
#include "netcap_conntrack.h"

#include <stdlib.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <inttypes.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/uthread.h>
#include <mvutil/unet.h>

#include "netcap_hook.h"

void* netcap_conntrack_listen( void* arg )
{
    /* dhan FIXME IMPLEMENT ME */
    debug( 1, "ConntrackD listening for conntrack updates..." );
    
    while (1) {
        sleep(30);
        // FIXME listen for conntrack updates
        global_conntrack_hook(  NULL );
    }
}

void netcap_conntrack_cleanup_hook ( /* dhan FIXME args ? */ void *arg )
{

}

void netcap_conntrack_null_hook ( /* dhan FIXME args ? */ void *arg )
{
    errlog( ERR_WARNING, "netcap_conntrack_null_hook: No CONNTRACK hook registered\n" );
}
