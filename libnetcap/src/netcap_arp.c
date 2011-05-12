/* $HeadURL$ */

#include <stdio.h>
#include <errno.h>
#include <stdlib.h>
#include <netinet/in.h>
#include <netinet/if_ether.h>
#include <linux/if_packet.h>
#include <net/if_arp.h>
#include <net/if.h>
#include <sys/ioctl.h>
#include <unistd.h>
#include <pthread.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/unet.h>

#include "netcap_arp.h"
#include "netcap_interface.h"

#define BRCTL_GET_DEVNAME    19
#define SIOCFINDEV           0x890E 

/* 10 Seconds, just to make sure it is not iterating through nothing */
#define NETCAP_ARP_MAX_DELAY 10000000L

// A port that is never actually used, it is just so there is a known value inside of the fake
// connect socket
#define NULL_PORT            59999

#define MAC_STRING_LENGTH    20

static struct
{
    /* The socket used to query the ARP tables */
    int sock;

    /* The socket used to send an ARP ping */
    int pkt_sock;

    struct ether_addr zero_mac;
        
    /* Variable sized, must be at the end of the array, the default values of arrays */
    unsigned long delay_array[];
    
} _netcap_arp = 
{
    .sock     = -1,
    .pkt_sock = -1,

    .zero_mac = 
    {
        .ether_addr_octet = 
        { 
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        }
    },

    .delay_array = 
    {
        3000,
        6000,
        20000,
        60000,
        250000,
        700000,
        1000000,
        15000,
        2000000,
        30000,
        3000000,
        10000,
        0
    }
};


static int  _is_initialized    ( void );

static int  _netcap_arp_dst_intf_delay ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip, unsigned long* delay_array );

static int  _issue_arp_request ( struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name );

static int  _build_arp_packet  ( struct ether_arp* pkt, struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name );

static int  _arp_dst_intf      ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip, unsigned long* delay_array );

static int  _arp_address       ( struct in_addr* dst_ip, struct ether_addr* mac, char* intf_name, unsigned long* delays, int force_request );

static int  _arp_bridge_intf   ( netcap_intf_t* out_intf, struct ether_addr* mac_address, char* intf_name );

/**
 * Perform the ioctl to determine which interface a packet is going to go out on.
 * this will just determine if it is going to out on a bridge, not which interface,
 * @param index    - Updated to contain the index of the interface for the next hop.
 * @param src_ip   - The source of the session(presently ignored)
 * @param dst_ip   - The destination to be lookuped up.
 * @param next_hop - Updated to contain the address of the next hop.
 */
static int  _out_interface     ( int* index, struct in_addr* src_ip, struct in_addr* dst_ip, struct in_addr* next_hop );

/**
 * A fake connection is required in order to make the kernel care about the ARP response that comes 
 * back from the machine.  Otherwise, the kernel will ignore the ARP response from dst_ip.
 **/
static int  _fake_connect      ( struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name );

static int  _get_arp_entry     ( struct in_addr* ip, struct ether_addr* mac, char* intf_name );

static void _mac_to_string     ( char *mac_string, int len, struct ether_addr* mac );



int netcap_arp_init           ( void )
{    
    if (( _netcap_arp.sock > 0 ) || (_netcap_arp.pkt_sock > 0 ))  {
        errlog( ERR_CRITICAL, "netcap_arp is already initialized\n" );
        return -1;
    }

    if (( _netcap_arp.sock = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) {
        perrlog( "socket" );
        _netcap_arp.sock = -1;
        return -1;
    }

    if (( _netcap_arp.pkt_sock = socket( PF_PACKET, SOCK_DGRAM, 0 )) < 0 ) {
        int tmp;
        tmp = errno;
        if ( unet_close( &_netcap_arp.sock ) < 0 ) errlog( ERR_CRITICAL, "unet_close" );
        errno = tmp;
        return perrlog( "socket" );
    }
    

    return 0;
}

int netcap_arp_cleanup        ( void )
{
    if ( unet_close( &_netcap_arp.sock ) < 0 )     errlog( ERR_CRITICAL, "unet_close\n" );
    if ( unet_close( &_netcap_arp.pkt_sock ) < 0 ) errlog( ERR_CRITICAL, "unet_close\n" );
            
    return 0;
}

int netcap_arp_dst_intf       ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip )
{
    return _netcap_arp_dst_intf_delay( intf, src_intf, src_ip, dst_ip, _netcap_arp.delay_array );
}



static int _netcap_arp_dst_intf_delay ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip, unsigned long* delay_array )
{
    if ( !_is_initialized()) return errlog( ERR_CRITICAL, "netcap_arp is not initialized\n" );
    
    if ( intf == NULL || dst_ip == NULL || delay_array == NULL ) return errlogargs();

    return _arp_dst_intf( intf, src_intf, src_ip, dst_ip, delay_array );
}

static int  _issue_arp_request ( struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name )
{
    struct ether_arp pkt;
    struct sockaddr_ll broadcast = {
        .sll_family   = AF_PACKET,
        .sll_protocol = htons( ETH_P_ARP ),
        .sll_ifindex  = 0, // set me 
        .sll_hatype   = htons( ARPHRD_ETHER ),
        .sll_pkttype  = PACKET_BROADCAST, 
        .sll_halen    = ETH_ALEN,
        .sll_addr = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
        }
    };
    int size;

    /* Set the index */
    broadcast.sll_ifindex = if_nametoindex(intf_name);

    if (broadcast.sll_ifindex == 0) {
        return errlog( ERR_CRITICAL, "failed to find index of \"%s\"\n", intf_name);
    }
    
    if ( _build_arp_packet( &pkt, src_ip, dst_ip, intf_name ) < 0 ) {
        return errlog( ERR_CRITICAL, "_build_arp_packet\n" );
    }
    
    size = sendto( _netcap_arp.pkt_sock, &pkt, sizeof( pkt ), 0, (struct sockaddr*)&broadcast, sizeof( broadcast ));

    if ( size < 0 ) {
        errlog(ERR_CRITICAL,"sendto( %i , %08x , %i, %i, %08x, %i )\n", _netcap_arp.pkt_sock, &pkt, sizeof( pkt ), 0, (struct sockaddr*)&broadcast, sizeof( broadcast ));
        return perrlog( "sendto" );
    }
    
    if ( size != sizeof( pkt )) {
        return errlog( ERR_WARNING, "Transmitted truncated ARP packet %d < %d", size, sizeof( pkt ));
    }
         
    return 0;
}

static int  _arp_dst_intf      ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, struct in_addr* dst_ip, unsigned long* delay_array )
{
    int intf_index;
    struct in_addr next_hop;
    char intf_name[IF_NAMESIZE];
    int is_bridge = 0;
    
    /* Indicate the interface is unknown in case there is an error */
    *intf = NF_INTF_UNKNOWN;

    if ( _out_interface( &intf_index, src_ip, dst_ip, &next_hop ) < 0 ) {
        return errlog( ERR_CRITICAL, "_out_interface\n" );
    }

    /* unable to determine the destination interface(but not an error eg default route unset), return. */    
    if ( intf_index < 0 ) return 0;

    if ( if_indextoname(intf_index, intf_name) == NULL) {
        return perrlog("if_indextoname");
    }

    /* ghetto way to check that an interface is a bridge */
    if (intf_name[0] == 'b' && intf_name[1] == 'r')
        is_bridge = 1;

    if ( is_bridge ) {
        int ret;
        struct ether_addr mac_address;
        // int src_intf_index = 0;

        int force = 1;
        
        while ( 1 ) {
            if (( ret = _arp_address( &next_hop, &mac_address, intf_name, delay_array, force )) < 0 ) {
                return errlog( ERR_CRITICAL, "_arp_address\n" );
            }
            
            if ( ret == NETCAP_ARP_NOERROR ) {
                debug( 10, "ROUTE: Unable to resolve the MAC address %s\n", 
                       unet_next_inet_ntoa( next_hop.s_addr ));
                return NETCAP_ARP_NOERROR;
            }
            
            if (( ret = _arp_bridge_intf( intf, &mac_address, intf_name )) < 0 ) {
                if (( force == 1 ) && ( errno == EINVAL )) {
                    debug( 10, "ROUTE: Forcing ARP for cached MAC Address.\n" );
                    force = 0;
                    continue;
                }
                return errlog( ERR_CRITICAL, "_arp_bridge_interface\n" );
            }

            break;
        }

        return NETCAP_ARP_SUCCESS;
    }
    
    return NETCAP_ARP_SUCCESS;
}

static int  _arp_address       ( struct in_addr* dst_ip, struct ether_addr* mac, char* intf_name, unsigned long* delay_array, int force_request )
{
    int c;
    unsigned long delay;
    int ret;
    struct in_addr src_ip;

    for ( c = 0 ; c < NETCAP_ARP_MAX ; c++ ) {
        /* Check the cache before issuing the request */
        if (( ret = _get_arp_entry( dst_ip, mac, intf_name )) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_arp_entry\n" );
        }
        
        /* If C is nonzero, then at least one ARP request is sent.  If force_request isn't sent
         * then it doesn't matter if the response comes from the cache */
        if ((( c != 0 ) || ( force_request != 0 )) && ( ret == NETCAP_ARP_SUCCESS )) {
            return NETCAP_ARP_SUCCESS;
        }

        /* Connect and close so the kernel grabs the source address */
        if (( c == 0 ) && ( _fake_connect( &src_ip, dst_ip, intf_name ) < 0 )) {
            return errlog( ERR_CRITICAL, "_fake_connect\n" );
        }

        delay = delay_array[c];
        if ( delay == 0 ) break;
        if ( delay > NETCAP_ARP_MAX_DELAY ) {
            return errlog( ERR_CRITICAL, "Invalid delay: index %d is %l\n", c, delay );
        }

        /* Issue the arp request */
        if ( _issue_arp_request( &src_ip, dst_ip, intf_name ) < 0 ) {
            return errlog( ERR_CRITICAL, "_issue_arp_request\n" );
        }

        debug( 11, "Waiting for the response for: %d\n", delay );

        usleep( delay );
    }
    
    return NETCAP_ARP_NOERROR;
}

static int  _arp_bridge_intf   ( netcap_intf_t* out_intf, struct ether_addr* mac_address, char* intf_name )
{
    struct ifreq ifr;
	int ret;
    netcap_intf_string_t buffer;
    
    /* this way is more 32/64-bit compatible */
    unsigned long args[4] = {BRCTL_GET_DEVNAME, (unsigned long)&buffer, (unsigned long)mac_address, 0};
    
	strncpy( buffer.s, intf_name, sizeof( buffer ));
	strncpy( ifr.ifr_name, intf_name, sizeof( ifr.ifr_name ));
	ifr.ifr_data = (char*)&args;
        
	if (( ret = ioctl( _netcap_arp.sock, SIOCDEVPRIVATE, &ifr )) < 0 ) {
        if ( errno == EINVAL ) {
            /* this indicates that the MAC address requested is not in the bridge table.
             * to solve this, you have to go through a full ARP request. */
            *out_intf = NF_INTF_UNKNOWN;
            debug ( 10, "ROUTE: Invalid argument, MAC Address is only found in ARP Cache.\n" );
            return -1;
        } else {
            return perrlog( "ioctl" );
        }
    }
        
    *out_intf = ret;

    debug( 8, "ROUTE[%s]: Outgoing interface index is %s,%d\n", intf_name, buffer.s, *out_intf );
    
	return 0;
}

static int  _build_arp_packet  ( struct ether_arp* pkt, struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name )
{    
    struct ifreq ifr;
    struct ether_addr mac;
    struct sockaddr_ll broadcast = {
        .sll_family   = AF_PACKET,
        .sll_protocol = htons( ETH_P_ARP ),
        .sll_ifindex  = 6, // SETME WITH SOMETHING
        .sll_hatype   = htons( ARPHRD_ETHER ),
        .sll_pkttype  = PACKET_BROADCAST, 
        .sll_halen    = ETH_ALEN,
        .sll_addr = {
            0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
        }
    };

    strncpy( ifr.ifr_name, intf_name, IFNAMSIZ );

    if ( ioctl( _netcap_arp.sock, SIOCGIFHWADDR, &ifr ) < 0 ) return perrlog( "ioctl" );

    memcpy( &mac, ifr.ifr_hwaddr.sa_data, sizeof( mac ));
    
    pkt->ea_hdr.ar_hrd = htons( ARPHRD_ETHER );
    pkt->ea_hdr.ar_pro = htons( ETH_P_IP );
    pkt->ea_hdr.ar_hln = ETH_ALEN;
	pkt->ea_hdr.ar_pln = sizeof( *dst_ip );
	pkt->ea_hdr.ar_op  = htons( ARPOP_REQUEST );
    memcpy( &pkt->arp_sha, &mac, sizeof( pkt->arp_sha ));
    memcpy( &pkt->arp_spa, src_ip, sizeof( pkt->arp_spa ));
    memcpy( &pkt->arp_tha, &broadcast.sll_addr, sizeof( pkt->arp_tha ));
    memcpy( &pkt->arp_tpa, dst_ip, sizeof( pkt->arp_tpa ));

    return 0;
}

static int  _out_interface     ( int* index, struct in_addr* src_ip, struct in_addr* dst_ip, struct in_addr* next_hop )
{
    /* Presently src_ip is ignored. ignored, but this may one day become important */
    if ( src_ip != NULL ) debug( 11, "ROUTE: Source ip presently ignored\n" );

    struct 
    {
        in_addr_t addr;
        in_addr_t nh;
        char name[IFNAMSIZ];
    } args;
    
    bzero( &args, sizeof( args ));
    args.addr = dst_ip->s_addr;
    
    if (( *index = ioctl( _netcap_arp.sock, SIOCFINDEV, &args )) < 0) {
        switch ( errno ) {
        case ENETUNREACH:
            debug( 10, "ROUTE: Destination IP is not reachable\n" );
            *index = -1;
            return 0;

            /* Ignore all other error codes */
        default:
            break;
        }

        return errlog( ERR_CRITICAL, "SIOCFINDEV[%s] %s.\n", unet_next_inet_ntoa( dst_ip->s_addr ), errstr );
    }

    /* If the next hop is on the local network, (eg. the next hop is the destination), 
     * the ioctl returns 0 */
    if ( args.nh != 0x00000000 ) {
        next_hop->s_addr = args.nh;
    } else {
        next_hop->s_addr = dst_ip->s_addr;
    }
    
    /* Assuming that the return value is the index of the interface,
     * make sure this is always true */
    debug( 9, "ROUTE: The destination %s is going out [%s,%d] to %s\n", 
           unet_next_inet_ntoa( dst_ip->s_addr ), args.name, *index, 
           unet_next_inet_ntoa( next_hop->s_addr ));
        
    return 0;
}

static int  _fake_connect      ( struct in_addr* src_ip, struct in_addr* dst_ip, char* intf_name )
{
    struct sockaddr_in dst_addr;
    int fake_fd;
    int ret = 0;

    int _critical_section( void ) {
        int one = 1;
        u_int addr_len = sizeof( dst_addr );
        int name_len = strnlen( intf_name, sizeof( intf_name )) + 1;

        if ( setsockopt( fake_fd, SOL_SOCKET, SO_BINDTODEVICE, intf_name, name_len ) < 0 ) {
            perrlog( "setsockopt(SO_BINDTODEVICE)" );
        }

        if ( setsockopt( fake_fd, SOL_SOCKET, SO_BROADCAST,  &one, sizeof(one)) < 0 ) {
            perrlog( "setsockopt(SO_BROADCAST)" );
        }

        if ( setsockopt( fake_fd, SOL_SOCKET, SO_DONTROUTE, &one, sizeof( one )) < 0 ) {
            return perrlog( "setsockopt(SO_DONTROUTE)" );
        }
        
        if ( connect( fake_fd, (struct sockaddr*)&dst_addr, sizeof( dst_addr )) < 0 ) {
            return errlog( ERR_CRITICAL, "connect[%s] %s.\n", unet_next_inet_ntoa( dst_ip->s_addr ), errstr );
        }
        
        if ( getsockname( fake_fd, (struct sockaddr*)&dst_addr, &addr_len ) < 0 ) {
            return perrlog( "getsockname" );
        }
        
        memcpy( src_ip, &dst_addr.sin_addr, sizeof( dst_addr.sin_addr ));
        return NETCAP_ARP_NOERROR;
    }
    
    dst_addr.sin_family = AF_INET;
    dst_addr.sin_port = htons( NULL_PORT );
    memcpy( &dst_addr.sin_addr, dst_ip, sizeof( dst_addr.sin_addr ));

    if (( fake_fd = socket( AF_INET, SOCK_DGRAM, 0 )) < 0 ) return perrlog( "socket" );
    
    ret = _critical_section();
    
    if ( close( fake_fd ) < 0 ) perrlog( "close" );
    
    return ret;
}

static int  _get_arp_entry     ( struct in_addr* ip, struct ether_addr* mac, char* intf_name )
{
    struct arpreq request;
    struct sockaddr_in* sin = (struct sockaddr_in*)&request.arp_pa;

#ifdef DEBUG_ON
    char mac_string[MAC_STRING_LENGTH];
#endif // DEBUG_ON
    
    /* XXX Probably not necessary */
    bzero( &request, sizeof( request ));

    sin->sin_family = AF_INET;
    sin->sin_port  = 0;
    memcpy( &sin->sin_addr, ip, sizeof( sin->sin_addr ));

    request.arp_ha.sa_family = ARPHRD_ETHER;
    
    strncpy( request.arp_dev, intf_name, sizeof( request.arp_dev ));

    request.arp_flags = 0;
    
    int ret;
    
    if (( ret = ioctl( _netcap_arp.sock, SIOCGARP, &request )) < 0 ) {
        /* This only fails if a socket has never been opened to this IP address.
         * Must also check that the address returned a zero MAC address */
        if ( errno == ENXIO ) {
            debug( 11, "ROUTE: Ethernet address for %s was not found\n", unet_next_inet_ntoa( ip->s_addr ));
            return NETCAP_ARP_NOERROR;
        }

        return perrlog( "ioctl" );
    }

    /* Returning an all zero MAC address indicates that the MAC was not found */
    if ( memcmp( &request.arp_ha.sa_data, &_netcap_arp.zero_mac, sizeof( struct ether_addr )) == 0 ) {
        debug( 11, "ROUTE: Ethernet address for %s was not found\n", unet_next_inet_ntoa( ip->s_addr ));
        return NETCAP_ARP_NOERROR;
    }

    memcpy( mac, &request.arp_ha.sa_data, sizeof( struct ether_addr ));

#ifdef DEBUG_ON
    _mac_to_string( mac_string, sizeof( mac_string ), mac );

    debug( 8, "ROUTE: Resolved MAC[%d]: '%s' -> '%s'\n", ret, inet_ntoa( *ip ), mac_string );
#endif // DEBUG_ON

    return NETCAP_ARP_SUCCESS;
}

static void _mac_to_string     ( char *mac_string, int len, struct ether_addr* mac )
{
    snprintf( mac_string, len, "%02x:%02x:%02x:%02x:%02x:%02x",
              mac->ether_addr_octet[0], mac->ether_addr_octet[1], mac->ether_addr_octet[2], 
              mac->ether_addr_octet[3], mac->ether_addr_octet[4], mac->ether_addr_octet[5] );
}

static int  _is_initialized    ( void )
{
    return ( _netcap_arp.sock > 0 ) && ( _netcap_arp.pkt_sock > 0 );
}


