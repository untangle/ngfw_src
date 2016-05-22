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
static void _netcap_conntrack_ct_entry_copy(struct netcap_ct_entry *n, struct nf_conntrack *ct);

#define GET_CT_ITEM(elem, attr, x)                              \
        do { n->elem = nfct_get_attr_u##x(ct,(attr)); } while (0)


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

        nfct_callback_register(cth, NFCT_T_ALL, _netcap_conntrack_callback, NULL);

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

void netcap_conntrack_null_hook ( int type, long mark, long conntrack_id, u_int64_t session_id, 
                                  int l3_proto, int l4_proto, int icmp_type,
                                  long c_client_addr, long c_server_addr,
                                  int  c_client_port, int c_server_port,
                                  long s_client_addr, long s_server_addr,
                                  int  s_client_port, int s_server_port,
                                  int c2s_packets, int c2s_bytes,
                                  int s2c_packets, int s2c_bytes,
                                  long timestamp_start, long timestamp_stop )
{
    //do nothing
}

int _netcap_conntrack_callback( enum nf_conntrack_msg_type type, struct nf_conntrack *ct, void *data )
{
        uint32_t mark = nfct_get_attr_u32(ct, ATTR_MARK);
        struct netcap_ct_entry netcap_ct;

        _netcap_conntrack_ct_entry_copy(&netcap_ct, ct);

        // if its TCP and not bypassed, the event will be logged elsewhere
        /* if( netcap_ct.l4_proto == 6 && (mark & BYPASS_MARK) != BYPASS_MARK) { */
        /*     debug( 10, "CONNTRACK: type=6 mark=0x%08x\n", mark ); */
        /*     return NFCT_CB_CONTINUE; */
        /* } */
        // if its UDP and not bypassed, the event will be logged elsewhere
        /* if( netcap_ct.l4_proto == 17 && (mark & BYPASS_MARK) != BYPASS_MARK) { */
        /*     debug( 10, "CONNTRACK: type=17 mark=0x%08x\n", mark ); */
        /*     return NFCT_CB_CONTINUE; */
        /* } */
        /* ignore sessions from 127.0.0.1 to 127.0.0.1 */
        if ( netcap_ct.ip4_src_addr == 0x0100007f && netcap_ct.ip4_dst_addr == 0x0100007f ) {
            //debug( 10, "CONNTRACK: local mark=0x%08x\n", mark );
            return NFCT_CB_CONTINUE;
        }

        u_int64_t session_id = 0;
        
        switch (type) {
        case NFCT_T_DESTROY:
            debug( 10, "CONNTRACK: type=DESTROY mark=0x%08x %s:%d -> %s:%d\n", mark,
                   unet_next_inet_ntoa(netcap_ct.ip4_src_addr), netcap_ct.port_src,
                   unet_next_inet_ntoa(netcap_ct.r_ip4_src_addr), netcap_ct.r_port_src);
            //_netcap_conntrack_print_ct_entry(10, &netcap_ct);
            break;
        case NFCT_T_NEW:
            debug( 10, "CONNTRACK: type=NEW mark=0x%08x %s:%d -> %s:%d\n", mark,
                   unet_next_inet_ntoa(netcap_ct.ip4_src_addr), netcap_ct.port_src,
                   unet_next_inet_ntoa(netcap_ct.r_ip4_src_addr), netcap_ct.r_port_src);
            //_netcap_conntrack_print_ct_entry(10, &netcap_ct);
            session_id = netcap_session_next_id();
            break;
        default:
            debug( 10, "CONNTRACK: type=unknow mark=0x%08x\n", mark );
            break;
        }

        global_conntrack_hook( type, netcap_ct.mark, netcap_ct.ct_id, session_id,
                               netcap_ct.l3_proto, netcap_ct.l4_proto, netcap_ct.icmp_type,
                               netcap_ct.ip4_src_addr, netcap_ct.ip4_dst_addr,
                               netcap_ct.port_src, netcap_ct.port_dst,
                               netcap_ct.r_ip4_dst_addr, netcap_ct.r_ip4_src_addr,
                               netcap_ct.r_port_dst, netcap_ct.r_port_src,
                               netcap_ct.counter_pkts, netcap_ct.counter_bytes,
                               netcap_ct.r_counter_pkts, netcap_ct.r_counter_bytes,
                               netcap_ct.timestamp_start, netcap_ct.timestamp_stop );
                               
        return NFCT_CB_CONTINUE;
}

void _netcap_conntrack_ct_entry_copy(struct netcap_ct_entry *n, struct nf_conntrack *ct)
{
        GET_CT_ITEM(l3_proto, ATTR_ORIG_L3PROTO, 8);
        GET_CT_ITEM(l4_proto, ATTR_ORIG_L4PROTO, 8);

        GET_CT_ITEM(ip4_src_addr, ATTR_ORIG_IPV4_SRC, 32);
        GET_CT_ITEM(ip4_dst_addr, ATTR_ORIG_IPV4_DST, 32);

        GET_CT_ITEM(port_src, ATTR_ORIG_PORT_SRC, 16);
        GET_CT_ITEM(port_dst, ATTR_ORIG_PORT_DST, 16);
        n->port_src = ntohs(n->port_src);
        n->port_dst = ntohs(n->port_dst);
        
        GET_CT_ITEM(r_ip4_src_addr, ATTR_REPL_IPV4_SRC, 32);
        GET_CT_ITEM(r_ip4_dst_addr, ATTR_REPL_IPV4_DST, 32);

        GET_CT_ITEM(icmp_type, ATTR_ICMP_TYPE, 8);
        
        GET_CT_ITEM(r_port_src, ATTR_REPL_PORT_SRC, 16);
        GET_CT_ITEM(r_port_dst, ATTR_REPL_PORT_DST, 16);
        n->r_port_src = ntohs(n->r_port_src);
        n->r_port_dst = ntohs(n->r_port_dst);

        GET_CT_ITEM(counter_pkts, ATTR_ORIG_COUNTER_PACKETS, 64);
        GET_CT_ITEM(counter_bytes, ATTR_ORIG_COUNTER_BYTES, 64);

        GET_CT_ITEM(r_counter_pkts, ATTR_REPL_COUNTER_PACKETS, 64);
        GET_CT_ITEM(r_counter_bytes, ATTR_REPL_COUNTER_BYTES, 64);

        /* XXX - these always returns 0? */
        GET_CT_ITEM(timestamp_start, ATTR_TIMESTAMP_START, 64);
        GET_CT_ITEM(timestamp_stop, ATTR_TIMESTAMP_STOP, 64);

        GET_CT_ITEM(ct_id, ATTR_ID, 32);
        GET_CT_ITEM(mark, ATTR_MARK, 32);
}
