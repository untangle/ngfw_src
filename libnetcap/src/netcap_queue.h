/**
 * $Id: netcap_queue.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_QUEUE_H
#define __NETCAP_QUEUE_H

#include <netinet/in.h>
#include <linux/netfilter.h>
#include "libnetcap.h"

int  netcap_queue_init       ( void );
int  netcap_queue_cleanup    ( void );
int  netcap_nfqueue_get_sock ( void );
int  netcap_nfqueue_read     ( u_char* buffer, int max, netcap_pkt_t* props );
int  netcap_set_verdict_mark ( u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, 
                               u_int32_t mark );
int  netcap_raw_send         ( u_char* pkt, int len );


#endif
