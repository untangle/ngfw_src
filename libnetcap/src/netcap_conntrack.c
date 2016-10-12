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
#include "netcap_session.h"
#include <libnetfilter_conntrack/libnetfilter_conntrack.h>

#define BYPASS_MARK 0x1000000
#define BUFFER_SIZE 0x800000

static struct nfct_handle *cth;

struct netcap_ct_entry {
    uint32_t mark;
    uint32_t ct_id;
    uint8_t  l3_proto, l4_proto;
    uint8_t  icmp_type;
    uint32_t ip4_src_addr, ip4_dst_addr;  /*original direction*/
    uint32_t r_ip4_src_addr, r_ip4_dst_addr; /*reply direction*/
    uint16_t port_src, port_dst;
    uint16_t r_port_src, r_port_dst;
    uint64_t counter_pkts, counter_bytes;
    uint64_t r_counter_pkts, r_counter_bytes;
    uint64_t timestamp_start, timestamp_stop;
};

static int  _netcap_conntrack_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *ct, void *data );

#if 0
static void _netcap_conntrack_print_ct_entry( int level, struct netcap_ct_entry* netcap_ct )
{
    debug( level, "ATTR_ID                  = %u\n",netcap_ct->ct_id);
    debug( level, "ATTR_ORIG_L3PROTO        = %d\n",netcap_ct->l3_proto );
    debug( level, "ATTR_ORIG_L4PROTO        = %d\n", netcap_ct->l4_proto );

    debug( level, "ATTR_ORIG_IPV4_SRC        = %s\n",unet_next_inet_ntoa(netcap_ct->ip4_src_addr));
    debug( level, "ATTR_ORIG_IPV4_DST        = %s\n",unet_next_inet_ntoa(netcap_ct->ip4_dst_addr));
    debug( level, "ATTR_REPL_IPV4_SRC        = %s\n",unet_next_inet_ntoa(netcap_ct->r_ip4_src_addr));
    debug( level, "ATTR_REPL_IPV4_DST        = %s\n",unet_next_inet_ntoa(netcap_ct->r_ip4_dst_addr));

    debug( level, "ATTR_ORIG_PORT_SRC        = %d\n",netcap_ct->port_src);
    debug( level, "ATTR_ORIG_PORT_DST        = %d\n",netcap_ct->port_dst);
    debug( level, "ATTR_REPL_PORT_SRC        = %d\n",netcap_ct->r_port_src);
    debug( level, "ATTR_REPL_PORT_DST        = %d\n",netcap_ct->r_port_dst);
}
#endif

int  netcap_conntrack_init()
{
    int ret = 0;
    cth = nfct_open(CONNTRACK, NF_NETLINK_CONNTRACK_NEW|NF_NETLINK_CONNTRACK_DESTROY);

    if (!cth)  return -1;

    ret = nfnl_rcvbufsiz(nfct_nfnlh(cth), BUFFER_SIZE);
    debug( 5, "CONNTRACK: set socket buffer size to %d.\n", ret );

    //nfct_callback_register(cth, NFCT_T_ALL, _netcap_conntrack_callback, NULL);
    nfct_callback_register(cth, NFCT_T_NEW | NFCT_T_DESTROY, _netcap_conntrack_callback, NULL);

    return 0;
}

void* netcap_conntrack_listen ( void* arg )
{
    int res = 0;
    debug( 1, "ConntrackD listening for conntrack updates...\n" );

    while (1) {

        res = nfct_catch(cth);  
        if (res == -1) {
            errlog( ERR_WARNING,"nfct_catch() returned! %s\n", strerror(errno) );
            return NULL;
        }
    }

}

void netcap_conntrack_null_hook ( struct nf_conntrack* ct, int type )
{
    nfct_destroy(ct);
    //do nothing
}

int _netcap_conntrack_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *ct, void *data )
{
    if ( ct == NULL ) {
        errlog(ERR_WARNING, "_netcap_conntrack_callback() called with NULL conntrack");
        return NFCT_CB_CONTINUE;
    }

    uint32_t client = nfct_get_attr_u32(ct, ATTR_ORIG_IPV4_SRC);
    uint32_t server = nfct_get_attr_u32(ct, ATTR_REPL_IPV4_SRC);

    /* ignore sessions from 127.0.0.1 to 127.0.0.1 */
    if ( client == 0x0100007f && server == 0x0100007f ) {
        //debug( 10, "CONNTRACK: local mark=0x%08x\n", nfct_get_attr_u32(my_ct, ATTR_MARK) );
        return NFCT_CB_CONTINUE;
    }

    struct nf_conntrack *my_ct = nfct_clone(ct); // clone it because ct is gone after this hook returns

    switch (type) {
    case NFCT_T_DESTROY:
        debug( 10, "CONNTRACK: type=DESTROY mark=0x%08x %s:%d -> %s:%d\n", nfct_get_attr_u32(my_ct, ATTR_MARK),
               unet_next_inet_ntoa(client), ntohs(nfct_get_attr_u16(my_ct, ATTR_ORIG_PORT_SRC)),
               unet_next_inet_ntoa(server), ntohs(nfct_get_attr_u16(my_ct, ATTR_REPL_PORT_SRC)));
        //_netcap_conntrack_print_ct_entry(10, &netcap_ct);
        break;
    case NFCT_T_NEW:
        debug( 10, "CONNTRACK: type=NEW mark=0x%08x %s:%d -> %s:%d\n", nfct_get_attr_u32(my_ct, ATTR_MARK),
               unet_next_inet_ntoa(client), ntohs(nfct_get_attr_u16(my_ct, ATTR_ORIG_PORT_SRC)),
               unet_next_inet_ntoa(server), ntohs(nfct_get_attr_u16(my_ct, ATTR_REPL_PORT_SRC)));
        //_netcap_conntrack_print_ct_entry(10, &netcap_ct);
        break;
    default:
        errlog( ERR_WARNING, "CONNTRACK: unknown type: %i\n", type );
        break;
    }

    global_conntrack_hook( my_ct, type );
    return NFCT_CB_CONTINUE;
}

