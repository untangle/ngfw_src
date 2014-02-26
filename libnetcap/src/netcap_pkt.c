/**
 * $Id$
 */
#include <netinet/in.h>
#include <netinet/ip.h>
#include <netinet/tcp.h>
#include <stdlib.h>
#include <mvutil/errlog.h>

#include "netcap_pkt.h"
#include "netcap_queue.h"

netcap_pkt_t* netcap_pkt_malloc (void)
{
    netcap_pkt_t* pkt = calloc(1,sizeof(netcap_pkt_t));

    if ( pkt == NULL ) return errlogmalloc_null();

    return pkt;
}

netcap_pkt_t* netcap_pkt_create (void)
{
    netcap_pkt_t* pkt;
    
    if ( ( pkt = netcap_pkt_malloc() ) == NULL ) {
        return errlog_null(ERR_CRITICAL,"netcap_pkt_malloc");
    }
        
    if ( netcap_pkt_init ( pkt ) < 0) {
        return errlog_null(ERR_CRITICAL, "netcap_pkt_init");
    }

    return pkt;
}

int netcap_pkt_init ( netcap_pkt_t* pkt ) {
    if ( pkt == NULL ) {
        return errlogargs();
    }

    // Zero out the packet
    bzero(pkt, sizeof(netcap_pkt_t));
    
    return 0;
}

void netcap_pkt_free (netcap_pkt_t* pkt)
{
    if (!pkt) {
        return (void)errlogargs();
    }
    
    free(pkt);
}

void netcap_pkt_destroy ( netcap_pkt_t* pkt )
{
    if ( !pkt ) {
        return (void)errlogargs();
    }

    if (pkt->opts) {
        free(pkt->opts);
    }

    /**
     * only free one of the two, if buffer exists free it,
     * otherwise free data if it exists
     */
    if (pkt->buffer) {
        free(pkt->buffer);
    } else if (pkt->data) {
        free(pkt->data);
    }
}

void netcap_pkt_raze (netcap_pkt_t* pkt)
{
    if ( !pkt ) {
        return (void)errlogargs();
    }

    netcap_pkt_destroy(pkt);

    netcap_pkt_free(pkt);
}

int  netcap_pkt_action_raze( netcap_pkt_t* pkt, int action )
{
    if (!pkt)
        return errlogargs();
       
    if (( pkt->packet_id != 0 ) && netcap_set_verdict( pkt->packet_id, action, NULL, 0 ) < 0 ) {
        perrlog( "netcap_set_verdict" );
        netcap_pkt_raze( pkt );
        return -1;
    }

    pkt->packet_id = 0;

    netcap_pkt_raze( pkt );
    return 0;
}


struct iphdr* netcap_pkt_get_ip_hdr ( netcap_pkt_t* pkt )
{
    if ( pkt == NULL || pkt->proto != IPPROTO_TCP|| pkt->data == NULL) {
        return errlogargs_null();
    }

    return (struct iphdr*) (pkt->data);
}

struct tcphdr* netcap_pkt_get_tcp_hdr ( netcap_pkt_t* pkt )
{
    struct iphdr* ip_hdr;
    int length = sizeof(struct iphdr) >> 2;

    if ( pkt == NULL || pkt->proto != IPPROTO_TCP|| pkt->data == NULL) {
        return errlogargs_null();
    }

    ip_hdr = (struct iphdr*) (pkt->data);

    if ( ip_hdr->ihl > 15 ) {
        errlog(ERR_WARNING,"Large IP header length (%i), Assuming %d.\n", 
               ip_hdr->ihl, length);
    } else if ( ip_hdr->ihl < length ) {
        errlog(ERR_WARNING,"Small IP header length (%i), Assuming %d.\n", 
               ip_hdr->ihl, length);
    } else {
        length = ip_hdr->ihl;
    }

    /* Multiply the length by 4 (Convert from words to bytes) */
    length = length << 2;
    
    if ( (length+sizeof(struct tcphdr)) > pkt->data_len ) {
        return errlog_null(ERR_CRITICAL,"Packet is too small to contain a TCP Header\n");
    }
    
    return (struct tcphdr*)( pkt->data + length );
}
