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

/* $Id$ */
#include <stdlib.h>
#include <stdio.h>

#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/libmvutil.h>
#include <mvutil/unet.h>

#include <libnetcap.h>
#include <netcap_route.h>
#include <netcap_interface.h>

#define DEBUG_INDEX            1
#define INTERFACE_LIST_INDEX   2
#define IP_START               3
#define MINIMUM_ARGS           IP_START

#define INTERFACE_LIST_MAX     (( IFNAMSIZ + 2 ) * NETCAP_MAX_INTERFACES )

#define LIST_DELIM   ","

#define USAGE    "\nUSAGE: %s <debug level> <interface list(comma seperated)> <IP>+\n" \
                 "  This will initialize netcap with a 'debug level',\n" \
                 "  configure the interface list, and then check with,\n" \
                 "  netcap interface each IP should go out on.\n\n"

static struct _test
{
    int debug_level;

    int num_intf;
    netcap_intf_string_t intf_array[NETCAP_MAX_INTERFACES];
    
    int ip_count;
    struct in_addr* ip_array;
} _test = 
{
    .debug_level   10,
    .num_intf       0,
    .ip_count       0,
    .ip_array    NULL
};

static int _parse_args ( int argc, const char* argv[] );
static int _init       ( void );
static int _run        ( void );
static int _cleanup    ( void );

int main( int argc, const char* argv[] )
{
    int _critical_section() {
        if ( _run() < 0 ) return errlog( ERR_CRITICAL, "_run\n" );
        
        return 0;
    }
    
    int ret;

    if ( libmvutil_init() < 0 ) fprintf( stderr, "Unable to initialize output\n" );
    
    if ( _parse_args( argc, argv ) < 0 ) return errlog( ERR_CRITICAL, "_parse_args\n" );
        
    if ( _init() < 0 ) return errlog( ERR_CRITICAL, "_init\n" );
    
    ret = _critical_section();
    
    if ( _cleanup() < 0 ) return errlog( ERR_CRITICAL, "_cleanup\n" );

    return ret;
}

static int _parse_args ( int argc, const char** argv )
{
    if ( argc < MINIMUM_ARGS ) return errlog( ERR_CRITICAL, USAGE, argv[0] );
    
    long tmp;
    int c;

    _test.num_intf = 0;
    bzero( &_test.intf_array, sizeof( _test.intf_array ));

    tmp = strtol( argv[DEBUG_INDEX], (NULL), 10 );

    if (( tmp < 0 ) || ( tmp > 20 ) ) {
        return errlog( ERR_CRITICAL, "Unable to parse the debug level '%s'", argv[DEBUG_INDEX] );
    }
    _test.debug_level = tmp;
    debug_set_mylevel( _test.debug_level );
    
    char* tok = NULL;
    if (( tok = malloc( strnlen( argv[INTERFACE_LIST_INDEX], INTERFACE_LIST_MAX ))) == NULL ) {
        return errlogmalloc();
    }
    
    strncpy( tok, argv[INTERFACE_LIST_INDEX], strnlen( argv[INTERFACE_LIST_INDEX], INTERFACE_LIST_MAX ));
    char* data = NULL;

    _test.num_intf = 0;
    for ( c = 0 ; c < NETCAP_MAX_INTERFACES ; c++ ) {
        char* token;
        if (( token = strtok_r(( c == 0 ) ? tok : NULL, LIST_DELIM, &data )) == NULL ) break;
        debug( 5, "Adding interface '%s'\n", token );
        strncpy( _test.intf_array[c].s, token, sizeof( _test.intf_array[c] ));
        _test.num_intf++;
    }
    
    free( tok );

    /* The first argument is the name of the program */
    _test.ip_count = argc - IP_START;

    if (( _test.ip_array = malloc(( _test.ip_count ) * sizeof ( _test.ip_array[0] ))) == NULL ) {
        return errlogmalloc();
    }
    
    for ( c = 0 ; c < _test.ip_count ; c++ ) {
        if ( inet_aton( argv[c + IP_START], &_test.ip_array[c] ) < 0 ) {
            return errlog( ERR_CRITICAL, "Unable to parse the ip '%s'\n", argv[c + IP_START] );
            
        }
    }
    
    return 0;
}

static int _init       ( void )
{
    int c;
    pthread_t id;

    for ( c = 0 ; c < DEBUG_MAX_PKGS ; c++ ) {
        debug_set_level( c, _test.debug_level );
    }

    if ( netcap_init( 0 ) < 0 ) return errlog( ERR_CRITICAL, "_netcap_init\n" );

    if ( pthread_create( &id, NULL, netcap_sched_donate, NULL )) return perrlog( "pthread_create" );

    return 0;
}

static int _run        ( void )
{    
    int c;
    
    if ( netcap_interface_configure_intf( _test.intf_array, _test.num_intf ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_interface_configure_intf\n" );
    }
    
    for ( c = 0 ; c < _test.ip_count ; c++ ) {
        netcap_intf_t intf;
        char name[IFNAMSIZ];
        struct in_addr* ip;
        int ret;

        ip = &_test.ip_array[c];

        /* src_intf is only important for broadcast, but src_ip is unused, just pass in anything */
        if (( ret = netcap_arp_dst_intf( &intf, NC_INTF_3, ip, ip )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_arp_dst_intf\n" );
            continue;
        }
        
        if ( NETCAP_ARP_NOERROR == ret ) {
            debug( 0, "Unable to lookup the address %s\n", unet_inet_ntoa( ip->s_addr ));
            continue;
        }
        
        if ( netcap_interface_intf_to_string( intf, name, sizeof( name )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_interface_intf_to_string\n" );
            continue;
        }

        debug( 2, "The IP %s is going out on the interface (%d,%s,%d)\n", unet_inet_ntoa( ip->s_addr ),
               intf, name, netcap_interface_intf_to_index( intf ));
               
    }
        
    return 0;
}

static int _cleanup    ( void )
{
    if ( netcap_cleanup() < 0 ) errlog( ERR_CRITICAL, "netcap_cleanup\n" );

    free( _test.ip_array );
    _test.ip_array = NULL;
    _test.ip_count = 0;

    return 0;
}


