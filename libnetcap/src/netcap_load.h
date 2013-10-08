/**
 * $Id: netcap_load.h 35571 2013-08-08 18:37:27Z dmorris $
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
