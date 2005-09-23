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

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>


#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>

#include "netcap_trie.h"
#include "netcap_lru.h"
#include "netcap_interface.h"
#include "netcap_sched.h"

#define HIGH_WATER 768
#define LOW_WATER  512
#define SIEVE_SIZE 16

#define NUM_INSERT_THREADS 3
#define NUM_GET_THREADS    3

struct _basic_test;

typedef int  (_basic_func_t)    ( netcap_trie_t* trie, struct _basic_test* vector );

typedef struct _basic_test
{
    _basic_func_t* exec;
    in_addr_t      ip;
    unsigned int   expected;
    int            line;
} _basic_test_t;

typedef struct 
{
    netcap_trie_t* trie;
    netcap_lru_t* lru;
    volatile int is_alive;
} _lru_thread_t;

typedef struct
{
    struct in_addr ip;
    unsigned int answer;
    netcap_lru_node_t lru_node;
} _lru_trie_data_t;

/* The following functions for the basic test are not thread safe */
static int  _validate_basics( void );
static int  _basic_init ( netcap_trie_item_t* item, in_addr_t ip );
static int  _insert     ( netcap_trie_t* trie, struct _basic_test* vector );
static int  _get        ( netcap_trie_t* trie, struct _basic_test* vector );
static int  _remove     ( netcap_trie_t* trie, struct _basic_test* vector );

/* Threads for testing the LRU */
static int   _validate_lru    ( void );
static int   _lru_init        ( netcap_trie_item_t* item, in_addr_t ip );
static void* _lru_insert      ( void* arg );
static void* _lru_get         ( void* arg );
static void* _lru_lru         ( void* arg );
static int   _lru_remove      ( void* data );
static int   _lru_is_deletable( void* data );

static in_addr_t _get_ip ( unsigned int* seed );
static unsigned long _get_sleep_value( unsigned int* seed );

#define _NFO_ .line __LINE__

static struct
{
    int index;

    netcap_lru_t lru;

    /* Iterate through all of the test vectors */
    netcap_trie_t trie;

    /* Range of IPs to use for the LRU test */
    in_addr_t mask[5];
    _basic_test_t vectors[];
} _test = {
    .mask = {
        0x0000FFF0,
        0x00FF0000,
        0x000FF000,
        0x0000FF10,
        0x0000000F
    },

    .vectors = {
        { .exec _insert, .ip 0x00000000, .expected  4, _NFO_ },
        { .exec _get   , .ip 0x000000FF, .expected  3, _NFO_ },
        { .exec _get   , .ip 0x0000FF00, .expected  2, _NFO_ },
        { .exec _get   , .ip 0x00FF0000, .expected  1, _NFO_ },
        { .exec _get   , .ip 0xFF000000, .expected  0, _NFO_ },
        { .exec _insert, .ip 0x00000000, .expected  4, _NFO_ }, /* insert a node that already exists */
        { .exec _insert, .ip 0x000000FF, .expected  5, _NFO_ }, /* insert a node that splits at the bottom */
        { .exec _remove, .ip 0x0000FF00, .expected  0, _NFO_ }, /* Remove a node doesn't exist */
        { .exec _remove, .ip 0x000000FF, .expected  1, _NFO_ }, /* Remove half of the split */
        { .exec _get   , .ip 0x000000FF, .expected  3, _NFO_ },
        { .exec _insert, .ip 0x0000FF00, .expected  7, _NFO_ }, /* Create some nodes above the split */
        { .exec _insert, .ip 0x00FF0000, .expected 10, _NFO_ },
        { .exec _insert, .ip 0xFF000000, .expected 14, _NFO_ },
        { .exec _remove, .ip 0x00000000, .expected  1, _NFO_ }, /* Remove the other half of the split */
        { .exec _get   , .ip 0x00000000, .expected  2, _NFO_ },
        { .exec _get   , .ip 0x0000FF00, .expected  7, _NFO_ },
        { .exec _get   , .ip 0x00FF0000, .expected 10, _NFO_ },
        { .exec _get   , .ip 0xFFFF0000, .expected 11, _NFO_ },
        /* Create a node where the node was removed */
        { .exec _insert, .ip 0x00000000, .expected 16, _NFO_ },
        { .exec _get   , .ip 0x000000FF, .expected 15, _NFO_ },
        { .exec _remove, .ip 0x00000000, .expected  1, _NFO_ }, /* all remaining nodes */
        { .exec _remove, .ip 0x000000FF, .expected  0, _NFO_ }, /* all remaining nodes */
        { .exec _remove, .ip 0x0000FF00, .expected  1, _NFO_ }, /* all remaining nodes */
        { .exec _remove, .ip 0x00FF0000, .expected  1, _NFO_ }, /* all remaining nodes */
        { .exec _remove, .ip 0xFF000000, .expected  1, _NFO_ }, /* all remaining nodes */
        { .exec NULL }
    }
};

int main ( int argc, char **argv )
{
    int _critical_section( void ) {
        /* See basic operations work ( get, insert and remove), no threads */
        if ( _validate_basics() < 0 ) return errlog( ERR_CRITICAL, "_validate_basics\n" );

        netcap_debug_set_level( 4 );
        debug_set_mylevel( 4 );
        
        if ( _validate_lru() < 0 ) return errlog( ERR_CRITICAL, "_validate_lru\n" );
        return 0;
    }
    
    /* Initialize the mvvm */
    if ( libmvutil_init() < 0 ) {
        perror( "libmvutil_init\n" );
        exit( 1 );
    }
    
    int ret = 0;

    netcap_debug_set_level( 11 );
    debug_set_mylevel( 11 );
    netcap_sched_init();
    
    ret = _critical_section();

    // showMemStats( 1 );

    libmvutil_cleanup();

    return ret;
}

static int   _validate_basics( void )
{
    /* Iterate through all of the test vectors */
    netcap_trie_t trie;

    int _critical_section( void ) {
        int c;
        
        _test.index = 1;

        for( c = 0 ; _test.vectors[c].exec != NULL ; c++ ) {
            if ( _test.vectors[c].exec( &trie, &_test.vectors[c] ) < 0 ) {
                return errlog( ERR_CRITICAL, "Vector %d failed at line number %d\n", c, 
                               _test.vectors[c].line );
            }
        }
        
        return 0;
    }
    
    int ret;
    /* Initialize the trie */
    /* Get the flags right */
    if ( netcap_trie_init( &trie, 0, NULL, 0, _basic_init, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_trie_init\n" );
    }

    ret = _critical_section();
    
    netcap_trie_destroy( &trie );
    
    return ret;
}


static int  _basic_init ( netcap_trie_item_t* item, in_addr_t ip )
{
    item->data = (void*)_test.index++;
    return 0;
}

static int  _insert     ( netcap_trie_t* trie, struct _basic_test* vector )
{
    netcap_trie_line_t line;
    unsigned int answer;
    struct in_addr ip = { .s_addr htonl( vector->ip ) };
    
    /* First do the insert */
    if ( new_netcap_trie_insert_and_get( trie, &ip, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_insert_and_get\n" );
    }
    
    if ( line.count != ( NC_TRIE_DEPTH_TOTAL + 1 )) {
        return errlog( ERR_CRITICAL, "Invalid line count %d\n", line.count );
    }

    if ( line.is_bottom_up ) {
        debug( 7, "_insert: bottom up\n" );
        int c;
        for ( c = line.count ; c-- > 0  ;  ) {
            debug( 7, "_insert: %#010x %d\n", ip.s_addr, line.d[c].base->data );
        }
                   
        answer = (unsigned int)line.d[0].base->data;
    } else {
        debug( 7, "_insert: top down\n" );
        int c;
        for ( c = 0 ; c < line.count ; c++ ) {
            debug( 7, "_insert: %#010x %d\n", ip.s_addr, line.d[c].base->data );
        }
                   
        answer = (unsigned int)line.d[line.count-1].base->data;
    }
    
    if ( answer != vector->expected ) {
        return errlog( ERR_CRITICAL, "Data mismatch %d != %d\n", answer, vector->expected );
    }
    
    /* Perform the get test just to make sure that it was actually inserted properly */
    if ( _get( trie, vector ) < 0 ) return errlog( ERR_CRITICAL, "_get\n" );
    
    return 0;
}

static int  _get        ( netcap_trie_t* trie, struct _basic_test* vector )
{
    netcap_trie_line_t line;
    unsigned int answer;
    struct in_addr ip = { .s_addr htonl( vector->ip ) };

    /* Next to do the get */
    if ( new_netcap_trie_get( trie, &ip, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_get\n" );
    }

    if ( line.is_bottom_up ) return errlog( ERR_CRITICAL, "bottom_up get\n" );
    
    if ( line.count > ( NC_TRIE_DEPTH_TOTAL + 1 )) {
        return errlog( ERR_CRITICAL, "Invalid line count %d\n", line.count );
    }
    
    int c;
    debug( 7, "_get: top down\n" );
    for ( c = 0 ; c < line.count ; c++ ) {
        debug( 7, "_get: %#010x %d\n", ip.s_addr, line.d[c].base->data );
    }
    
    answer = (unsigned int)line.d[line.count - 1].base->data;
    
    if ( answer != vector->expected ) {
        return errlog( ERR_CRITICAL, "Data mismatch %d != %d\n", answer, vector->expected );
    }

    return 0;
}

static int  _remove     ( netcap_trie_t* trie, struct _basic_test* vector )
{
    struct in_addr ip = { .s_addr htonl( vector->ip ) };
    netcap_trie_line_t line;

    /* Remove a node */
    if ( new_netcap_trie_remove( trie, &ip, &line ) < 0 ) {
        return errlog( ERR_CRITICAL, "new_netcap_trie_remove\n" );
    }
    
    if ( !line.is_bottom_up ) return errlog( ERR_CRITICAL, "non bottom_up remove\n" );

    /* You can't remove the root node */
    if ( line.count > ( NC_TRIE_DEPTH_TOTAL )) {
        return errlog( ERR_CRITICAL, "Invalid line count %d\n", line.count );
    }

    if ( line.count > 0 ) {
        if (  !vector->expected ) return errlog( ERR_CRITICAL, "remove shouldn't have removed an item\n" );

        if ( new_netcap_trie_line_destroy( trie, &line ) < 0 ) {
            return errlog( ERR_CRITICAL, "new_netcap_trie_line_destroy\n" );
        }
    } else if (( line.count == 0 ) && vector->expected ) {
        return errlog( ERR_CRITICAL, "remove should have removed an item\n" );
    }
    
    return 0;
}

static int   _validate_lru( void )
{    
    _lru_thread_t data = {
        .trie &_test.trie,
        .lru  &_test.lru,
        .is_alive 1
    };
    
    int _critical_section( void ) {
        pthread_t thread_insert[NUM_INSERT_THREADS];
        pthread_t thread_get[NUM_GET_THREADS];
        pthread_t thread_lru;
        int c;

        for ( c = 0 ; c < NUM_INSERT_THREADS ; c++ ) {
            if ( pthread_create ( &thread_insert[c], NULL, _lru_insert, &data ) < 0 ) {
                return perrlog("pthread_create");
            }
        }
        
        for ( c = 0 ; c < NUM_GET_THREADS ; c++ ) {
            if ( pthread_create ( &thread_get[c], NULL, _lru_get, &data ) < 0 ) {
                return perrlog("pthread_create");
            }
        }
        
        if ( pthread_create ( &thread_lru, NULL, _lru_lru, &data ) < 0 ) return perrlog("pthread_create");

        sleep ( 30 );

        data.is_alive = 0;
        for ( c = 0 ; c < NUM_INSERT_THREADS ; c++ ) {
            if ( pthread_join( thread_insert[c], NULL ) < 0 ) return perrlog( "pthread_join" );
        }
        
        for ( c = 0 ; c < NUM_GET_THREADS ; c++ ) {
            if ( pthread_join( thread_get[c], NULL ) < 0 ) return perrlog( "pthread_join" );
        }

        if ( pthread_join( thread_lru, NULL ) < 0 ) return perrlog( "pthread_join" );

        return 0;
    }
    
    int ret;
    /* Initialize the trie */
    /* Get the flags right */
    if ( netcap_trie_init( &_test.trie, 0, NULL, 0, _lru_init, NULL, NULL, 0, 0 ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_trie_init\n" );
    }
    
    /* setup the root node */
    _test.trie.root.base.data = calloc( 1, sizeof( _lru_trie_data_t ));
    if ( _test.trie.root.base.data == NULL ) { errlog( ERR_CRITICAL, "malloc\n" ); exit( -1 ); }
    

    if ( netcap_lru_init( &_test.lru, HIGH_WATER, LOW_WATER, SIEVE_SIZE, _lru_is_deletable, 
                          _lru_remove, &_test.trie.mutex ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_lru_init\n" );
    }

    ret = _critical_section();
    
    netcap_trie_destroy( &_test.trie );
    
    return ret;

}

static int _get_value( in_addr_t ip, int depth )
{
    unsigned int mask = 0;
    switch( depth ) {
    case 4: mask += 0xFF000000;
    case 3: mask += 0x00FF0000;
    case 2: mask += 0x0000FF00;
    case 1: mask += 0x000000FF;
        /* nothing */
    }

    return mask & ip;
}

static int   _lru_init ( netcap_trie_item_t* item, in_addr_t ip )
{
    _lru_trie_data_t* data = malloc( sizeof( _lru_trie_data_t ));
    if ( data == NULL ) {
        errlog( ERR_CRITICAL, "malloc\n" );
        exit( -1 );
    }

    data->answer    = _get_value( ip, item->depth );
    data->ip.s_addr = ip;
    bzero( &data->lru_node, sizeof( data->lru_node ));
    if (( NC_TRIE_DEPTH_TOTAL == item->depth ) && 
        ( netcap_lru_add( &_test.lru, &data->lru_node, item ) < 0 )) {
        errlog( ERR_CRITICAL, "netcap_lru_add\n" );
        exit( -1 );
    }

    item->data = data;

    debug_nodate( 0, "'[%d]%#010x'\n", item->depth, data->answer );
    
    return 0;
}

static void* _lru_insert  ( void* arg )
{
    _lru_thread_t* data = arg;

    netcap_trie_t* trie = data->trie;
    unsigned int seed = 0xABCDEF01;
    netcap_lru_t* lru = data->lru;  /* Used the LRU to move to the front */
    netcap_trie_line_t line;
    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ));
        struct in_addr ip = { .s_addr _get_ip( &seed ) };
        if ( new_netcap_trie_insert_and_get( trie, &ip, &line ) < 0 ) {
            errlog( ERR_CRITICAL, "new_netcap_trie_insert_and_get\n" );
            exit( -1 );
        }

        if ( line.count != ( NC_TRIE_DEPTH_TOTAL + 1 )) {
           errlog( ERR_CRITICAL, "Invalid line count %d\n", line.count );
           exit( -1 );
        }
        
        int c;
        for ( c = line.count ; c-- > 0  ;  ) {
            int depth = line.d[c].base->depth;
            unsigned int answer   =  ((_lru_trie_data_t*)line.d[c].base->data)->answer;
            unsigned int expected = _get_value( ip.s_addr, depth  );
            if ( answer != expected ) {
                errlog( ERR_CRITICAL, "Data mismatch, done %#010x != %#010x, %d\n", 
                        expected, answer, line.d[c].base->depth );
                exit( -1 );
            }
        }
    }
        
    return NULL;
}

static void* _lru_get     ( void* arg )
{
    _lru_thread_t* data = arg;

    netcap_trie_t* trie = data->trie;
    unsigned int seed = 0x73C21C8D;
    netcap_trie_line_t line;
    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ));
        struct in_addr ip = { .s_addr _get_ip( &seed ) };
        if ( new_netcap_trie_get( trie, &ip, &line ) < 0 ) {
            errlog( ERR_CRITICAL, "new_netcap_trie_insert_and_get\n" );
            exit( -1 );
        }

        if ( line.count > NC_TRIE_DEPTH_TOTAL + 1 ) {
            errlog( ERR_CRITICAL, "Invalid line count %d\n", line.count );
            exit( -1 );
        }
        
        int c;
        for ( c = line.count ; c-- > 0  ;  ) {
            int depth = line.d[c].base->depth;
            unsigned int answer   =  ((_lru_trie_data_t*)line.d[c].base->data)->answer;
            unsigned int expected = _get_value( ip.s_addr, depth  );
            if ( answer != expected ) {
                errlog( ERR_CRITICAL, "Data mismatch, done %#010x != %#010x, %d\n", expected, answer, line.d[c].base->depth );
                exit( -1 );
            }
        }
    }
        
    return NULL;
}

static void* _lru_lru     ( void* arg )
{
    _lru_thread_t* data = arg;

    // netcap_trie_t* trie = data->trie;
    netcap_lru_t* lru = data->lru;
    unsigned int seed = 0x19C8603B;
    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ) * 1024 );
        
        if ( netcap_lru_cut( lru, NULL, 0 ) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_lru_cut\n" );
            exit( -1 );
        }
    }
        
    return NULL;
}

static int _lru_remove ( void* arg )
{
    netcap_trie_item_t* trie_item = (netcap_trie_item_t*)arg;
    
    _lru_trie_data_t* data = trie_item->data;
    netcap_trie_line_t* line = malloc( sizeof( netcap_trie_line_t ));
    
    if ( line == NULL ) {
        errlog( ERR_CRITICAL, "malloc\n" );
        exit( -1 );
    }

    /* Just remove the item from the trie */
    if ( new_netcap_trie_remove( &_test.trie, &data->ip, line ) < 0 ) {
        errlog( ERR_CRITICAL, "new_netcap_trie_remove\n" );
        exit( -1 );
    }
    
    /* XXXXX Must free the stuff with the scheduler */
    
    return 0;
}

static int   _lru_is_deletable( void* data )
{
    return 0;
}


static in_addr_t _get_ip ( unsigned int* seed )
{
    in_addr_t ip = ( rand_r( seed ) & 0xFFFF ) | (( rand_r( seed ) << 16 ) & 0xFFFF0000 );
    
    /* This must match the size of the masks in the global structure */
    int j=(int)(5.0*rand_r( seed )/(RAND_MAX+1.0));
    
    /* Only get the necessary bits */
    return ( ip & htonl( _test.mask[j] ));
}

static unsigned long _get_sleep_value( unsigned int* seed ) {
    return (unsigned long)( rand_r( seed ) & 0xFF );
}


