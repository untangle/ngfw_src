/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#ifndef __INTERFACE_H
#define __INTERFACE_H

#include "libnetcap.h"

int netcap_interface_init (void);
int netcap_interface_cleanup (void);
int netcap_interface_marking(int ifAdd);

int netcap_interface_mark_to_intf(int nfmark, netcap_intf_t* intf);

int netcap_interface_update_address( void );

#endif
