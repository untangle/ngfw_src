/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

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

/* Flag to indicate that an ARP should be forced even if it is in the ARP cache */
#define NETCAP_ARP_FORCE     0xD0DAB

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
static int  _issue_arp_request ( struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 netcap_intf_info_t* info );
static int  _build_arp_packet  ( struct ether_arp* pkt, struct in_addr* src_ip, struct in_addr* dst_ip,
                                 netcap_intf_bridge_info_t* info );

static int  _arp_dst_intf      ( netcap_intf_db_t* db, netcap_intf_t* intf, netcap_intf_t src_intf, 
                                 struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 unsigned long* delay_array );

static int  _arp_address       ( netcap_intf_db_t* db, struct in_addr* dst_ip, struct ether_addr* mac, 
                                 netcap_intf_info_t* intf_info, unsigned long* delays, int force_request );

static int _arp_bridge_intf   ( netcap_intf_db_t* db, netcap_intf_t* out_intf, 
                                struct ether_addr* mac_address, netcap_intf_info_t* intf_info );


/**
 * Perform the ioctl to determine which interface a packet is going to go out on.
 * this will just determine if it is going to out on a bridge, not which interface,
 * @param index    - Updated to contain the index of the interface for the next hop.
 * @param src_ip   - The source of the session(presently ignored)
 * @param dst_ip   - The destination to be lookuped up.
 * @param next_hop - Updated to contain the address of the next hop.
 */
static int  _out_interface     ( int* index, struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 struct in_addr* next_hop );

/**
 * A fake connection is required in order to make the kernel care about the ARP response that comes 
 * back from the machine.  Otherwise, the kernel will ignore the ARP response from dst_ip.
 **/
static int  _fake_connect      ( struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 netcap_intf_info_t* intf_info );
static int  _get_arp_entry     ( struct in_addr* ip, struct ether_addr* mac, 
                                 netcap_intf_info_t* intf_info );
static void _mac_to_string     ( char *mac_string, int len, struct ether_addr* mac );

static netcap_intf_info_t* _get_bridge_info ( netcap_intf_db_t* db, int index );

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

int netcap_arp_configure_bridge( netcap_intf_db_t* db, netcap_intf_info_t* intf_info )
{
    netcap_intf_bridge_info_t* bridge_info = NULL;
    int ret = 0;

    if ( intf_info == NULL || !intf_info->is_valid ) return errlogargs();

    if ( intf_info->bridge_info != NULL ) {
        errlog( ERR_WARNING, "Bridge already has configuration data, freeing\n" );
        free( intf_info->bridge_info );
        intf_info->bridge_info = NULL;
    }
    
    int index = intf_info->index;
        
    int _critical_section() {
        struct ifreq ifr;
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

#ifdef DEBUG_ON
        char mac_string[MAC_STRING_LENGTH];
#endif
        
        /* Set the index */
        broadcast.sll_ifindex = index;

        strncpy( ifr.ifr_name, intf_info->name.s, IFNAMSIZ );
        
        if ( ioctl( _netcap_arp.sock, SIOCGIFHWADDR, &ifr ) < 0 ) return perrlog( "ioctl" );
        
        memcpy( &bridge_info->mac, ifr.ifr_hwaddr.sa_data, sizeof( bridge_info->mac ));
        memcpy( &bridge_info->broadcast, &broadcast, sizeof( bridge_info->broadcast ));        
        
#ifdef DEBUG_ON
        _mac_to_string( mac_string, sizeof( mac_string ), &bridge_info->mac );

        debug( 4, "ROUTE: Bridge[%s] configuration MAC: %s index: %d\n", intf_info->name.s, mac_string, 
               intf_info->index );
#endif
        
        return 0;
    }

    if ( !_is_initialized()) return errlog( ERR_CRITICAL, "netcap_arp is not initialized\n" );
        
    if (( bridge_info = calloc( 1, sizeof( *bridge_info ))) == NULL ) return errlogmalloc();
    
    if (( ret = _critical_section()) < 0 ) {
        intf_info->bridge_info = NULL;
        free( bridge_info );
    } else {
        intf_info->bridge_info = bridge_info;
    }

    return ret;
}

int netcap_arp_dst_intf       ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, 
                                struct in_addr* dst_ip )
{
    return netcap_arp_dst_intf_delay( intf, src_intf, src_ip, dst_ip, _netcap_arp.delay_array );
}

int netcap_arp_dst_intf_delay ( netcap_intf_t* intf, netcap_intf_t src_intf, struct in_addr* src_ip, 
                                struct in_addr* dst_ip, unsigned long* delay_array )
{
    netcap_intf_db_t* db = NULL;

    if ( !_is_initialized()) return errlog( ERR_CRITICAL, "netcap_arp is not initialized\n" );
    
    if ( intf == NULL || dst_ip == NULL || delay_array == NULL ) return errlogargs();

    if (( db = netcap_interface_get_db()) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_interface_get_db\n" );
    }

    /* XXX Don't really need this one since it is only called from here */
    return _arp_dst_intf( db, intf, src_intf, src_ip, dst_ip, delay_array );
}

int netcap_arp_address        ( struct in_addr* dst_ip, struct ether_addr* mac, int bridge_intf_index,
                                unsigned long* delays )
{
    netcap_intf_db_t* db = NULL;
    netcap_intf_info_t* intf_info = NULL;

    if ( !_is_initialized())  return errlog( ERR_CRITICAL, "netcap_arp is not initialized\n" );

    if (( dst_ip == NULL ) || ( mac == NULL ) || ( delays == NULL )) return errlogargs();

    if (( db = netcap_interface_get_db()) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_interface_get_db\n" );
    }
    
    if (( intf_info = _get_bridge_info( db, bridge_intf_index )) == NULL ) {
        return errlog( ERR_CRITICAL, "Device index %d is not a bridge\n", bridge_intf_index );
    } 
    
    return _arp_address( db, dst_ip, mac, intf_info, delays, ~NETCAP_ARP_FORCE );
}

int netcap_arp_bridge_intf    ( netcap_intf_t* out_intf, struct ether_addr* mac_address, 
                                int bridge_intf_index )
{
    if ( !_is_initialized()) return errlog( ERR_CRITICAL, "netcap_arp is not initialized\n" );

    if ( out_intf == NULL || mac_address == NULL ) return errlogargs();

    netcap_intf_db_t* db = NULL;
    netcap_intf_info_t* intf_info = NULL;
    
    if (( db = netcap_interface_get_db()) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_interface_get_db\n" );
    }
    
    if (( intf_info = _get_bridge_info( db, bridge_intf_index )) == NULL ) {
        return errlog( ERR_CRITICAL, "Device index %d is not a bridge\n", bridge_intf_index );
    } 
    
    return _arp_bridge_intf( db, out_intf, mac_address, intf_info );
}

static int  _issue_arp_request ( struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 netcap_intf_info_t* intf_info )
{
    struct ether_arp pkt;
    
    int size;

    netcap_intf_bridge_info_t* bridge_info;
    if (( bridge_info = intf_info->bridge_info ) == NULL ) {
        return errlog( ERR_CRITICAL, "Interface %s is not a bridge\n", intf_info->name.s );
    }

    if ( _build_arp_packet( &pkt, src_ip, dst_ip, bridge_info ) < 0 ) {
        return errlog( ERR_CRITICAL, "_build_arp_packet\n" );
    }
    
    size = sendto( _netcap_arp.pkt_sock, &pkt, sizeof( pkt ), 0, (struct sockaddr*)&bridge_info->broadcast, 
                   sizeof( bridge_info->broadcast ));

    if ( size < 0 ) return perrlog( "sendto" );
    
    if ( size != sizeof( pkt )) {
        return errlog( ERR_WARNING, "Transmitted truncated ARP packet %d < %d", size, sizeof( pkt ));
    }
         
    return 0;
}

static int _arp_dst_intf ( netcap_intf_db_t* db, netcap_intf_t* intf, netcap_intf_t src_intf, 
                           struct in_addr* src_ip, struct in_addr* dst_ip, unsigned long* delay_array )
{
    int intf_index;
    struct in_addr next_hop;
    netcap_intf_info_t* intf_info;

    /* Indicate the interface is unknown in case there is an error */
    *intf = NC_INTF_UNK;

    if ( _out_interface( &intf_index, src_ip, dst_ip, &next_hop ) < 0 ) {
        return errlog( ERR_CRITICAL, "_out_interface\n" );
    }

    /* unable to determine the destination interface(but not an error eg default route unset), return. */    
    if ( intf_index < 0 ) return 0;

    if (( intf_info = netcap_intf_db_index_to_info( db, intf_index )) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_intf_db_index_to_info %d\n", intf_index );
    }

    if ( intf_info->bridge_info != NULL ) {
        int ret;
        struct ether_addr mac_address;
        // int src_intf_index = 0;
        // netcap_intf_info_t* src_intf_info = NULL;

        int force = ~NETCAP_ARP_FORCE;
        
        while ( 1 ) {
            if (( ret = _arp_address( db, &next_hop, &mac_address, intf_info, delay_array, force )) < 0 ) {
                return errlog( ERR_CRITICAL, "_arp_address\n" );
            }
            
            if ( ret == NETCAP_ARP_NOERROR ) {
                debug( 10, "ROUTE: Unable to resolve the MAC address %s\n", 
                       unet_next_inet_ntoa( next_hop.s_addr ));
                return NETCAP_ARP_NOERROR;
            }
            
            if (( ret = _arp_bridge_intf( db, intf, &mac_address, intf_info )) < 0 ) {
                if (( force == ~NETCAP_ARP_FORCE ) && ( errno == EINVAL )) {
                    debug( 10, "ROUTE: Forcing ARP for cached MAC Address.\n" );
                    force = NETCAP_ARP_FORCE;
                    continue;
                }
                return errlog( ERR_CRITICAL, "_arp_bridge_interface\n" );
            }

            break;
        }

        /* Convert the source interface from a netcap index to a linux index, not necessary,
         * just use the bridge it is going out on. */
        // if (( src_intf_info = netcap_intf_db_info_to_info( db, src_intf )) == NULL ) {
        // errlog( ERR_WARNING, "netcap_intf_db_info_to_info[%d]\n", src_intf );
        // } else {
        // src_intf_index = src_intf_info->index;
        // }

        /* XXXX should pass in the db rather than the default one to avoid synchronization issues */
        /* If the packet is multicast or broadcast, you have to force it out the other
         * interface */
        if ((( ret = netcap_interface_is_multicast( dst_ip->s_addr )) != 1 ) &&
            ( ret = netcap_interface_is_broadcast( dst_ip->s_addr, intf_info->index )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_inetface_is_broadcast\n" );
        } else if ( ret == 1 ) {
            netcap_intf_bridge_info_t* bridge_info = intf_info->bridge_info;
            int c;
            /* Find the first interface in the bridge that is not the source interface */
            for ( c = 0 ; c < bridge_info->intf_count && c < NETCAP_MAX_INTERFACES ; c++ ) {
                if (( bridge_info->ports[c] != NULL ) && 
                    ( bridge_info->ports[c]->netcap_intf != src_intf )) {
                    *intf = bridge_info->ports[c]->netcap_intf;
                    break;
                }
            }
        }
        
        return NETCAP_ARP_SUCCESS;
    }

    /* Don't have to do anything special here for broadcasts  *
     * because they will be going out the same interface they *
     * came in on, and those will be dropped.                 */

    if ( NC_INTF_UNK == ( *intf = intf_info->netcap_intf )) {
        return errlog( ERR_CRITICAL, "netcap_intf_db_index_to_info %d\n", intf_index );
    }
    
    return NETCAP_ARP_SUCCESS;
}

static int  _arp_address       ( netcap_intf_db_t* db, struct in_addr* dst_ip, struct ether_addr* mac, 
                                 netcap_intf_info_t* intf_info, unsigned long* delay_array, 
                                 int force_request )
{
    int c;
    unsigned long delay;
    int ret;
    struct in_addr src_ip;

    for ( c = 0 ; c < NETCAP_ARP_MAX ; c++ ) {
        /* Check the cache before issuing the request */
        if (( ret = _get_arp_entry( dst_ip, mac, intf_info )) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_arp_entry\n" );
        }
        
        /* If C is nonzero, then at least one ARP request is sent.  If force_request isn't sent
         * then it doesn't matter if the response comes from the cache */
        if ((( c != 0 ) || ( force_request != NETCAP_ARP_FORCE )) && ( ret == NETCAP_ARP_SUCCESS )) {
            return NETCAP_ARP_SUCCESS;
        }

        /* Connect and close so the kernel grabs the source address */
        if (( c == 0 ) && ( _fake_connect( &src_ip, dst_ip, intf_info ) < 0 )) {
            return errlog( ERR_CRITICAL, "_fake_connect\n" );
        }

        delay = delay_array[c];
        if ( delay == 0 ) break;
        if ( delay > NETCAP_ARP_MAX_DELAY ) {
            return errlog( ERR_CRITICAL, "Invalid delay: index %d is %l\n", c, delay );
        }

        /* Issue the arp request */
        if ( _issue_arp_request( &src_ip, dst_ip, intf_info ) < 0 ) {
            return errlog( ERR_CRITICAL, "_issue_arp_request\n" );
        }

        debug( 11, "Waiting for the response for: %d\n", delay );

        usleep( delay );
    }
    
    return NETCAP_ARP_NOERROR;
}

static int _arp_bridge_intf   ( netcap_intf_db_t* db, netcap_intf_t* out_intf, 
                                struct ether_addr* mac_address, netcap_intf_info_t* intf_info )
{
    struct ifreq ifr;
	int ret;
    netcap_intf_string_t buffer;
    
/*     struct  */
/*     { */
/*         int command; */
/*         // This is read as the bridge, and overwritten with outgoing interface. */
/*         netcap_intf_string_t* bridge; */
/*         struct ether_addr* mac_address; */
/*         void* unused; */
/*     } args = { */
/*         .command     = BRCTL_GET_DEVNAME, */
/*         .bridge      = &buffer, */
/*         .mac_address = mac_address, */
/*         .unused      = NULL */
/*     }; */

    /* this way is more 32/64-bit compatible */
    unsigned long args[4] = {BRCTL_GET_DEVNAME, (unsigned long)&buffer, (unsigned long)mac_address, 0};
    
	strncpy( buffer.s, intf_info->name.s, sizeof( buffer ));
	strncpy( ifr.ifr_name, intf_info->name.s, sizeof( ifr.ifr_name ));
	ifr.ifr_data = (char*)&args;
        
	if (( ret = ioctl( _netcap_arp.sock, SIOCDEVPRIVATE, &ifr )) < 0 ) {
        if ( errno == EINVAL ) {
            /* this indicates that the MAC address requested is not in the bridge table.
             * to solve this, you have to go through a full ARP request. */
            *out_intf = NC_INTF_UNK;
            debug ( 10, "ROUTE: Invalid argument, MAC Address is only found in ARP Cache.\n" );
            return -1;
        } else {
            return perrlog( "ioctl" );
        }
    }
        
    netcap_intf_info_t* out_intf_info = NULL;
    if (( out_intf_info = netcap_intf_db_index_to_info( db, ret )) == NULL ) {
        /* XXX What to do here */
        errlog( ERR_WARNING, "Nothing is known about the outgoing interface index %d\n", ret );
        *out_intf = NC_INTF_UNK;
    } else {
        *out_intf = out_intf_info->netcap_intf;
    }

    debug( 8, "ROUTE[%s]: Outgoing interface index is %s,%d\n", intf_info->name.s, buffer.s, *out_intf );
    
	return 0;
}

static int  _build_arp_packet  ( struct ether_arp* pkt, struct in_addr* src_ip, struct in_addr* dst_ip,
                                 netcap_intf_bridge_info_t* bridge_info )
{    
    pkt->ea_hdr.ar_hrd = htons( ARPHRD_ETHER );
    pkt->ea_hdr.ar_pro = htons( ETH_P_IP );
    pkt->ea_hdr.ar_hln = ETH_ALEN;
	pkt->ea_hdr.ar_pln = sizeof( *dst_ip );
	pkt->ea_hdr.ar_op  = htons( ARPOP_REQUEST );
    memcpy( &pkt->arp_sha, &bridge_info->mac, sizeof( pkt->arp_sha ));
    memcpy( &pkt->arp_spa, src_ip, sizeof( pkt->arp_spa ));
    memcpy( &pkt->arp_tha, &bridge_info->broadcast.sll_addr, sizeof( pkt->arp_tha ));
    memcpy( &pkt->arp_tpa, dst_ip, sizeof( pkt->arp_tpa ));

    return 0;
}

static int  _out_interface     ( int* index, struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 struct in_addr* next_hop )
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


static int  _fake_connect      ( struct in_addr* src_ip, struct in_addr* dst_ip, 
                                 netcap_intf_info_t* intf_info )
{
    struct sockaddr_in dst_addr;
    int fake_fd;
    int ret = 0;

    int _critical_section( void ) {
        int one = 1;
        u_int addr_len = sizeof( dst_addr );
        int name_len = strnlen( intf_info->name.s, sizeof( intf_info->name )) + 1;

        if ( setsockopt( fake_fd, SOL_SOCKET, SO_BINDTODEVICE, intf_info->name.s, name_len ) < 0 ) {
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

static int  _get_arp_entry     ( struct in_addr* ip, struct ether_addr* mac, netcap_intf_info_t* intf_info )
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
    
    strncpy( request.arp_dev, intf_info->name.s, sizeof( request.arp_dev ));

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

static netcap_intf_info_t* _get_bridge_info ( netcap_intf_db_t* db, int index )
{
    netcap_intf_info_t* intf_info = netcap_intf_db_index_to_info ( db, index );
    if ( intf_info == NULL ) return errlog_null( ERR_CRITICAL, "netcap_intf_db_index_to_info\n" );
    if ( intf_info->bridge_info == NULL ) {
        return errlog_null( ERR_CRITICAL, "Interface is not  a bridge %d\n", index );
    }
    
    return intf_info;
}
