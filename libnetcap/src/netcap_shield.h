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

#ifndef __NETCAP_SHIELD_H_
#define __NETCAP_SHIELD_H_

#include <netinet/in.h>
#include <pthread.h>

#include "libnetcap.h"
#include "netcap_load.h"
#include "netcap_shield_cfg.h"


#ifdef DEBUG_ON
#define _TRIE_DEBUG_PRINT
#endif

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
    u_char tcp;
    u_char udp;
    u_char icmp;
} netcap_shield_response_t;

typedef struct {
    netcap_shield_response_t ans;
} shield_tls_t;

typedef struct reputation {
    pthread_mutex_t mutex;

#ifdef _TRIE_DEBUG_PRINT
    in_addr_t       ip;
    /* Pointer to the value on the ip_list, must have the dbg_mutex to read or set this value*/
    list_node_t*    self;
#endif
    int             active_sessions;
    
    uint             limited_sessions;      /* Total number of sessions given limited access */
    uint             rejected_sessions;     /* Total number of sessions that are rejected */
    uint             dropped_sessions;      /* Total number of dropped sessions */

    struct {
        uint limited_sessions;  /* Number of limited session the last log time */
        uint rejected_sessions; /* Number of rejected session the last log time */
        uint dropped_sessions;  /* Total number of dropped sessions */
    } last_log;
    
    nc_shield_rep_t rep;           /* The current reputation for the IP */
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
netcap_shield_response_t* netcap_shield_rep_check        ( in_addr_t ip );

int                       netcap_shield_rep_add_request  ( in_addr_t ip );

int                       netcap_shield_rep_add_session  ( in_addr_t ip );

int                       netcap_shield_rep_add_srv_conn ( in_addr_t ip );

int                       netcap_shield_rep_add_srv_fail ( in_addr_t ip );

int                       netcap_shield_rep_blame        ( in_addr_t ip, int amount );

int                       netcap_shield_tls_init         ( shield_tls_t* tls );


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
int nc_shield_stats_add_session( netcap_shield_ans_t response );

#endif /* __NETCAP_SHIELD_H_ */
