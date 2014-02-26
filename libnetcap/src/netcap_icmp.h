/* $HeadURL$ */
#ifndef __NETCAP_ICMP_H_
#define __NETCAP_ICMP_H_

#include <netinet/ip_icmp.h>

#include "libnetcap.h"

int  netcap_icmp_init         ( void );
int  netcap_icmp_cleanup      ( void );
int  netcap_icmp_send         ( char *data, int data_len, netcap_pkt_t* pkt );

int  netcap_icmp_verify_type_and_code( u_int type, u_int code );

#endif
