/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_init.h,v 1.1 2004/11/09 19:39:59 dmorris Exp $
 */
#ifndef __NETCAP_INIT_
#define __NETCAP_INIT_

#include <mvutil/errlog.h>

extern int netcap_inited;

#define TEST_INIT()  if (!netcap_inited) \
                        return errlog(ERR_CRITICAL,"Netcap Uninitialized\n")


#endif

