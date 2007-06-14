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

#ifndef __NETCAP_SHIELD_H_
#define __NETCAP_SHIELD_H_

#include <netinet/in.h>
#include <pthread.h>

#include "libnetcap.h"
#include "netcap_lru.h"
#include "netcap_load.h"
#include "netcap_shield_cfg.h"


#ifdef DEBUG_ON
#define _TRIE_DEBUG_PRINT
#endif

#define NC_SHIELD_DEBUG_LOW   7
#define NC_SHIELD_DEBUG_HIGH 11

// This is the maximum value for the divider, 100.1, to allow for 100.0
#define NC_SHIELD_DIVIDER_MAX  ( 100.1 )

// This is the minimum value for the divider.
#define NC_SHIELD_DIVIDER_MIN  ( 1.0 / 50.0 )

enum {
    NC_SHIELD_ERR_1 = 5,
    NC_SHIELD_ERR_2 = 15,
    NC_SHIELD_ERR_3 = 45,
    NC_SHIELD_ERR_4 = 80
};

int netcap_shield_init ( void );

int netcap_shield_cleanup ( void );

typedef enum {
    NC_SHIELD_RESET,    /* Do not let them in, and reset their connection */
    NC_SHIELD_DROP,     /* Do not let them in, but drop their connection (For now always use DROP mode) */
    NC_SHIELD_LIMITED,  /* Let them in, with "limited" access */
    NC_SHIELD_YES       /* Let them in. */
} netcap_shield_ans_t;

typedef struct {
    u_char if_print;
    u_char ans;
} netcap_shield_response_t;

typedef struct {
    int limited;      /* Total number of requests given limited access */
    int dropped;      /* Total number of dropped requests */    
    int rejected;     /* Total number of requests that are rejected */
} nc_shield_rejection_counter_t;

typedef struct
{
    pthread_mutex_t mutex;
    
    /* Node to track this item inside of the LRU */
    netcap_lru_node_t lru_node;

    /* Configuration data */
    double divider;

    struct in_addr ip;

#ifdef _TRIE_DEBUG_PRINT
    /* Pointer to the value on the ip_list, must have the dbg_mutex to read or set this value*/
    list_node_t*    self;
#endif
    int             active_sessions;
    
    /* These are the number of events since the last log event */
    /* xxx terrible waste of space, but it is efficient */
    nc_shield_rejection_counter_t counters[NC_INTF_MAX];
    
    nc_shield_score_t score;       /* The current score for the IP */
    netcap_load_t   evil_load;     /* Evil events per second */
    netcap_load_t   request_load;  /* Number of request this IP makes */
    netcap_load_t   session_load;  /* Active number of sessions used */
    netcap_load_t   srv_conn_load; /* Server connections load */
    netcap_load_t   srv_fail_load; /* Load of failed connection attempts */
    netcap_load_t   tcp_chk_load;  /* TCP chunk load */
    netcap_load_t   udp_chk_load;  /* UDP chunk load */
    netcap_load_t   icmp_chk_load; /* ICMP chunk load */
    netcap_load_t   byte_load;     /* Byte load */
    netcap_load_t   print_load;    /* Printing rate, limited to x per second */
    netcap_load_t   lru_load;      /* LRU rate, limited to x per second */
} nc_shield_reputation_t;

/* Indicate if an IP should allowed in */
int netcap_shield_rep_check ( netcap_shield_response_t* response, struct in_addr* ip, int protocol, 
                              netcap_intf_t intf );

int netcap_shield_rep_add_request  ( struct in_addr* ip );

int netcap_shield_rep_add_session  ( struct in_addr* ip );

int netcap_shield_rep_add_srv_conn ( struct in_addr* ip );

int netcap_shield_rep_add_srv_fail ( struct in_addr* ip );

int netcap_shield_rep_blame        ( struct in_addr* ip, int amount );

/* Local utility functions that should only used internally by the shield */

/* Donate a thread to update the mode */
int nc_shield_mode_init( nc_shield_cfg_t* cfg, nc_shield_reputation_t* root_rep, 
                         netcap_shield_mode_t* mode, nc_shield_fence_t** fence );

/* Tell the mode thread to exit */
int nc_shield_mode_cleanup( void );

/* Update the loads for a reputation */
int nc_shield_reputation_update( nc_shield_reputation_t* reputation );

/* Call the event hook */
int nc_shield_event_hook( netcap_shield_event_data_t* event );

/* Add an session to the global stats */
int nc_shield_stats_add_request( int protocol, netcap_shield_ans_t response );

#endif /* __NETCAP_SHIELD_H_ */
