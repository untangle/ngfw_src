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
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/usystem.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_subscriptions.h"

#define IF_TABLE_SIZE 31

#define NETCAP_MARK_INTF_MAX 4 // temp lowered
#define NETCAP_MARK_INTF_MASK 0xF

/* This is in network byte order */
#define MULTICAST_MASK htonl(0xF0000000)
#define MULTICAST_FLAG htonl(0xE0000000)

#define RULES_ADD 1
#define RULES_DEL 0

#define PORT_GUARD_CMD "/sbin/iptables -t mangle -%c PREROUTING -p %s -m mark --mark %d/%d %s %s -j DROP"

typedef struct  {
    char cmd[MAX_CMD_LEN];    
} iptables_cmd_t;

static in_addr_t _if_addrs[NETCAP_MAX_INTERFACES];
static in_addr_t _netmasks[NETCAP_MAX_INTERFACES];
static in_addr_t _broadcasts[NETCAP_MAX_INTERFACES];
static int       _num_if = 0;
static char      _if_names[NETCAP_MAX_INTERFACES][NETCAP_MAX_IF_NAME_LEN];
static int       _if_count = 0;
static ht_t      _if_name_to_id;


static __inline__ int _validate_intf_str( char* intf_str )
{
    if ( intf_str == NULL ) return errlogargs();

    if ( strnlen( intf_str, NETCAP_MAX_IF_NAME_LEN ) >= NETCAP_MAX_IF_NAME_LEN ) {
        char str[NETCAP_MAX_IF_NAME_LEN];
        strncpy( str, intf_str, sizeof( str ));
        return errlog( ERR_CRITICAL, "Interface name is too long(first %d bytes): '%s'\n", 
                       sizeof( str ), str );
    }

    return 0;
}

static __inline__ int _check_intf( netcap_intf_t intf )
{
    if ( intf < NC_INTF_0 || intf > _if_count ) {
        return errlog( ERR_CRITICAL, "Invalid interface: %d\n", intf );
    }
    
    return 0;
}

static int _netcap_interface_marking               ( int if_add );

static int _netcap_interface_disable_srv_conntrack ( int if_add );

/* Limit connections on the inside and outside to just the current subnet */
static int _limit_subnet  ( char* dev_inside, char* dev_outside, int if_add );

static int _interface_update_addrs( void );

/* Retrive the address and netmask of the interface named interface_name */
static int _get_interface_address ( char* interface_name, in_addr_t* address, in_addr_t* netmask );

/* Setup or reomve all of the locally destined packets */
static int _modify_local_marks( char* intf_name, int intf_mark, int if_add );

/* Setup or remove a guard on a port */
int _command_port_guard( netcap_intf_t gate, int protocol, char* ports, char* guest, int if_add );

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
    
    if ( _netcap_interface_disable_srv_conntrack( RULES_ADD ) < 0 ) {
        return errlog( ERR_CRITICAL, "_netcap_interface_disable_srv_conntrack\n" );
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
    
    // Insert all of the interface marking rules
    if ( _netcap_interface_marking(RULES_ADD) < 0 ) {
        return errlog(ERR_CRITICAL,"_netcap_interface_marking\n");
    }

    return 0;
}

/**
 *
 */
int netcap_interface_cleanup( void )
{
    // Remove all of the interface marking rules
    _netcap_interface_marking( RULES_DEL );
    
    /* Insert a rule to disable conntracking on the server side of
     * connection completes  */
    _netcap_interface_disable_srv_conntrack( RULES_DEL );

    /* Destroy the hash table */
    ht_destroy(&_if_name_to_id);

    /* Remove the limit rules if necessary */
    if ( _limit_subnet ( NULL, NULL, RULES_DEL ) < 0 ) return errlog ( ERR_CRITICAL, "_limit_subnet\n" );

    return 0;
}

int netcap_interface_update_address()
{
    /* XXX This creates a gap where none of the marking rules are in place, this could be
     * problematic */
    /*** XXX Look into creating a temporary rule for dropping all incoming packets on br0 */
    
    _netcap_interface_marking( RULES_DEL );

    /* Update all of the addresses */
    /* XXXXX this might be fatal */
    if ( _interface_update_addrs() < 0 ) { 
        errlog( ERR_CRITICAL, "Critical Error, unable to update the address table\n" );
        return errlog( ERR_CRITICAL, "_interface_update_addrs\n" );
    }

    if ( _netcap_interface_marking( RULES_ADD ) < 0 ) {
        return errlog( ERR_CRITICAL, "_netcap_interface_marking\n" );
    }

    return 0;
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

static int _netcap_interface_marking               ( int if_add )
{
    int c;
    char add_del;
    // String for holding the insert command for each interface
    char insert_cmd[MAX_CMD_LEN];
    int  if_err = 0;
    
    add_del = ( if_add == RULES_ADD ) ? 'A' : 'D';
    
    /* Insert a mark for each eth interface from 0 to max */
#define MARK_BASE "/sbin/iptables -t mangle -%c " INTERFACE_CHAIN " -m physdev " \
                  "--physdev-in %s -j MARK --set-mark %d"
    
    /* Rules to block u-turns(going out on the same interface the packet came in) */
    /* Use 0x1F as the mask, this guarantees that antisubcribed packets 
     * matched.
     * 04/13/05 - UPDATE: 0x1F is not necessary since antisubscribed packets are set
     * to 0x10 by the antisubscribe rules, therefore the lower bits will always be zero once they get
     * to this point.  0x1f also causes problems, if all outgoing UDP packets are antisubscribed,
     * (This is necessary to enable local traffic), then broadcasts are not caught properly.
     * EG: packet marked 0x13 would not be prevented from going out the incorrect interface
     */
#define UTURN_BASE "/sbin/iptables -t filter -%c OUTPUT -m mark -m physdev --physdev-out %s " \
                   "--mark %d/0x0F -j DROP"
    
    for ( c = _if_count ; c-- > 0  ; ) {
        /* Build the command for each interface, Mark interface eth0 with 1, eth1 with 2, etc*/
        snprintf( insert_cmd, MAX_CMD_LEN, MARK_BASE, add_del, _if_names[c], c + 1 );
        
        if ( mvutil_system ( insert_cmd ) < 0 ) {
            perrlog("mvutil_system");
            if_err = -1;
        } else {
            debug(5,"NETCAP: Run Command: '%s' \n", insert_cmd);
        }
        
        /* Build the command for each interface, Mark interface eth0 with 1, eth1 with 2, etc*/
        snprintf( insert_cmd, MAX_CMD_LEN, UTURN_BASE, add_del, _if_names[c], c+1 );
        
        if ( mvutil_system ( insert_cmd ) < 0 ) {
            perrlog("mvutil_system");
            if_err = -1;
        } else {
            debug(5,"NETCAP: Run Command: '%s' \n", insert_cmd);
        }
        
        if ( _modify_local_marks( _if_names[c], c + 1, if_add ) < 0 ) {
            errlog( ERR_CRITICAL, "_modify_local_marks\n" );
        }
        
#undef MARK_BASE
  }

  return if_err;
}

static int _netcap_interface_disable_srv_conntrack ( int if_add )
{
    char add_del;
    char insert_cmd[MAX_CMD_LEN];
    
    if ( if_add == RULES_ADD ) {
        add_del = 'A';
    } else {
        add_del = 'D';
    }
    
    /* Insert the rule to disable conntracking on all outgoing packets from this program */
    snprintf( insert_cmd, sizeof( insert_cmd ), "/sbin/iptables -t raw -%c OUTPUT -m mark -j NOTRACK "
              "--mark " MARK_S_MASK_NOTRACK, add_del );

    if ( mvutil_system ( insert_cmd ) < 0 ) {
        return perrlog("mvutil_system");
    } else {
        debug( 5, "NETCAP: Run Command: '%s' \n", insert_cmd );
    }

    /* Antisubscribe all packets that are related to sessions initiated at the box */
    snprintf( insert_cmd, sizeof( insert_cmd ), 
              "/sbin/iptables -t mangle -%c antisub -p ! tcp -m state -m mark -j MARK "
              " ! --mark " MARK_S_MASK_ANTISUB " --state related,established  --set-mark " 
              MARK_S_ANTISUB, add_del );

    if ( mvutil_system ( insert_cmd ) < 0 ) {
        return perrlog("mvutil_system");
    } else {
        debug( 5, "NETCAP: Run Command: '%s' \n", insert_cmd );
    }

    return 0;
}

int netcap_interface_limit_subnet   ( char* dev_inside, char* dev_outside )
{
    return _limit_subnet ( dev_inside, dev_outside, RULES_ADD );
}

static int _limit_subnet  ( char* dev_inside, char* dev_outside, int if_add )
{
    char insert_cmd[MAX_CMD_LEN];
    in_addr_t address;
    in_addr_t netmask;
    char address_str[INET_ADDRSTRLEN];
    char netmask_str[INET_ADDRSTRLEN];
    static char delete_cmd[2][MAX_CMD_LEN] = { "", "" };
    static int initialized = 0;

    if ( if_add == RULES_ADD ) {
        if ( initialized ) return errlog ( ERR_CRITICAL, "_limit_subnet: already initialized\n" );
        
        if ( _validate_intf_str( dev_inside ) < 0 || _validate_intf_str( dev_outside ) < 0 ) return -1;

        debug( 5, "NETCAP: Limiting the subnet with inside: '%s', outside: '%s'\n", dev_inside, dev_outside );

        if ( strncmp( dev_inside, dev_outside, NETCAP_MAX_IF_NAME_LEN ) == 0 ) {
            return errlog( ERR_CRITICAL, "Cannot limit subnet with same inside and outside device: %s\n" );
        }
        
        /* Retrieve the address and netmask of the outside interface */
        if ( _get_interface_address( "br0", &address, &netmask ) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_interface_address\n" );
        }
        
        if ( !inet_ntop( AF_INET, &address, address_str, sizeof ( address_str ))) {
            return perrlog ( "inet_ntop" );
        }
        
        if ( !inet_ntop( AF_INET, &netmask, netmask_str, sizeof ( netmask_str ))) {
            return perrlog ( "inet_ntop" );
        }
        
        snprintf ( insert_cmd, sizeof ( insert_cmd ), 
                   "/sbin/iptables -t mangle -I PREROUTING 1 ! -s %s/%s -m physdev --physdev-in %s "
                   "-m pkttype --pkt-type unicast -j DROP", address_str, netmask_str, dev_inside );

        if ( mvutil_system ( insert_cmd ) < 0 ) {
            return perrlog("mvutil_system");
        } else {
            debug( 5, "NETCAP: Run Command: '%s' \n", insert_cmd );
        }

        snprintf ( delete_cmd[0], sizeof ( delete_cmd[0] ), 
                  "/sbin/iptables -t mangle -D PREROUTING ! -s %s/%s -m physdev --physdev-in %s -m pkttype "
                  "--pkt-type unicast -j DROP", address_str, netmask_str, dev_inside );

        snprintf ( insert_cmd, sizeof ( insert_cmd ), 
                  "/sbin/iptables -t mangle -I PREROUTING 1 ! -d %s/%s -m physdev --physdev-in %s "
                   "-m pkttype --pkt-type unicast -j DROP", address_str, netmask_str, dev_outside );
        
        if ( mvutil_system ( insert_cmd ) < 0 ) {
            return perrlog("mvutil_system");
        } else {
            debug( 5, "NETCAP: Run Command: '%s' \n", insert_cmd );
        }

        snprintf ( delete_cmd[1], sizeof ( delete_cmd[1] ), 
                  "/sbin/iptables -t mangle -D PREROUTING ! -d %s/%s -m physdev --physdev-in %s -m pkttype "
                  "--pkt-type unicast -j DROP", address_str, netmask_str, dev_outside );

        initialized = 1;

    } else if ( if_add == RULES_DEL ) {
        if ( !initialized ) return 0;

        if ( delete_cmd[0][0] != '\0' ) {
            if ( mvutil_system( delete_cmd[0] ) < 0 ) return perrlog( "mvutil_system" );
            else debug ( 5, "NETCAP: Run Command: '%s'\n", delete_cmd[0] );
        }

        if ( delete_cmd[1][0] != '\0' ) {
            if ( mvutil_system( delete_cmd[1] ) < 0 ) return perrlog( "mvutil_system" );
            else debug ( 5, "NETCAP: Run Command: '%s'\n", delete_cmd[1] );
        }
    } else {
        return errlogargs();
    }
   
    return 0;
}

int netcap_interface_station_port_guard( netcap_intf_t gate, int protocol, char* port, char* guest )
{
    return _command_port_guard( gate, protocol, port, guest, RULES_ADD );
}

int netcap_interface_relieve_port_guard( netcap_intf_t gate, int protocol, char* port, char* guest )
{
    return _command_port_guard( gate, protocol, port, guest, RULES_DEL );
}

int _command_port_guard( netcap_intf_t gate, int protocol, char* ports, char* guests, int if_add )
{
    iptables_cmd_t cmd;
    char* protocol_str;
    char  action;
    int   mark      = MARK_LOCAL;
    int   mark_mask = MARK_LOCAL;
    char  ports_str[100] = "";
    char  guests_str[100] = "";

    if ( protocol == IPPROTO_TCP ) {
        protocol_str = "tcp";
    } else if ( protocol == IPPROTO_UDP ) {
        protocol_str = "udp";
    } else {
        return errlog( ERR_CRITICAL, "Invalid protocol: %d\n", protocol );
    }
    
    if ( gate != NC_INTF_UNK ) {
        if ( _check_intf( gate ) < 0 ) {
            return errlog( ERR_CRITICAL, "_check_intf\n" );
        }
        
        mark_mask |= NETCAP_MARK_INTF_MASK;
        mark      |= gate;
    }
        
    
    /* XXX Must be able to specify a particular address eg br0 or br0:0 */
    if ( ports != NULL ) {
        /* [ is a secret code for passing a range */
        if ( ports[0] == '[' ) {
            snprintf( ports_str, sizeof( ports_str ), " --dport %s ", &ports[1] );
        } else {
            snprintf( ports_str, sizeof( ports_str ), " -m multiport --dports %s ", ports );
        }
    }

    if ( guests != NULL ) {
        snprintf( guests_str, sizeof( guests_str ), " -s ! %s ", guests );
    }
    
    action = ( if_add == RULES_ADD ) ? 'A' : 'D';
    
    snprintf( cmd.cmd, sizeof( iptables_cmd_t ), PORT_GUARD_CMD, action, protocol_str, mark, mark_mask,
              guests_str, ports_str );

    debug( 3, "Inserting guard command: '%s'\n", cmd.cmd );
    
    if ( mvutil_system( cmd.cmd ) < 0 )
        return perrlog( "mvutil_system" );

    return 0;
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

/* INTF_SET_ALL_MASK lets the user know that this is interface set is in  *
 * its default state, and it should clear all interfaces before setting a *
 * new one */
#define INTF_SET_ALL_MASK  (0x10000)
#define INTF_SET_ALL       (INTF_SET_ALL_MASK | ((1<<_if_count) - 1))

/* Turn all interfaces on */
int netcap_intfset_clear ( netcap_intfset_t* intfset) 
{
    if ( intfset == NULL ) return errlogargs();
    
    *intfset = INTF_SET_ALL;
    
    return 0;
}

int netcap_intfset_get_all ( netcap_intfset_t* intfset)
{
    if ( intfset == NULL ) return errlogargs();
    
    return ( *intfset & INTF_SET_ALL_MASK );
}

/* Set an interface in the set.  (This enables one interface) */
int netcap_intfset_add   ( netcap_intfset_t* intfset, netcap_intf_t intf) 
{
    if ( intfset == NULL ) return errlogargs();

    if ( _check_intf( intf ) < 0 ) return errlog( ERR_CRITICAL, "_check_intf\n" );

    if ( *intfset & INTF_SET_ALL_MASK ) {
        *intfset = 0;
    }
    
    *intfset |= (1 << ( intf - NC_INTF_0));

    return 0;
}

/* Check an interface in the set. (Returns 1 if set, 0 if not, -1 on error */
int netcap_intfset_get   ( netcap_intfset_t* intfset, netcap_intf_t intf) 
{
    if ( intfset == NULL ) return errlogargs();

    if ( _check_intf( intf ) < 0 ) return errlog( ERR_CRITICAL, "_check_intf\n" );
    
    return ( (*intfset & (1 << (intf - NC_INTF_0))) ? 1 : 0);
}

int netcap_intfset_to_string (char* dst, int dst_len, netcap_intfset_t* intfset_p) 
{
    int c;
    int count;
    netcap_intfset_t intfset;

    if ( dst == NULL || intfset_p == NULL || dst_len <= 0 ) return errlogargs();
    
    intfset = *intfset_p;

    /* Clear out the string */
    dst[dst_len-1] = dst[0] = '\0';
    
    /* Empty list if none of the bits or set, or all of the bits are set */
    /* XXX Technically this should be an error if no bits are set */
    if ( intfset == 0 || ( intfset & INTF_SET_ALL_MASK )) return 0;
    
    /* Iterate through the bits and append to the list */
    for ( c = 0 ; c < _if_count ; c++ ) {
        if ( intfset & ( 1 << c ) ) {
            count = snprintf(dst,dst_len,"%d ", c);
            if  ( count >= dst_len ) {
                return errlog(ERR_CRITICAL, "intfset: string buffer is too small\n");
            }
            dst += count;
            dst_len -= count;
        }
    }

    return 0;
}

int netcap_intfset_to_rule( netcap_intfset_t* intfset_p, char *str, int str_len, int if_in )

{
    netcap_intfset_t intfset;
    int c;
    int mask;
    
    if ( str == NULL || intfset_p == NULL || str_len <= 0 ) return errlogargs();

    intfset = *intfset_p;
    
    /* Clear out the response */
    str[0] = '\0';
    
    /* Do nothing if the user requests all interfaces */
    if ( intfset & INTF_SET_ALL_MASK || intfset == 0 ) return 0;
    
    mask = 1;

    for ( c = 0 ; c < _if_count ; c++ ) {
        if ( intfset & mask) {
            snprintf(str, str_len," --physdev-%s %s ",
                     (if_in==1) ? "in" : "out", _if_names[c]);

            /* Turn off that bit */
            *intfset_p &= ~(mask);

            return ( *intfset_p ) ? 1 : 0;
        }
        mask <<= 1;
    }
    
    return errlog(ERR_WARNING, "Invalid netcap_intfset_t: %#010x\n",intfset);
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
}

static int _interface_update_addrs( void )
{
    struct ifconf conf;
    struct ifreq interfaces[NETCAP_MAX_INTERFACES];
    int i, ret = 0;
    int sockfd;
    
    debug( 1, "Updating interface addresses\n" );
        
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
                debug(4,"IP        : %s\n",inet_ntoa(addr));
                debug(4,"Netmask   : %s\n",inet_ntoa(netmask));
                debug(4,"Broadcast : %s\n",inet_ntoa(broadcast));
                
                _if_addrs[_num_if]    = addr.s_addr;
                _broadcasts[_num_if]  = broadcast.s_addr;
                _netmasks[_num_if]    = netmask.s_addr;
                
                _num_if++;            
            }
        }
    } while ( 0 );

    if ( close( sockfd ) < 0 ) 
        return perrlog( "close" );
    
    return ret;
}

static int _get_interface_address ( char* interface_name, in_addr_t* address, in_addr_t* netmask )
{
    int sockfd, ret = 0;
    struct ifreq ifr;
    
    if (( sockfd = socket( PF_INET, SOCK_DGRAM, 0 )) < 0 ) return perrlog( "socket" );

    do {
        strncpy( ifr.ifr_name, interface_name, sizeof( ifr.ifr_name ));

        /* Get the address */
        if ( ioctl( sockfd, SIOCGIFADDR, &ifr ) < 0 ) { ret = perrlog( "ioctl" ); break; }
        *address = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr.s_addr;

        /* Get the netmask */
        if ( ioctl( sockfd, SIOCGIFNETMASK, &ifr ) < 0 ) { ret = perrlog( "ioctl" ); break; }
        *netmask = (*(struct sockaddr_in*)&ifr.ifr_netmask).sin_addr.s_addr;
    } while ( 0 );

    if ( close ( sockfd )  < 0 ) return perrlog( "close" );
    
    return ret;
}

/* Setup or reomve all of the locally destined packets */
static int _modify_local_marks( char* intf_name, int intf_mark, int if_add )
{
    int c;
    char add_del;
    char insert_cmd[MAX_CMD_LEN];
    int mark;

    add_del = ( if_add == RULES_ADD ) ? 'A' : 'D';

/* XXXX This is somewhat of a hack since we cannot modify part of the mark, 
 * if we could then you would just need one rule per address.
 * Instead we have to create all combinations of addresses and interfaces
 */
#define LOCAL_MARK_BASE "/sbin/iptables -t mangle -%c " INTERFACE_CHAIN " -m physdev " \
                  " --destination %s --physdev-in %s -j MARK --set-mark %d"

    for ( c = 0 ; c < _num_if ; c++ ) {
        mark = intf_mark | MARK_LOCAL | (( c + 1 ) << MARK_LOCAL_OFFSET);
        /* Build the command for each interface, Mark interface eth0 with 1, eth1 with 2, etc*/
        if ( snprintf( insert_cmd, sizeof( insert_cmd ), LOCAL_MARK_BASE, add_del, 
                       unet_inet_ntoa( _if_addrs[c]), intf_name, mark ) < 0 ) {
            return perrlog( "snprintf" );
        }

        if ( mvutil_system ( insert_cmd ) < 0 ) {
            return perrlog("mvutil_system");
        } else {
            debug(5,"NETCAP: Run Command: '%s' \n", insert_cmd);
        }
    }
    

    
    return 0;
}


