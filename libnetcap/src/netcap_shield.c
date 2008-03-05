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

#include <pthread.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <math.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>
#include <mvutil/utime.h>
#include <mvutil/unet.h>

#include "libnetcap.h"
#include "netcap_init.h"
#include "netcap_trie.h"
#include "netcap_lru.h"
#include "netcap_trie_support.h"
#include "netcap_shield.h"
#include "netcap_shield_cfg.h"
#include "netcap_load.h"
#include "netcap_sched.h"

/* 1 second in u-seconds */
#define _LOAD_INTERVAL_SEC     1000000

/* A limit check to make sure it doesn't go into a long loop */
#define BLESS_COUNT_MAX        128

/* Flag to indicate that all of the traffic should be passed */
#define _PASS_ALL_FLAG         0x10DDBA11

#define _DEFAULT_DIVIDER       1.0

#define _LRU_TRASH_DELAY       SEC_TO_USEC( 5 )

/* 1 ms in u-seconds */
#define _LOAD_INTERVAL_MS      1000

#define _LOAD_INTERVAL_EVIL    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_SESS    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_CHK     (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_BYTE    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_PRINT   (  5 * _LOAD_INTERVAL_SEC )
#define _LOAD_INTERVAL_LRU     (  5 * _LOAD_INTERVAL_SEC )

#define _SHIELD_FILL_TRASH_INTERVAL ( 5 * _LOAD_INTERVAL_SEC )

/* Flag for whether or not to update the load at this time */
#define _NC_UPDATE_LOAD        0xDA14D

/* Ignore array size */
#define IGNORE_ARRAY_SIZE BLESS_COUNT_MAX

static void            _null_event_hook     ( netcap_shield_event_data_t* data );
    
static struct {
    int                  enabled;

    /* Root reputation */
    nc_shield_reputation_t* root;

    /* The LRU for the trie */
    netcap_lru_t lru;

    /* Iterate through all of the test vectors */
    netcap_trie_t trie;
    
    /* Mutex for the trie and the LRU */
    pthread_mutex_t mutex;

    /* How tight is the list right now */
    netcap_shield_mode_t mode;
    nc_shield_fence_t*   fence;
    
    nc_shield_cfg_t cfg;

#ifdef _TRIE_DEBUG_PRINT
    list_t ip_list;
    /* Cannot run the dump function and the trash at the same time */
    pthread_mutex_t dbg_mutex;
#endif

    netcap_shield_event_hook_t event_hook;
    
    /* Used to return a NULL element for get_end_of_line */
    netcap_trie_element_t null_element;

    /* Used to limit the number of times to print out messages about high reputations */
    struct timeval threshold_last_update;

    /* Mutex to limit the number of times to print out messages about high reputations */
    pthread_mutex_t threshold_mutex;

    /* Ignore array. (XXX, not efficient (and fixed sized), but this requires zero locking.) */
    in_addr_t ignore_array[IGNORE_ARRAY_SIZE];
} _shield = {
    .root       = NULL,
    .mode       = NC_SHIELD_MODE_RELAXED,
    .enabled    = 0,
    .event_hook = _null_event_hook,
    .mutex      = PTHREAD_MUTEX_INITIALIZER,

    .null_element = { 
        .base = NULL
    },

    /* Set the time to something very far in the past */
    .threshold_last_update = {
        .tv_sec = 0,
        .tv_usec = 0
    },

    .threshold_mutex  = PTHREAD_MUTEX_INITIALIZER,

#ifdef _TRIE_DEBUG_PRINT
    .dbg_mutex  = PTHREAD_MUTEX_INITIALIZER
#endif
};

typedef struct _chk {
    u_short  size;               /* Size of the chunk in bytes */
    u_char   if_rx;              /* 1 for rx, 0 for tx XXX Presently unused */
    u_char   protocol;
} _chk_t;

/* update_load is 1 if the load should be updated, zero if only totals should update */
typedef struct {
    int (*func) ( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
    void *arg;
    double divider;
} _apply_func_t;

static void  _trash_fill         ( void* arg );

/** Check if a node is deletable from the LRU */
static int  _lru_is_deletable    ( void* data );
static int  _lru_remove          ( void* data );

/** These are all helper functions that are called by _apply_func */
static int  _add_evil( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _add_request( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _add_session( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _end_session( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _add_chunk( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _add_srv_conn( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );
static int  _add_srv_fail( nc_shield_reputation_t* rep, int count, double divider, void* arg, int update_load );

static int   _apply_func( netcap_trie_element_t element, void* arg, struct in_addr* ip );

static int  _apply_close         ( struct in_addr* ip, _apply_func_t* func );
static int  _apply               ( struct in_addr* ip, _apply_func_t* func );
static int  _check_lru           ( int depth, nc_shield_reputation_t* reputation );

static int _set_node_settings ( double divider, struct in_addr* ip, struct in_addr* netmask );


/** Get a response for <address> and place the result into response.
 *  The reputation used to determine the result is always placed into <reputation>. */
static int  _get_response        ( netcap_shield_response_t* response, nc_shield_reputation_t** reputation,
                                   int protocol, struct in_addr* address );

static int  _get_response_closed ( netcap_shield_response_t* response, nc_shield_reputation_t** reputation,
                                   int protocol, struct in_addr* address );

static int  _reputation_update   ( nc_shield_reputation_t* reputation );

static int _update_score( netcap_trie_element_t line );

static nc_shield_score_t _reputation_eval     ( netcap_trie_item_t* item );
static int             _reputation_init     ( netcap_trie_item_t* item, struct in_addr* ip );
static void            _reputation_destroy  ( netcap_trie_item_t* item );
static netcap_shield_ans_t _put_in_fence    ( nc_shield_fence_t* fence, nc_shield_score_t rep_val, 
                                              nc_shield_reputation_t* rep, int protocol );

static int _is_on_ignore_list( struct in_addr* address );

static int _reset_dividers( void );

static int _clean_lru_and_trie( void );

static netcap_trie_element_t _get_end_of_line ( netcap_trie_line_t* line );

static void _debug_reputation_threshold       ( netcap_trie_line_t* line );

static void _report_intf_event( netcap_intf_t intf, netcap_shield_event_data_t* event, 
                                nc_shield_rejection_counter_t* counter );

static __inline__ nc_shield_fence_t* _get_fence( int mode )
{
    switch ( mode ) {
    case NC_SHIELD_MODE_RELAXED: return &_shield.cfg.fence.relaxed; break;
    case NC_SHIELD_MODE_LAX:     return &_shield.cfg.fence.lax; break;
    case NC_SHIELD_MODE_TIGHT:   return &_shield.cfg.fence.tight; break;
    case NC_SHIELD_MODE_CLOSED:  return &_shield.cfg.fence.closed; break;
    }
    return errlog_null( ERR_CRITICAL, "Invalid mode: %d\n", mode );
}

static int _check_if_print( struct in_addr* ip, nc_shield_reputation_t* rep, 
                            netcap_shield_response_t* response )
{
    int ret = 0;
    int c;
    netcap_shield_event_data_t event;

    /* Never print for the YES responses */
    if ( response->ans == NC_SHIELD_YES ) {
        response->if_print = 0;
        return 0;
    }

    if ( rep->print_load.load > _shield.cfg.print_rate ) {
        response->if_print = 0;
        return 0;
    }
    
    if ( pthread_mutex_lock( &rep->mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );
    
    do {
        /* Check again after the lock to make sure that another thread didn't update the
         * value */
        if ((volatile netcap_load_val_t)(rep->print_load.load) <  _shield.cfg.print_rate ) {
            bzero( &event, sizeof( event ));
            event.type                      = NC_SHIELD_EVENT_REJECTION;
            event.data.rejection.ip         = ip->s_addr;
            event.data.rejection.reputation = rep->score;
            event.data.rejection.mode       = _shield.mode;

            for (  c = 1 ; c <= NC_INTF_MAX ; c++ ) {
                _report_intf_event((netcap_intf_t)c, &event, &rep->counters[c-1] );
            }
            
            netcap_load_update( &rep->print_load, 1, 1 );
            /* In some cases one event will not put the print load high enough */
            if ( rep->print_load.load < _shield.cfg.print_rate ) {
                netcap_load_update( &rep->print_load, 1, 1 );
            }
            response->if_print = 1;
        } else {
            response->if_print = 0;
        }
    } while ( 0 );

    if ( pthread_mutex_unlock( &rep->mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
        
    return ret;
}


#ifdef _TRIE_DEBUG_PRINT
static int   _status             ( int conn, struct sockaddr_in *dst_addr );
#endif

int netcap_shield_init    ( void )
{
    int flags;
    nc_shield_reputation_t rep;

    _shield.enabled = NETCAP_SHIELD_ENABLE;

    /* Zero out the ignore array */
    bzero( _shield.ignore_array, sizeof( _shield.ignore_array ));
    bzero(&rep.lru_node, sizeof(netcap_lru_node_t));

#ifdef _TRIE_DEBUG_PRINT
    if ( list_init( &_shield.ip_list, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"list_init\n");
    }
#endif

    /* Load the default shield configuration */
    if ( nc_shield_cfg_def ( &_shield.cfg ) < 0 ) return errlog ( ERR_CRITICAL, "nc_shield_cfg_def\n" );

    flags = NC_TRIE_INHERIT | NC_TRIE_COPY | NC_TRIE_FREE;
    
    netcap_load_init( &rep.evil_load,     _LOAD_INTERVAL_EVIL,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.request_load,  _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.session_load,  _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.srv_conn_load, _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.srv_fail_load, _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.tcp_chk_load,  _LOAD_INTERVAL_CHK,   NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.udp_chk_load,  _LOAD_INTERVAL_CHK,   NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.icmp_chk_load, _LOAD_INTERVAL_CHK,   NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.byte_load,     _LOAD_INTERVAL_BYTE,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.print_load,    _LOAD_INTERVAL_PRINT, NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.lru_load,      _LOAD_INTERVAL_LRU,   NC_LOAD_INIT_TIME );

    /* Initialize the number of active sessions */
    rep.active_sessions = 0;
    
    /* Initialize the reputation value */
    rep.score = 0;

    /* Initialize the divider */
    rep.divider = _DEFAULT_DIVIDER;
    rep.pass_all = 0;

    /* The init function will initialize the mutex, the functions above, *
     * guarantee that the last update times all get set */
    if ( netcap_trie_init( &_shield.trie, flags, &rep, sizeof( rep ), _reputation_init, 
                           _reputation_destroy ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_root_init\n");
    }

    /* Initialize the LRU */
    if ( netcap_lru_init( &_shield.lru, _shield.cfg.lru.high_water, _shield.cfg.lru.low_water, 
                          _shield.cfg.lru.sieve_size, _lru_is_deletable, _lru_remove ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_lru_init\n" );
    }

    if (( _shield.root = netcap_trie_data ( &_shield.trie )) == NULL ) {
        return errlog(ERR_CRITICAL,"Shield is unitialized\n");
    }

    /* Schedule an event that fills the trash every x second */
    if ( netcap_sched_event ( _trash_fill, NULL, _SHIELD_FILL_TRASH_INTERVAL << 1 ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_sched_event\n" );
    }
    
    /* This must be initialized after the scheduler */
    if ( nc_shield_mode_init( &_shield.cfg, _shield.root, &_shield.mode, &_shield.fence ) < 0 ) {
        return errlog( ERR_CRITICAL, "nc_shield_mode_init\n" );
    }

    return 0;
}

int netcap_shield_cleanup ( void )
{
    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;

    _shield.enabled = 0;

    /* Give any threads time to get out */
    sleep( 1 );

    /* Exit the mode thread */
    nc_shield_mode_cleanup();

    /* Null out the event hook */
    netcap_shield_unregister_hook();

    /* Remove everything from the trie */
    _clean_lru_and_trie();

    netcap_trie_destroy( &_shield.trie );

#ifdef _TRIE_DEBUG_PRINT
    list_destroy  ( &_shield.ip_list );
#endif
    
    return 0;
}

/* Indicate if an IP should allowed in */
int netcap_shield_rep_check ( netcap_shield_response_t* response, struct in_addr* ip, 
                              int protocol, netcap_intf_t intf )
{ 
    nc_shield_reputation_t* reputation = _shield.root;
    
    if ( response == NULL ) return errlogargs();

    response->ans      = NC_SHIELD_YES;
    response->if_print = 0; /* ??? What should the default be */
    
    /* If the shield is not enabled return true */
    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;
        
    /* If things are really bad, do not let anything in */
    if ( _shield.mode == NC_SHIELD_MODE_CLOSED ) {
        if ( _get_response_closed( response, &reputation, protocol, ip ) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_response_closed\n" );
        }        
    } else {        
        /* Update the line of nodes */
        if ( _get_response( response,  &reputation, protocol, ip ) < 0 ) {
            return errlog( ERR_CRITICAL, "_get_response\n" );
        }
    }

    /* XXX This should not be debug 0, but it is kind of strange because this is the client
     * interface which should always be known */
    if ( intf < NC_INTF_0 || intf > NC_INTF_MAX ) {
        debug( 0, "Unable to track session for interface %d, using %d\n", intf, NC_INTF_0 );
        intf = NC_INTF_0;
    }
    
    if ( reputation != NULL ) {
        switch ( response->ans ) {
        case NC_SHIELD_LIMITED: reputation->counters[intf-1].limited++;  break;
        case NC_SHIELD_DROP:    reputation->counters[intf-1].dropped++;  break;
        case NC_SHIELD_RESET:   reputation->counters[intf-1].rejected++; break;
        default: break;
        }
    } else {
        errlog( ERR_CRITICAL, "NULL reputation, ignoring\n" );
    }

    if ( nc_shield_stats_add_request( protocol, response->ans ) < 0 ) {
        errlog( ERR_CRITICAL, "nc_shield_stats_add_session\n" );
    }

    return 0;
}
          
int                  netcap_shield_register_hook    ( netcap_shield_event_hook_t hook )
{
    if ( hook == NULL ) return errlogargs();
    
    _shield.event_hook = hook;
    return 0;
}

void                 netcap_shield_unregister_hook  ( void )
{    
    _shield.event_hook = _null_event_hook;
}

int                  netcap_shield_rep_blame        ( struct in_addr* ip, int amount )
{
    _apply_func_t func = { .func = _add_evil, .arg = (void*)amount };

    /* Do not create nodes, only update the nodes that exist */
    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply_close( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply_close\n" );
    }
    
    return 0;    
}

/* Increment the number of active sessions for ip     *
 * If the ip is not on the list, automatically add it */
int                  netcap_shield_rep_add_request  ( struct in_addr* ip )
{
    _apply_func_t func = { .func = _add_request, .arg = NULL };

    /* Do not create nodes, only update the nodes that exist */
    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply_close( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply_close\n" );
    }
    
    return 0;    
}

/* Decrement the number of active sessions for ip     *
 * If the ip is not on the list, automatically add it */ 
int                  netcap_shield_rep_add_session  ( struct in_addr* ip )
{
    _apply_func_t func = { .func = _add_session, .arg = NULL };

    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply\n" );
    }

    return 0;
}

int                  netcap_shield_rep_end_session  ( struct in_addr* ip )
{
    _apply_func_t func = { .func = _end_session, .arg = NULL };
    
    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply\n" );
    }

    return 0;
}

/* Increment the number of server connections */
int                  netcap_shield_rep_add_srv_conn ( struct in_addr* ip )
{
    _apply_func_t func = { .func = _add_srv_conn, .arg = NULL };

    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply\n" );
    }

    return 0;
}

/* Increment the number of server connections failures */
int                  netcap_shield_rep_add_srv_fail ( struct in_addr* ip )
{
    _apply_func_t func = { .func = _add_srv_fail, .arg = NULL };

    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply\n" );
    }

    return 0;
}

int                  netcap_shield_rep_add_chunk ( struct in_addr* ip, int protocol, u_short size )
{
    _chk_t chk = { .size = size, .if_rx = 1, .protocol = protocol };
    _apply_func_t func = { .func = _add_chunk, .arg = (void*)&chk };

    if ( _shield.enabled == NETCAP_SHIELD_ENABLE && _apply_close( ip, &func ) < 0 ) {
        return errlog( ERR_CRITICAL, "_apply_close\n" );
    }

    return 0;
}

int                  netcap_shield_cfg_load      ( char *buf, int buf_len )
{
    int lw, hw, ss;

    if ( buf == NULL || buf_len < 0 ) return errlogargs();

    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;

    if ( nc_shield_cfg_load ( &_shield.cfg, buf, buf_len ) < 0 ) {
        return errlog ( ERR_CRITICAL, "nc_shield_cfg_load\n" );
    }

    lw = _shield.cfg.lru.low_water;
    hw = _shield.cfg.lru.high_water;
    ss = _shield.cfg.lru.sieve_size;

    /* Configure the LRU */
    if ( netcap_lru_config ( &_shield.lru, hw, lw, ss, &_shield.mutex ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_trie_lru_config\n" );
    }

    return 0;
}

int                  netcap_shield_status        ( int conn, struct sockaddr_in *dst_addr )
{
#ifndef _TRIE_DEBUG_PRINT
    return 0;
#else 
    int ret = 0;

    if ( conn < 0 || dst_addr == NULL ) return errlogargs();

    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;

    if ( pthread_mutex_lock ( &_shield.dbg_mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );
    
    ret = _status ( conn, dst_addr );
    
    if ( pthread_mutex_unlock ( &_shield.dbg_mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    return ret;
#endif
}

int nc_shield_reputation_update( nc_shield_reputation_t* reputation )
{
    int ret = 0;

    if ( reputation == NULL ) return errlogargs();

    if ( pthread_mutex_lock( &reputation->mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    do {
        ret = _reputation_update( reputation );
    } while ( 0 );

    if ( pthread_mutex_unlock( &reputation->mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
    
    return ret;
}

/* Call the event hook */
int nc_shield_event_hook( netcap_shield_event_data_t* event )
{
    _shield.event_hook( event );
    
    return 0;
}

/* Clear the Permanent, and then add nodes to the permanent */
int netcap_shield_bless_users( netcap_shield_bless_array_t* nodes )
{

    int count;

    if ( nodes == NULL || nodes->d == NULL ) return errlogargs();

    if ( nodes->count < 0 ) {
        return errlog( ERR_CRITICAL, "Invalid node count %d\n", nodes->count );
    }

    count = nodes->count;

    if ( nodes->count > BLESS_COUNT_MAX ) {
        errlog( ERR_WARNING, "Node count %d is higher than max, blessing %d nodes\n", nodes->count,
                BLESS_COUNT_MAX );
        count = BLESS_COUNT_MAX;
    }

    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) {
        debug( NC_SHIELD_DEBUG_LOW, "Shield is disabled, ignoring blessings\n" );
        return 0;
    }
   
    debug( NC_SHIELD_DEBUG_LOW, "Blessing %d users\n", nodes->count );

    int _critical_section( void ) {
        /* Iterate all of the items on the permanent LRU and set their dividers to 1.0 */
        if ( _reset_dividers() < 0 ) return errlog( ERR_CRITICAL, "_reset_dividers\n" );       

        if ( netcap_lru_permanent_clear( &_shield.lru, NULL ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_lru_permanent_clear\n" );
        }

        int c;
        int ignore_list_size = 0;
        for ( c = 0 ; c < count ; c++ ) {
            netcap_shield_bless_t* item = &nodes->d[c];
            debug( NC_SHIELD_DEBUG_LOW, "Divider[%d] settings %s %g\n", c,
                   unet_next_inet_ntoa( item->address.s_addr ), item->divider );
            
            if ( _set_node_settings( item->divider, &item->address, &item->netmask ) < 0 ) {
                return errlog( ERR_CRITICAL, "netcap_shield_set_settings\n" );
            }

            if ( item->divider < 0 ) _shield.ignore_array[ignore_list_size++] = item->address.s_addr;
        }

        return 0;
    }

    int ret;

    if ( pthread_mutex_lock( &_shield.mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );
        
    ret = _critical_section();
    
    if ( pthread_mutex_unlock( &_shield.mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
    
    return 0;
}

/* First cut, only bless a specific IP address */
static int _set_node_settings ( double divider, struct in_addr* ip, struct in_addr* netmask )
{
    netcap_trie_line_t      line;
    netcap_trie_element_t   element;
    nc_shield_reputation_t* reputation;
    int pass_all = 0;
    
    if ( divider < 0 ) {
        pass_all = _PASS_ALL_FLAG;
        divider = NC_SHIELD_DIVIDER_MAX;
    } else if ( divider < NC_SHIELD_DIVIDER_MIN ) {
        errlog( ERR_CRITICAL, "divider[%g] must greater than %g, using min\n", 
                divider, NC_SHIELD_DIVIDER_MIN );
        divider = NC_SHIELD_DIVIDER_MIN;
    } else if ( divider > NC_SHIELD_DIVIDER_MAX ) {
        errlog( ERR_CRITICAL, "divider[%g] must less than %g, using max\n", 
                divider, NC_SHIELD_DIVIDER_MAX );
        divider = NC_SHIELD_DIVIDER_MAX;
    }

    if (( ip == NULL ) || ( netmask == NULL )) return errlogargs();

    if ( netmask->s_addr != 0xFFFFFFFF ) {
        errlog( ERR_WARNING, "Netmask[%s] is not implemented for shield settings\n", 
                unet_next_inet_ntoa( ip->s_addr ));
    }

    if ( new_netcap_trie_insert_and_get( &_shield.trie, ip, NULL, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_insert_and_get\n" );
    }
    
    /* For now just apply it to the end of the line */
    element = _get_end_of_line( &line );
    
    if ( element.base == NULL ) return errlog( ERR_CRITICAL, "_get_end_of_line\n" );
    
    if (( reputation = element.base->data ) == NULL ) return errlog( ERR_CRITICAL, "NULL Reputation\n" );
    
    /* Set the divider */
    reputation->divider = divider;
    reputation->pass_all = pass_all;
    
    /* Add the node to the permanent list */
    if ( netcap_lru_permanent_add( &_shield.lru, &reputation->lru_node, element.base, NULL ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_lru_permanent_add\n" );
    }
    
    return 0;
}

static void  _trash_fill         ( void* arg )
{
    if ( _shield.enabled == NETCAP_SHIELD_ENABLE ) {
#ifdef _TRIE_DEBUG_PRINT
        if ( pthread_mutex_lock ( &_shield.dbg_mutex ) < 0 ) perrlog ( "pthread_mutex_lock" );
#endif
        if ( netcap_lru_cut( &_shield.lru, NULL, 0, &_shield.mutex ) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_lru_cut\n" );
        }

        /* Schedule to fill the trash again */
        if ( netcap_sched_event ( _trash_fill, NULL, _SHIELD_FILL_TRASH_INTERVAL ) < 0 ) {
            errlog ( ERR_FATAL, "netcap_sched_event\n" );
        }

#ifdef _TRIE_DEBUG_PRINT
        if ( pthread_mutex_unlock ( &_shield.dbg_mutex ) < 0 ) perrlog ( "pthread_mutex_unlock" );
#endif
    }
}

static int  _apply_func          ( netcap_trie_element_t element, void* arg, struct in_addr* ip )
{
    _apply_func_t* func;
    nc_shield_reputation_t* reputation;
    int ret = 0;
    int children;
    int update_load = 0;

    if ( element.item == NULL || ( reputation = netcap_trie_item_data( element.item )) == NULL ) {
        return errlogargs();
    }
    
    if ( arg == NULL || ((_apply_func_t*)arg)->func == NULL ) return errlogargs();
    
    func = (_apply_func_t*)arg;
    
    if ( pthread_mutex_lock( &reputation->mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    do {
        if (( children = netcap_trie_element_children( element )) < 0 ) {
            ret = errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
            break;
        }

        /* Count is zero if a node doesn't have any children */
        children = ( children == 0 ) ? 1 : children;
        
        /* Apply the function to the argument */
        /* XXXXX 115 is a magic number */
        update_load = ( reputation->score < 115 ) ? _NC_UPDATE_LOAD : ~_NC_UPDATE_LOAD;
        if ( func->func( reputation, children, func->divider, func->arg, update_load ) < 0 ) {
             ret = errlog( ERR_CRITICAL, "apply_function\n" );
            break;
        }
        
        /* Check if this node should move up the LRU */
        if ( _check_lru( element.base->depth, reputation ) < 0 ) {
            ret = errlog( ERR_CRITICAL, "_check_lru\n" );
            break;
        }

        ret = 0;
    } while( 0 );
    
    if ( pthread_mutex_unlock( &reputation->mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
        
    return ret;
}

static int  _lru_is_deletable       ( void* data )
{
    netcap_trie_item_t* item = data;
    nc_shield_reputation_t* reputation;
    netcap_load_val_t lru_load;
    
    if ( item == NULL || ( reputation = netcap_trie_item_data( item )) == NULL ) return errlogargs();

    if (( lru_load = netcap_load_get( &reputation->lru_load )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_load_get\n" );
    }

    return ( lru_load < .000001 ) ? 0 : NC_LRU_DONT_DELETE;
}

static void _lru_empty_trash  ( void* arg )
{
    netcap_trie_line_t* line = arg;

    debug( NC_SHIELD_DEBUG_HIGH, "EMPTY TRASH %#010x\n", line );
    
    if ( new_netcap_trie_line_raze( &_shield.trie, line) < 0 ) {
        errlog( ERR_CRITICAL, "new_netcap_trie_line_raze\n" );
    }
}

static int  _lru_remove       ( void* arg )
{
    netcap_trie_item_t* item = (netcap_trie_item_t*)arg;
    nc_shield_reputation_t* reputation = NULL;
    netcap_trie_line_t* line = NULL;

    if ( item == NULL ) return errlogargs();
    if (( reputation = item->data ) == NULL ) return errlogargs();
    
    if (( line = malloc( sizeof( netcap_trie_line_t ))) == NULL ) return errlogmalloc();

    /* Just remove the item from the trie, Don't use the mutex because it is
     * already locked by the LRU. */
    if ( new_netcap_trie_remove( &_shield.trie, &reputation->ip, NULL, line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_remove\n" );
    }
    
    if ( netcap_sched_event( _lru_empty_trash, line, _LRU_TRASH_DELAY ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_sched_event\n" );
    }
    
    return 0;
}


static int _add_request ( nc_shield_reputation_t *rep, int count, double divider, void* arg, 
                          int update_load )
{
    if ( update_load == _NC_UPDATE_LOAD ) {
        netcap_load_val_t val;

        val = ( rep->score < 105 ) ? 1.0 : 5.0;
        
        return netcap_load_update( &rep->request_load, 1, ((netcap_load_val_t)val));
    }
    return 0;
}

static int _add_session ( nc_shield_reputation_t *rep, int count, double divider, void* arg, 
                          int update_load )
{
    /* Increment the number of sessions */
    rep->active_sessions = ( rep->active_sessions < 1 ) ? 1 : ( rep->active_sessions + 1 );

    if ( update_load == _NC_UPDATE_LOAD ) {
        return netcap_load_update( &rep->session_load, 1, ((netcap_load_val_t)1.0));
    }
    return 0;
}

static int _end_session ( nc_shield_reputation_t *rep, int count, double divider, void* arg, 
                          int update_load )
{
    /* Decrement the number of sessions */
    if ( rep->active_sessions < 1 ) rep->active_sessions = 0;
    else rep->active_sessions--;
    return 0;
}

static int _add_srv_conn ( nc_shield_reputation_t *rep, int count, double divider, void* arg, 
                           int update_load )
{
    if ( update_load == _NC_UPDATE_LOAD ) {
        /* Just in case */
        count = ( count <= 0 ) ? 1 : count;
        return netcap_load_update( &rep->srv_conn_load, 1, ( 1.0 / ( divider * count )));
    }
    return 0;
}

static int _add_srv_fail ( nc_shield_reputation_t *rep, int count, double divider, void* arg, 
                           int update_load )
{
    if ( update_load == _NC_UPDATE_LOAD ) {
        /* Just in case */
        count = ( count <= 0 ) ? 1 : count;
        return netcap_load_update( &rep->srv_fail_load, 1, ( 1.0 / ( divider * count )));
    }
    return 0;
}

static int _add_evil ( nc_shield_reputation_t *rep, int count, double divider, void* arg, int update_load )
                        
{
    int evil_count = (int)arg;
    /* Just in case */
    count = ( count <= 0 ) ? 1 : count;

    netcap_load_val_t evil_val = ((netcap_load_val_t)evil_count + 0.0 ) / ( divider * count );
    if ( update_load == _NC_UPDATE_LOAD ) return netcap_load_update( &rep->evil_load, evil_count, evil_val );
    
    return 0;
}

static int _add_chunk ( nc_shield_reputation_t *rep, int count, double divider, void* arg, int update_load )
{
    _chk_t* chk;
    netcap_load_t* load = NULL;
    
    if ( (chk = (_chk_t*)arg) == NULL ) return errlogargs();

    /* Just in case */
    count = ( count <= 0 ) ? 1 : count;

    switch ( chk->protocol ) {
    case IPPROTO_TCP:  load = &rep->tcp_chk_load;  break;
    case IPPROTO_UDP:  load = &rep->udp_chk_load;  break;
    case IPPROTO_ICMP: load = &rep->icmp_chk_load; break;
    default: return errlogargs();
    }
    
    if ( chk->if_rx == 1 ) {
        if ( update_load == _NC_UPDATE_LOAD ) {
            netcap_load_update( load, 1, ( 1.0 / ( divider * count )));
            netcap_load_update( &rep->byte_load, chk->size, 
                                ((netcap_load_val_t)chk->size) / ( divider * count ));
        }
    } else {
        return errlog( ERR_CRITICAL, "Invalid chunk Description\n" );
    }
    
    return 0;
}

static int  _get_response        ( netcap_shield_response_t* response, nc_shield_reputation_t** reputation,
                                   int protocol, struct in_addr* address )
{
    netcap_trie_line_t line;
    netcap_trie_element_t element;
    
    if ( reputation == NULL || address == NULL ) return errlogargs();

    if ( new_netcap_trie_get( &_shield.trie, address, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_get\n" );
    }

    element = _get_end_of_line( &line );
    
    if ( element.base == NULL ) return errlog( ERR_CRITICAL, "_get_end_of_line\n" );

    if ( _update_score( element ) < 0 ) return errlog( ERR_CRITICAL, "_update_score\n" );
    
    if (( *reputation = element.base->data ) == NULL ) {
        return errlog( ERR_CRITICAL, "_get_end_of_line\n" );
    }

    if ((*reputation)->pass_all == _PASS_ALL_FLAG ) {
        response->if_print = 0;
        response->ans = NC_SHIELD_YES;
        return 0;
    }

    nc_shield_score_t score = (*reputation)->score;

    /* If the reputation is too high, and the load is low enough, print out debugging information. */
    if ( score > _shield.cfg.rep_threshold ) _debug_reputation_threshold( &line );

    response->ans = _put_in_fence( _shield.fence, score, *reputation, protocol );

    response->if_print = 0;
    
    if ( _check_if_print( address, *reputation, response ) < 0 ) {
        errlog( ERR_CRITICAL, "_check_if_print\n" );
    }
    
    return 0;
}

static int  _get_response_closed ( netcap_shield_response_t* response, nc_shield_reputation_t** reputation,
                                   int protocol, struct in_addr* ip )
{
    /* The root reputation is always used when the shield is closed */
    if ( reputation == NULL ) return errlogargs();

    *reputation = _shield.root;
    
    if ( _is_on_ignore_list( ip ) == 1 ) {
        response->ans = NC_SHIELD_YES;
        response->if_print = 0;
        return 0;
    }

    response->ans = _put_in_fence( &_shield.cfg.fence.closed, _shield.cfg.fence.closed.limited.post, 
                                   _shield.root, protocol );

    response->if_print = 0;

    /* XXX Printing to the 0 ip address because the root reputation was used */
    if ( _check_if_print( 0, _shield.root, response ) < 0 ) errlog( ERR_CRITICAL, "_check_if_print\n" );

    return 0;
}

static int _update_score( netcap_trie_element_t element )
{
    nc_shield_reputation_t* reputation = element.base->data;
    
    if ( reputation == NULL ) return errlogargs();
    
    /* Locked during this function */
    if ( nc_shield_reputation_update( reputation ) < 0 ) {
        return errlog( ERR_CRITICAL, "nc_shield_reputation_update\n" );
    }
    
    /* Not locked during this function */
    if ( _reputation_eval( element.item ) < 0 ) return errlog( ERR_CRITICAL, "_reputation_eval\n" );

    return 0;
}

/* XXX May want to have a reputation update load and use that to compute
 * how often to compute the load, right now performance is fine, and
 * this is not a problem */
static int  _reputation_update   ( nc_shield_reputation_t* reputation )
{
    if (( netcap_load_update( &reputation->evil_load,     0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->request_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->session_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->srv_conn_load, 0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->srv_fail_load, 0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->tcp_chk_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->udp_chk_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->icmp_chk_load, 0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->byte_load,     0, 0 ) < 0) ||
        ( netcap_load_update( &reputation->print_load,    0, 0 ) < 0)) {
        return errlog(ERR_CRITICAL,"netcap_load_update\n");
    }
    
    return 0;
}

static int  _reputation_init     ( netcap_trie_item_t* item, struct in_addr* ip )
{
    int ret = 0;
    nc_shield_reputation_t* reputation;
    nc_shield_fence_t* fence = NULL;

    if ( item == NULL || ( reputation = netcap_trie_item_data( item )) == NULL ) return errlogargs();

    reputation = item->data;

    /* Create the mutex */
    if ( pthread_mutex_init ( &reputation->mutex, NULL ) < 0 ) {
        return perrlog("pthread_mutex_init");
    }
    
    /* Initalize each of the loads */
    /* Right now these inherit the load and the last update time of the parent */
    if (( netcap_load_init( &reputation->evil_load,     _LOAD_INTERVAL_EVIL,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->request_load,  _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->session_load,  _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->srv_conn_load, _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->srv_fail_load, _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->tcp_chk_load,  _LOAD_INTERVAL_CHK,   !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->udp_chk_load,  _LOAD_INTERVAL_CHK,   !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->icmp_chk_load, _LOAD_INTERVAL_CHK,   !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->byte_load,     _LOAD_INTERVAL_BYTE,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->print_load,    _LOAD_INTERVAL_PRINT, !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &reputation->lru_load,      _LOAD_INTERVAL_LRU,   !NC_LOAD_INIT_TIME ) < 0)) {
        ret = errlog( ERR_CRITICAL, "netap_load_init\n" );
    }
    
    /* Parent is unititialized, set the reputation to zero */
    if ( reputation->score < 0 ) reputation->score = 0;

    reputation->active_sessions   = 0;

    /* Reset all of the counters */
    bzero( &reputation->counters, sizeof( reputation->counters ));

    if (( fence = _get_fence( _shield.mode )) == NULL ) {
        ret = errlog( ERR_CRITICAL, "_get_fence\n" );
    } else {
        /* Apply inheritance to the reputation */
        reputation->evil_load.load     = ( fence->inheritance ) * reputation->evil_load.load;
        reputation->request_load.load  = ( fence->inheritance ) * reputation->request_load.load;
        reputation->session_load.load  = ( fence->inheritance ) * reputation->session_load.load;
        reputation->srv_conn_load.load = ( fence->inheritance ) * reputation->srv_conn_load.load;
        reputation->srv_fail_load.load = ( fence->inheritance ) * reputation->srv_fail_load.load;
        reputation->tcp_chk_load.load  = ( fence->inheritance ) * reputation->tcp_chk_load.load;
        reputation->udp_chk_load.load  = ( fence->inheritance ) * reputation->udp_chk_load.load;
        reputation->icmp_chk_load.load = ( fence->inheritance ) * reputation->icmp_chk_load.load;
        reputation->byte_load.load     = ( fence->inheritance ) * reputation->byte_load.load;
    }

    reputation->ip.s_addr = ip->s_addr;

#ifdef _TRIE_DEBUG_PRINT
    if (( reputation->self = list_add_tail ( &_shield.ip_list, item )) == NULL ) {
        return errlog(ERR_CRITICAL,"list_add_tail\n");
    }    
#endif

    /* If this is a terminal node, throw it on the LRU */
    /* Throw this node onto the LRU, no need to lock, mutex is already locked */
    if (( item->depth == NC_TRIE_DEPTH_TOTAL ) &&
        ( netcap_lru_add( &_shield.lru, &reputation->lru_node, item, NULL ) < 0 )) {
        return errlog( ERR_CRITICAL, "netcap_lru_add\n" );        
    }

    return ret;
}

static nc_shield_score_t  _reputation_eval     ( netcap_trie_item_t* item )
{
    nc_shield_score_t score;
    nc_shield_reputation_t* rep;
    
    if ( item == NULL ) return (nc_shield_score_t)errlogargs();

    if (( rep = netcap_trie_item_data( item )) == NULL ) {
        return (nc_shield_score_t)errlog ( ERR_CRITICAL, "netcap_trie_item_data\n" );
    }

    score  = rep->request_load.load * _shield.cfg.mult.request_load;
    score += rep->session_load.load * _shield.cfg.mult.session_load;
    score += rep->tcp_chk_load.load * _shield.cfg.mult.tcp_chk_load;
    score += rep->udp_chk_load.load * _shield.cfg.mult.udp_chk_load;
    score += rep->evil_load.load    * _shield.cfg.mult.evil_load;

    /* ICMP is not included in the reputation of the user, it is special cased in the response */

    /* Use the number of active sessions * multiplier * (depth/total_depth)^2, this way single child
     * nodes cannot refuse connections to new nodes */
    score += (rep->active_sessions+0.0) * _shield.cfg.mult.active_sess * 
        (( item->depth * item->depth ) / (( 0.0 + NC_TRIE_DEPTH_TOTAL ) * ( 0.0 + NC_TRIE_DEPTH_TOTAL )));
    
    /* XXX Use whether or not their depth is terminal (this is a new node) */

    /* XXX Use the server connection/failure rate */

    return ( rep->score = score );
}

static void _reputation_destroy  ( netcap_trie_item_t* item )
{
    nc_shield_reputation_t* rep;
    
    if ( item == NULL || (rep = netcap_trie_item_data(item)) == NULL ) return (void)errlogargs();

    rep = item->data;

    /* Destroy the mutex */
    if ( pthread_mutex_destroy ( &rep->mutex ) < 0 ) perrlog( "pthread_mutex_destroy" );

#ifdef _TRIE_DEBUG_PRINT
    /* Set the item on the list to NULL */
    rep->self->val = NULL;
#endif // _TRIE_DEBUG_PRINT
}

static netcap_shield_ans_t _put_in_fence  ( nc_shield_fence_t* fence, nc_shield_score_t score, 
                                            nc_shield_reputation_t*  rep, int protocol )
{
    nc_shield_score_t prob;
    netcap_shield_ans_t ans = NC_SHIELD_DROP;

    prob = ( rand() + 0.0 ) / RAND_MAX;
    
    if ( score > fence->error.post ) {
        ans = ( prob < fence->error.prob )   ? NC_SHIELD_RESET   : NC_SHIELD_DROP;
    } else if ( score > fence->closed.post ) {
        ans = ( prob < fence->closed.prob )  ? NC_SHIELD_DROP    : NC_SHIELD_LIMITED;
    } else if ( score > fence->limited.post ) {
        ans = ( prob < fence->limited.prob ) ? NC_SHIELD_LIMITED : NC_SHIELD_YES;
    } else {
        ans = NC_SHIELD_YES;
    }

    /* ICMP is always rate limited and handled slightly differently */
    if ( ans == NC_SHIELD_YES && protocol == IPPROTO_ICMP ) {
        if ( rep->icmp_chk_load.load > _shield.cfg.mult.icmp_chk_load ) {
            prob = ( rand() + 0.0 ) / RAND_MAX;
            if ( prob < ( _shield.cfg.mult.icmp_chk_load / rep->icmp_chk_load.load )) {
                ans = NC_SHIELD_YES;
            } else {
                ans = NC_SHIELD_DROP;
            }
        } else {
            ans = NC_SHIELD_YES;
        }
    }
    
    return ans;
}

static void            _null_event_hook     ( netcap_shield_event_data_t* data )
{
    /* Null hook */
}

static int _reset_dividers( void )
{
    int c;

    list_node_t* node;

    /* Zero out the ignore array */
    bzero( _shield.ignore_array, sizeof( _shield.ignore_array ));
    
    node = list_head( &_shield.lru.permanent_list );

    /* Just to guarantee that it eventually finishes */
    for ( c = BLESS_COUNT_MAX ; c-- > 0 && ( node != NULL ) ;  node = list_node_next( node )) {
        netcap_lru_node_t* lru_node = list_node_val( node );
        if ( lru_node == NULL ) {
            errlog( ERR_CRITICAL, "NULL node at %d, continuing\n", c );
            continue;
        }
        
        netcap_trie_item_t* trie_item = lru_node->data;
        if ( trie_item == NULL ) {
            errlog( ERR_CRITICAL, "NULL trie item at %d, continuing\n", c );
            continue;
        }

        nc_shield_reputation_t* reputation = trie_item->data;
        if ( reputation == NULL ) {
            errlog( ERR_CRITICAL, "NULL reputation at %d, continuing\n", c );
            continue;            
        }
        reputation->divider = _DEFAULT_DIVIDER;
    }
    
    return 0;
}

static void _report_intf_event( netcap_intf_t intf, netcap_shield_event_data_t* event, 
                                nc_shield_rejection_counter_t* counter )
{
    int tmp;

    if ( counter->limited > 0 || counter->rejected > 0 || counter->dropped > 0 ) {
        event->data.rejection.client_intf = intf;
        tmp = counter->limited;
        event->data.rejection.limited     = ( tmp > 0 ) ? tmp : 0;

        tmp = counter->dropped;
        event->data.rejection.dropped     = ( tmp > 0 ) ? tmp : 0;

        tmp = counter->rejected;
        event->data.rejection.rejected    = ( tmp > 0 ) ? tmp : 0;
        
        _shield.event_hook( event );
    }

    bzero( counter, sizeof( nc_shield_rejection_counter_t ));
}

static netcap_trie_element_t _get_end_of_line ( netcap_trie_line_t* line )
{
    /* in case the value is unitialized */
    _shield.null_element.base = NULL;

    if ( line->count == 0 ) {
        errlog( ERR_CRITICAL, "Line has zero nodes\n" );
        return _shield.null_element;
    }
    
    if ( line->count > NC_TRIE_LINE_COUNT_MAX ) {
        errlog( ERR_CRITICAL, "Line w/ invalid count %d\n", line->count );
        return _shield.null_element;
    }

    return (( line->is_bottom_up ) ? line->d[0] : line->d[line->count-1]);
}

static void _debug_reputation_threshold       ( netcap_trie_line_t* line )
{
    struct timeval current_time;

    void _critical_section()
    {
        /* Check again after locking the mutex */
        if ( gettimeofday( &current_time, NULL ) < 0 ) {
            perrlog("gettimeofday");
            return;
        }

        /* If the enough time hasn't elapsed yet, no need to check after locking the mutex */
        if ( utime_usec_diff( &_shield.threshold_last_update, &current_time ) < _shield.cfg.print_delay ) {
            return;
        }

        /* Copy in the current time */
        memcpy( &_shield.threshold_last_update, &current_time, sizeof( _shield.threshold_last_update ) );
        
        int c;
        for ( c = 0 ; c < line->count ; c++ ) {
            netcap_trie_element_t element = line->d[c];
            netcap_trie_item_t* item = element.item;
            nc_shield_reputation_t* reputation;
            int children;
            
            if ( element.base == NULL ) {
                errlog( ERR_CRITICAL, "Line with null element at index: %d, %d\n", c, line->is_bottom_up );
                continue;
            }
            
            reputation = (nc_shield_reputation_t*)netcap_trie_item_data( item );
            
            if ( reputation == NULL ) {
                errlog( ERR_CRITICAL, "Item with null reputation at index: %d, %d\n", c, line->is_bottom_up );
                continue;
            }
            
            if (( children = netcap_trie_element_children( element )) < 0 ) {
                errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
            }
            
            children = ( children <= 0 ) ? 1 : children;
            
            /* This prints out all of hte information that is used to
             * calculate the reputation, at each level */
            debug( 0, "Reputation[%08X,%d,%2d]: r %12lg a %08d r %12lg s %12lg t %12lg u %012lg e %12lg\n",
                   ntohl( reputation->ip.s_addr ), item->depth, children, reputation->score,
                   reputation->active_sessions,   reputation->request_load.load,
                   reputation->session_load.load, reputation->tcp_chk_load.load,
                   reputation->udp_chk_load.load, reputation->evil_load.load );
        }
    }

    if ( line == NULL ) {
        errlog( ERR_CRITICAL, "NULL line" );
        return;
    }
    
    if ( line->count == 0 ) {
        errlog( ERR_CRITICAL, "Line has zero nodes\n" );
        return;
    }

    if ( line->count > NC_TRIE_LINE_COUNT_MAX ) {
        errlog( ERR_CRITICAL, "Line w/ invalid count %d\n", line->count );
        return;
    }

    /* Check if the output should be printed. */
    if ( gettimeofday( &current_time, NULL ) < 0 ) {
        perrlog("gettimeofday");
        return;
    }
    
    /* If the enough time hasn't elapsed yet, no need to check after locking the mutex */
    if ( utime_usec_diff( &_shield.threshold_last_update, &current_time ) < _shield.cfg.print_delay ) {
        return;
    }

    /* There is only global load that controls all threads */
    if ( pthread_mutex_lock( &_shield.threshold_mutex ) < 0 ) {
        perrlog( "pthread_mutex_lock" );
        return;
    }

    _critical_section();

    if ( pthread_mutex_unlock( &_shield.threshold_mutex ) < 0 ) {
        perrlog( "pthread_mutex_unlock" );
    }
}

/** This function applies func to a line of nodes.  If the node
 * doesn't exist, then it is not created */
static int  _apply_close( struct in_addr* ip, _apply_func_t* func )
{
    netcap_trie_line_t line;
    netcap_trie_element_t element;
    
    if ( ip == NULL ) return errlogargs();

    /* If the shield is not enabled return */
    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;
    
    if ( new_netcap_trie_get( &_shield.trie, ip, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_get\n" );
    }
    
    element = _get_end_of_line( &line );
    if ( element.base == NULL || element.base->data == NULL ) {
        return errlog( ERR_CRITICAL, "_get_end_of_line\n" );
    }
    
    func->divider = ((nc_shield_reputation_t*)element.base->data)->divider;
    if ( func->divider < NC_SHIELD_DIVIDER_MIN  || func->divider > NC_SHIELD_DIVIDER_MAX ) {
        errlog( ERR_WARNING, "Invalid divider[%g], using %g\n", func->divider, _DEFAULT_DIVIDER );
        func->divider = _DEFAULT_DIVIDER;
        ((nc_shield_reputation_t*)element.base->data)->divider = _DEFAULT_DIVIDER;
    }

    int c;
    for ( c = 0 ; c < line.count && c <= NC_TRIE_DEPTH_TOTAL ; c++ ) {
        if ( _apply_func( line.d[c], func, ip ) < 0 ) return errlog( ERR_CRITICAL, "_apply_func\n" );
    }

    return 0;
}

/** This function applies func to a line of nodes.  If the node
 * doesn't exist, then one is created. */
static int  _apply      ( struct in_addr* ip, _apply_func_t* func )
{
    netcap_trie_line_t line;
    netcap_trie_element_t element;
    int c;

    if ( ip == NULL ) return errlogargs();

    /* If the shield is not enabled return */
    if ( _shield.enabled != NETCAP_SHIELD_ENABLE ) return 0;

    if ( new_netcap_trie_insert_and_get( &_shield.trie, ip, &_shield.mutex, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_insert_and_get\n" );
    }

    element = _get_end_of_line( &line );
    if ( element.base == NULL || element.base->data == NULL ) {
        return errlog( ERR_CRITICAL, "_get_end_of_line\n" );
    }
    
    func->divider = ((nc_shield_reputation_t*)element.base->data)->divider;
    if (( func->divider < NC_SHIELD_DIVIDER_MIN ) || func->divider > NC_SHIELD_DIVIDER_MAX ) {
        errlog( ERR_WARNING, "Invalid divider[%lg], using %g\n", func->divider, _DEFAULT_DIVIDER );
        func->divider = _DEFAULT_DIVIDER;
        ((nc_shield_reputation_t*)element.base->data)->divider = _DEFAULT_DIVIDER;
    }

    for ( c = 0 ; c < line.count && c <= NC_TRIE_DEPTH_TOTAL ; c++ ) {
        if ( _apply_func( line.d[c], func, ip ) < 0 ) return errlog( ERR_CRITICAL, "_apply_func\n" );
    }
    
    return 0;
}

static int _check_lru( int depth, nc_shield_reputation_t* reputation )
{
    netcap_load_val_t lru_load;

    /* Only nodes at the terminal end go on the lru */
    if ( depth < NC_TRIE_DEPTH_TOTAL ) return 0;

    if (( lru_load = netcap_load_get( &reputation->lru_load )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_load_get\n" );
    }

    /* Move the node to front of the LRU */
    if ( lru_load < _shield.cfg.lru.ip_rate ) {
        debug( NC_SHIELD_DEBUG_HIGH, "moving to front of lru %lg\n", lru_load );
            
        if ( netcap_lru_move_front( &_shield.lru, &reputation->lru_node, &_shield.mutex ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_lru_move_front\n" );
        }

        if ( netcap_load_update( &reputation->lru_load, 1, 1 ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_load_update\n" );
        }
    }

    return 0;
}

static int _clean_lru_and_trie( void )
{
    int ret;

    int _critical_section( void ) {
        int length;
        int c;

        if (( length = list_length ( &_shield.lru.lru_list )) < 0 ) {
            return errlog( ERR_CRITICAL, "list_length\n" );
        }
        
        debug( NC_SHIELD_DEBUG_LOW, "SHIELD: CLEAN - length %d items\n", length );
        
        for ( c = length ; c-- > 0 ; ) {
            netcap_lru_node_t* node;
            netcap_trie_line_t line;
            nc_shield_reputation_t* reputation = NULL;
            netcap_trie_item_t* item;

            if (( node = list_tail_val( &_shield.lru.lru_list )) == NULL ) {
                errlog( ERR_CRITICAL, "list_tail_val\n" );
                continue;
            }
            
            if (( item = node->data ) == NULL ) {
                errlog( ERR_CRITICAL, "NULL item data\n" );
                continue;
            }
            
            if (( reputation = item->data ) == NULL ) {
                errlog( ERR_CRITICAL, "NULL reputation\n" );
                continue;
            }
            
            if ( new_netcap_trie_remove( &_shield.trie, &reputation->ip, NULL, &line ) < 0 ) {
                errlog( ERR_CRITICAL, "new_netcap_trie_remove\n" );
                continue;
            }
            
            if ( new_netcap_trie_line_destroy( &_shield.trie, &line) < 0 ) {
                errlog( ERR_CRITICAL, "new_netcap_trie_line_raze\n" );
            }

            if ( list_remove( &_shield.lru.lru_list, node->list_node ) < 0 ) perrlog( "list_remove\n" );
        }

        return 0;
    }
    
    /* Ignore locking and unlocking errors */
    if ( pthread_mutex_lock( &_shield.mutex ) < 0 ) perrlog( "pthread_mutex_lock" );
    
    ret = _critical_section();
    
    if ( pthread_mutex_unlock( &_shield.mutex ) < 0 ) perrlog( "pthread_mutex_unlock" );

    return ret;
}

static int _is_on_ignore_list( struct in_addr* address )
{
    int c;

    if ( address == NULL ) return 0;

    for ( c = 0 ; c < IGNORE_ARRAY_SIZE ; c++ ) {
        if ( _shield.ignore_array[c] == 0 ) return 0;
        
        if ( _shield.ignore_array[c] == address->s_addr ) return 1;
    }

    return 0;
}

#ifdef _TRIE_DEBUG_PRINT
static int  _status             ( int conn, struct sockaddr_in *dst_addr )
{
    list_node_t* step;
    list_node_t* next;
    nc_shield_reputation_t* rep;
    netcap_trie_item_t* item;
    int length;
    char buf[1000];
    int msg_len;
    time_t now;
    int children;
    
    length = list_length( &_shield.ip_list );
    
    if ( length == 0 ) return 0;
    
    if (( now = time ( NULL )) < 0 ) return perrlog ( "time" );
    
    msg_len = snprintf( buf, sizeof( buf ), "<shield mode='%d' time='%d'>\n", _shield.mode, (int)now );

    if ( sendto( conn, buf, msg_len, 0, dst_addr, sizeof( struct sockaddr_in )) < 0 ) {
        return perrlog ( "sendto" );
    }
    
    for ( step = list_head( &_shield.ip_list ); step && length-- >= 0;  step = next ) {
        next = list_node_next ( step );
        /* This means that the item has been removed */
        if ((item = (netcap_trie_item_t*)list_node_val( step )) == NULL ) {
            list_remove ( &_shield.ip_list, step );
            continue;
        }
        
        if (( rep  = (nc_shield_reputation_t*)netcap_trie_item_data ( item )) == NULL ) continue;

        if (( children = netcap_trie_element_children((netcap_trie_element_t)item )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
            continue;
        }
        
        children = ( children == 0 ) ? 1 : children;

        /* Update the reputation */
        _reputation_update ( rep );
        _reputation_eval( item );
        
        msg_len =  snprintf( buf, sizeof(buf), "<rep ip='%#08x' children='%d' depth='%d' active='%d' "
                             "val='%lg' evil-load='%g' evil-total='%d' request-load='%g' request-total='%d' "
                             "session-load='%g' session-total='%d' srv-conn-load='%g' srv-conn-total='%d' "
                             "srv-fail-load='%g' srv-fail-total='%d' "
                             "tcp-chk-load='%g' tcp-chk-total='%d' udp-chk-load='%g' udp-chk-total='%d' "
                             "icmp-chk-load='%g' icmp-chk-total='%d' byte-load='%g' byte-total='%d' "
                             "rejected='%d' limited='%d' divider='%g'/>\n",
                             ntohl(rep->ip.s_addr), children, item->depth, rep->active_sessions, rep->score,
                             rep->evil_load.load, rep->evil_load.total,
                             rep->request_load.load, rep->request_load.total,
                             rep->session_load.load, rep->session_load.total,
                             rep->srv_conn_load.load, rep->srv_conn_load.total,
                             rep->srv_fail_load.load, rep->srv_fail_load.total,
                             rep->tcp_chk_load.load, rep->tcp_chk_load.total,
                             rep->udp_chk_load.load, rep->udp_chk_load.total,
                             rep->icmp_chk_load.load, rep->icmp_chk_load.total,
                             rep->byte_load.load, rep->byte_load.total,
                             rep->counters[0].rejected, rep->counters[0].limited, rep->divider );
        if ( sendto( conn, buf, msg_len, 0, dst_addr, sizeof( struct sockaddr_in )) < 0 ) {
            return perrlog ( "sendto" );
        }
    }

    msg_len = nc_shield_cfg_get ( &_shield.cfg, buf, sizeof( buf ));

    if ( sendto ( conn, buf, msg_len, 0, dst_addr, sizeof( struct sockaddr_in )) < 0 ) {
        return perrlog ( "sendto" );
    }
    
    msg_len = snprintf( buf, sizeof(buf), "</shield>\n\026");
    if ( sendto( conn, buf, msg_len, 0, dst_addr, sizeof( struct sockaddr_in )) < 0 ) {
        return perrlog ( "sendto" );
    }
    
    return 0;

}
#endif

