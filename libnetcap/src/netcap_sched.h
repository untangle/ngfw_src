/**
 * $Id: netcap_sched.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_SCHED_H_
#define __NETCAP_SCHED_H_

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


