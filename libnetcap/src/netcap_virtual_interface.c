/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ip.h 8515 2007-01-03 00:13:24Z rbscott $
 */

#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#include <sys/ioctl.h>
#include <sys/socket.h>
#include <linux/if.h>
#include <linux/if_tun.h>
#include <linux/if_ether.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include <netinet/ip.h>

#include "netcap_virtual_interface.h"

#define TUN_DEVICE "/dev/net/tun"

#define TUN_DEVICE_RP_FILTER_FORMAT "/proc/sys/net/ipv4/conf/%s/rp_filter"

typedef struct 
{
    char name[IFNAMSIZ];
    int fd;
} netcap_virtual_interface_t;

static netcap_virtual_interface_t tun_dev =
{
    .name = "",
    .fd = 0
};


/**
 * This will open a tun device and set the name to name
 */
int netcap_virtual_interface_init( char *name )
{
    struct ifreq ifr;
    int fd;

    int _critical_section( void ) {
      /* sets the tun device to be an IP TUN device
       * and uses fd to send an ioctl to bring the interface up by name */
        bzero( &ifr, sizeof( ifr ));
        
        ifr.ifr_flags = IFF_TUN;
        strncpy(ifr.ifr_name, tun_dev.name, sizeof( ifr.ifr_name ));

        /* create the virtual device and returns the name in &ifr */
        if ( ioctl( tun_dev.fd, TUNSETIFF, &ifr ) < 0 ) return perrlog( "ioctl [TUNSETIFF]" );

        strncpy(tun_dev.name, ifr.ifr_name, sizeof( tun_dev.name ));
            
        /* same ioctl but on a the temporary socket "fd" */
        if ( ioctl( fd, SIOCGIFFLAGS, &ifr ) < 0 ) return perrlog( "ioctl [SIOCGIFFLAGS]" );

        /* bring the interface up */
        ifr.ifr_flags |= IFF_UP;
        ifr.ifr_flags |= IFF_RUNNING;
        debug( 2, "Bringing up the tun interface: %s, %#010x\n", tun_dev.name, ifr.ifr_flags );
        
        if ( ioctl( fd, SIOCSIFFLAGS, &ifr ) < 0 ) return perrlog( "ioctl [SIOCSIFFLAGS]" );

        return 0;
    }


    /* Verify the name is valid by checking its length (doesn't print
     * interface because it could be a bad pointer) */
    if (( name != NULL ) && ( strnlen( name, IFNAMSIZ + 1 ) > IFNAMSIZ )) {
        return errlog( ERR_CRITICAL, "Invalid interface name." );
    }

    if ( tun_dev.fd != 0 ) errlog( ERR_WARNING, "initializing tun with non-zero fd %d\n", tun_dev.fd );

    /* If the name is set, copy it into the main structure, otherwise null it out. */
    if ( name != NULL ) strncpy( tun_dev.name, name, sizeof( tun_dev.name ));
    else bzero( tun_dev.name, sizeof( tun_dev.name ));
    
    /* There is an old technique, but we are not using it here */
    if (( tun_dev.fd = open( TUN_DEVICE, O_RDWR )) < 0 ) return perrlog( "open" );

    /* open a temporary socket to send the "bring interface up" ioctl on */
    if (( fd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) {
        perrlog( "socket" );
        if ( close( tun_dev.fd ) < 0 ) perrlog( "close" );
        tun_dev.fd = 0;
        return -1;
    }

    int ret = _critical_section();
    /* close the temp socket after bringing up the virtual interface */
    if ( close( fd ) < 0 ) perrlog( "close" );
    
    if ( ret < 0 ) {
        if ( close( tun_dev.fd ) < 0 ) perrlog( "close" );
        tun_dev.fd = 0;
        return errlog( ERR_CRITICAL, "_critical_section\n" );
    }

    return 0;
}


int netcap_virtual_interface_send_pkt( netcap_pkt_t* pkt )
{
    void *packet = NULL;
    int packet_len = 0;
    struct iphdr* ip_header = (struct iphdr*) pkt->data;
    
    int _critical_section() {
        int bytes_written = 0;

        ((struct tun_pi*)packet)->flags = htons( TUN_TUN_DEV );
        ((struct tun_pi*)packet)->proto = htons( ETH_P_IP );
        memcpy( &(((struct tun_pi*)packet)[1]), ip_header, packet_len - sizeof( struct tun_pi ));
        
        debug(8, "sending packet  %s -> %s to fd: %d length: %d\n",
              unet_next_inet_ntoa( ip_header->saddr), 
              unet_next_inet_ntoa(ip_header->daddr), tun_dev.fd, packet_len );
        
        bytes_written = write( tun_dev.fd, packet, packet_len );
        if ( bytes_written < packet_len ){
            errlog( ERR_CRITICAL, "netcap_virtual_interface_send_pkt: sent %d out of %d bytes\n", 
                    bytes_written, packet_len );
        }

        return bytes_written;
    }

    if ( pkt == NULL ) return errlogargs();

    packet_len = pkt->data_len + sizeof( struct tun_pi );

    if ( packet_len < sizeof( struct iphdr )) {
        return errlog( ERR_CRITICAL, "Invalid packet length %d", packet_len );
    }

    if (( packet = malloc( packet_len + sizeof( struct tun_pi ))) == NULL ) {
        return errlog( ERR_CRITICAL, "malloc\n" );
    }

    int ret = _critical_section();

    free( packet );

    if ( ret < 0 ) return errlog( ERR_CRITICAL, "_critical_section" );

    return ret;    
}


void netcap_virtual_interface_destroy( void )
{
    
    if (( tun_dev.fd > 0 ) && ( close( tun_dev.fd ) < 0 )) perrlog( "close" );
    
    tun_dev.fd = 0;
}


