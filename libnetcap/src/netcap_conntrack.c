/**
 * $Id: netcap_conntrackd.c 37443 2014-03-27 19:17:19Z dmorris $
 */
#include "netcap_conntrack.h"

#include <stdlib.h>
#include <semaphore.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/ip.h>
#include <netinet/udp.h>
#include <arpa/inet.h>
#include <string.h>
#include <errno.h>
#include <unistd.h>
#include <inttypes.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/list.h>
#include <mvutil/uthread.h>
#include <mvutil/unet.h>

#include "netcap_hook.h"
#include <libnetfilter_conntrack/libnetfilter_conntrack.h>

#define BYPASS_MARK 0x1000000

static struct nfct_handle *cth;
void netcap_conntrack_print_ct_entry( void );

struct netcap_ct_entry {
        uint32_t mark;
        int type;
        uint32_t ct_id ;
        uint8_t  l3_proto, l4_proto;
        uint32_t ip4_src_addr, ip4_dst_addr;  /*original direction*/
        uint32_t r_ip4_src_addr, r_ip4_dst_addr; /*reply direction*/
        uint16_t port_src, port_dst;
        uint16_t r_port_src, r_port_dst;
        uint64_t counter_pkts, counter_bytes;
        uint64_t r_counter_pkts, r_counter_bytes;
        uint64_t timestamp_start, timestamp_stop;
};
struct netcap_ct_entry netcap_ct;


#define GET_CT_ITEM(elem, attr, x)                              \
        do { n->elem = nfct_get_attr_u##x(ct,(attr)); } while (0)

static void ct_entry_copy(struct netcap_ct_entry *n, struct nf_conntrack *ct)
{

        GET_CT_ITEM(l3_proto, ATTR_ORIG_L3PROTO, 8);
        GET_CT_ITEM(l4_proto, ATTR_ORIG_L4PROTO, 8);

        GET_CT_ITEM(ip4_src_addr, ATTR_ORIG_IPV4_SRC, 32);
        GET_CT_ITEM(ip4_dst_addr, ATTR_ORIG_IPV4_DST, 32);

        GET_CT_ITEM(port_src, ATTR_ORIG_PORT_SRC, 16);
        GET_CT_ITEM(port_dst, ATTR_ORIG_PORT_DST, 16);

        GET_CT_ITEM(r_ip4_src_addr, ATTR_REPL_IPV4_SRC, 32);
        GET_CT_ITEM(r_ip4_dst_addr, ATTR_REPL_IPV4_DST, 32);

        GET_CT_ITEM(r_port_src, ATTR_REPL_PORT_SRC, 16);
        GET_CT_ITEM(r_port_dst, ATTR_REPL_PORT_DST, 16);

        GET_CT_ITEM(counter_pkts, ATTR_ORIG_COUNTER_PACKETS, 64);
        GET_CT_ITEM(counter_bytes, ATTR_ORIG_COUNTER_BYTES, 64);

        GET_CT_ITEM(r_counter_pkts, ATTR_REPL_COUNTER_PACKETS, 64);
        GET_CT_ITEM(r_counter_bytes, ATTR_REPL_COUNTER_BYTES, 64);

        GET_CT_ITEM(timestamp_start, ATTR_TIMESTAMP_START, 64);
        GET_CT_ITEM(timestamp_stop, ATTR_TIMESTAMP_STOP, 64);

        GET_CT_ITEM(ct_id, ATTR_ID, 32);

}

static int netcap_bypass_cb(enum nf_conntrack_msg_type type,
                    struct nf_conntrack *ct,
                    void *data)
{
        uint32_t mark = nfct_get_attr_u32(ct, ATTR_MARK);
         
        if( (mark & BYPASS_MARK) != BYPASS_MARK) {
            return NFCT_CB_CONTINUE;
        }

        switch (type) {
        /*Only take care of destory , as it includes whole session info*/
        case NFCT_T_DESTROY:
                ct_entry_copy(&netcap_ct, ct);
                netcap_conntrack_print_ct_entry();
                break;
        default:
                break;
        }

        return NFCT_CB_CONTINUE;
}

int  netcap_conntrack_init()
{
        cth = nfct_open(CONNTRACK, NF_NETLINK_CONNTRACK_DESTROY);

        if (!cth)  return -1;

        nfct_callback_register(cth, NFCT_T_ALL, netcap_bypass_cb, NULL);

        return 0;
}

void netcap_conntrack_run ( void )
{

    int res = 0;

    debug( 1, "ConntrackD thread run to catch conntrack update..." );
    /*The nfct_catch already included a while loop inside*/
    res = nfct_catch(cth);  
    if (res == -1) return;

}

void* netcap_conntrack_listen ( void* arg )
{
    int res = 0;
    debug( 1, "ConntrackD listening for conntrack updates..." );

    while (1)
    {

        /*following two lines should be called in above function netcap_conntrack_run*/
        /*nfct_catch is a infinity while loop*/
        res = nfct_catch(cth);  
        if (res == -1) return;

        netcap_conntrack_print_ct_entry( );
        global_conntrack_hook(  NULL );
        sleep(30);
    }

}

void netcap_conntrack_print_ct_entry( void )
{

    debug( 10,"ATTR_ID                  = %d\n",netcap_ct.ct_id);
    debug( 10,"ATTR_ORIG_L3PROTO        = %d\n",netcap_ct.l3_proto );
    debug( 10,"ATTR_ORIG_L4PROTO        = %d\n", netcap_ct.l4_proto );

    debug( 10,"ATTR_ORIG_IPV4_SRC        = %s\n",unet_next_inet_ntoa(netcap_ct.ip4_src_addr));
    debug( 10,"ATTR_ORIG_IPV4_DST        = %s\n",unet_next_inet_ntoa(netcap_ct.ip4_dst_addr));
    debug( 10,"ATTR_REPL_IPV4_SRC        = %s\n",unet_next_inet_ntoa(netcap_ct.r_ip4_src_addr));
    debug( 10,"ATTR_REPL_IPV4_DST        = %s\n",unet_next_inet_ntoa(netcap_ct.r_ip4_dst_addr));

    debug( 10,"ATTR_ORIG_PORT_SRC        = %d\n",ntohs(netcap_ct.port_src));
    debug( 10,"ATTR_ORIG_PORT_DST        = %d\n",ntohs(netcap_ct.port_dst));
    debug( 10,"ATTR_REPL_PORT_SRC        = %d\n",ntohs(netcap_ct.r_port_src));
    debug( 10,"ATTR_REPL_PORT_DST        = %d\n",ntohs(netcap_ct.r_port_dst));

}

void netcap_conntrack_cleanup_hook ( void )
{
    nfct_close(cth);
}

void netcap_conntrack_null_hook ( void )
{
    errlog( ERR_WARNING, "netcap_conntrack_null_hook: No CONNTRACK hook registered\n" );
    netcap_conntrack_cleanup_hook ( );
}
