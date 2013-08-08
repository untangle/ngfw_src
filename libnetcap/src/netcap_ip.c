/**
 * $Id$
 */
#include "netcap_queue.h"

#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <string.h>
#include <sys/wait.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <errno.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <netinet/udp.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include "libnetcap.h"
#include "netcap_globals.h"

/* XXXXXXXXXXXXXXXX this is just repeated functionality from the bottom of netcap_pkt.c XXXXXXXXXXXXX */

static void* _get_second_header( struct iphdr* iph, int len, int protocol );

/* Parse out the UDP header */
struct udphdr* netcap_ip_get_udp_header( struct iphdr* iph, int len )
{
    struct udphdr* udphdr;

    if (( udphdr = _get_second_header( iph, len, IPPROTO_UDP )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "_get_second_header\n" );
    }
    
    /* XXX SHould add size verification code here */

    return udphdr;
}

/* Parse out the TCP header */
struct tcphdr* netcap_ip_get_tcp_header( struct iphdr* iph, int len )
{
    /* XXX SHould add size verification code here */

    return (struct tcphdr*)_get_second_header( iph, len, IPPROTO_TCP );
}

static void* _get_second_header( struct iphdr* iph, int len, int protocol )
{
    int offset;
    
    if ( iph == NULL ) {
        return errlogargs_null();
    }

    if ( iph->protocol != protocol ) {
        return errlog_null( ERR_CRITICAL, "Protocol mismatch: %d != %d\n", protocol, iph->protocol );
    }
    
    offset = iph->ihl;
    
    if ( offset > 15 ) return errlog_null( ERR_CRITICAL, "IP: Invalid data offset - %d\n", offset );
    
    /* Words to bytes */
    offset = offset << 2;
    
    if ( offset > len ) {
        return errlog_null( ERR_CRITICAL, "Packet is too short\n" );
    }
    
    return (((char*)iph) +  offset );
}
