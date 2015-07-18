/**
 * $Id$
 */
#ifndef __NETCAP_QUEUE_H
#define __NETCAP_QUEUE_H

#include <netinet/in.h>
#include <linux/netfilter.h>
#include "libnetcap.h"

int  netcap_queue_init       ( void );
int  netcap_queue_cleanup    ( void );
int                  netcap_nfqueue_get_udp_sock ( void );
struct nfq_handle*   netcap_nfqueue_get_udp_nfq ( void );
struct nfq_q_handle* netcap_nfqueue_get_udp_nfqh ( void );
int                  netcap_nfqueue_get_tcp_sock ( void );
struct nfq_handle*   netcap_nfqueue_get_tcp_nfq ( void );
struct nfq_q_handle* netcap_nfqueue_get_tcp_nfqh ( void );
int  netcap_nfqueue_get_tcp_sock ( void );
int  netcap_nfqueue_read     ( struct nfq_handle*  nfq_h, struct nfq_q_handle* nfq_qh, int nfq_fd, u_char* buf, int buf_len, netcap_pkt_t* pkt );
int  netcap_set_verdict      ( struct nfq_q_handle* nfq_qh, u_int32_t packet_id, int verdict, u_char* buf, int len);
int  netcap_set_verdict_mark ( struct nfq_q_handle* nfq_qh, u_int32_t packet_id, int verdict, u_char* buf, int len, int set_mark, u_int32_t mark );
int  netcap_raw_send         ( u_char* pkt, int len );


#endif
