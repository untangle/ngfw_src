/*
 * $HeadURL:$
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

#include <netinet/in.h>
#include <arpa/inet.h>
#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/utime.h>

#include "libnetcap.h"
#include "netcap_shield.h"
#include "netcap_shield_cfg.h"
#include "netcap_load.h"
#include "netcap_sched.h"

/* Flag to indicate that the shield mode updates are alive */
#define _NC_SHIELD_MODE_IS_ALIVE 0xA3D00DAD
#define _NC_SHIELD_MODE_IS_DEAD  0xDEADD00D

/* Perform a shield update every 50 milliseconds */
#define _NC_SHIELD_UPD_INTVL_USEC MSEC_TO_USEC( 50 )

/* Generate a shield event every 6000 ticks (5 minutes at 50 milliseconds) */
#define _NC_SHIELD_EVT_TICKS 6000

/*  USE JUST FOR TESTING */
// #define _NC_SHIELD_EVT_TICKS 200

#define _EXCEED_MODE( MODE ) (( cpu_load      > cfg->limit.cpu_load. MODE )    || \
                              ( num_sessions  > cfg->limit.sessions.  MODE )     || \
                              ( request_load  > cfg->limit.request_load.  MODE ) || \
                              ( session_load  > cfg->limit.session_load.  MODE ) || \
                              ( tcp_chk_load  > cfg->limit.tcp_chk_load.  MODE ) || \
                              ( udp_chk_load  > cfg->limit.udp_chk_load.  MODE ) || \
                              ( icmp_chk_load > cfg->limit.icmp_chk_load. MODE ) || \
                              ( evil_load     > cfg->limit.evil_load.     MODE ))

static struct
{
    int is_alive;
    nc_shield_cfg_t* cfg;
    nc_shield_reputation_t* root_rep;
    netcap_shield_mode_t* mode;
    nc_shield_fence_t** fence;
    /* Number of ticks since generating the last event */
    int ticks;
    
    netcap_shield_counters_t counters;
    
    struct {
        /* This is the number of times in each mode since the last tick */
        int relaxed;
        int lax;
        int tight;
        int closed;
    } mode_ticks;
} _shield_mode = {
    .is_alive = 0,
    .cfg      = NULL,
    .root_rep = NULL,
    .mode     = NULL,
    .mode     = NULL,
    .fence    = NULL,
    
    .ticks    = 0,

    .mode_ticks = {
        .relaxed = 0,
        .lax     = 0,
        .tight   = 0,
        .closed  = 0
    }    
};

static int  _update( nc_shield_cfg_t* cfg, nc_shield_reputation_t* root_rep, 
                     netcap_shield_mode_t* mode, nc_shield_fence_t** fence );

static void _scheduler_event( void );

static int  _event( void );

static void _reset_counters();

static __inline__ int _is_alive( void )
{
    return ( _shield_mode.is_alive == _NC_SHIELD_MODE_IS_ALIVE );
}

int nc_shield_mode_init( nc_shield_cfg_t* cfg, nc_shield_reputation_t* root_rep, 
                         netcap_shield_mode_t* mode, nc_shield_fence_t** fence )
{
    if ( cfg == NULL || root_rep == NULL || mode == NULL || fence == NULL ) {
        return errlogargs();
    }

    _shield_mode.cfg      = cfg;
    _shield_mode.root_rep = root_rep;
    _shield_mode.mode     = mode;
    _shield_mode.fence    = fence;
        
    _reset_counters();
    
    debug( 4, "Shield monitor starting\n" );

    if ( netcap_sched_event_z( _scheduler_event, _NC_SHIELD_UPD_INTVL_USEC ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_sched_event\n" );
    }

    _shield_mode.is_alive = _NC_SHIELD_MODE_IS_ALIVE;
    
    return 0;
}

int nc_shield_mode_cleanup( void )
{    
    if ( !_is_alive()) return 0;
    
    /* Indicate that the mode manager is no longer alive */
    _shield_mode.is_alive = _NC_SHIELD_MODE_IS_DEAD;
        
    return 0;
}

int nc_shield_stats_add_request( int protocol, netcap_shield_ans_t response )
{
    netcap_shield_response_counters_t* protocol_counter = NULL;
    
    if ( _is_alive()) {
        switch ( protocol ) {
        case IPPROTO_TCP:
            protocol_counter = &_shield_mode.counters.tcp;
            break;
            
        case IPPROTO_UDP:
            protocol_counter = &_shield_mode.counters.udp;
            break;
            
        case IPPROTO_ICMP:
            protocol_counter = &_shield_mode.counters.icmp;
            break;
            
        default:
            errlog( ERR_CRITICAL, "Unknown protocol: %d\n", protocol );
            protocol_counter = NULL;
        }

        switch ( response ) {
        case NC_SHIELD_RESET:
            _shield_mode.counters.total.rejected++; 
            if ( protocol_counter != NULL ) protocol_counter->rejected++;
            break;
            
        case NC_SHIELD_DROP:
            _shield_mode.counters.total.dropped++;  
            if ( protocol_counter != NULL ) protocol_counter->dropped++;
            break;
            
        case NC_SHIELD_LIMITED:
            _shield_mode.counters.total.limited++;
            if ( protocol_counter != NULL ) protocol_counter->limited++;
            break;

        case NC_SHIELD_YES:
            _shield_mode.counters.total.accepted++;
            if ( protocol_counter != NULL ) protocol_counter->accepted++;
            break;

        default: 
            return errlog( ERR_CRITICAL, "Invalid response: %d\n", response );
        }
        /* XXX Add the interface also */
    } else {
        debug( 4, "Shield monitor is no longer runnning\n" );
    }
    return 0;
}


static int _update( nc_shield_cfg_t* cfg, nc_shield_reputation_t* root_rep, 
                    netcap_shield_mode_t* mode, nc_shield_fence_t** fence )
{
    int num_sessions;
    double num_clients;

    double cpu_load;
    
    netcap_load_val_t request_load;
    netcap_load_val_t session_load;
    netcap_load_val_t tcp_chk_load;
    netcap_load_val_t udp_chk_load;
    netcap_load_val_t icmp_chk_load;
    netcap_load_val_t evil_load;
    
    if ( cfg == NULL || root_rep == NULL || mode == NULL || fence == NULL ) return errlogargs();

    if ( getloadavg( &cpu_load, 1 ) < 1 ) {
        perrlog ( "getloadavg" );

        /* No reason to make this fatal, it is mostly ignored */
        cpu_load = 0.5;
    }

    /* Update the root reputation */
    if ( nc_shield_reputation_update( root_rep ) < 0 ) {
        return errlog( ERR_CRITICAL, "nc_shield_reputation_update\n" );
    }
    
    num_sessions = root_rep->active_sessions;

    // Need to determine a way to calculate better averages, right now using the sum
    // Could turn the number of clients into a load that gets calculated over time
    num_clients = 1.0;
    
    request_load  = root_rep->request_load.load  * num_clients;
    session_load  = root_rep->session_load.load  * num_clients;
    tcp_chk_load  = root_rep->tcp_chk_load.load  * num_clients;
    udp_chk_load  = root_rep->udp_chk_load.load  * num_clients;
    icmp_chk_load = root_rep->icmp_chk_load.load * num_clients;
    evil_load     = root_rep->evil_load.load     * num_clients;

    if ( _EXCEED_MODE( closed )) {
        *mode = NC_SHIELD_MODE_CLOSED;
        *fence = &cfg->fence.closed;
        _shield_mode.mode_ticks.closed++;
    } 
    else if ( _EXCEED_MODE( tight )) {
        *mode = NC_SHIELD_MODE_TIGHT;
        *fence = &cfg->fence.tight;
        _shield_mode.mode_ticks.tight++;
    } 
    else if ( _EXCEED_MODE( lax )) {
        *mode = NC_SHIELD_MODE_LAX;
        *fence = &cfg->fence.lax;
        _shield_mode.mode_ticks.lax++;
    } else {
        *mode = NC_SHIELD_MODE_RELAXED;
        *fence = &cfg->fence.relaxed;
        _shield_mode.mode_ticks.relaxed++;
    }
    
    return 0;
}

static void _scheduler_event( void )
{
    int _critical_section( void ) {
        _update( _shield_mode.cfg, _shield_mode.root_rep, _shield_mode.mode, _shield_mode.fence );
        
        if ( _shield_mode.ticks >= _NC_SHIELD_EVT_TICKS ) {
            _event();
            
            _reset_counters();
        } else {
            _shield_mode.ticks++;
        }
        
        return 0;
    }

    if ( !_is_alive()) {
        debug( 4, "Shield monitor is no longer alive\n" );
        return;
    }
    
    /* A cleaner way of doing the do while ( 0 ) clause */
    if ( _critical_section() < 0 ) errlog( ERR_CRITICAL, "Error updating the shield mode\n" );
    
    /* Always make sure to schedule the event again, if the shield is alive */
    if ( _is_alive()) {
        if ( netcap_sched_event_z( _scheduler_event, _NC_SHIELD_UPD_INTVL_USEC ) < 0 ) {
            errlog ( ERR_FATAL, "netcap_sched_event_z\n" );
        }
    }
}


static int _event( void )
{
    netcap_shield_event_data_t event;

    bzero( &event, sizeof( event ));
    
    event.type = NC_SHIELD_EVENT_STATISTIC;

    /* Response statistics */
    memcpy( &event.data.statistic.counters, &_shield_mode.counters, sizeof( _shield_mode.counters ));

    /* Mode statistics */
    event.data.statistic.relaxed  = _shield_mode.mode_ticks.relaxed;
    event.data.statistic.lax      = _shield_mode.mode_ticks.lax;
    event.data.statistic.tight    = _shield_mode.mode_ticks.tight;
    event.data.statistic.closed   = _shield_mode.mode_ticks.closed;
    
    /* Call the shield event hook */
    if ( nc_shield_event_hook( &event ) < 0 )
        errlog( ERR_CRITICAL, "nc_shield_event_hook\n" );
    
    return 0;
}

static void _reset_counters()
{
    /* Zero out all of the counters */
    bzero( &_shield_mode.counters, sizeof( _shield_mode.counters ));
    bzero( &_shield_mode.mode_ticks, sizeof( _shield_mode.mode_ticks ));
    
    /* Reset the ticks */
    _shield_mode.ticks = 1;
}
