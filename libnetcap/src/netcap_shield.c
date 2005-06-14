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

#include "libnetcap.h"
#include "netcap_init.h"
#include "netcap_trie.h"
#include "netcap_trie_support.h"
#include "netcap_shield.h"
#include "netcap_shield_cfg.h"
#include "netcap_load.h"
#include "netcap_sched.h"

#ifdef DEBUG_ON
#define _TRIE_DEBUG_PRINT
#endif

/* 1 second in u-seconds */
#define _LOAD_INTERVAL_SEC     1000000

/* 1 ms in u-seconds */
#define _LOAD_INTERVAL_MS      1000

#define _LOAD_INTERVAL_EVIL    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_SESS    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_CHK     (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_BYTE    (  5 * _LOAD_INTERVAL_SEC)
#define _LOAD_INTERVAL_PRINT   (  5 * _LOAD_INTERVAL_SEC )
#define _LOAD_INTERVAL_LRU     (  5 * _LOAD_INTERVAL_SEC )

#define _SHIELD_FILL_TRASH_INTERVAL ( 5 * _LOAD_INTERVAL_SEC )

typedef struct reputation {
    pthread_mutex_t mutex;

#ifdef _TRIE_DEBUG_PRINT
    in_addr_t       ip;
    /* Pointer to the value on the ip_list, must have the dbg_mutex to read or set this value*/
    list_node_t*    self;
#endif
    int             active_sessions;
    int             limited_sessions;    /* Total number of sessions given limited access */
    int             rejected_sessions;   /* Total number of sessions that are rejected */
    
    nc_shield_rep_t rep;           /* The current reputation for the IP */
    netcap_load_t   evil_load;     /* Evil events per second */
    netcap_load_t   request_load;  /* Number of request this IP makes */
    netcap_load_t   session_load;  /* Active number of sessions used */
    netcap_load_t   srv_conn_load; /* Server connections load */
    netcap_load_t   srv_fail_load; /* Load of failed connection attempts */
    netcap_load_t   tcp_chk_load;  /* TCP chunk load */
    netcap_load_t   udp_chk_load;  /* UDP chunk load */
    netcap_load_t   byte_load;     /* Byte load */
    netcap_load_t   print_load;    /* Printing rate, limited to x per second */
    netcap_load_t   lru_load;      /* LRU rate, limited to x per second */
} reputation_t;
    
static struct {
    reputation_t*        root;
    /* How tight is the list right now */
    netcap_shield_mode_t mode;
    int                  enabled;
    
    nc_shield_cfg_t cfg;

    /* The trie holding all of the ips */
    netcap_trie_t trie;

#ifdef _TRIE_DEBUG_PRINT
    list_t ip_list;
    /* Cannot run the dump function and the trash at the same time */
    pthread_mutex_t dbg_mutex;
#endif

} _shield = {
    .root NULL,
    .mode NC_SHIELD_MODE_RELAXED,
    .enabled 0,
#ifdef _TRIE_DEBUG_PRINT
    .dbg_mutex PTHREAD_MUTEX_INITIALIZER,
#endif
};

typedef struct _chk {
    u_short  size;               /* Size of the chunk in bytes */
    u_char   if_rx;              /* 1 for rx, 0 for tx XXX Presently unused */
    u_char   protocol;
} _chk_t;

typedef struct _apply_func {
    int (*func) ( reputation_t* rep, int count, void* arg);
    void *arg;
} _apply_func_t;

static void  _trash_fill         ( void* arg );

static int   _apply_func         ( netcap_trie_item_t* item, void* arg, in_addr_t ip );

/** Check if a node is deletable from the LRU */
static int  _lru_check           ( netcap_trie_item_t* item );

/** These are all helper functions that are called by _apply_func */
static int  _add_evil            ( reputation_t* rep, int count, void* arg );
static int  _add_request         ( reputation_t* rep, int count, void* arg );
static int  _add_session         ( reputation_t* rep, int count, void* arg );
static int  _end_session         ( reputation_t* rep, int count, void* arg );
static int  _add_chk             ( reputation_t* rep, int count, void* arg );
static int  _add_srv_conn        ( reputation_t* rep, int count, void* arg );
static int  _add_srv_fail        ( reputation_t* rep, int count, void* arg );

static int             _reputation_update   ( reputation_t* rep, int count, void* arg );
static nc_shield_rep_t _reputation_eval     ( netcap_trie_item_t* item );
static int             _reputation_init     ( netcap_trie_item_t* item, in_addr_t ip );
static void            _reputation_destroy  ( netcap_trie_item_t* item );
static netcap_shield_ans_t _put_in_fence    ( nc_shield_fence_t* fence, nc_shield_rep_t rep_val );

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

#ifdef _TRIE_DEBUG_PRINT
static int   _status             ( int conn, struct sockaddr_in *dst_addr );
#endif

static netcap_shield_mode_t _mode_eval ( void );

static netcap_shield_response_t* _tls_get( void );

int netcap_shield_init    ( void )
{
    int flags;
    reputation_t rep;

    _shield.enabled = NETCAP_SHIELD_ENABLE;

#ifdef _TRIE_DEBUG_PRINT
    if ( list_init( &_shield.ip_list, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"list_init\n");
    }
#endif

    /* Load the default shield configuration */
    if ( nc_shield_cfg_def ( &_shield.cfg ) < 0 ) return errlog ( ERR_CRITICAL, "nc_shield_cfg_def\n" );

    flags = NC_TRIE_FREE | NC_TRIE_INHERIT | NC_TRIE_COPY | NC_TRIE_LRU;
    
    netcap_load_init( &rep.evil_load,     _LOAD_INTERVAL_EVIL,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.request_load,  _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.session_load,  _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.srv_conn_load, _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.srv_fail_load, _LOAD_INTERVAL_SESS,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.tcp_chk_load,  _LOAD_INTERVAL_CHK,   NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.udp_chk_load,  _LOAD_INTERVAL_CHK,   NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.byte_load,     _LOAD_INTERVAL_BYTE,  NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.print_load,    _LOAD_INTERVAL_PRINT, NC_LOAD_INIT_TIME );
    netcap_load_init( &rep.lru_load,      _LOAD_INTERVAL_LRU,   NC_LOAD_INIT_TIME );

    /* Initialize the number of active sessions */
    rep.active_sessions = 0;
    
    /* Initialize the reputation value */
    rep.rep = 0;

    /* The init function will initialize the mutex, the functions above, *
     * guarantee that the last update times all get set */
    /* XXX Update the default keep size and delete size to something useful */
    if ( netcap_trie_init( &_shield.trie, flags, &rep, sizeof(rep), _reputation_init, _reputation_destroy, 
                           NULL, _shield.cfg.lru.high_water, _shield.cfg.lru.low_water ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_root_init\n");
    }

    /* Set the check function */
    _shield.trie.check = _lru_check;

    if (( _shield.root = netcap_trie_data ( &_shield.trie )) == NULL ) {
        return errlog(ERR_CRITICAL,"Shield is unitialized\n");
    }

    /* Start a thread that fills the trash every x second */
    if ( netcap_sched_event ( _trash_fill, NULL, _SHIELD_FILL_TRASH_INTERVAL << 1 ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_sched_event\n" );
    }
    
    return 0;
}

int netcap_shield_cleanup ( void )
{
    if ( !_shield.enabled ) return 0;

    _shield.enabled = 0;

    netcap_trie_destroy( &_shield.trie );

#ifdef _TRIE_DEBUG_PRINT
    list_destroy  ( &_shield.ip_list );
#endif
    
    return 0;
}

/* Indicate if an IP should allowed in */
netcap_shield_response_t* netcap_shield_rep_check        ( in_addr_t ip )
{ 
    netcap_trie_item_t* item = NULL;
    _apply_func_t func = { _reputation_update, NULL };
    netcap_shield_mode_t mode;    
    nc_shield_rep_t rep_val;
    netcap_shield_ans_t ans = NC_SHIELD_YES;
    reputation_t*  rep;
    nc_shield_fence_t* fence = NULL;
    netcap_shield_response_t* response = NULL;

    if (( response = _tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "_tls_get\n" );
    
    /* If the shield is not enabled return true */
    if ( !_shield.enabled ) {
        response->tcp      = NC_SHIELD_YES;
        response->udp      = NC_SHIELD_YES;
        response->icmp     = NC_SHIELD_YES;
        response->if_print = 1;
        return response;
    }

    do {
        rep = _shield.root;

        /* Update the mode, and the current overall shield mode */
        if (( mode = _shield.mode = _mode_eval()) < 0 ) {
            return errlog_null ( ERR_CRITICAL, "_mode_eval\n" );
        }
        
        /* If things are really bad, do not let anything in */
        if ( mode == NC_SHIELD_MODE_CLOSED ) { 
            rep_val= _shield.cfg.fence.closed.limited.post;
            fence = &_shield.cfg.fence.closed;
            item = NULL;
            break;
        }
        
        /* Update the reputation */
        if (( item = netcap_trie_apply_close ( &_shield.trie, ip, _apply_func, &func )) == NULL ) {
            return errlog_null( ERR_CRITICAL, "netcap_trie_apply\n");
        }

        if (( rep = netcap_trie_item_data( item )) == NULL ) {
            return errlog_null( ERR_CRITICAL, "netcap_trie_item_data\n" );
        }
        
        if (( rep_val = _reputation_eval( item )) < 0 ) {
            return errlog_null( ERR_CRITICAL,"_reputation_eval\n");
        }
        
        /* IP has a terrible rep, user is done no matter what the status */
        /* -RBS No more concept of a maximum reputation */
        /* if ( rep_val > _SHIELD_REP_MAX ) { ans = NC_SHIELD_DROP; break; } */
                    
        /* Determine whether they get in based on the state of the shield */
        if (( fence = _get_fence( mode )) == NULL )
            return errlog_null( ERR_CRITICAL, "_get_fence" );
    } while ( 0 );
    
    /* Only inherit a piece of the reputation if this is not a terminal node */
    if ( item != NULL && item->type == NC_TRIE_BASE_LEVEL ) { 
        rep_val = rep_val *  fence->inheritance;
    }
    
    ans = _put_in_fence ( fence, rep_val );
    
    response->if_print = 0;

    if ( ans != NC_SHIELD_YES ) {
        /* If the count is low, update the print count */
        if ( rep->print_load.load < _shield.cfg.print_rate ) {
            if ( pthread_mutex_lock( &rep->mutex ) < 0 ) return perrlog_null( "pthread_mutex_lock" );
            netcap_load_update( &rep->print_load, 1, 1 );
            if ( pthread_mutex_unlock( &rep->mutex ) < 0 ) return perrlog_null( "pthread_mutex_unlock" );
            response->if_print = 1;
        }
    }

    /* XXX Could add a mutex ??? for the ++, but the information is not critical, just for debugging */
    switch ( ans ) {
    case NC_SHIELD_LIMITED: rep->limited_sessions++; break;
    case NC_SHIELD_DROP:
    case NC_SHIELD_RESET:  rep->rejected_sessions++; break;
    default: break;
    }
    
    /* XXX For now set everything the same */
    response->tcp = ans;
    response->udp = ans;
    response->icmp = ans;

    return response;
}
 
int                  netcap_shield_rep_blame        ( in_addr_t ip, int amount )
{
    _apply_func_t func = { _add_evil, (void*)amount };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;
        
    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func, &func ) == NULL ) {
        return errlog(ERR_CRITICAL, "netcap_trie_apply\n");
    }
    return 0;
}

int                  netcap_shield_tls_init         ( shield_tls_t* tls )
{
    if ( tls == NULL ) return errlogargs();

    /* Nothing to do here, tls is just an output buffer */
    return 0;
}

/* Increment the number of active sessions for ip     *
 * If the ip is not on the list, automatically add it */
int                  netcap_shield_rep_add_request  ( in_addr_t ip )
{
    _apply_func_t func = { .func _add_request, .arg NULL };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;

    /* Do not create nodes, only update the nodes that exist */
    if ( netcap_trie_apply_close ( &_shield.trie, ip, _apply_func, &func ) == NULL ) {
        return errlog(ERR_CRITICAL, "netcap_trie_apply_close\n");
    }
    return 0;
}

/* Decrement the number of active sessions for ip     *
 * If the ip is not on the list, automatically add it */ 
int                  netcap_shield_rep_add_session  ( in_addr_t ip )
{
    _apply_func_t func = { .func _add_session, .arg NULL };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;

    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func, &func ) == NULL ) {
        return errlog(ERR_CRITICAL, "netcap_trie_apply\n");
    }
    return 0;
}

int                  netcap_shield_rep_end_session  ( in_addr_t ip )
{
    _apply_func_t func = { .func _end_session, .arg NULL };
    
    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;
    
    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func,&func ) == NULL ) {
        return errlog(ERR_CRITICAL,"netcap_trie_apply\n");
    }
    
    return 0;
}

/* Increment the number of server connections */
int                  netcap_shield_rep_add_srv_conn ( in_addr_t ip )
{
    _apply_func_t func = { .func _add_srv_conn, .arg NULL };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;

    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func, &func ) == NULL ) {
        return errlog(ERR_CRITICAL, "netcap_trie_apply\n");
    }

    return 0;
}

/* Increment the number of server connections failures */
int                  netcap_shield_rep_add_srv_fail ( in_addr_t ip )
{
    _apply_func_t func = { .func _add_srv_fail, .arg NULL };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;

    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func, &func ) == NULL ) {
        return errlog(ERR_CRITICAL, "netcap_trie_apply\n");
    }

    return 0;
}

int                  netcap_shield_rep_add_chunk ( in_addr_t ip, int protocol, u_short size )
{
    _chk_t chk = { .size size, .if_rx 1, .protocol protocol };
    _apply_func_t func = { .func _add_chk, .arg (void*)&chk };

    /* Do nothing if the shield is not enabled */
    if ( !_shield.enabled ) return 0;
    
    if ( netcap_trie_apply ( &_shield.trie, ip, _apply_func, &func) == NULL ) {
        return errlog(ERR_CRITICAL,"netcap_trie_apply\n");
    }

    return 0;
}

int                  netcap_shield_cfg_load      ( char *buf, int buf_len )
{
    int lw, hw, ss;

    if ( buf == NULL || buf_len < 0 ) return errlogargs();

    if ( nc_shield_cfg_load ( &_shield.cfg, buf, buf_len ) < 0 ) {
        return errlog ( ERR_CRITICAL, "nc_shield_cfg_load\n" );
    }

    lw = _shield.cfg.lru.low_water;
    hw = _shield.cfg.lru.high_water;
    ss = _shield.cfg.lru.sieve_size;

    /* Configure the LRU */
    if ( netcap_trie_lru_config ( &_shield.trie, hw, lw, ss ) < 0 ) {
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

    if ( !_shield.enabled ) return 0;

    if ( pthread_mutex_lock ( &_shield.dbg_mutex ) < 0 ) return perrlog ( "pthread_mutex_lock" );
    
    ret = _status ( conn, dst_addr );
    
    if ( pthread_mutex_unlock ( &_shield.dbg_mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    return ret;
#endif
}


static void  _trash_fill         ( void* arg )
{
    if ( _shield.enabled ) {
#ifdef _TRIE_DEBUG_PRINT
        if ( pthread_mutex_lock ( &_shield.dbg_mutex ) < 0 ) perrlog ( "pthread_mutex_lock" );
#endif
        netcap_trie_lru_update ( &_shield.trie );

        /* Schedule to fill the trash again */
        if ( netcap_sched_event ( _trash_fill, NULL, _SHIELD_FILL_TRASH_INTERVAL ) < 0 ) {
            errlog ( ERR_CRITICAL, "netcap_sched_event\n" );
        }

#ifdef _TRIE_DEBUG_PRINT
        if ( pthread_mutex_unlock ( &_shield.dbg_mutex ) < 0 ) perrlog ( "pthread_mutex_unlock" );
#endif
    }
}

static int  _apply_func          ( netcap_trie_item_t* item, void* arg, in_addr_t ip )
{
    _apply_func_t* func;
    reputation_t* rep;
    int ret = 0;
    int children;
    netcap_load_val_t lru_load;

    if ( item == NULL || ( rep = netcap_trie_item_data( item )) == NULL ) return errlogargs();
    if ( arg == NULL || ((_apply_func_t*)arg)->func == NULL ) return errlogargs();
    
    func = (_apply_func_t*)arg;
    
    if ( pthread_mutex_lock( &rep->mutex ) < 0 ) {
        return perrlog( "pthread_mutex_lock" );
    }

    do {
        if (( children = netcap_trie_element_children((netcap_trie_element_t)item )) < 0 ) {
            ret = errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
            break;
        }
        
        /* Count is zero if a node doesn't have any children */
        children = ( children == 0 ) ? 1 : children;
        
        /* Apply the function to the argument */
        if (( ret == 0 ) && ( func->func( rep, children, func->arg ) < 0 )) {
            ret = errlog( ERR_CRITICAL, "apply_function\n" );
            break;
        }
        
        if (( lru_load = netcap_load_get( &rep->lru_load )) < 0 ) {
            ret = errlog( ERR_CRITICAL, "netcap_load_get\n" );
            break;
        }

        /* Move the node to front of the LRU */
        if ( lru_load < _shield.cfg.lru.ip_rate ) {
            if ( netcap_trie_lru_front( &_shield.trie, (netcap_trie_element_t)item ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "netcap_trie_lru_front\n" );
                break;
            }
            if ( netcap_load_update( &rep->lru_load, 1, 1 ) < 0 ) {
                ret = errlog( ERR_CRITICAL, "netcap_load_update\n" );
                break;
            }
        }
        ret = 0;
    } while( 0 );
    
    if ( pthread_mutex_unlock( &rep->mutex ) < 0 ) {
        ret = perrlog("pthread_mutex_unlock");
    }
    
    return ret;
}

static int  _lru_check           ( netcap_trie_item_t* item )
{
    reputation_t* rep;
    netcap_load_val_t lru_load;
    
    if ( item == NULL || ( rep = netcap_trie_item_data( item )) == NULL ) return errlogargs();

    if (( lru_load = netcap_load_get( &rep->lru_load )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_load_get\n" );
    }

    if ( lru_load < .000001 ) {
        return NC_TRIE_IS_DELETABLE;
    }
    
    return !NC_TRIE_IS_DELETABLE;
}

static int  _add_request         ( reputation_t *rep, int count, void* arg )
{
    return netcap_load_update( &rep->request_load, 1, ((netcap_load_val_t)1.0));
}

static int  _add_session         ( reputation_t *rep, int count, void* arg )
{
    /* Increment the number of sessions */
    rep->active_sessions++;
    return netcap_load_update( &rep->session_load, 1, ((netcap_load_val_t)1.0));
}

static int  _end_session         ( reputation_t *rep, int count, void* arg )
{
    /* Decrement the number of sessions */
    if ( rep->active_sessions < 1 ) rep->active_sessions = 0;
    else rep->active_sessions--;
    return 0;
}

static int  _add_srv_conn        ( reputation_t *rep, int count, void* arg )
{
    return netcap_load_update( &rep->srv_conn_load, 1, ((netcap_load_val_t)1.0));
}

static int  _add_srv_fail        ( reputation_t *rep, int count, void* arg )
{
    return netcap_load_update( &rep->srv_fail_load, 1, ((netcap_load_val_t)1.0));
}

static int  _add_evil            ( reputation_t *rep, int count, void* arg )
{
    int evil = (int)arg;
    return netcap_load_update( &rep->evil_load, evil, ((netcap_load_val_t)evil));
}

static int _add_chk              ( reputation_t *rep, int count, void* arg )
{
    _chk_t* chk;
    netcap_load_t* load = NULL;
    
    if ( (chk = (_chk_t*)arg) == NULL ) return errlogargs();

    switch ( chk->protocol ) {
    case IPPROTO_TCP: load = &rep->tcp_chk_load; break;
    case IPPROTO_UDP: load = &rep->udp_chk_load; break;
    default: return errlogargs();
    }
    
    if ( chk->if_rx == 1 ) {
        netcap_load_update ( load, 1, ((netcap_load_val_t)1.0));
        netcap_load_update ( &rep->byte_load, chk->size, ((netcap_load_val_t)chk->size));
    } else {
        return errlog( ERR_CRITICAL, "Invalid chunk Description\n" );
    }
    
    return 0;
}

static int _reputation_update    ( reputation_t *rep, int count, void* arg )
{
    if (( netcap_load_update( &rep->evil_load,     0, 0 ) < 0) ||
        ( netcap_load_update( &rep->request_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &rep->session_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &rep->srv_conn_load, 0, 0 ) < 0) ||
        ( netcap_load_update( &rep->srv_fail_load, 0, 0 ) < 0) ||
        ( netcap_load_update( &rep->tcp_chk_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &rep->udp_chk_load,  0, 0 ) < 0) ||
        ( netcap_load_update( &rep->byte_load,     0, 0 ) < 0) ||
        ( netcap_load_update( &rep->print_load,    0, 0 ) < 0)) {
        return errlog(ERR_CRITICAL,"netcap_load_update\n");
    }
    
    return 0;
}

static int  _reputation_init     ( netcap_trie_item_t* item, in_addr_t ip )
{
    int ret = 0;
    reputation_t* rep;
    nc_shield_fence_t* fence = NULL;

    if ( item == NULL || (rep = netcap_trie_item_data(item)) == NULL ) return errlogargs();

    rep = item->data;

    /* Create the mutex */
    if ( pthread_mutex_init ( &rep->mutex, NULL ) < 0 ) {
        return perrlog("pthread_mutex_init");
    }

    /* Lock mutex */
    if ( pthread_mutex_lock( &rep->mutex ) < 0 ) {
        return perrlog("pthread_mutex_lock");
    }
    
    /* Initalize each of the loads */
    /* Right now these inherit the load and the last update time of the parent */
    if (( netcap_load_init( &rep->evil_load,     _LOAD_INTERVAL_EVIL,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->request_load,  _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->session_load,  _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->srv_conn_load, _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->srv_fail_load, _LOAD_INTERVAL_SESS,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->tcp_chk_load,  _LOAD_INTERVAL_CHK,   !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->udp_chk_load,  _LOAD_INTERVAL_CHK,   !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->byte_load,     _LOAD_INTERVAL_BYTE,  !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->print_load,    _LOAD_INTERVAL_PRINT, !NC_LOAD_INIT_TIME ) < 0) ||
        ( netcap_load_init( &rep->lru_load,      _LOAD_INTERVAL_LRU,   !NC_LOAD_INIT_TIME ) < 0)) {
        ret = errlog( ERR_CRITICAL, "netap_load_init\n" );
    }
    
    /* Parent is unititialized, set the rep to zero */
    if ( rep->rep < 0 ) rep->rep = 0;

    rep->active_sessions   = 0;
    rep->rejected_sessions = 0;
    rep->limited_sessions   = 0;

    if (( fence = _get_fence( _shield.mode )) == NULL ) {
        ret = errlog( ERR_CRITICAL, "_get_fence\n" );
    } else {
        /* Apply inheritance to the reputation */
        rep->evil_load.load     = ( fence->inheritance ) * rep->evil_load.load;
        rep->request_load.load  = ( fence->inheritance ) * rep->request_load.load;
        rep->session_load.load  = ( fence->inheritance ) * rep->session_load.load;
        rep->srv_conn_load.load = ( fence->inheritance ) * rep->srv_conn_load.load;
        rep->srv_fail_load.load = ( fence->inheritance ) * rep->srv_fail_load.load;
        rep->tcp_chk_load.load  = ( fence->inheritance ) * rep->tcp_chk_load.load;
        rep->udp_chk_load.load  = ( fence->inheritance ) * rep->udp_chk_load.load;
        rep->byte_load.load     = ( fence->inheritance ) * rep->byte_load.load;
    }


    pthread_mutex_unlock(&rep->mutex);

#ifdef _TRIE_DEBUG_PRINT
    rep->ip    = ip;
    if (( rep->self = list_add_tail ( &_shield.ip_list, item )) == NULL ) {
        return errlog(ERR_CRITICAL,"list_add_tail\n");
    }    
#endif

    return ret;
}

static nc_shield_rep_t  _reputation_eval     ( netcap_trie_item_t* item )
{
    nc_shield_rep_t rep_val;
    reputation_t* rep;
    
    if ( item == NULL ) return (nc_shield_rep_t)errlogargs();

    if (( rep = netcap_trie_item_data( item )) == NULL ) {
        return (nc_shield_rep_t)errlog ( ERR_CRITICAL, "netcap_trie_item_data\n" );
    }

    rep_val  = rep->request_load.load * _shield.cfg.mult.request_load;
    rep_val += rep->session_load.load * _shield.cfg.mult.session_load;
    rep_val += rep->tcp_chk_load.load * _shield.cfg.mult.tcp_chk_load;
    rep_val += rep->udp_chk_load.load * _shield.cfg.mult.udp_chk_load;
    rep_val += rep->evil_load.load    * _shield.cfg.mult.evil_load;

    /* Use the number of active sessions * multiplier * (depth/total_depth)^2, this way single child
     * nodes cannot refuse connections to new nodes */
    rep_val += (rep->active_sessions+0.0) * _shield.cfg.mult.active_sess * 
        (( item->depth * item->depth ) / (( 0.0 + NC_TRIE_DEPTH_TOTAL ) * ( 0.0 + NC_TRIE_DEPTH_TOTAL )));
    
    /* XXX Use whether or not their depth is terminal (this is a new node) */
    /* XXX Use the server connection/failure rate */
    return ( rep->rep = rep_val );
}

static void _reputation_destroy  ( netcap_trie_item_t* item )
{
    reputation_t* rep;
    
    if ( item == NULL || (rep = netcap_trie_item_data(item)) == NULL ) return (void)errlogargs();

    rep = item->data;

    /* Destroy the mutex */
    if ( pthread_mutex_destroy ( &rep->mutex ) < 0 ) perrlog("pthread_mutex_destroy");

#ifdef _TRIE_DEBUG_PRINT
    /* Set the item on the list to NULL */
    rep->self->val = NULL;
#endif // _TRIE_DEBUG_PRINT
}

static netcap_shield_ans_t _put_in_fence  ( nc_shield_fence_t* fence, nc_shield_rep_t rep_val )
{
    nc_shield_rep_t prob;
    netcap_shield_ans_t ans = NC_SHIELD_DROP;

    prob = ( rand() + 0.0 ) / RAND_MAX;
    
    /* -RBS */
    // debug ( 0, "_put_in_fence: %lg cmp %lg\n", prob, fence->closed.prob );

    if ( rep_val > fence->closed.post ) {
        ans = ( prob < fence->closed.prob ) ? NC_SHIELD_DROP : NC_SHIELD_LIMITED;
    } else if ( rep_val > fence->limited.post ) {
        ans = ( prob < fence->limited.prob ) ? NC_SHIELD_LIMITED : NC_SHIELD_YES;
    } else {
        ans = NC_SHIELD_YES;
    }
    
    return ans;
}    


static netcap_shield_mode_t _mode_eval ( void )
{
    reputation_t* root_rep;
    int num_sessions;
    int num_clients;
    root_rep = _shield.root;
    int children;

    /* XXX Right now, checking the load on each call, for performance,
     * possibly consider doing this periodically */
    double load;
    
    netcap_load_val_t request_load;
    netcap_load_val_t session_load;
    netcap_load_val_t tcp_chk_load;
    netcap_load_val_t udp_chk_load;
    netcap_load_val_t evil_load;
    
    if ( root_rep == NULL ) return errlog(ERR_CRITICAL,"Shield is uninitialized\n");

    if ( getloadavg ( &load, 1 ) < 0 ) return perrlog ( "sysinfo" );
    
    /* Update the root reputation */
    if (( children = netcap_trie_element_children(( netcap_trie_element_t)&_shield.trie.root )) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
    }
    children = ( children == 0 ) ? 1 : children;

    if ( _reputation_update ( root_rep, children, NULL) < 0 ) {
        return errlog(ERR_CRITICAL,"_reputation_update\n");
    }

    num_sessions = root_rep->active_sessions;

    // Need to determine a way to calculate better averages, right now using the sum
    // num_clients = _shield.trie.lru_length;
    num_clients = 1;
    
    request_load = root_rep->request_load.load * num_clients;
    session_load = root_rep->session_load.load * num_clients;
    tcp_chk_load = root_rep->tcp_chk_load.load * num_clients;
    udp_chk_load = root_rep->udp_chk_load.load * num_clients;
    evil_load    = root_rep->evil_load.load    * num_clients;
    
    if (( load         > _shield.cfg.limit.cpu_load.closed )     || 
        ( num_sessions > _shield.cfg.limit.sessions.closed )     ||
        ( request_load > _shield.cfg.limit.request_load.closed ) || 
        ( session_load > _shield.cfg.limit.session_load.closed ) ||
        ( tcp_chk_load > _shield.cfg.limit.tcp_chk_load.closed ) ||
        ( udp_chk_load > _shield.cfg.limit.udp_chk_load.closed ) ||
        ( evil_load    > _shield.cfg.limit.evil_load.closed )) return NC_SHIELD_MODE_CLOSED;


    if (( load         > _shield.cfg.limit.cpu_load.tight )     ||
        ( num_sessions > _shield.cfg.limit.sessions.tight )     ||
        ( request_load > _shield.cfg.limit.request_load.tight ) ||
        ( session_load > _shield.cfg.limit.session_load.tight ) ||
        ( tcp_chk_load > _shield.cfg.limit.tcp_chk_load.tight ) ||
        ( udp_chk_load > _shield.cfg.limit.udp_chk_load.tight ) ||
        ( evil_load    > _shield.cfg.limit.evil_load.tight )) return NC_SHIELD_MODE_TIGHT;


    if (( load         > _shield.cfg.limit.cpu_load.lax )     ||
        ( num_sessions > _shield.cfg.limit.sessions.lax )     ||
        ( request_load > _shield.cfg.limit.request_load.lax ) ||
        ( session_load > _shield.cfg.limit.session_load.lax ) ||
        ( tcp_chk_load > _shield.cfg.limit.tcp_chk_load.lax ) ||
        ( udp_chk_load > _shield.cfg.limit.udp_chk_load.lax ) ||
        ( evil_load    > _shield.cfg.limit.evil_load.lax )) return NC_SHIELD_MODE_LAX;

    return NC_SHIELD_MODE_RELAXED;
}

static netcap_shield_response_t* _tls_get( void )
{
    netcap_tls_t* netcap_tls;
    if (( netcap_tls = netcap_tls_get()) == NULL ) return errlog_null( ERR_CRITICAL, "netcap_tls_get\n" );
    
    return &netcap_tls->shield.ans;
}

#ifdef _TRIE_DEBUG_PRINT
static int  _status             ( int conn, struct sockaddr_in *dst_addr )
{
    list_node_t* step;
    list_node_t* next;
    reputation_t* rep;
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
        
        if (( rep  = (reputation_t*)netcap_trie_item_data ( item )) == NULL ) continue;

        if (( children = netcap_trie_element_children((netcap_trie_element_t)item )) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_trie_element_children\n" );
            continue;
        }
        
        children = ( children == 0 ) ? 1 : children;

        /* Update the reputation */
        _reputation_update ( rep, children, NULL );
        _reputation_eval( item );
        
        msg_len =  snprintf( buf, sizeof(buf), "<rep ip='%#08x' depth='%d' active='%d' val='%lg' "
                             "evil-load='%g' evil-total='%d' request-load='%g' request-total='%d' "
                             "session-load='%g' session-total='%d' srv-conn-load='%g' srv-conn-total='%d' "
                             "srv-fail-load='%g' srv-fail-total='%d' "
                             "tcp-chk-load='%g' tcp-chk-total='%d' udp-chk-load='%g' udp-chk-total='%d' "
                             "byte-load='%g' byte-total='%d' rejected='%d' limited='%d'/>\n",
                             ntohl(rep->ip), item->depth, rep->active_sessions, rep->rep,
                             rep->evil_load.load, rep->evil_load.total,
                             rep->request_load.load, rep->request_load.total,
                             rep->session_load.load, rep->session_load.total,
                             rep->srv_conn_load.load, rep->srv_conn_load.total,
                             rep->srv_fail_load.load, rep->srv_fail_load.total,
                             rep->tcp_chk_load.load, rep->tcp_chk_load.total,
                             rep->udp_chk_load.load, rep->udp_chk_load.total,
                             rep->byte_load.load, rep->byte_load.total,
                            rep->rejected_sessions, rep->limited_sessions );
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



