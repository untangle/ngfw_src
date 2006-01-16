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
#ifndef __NETCAP_QUEUE_H
#define __NETCAP_QUEUE_H

#include <linux/netfilter.h>
#include "libnetcap.h"

int  netcap_queue_init (void);
int  netcap_queue_cleanup (void);
int  netcap_queue_get_sock (void);
int  netcap_queue_read (u_char* buffer, int max, netcap_pkt_t* props);
int  netcap_set_verdict (unsigned long packet_id, int verdict, u_char* buffer, int len);
int  netcap_raw_send (u_char* pkt, int len);


#endif
