/* $Id: netcap_version.c,v 1.1 2004/11/09 19:40:00 dmorris Exp $ */
#include "version.h"

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
