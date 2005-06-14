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

#ifndef __NETCAP_TRIE_H_
#define __NETCAP_TRIE_H_

#include <netinet/in.h>
#include <time.h>

#include <mvutil/list.h>
#include <mvutil/mailbox.h>

#define NC_TRIE_DEPTH_TOTAL 4
#define NC_TRIE_DEBUG_LOW   7
#define NC_TRIE_DEBUG_HIGH  11
#define NC_TRIE_ROW_SIZE    256

/* Just some random values to make it a little harder to accidentally free a random block of memory */
#define NC_TRIE_BASE_LEVEL  0xCB
#define NC_TRIE_BASE_ITEM   0xAC

/* This assumes all IPs are in Network Byte order */

typedef enum {
    NC_TRIE_FREE    = 1, /* Free items at the terminal nodes of the tree */
    NC_TRIE_COPY    = 2, /* Copy in items in new, and if the inherit flag is set. */
    NC_TRIE_INHERIT = 4, /* Set any new children to the most specific parent */
    NC_TRIE_LRU     = 8  /* Use the LRU queue to remove nodes */
} netcap_trie_flags_t;

/* The items marked with !!! should only be modified when you have the mutex */
typedef struct {
    /* These are all bytes so they get packed into one word */
    /* 1 - This is a level, 0 - This is an item */
    u_char type;

    /* Depth of this trie/item, the root is at 0 */
    u_char depth;
    
    /* Position of this trie/item inside of its parent */
    u_char pos;

    /* !!! 1, node can be added to the LRU, 0, node cannot be added to the LRU */
    u_char lru_rdy;

    /* An item at each level */
    void* data;

    /* !!! The position inside of the LRU or NULL if not on the LRU */
    list_node_t* lru_node;

    /* A mutex for inserting nodes and removing a node from the LRU */
    pthread_mutex_t      mutex;

    struct netcap_trie_level* parent;
} netcap_trie_base_t;

typedef netcap_trie_base_t netcap_trie_item_t;

typedef union {
    netcap_trie_base_t*       base;
    struct netcap_trie_level* level;
    netcap_trie_item_t*       item;
} netcap_trie_element_t;

typedef struct netcap_trie_level {
    netcap_trie_base_t base;

    int count;

    netcap_trie_element_t r[NC_TRIE_ROW_SIZE];
} netcap_trie_level_t;

/* This is an update function, it takes a pointer to a user defined structure,  *
 * updates it in some way, then returns.  This is a generic method for updating *
 * all of the parents of a terminal node on a trie */

typedef int  (netcap_trie_func_t)    ( netcap_trie_item_t* item, void* arg, in_addr_t ip );
typedef int  (netcap_trie_init_t)    ( netcap_trie_item_t* item, in_addr_t ip );
typedef void (netcap_trie_destroy_t) ( netcap_trie_item_t* item );

#define NC_TRIE_IS_DELETABLE 1

/* Return < 0 on an error, NC_IS_DELETABLE if the item is deletable, 0 otherwise */
typedef int  (netcap_trie_check_t)   ( netcap_trie_item_t* item );

typedef struct {
    int   mem;        /* Total amount of memory used by this trie */
    int   flags;
    int   item_count; /* Total number of items in the trie */
    int   lru_length; /* This may not be accurate at the time, but it is updated fairly regularly */

    netcap_trie_level_t root;

    /* When using the NC_TRIE_COPY flag, all items must be this size */
    int   item_size;

    /* An init function, called on the item, after each new item is created */
    netcap_trie_init_t* init;
    
    /* Destroy function, called on the item, before being destroyed */
    netcap_trie_destroy_t* destroy;

    /* Check function that indicates whether or not a node can be deleted */
    netcap_trie_check_t* check;

    /* LRU */
    /* Mutex required to call the function netcap_trie_lru_update */
    pthread_mutex_t lru_mutex;

    /* Number of items on LRU to initiate a delete */
    int    lru_high_water;

    /* The size at which to stop deleting items */
    int    lru_low_water;

    /* Number of items on the LRU to search for items to delete */
    int    lru_sieve_size;

    list_t lru_list;

    /* Trash mailbox */
    /* -RBS mailbox_t trash; */
} netcap_trie_t;

void* netcap_trie_get              ( netcap_trie_t* trie, in_addr_t ip );

/* Get the most specific node in the trie */
void* netcap_trie_get_close        ( netcap_trie_t* trie, in_addr_t ip );

void* netcap_trie_get_depth        ( netcap_trie_t* trie, in_addr_t ip, int depth );

/* This will automatically add an item if it is not in the trie */
netcap_trie_item_t* netcap_trie_set         ( netcap_trie_t* trie, in_addr_t ip, void *item);

netcap_trie_item_t* netcap_trie_set_depth   ( netcap_trie_t* trie, in_addr_t ip, void *item, int depth);

/* NULL on error, the last item func was applied to on success */
netcap_trie_item_t* netcap_trie_apply       ( netcap_trie_t* trie, in_addr_t ip, netcap_trie_func_t* func, 
                                              void* arg );

/* Apply until you have to create a node */
netcap_trie_item_t* netcap_trie_apply_close ( netcap_trie_t* trie, in_addr_t ip, netcap_trie_func_t* func, 
                                              void* arg );

netcap_trie_item_t* netcap_trie_apply_depth ( netcap_trie_t* trie, in_addr_t ip, 
                                              netcap_trie_func_t* func, void* arg, int depth );

/* delete: remove an item from the trie and then delete it, do not use
 * this function if the LRU flag is set */
int   netcap_trie_delete           ( netcap_trie_t* trie, in_addr_t ip );

int   netcap_trie_delete_depth     ( netcap_trie_t* trie, in_addr_t ip, int depth );

netcap_trie_t* netcap_trie_malloc  ( void );


int            netcap_trie_init    ( netcap_trie_t* trie, int flags, void* item, int item_size, 
                                     netcap_trie_init_t* init, netcap_trie_destroy_t* destroy,
                                     netcap_trie_check_t* check, int high_water, int low_water );

netcap_trie_t* netcap_trie_create  ( int flags, void* item, int item_size, 
                                     netcap_trie_init_t* init, netcap_trie_destroy_t* destroy,
                                     netcap_trie_check_t* check, int high_water, int low_water );

void           netcap_trie_free    ( netcap_trie_t* trie );

void           netcap_trie_destroy ( netcap_trie_t* trie );

void           netcap_trie_raze    ( netcap_trie_t* trie );

void*          netcap_trie_data    ( netcap_trie_t* trie );

void*          netcap_trie_item_data ( netcap_trie_item_t* item );


/* Move element to the front of the LRU */
int netcap_trie_lru_front  ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_lru_update ( netcap_trie_t* trie );

int netcap_trie_lru_config ( netcap_trie_t* trie, int high_water, int low_water, int sieve_size );

#endif /* __NETCAP_TRIE_H_ */
