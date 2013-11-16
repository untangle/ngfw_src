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

#ifndef _NETCAP_INTF_H_
#define _NETCAP_INTF_H_

#include <netinet/if_ether.h>
#include <linux/if_packet.h>

#include "libnetcap.h"

#define INTF_DB_NO_LOCKS 0x80

struct netcap_intf_info;

/* Information pertinent to a bridge */
typedef struct 
{
    /* The MAC address for the bridge */
    struct ether_addr mac;

    /* Broadcast socket to use for this bridge (the ifindex has to be correct) */
    struct sockaddr_ll broadcast;

    /* Number of interfaces in the bridge */
    int intf_count;
    
    /* An array of all of the interfaces in the bridge, in no particular order */
    struct netcap_intf_info* ports[NETCAP_MAX_INTERFACES];
} netcap_intf_bridge_info_t;

/* Description of an interface */
typedef struct netcap_intf_info
{
    char is_valid;
    char is_loopback;

    int index;

    netcap_intf_t netcap_intf;
    netcap_intf_string_t name;

    /* Interface info for the bridge this interface participates in,
     * or NULL if it doesn't participate in a bridge. */
    struct netcap_intf_info* bridge;

    /* Parameters for ARPing devices on a bridge. (NULL if the
     * interface is not a bridge) This is initialized, configured and
     * used by netcap_route.c */
    netcap_intf_bridge_info_t* bridge_info;

    /* number of items in the data array */
    int data_count;
    
    /* An array of address data, the primary address and any aliases should go in here. */
    /* For each of these parameters, they are either the value for the interface
     * or the value for the bridge that contains the interface.  The case where an
     * interface contains an address and is in a bridge should ALWAYS be avoided. */
    netcap_intf_address_data_t* data;
} netcap_intf_info_t;

/* A database of information about all of the interfaces */
typedef struct
{
    /* Array of the values that are stored in the interface array */
    /* This is the map from "os_index - 1" to interface into */
    /* This array holds the actually data, not pointers to it */
    netcap_intf_info_t info[NETCAP_MAX_INTERFACES];

    /* Number of entries in the info array */
    int info_count;
    
    /* An array mapping a "netcap interfaces" to interface info */
    netcap_intf_info_t* intf_to_info[NETCAP_MAX_INTERFACES];

    /* Interface array, this is a cache of the values from configure_intf so the
     * user doesn't have to run configure_intf every time they refresh all of the
     * settings */
    /* XXX Not sure if this also has to be here */
    netcap_intf_string_t intf_name_array[NETCAP_MAX_INTERFACES];
    
    /* Number of interfaces in intf_name_array */
    int intf_count;

    /* Hash table that maps device names to the device info */
    ht_t name_to_info;

    /* Hash table that maps device indices to device info */
    ht_t index_to_info;
} netcap_intf_db_t;

/* Functions for allocating memory and creating interface databases */
netcap_intf_db_t*   netcap_intf_db_malloc  ( void );
int                 netcap_intf_db_init    ( netcap_intf_db_t* db, int flags );
netcap_intf_db_t*   netcap_intf_db_create  ( int flags );

/* Functions for deleting interface database structures */
int                 netcap_intf_db_free    ( netcap_intf_db_t* db );
int                 netcap_intf_db_destroy ( netcap_intf_db_t* db );
int                 netcap_intf_db_raze    ( netcap_intf_db_t* db );

/* Add info to the interface database */
int                 netcap_intf_db_add_info( netcap_intf_db_t* db, netcap_intf_info_t* intf_info );

/* Configure the mapping from netcap interfaces to interface info */ 
int                 netcap_intf_db_configure_intf( netcap_intf_db_t* db, netcap_intf_t* intf_array,
                                                   netcap_intf_string_t* intf_name_array, int intf_count );

/* Functions for retrieving an item from the interface database using a variety of keys */
netcap_intf_info_t* netcap_intf_db_index_to_info ( netcap_intf_db_t* db, int index );
netcap_intf_info_t* netcap_intf_db_intf_to_info  ( netcap_intf_db_t* db, netcap_intf_t intf );
netcap_intf_info_t* netcap_intf_db_name_to_info  ( netcap_intf_db_t* db, netcap_intf_string_t* name );

/* If necessary, add a address data structure to the address, if the
 * address for name is already inside of the interface info, the
 * address is not added
 * @return 1 - Info was either added or valid information was found.
 *         0 - No info was added because address data was not found.
 *       > 0 - an error occured.
 */
int                netcap_intf_db_fill_data( netcap_intf_info_t* intf_info, int sockfd, 
                                             netcap_intf_string_t* name );

#endif

