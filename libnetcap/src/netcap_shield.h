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

typedef enum  {
    NC_SHIELD_MODE_RELAXED,
    NC_SHIELD_MODE_LAX,
    NC_SHIELD_MODE_TIGHT,
    NC_SHIELD_MODE_CLOSED
} netcap_shield_mode_t;

#define NC_SHIELD_MODE_MAX NC_SHIELD_MODE_CLOSED

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

/* Indicate if an IP should allowed in */
netcap_shield_response_t* netcap_shield_rep_check        ( in_addr_t ip );

int                       netcap_shield_rep_add_request  ( in_addr_t ip );

int                       netcap_shield_rep_add_session  ( in_addr_t ip );

int                       netcap_shield_rep_add_srv_conn ( in_addr_t ip );

int                       netcap_shield_rep_add_srv_fail ( in_addr_t ip );

int                       netcap_shield_rep_blame        ( in_addr_t ip, int amount );

int                       netcap_shield_tls_init         ( shield_tls_t* tls );

#endif /* __NETCAP_SHIELD_H_ */
