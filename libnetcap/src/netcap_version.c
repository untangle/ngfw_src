/**
 * $Id: netcap_version.c 35571 2013-08-08 18:37:27Z dmorris $
 */
#include "libnetcap.h"
#include <mvutil/debug.h>

static const char vers[] = VERSION;

const char * netcap_version(void)
{
    return vers;
}

void netcap_debug_set_level(int lev)
{
    debug_set_level(NETCAP_DEBUG_PKG, lev);
}
