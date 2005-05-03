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

int netcap_interface_update_address( int inside, int outside );

#define NC_INTF_SET_TO_RULE_IN  1

/* Returns <0 on error, 0 for no more rules, 1 for more rules left *
 * destructive on the input intfset_p */
int netcap_intfset_to_rule(netcap_intfset_t* intfset_p, char *str, int str_len, int if_in);

#endif
