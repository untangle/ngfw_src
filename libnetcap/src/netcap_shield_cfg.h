/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_shield_cfg.h,v 1.1 2004/11/09 19:40:00 dmorris Exp $
 */

#ifndef __NETCAP_SHIELD_CFG_H_
#define __NETCAP_SHIELD_CFG_H_

typedef double nc_shield_rep_t;

typedef struct {
    double lax;
    double tight;
    double closed;
} nc_shield_limit_t;

typedef struct {
    /* Probability the fence post is used, if it isn't the level below is used */
    double          prob;
    nc_shield_rep_t post;
} nc_shield_post_t;

typedef struct {
    nc_shield_post_t limited;
    nc_shield_post_t closed;
} nc_shield_fence_t;

typedef struct {
    struct {
        nc_shield_limit_t cpu_load;
        nc_shield_limit_t sessions;
        nc_shield_limit_t request_load;
        nc_shield_limit_t session_load;
        nc_shield_limit_t tcp_chk_load;
        nc_shield_limit_t udp_chk_load;
        nc_shield_limit_t evil_load;
    } limit;
    
    struct {
        double request_load;
        double session_load;
        double evil_load;
        double tcp_chk_load;
        double udp_chk_load;
        double active_sess;
    } mult;

    struct {
        int low_water;
        int high_water;
    } lru;
        
    struct {
        nc_shield_fence_t relaxed;
        nc_shield_fence_t lax;
        nc_shield_fence_t tight;
        nc_shield_fence_t closed;
    } fence;
} nc_shield_cfg_t;

/* Load the default shield configuration */
int nc_shield_cfg_def  ( nc_shield_cfg_t* cfg );

int nc_shield_cfg_load ( nc_shield_cfg_t* cfg, char* buf, int buf_len );

int nc_shield_cfg_get  ( nc_shield_cfg_t* cfg, char* buf, int buf_len );

#endif // __NETCAP_SHIELD_CFG_H_
