/**
 * $Id: netcap_trie.h 35571 2013-08-08 18:37:27Z dmorris $
 */
#ifndef __NETCAP_TRIE_H_
#define __NETCAP_TRIE_H_

#include <netinet/in.h>
#include <time.h>

#include <mvutil/list.h>
#include <mvutil/mailbox.h>
#include <mvutil/hash.h>

#define NC_TRIE_DEPTH_TOTAL 4
#define NC_TRIE_LINE_COUNT_MAX ( NC_TRIE_DEPTH_TOTAL + 1 )
#define NC_TRIE_DEBUG_LOW   7
#define NC_TRIE_DEBUG_HIGH  11
#define NC_TRIE_ROW_SIZE    256

/* Just some random values to make it a little harder to accidentally free a random block of memory */
#define NC_TRIE_BASE_LEVEL  0xCB
#define NC_TRIE_BASE_ITEM   0xAC

/* This assumes all IPs are in Network Byte order */

typedef enum {
    NC_TRIE_INHERIT = 1,  /* Inherit the data from parents to children. */
    NC_TRIE_COPY    = 2,  /* Make a copy of the data when inheriting from parents to children */
    NC_TRIE_FREE    = 4   /* Free any data allocated, this is not done in remove, but in line_raze */
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

    /* An item at each level */
    void* data;

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

    /* Number of direct descendents */
    int count;
    
    /* estimated total number of terminal child nodes for this node */
    int num_children;

    netcap_trie_element_t r[NC_TRIE_ROW_SIZE];
} netcap_trie_level_t;

typedef int  (netcap_trie_init_t)    ( netcap_trie_item_t* item, struct in_addr* ip );
typedef void (netcap_trie_destroy_t) ( netcap_trie_item_t* item );

/* The new functions */
typedef struct
{
    /* 1 if this line starts at the bottom, 0 otherwise */
    unsigned char is_bottom_up;

    /* Number of nodes the in this line */
    unsigned char count;
    
    /* Up to depth total + 1 for the root. items */
    netcap_trie_element_t d[NC_TRIE_DEPTH_TOTAL + 1];
} netcap_trie_line_t;

typedef struct {
    int   mem;        /* Total amount of memory used by this trie */
    int   flags;
    int   item_count; /* Total number of items in the trie */

    netcap_trie_level_t root;

    /* When using the NC_TRIE_COPY flag, all items must be this size */
    int   item_size;

    /* An init function, called on the item, after each new item is created */
    netcap_trie_init_t* init;
    
    /* Destroy function, called on the item, before being destroyed */
    netcap_trie_destroy_t* destroy;

    /* Hash (ip->item) of all of the terminal nodes */
    ht_t   ip_element_table;
} netcap_trie_t;

netcap_trie_t* netcap_trie_malloc  ( void );


int            netcap_trie_init    ( netcap_trie_t* trie, int flags, void* item, int item_size, 
                                     netcap_trie_init_t* init, netcap_trie_destroy_t* destroy );

netcap_trie_t* netcap_trie_create  ( int flags, void* item, int item_size, 
                                     netcap_trie_init_t* init, netcap_trie_destroy_t* destroy );

void           netcap_trie_free    ( netcap_trie_t* trie );

void           netcap_trie_destroy ( netcap_trie_t* trie );

void           netcap_trie_raze    ( netcap_trie_t* trie );

void*          netcap_trie_data    ( netcap_trie_t* trie );

void*          netcap_trie_item_data ( netcap_trie_item_t* item );

/* For all of these functions mutex is passed in, if it is non-null it is used, otherwise it is not */
/* Get the closest item to ip, this never creates a new item, never blocks */
int new_netcap_trie_get            ( netcap_trie_t* trie, struct in_addr* ip, netcap_trie_line_t* line );

/* Insert an item into at ip, and the fill the line with item, if the
 * item is already in the trie, this just returns its line. may use mutex. */
int new_netcap_trie_insert_and_get ( netcap_trie_t* trie, struct in_addr* ip, pthread_mutex_t* mutex,
                                     netcap_trie_line_t* line );

/* Remove an item from the trie and return the memory it used in <line>.
 *   Depending on the application, this memory shouldn't be freed immediately
 *   as it may be used by other threads. always grabs mutex.
 */
int new_netcap_trie_remove         ( netcap_trie_t* trie, struct in_addr* ip, pthread_mutex_t* mutex,
                                     netcap_trie_line_t* line );

/* Raze a line that has been removed from the trie */
int new_netcap_trie_line_free      ( netcap_trie_line_t* line );
int new_netcap_trie_line_destroy   ( netcap_trie_t* trie, netcap_trie_line_t* line );
int new_netcap_trie_line_raze      ( netcap_trie_t* trie, netcap_trie_line_t* line );


/* Move element to the front of the LRU */
// int netcap_trie_lru_front  ( netcap_trie_t* trie, netcap_trie_element_t element );

// int netcap_trie_lru_update ( netcap_trie_t* trie );

// int netcap_trie_lru_config ( netcap_trie_t* trie, int high_water, int low_water, int sieve_size );

#endif /* __NETCAP_TRIE_H_ */
