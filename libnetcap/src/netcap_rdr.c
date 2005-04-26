/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include "netcap_rdr.h"

#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <pthread.h>
#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/usystem.h>
#include "libnetcap.h"
#include "netcap_globals.h"
#include "netcap_traffic.h"
#include "netcap_interface.h"
#include "netcap_subscriptions.h"

static pthread_mutex_t _exec_mutex = PTHREAD_MUTEX_INITIALIZER;

static int _rdr_insert_iptables(rdr_t * rdr);
static int _rdr_create_iptables(rdr_t * rdr, netcap_traffic_t* traf);
static int _rdr_remove_iptables(rdr_t * rdr);

static int _rdr_exec ( char* cmd );

/* Build a string out of the src components of the traffic structure */
#define _netcap_rdr_build_host_src(str,dir) \
  _netcap_rdr_build_host((str),(dir),traf->shost_used,traf->shost,traf->shost_netmask)

/* Build a string out of the dst components of the traffic structure */
#define _netcap_rdr_build_host_dst(str,dir) \
  _netcap_rdr_build_host((str),(dir),traf->dhost_used,traf->dhost,traf->dhost_netmask)

#define _netcap_rdr_build_port_src(str,dir) \
  _netcap_rdr_build_port((str),(dir),traf->src_port_range_flag,traf->sport_low,traf->sport_high,traf->sport)

#define _netcap_rdr_build_port_dst(str,dir) \
  _netcap_rdr_build_port((str),(dir),traf->dst_port_range_flag,traf->dport_low,traf->dport_high,traf->dport)

static __inline__ void _netcap_rdr_build_host ( char *host_str, char *dir, 
                                                u_char used,
                                                in_addr_t addr, in_addr_t netmask )
{
    char host[INET_ADDRSTRLEN];
    char mask[INET_ADDRSTRLEN];

    /* Clear out the host */
    host_str[0] = '\0';
    
    if ( !used ) return;
    
    if (!inet_ntop(AF_INET,(struct in_addr *)&addr,host,sizeof(host))) {
        errlog(ERR_CRITICAL,"inet_ntop failed\n");
    } else {
        if (!inet_ntop(AF_INET,(struct in_addr *)&netmask,mask,sizeof(mask))) {
            errlog(ERR_CRITICAL,"inet_ntop failed\n");
        } else {
            snprintf(host_str,MAX_CMD_LEN," --%s %s/%s ",dir, host, mask);
        }
    }
}

static __inline__ void _netcap_rdr_build_port ( char* port_str,char* dir,
                                                u_short port_range_flag,
                                                u_short port_low, u_short port_high,
                                                u_short port )
{
    /* Clear out the port string */
    port_str[0] = '\0';

    if ( port_range_flag ) {
        if (!((port_low == 0 && port_high == 0) || 
              (port_low == 1 && port_high == 65535))) {
            snprintf(port_str,MAX_CMD_LEN, " --%s-port %i:%i ",dir,port_low,port_high);
        }
    } else {
        if ( port != 0 ) {
            snprintf(port_str,MAX_CMD_LEN, " --%s-port %i ",dir,port);
        }
    }
}

rdr_t* rdr_malloc (void)
{
    rdr_t* newrdr;
    
    if (( newrdr = calloc( 1, sizeof( rdr_t ))) == NULL ) return errlogmalloc_null();

    return newrdr;
}

int    rdr_init (rdr_t* rdr, netcap_traffic_t* traf, int flags, u_short port, int* socks, int sock_count )
{
    if (!rdr || !traf)
        return errlogargs();

    if ( sock_count > 0 ) {
        if ( socks == NULL )
            return errlogargs();
        
        if ( port == 0 )
            return errlog( ERR_CRITICAL, "Must provide a non-zero port with a sock_count > 0\n" );

        if (( rdr->socks = malloc( sock_count * sizeof( int ))) == NULL )
            return errlogmalloc();

        memcpy( rdr->socks, socks, sock_count * sizeof( int ));

        rdr->port_min       = port;
        rdr->port_max       = port + sock_count - 1;

    } else {
        rdr->socks = NULL;
        rdr->port_min = 0;
        rdr->port_max = 0;
    }

    rdr->flags          = flags;
    rdr->iptables_count = 0;
    
    if ( _rdr_create_iptables( rdr, traf ) < 0) 
        return errlog(ERR_CRITICAL,"Error creating redirect\n");
    
    return 0;
}

rdr_t* rdr_create (netcap_traffic_t* traf, int flags, u_short port, int* socks, int sock_count )
{
    rdr_t* rdr = rdr_malloc();

    if ( rdr == NULL ) {
        return errlog_null( ERR_CRITICAL, "rdr_malloc" );
    }

    if ( rdr_init( rdr, traf, flags, port, socks, sock_count ) < 0 ) {
        return perrlog_null("rdr_init");
    }

    return rdr;
}

int    rdr_free ( rdr_t* rdr )
{
    if (!rdr) {
        return -1;
    }

    free(rdr);

    return 0;
}

int    rdr_destroy( rdr_t* rdr )
{
    int c;
    int size;

    if ( rdr == NULL ) {
        return errlogargs();
    }
    
    if ( rdr->socks != NULL ) {
        size = rdr->port_max - rdr->port_min + 1;
        
        for ( c = 0 ; c < size ; c++ ) {
            if ( rdr->socks[c] > 0 && close( rdr->socks[c] ))
                perrlog( "close" );
            
            rdr->socks[c] = -1;
        }
        
        free( rdr->socks );
        rdr->socks = NULL;
    }

    for ( c = 0 ; c < rdr->iptables_count ; c++ ) {
        if ( rdr->iptables_insert_cmd[c] )  free( rdr->iptables_insert_cmd[c] );
        if ( rdr->iptables_remove_cmd[c] )  free( rdr->iptables_remove_cmd[c] );
    }

    return 0;
}

int    rdr_raze (rdr_t* rdr)
{
    int err =0;

    if (rdr_destroy(rdr)<0) {
        perrlog("rdr_destroy");
        err -= 1;
    }

    if (rdr_free(rdr)<0) {
        perrlog("rdr_free");
    }

    return err;
}

int    rdr_insert (rdr_t* rdr)
{
    int ret = -1;
    
    ret =  _rdr_insert_iptables(rdr);

    if (ret == 0)
        debug(10,"NETCAP: Redirect insertion complete.\n");
    else
        errlog(ERR_WARNING,"Redirect insertion failed.\n");
    
    return ret;
}

int    rdr_remove (rdr_t* rdr)
{
    int ret = -1;
    
    ret = _rdr_remove_iptables(rdr);

    if (ret == 0) {
        debug(10,"NETCAP: Redirect removal complete.\n");
    } else {
        errlog(ERR_WARNING,"Redirect removal failed.\n");
    }

    return ret;
}

static int  _rdr_insert_iptables (rdr_t* rdr)
{
    int i;
    
    if (!rdr)
        return errlogargs();

    for (i=0;i<rdr->iptables_count;i++) {
        if (rdr->iptables_remove_cmd[i]) {
            if ( _rdr_exec( rdr->iptables_insert_cmd[i] ) < 0 )
                perrlog("system");
            else
                debug(5,"NETCAP: Inserted redirect : '%s' \n",rdr->iptables_insert_cmd[i]);
        }
        else
            errlog(ERR_CRITICAL,"Constraint failed\n");
    }

    return 0;
}

#define IF_INTFSET_TRUE  1
#define IF_INTFSET_FALSE 0

static void _rdr_create_rules ( char *rule, rdr_t* rdr, int if_intfset, 
                                netcap_intfset_t *cli_intfset_p)
{
    int c;
    netcap_intfset_t cli_intfset;
    char cli_intf_str[MAX_CMD_LEN];
    char* mphysdev_str = "";

    if ( cli_intfset_p == NULL ) {
        errlogargs(); 
        return;
    }

    cli_intfset = *cli_intfset_p;

    if ( rdr->iptables_count >=MAX_CMD_IPTABLES_PER_RDR ) {
        errlog(ERR_CRITICAL,"Too many redirect rules in a rdr_t structure.");
        return;
    }
    
    if ( if_intfset ) {
        /* Create at least one rule */
        do {
            c = netcap_intfset_to_rule ( &cli_intfset, cli_intf_str, 
                                         sizeof(cli_intf_str), NC_INTF_SET_TO_RULE_IN);
            
            if ( c < 0 ) {
                errlog(ERR_WARNING,"netcap_intfset_to_rule");
                return;
            }
            
            mphysdev_str = ( cli_intf_str[0] == '\0' ) ? "" : " -m physdev ";
            
            snprintf(rdr->iptables_insert_cmd[rdr->iptables_count],MAX_CMD_LEN,rule,
                     mphysdev_str,'A',cli_intf_str);
            snprintf(rdr->iptables_remove_cmd[rdr->iptables_count],MAX_CMD_LEN,rule,
                     mphysdev_str,'D',cli_intf_str);
            
            rdr->iptables_count++;
        } while ( rdr->iptables_count < MAX_CMD_IPTABLES_PER_RDR && c );
    } else {
        snprintf(rdr->iptables_insert_cmd[rdr->iptables_count],MAX_CMD_LEN, rule, 'A');
        snprintf(rdr->iptables_remove_cmd[rdr->iptables_count],MAX_CMD_LEN, rule, 'D');
        rdr->iptables_count++;
    }
}

static int _rdr_create_iptables (rdr_t* rdr, netcap_traffic_t* traf)
{
    char src_str    [MAX_CMD_LEN];
    char dst_str    [MAX_CMD_LEN];
    char srcport_str[MAX_CMD_LEN];
    char dstport_str[MAX_CMD_LEN];
    char rule[MAX_CMD_LEN];
    char* protocol;
    char* prefix;
    char* reject_action;
    int i;
    
    if (!rdr || !traf) return errlogargs();

    if (rdr->flags & NETCAP_FLAG_SUDO) {
        prefix = "sudo ";
    } else {
        prefix = "";
    }

    for (i=0;i<MAX_CMD_IPTABLES_PER_RDR;i++) {
        if (!rdr->iptables_remove_cmd[i]) 
            rdr->iptables_remove_cmd[i]  = malloc(MAX_CMD_LEN);
        if (!rdr->iptables_insert_cmd[i]) 
            rdr->iptables_insert_cmd[i]  = malloc(MAX_CMD_LEN);
        if (!rdr->iptables_insert_cmd[i] || !rdr->iptables_remove_cmd[i])
            return errlogmalloc();

        strncpy(rdr->iptables_insert_cmd[i],"", MAX_CMD_LEN);
        strncpy(rdr->iptables_remove_cmd[i],"", MAX_CMD_LEN);
    }


    /**
     * start building the system calls 
     */
    strcpy(src_str,"");
    strcpy(dst_str,"");
    strcpy(srcport_str,"");
    strcpy(dstport_str,"");

    switch(traf->protocol) {
    case IPPROTO_TCP:  protocol = "-p tcp"; reject_action = "tcp-reset"; break;
    case IPPROTO_UDP:  protocol = "-p udp"; reject_action = "icmp-host-prohibited"; break;
    case IPPROTO_ICMP: protocol = "-p icmp"; reject_action = "icmp-host-prohibited"; break;
    case IPPROTO_ALL:  protocol = ""; reject_action = "icmp-host-prohibited"; break;
    default: 
        errlog(ERR_WARNING,"Invalid protocol - defaulting to tcp\n");
        protocol = "-p tcp"; reject_action = "icmp-host-prohibited"; break;
        traf->protocol = IPPROTO_TCP;
    }
    
    _netcap_rdr_build_host_src(src_str,"source");
    _netcap_rdr_build_host_dst(dst_str,"destination");

    _netcap_rdr_build_port_src(srcport_str,"source");
    _netcap_rdr_build_port_dst(dstport_str,"destination");
    
#define BIN_BASE "%s /sbin/iptables %s"
#define RULE_BASE " -m mark ! --mark " MARK_S_MASK_ANTISUB " %s%s%s %s%s%s%s "
#define RULE_BASE_NOMARK " %s%s%s %s%s%s%s "
#define ANTISUB_RULE_BASE " -m mark --mark %d/%d %s%s%s %s%s%s%s "
    
    if (rdr->flags & NETCAP_FLAG_ANTI_SUBSCRIBE) {
        /* Don't want to mark things that are already antisubscribed */
        int mark = 0;
        int mark_mask = MARK_ANTISUB;

        if ( rdr->flags & NETCAP_FLAG_LOCAL ) {
            mark |= MARK_LOCAL;
            mark_mask |= MARK_LOCAL;
            src_str[0] = '\0';
            dst_str[0] = '\0';
        }
        
        snprintf(rule, MAX_CMD_LEN, 
                 BIN_BASE" -t mangle -%%c " ANTISUBSCRIBE_CHAIN ANTISUB_RULE_BASE " -j MARK --set-mark " \
                 MARK_S_ANTISUB,
                 prefix, "%s", mark, mark_mask,
                 protocol,"%s","",
                 src_str,srcport_str,dst_str,dstport_str);

        // Create the two insert and remove rules
        _rdr_create_rules(rule,rdr, IF_INTFSET_TRUE, &traf->cli_intfset);
        
        if (( rdr->flags & NETCAP_FLAG_NO_REVERSE ) == 0 ) {
            /* ANTI subscribes need the reverse rule */
            if ( rdr->flags & NETCAP_FLAG_LOCAL ) {
                mark |= MARK_LOCAL;
                mark_mask |= MARK_LOCAL;
                src_str[0] = '\0';
                dst_str[0] = '\0';
            } else {
                _netcap_rdr_build_host_src(src_str,"destination");
                _netcap_rdr_build_host_dst(dst_str,"source");
            } 
            
            _netcap_rdr_build_port_src(srcport_str,"destination");
            _netcap_rdr_build_port_dst(dstport_str,"source");
            
            snprintf(rule, MAX_CMD_LEN, 
                     BIN_BASE" -t mangle -%%c " ANTISUBSCRIBE_CHAIN ANTISUB_RULE_BASE " -j MARK --set-mark " \
                     MARK_S_ANTISUB,
                     prefix, "%s", mark, mark_mask, 
                     protocol,"%s","",
                     src_str,srcport_str,dst_str,dstport_str);
            
            // Create the two insert and remove rules
            _rdr_create_rules(rule,rdr, IF_INTFSET_TRUE, &traf->srv_intfset);
        }
    }
    else if (traf->protocol == IPPROTO_TCP) {
        /* ??? What should happen here */
        if ( rdr->port_max < rdr->port_min )
            errlog( ERR_CRITICAL, "port_max(%d) < port_min(%d)", rdr->port_max, rdr->port_min );

        snprintf( rule, MAX_CMD_LEN,
                  BIN_BASE" -t nat -%%c PREROUTING "RULE_BASE" -j REDIRECT --to-ports %i-%i",
                  prefix,"%s",
                  protocol,"%s","",
                  src_str,srcport_str,dst_str,dstport_str,
                  rdr->port_min, rdr->port_max );
            
        _rdr_create_rules( rule, rdr, IF_INTFSET_TRUE, &traf->cli_intfset );
                    
        if (rdr->flags & NETCAP_FLAG_BLOCK_CURRENT) {
            snprintf(rule,MAX_CMD_LEN,
                     BIN_BASE" -t filter -%%c FORWARD "RULE_BASE" -j REJECT --reject-with %s",
                     prefix,"%s",
                     protocol,"%s","",
                     src_str,srcport_str,dst_str,dstport_str,
                     reject_action);
            
            _rdr_create_rules ( rule, rdr, IF_INTFSET_TRUE, &traf->cli_intfset);
        }

        if (rdr->flags & NETCAP_FLAG_CLI_UNFINI) {
            snprintf(rule,MAX_CMD_LEN,
                     BIN_BASE" -t mangle -%%c PREROUTING "RULE_BASE" --tcp-flags SYN,ACK SYN -j QUEUE",
                     prefix,"%s",
                     protocol,"%s","",
                     src_str,srcport_str,dst_str,dstport_str);

            _rdr_create_rules ( rule, rdr, IF_INTFSET_TRUE, &traf->cli_intfset);
        }

    } else if (traf->protocol == IPPROTO_UDP) {
        snprintf(rule,MAX_CMD_LEN,
                 BIN_BASE" -t mangle -%%c PREROUTING "RULE_BASE" -j QUEUE",
                 prefix,"%s",
                 protocol,"%s","",
                 src_str,srcport_str,dst_str,dstport_str );
        
        _rdr_create_rules(rule, rdr, IF_INTFSET_TRUE, &traf->cli_intfset);
        
        _netcap_rdr_build_host_src(src_str,"destination");
        _netcap_rdr_build_host_dst(dst_str,"source");
        
        _netcap_rdr_build_port_src(srcport_str,"destination");
        _netcap_rdr_build_port_dst(dstport_str,"source");
        
        snprintf(rule,MAX_CMD_LEN,
                 BIN_BASE" -t mangle -%%c PREROUTING "RULE_BASE" -j QUEUE",
                 prefix,"%s",
                 protocol,"%s","",
                 src_str,srcport_str,dst_str,dstport_str );
        
        _rdr_create_rules(rule, rdr, IF_INTFSET_TRUE, &traf->srv_intfset);
        
    } else if (traf->protocol == IPPROTO_ICMP) {
        snprintf(rule,MAX_CMD_LEN,
                 BIN_BASE" -t mangle -%%c PREROUTING "RULE_BASE" -j QUEUE",
                 prefix,"%s",
                 protocol,"%s","",
                 src_str,"",dst_str,"");

        _rdr_create_rules(rule, rdr, IF_INTFSET_TRUE, &traf->cli_intfset);

    } else {
        errlog(ERR_CRITICAL,"Failed Constraint. Invalid Subscription\n");
    }
    
    for (i=rdr->iptables_count;i<MAX_CMD_IPTABLES_PER_RDR;i++) {
        free(rdr->iptables_remove_cmd[i]);
        rdr->iptables_remove_cmd[i] = NULL;
        free(rdr->iptables_insert_cmd[i]);
        rdr->iptables_insert_cmd[i] = NULL;
    }

    return 0;
}

static int _rdr_remove_iptables (rdr_t* rdr)
{
    int i;
    
    if (!rdr)
        return errlogargs();

    for (i=0;i<rdr->iptables_count;i++) {
        if (rdr->iptables_remove_cmd[i]) {
            if ( _rdr_exec( rdr->iptables_remove_cmd[i] ) < 0 )
                errlog( ERR_CRITICAL, "_rdr_exec\n" );
            else
                debug(5,"NETCAP: Removed redirect : '%s' \n",rdr->iptables_remove_cmd[i]);
        }
        else
            errlog(ERR_CRITICAL,"Constraint failed\n");
    }

    return 0;
}

static int _rdr_exec ( char* cmd )
{
    int ret = 0;
    
    if ( pthread_mutex_lock( &_exec_mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );
    if (( ret = mvutil_system( cmd )) < 0 ) errlog( ERR_CRITICAL, "mvutil_system\n" );

    /* Just to make sure that two commands do not run on top of one another */
    usleep( 10000 );
    if ( pthread_mutex_unlock( &_exec_mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    return ret;
}

