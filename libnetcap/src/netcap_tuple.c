/**
 * $Id$
 */
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#include <stdlib.h>
#include <string.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include <libnetcap.h>

int  netcap_endpoints_copy          ( netcap_endpoints_t* dst, netcap_endpoints_t* src )
{
    if ( src == NULL || dst == NULL ) return errlogargs();

    memcpy ( dst, src, sizeof( netcap_endpoints_t));
    return 0;
}

int  netcap_endpoints_bzero         ( netcap_endpoints_t* endpoints )
{
    if ( endpoints == NULL ) return errlogargs();

    bzero ( endpoints, sizeof( netcap_endpoints_t ));
    return 0;
}
