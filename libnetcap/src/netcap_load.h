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


#ifndef __NETCAP_LOAD_H_
#define __NETCAP_LOAD_H_

#include <sys/time.h>
#include <time.h>

typedef double netcap_load_val_t;

typedef struct netcap_load {
    struct timeval    last_update;
    netcap_load_val_t load;
    netcap_load_val_t interval;
    int               total;
} netcap_load_t;

#define NC_LOAD_INIT_TIME 1

/**
 * load: load to update
 * count: Amount to update the total by
 * val: amount to update the load by
 */ 
int               netcap_load_update  ( netcap_load_t* load, int count, netcap_load_val_t val );

netcap_load_val_t netcap_load_get     ( netcap_load_t* load );

netcap_load_t*    netcap_load_malloc  ( void );
int               netcap_load_init    ( netcap_load_t* load, netcap_load_val_t interval, int if_init_time );
netcap_load_t*    netcap_load_create  ( netcap_load_val_t interval, int if_init_time );

void              netcap_load_free    ( netcap_load_t* load );
void              netcap_load_destroy ( netcap_load_t* load );
void              netcap_load_raze    ( netcap_load_t* load );

#endif // __NETCAP_LOAD_H_
