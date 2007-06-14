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

#ifndef __NETCAP_SHIELD_CFG_H_
#define __NETCAP_SHIELD_CFG_H_

typedef double nc_shield_score_t;

typedef struct {
    double lax;
    double tight;
    double closed;
} nc_shield_limit_t;

typedef struct {
    /* Probability the fence post is used, if it isn't the level below is used */
    double          prob;
    nc_shield_score_t post;
} nc_shield_post_t;

typedef struct {
    /* Percent to inhert at each node */
    double           inheritance;
    /* IP access should be limited */
    nc_shield_post_t limited;
    /* IP should not be given access */
    nc_shield_post_t closed;
    /* IP is using an excessive amount of resources and may be notified */
    nc_shield_post_t error;
} nc_shield_fence_t;

typedef struct {
    struct {
        nc_shield_limit_t cpu_load;
        nc_shield_limit_t sessions;
        nc_shield_limit_t request_load;
        nc_shield_limit_t session_load;
        nc_shield_limit_t tcp_chk_load;
        nc_shield_limit_t udp_chk_load;
        nc_shield_limit_t icmp_chk_load;
        nc_shield_limit_t evil_load;
    } limit;
    
    struct {
        double request_load;
        double session_load;
        double evil_load;
        double tcp_chk_load;
        double udp_chk_load;
        double icmp_chk_load;
        double active_sess;
    } mult;

    struct {
        int low_water;
        int high_water;

        /** Parameters to control the removal of "dead" nodes */
        /** Number of items to look through at the end of the LRU for inactive nodes */
        int sieve_size;
    
        /** Rate at which active nodes should be moved to the front of the LRU */
        double ip_rate;
    } lru;
        
    struct {
        nc_shield_fence_t relaxed;
        nc_shield_fence_t lax;
        nc_shield_fence_t tight;
        nc_shield_fence_t closed;
    } fence;

    /** Parameters that control how often rejection debugging messages
     * should printed, how often the shield listener is triggered for
     * these events, and how often high reputation debugging messages
     * are printed. */
    double print_rate;

    /** The inverse of the print rate, converted to microseconds */
    long print_delay;

    /** If a reputation exceeds this threshold, debugging messages are
     * printed out for a reputation. */
    double rep_threshold;
} nc_shield_cfg_t;

/* Load the default shield configuration */
int nc_shield_cfg_def  ( nc_shield_cfg_t* cfg );

int nc_shield_cfg_load ( nc_shield_cfg_t* cfg, char* buf, int buf_len );

int nc_shield_cfg_get  ( nc_shield_cfg_t* cfg, char* buf, int buf_len );

#endif // __NETCAP_SHIELD_CFG_H_
