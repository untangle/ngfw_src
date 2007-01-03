/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

/* $Id$ */
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
