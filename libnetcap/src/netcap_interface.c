/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include "netcap_interface.h"

#include <stdlib.h>
#include <sys/ioctl.h>
#include <net/if.h>
#include <pthread.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sysfs/libsysfs.h>

#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_init.h"

#define IF_TABLE_SIZE 31

#define BRIDGE_PREFIX "br"
/* XXXX brif is magic string, it is normally SYSFS_BRIDGE_ATTR, but the header that has this
 * doesn't have that value set */
#ifndef SYSFS_BRIDGE_ATTR
#define SYSFS_BRIDGE_ATTR       "bridge"
#endif

#ifndef SYSFS_BRIDGE_FDB
#define SYSFS_BRIDGE_FDB        "brforward"
#endif

#ifndef SYSFS_BRIDGE_PORT_SUBDIR
#define SYSFS_BRIDGE_PORT_SUBDIR "brif"
#endif

#ifndef SYSFS_BRIDGE_PORT_ATTR
#define SYSFS_BRIDGE_PORT_ATTR  "brport"
#endif

#ifndef SYSFS_BRIDGE_PORT_LINK
#define SYSFS_BRIDGE_PORT_LINK  "bridge"
#endif

/**
 * XXXXXXXXXXXXXXXXXXXXXXXXXXX
 * RBS:
 * It seems that all of the netcap_interface functions are no longer
 * necessary.  Since it is now the "rule-generators" responsibility to
 * properly mark all packets, most of this functionality is no longer 
 * important.
 *
 * As of now, these are being left around just in case they are needed one day.
 *
 * It is actually needed to maintain the new map from interface name to dev_info.
 */

#define NETCAP_MARK_INTF_MAX 4 // temp lowered
#define NETCAP_MARK_INTF_MASK 0xF

/* This is in network byte order */
#define MULTICAST_MASK htonl(0xF0000000)
#define MULTICAST_FLAG htonl(0xE0000000)

typedef struct
{
    char is_valid;
    char index;
    char is_in_bridge;
    char bridge_index;

    char name[NETCAP_MAX_IF_NAME_LEN];

    /* 
     * For each of these parameters, they are either the value for the interface
     * or the value for the bridge that contains the interface.  The case where an
     * interface contains an address and is in a bridge should ALWAYS be prevented.
     */
    struct in_addr address;
    struct in_addr netmask;
    struct in_addr broadcast;
} _dev_info_t;

/* 
 * Interface (eg. eth,eth1) to address map
 */
static struct {
    /* Hash table that maps strings to the device info */
    ht_t table;
    
    /* Array of the values that are stored in the interface array */
    _dev_info_t dev_info_array[NETCAP_MAX_INTERFACES];
    
    /* is the hash table initialized */
    char is_initialized;

    /* Number of items currently in the interface table */
    int count;

    /* Bridge class for sysfs */
    struct sysfs_class* sysfs_class_net;
} _interface = {
    .is_initialized  0,
    .count           0,
    .sysfs_class_net NULL
};

static char      _if_names_assigned[NETCAP_MAX_INTERFACES][NETCAP_MAX_IF_NAME_LEN];
static in_addr_t _if_addrs[NETCAP_MAX_INTERFACES];
static in_addr_t _netmasks[NETCAP_MAX_INTERFACES];
static in_addr_t _broadcasts[NETCAP_MAX_INTERFACES];
static int       _num_if = 0;
static char      _if_names[NETCAP_MAX_INTERFACES][NETCAP_MAX_IF_NAME_LEN];
static int       _if_count = 0;
static ht_t      _if_name_to_id;

pthread_mutex_t  _update_address_mutex = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;

static __inline__ int _check_intf( netcap_intf_t intf )
{
    if ( intf < NC_INTF_0 || intf > _if_count ) {
        return errlog( ERR_CRITICAL, "Invalid interface: %d\n", intf );
    }
    
    return 0;
}

static int _interface_update_addrs( void );

static int _update_dev_info( void );

static int _update_bridge_devices( void );
static int _update_bridge_device( _dev_info_t* bridge_dev_info, struct sysfs_class_device* dev );

/* Lookup the information for a device, if the device is in a bridge, this will return
 * the information about the bridge.
 */
static _dev_info_t* _get_dev_info( char* name );

/**
 * Return 1 if the bridge exists, 0 otherwise
 */
int netcap_interface_bridge_exists( void )
{
    struct ifreq interfaces[NETCAP_MAX_INTERFACES];
    struct ifconf conf;
    int  sockfd, i;
    
    /* Clear out all of the interface names */
    bzero( _if_names, sizeof(_if_names));

    /* XXX Have to make sure to close this socket */
    sockfd = socket( PF_INET, SOCK_DGRAM, 0 );

    if ( sockfd < 0 ) return perrlog("socket");

    conf.ifc_len = sizeof(interfaces);
    conf.ifc_req = interfaces;

    if ( ioctl( sockfd, SIOCGIFCONF, &conf ) < 0 )
        return perrlog("ioctl");

    if ( close( sockfd ))
        perrlog( "close" );
    
    i = conf.ifc_len / sizeof(struct ifreq);
    
    for ( ; --i >= 0 ; ) {
        if ( strncmp( interfaces[i].ifr_name, "br0", 3 ) == 0 ) {
            return 1;
        }
    }
    
    return 0;
}

/**
 *
 */
int netcap_interface_init ()
{
    int i, j;
    char if_name[NETCAP_MAX_IF_NAME_LEN];
    
    /* Clear out all of the interface names */
    bzero(_if_names,sizeof(_if_names));    

    if ( _interface_update_addrs() < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_interface_update_addrs\n" );
    }
    
    /* The code above only gets the "active" interfaces, which means it only
     * retrieves the bridge */
    /* Retrieve all of the interface names */
    if (ht_init(&_if_name_to_id,IF_TABLE_SIZE,string_hash_func,string_equ_func,0)<0){
        return errlog(ERR_CRITICAL,"ht_init");
    }

    /* XXX Is it an error if you reach the max */
    j = 0;
    for ( i = 1 ; i < NETCAP_MAX_INTERFACES ; i++ ) {
        if ( if_indextoname(i,if_name) == NULL ) continue;
        
        if ( strncmp(if_name,"lo",2) == 0 ) continue;
        if ( strncmp(if_name,"br",2) == 0 ) continue;
        if ( strncmp(if_name,"sit",3) == 0 ) continue;
        
        strncpy(_if_names[j],if_name,NETCAP_MAX_IF_NAME_LEN);
        ht_add(&_if_name_to_id,(void*)_if_names[j],(void*)(j+1));
        j++;
    }

    _if_count = j;

    if (( _interface.sysfs_class_net = sysfs_open_class( "net" )) == NULL ) {
        return perrlog( "sysfs_open_class" );
    }

    if ( _update_dev_info() < 0 ) return errlog( ERR_CRITICAL, "_update_dev_info\n" );
    
    return 0;
}

/**
 *
 */
int netcap_interface_cleanup( void )
{
    /* Destroy the hash table */
    ht_destroy( &_if_name_to_id );

    // if ( _interface.sysfs_class_net != NULL ) sysfs_close_class( _interface.sysfs_class_net );
    // _interface.sysfs_class_net = NULL;

    return 0;
}

int netcap_interface_update_address( void )
{
    int ret = 0;
    
    if ( pthread_mutex_lock( &_update_address_mutex ) < 0 ) {
        return errlog ( ERR_CRITICAL, "pthread_mutex_lock" );
    }
    
    do {
        /* Nothing to do if netcap is not initialized */
        if ( !netcap_is_initialized()) {
            break;
        }

        if ( _update_dev_info() < 0 ) {
            ret = errlog( ERR_CRITICAL, "_update_dev_info\n" );
            break;
        }
        
        /* Update all of the addresses */
        /* XXXXX this might be fatal */
        if ( _interface_update_addrs() < 0 ) { 
            errlog( ERR_CRITICAL, "Critical Error, unable to update the address table\n" );
            ret = errlog( ERR_CRITICAL, "_interface_update_addrs\n" );
            break;
        }        
    } while ( 0 );
    
    if ( pthread_mutex_unlock( &_update_address_mutex ) < 0 ) {
        return errlog ( ERR_CRITICAL, "pthread_mutex_lock" );
    }
    
    return ret;
}

/* Returns 1 if it is a broadcast */
int netcap_interface_is_broadcast (in_addr_t addr)
{
    int i;

    if (addr == ((in_addr_t) 0xffffffff))
        return 1;

    for (i=0;i<_num_if;i++) {
        if (addr == _broadcasts[i]) return 1;

        if ((addr & ~_netmasks[i]) == (_broadcasts[i] & ~_netmasks[i])) {
            return 1;
        }
    }
    
    return 0;
}

/* Returns 1 if it is a multicast, 0 otherwise */
int netcap_interface_is_multicast (in_addr_t addr)
{
    if ((((unsigned int)addr) & MULTICAST_MASK ) == MULTICAST_FLAG ) {
        return 1;
    }

    return 0;
}
 
int netcap_interface_is_local (in_addr_t addr)
{
    int i;
    for (i=0 ; i<_num_if; i++) {
        if (addr == _if_addrs[i]) return 1;
    }
    
      return 0;
}

int netcap_interface_count (void)
{
    return _num_if;
}

in_addr_t* netcap_interface_addrs (void)
{
    return _if_addrs;
}

/* This function should either go away or become a macro now that the interface is 
 * just an int */
int  netcap_interface_mark_to_intf(int nfmark, netcap_intf_t* intf)
{
    if ( intf == NULL ) return errlogargs();

    nfmark &= NETCAP_MARK_INTF_MASK;
    
    if ( nfmark < 0 || nfmark > NETCAP_MARK_INTF_MAX ) {
        *intf = 0;
        return errlog( ERR_CRITICAL, "Invalid interface mark\n");
    }

    /* Map the marking to the corresponding interface */
    *intf = nfmark;
    
    return 0;
}

int netcap_interface_intf_verify( netcap_intf_t intf )
{
    return _check_intf( intf );
}

int netcap_interface_intf_to_string ( netcap_intf_t intf, char *intf_str, int str_len )
{
    if ( str_len <= 0 || intf_str == NULL ) return errlogargs();

    if ( _check_intf( intf ) < 0 ) return errlog( ERR_CRITICAL, "_check_intf\n" );

    strncpy( intf_str, _if_names[ intf - NC_INTF_0 ], str_len );

    return 0;
}

int netcap_interface_string_to_intf (char *intf_str, netcap_intf_t *intf )
{
    if ( intf_str == NULL || intf_str[0] == '\0' || intf == NULL ) return errlogargs();
    *intf = (netcap_intf_t)ht_lookup(&_if_name_to_id,(void*)intf_str);
    
    if ( *intf == (netcap_intf_t)NULL ) {
        return errlog(ERR_WARNING,"Invalid interface: %s\n", intf_str);
    }
        
    return 0;
}

int netcap_interface_get_address( char* name, struct in_addr* address )
{
    _dev_info_t* dev_info;

    if ( address == NULL || name == NULL ) return errlogargs();

    if (( dev_info = _get_dev_info( name )) == NULL ) {
        return errlog( ERR_CRITICAL, "_get_dev_info\n" );
    }

    memcpy( address, &dev_info->address, sizeof( dev_info->address ));
    
    return 0;
}

int netcap_interface_get_netmask( char* name, struct in_addr* netmask )
{
    _dev_info_t* dev_info;

    if ( netmask == NULL || name == NULL ) return errlogargs();

    if (( dev_info = _get_dev_info( name )) == NULL ) {
        return errlog( ERR_CRITICAL, "_get_dev_info\n" );
    }

    memcpy( netmask, &dev_info->netmask, sizeof( dev_info->netmask ));
    
    return 0;
}

int netcap_interface_get_broadcast( char* name, struct in_addr* broadcast )
{
    _dev_info_t* dev_info;

    if ( broadcast == NULL || name == NULL ) return errlogargs();

    if (( dev_info = _get_dev_info( name )) == NULL ) {
        return errlog( ERR_CRITICAL, "_get_dev_info\n" );
    }

    memcpy( broadcast, &dev_info->broadcast, sizeof( dev_info->broadcast ));
    
    return 0;
}


/* XXX This is just a function that is used by test_xml inside of xenon in order
 * to initialize the interface hash table and array */
void netcap_interface_ht_init (void) {
    int i,j;
    char if_name[NETCAP_MAX_IF_NAME_LEN];

    /* The code above only gets the "active" interfaces, which means it only
     * retrieves the bridge */
    /* Retrieve all of the interface names */
    
    if (ht_init(&_if_name_to_id,IF_TABLE_SIZE,string_hash_func,string_equ_func,0)<0){
        errlog(ERR_CRITICAL,"ht_init");
        return;
    }
    
    j = 0;
    for ( i = 1 ; i < NETCAP_MAX_INTERFACES ; i++ ) {
        if ( if_indextoname(i,if_name) == NULL ) continue;
        
        if ( strncmp(if_name,"lo",2) == 0 ) continue;
        if ( strncmp(if_name,"br",2) == 0 ) continue;
        if ( strncmp(if_name,"sit",3) == 0 ) continue;
        
        strncpy(_if_names[j],if_name,NETCAP_MAX_IF_NAME_LEN);
        ht_add(&_if_name_to_id,(void*)_if_names[j],(void*)(j+1));
        j++;
    }

    _if_count = j;
}

static int _interface_update_addrs( void )
{
    struct ifconf conf;
    struct ifreq interfaces[NETCAP_MAX_INTERFACES];
    int i, ret = 0;
    int sockfd;
    
    debug(2, "NETCAP: Updating interface addresses\n" );
        
    /* XXX Have to make sure to close this socket */
    if (( sockfd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) return perrlog( "socket" );
    
    do {
        conf.ifc_len = sizeof( interfaces );
        conf.ifc_req = interfaces;
        if ( ioctl( sockfd,SIOCGIFCONF,&conf ) < 0 ) {
            ret = perrlog("ioctl");
            break;
        }
        
        i =  conf.ifc_len / sizeof(struct ifreq);
        _num_if = 0;
        
        for ( ; ( --i >= 0 ) && ( ret == 0 ) ; ) {
            struct in_addr addr;
            struct in_addr broadcast;
            struct in_addr netmask;
            struct ifreq ifr;
            
            memcpy(&addr,&(*(struct sockaddr_in*)&interfaces[i].ifr_addr).sin_addr,sizeof(struct in_addr));
            
            strncpy(ifr.ifr_name, interfaces[i].ifr_name, sizeof(interfaces[i].ifr_name));
            
            if (ioctl(sockfd, SIOCGIFBRDADDR, &ifr) < 0) {
                ret = perrlog("ioctl");
                break;
            } else {
                broadcast = (*(struct sockaddr_in*)&ifr.ifr_broadaddr).sin_addr; 
            }
            
            if (ioctl(sockfd, SIOCGIFNETMASK, &ifr) < 0) {
                ret = perrlog("ioctl");
                break;
            } else {
                netmask = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr;
            }
            
            if ( addr.s_addr != inet_addr("127.0.0.1")) {
                debug(4,"Interface : %i\n",_num_if);
                debug(4,"Name      : %s\n", interfaces[i].ifr_name );
                debug(4,"IP        : %s\n",inet_ntoa(addr));
                debug(4,"Netmask   : %s\n",inet_ntoa(netmask));
                debug(4,"Broadcast : %s\n",inet_ntoa(broadcast));
                
                _if_addrs[_num_if]    = addr.s_addr;
                _broadcasts[_num_if]  = broadcast.s_addr;
                _netmasks[_num_if]    = netmask.s_addr;
                strncpy( _if_names_assigned[_num_if], interfaces[i].ifr_name, NETCAP_MAX_IF_NAME_LEN);

                _num_if++;            
            }
        }
    } while ( 0 );

    if ( close( sockfd ) < 0 ) 
        return perrlog( "close" );
    
    return ret;
}

static int _update_dev_info( void )
{
    int i, ret = 0;
    int sockfd;
    char if_name[NETCAP_MAX_IF_NAME_LEN];
    struct ifreq ifr;
    _dev_info_t* dev_info = NULL;
    struct in_addr* ioctl_data = &((*(struct sockaddr_in*)&ifr.ifr_addr).sin_addr);

    debug( 2, "INTERFACE: Updating interface addresses\n" );

    if (( _interface.is_initialized != 0 ) && ht_destroy( &_interface.table ) < 0 ) {
        errlog( ERR_CRITICAL, "Error destroying table\n" );
    }

    /* Null out the device interface arrray */
    bzero( _interface.dev_info_array, sizeof( _interface.dev_info_array ));
   
    if ( ht_init( &_interface.table, IF_TABLE_SIZE, string_hash_func, string_equ_func, 0 ) < 0 ) {
        return errlog( ERR_CRITICAL, "Error initializing table\n" );
    }
    
    _interface.is_initialized = 1;
    
    if (( sockfd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) return perrlog( "socket" );
    
    do {
        _interface.count = 0;
                
        struct ifconf conf;
        struct ifreq interfaces[NETCAP_MAX_INTERFACES];

        conf.ifc_len = sizeof( interfaces );
        conf.ifc_req = interfaces;
        if ( ioctl( sockfd, SIOCGIFCONF, &conf ) < 0 ) {
            ret = perrlog("ioctl");
            break;
        }
        
        i =  conf.ifc_len / sizeof(struct ifreq);
        _num_if = 0;
        
        for ( ; ( --i >= 0 ) && ( ret == 0 ) ; ) {
            struct in_addr addr;
            struct in_addr broadcast;
            struct in_addr netmask;
            struct ifreq ifr;
            
            memcpy(&addr,&(*(struct sockaddr_in*)&interfaces[i].ifr_addr).sin_addr,sizeof(struct in_addr));
            
            strncpy(ifr.ifr_name, interfaces[i].ifr_name, sizeof(interfaces[i].ifr_name));
            
            if (ioctl(sockfd, SIOCGIFBRDADDR, &ifr) < 0) {
                ret = perrlog("ioctl");
                break;
            } else {
                broadcast = (*(struct sockaddr_in*)&ifr.ifr_broadaddr).sin_addr; 
            }
            
            if (ioctl(sockfd, SIOCGIFNETMASK, &ifr) < 0) {
                ret = perrlog("ioctl");
                break;
            } else {
                netmask = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr;
            }
            
            if ( addr.s_addr != inet_addr("127.0.0.1")) {
                debug( 3, "INTERFACE: Retrieving information for device: %s\n", interfaces[i].ifr_name );
                
                dev_info = &_interface.dev_info_array[_interface.count];
                dev_info->index = _interface.count++;
                /* Copy in the interface name */
                strncpy( dev_info->name, interfaces[i].ifr_name, NETCAP_MAX_IF_NAME_LEN );
                memcpy( &dev_info->address, &addr, sizeof( dev_info->address ));
                memcpy( &dev_info->netmask, &netmask, sizeof( dev_info->netmask ));
                memcpy( &dev_info->broadcast, &broadcast, sizeof( dev_info->broadcast ));

                if ( ht_add( &_interface.table, dev_info->name, dev_info ) < 0 ) {
                    ret = errlog( ERR_CRITICAL, "ht_add\n" );
                    break;
                }

                dev_info->is_valid = 1;
            }
        }

        if ( ret < 0 ) break;             
        
        for ( i = 1 ; i < NETCAP_MAX_INTERFACES ; i++ ) {
            if ( if_indextoname( i, if_name ) == NULL ) {
                debug( 10, "INTERFACE: %d doesn't exist\n", i );
                continue;
            }
            
            if (( strncmp( if_name, "lo", 2 ) == 0 ) ||
                ( strncmp( if_name, "sit", 3 ) == 0 )  ||
                ( strncmp( if_name, "dummy", 5 ) == 0 )) {
                debug( 10, "INTERFACE: skipping %s\n", if_name );
                continue;
            }

            debug( 3, "INTERFACE: Retrieving information for device: %s\n", if_name );

            if ( ht_lookup( &_interface.table, if_name ) != NULL ) {
                debug( 3, "INTERFACE: Is %s already in the table, skipping\n", if_name );
                continue;
            }

            dev_info = &_interface.dev_info_array[_interface.count];
            dev_info->index = _interface.count++;

            /* Copy in the interface name */
            strncpy( dev_info->name, if_name, NETCAP_MAX_IF_NAME_LEN );
            strncpy( ifr.ifr_name, if_name, sizeof(if_name));
            
            /* Insert the device into the hash table */
            if ( ht_add( &_interface.table, dev_info->name, dev_info ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "ht_add\n" );
                break;
            }
            
            /* Get the address */
            if (ioctl( sockfd, SIOCGIFADDR, &ifr ) < 0 ) {
                if ( errno == EADDRNOTAVAIL ) {
                    dev_info->is_valid = 0;
                    continue;
                } else {
                    ret = perrlog( "ioctl" );
                    break;
                }
            }
            memcpy( &dev_info->address, ioctl_data, sizeof( dev_info->address ));
            
            /* Get the netmask */
            if (ioctl( sockfd, SIOCGIFNETMASK, &ifr ) < 0 ) {
                if ( errno == EADDRNOTAVAIL ) {
                    dev_info->is_valid = 0;
                    continue;
                } else {
                    ret = perrlog( "ioctl" );
                    break;
                }                
            }
            memcpy( &dev_info->netmask, ioctl_data, sizeof( dev_info->netmask ));

            /* Get the broadcast address */
            if (ioctl( sockfd, SIOCGIFBRDADDR, &ifr ) < 0 ) {
                if ( errno == EADDRNOTAVAIL ) {
                    dev_info->is_valid = 0;
                    continue;
                } else {
                    ret = perrlog( "ioctl" );
                    break;
                }                
            }
            memcpy( &dev_info->broadcast, ioctl_data, sizeof( dev_info->broadcast ));
            
            debug( 3, "INTERFACE: Device configuration: %s address %s netmask %s broadcast %s\n", 
                   dev_info->name,
                   unet_next_inet_ntoa( dev_info->address.s_addr ),
                   unet_next_inet_ntoa( dev_info->netmask.s_addr ),
                   unet_next_inet_ntoa( dev_info->broadcast.s_addr ));
            
            dev_info->is_valid = 1;            
        }

        if (( ret >= 0 ) && ( _update_bridge_devices() < 0 )) { 
            ret = errlog( ERR_CRITICAL, "_update_bridge_devices\n" );
        }

    } while ( 0 );            


    if ( close( sockfd ) < 0 )
        return perrlog( "close" );
    
    return ret;
}

/**
 * Update the info for all of the bridges all of the bridges
 */

static int _update_bridge_devices( void )
{
    int c;
    int ret = 0;

    for ( c = 0 ; c < _interface.count ; c++ ) {
        _dev_info_t* dev_info = &_interface.dev_info_array[c];
        struct sysfs_class_device* dev;

        /* Don't include the null character in the comparison */
        if ( strncmp( dev_info->name, BRIDGE_PREFIX, sizeof(BRIDGE_PREFIX) - 1 ) != 0 ) {
            /* Not a bridge */
            debug( 4, "Ignoring non-bridge interface %s\n", dev_info->name );
            continue;
        }

        debug( 3, "Retrieving interfaces for bridge '%s'\n", dev_info->name );
        
        if ( _interface.sysfs_class_net == NULL ) {
            return errlog( ERR_CRITICAL, "sysfs_class_net is not initialzed\n" );
        }
        
        if (( dev = sysfs_get_class_device( _interface.sysfs_class_net, dev_info->name )) == NULL ) {
            return perrlog( "sysfs_get_class_device" );
        }
        
        /* dev must be closed */
        ret = _update_bridge_device( dev_info, dev );
        
        // XXX Not sure if these have to be closed
        // sysfs_close_class_device( dev );
        
        if ( ret < 0 ) return errlog( ERR_CRITICAL, "_update_bridge_device\n" );        
    }
    
    return 0;
}

/**
 * Register any of bridge devices of for the last interface that was registered
 */
static int _update_bridge_device( _dev_info_t* bridge_dev_info, struct sysfs_class_device* dev )
{
    char path[SYSFS_PATH_MAX];
    struct sysfs_directory* dir;

    int _critical_section( void ) {
        struct dlist* dir_links;
        struct sysfs_link* plink;
        _dev_info_t* dev_info;

        /* Retrieve a list of all of the directories that link to
         * this, these are all of the interfaces in the bridge */
        if (( dir_links = sysfs_get_dir_links( dir )) == NULL ) return perrlog( "sysfs_get_dir_links" );
        
        /* Iterate through all of the entries */
        dlist_for_each_data( dir_links, plink, struct sysfs_link ) {
            debug( 3, "Bridge %s contains interface %s\n", bridge_dev_info->name, plink->name );
            if (( dev_info = ht_lookup( &_interface.table, plink->name )) == NULL ) {
                return errlog( ERR_CRITICAL, "Interface table does not contain %s\n", plink->name );
            }
            
            if ( dev_info->is_in_bridge != 0 ) {
                return errlog( ERR_CRITICAL, "Interface %s is already in a bridge\n", dev_info->name );
            }
            
            dev_info->is_valid     = 1;
            dev_info->is_in_bridge = 1;
            dev_info->bridge_index = bridge_dev_info->index;
        }
        
        return 0;
    }

    int ret = 0;

    snprintf( path, sizeof( path ), "%s/%s", dev->path, SYSFS_BRIDGE_PORT_SUBDIR );
    
    if (( dir = sysfs_open_directory( path )) == NULL ) return perrlog( "sysfs_open_directory" );

    ret = _critical_section();
    
    sysfs_close_directory( dir );
    
    return ret;
}

static _dev_info_t* _get_dev_info( char* name )
{
    _dev_info_t* dev_info = NULL;

    if (( dev_info = ht_lookup( &_interface.table, name )) == NULL ) {
        return errlog_null( ERR_CRITICAL, "Nothing is known about the interface: '%s'\n", name );
    }
    
    if ( dev_info->is_in_bridge ) {
        if ( dev_info->bridge_index < 0 || dev_info->bridge_index >= NETCAP_MAX_INTERFACES ) {
            return errlog_null( ERR_CRITICAL, "Database is broken for interface: '%s'\n", name );
        }
        dev_info = &_interface.dev_info_array[(int)dev_info->bridge_index];
    }

    if ( !dev_info->is_valid ) {
        return errlog_null( ERR_CRITICAL, "The interface '%s' is not valid\n", dev_info->name );
    }

    return dev_info;

}
