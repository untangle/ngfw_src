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

#ifndef _NETCAP_SCHED_H_

typedef void  (netcap_sched_func_t)    ( void* arg );

typedef void  (netcap_sched_func_z_t)  ( void );


int   netcap_sched_init      ( void );

int   netcap_sched_event     ( netcap_sched_func_t* func, void* arg, int usec );

int   netcap_sched_event_z   ( netcap_sched_func_z_t* func, int usec );

int   netcap_sched_cleanup   ( netcap_sched_func_t* func, void* arg );

int   netcap_sched_cleanup_z ( netcap_sched_func_z_t* func );

/* -RBS XXX - These functions are in the case where you want to have multiple schedulers */
/* netcap_sched_t* netcap_sched_malloc     ( void ); */
/* int             netcap_sched_init       ( netcap_sched_t* sched ); */
/* netcap_sched_t* netcap_sched_create     ( void ); */

/* int             netcap_sched_free       ( netcap_sched_t* sched ); */
/* int             netcap_sched_destroy    ( netcap_sched_t* sched ); */
/* int             netcap_sched_raze       ( netcap_sched_t* sched ); */

#endif // _NETCAP_SCHED_H_


