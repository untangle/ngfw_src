/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_queue.h,v 1.1 2004/11/09 19:39:59 dmorris Exp $
 */
#ifndef __NETCAP_QUEUE_H
#define __NETCAP_QUEUE_H

#include <linux/netfilter.h>
#include "libnetcap.h"

int  netcap_queue_init (void);
int  netcap_queue_cleanup (void);
int  netcap_queue_get_sock (void);
int  netcap_queue_read (char *buffer, int max, netcap_pkt_t* props);
int  netcap_set_verdict (unsigned long packet_id, int verdict, char *buffer, int len);
int  netcap_raw_send (char* pkt, int len);


#endif
