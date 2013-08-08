/**
 * $Id$
 */
#ifndef __NETCAP_UDP_H
#define __NETCAP_UDP_H

#include "libnetcap.h"

int  netcap_udp_init         ( void );
int  netcap_udp_cleanup      ( void );

int netcap_udp_divert_sock   ( void );
int netcap_udp_divert_port   ( void );

int  netcap_udp_recvfrom     ( int s, void* buf, size_t len, int flags, netcap_pkt_t* pkt );
int  netcap_udp_call_hooks   ( netcap_pkt_t* pkt, void* arg );

void netcap_udp_null_hook    ( netcap_session_t* netcap_sess, void *arg );
void netcap_udp_cleanup_hook ( netcap_session_t* netcap_sess, void *arg );


#endif
