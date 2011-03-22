/* $HeadURL$ */

#include <stdio.h>
#include <stdlib.h>
#include <sys/ioctl.h>
#include <net/if.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/unet.h>

#include "netcap_intf_db.h"

/* Just to avoid collisions make it slightly larger */
#define IF_TABLE_SIZE           357

static __inline__ int _check_intf  ( netcap_intf_t intf )
{
    if ( intf < 1 || intf > NETCAP_MAX_INTERFACES ) {
        return errlog( ERR_CRITICAL, "Invalid interface: %d\n", intf );
    }
    
    return 0;
}

static __inline__ int _check_index    ( int index )
{
    if ( index < 1  ) return errlog( ERR_CRITICAL, "Invalid index %d\n", index );
    
    return 0;
}

static int  _add_data( netcap_intf_info_t* intf_info, netcap_intf_address_data_t* data );

/**
 * Check if it is safe to ignore an interface that doesn't have any configuration data.
 * @param intf_name Name of the interface to test.
 * @return 1 if the interface can be safely ignored, 0 otherwise.
 */
static int _is_ignore_interface( netcap_intf_string_t* intf_name );



netcap_intf_db_t* netcap_intf_db_malloc  ( void )
{
    netcap_intf_db_t* db;
    if (( db = malloc( sizeof( netcap_intf_db_t ))) == NULL ) return errlogmalloc_null();
    return db;
}

int               netcap_intf_db_init    ( netcap_intf_db_t* db, int flags )
{
    if ( db == NULL ) return errlogargs();
    bzero( db, sizeof( netcap_intf_db_t ));

    int hash_flags = ( flags & INTF_DB_NO_LOCKS ) ? HASH_FLAG_NO_LOCKS : 0;

    /* Initialize the hash table */
    /* Once these tables are populated, they are never modified in a multithreaded environment */
    if ( ht_init( &db->name_to_info, IF_TABLE_SIZE, string_hash_func, string_equ_func, hash_flags ) < 0 ) {
        return errlog( ERR_CRITICAL, "ht_init\n" );
    }

    if ( ht_init( &db->index_to_info, IF_TABLE_SIZE, int_hash_func, int_equ_func, hash_flags ) < 0 ) {
        return errlog( ERR_CRITICAL, "ht_init\n" );
    }
    
    return 0;
}

netcap_intf_db_t* netcap_intf_db_create  ( int flags )
{
    netcap_intf_db_t* db;
    if (( db = netcap_intf_db_malloc()) == NULL ) {
        return errlog_null( ERR_CRITICAL, "netcap_intf_db_malloc\n" );
    }

    if ( netcap_intf_db_init( db, flags ) < 0 ) {
        netcap_intf_db_raze( db );
        return errlog_null( ERR_CRITICAL, "netcap_intf_db_init\n" );
    }
    
    return db;
}

int               netcap_intf_db_free    ( netcap_intf_db_t* db )
{
    if ( db == NULL ) return errlogargs();

    free( db );
    return 0;
}

int               netcap_intf_db_destroy ( netcap_intf_db_t* db )
{
    if ( db == NULL ) return errlogargs();

    /* Destroy all of the bridge devices */
    int c = 0;
    for ( c = 0 ; c < NETCAP_MAX_INTERFACES ; c++ ) {
        if ( db->info[c].bridge_info != NULL ) free( db->info[c].bridge_info );
        if ( db->info[c].data != NULL ) free( db->info[c].data );
        db->info[c].bridge_info = NULL;
        db->info[c].data = NULL;
        db->info[c].data_count = 0;
    }

    /* Destroy the hash tables */
    ht_destroy( &db->name_to_info );
    ht_destroy( &db->index_to_info );

    return 0;
}

int               netcap_interface_intf_verify( netcap_intf_t intf )
{
    return _check_intf( intf );
}

int               netcap_intf_db_raze         ( netcap_intf_db_t* db )
{
    if ( db == NULL ) return errlogargs();

    if ( netcap_intf_db_destroy( db ) < 0 ) errlog( ERR_CRITICAL, "netcap_intf_db_destroy\n" );
    
    if ( netcap_intf_db_free( db ) < 0 ) errlog( ERR_CRITICAL, "netcap_intf_db_free\n" );

    return 0;
}

int               netcap_intf_db_add_info( netcap_intf_db_t* db, netcap_intf_info_t* intf_info )
{
    netcap_intf_info_t* intf_info_dst = NULL;

    if ( db == NULL || intf_info == NULL ) return errlogargs();

    if ( db->info_count < 0 ) {
        errlog( ERR_WARNING, "Data count isn't initialized, resetting to zero\n" );
        db->info_count = 0;
    }

    if ( db->info_count > NETCAP_MAX_INTERFACES ) {
        return errlog( ERR_WARNING, "Data count[%d] exceeded\n", db->info_count );
    }

    if ( _check_index( intf_info->index ) < 0 ) return errlog( ERR_CRITICAL, "_check_index\n" );

    if ( ht_lookup( &db->index_to_info, (void*)(long)intf_info->index ) != NULL ) {
        errlog( ERR_CRITICAL, "The interface[%d] is already in the database.", intf_info->index );
        return 0;
    }

    if ( ht_lookup( &db->name_to_info, intf_info->name.s ) != NULL ) {
        errlog( ERR_CRITICAL, "The interface[%s] is already in the database.", intf_info->name.s );
        return 0;
    }
    
    intf_info_dst = &db->info[db->info_count];
    memcpy( intf_info_dst, intf_info, sizeof( *intf_info ));

    if ( ht_add( &db->index_to_info, (void*)(long)intf_info_dst->index, intf_info_dst ) < 0 ) {
        return errlog( ERR_CRITICAL, "ht_add\n" );
    }

    if ( ht_add( &db->name_to_info, intf_info_dst->name.s, intf_info_dst ) < 0 ) {
        ht_remove( &db->index_to_info, (void*)(long)intf_info_dst->index );
        return errlog( ERR_CRITICAL, "ht_add\n" );
    }

    db->info_count++;

    return 0;
}

int               netcap_intf_db_configure_intf( netcap_intf_db_t* db, netcap_intf_t* intf_array, netcap_intf_string_t* intf_name_array, int intf_count )
{
    if (( intf_name_array == NULL ) || ( intf_array == NULL ) || 
        ( intf_count < 0 ) || ( intf_count > NETCAP_MAX_INTERFACES )) {
        return errlog( ERR_CRITICAL, "Invalid argument %#10x %#10x %d\n", intf_name_array, intf_array, 
                       intf_count );
    }
    
    int c;
    for ( c = 0 ; c < intf_count ; c++ ) {
        netcap_intf_string_t* intf_name;
        netcap_intf_info_t* info;
        intf_name = &intf_name_array[c];
        netcap_intf_t intf = intf_array[c];
        
        if ( strnlen( intf_name->s, sizeof( netcap_intf_string_t )) == 0 ) {
            debug( 4, "INTF DB: Ignoring unused interface at index %d\n", c );
            continue;
        }
        
        if (( info = netcap_intf_db_name_to_info( db, intf_name )) == NULL ) {
            /* This shouldn't happen in a properly configured system.
             * this could actually happen at startup if EG VPN is registered but the
             * interface is not setup yet.  (VPN transform doesn't start until after
             * the first initialization) */
            /* It is safe to ignore these interfaces. */
            if ( _is_ignore_interface( intf_name ) == 1 ) continue;

            errlog( ERR_WARNING, "Ignoring unknown interface '%s' at index %d.\n", intf_name->s, c );
            errlog( ERR_WARNING, "Interfaces may be configured incorrectly.\n" );
            continue;
        }
        
        if ( info->bridge_info != NULL ) {
            return errlog( ERR_CRITICAL, "The interface %s is a bridge.\n", info->name.s );
        }
        
        debug( 4, "INTF DB: Mapping netcap_intf %d to %d '%s'\n", intf, info->index, info->name.s );
        
        /* Establish the mapping between the interface and the info */
        db->intf_to_info[c] = info;
        info->netcap_intf   = intf;
    }
    
    /* Copy in all of the interfaces */
    bzero( db->intf_name_array, sizeof( db->intf_name_array ));
    memcpy( db->intf_name_array, intf_name_array, intf_count * sizeof( netcap_intf_string_t ));
    
    /* Set the number of interfaces */
    db->intf_count = intf_count;
    
    return 0;
}

/* Functions for retrieving an item from the interface database using a variety of keys */
netcap_intf_info_t* netcap_intf_db_index_to_info ( netcap_intf_db_t* db, int index )
{
    if ( db == NULL ) return errlogargs_null();

    if ( _check_index( index ) < 0 ) return errlog_null( ERR_CRITICAL, "_check_index %d\n", index );

    netcap_intf_info_t* intf_info = NULL;

    if (( intf_info = ht_lookup( &db->index_to_info, (void*)(long)index )) == NULL || !intf_info->is_valid ) {
        return errlog_null( ERR_CRITICAL, "Nothing is known about '%d'\n", index );
    }

    return intf_info;
}

netcap_intf_info_t* netcap_intf_db_intf_to_info  ( netcap_intf_db_t* db, netcap_intf_t intf )
{
    if ( db == NULL ) return errlogargs_null();
    
    if ( _check_intf( intf ) < 0 ) return errlog_null( ERR_CRITICAL, "_check_intf %d\n", intf );
    netcap_intf_info_t* intf_info = NULL;
            
    if ((( intf_info = db->intf_to_info[intf-1] ) != NULL ) && intf_info->is_valid ) return intf_info;
    
    /* XXX Should this print an error message */
    return NULL;
}

netcap_intf_info_t* netcap_intf_db_name_to_info  ( netcap_intf_db_t* db, netcap_intf_string_t* name )
{
    if ( db == NULL || name == NULL ) return errlogargs_null();
    netcap_intf_info_t* intf_info = NULL;

    if ( strnlen( name->s, sizeof( netcap_intf_string_t ) + 1 ) > sizeof( netcap_intf_string_t )) {
        return errlog_null( ERR_CRITICAL, "Invalid interface name" );
    }
    
    if (( intf_info = ht_lookup( &db->name_to_info, name )) == NULL || !intf_info->is_valid ) {
        if ( _is_ignore_interface( name ) == 1 ) return NULL;

        return errlog_null( ERR_CRITICAL, "Nothing is known about '%s'\n", name );
    }
    return intf_info;

}

/* Fill in the address, netmask and broadcast address for the
 * interface name, If the information already exists, it is not added
 */
int                netcap_intf_db_fill_data( netcap_intf_info_t* intf_info, int sockfd, netcap_intf_string_t* name )
{
    struct ifreq ifr;
    struct in_addr* ioctl_data = &((struct sockaddr_in*)(&ifr.ifr_addr))->sin_addr;
    netcap_intf_address_data_t data;
    
    bzero( &data, sizeof( data ));

    if ( intf_info == NULL || name == NULL ) return errlogargs();

    /* Prepare the structure for the IOCTL */
    strncpy( ifr.ifr_name, name->s, sizeof( ifr.ifr_name ));
    
    if ( ioctl( sockfd, SIOCGIFADDR, &ifr ) < 0 ) {
        if ( errno == EADDRNOTAVAIL ) return 0;
        else return perrlog( "ioctl" );
    } else {
        memcpy( &data.address, ioctl_data, sizeof( data.address ));
    }
    
    /* Get the netmask */
    if ( ioctl( sockfd, SIOCGIFNETMASK, &ifr ) < 0 ) {
        if ( errno == EADDRNOTAVAIL ) return 0;
        else return perrlog( "ioctl" );
    } else {
        memcpy( &data.netmask, ioctl_data, sizeof( data.netmask ));
    }
    
    /* Get the broadcast address */
    if ( ioctl( sockfd, SIOCGIFBRDADDR, &ifr ) < 0 ) {
        if ( errno == EADDRNOTAVAIL ) return 0;
        else return perrlog( "ioctl" );
    } else {
        memcpy( &data.broadcast, ioctl_data, sizeof( data.broadcast ));
    }

    debug( 4, "INTF DB: Device configuration[%s,%s] address %s netmask %s broadcast %s\n", 
           intf_info->name.s, name->s,
           unet_next_inet_ntoa( data.address.s_addr ),
           unet_next_inet_ntoa( data.netmask.s_addr ),
           unet_next_inet_ntoa( data.broadcast.s_addr ));

    if ( _add_data( intf_info, &data ) < 0 ) return errlog( ERR_CRITICAL, "_add_data\n" );
    
    return 1;
}



static int  _add_data( netcap_intf_info_t* intf_info, netcap_intf_address_data_t* data )
{
    /* Determine if the data is already in there */
    int c = 0;

    if ( intf_info->data_count <= 0 ) {
        if ( intf_info->data != NULL ) errlog( ERR_CRITICAL, "DB is invalid, setting to NULL\n" );
        intf_info->data       = NULL;
        intf_info->data_count = 0;
    }
    
    if ( intf_info->data_count > NETCAP_MAX_INTERFACES ) {
        errlog( ERR_CRITICAL, "DB is invalid, %d data elements, resetting\n", intf_info->data_count );
        intf_info->data       = NULL;
        intf_info->data_count = 0;
    }

    for ( c = 0; c < intf_info->data_count ; c++ ) {
        if ( memcmp( &intf_info->data[c], data, sizeof( *data )) == 0 ) {
            debug( 4, "INTF DB: [%s] data is already in the database. address %s netmask %s broadcast %s\n", 
                   intf_info->name.s,
                   unet_next_inet_ntoa( data->address.s_addr ),
                   unet_next_inet_ntoa( data->netmask.s_addr ),
                   unet_next_inet_ntoa( data->broadcast.s_addr ));
            return 0;
        }
    }
    
    if (( intf_info->data = realloc( intf_info->data, 
                                     ( intf_info->data_count + 1 ) * sizeof( *data ))) == NULL ) {
        return errlogmalloc();
    }
    
    memcpy( &intf_info->data[intf_info->data_count++], data, sizeof( *data ));
    return 0;
}

/** Return 1 if the interface should be ignored if nothing is known
 * about it */
static int _is_ignore_interface( netcap_intf_string_t* intf_name )
{
    const char* name = (const char*)intf_name;
    /* For nointerface it could be nointerface1 or nointerface1 (minus 1 null termination) */
    if (( strncmp( "tun0", name, sizeof( netcap_intf_string_t )) == 0 ) ||
        ( strncmp( "dummy0", name, sizeof( netcap_intf_string_t )) == 0 ) ||
        ( strncmp( "utun", name, sizeof( netcap_intf_string_t )) == 0 ) ||
        ( strncmp( "nointerface", name, sizeof( "nointerface" ) - 1 ) == 0 )) return 1;

    return 0;
}


