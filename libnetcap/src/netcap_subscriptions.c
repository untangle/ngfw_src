/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_subscriptions.c,v 1.10 2005/02/26 03:39:43 rbscott Exp $
 */
#include <stdlib.h>
#include <unistd.h>
#include <pthread.h>

#include <netinet/in.h>
#include <netinet/udp.h>
#include <mvutil/hash.h>
#include <mvutil/unet.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/usystem.h>

#include <libnetcap.h>

#include "netcap_subscriptions.h"
#include "netcap_traffic.h"
#include "netcap_rdr.h"
#include "netcap_server.h"
#include "netcap_queue.h"
#include "netcap_globals.h"
#include "netcap_init.h"
#include "netcap_antisubscribe.h"

/* The number of sockets to listen on for TCP */
#define RDR_TCP_LOCALS_SOCKS 128

static ht_t _subscriptions;
static int  _subscription_next_id;

static pthread_mutex_t _id_mutex = PTHREAD_MUTEX_INITIALIZER;

static int _netcap_unsubscribe (netcap_sub_t* sub);

int netcap_subscriptions_init (void)
{
    int ret;

    if (ht_init(&_subscriptions, SUBSCRIPTION_TABLE_SIZE, int_hash_func, int_equ_func, HASH_FLAG_KEEP_LIST)<0)
        return perrlog("ht_init");

    /* Insert the antisubscribe chain */
    ret = system( "/sbin/iptables -t mangle -N " ANTISUBSCRIBE_CHAIN );

    ret = WEXITSTATUS( ret );

    /* 0 means sucessful, 1 just means the chain already exists */
    if ( ret != 0 && ret != 1 ) {
        return errlog( ERR_CRITICAL, "Unable to add ANTISUBSCRIBE chain '" ANTISUBSCRIBE_CHAIN "'(%d)\n", ret );
    }
    
    if ( mvutil_system( "/sbin/iptables -t mangle -A PREROUTING -j " ANTISUBSCRIBE_CHAIN ) < 0 ) {
        errlog( ERR_CRITICAL, "Unable to insert jump to anti-subscribe chain\n" );
    }

    _subscription_next_id = 100;
    
    return 0;
}

int netcap_subscriptions_cleanup (void)
{
    int ret;

    if (ht_destroy(&_subscriptions)>0)
        errlog(ERR_WARNING,"Entries left in subscription table\n");

    if ( pthread_mutex_destroy ( &_id_mutex ) < 0 ) perrlog ( "pthread_mutex_destroy" );
    
    if ( mvutil_system( "/sbin/iptables -t mangle -D PREROUTING -j " ANTISUBSCRIBE_CHAIN ) < 0 ) {
        errlog( ERR_CRITICAL, "Unable to remove jump to anti-subscribe chain\n" );
    }
    
    /* Remove the antisubscribe chain */
    ret = system( "/sbin/iptables -t mangle -X " ANTISUBSCRIBE_CHAIN );

    ret = WEXITSTATUS( ret );

    /* 0 means sucessful, 1 just means the chain already exists */
    if ( ret != 0 && ret != 1 ) {
        errlog( ERR_CRITICAL, "Unable to remove anti-subscribe chain: '" ANTISUBSCRIBE_CHAIN "'(%d)\n", ret );
    }

    
    return 0;
}

/**
 * adds a subscription to the netcap system
 * things that are zero or NULL will be counted as wildcards \n
 * the protocol must be IPPROTO_TCP or IPPROTO_UDP \n
 *
 * what this does:
 * 1) creates a subscription object, inits it
 * 2) initializeds the traffic in the subscription
 * 3) opens a socket for the tcp server to listen on 
 * 4) inits the rdr (that redirects to that socket)
 * 5) inserts the rdr
 * 6) signals the server to update its sublist
 *
 * returns  the traffic_id number or -1 on failure
 */
int netcap_subscribe (int flags, void* arg, int proto, 
                      netcap_intfset_t cli_intfset,  netcap_intfset_t srv_intfset,
                      in_addr_t* src, in_addr_t* shost_netmask, u_short src_port_min, u_short src_port_max,
                      in_addr_t* dst, in_addr_t* dhost_netmask, u_short dst_port_min, u_short dst_port_max)
{
    netcap_sub_t* sub;
    u_short       port;
    char port_str[50];
    int           socks[RDR_TCP_LOCALS_SOCKS];
    int           sock_count = -1;

    TEST_INIT();

    bzero( socks, sizeof( socks ));

    if ( (sub = subscription_create(arg)) == NULL) {
        return perrlog("subscription_create");
    }

    if (ht_add(&_subscriptions,(void*)sub->sub_id,sub)<0) {
        subscription_free(sub);
        return perrlog("ht_add");
    }
    
    if (netcap_traffic_init(&sub->traf,proto,0,0,
                            src,shost_netmask,src_port_min,src_port_max,
                            dst,dhost_netmask,dst_port_min,dst_port_max)<0) {
        subscription_free(sub);
        return perrlog("netcap_traffic_init");
    }
    
    sub->traf.cli_intfset = cli_intfset;
    sub->traf.srv_intfset = srv_intfset;
        
    /**
     * open a socket for this rdr for non-antisubscribes
      */
    if (( flags & NETCAP_FLAG_ANTI_SUBSCRIBE ) == 0 ) {
        if (proto == IPPROTO_ICMP || proto == IPPROTO_ALL) {
            sock_count = -1;
            port = 0;
        }
        else if (proto == IPPROTO_TCP) {
            struct ip_sendnfmark_opts opts = {1,NETCAP_SYNACK_MARK};
            int c;
            
            if ( unet_startlisten_on_portrange( RDR_TCP_LOCALS_SOCKS, &port, socks ) < 0 ) {
                subscription_free( sub );
                return errlog( ERR_CRITICAL, "unet_startlisten_on_portrange\n" );
            }
            
            /* Change the socket options on all of the sockets */
            for ( c = 0 ; c < RDR_TCP_LOCALS_SOCKS ; c++ ) {
                if (setsockopt( socks[c], SOL_IP, IP_SENDNFMARK,  &opts, sizeof( opts )) < 0) 
                    perrlog("setsockopt");
            }
            
            /* Install a port guard on that port for all interfaces */
            /* Somewhat of a hack to work around */
            snprintf( port_str, sizeof( port_str ), "[%d:%d ", port, port + RDR_TCP_LOCALS_SOCKS -1 );
            netcap_interface_station_port_guard( NC_INTF_UNK, IPPROTO_TCP, port_str, NULL );
            
            sock_count = RDR_TCP_LOCALS_SOCKS;
        }
        else if (proto == IPPROTO_UDP) {
            int one = 1;
            
            if ( unet_startlisten_on_anyport_udp( &port, &socks[0] ) < 0 ) {
                subscription_free(sub);
                return perrlog("unet_startlisten_on_anyport_udp");
            }
            
            /* Install a port guard on that port for all interfaces */
            /* Somewhat of a hack to work around */
            snprintf( port_str, sizeof( port_str ), " %d ", port );
            netcap_interface_station_port_guard( NC_INTF_UNK, IPPROTO_UDP, port_str, NULL );
            
            sock_count = 1;
            
            /* set all the options */
            if (setsockopt(socks[0], SOL_IP,     IP_PKTINFO,  &one, sizeof(one)) < 0) 
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_SOCKET, SO_BROADCAST,&one, sizeof(one)) < 0) 
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_IP,     IP_RECVTOS,  &one, sizeof(one)) < 0) 
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_IP,     IP_RECVTTL,  &one, sizeof(one)) < 0)
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_IP,     IP_RETOPTS,  &one, sizeof(one)) < 0) 
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_UDP,    UDP_RECVDPORT, &one, sizeof(one)) < 0) 
                perrlog("setsockopt");
            if (setsockopt(socks[0], SOL_IP,     IP_RECVNFMARK,  &one, sizeof(one)) < 0) 
                perrlog("setsockopt");
        }
        else {
            return errlog(ERR_CRITICAL,"Unknown protocol\n");
        }
    } else {
        sock_count = -1;
        port = 0;        
    }
    
    /**
     * Initialize the redirect
     */
    if ( rdr_init( &sub->rdr, &sub->traf, flags, port, socks, sock_count ) < 0 ) {
        int c;
        subscription_free( sub );
        if ( netcap_traffic_destroy( &sub->traf ) < 0 ) {
            errlog(ERR_CRITICAL,"netcap_traffic_destroy");
        }
        
        for ( c = 0 ; c < sock_count ; c++ )
            if ( socks[c] > 0 && close( socks[c] ) < 0 )
                perrlog( "close" );
        
        return perrlog("rdr_init");
    }

    if (rdr_insert(&sub->rdr)<0) {
        subscription_free(sub);
        netcap_traffic_destroy(&sub->traf);
        rdr_destroy(&sub->rdr);
        return perrlog("rdr_insert");
    }

    /* Check for a local anti-subscribe */
    if ( sub->rdr.flags & NETCAP_FLAG_LOCAL_ANTI_SUBSCRIBE ) {
        netcap_local_antisubscribe_remove();
    }
    
    /**
     * Tell the server about the new subscription
     */
    if (netcap_server_sndmsg(NETCAP_MSG_ADD_SUB,sub)<0)
        perrlog("netcap_server_sndmsg");

    return sub->sub_id;
}

/**
 * removes a subscription
 *
 * returns 0 or -1 on failure
 */
int netcap_unsubscribe (int sub_id)
{
    netcap_sub_t* sub = ht_lookup(&_subscriptions,(void*)sub_id);
    int ret;

    TEST_INIT();

    if (!sub) {
        return errlog(ERR_CRITICAL,"Subscription not found\n");
    }

    if ( ( ret = _netcap_unsubscribe (sub)) < 0 ) {
        errlog(ERR_CRITICAL,"_netcap_unsubscribe");
    }
    
    return ret;
}

/**
 * removes all subscriptions
 *
 * returns 0 or -1 on failure
 */
int netcap_unsubscribe_all ()
{
    list_node_t* step;
    list_t* list = ht_get_content_list(&_subscriptions);
        
    TEST_INIT();

    if (!list)
        return perrlog("ht_get_content_list");

    debug(3,"NETCAP: Removing All Subs (%i)\n",list_length(list));

    for (step = list_head(list); step ; step = list_node_next(step)) {
        netcap_sub_t* sub = list_node_val(step);
        
        if (!sub) {
            errlog(ERR_CRITICAL,"Constraint Failed");
        } else {
            debug(3,"NETCAP: Remove Sub %i\n",sub->sub_id);
            _netcap_unsubscribe(sub);
        }
    }

    list_destroy(list);
    list_free(list);

    return 0;
}


netcap_sub_t* netcap_subscription_malloc(void)
{
    netcap_sub_t* sub = calloc(1,sizeof(netcap_sub_t));

    if ( sub == NULL ) return errlogmalloc_null();

    return sub;
}

int netcap_subscription_init (netcap_sub_t* sub, void* arg)
{
    int sub_id;
    
    pthread_mutex_lock ( &_id_mutex );
    sub_id = ++_subscription_next_id;
    pthread_mutex_unlock ( &_id_mutex );
    
    sub->sub_id = sub_id;
    sub->arg = arg;
    
    return 0;
}

netcap_sub_t* netcap_subscription_create ( void *arg)
{
    netcap_sub_t* sub;

    sub = netcap_subscription_malloc();

    if ( sub == NULL ) return errlog_null(ERR_CRITICAL,"netcap_subscription_malloc");

    if ( netcap_subscription_init(sub,arg) < 0 ) {
        return errlog_null(ERR_CRITICAL,"netcap_subscription_init");
    }

    return sub;
}


int netcap_subscription_free (netcap_sub_t* sub)
{
    if (!sub) {
        return errlogargs();
    }

    free(sub);
    
    return 0;
}

int netcap_subscription_destroy (netcap_sub_t* sub)
{
    if ( sub == NULL ) {
        return errlogargs();
    }

    return 0;
}

int netcap_subscription_raze (netcap_sub_t* sub)
{
    int err = 0;

    if ( sub == NULL ) {
        return errlogargs();
    }
    
    if ( netcap_subscription_destroy(sub) < 0 ) {
        errlog(ERR_CRITICAL,"netcap_subscription_destroy");
        err-=1;
    }

    if ( netcap_subscription_free(sub) < 0 ) {
        errlog(ERR_CRITICAL,"netcap_subscription_free");
        err -=2;
    }

    return err;
}

int netcap_subscription_is_subset ( int sub_id, netcap_traffic_t* traf ) {
    netcap_sub_t* sub;

    if ( (sub = (netcap_sub_t*)ht_lookup(&_subscriptions,(void*)sub_id)) == NULL ) {
        errlog(ERR_WARNING,"Subscription not found\n");
        return 0;
    }

    if (sub->rdr.flags & NETCAP_FLAG_IS_FAKE)
        return 0;
    else
        return netcap_traffic_is_subset(&sub->traf, traf);
}

static int _netcap_unsubscribe (netcap_sub_t* sub)
{
    char port_str[50];
        
    if (ht_remove(&_subscriptions,(void*)sub->sub_id)<0) {
        perrlog("ht_remove");
    }
    
    if (rdr_remove(&sub->rdr)<0) {
        errlog(ERR_CRITICAL,"Redirect remove failed\n");
    }

    /* Check for the local anti-subscribe */
    if ( sub->rdr.flags & NETCAP_FLAG_LOCAL_ANTI_SUBSCRIBE ) {
        netcap_local_antisubscribe_add();
    }

    /* If this is TCP or UDP, remove the guards, only if the port is set (not set for antisubscribes */
    if ( sub->rdr.port_min > 0 ) {
        if ( sub->traf.protocol == IPPROTO_TCP ) {
            snprintf( port_str, sizeof( port_str ), "[%d:%d ", sub->rdr.port_min, sub->rdr.port_max );
            netcap_interface_relieve_port_guard( NC_INTF_UNK, IPPROTO_TCP, port_str, NULL );
        } else if ( sub->traf.protocol == IPPROTO_UDP ) {
            snprintf( port_str, sizeof( port_str ), "%d ", sub->rdr.port_min );
            netcap_interface_relieve_port_guard( NC_INTF_UNK, IPPROTO_UDP, port_str, NULL );
        }
    }

    /**
     * At this point we will send a message to the server
     * The server will then call traffic_destroy, rdr_destroy, and sub_destroy, sub_free
     */
    if (netcap_server_sndmsg(NETCAP_MSG_REM_SUB,sub)<0) {
        perrlog("netcap_server_sndmsg");
    }

    return 0;
}



