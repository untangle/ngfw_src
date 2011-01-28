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
#include "netcap_interface.h"

#include <stdlib.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <net/if.h>
#include <pthread.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sysfs/libsysfs.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>
#include <mvutil/utime.h>

#include "libnetcap.h"

#include "netcap_arp.h"
#include "netcap_globals.h"
#include "netcap_init.h"
#include "netcap_sched.h"
#include "netcap_intf_db.h"

#define BROADCAST_MASK          htonl(0xFF000000)

/* This is in network byte order */
#define MULTICAST_MASK          htonl(0xF0000000)
#define MULTICAST_FLAG          htonl(0xE0000000)

#define LOCAL_HOST              htonl(0x7F000001)

/* The interface information database must be cleaned up. */
#define BRIDGE_PREFIX            "br"

/* XXXX brif is magic string, it is normally SYSFS_BRIDGE_ATTR, but the header that has this
 * doesn't have that value set */
#ifndef SYSFS_BRIDGE_ATTR
#define SYSFS_BRIDGE_ATTR        "bridge"
#endif

#ifndef SYSFS_BRIDGE_PORT_SUBDIR
#define SYSFS_BRIDGE_PORT_SUBDIR "brif"
#endif

#ifndef SYSFS_CLASS_NET
#define SYSFS_CLASS_NET "net"
#endif

#define SYSFS_CLASS_NET_DIR  "/sys/class/net"

#define SYSFS_ATTRIBUTE_INDEX  "ifindex"

#define NETCAP_MARK_INTF_MAX    8 // temp lowered
#define NETCAP_MARK_INTF_MASK   0xFF

// lookup maximum for the table
#define NETCAP_LOOKUP_MAX       129

// Wait this amount of time before freeing an old bridge configuration
#define _GARBAGE_DELAY_USEC     SEC_TO_USEC( 20 )

// Wait this amount of time on shutdown before freeing all of the interface conf
#define _CLEANUP_DELAY_USEC     SEC_TO_USEC( 2 )

#define _GARBAGE_ID     0x24BD74E4

typedef struct
{
    /* ID should be set to _GARBAGE_ID.  This is a tag on the trash to
     * insure the cleanup function is not being called with random
     * pointers, or it is not called with the same pointer twice.
     */
    unsigned int      id;
    netcap_intf_db_t* db;
} _garbage_t;

/*
 * Interface database. (eg. eth,eth1) to address map
 */
static struct
{
    /* Interface database */
    netcap_intf_db_t* db;

    /* Interface array, this is a cache of the values from configure_intf so the
     * user doesn't have to run configure_intf every time they refresh all of the
     * settings */
    netcap_intf_string_t intf_name_array[NETCAP_MAX_INTERFACES];

    /* Interface marking index array */
    netcap_intf_t        intf_array[NETCAP_MAX_INTERFACES];

    /* Number of interfaces in intf_name_array */
    int intf_count;

    /* Lookup up table to convert bits to an interface index.  EG _mark_lookup_table[8] -> 3 */
    char mark_lookup_table[NETCAP_LOOKUP_MAX];

    /* Mutex for reconfiguring the interface database.  This
     * guarantees that the database isn't being reconfigured twice at
     * the same time.  According to
     * http://www.gnu.org/software/libc/manual/html_node/Atomic-Types.html,
     * writing all pointer types is atomic.  This means most
     * operations here do not need to use this mutex because they deal
     * with an instance of the interface database which gets freed in
     * the scheduler at least 10 seconds later.  (If the function has
     * returned at that point then there is something far more serious
     * going on.)
     */
    pthread_mutex_t  mutex;
} _interface =
{
    .db              = NULL,
    .intf_count      = 0,
    .mutex           = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP
};

typedef struct
{
    int index;
    netcap_intf_string_t name;
} _intf_index_name_t;

static int _update_intf_info         ( void );
static int _update_bridge_devices    ( netcap_intf_db_t* db );
static int _update_bridge_device     ( netcap_intf_db_t* db, netcap_intf_info_t* bridge_intf_info,
                                       struct sysfs_class_device* dev );

/* Retrieve a list of all of the interfaces names and their corresponding indices */
static int _get_interface_list       ( _intf_index_name_t* intf_array, int size );

/* Retrieve and update all of the aliases for all of the interfaces */
static int _update_aliases           ( netcap_intf_db_t* db, int sockfd );

/* This retrieves the interface info, it will look into a bridge if the interface name is
 * in a bridge */
static netcap_intf_info_t* _get_intf_info  ( netcap_intf_db_t* db, char* name );

/**
 * A scheduled function to delay the deletion of bridge configurations.  This way
 * the bridge interfaces can be reconfigured without requiring a lock around all arp
 * functions.
 */
static void _empty_garbage       ( void* arg );

/* return 1 if this interface should be ignored */
static int _is_ignore_interface( char* name );

/**
 *
 */
int netcap_interface_init ( void )
{
    /* Build the lookup table */
    int c = 0;
    int bit = 1;
    int index = 1;
    for ( c = 0 ; c < NETCAP_LOOKUP_MAX ; c++ ) {
        if ( c == bit ) {
            _interface.mark_lookup_table[c] = index;
            index++;
            bit = bit << 1;
        } else {
            _interface.mark_lookup_table[c] = 0;
        }
    }

    if ( _update_intf_info() < 0 ) return errlog( ERR_CRITICAL, "_update_intf_info\n" );


    return 0;
}

/**
 *
 */
int netcap_interface_cleanup( void )
{
    netcap_intf_db_t* db = _interface.db;

    _interface.db = NULL;

    if ( db != NULL ) {
        _garbage_t* garbage = NULL;
        if (( garbage = malloc( sizeof( *garbage ))) == NULL ) return errlogmalloc();
        garbage->id   = _GARBAGE_ID;
        garbage->db   = db;
        netcap_sched_event( _empty_garbage, garbage, _CLEANUP_DELAY_USEC );
    }

    return 0;
}

int netcap_interface_update_address( void )
{
    int _critical_section( void ) {
        /* Nothing to do if netcap is not initialized */
        if ( !netcap_is_initialized()) {
            debug( 4, "Netcap is not initialized\n" );
            return 0;
        }

        if ( _update_intf_info() < 0 ) return errlog( ERR_CRITICAL, "_update_intf_info\n" );

        return 0;
    }
    int ret;

    if ( pthread_mutex_lock( &_interface.mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    ret = _critical_section();

    if ( pthread_mutex_unlock( &_interface.mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );

    return ret;
}

/* Setup the mapping between netcap interfaces and interface info */
int netcap_interface_configure_intf( netcap_intf_t* intf_array, netcap_intf_string_t* intf_name_array, int intf_count )
{
    if ( intf_name_array == NULL || intf_array == NULL || intf_count < 0 ||
         intf_count > NETCAP_MAX_INTERFACES ) {
        return errlog( ERR_CRITICAL, "Invalid argument %#10x %d\n", intf_name_array, intf_count );
    }

    debug( 4, "INTERFACE: Reconfiguring netcap interface mapping %d interfaces.\n", intf_count );

    int _critical_section( void ) {
        /* Copy in the new interface array */
        bzero( &_interface.intf_name_array, sizeof( _interface.intf_name_array ));
        bzero( &_interface.intf_array, sizeof( _interface.intf_array ));
        memcpy( &_interface.intf_name_array, intf_name_array, intf_count * sizeof( netcap_intf_string_t ));
        memcpy( &_interface.intf_array, intf_array, intf_count * sizeof( netcap_intf_t ));
        _interface.intf_count = intf_count;

        /* Update the address */
        return _update_intf_info();
    }

    int ret = 0;

    if ( pthread_mutex_lock( &_interface.mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    if (( ret = _critical_section()) < 0 ) {
        errlog( ERR_CRITICAL, "_critical_section\n" );
        bzero( &_interface.intf_name_array, sizeof( _interface.intf_name_array ));
        _interface.intf_count = 0;
    }

    if ( pthread_mutex_unlock( &_interface.mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );

    return ret;
}


/* Returns 1 if addr is a broadcast address,
 * Use index > 0 if you want to lookup for a particular index
 */
int netcap_interface_is_broadcast ( in_addr_t addr, int index )
{
    if ((((unsigned int)addr) & BROADCAST_MASK ) == BROADCAST_MASK ) return 1;

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) return errlog( ERR_CRITICAL, "interface.db is not initialized\n" );

    if ( index == 0 ) {
        int c;
        for ( c = 0 ; c < db->intf_count ; c++ ) {
            netcap_intf_info_t* info = db->intf_to_info[c];
            int d;

            if ( info == NULL || ( !info->is_valid ) || ( info->data_count <= 0 ) ||
                 ( info->data == NULL )) {
                continue;
            }

            for( d = 0 ; ( info->data != NULL ) && ( d < info->data_count ) ; d++ ) {
                if ( addr == info->data[d].broadcast.s_addr ) return 1;
            }
        }
    } else {
        netcap_intf_info_t* info;
        int d;

        if (( info = netcap_intf_db_index_to_info( db, index )) == NULL ) {
            return errlog( ERR_WARNING, "netcap_intf_db_index_to_info %d\n", index );
        }

        if (( !info->is_valid ) || ( info->data_count <= 0 ) || ( info->data == NULL )) return 0;

        for( d = 0 ; ( info->data != NULL ) && ( d < info->data_count ) ; d++ ) {
            if ( addr == info->data[d].broadcast.s_addr ) return 1;
        }
    }
    return 0;
}

/* Returns 1 if it is a multicast, 0 otherwise */
int netcap_interface_is_multicast (in_addr_t addr)
{
    if ((((unsigned int)addr) & MULTICAST_MASK ) == MULTICAST_FLAG ) return 1;

    return 0;
}

int netcap_interface_count (void)
{
    return _interface.intf_count;
}

/* Convert an interface mark to a netcap interface
 * (the netcap interfaces are equivalent to the netcap interfaces, so this is really just
 * for verification).
 */
int  netcap_interface_mark_to_intf(int nfmark, netcap_intf_t* intf)
{
    if ( intf == NULL ) return errlogargs();

    *intf = 0;

    nfmark &= NETCAP_MARK_INTF_MASK;

    /* Now convert the mark from a bit to a mark */
    if ( nfmark < 1 || nfmark >= NETCAP_LOOKUP_MAX ) {
        return errlog( ERR_CRITICAL, "Invalid interface mark[%d]\n", nfmark );
    }

    nfmark = _interface.mark_lookup_table[nfmark];

    if ( nfmark <= 0 || nfmark > NETCAP_MARK_INTF_MAX ) {
        return errlog( ERR_CRITICAL, "Invalid interface mark[%d]\n", nfmark );
    }

    /* Map the marking to the corresponding interface */
    *intf = nfmark;

    return 0;
}

int netcap_interface_intf_to_string ( netcap_intf_t intf, char *intf_str, int str_len )
{
    if (( str_len < sizeof( netcap_intf_string_t )) || ( intf_str == NULL )) return errlogargs();

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) return errlog( ERR_CRITICAL, "interface.db is not initialized\n" );

    netcap_intf_info_t* tmp;
    int ret = 0;

    if (( tmp = netcap_intf_db_intf_to_info( db, intf )) == NULL ) {
        return errlog( ERR_WARNING, "netcap_intf_db_intf_to_info %d\n", intf );
    }
    else strncpy( intf_str, tmp->name.s, sizeof( netcap_intf_string_t ));

    return ret;
}

int netcap_interface_string_to_intf ( char *intf_name, netcap_intf_t *intf )
{
    if ( intf_name == NULL || intf_name[0] == '\0' || intf == NULL ) return errlogargs();

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) return errlog( ERR_CRITICAL, "interface.db is not initialized\n" );

    netcap_intf_info_t* tmp;
    *intf = NF_INTF_UNKNOWN;

    if (( tmp = netcap_intf_db_name_to_info( db, (netcap_intf_string_t*)intf_name )) == NULL ) {
        return errlog( ERR_WARNING, "netcap_intf_db_name_to_info\n" );
    }

    *intf = tmp->netcap_intf;

    return 0;
}

int netcap_interface_get_data       ( char* name, netcap_intf_address_data_t* data, int data_size )
{
    netcap_intf_info_t* info = NULL;

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) return errlog( ERR_CRITICAL, "interface.db is not initialized\n" );

    /* Validate all of the arguments */
    if ( name == NULL || data == NULL || data_size <= sizeof( *data )) return errlogargs();
    if ( strnlen( name, sizeof( netcap_intf_string_t ) + 1 ) > sizeof( netcap_intf_string_t )) {
        return errlogargs();
    }

    if (( info = _get_intf_info( db, name )) == NULL ) {
        return errlog( ERR_CRITICAL, "Nothing is known about the interface '%s'\n", name );
    }

    if (( info->data_count <= 0 ) || ( NULL == info->data )) {
        return errlog( ERR_CRITICAL, "There are no address for interface '%s'\n", name );
    }

    if ( info->data_count > NETCAP_MAX_INTERFACES ) {
        return errlog( ERR_CRITICAL, "Interface database is corrupted for interface '%s'\n", name );
    }

    if ( info->data_count > ( data_size / sizeof( *data ))) return errlogargs();

    memcpy( data, info->data, info->data_count * sizeof( *data ));

    return info->data_count;
}

netcap_intf_t netcap_interface_index_to_intf( int index )
{
    netcap_intf_info_t* tmp;

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) {
        errlog( ERR_CRITICAL, "interface.db is not initialized\n" );
        return NF_INTF_UNKNOWN;
    }

    if (( tmp = netcap_intf_db_index_to_info( db, index )) == NULL ) {
        errlog( ERR_WARNING, "netcap_intf_db_index_to_info %d\n", index );
        return NF_INTF_UNKNOWN;
    }

    return tmp->netcap_intf;
}

netcap_intf_db_t* netcap_interface_get_db    ( void )
{
    return _interface.db;
}

int           netcap_interface_other_intf    ( netcap_intf_t* intf, netcap_intf_t src )
{
    return 0;
}

int netcap_interface_intf_to_index( netcap_intf_t netcap_intf )
{
    netcap_intf_info_t* tmp = NULL;

    netcap_intf_db_t* db = _interface.db;
    if ( db == NULL ) return errlog( ERR_CRITICAL, "interface.db is not initialized\n" );

    if (( tmp = netcap_intf_db_intf_to_info( db, netcap_intf )) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_intf_db_intf_to_info\n" );
    }

    return tmp->index;
}

int netcap_interface_dst_intf       ( netcap_session_t* session )
{
    if ( session == NULL ) return errlogargs();
    
    if ( session->srv.intf != NF_INTF_UNKNOWN ) {
        debug( 10, "INTERFACE: (%10u) Destination interface is already known %d\n", 
               session->session_id, session->srv.intf );
        return 0;
    }

    /* Need to determine the redirected destination interface, not the
     * original destination interface */
    /* From the reply, the source is where this session is heading, and the destination is
     * where it is coming from, the source is unused in this function, so it doesn't really
     * matter. */
    struct in_addr dst = { .s_addr = session->nat_info.reply.src_address };
    struct in_addr src = { .s_addr = session->nat_info.reply.dst_address };

    if ( netcap_arp_dst_intf( &session->srv.intf, session->cli.intf, &src, &dst ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_arp_dst_intf (%s -> %s)\n", unet_next_inet_ntoa( src.s_addr ), unet_next_inet_ntoa( dst.s_addr ) );
    }
    
    debug( 10, "INTERFACE: (%10u) Session (%s -> %s) is going out %d\n", 
           session->session_id, unet_next_inet_ntoa( src.s_addr ), unet_next_inet_ntoa( dst.s_addr ), 
           session->srv.intf );

    return 0;
}

/* This should be called with the mutex lock */
static int _update_intf_info( void )
{
    int ret = 0;
    int sockfd;
    netcap_intf_db_t* db = NULL;

    int _critical_section( void ) {
        int i;
        int num_intf;

        _intf_index_name_t intf_index_array[NETCAP_MAX_INTERFACES];

        bzero( intf_index_array, sizeof( intf_index_array ));

        if (( num_intf = _get_interface_list( intf_index_array, sizeof( intf_index_array ))) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_interface_list\n" );
        }

        for ( i = 0 ; i < num_intf ; i++ ) {
            netcap_intf_info_t intf_info;
            _intf_index_name_t* in;

            in = &intf_index_array[i];

            bzero( &intf_info, sizeof ( netcap_intf_info_t ));

            if ( in->index <= 0  ) {
                errlog( ERR_CRITICAL, "invalid index[%d] at position %d\n", in->index, i );
                continue;
            }

            if ( _is_ignore_interface( in->name.s ) == 1 ) {
                debug( 10, "INTERFACE: skipping %s\n", in->name.s );
                continue;
            }

            debug( 3, "INTERFACE: Retrieving information for device '%s'\n", in->name.s );

            intf_info.is_valid = 1;
            intf_info.index = in->index;

            if ( strncmp( in->name.s, "lo", sizeof( "lo" )) == 0 ) {
                debug( 10, "INTERFACE: Loopback interface at index %d\n", i );
                intf_info.netcap_intf = NC_INTF_LOOPBACK;
                intf_info.is_loopback = 1;
            } else {
                intf_info.is_loopback = 0;
            }

            /* Copy in the interface name */
            strncpy( intf_info.name.s, in->name.s, sizeof( intf_info.name ));

            /* Some interfaces may not have interface data */
            if ( netcap_intf_db_fill_data( &intf_info, sockfd, &in->name ) < 0 ) {
                return errlog( ERR_CRITICAL, "netcap_intf_db_fill_info\n" );
            }

            /* Insert the device into the hash table, this automatically checks for duplicates. */
            if ( netcap_intf_db_add_info( db, &intf_info ) < 0 ) {
                return errlog( ERR_CRITICAL, "netcap_intf_db_set_info\n" );
            }
        }

        /* Update the bridge devices */
        if ( _update_bridge_devices( db ) < 0 ) return errlog( ERR_CRITICAL, "_update_bridge_devices\n" );

        /* Update the aliases after initializing the bridges */
        if ( _update_aliases( db, sockfd ) < 0 ) return errlog( ERR_CRITICAL, "_update_aliases\n" );

        /* Update the interface array */
        if (( _interface.intf_count > 0 ) &&
            ( netcap_intf_db_configure_intf( db, _interface.intf_array, _interface.intf_name_array,
                                             _interface.intf_count ) < 0 )) {
            return errlog( ERR_CRITICAL, "netcap_intf_db_configure_intf\n" );
        }

        /* If the database is not null, then schedule it to be deleted later */
        if ( _interface.db != NULL ) {
            _garbage_t* garbage = NULL;
            if (( garbage = malloc( sizeof( *garbage ))) == NULL ) return errlogmalloc();
            garbage->id   = _GARBAGE_ID;
            garbage->db   = _interface.db;
            netcap_sched_event( _empty_garbage, garbage, _GARBAGE_DELAY_USEC );
        }

        /* Setup the new interface database */
        _interface.db = db;

        return 0;
    }

    debug( 2, "INTERFACE: Updating interface addresses\n" );

    /* Open up the socket for querying the interfaces */
    if (( sockfd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) return perrlog( "socket" );

    /* Create the new interface database */
    if (( db = netcap_intf_db_create( INTF_DB_NO_LOCKS )) == NULL ) {
        return errlog( ERR_CRITICAL, "netcap_intf_db_create\n" );
    }

    /* Configure the new interface database */
    ret = _critical_section();
    if ( close( sockfd ) < 0 ) perrlog( "close" );

    /* If there was an error, free the memory that the interface database used. */
    if (( ret < 0 ) && ( netcap_intf_db_raze( db ) < 0 )) errlog( ERR_CRITICAL, "netcap_intf_db_raze\n" );

    return ret;
}

/**
 * Update the info for all of the bridges all of the bridges
 */

static int _update_bridge_devices( netcap_intf_db_t* db )
{
    int c;
    int ret = 0;
    struct sysfs_class_device* dev;
    char sysfs_bridge_path[SYSFS_PATH_MAX];
    struct stat file_stat_buf;

    for ( c = 0 ; c < NETCAP_MAX_INTERFACES ; c++ ) {
        netcap_intf_info_t* intf_info = &db->info[c];

        if ( !intf_info->is_valid ) {
            debug( 11, "Skipping invalid interface %d\n", c );
            continue;
        }


        /* Check if this is a bridge using sysfs, it is a bridge if
         * /sys/class/net/<ifname>/bridge exist. */
        snprintf( sysfs_bridge_path, sizeof( sysfs_bridge_path ), "%s/%s/%s", SYSFS_CLASS_NET_DIR,
                  intf_info->name.s, SYSFS_BRIDGE_ATTR );

        if ( stat( sysfs_bridge_path, &file_stat_buf ) < 0 ) {
            if ( errno == ENOENT ) {
                debug( 4, "Ignoring non-bridge interface %s\n", intf_info->name.s );
            } else {
                errlog( ERR_WARNING, "Unable to stat sysfs bridge information for %s, %s\n", 
                        intf_info->name.s, errstr );
            }
            continue;
        }

        if ( strncmp( intf_info->name.s, BRIDGE_PREFIX, sizeof( BRIDGE_PREFIX ) - 1 ) != 0 ) {
            debug( 4, "Ignoring non-bridge interface %s\n", intf_info->name.s );
            errlog( ERR_CRITICAL, "BRIDGE LOOKUP IS DISABLED, talk to rbscott\n" );
            continue;
        }

        debug( 3, "INTERFACE: Retrieving interfaces for bridge '%s'\n", intf_info->name.s );

        if (( dev = sysfs_open_class_device( SYSFS_CLASS_NET, intf_info->name.s )) == NULL ) {
            return perrlog( "sysfs_open_class_device" );
        }

        /* dev must be closed */
        ret = _update_bridge_device( db, intf_info, dev );

        // RBS: 10/16/05 Probably should check for leaks, it doesn't
        // look like the value from get_class_device has to be closed,
        // in fact if it is it causes errors.  This may be the
        // incorrect function to use, it seems if it used
        // open_class_device then it would have to be closed.

        // RBS: 04/05/06: get_device seems buggie, and didn't work
        // when the interface changed switching to sysfs_open_device
        // instead which seems a little bit more stable.  This one
        // does require the device to be closed.
        sysfs_close_class_device( dev );

        if ( ret < 0 ) return errlog( ERR_CRITICAL, "_update_bridge_device\n" );
    }

    return 0;
}

/**
 * Register any of bridge devices for bridge_info.
 */
static int _update_bridge_device( netcap_intf_db_t* db, netcap_intf_info_t* bridge_info,
                                  struct sysfs_class_device* dev )
{
    char path[SYSFS_PATH_MAX];
    DIR* dir = NULL;

    int _critical_section( void ) {
        struct dirent* dir_entry;
        netcap_intf_info_t* intf_info;

        /* Reset the number of bridges */
        bridge_info->bridge_info->intf_count = 0;

        /* Iterate through all of the directory entries */
        while (( dir_entry = readdir( dir )) != NULL ) {
            /* Ignore the . and .. directories */
            if (( strncmp( dir_entry->d_name, ".", sizeof( "." ) + 1 ) == 0 ) ||
                ( strncmp( dir_entry->d_name, "..", sizeof( ".." ) + 1 ) == 0 )) {
                continue;
            }

            debug( 4, "INTERFACE: Bridge[%s] contains '%s'\n", bridge_info->name.s, dir_entry->d_name );

            if (( intf_info = ht_lookup( &db->name_to_info, dir_entry->d_name )) == NULL ) {
                return errlog( ERR_CRITICAL, "INTERFACE: table does not contain '%s'\n", dir_entry->d_name );
            }

            if ( intf_info->bridge != NULL ) {
                return errlog( ERR_CRITICAL, "INTERFACE: %s is already in a bridge\n", intf_info->name.s );
            }

            if ( intf_info->bridge_info != NULL ) {
                errlog( ERR_CRITICAL, "INTERFACE: %s is a bridge\n", intf_info->name.s );
            }

            intf_info->is_valid  = 1;
            intf_info->bridge    = bridge_info;
            bridge_info->bridge_info->ports[bridge_info->bridge_info->intf_count++] = intf_info;
        }

        return 0;
    }

    int ret = 0;

    /* XXX This was used more extensively before, the mac_address and broadcast addresses
     * are no longer needed, and now it is just a memory block */
    if ( bridge_info->bridge_info != NULL ) {
        errlog( ERR_WARNING, "Bridge already has configuration data, freeing\n" );
        free( bridge_info->bridge_info );
        bridge_info->bridge_info = NULL;
    }

    if ( netcap_arp_configure_bridge( db, bridge_info ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_arp_configure_bridge\n" );
    }

    /* Double check to make sure the memory was allocated */
    if ( bridge_info->bridge_info == NULL ) return errlog( ERR_CRITICAL, "netcap_arp_configure_bridge\n" );

    snprintf( path, sizeof( path ), "%s/%s", dev->path, SYSFS_BRIDGE_PORT_SUBDIR );

    if (( dir = opendir( path )) == NULL ) return perrlog( "sysfs_open_directory" );

    ret = _critical_section();

    if ( closedir( dir ) < 0 ) perrlog( "closedir" );

    return ret;
}

static int _get_interface_list       ( _intf_index_name_t* intf_array, int size )
{
    DIR* interface_directory = NULL;
    struct sysfs_attribute* attribute = NULL;

    int _critical_section( void ) {
        struct dirent* dir_entry;
        char path[SYSFS_PATH_MAX];
        int index = 0;
        int ifindex = 0;
        int c;

        while (( dir_entry = readdir( interface_directory )) != NULL ) {
            /* Ignore the . and .. directories */
            if (( strncmp( dir_entry->d_name, ".", sizeof( "." ) + 1 ) == 0 ) ||
                ( strncmp( dir_entry->d_name, "..", sizeof( ".." ) + 1 ) == 0 ) ||
                // For new sysfs, entries are links not directories.  Since there really can't
                // be any files in this directory, just removed the check. (bug3883)
                // ( dir_entry->d_type != DT_DIR ) || 
                ( _is_ignore_interface( dir_entry->d_name ) == 1 )) {
                continue;
            }

            debug( 4, "INTERFACE: Found interface: [%s]\n", dir_entry->d_name );

            snprintf( path, sizeof( path ), "%s/%s/%s", SYSFS_CLASS_NET_DIR,
                      dir_entry->d_name, SYSFS_ATTRIBUTE_INDEX );

            if (( attribute = sysfs_open_attribute( path )) == NULL ) {
                return perrlog( "sysfs_open_attribute" );
            }

            if ( sysfs_read_attribute( attribute ) < 0 ) return perrlog( "sysfs_read_attribute" );

            /* Convert the value to an int, (assuming no errors) */
            ifindex = atoi( attribute->value );

            if ( ifindex <= 0 ) {
                errlog( ERR_CRITICAL, "Invalid interface index for interface[%s]\n", dir_entry->d_name );
                sysfs_close_attribute( attribute );
                continue;
            }

            for ( c = 0 ; c < index ; c++ ) {
                if ( intf_array[c].index == ifindex ) {
                    errlog( ERR_CRITICAL, "Index %d used twice, continuing\n", ifindex );
                    sysfs_close_attribute( attribute );
                    continue;
                }
            }

            intf_array[index].index = ifindex;
            strncpy( intf_array[index++].name.s, dir_entry->d_name, sizeof( netcap_intf_string_t ));

            sysfs_close_attribute( attribute );
            attribute = NULL;

            if ( index >= size ) return errlog( ERR_CRITICAL, "input array is not large enough\n" );
        }

        return index;
    }

    /* Convert the byte size to the number of items */
    size = size / sizeof( _intf_index_name_t );

    int ret = 0;
    if (( interface_directory = opendir( SYSFS_CLASS_NET_DIR )) == NULL ) return perrlog( "opendir" );

    ret = _critical_section();

    if ( closedir( interface_directory ) < 0 ) perrlog( "closedir" );

    if ( attribute != NULL ) sysfs_close_attribute( attribute );

    return ret;
}

static int _update_aliases           ( netcap_intf_db_t* db, int sockfd )
{
    struct ifreq ifreq_array[NETCAP_MAX_INTERFACES];
    struct ifconf conf;
    int c = 0;

    conf.ifc_len = sizeof( ifreq_array );
    conf.ifc_req = ifreq_array;

    bzero( ifreq_array, sizeof( ifreq_array ));

    if ( ioctl( sockfd, SIOCGIFCONF, &conf ) < 0 ) return perrlog( "ioctl" );

    if ( sizeof( ifreq_array ) == conf.ifc_len ) {
        errlog( ERR_WARNING, "SIOCGIFCONF overflowed, using first %d interfaces\n", NETCAP_MAX_INTERFACES );
    } else if ( sizeof( ifreq_array ) < conf.ifc_len ) {
        errlog( ERR_WARNING, "SIOCGIFCONF returns invalid length %d\n", conf.ifc_len );
        c = NETCAP_MAX_INTERFACES;
    } else {
        c = conf.ifc_len / sizeof( ifreq_array[0] );
    }

    for (  ;  c-- > 0 ; ) {
        struct ifreq* ifreq = &ifreq_array[c];
        netcap_intf_string_t name;
        int d;

        /* Retrieve the device information using the name */
        bzero( &name, sizeof( name ));

        /* Split the name using the : character to look for aliases */
        /* Use the -1 to guarantee that there is always a '\0' (it is bzerod above) */
        for ( d = 0 ; d < sizeof( name ) - 1 ; d++ ) {
            switch( ifreq->ifr_name[d] ) {
            case ':':
            case '\0':
                name.s[d] = '\0';
                break;
            default:
                name.s[d] = ifreq->ifr_name[d];
                break;
            }

            if ( '\0' == name.s[d] ) break;
        }

        netcap_intf_info_t* intf_info = netcap_intf_db_name_to_info( db, &name );

        if ( intf_info == NULL ) {
            debug( 0, "Unknown interface '%s', continuing\n", ifreq->ifr_name );
            continue;
        }

        if ( intf_info->bridge != NULL ) {
            errlog( ERR_CRITICAL, "The interface '%s' is in a bridge, continuing\n", ifreq->ifr_name );
            continue;
        }

        switch( netcap_intf_db_fill_data( intf_info, sockfd, (netcap_intf_string_t*)ifreq->ifr_name )) {
        case 1: intf_info->is_valid = 1; break;
        case 0: break;
        default: return errlog( ERR_CRITICAL, "netcap_intf_db_fill_info\n" );
        }
    }


    return 0;
}

static netcap_intf_info_t* _get_intf_info( netcap_intf_db_t* db, char* name )
{
    netcap_intf_info_t* intf_info = NULL;

    if (( intf_info = netcap_intf_db_name_to_info( db, (netcap_intf_string_t*)name )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "netcap_intf_db_name_to_info\n" );
    }

    /* Lookup the bridge interface if necessary */
    if ( intf_info->bridge != NULL ) intf_info = intf_info->bridge;

    if ( !intf_info->is_valid ) {
        return errlog_null( ERR_CRITICAL, "The interface '%s' is not valid\n", intf_info->name.s );
    }

    return intf_info;
}

static int _is_ignore_interface( char* intf_name )
{
    if (( strncmp( intf_name, "sit",   3 ) == 0 ) ||
        ( strncmp( intf_name, "dummy", 5 ) == 0 ) ||
        ( strncmp( intf_name, "bc", 2 ) == 0 ) ||
        ( strncmp( intf_name, "bond", 4 ) == 0 ) ||
        ( strncmp( intf_name, "yam", 3 ) == 0 ) ||
        ( strncmp( intf_name, "scc", 3 ) == 0 ) ||
        ( strncmp( intf_name, "shaper", 6 ) == 0 ) ||
        ( strncmp( intf_name, "sdla", 4 ) == 0 ) ||
        ( strncmp( intf_name, "eql", 3 ) == 0 )) {
        debug( 10, "INTERFACE: skipping %s\n", intf_name );
        return 1;
    }
    
    return 0;
}

static void _empty_garbage       ( void* arg )
{
    _garbage_t* garbage = arg;
    unsigned int id;

    if ( garbage == NULL ) {
        errlogargs();
        return;
    }

    /* Set it t something else so it can't be reused. */
    id = garbage->id;
    garbage->id = ~_GARBAGE_ID;

    debug( 4, "INTERFACE: EMPTY GARBAGE %#010x/%#010x/%#010x\n", arg, id, garbage->db );

    if ( id != _GARBAGE_ID ) {
        errlog( ERR_CRITICAL, "Unable to throw away the argument %#08x, invalid id: %#08x != %#08x\n",
                garbage, id, _GARBAGE_ID );
        return;
    }

    if ( garbage->db == NULL ) errlog( ERR_WARNING, "NULL garbage\n" );
    else netcap_intf_db_raze( garbage->db );
    garbage->db = NULL;

    free( garbage );
}

