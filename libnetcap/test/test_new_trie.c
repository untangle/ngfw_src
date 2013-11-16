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

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>

#include <sys/time.h>
#include <time.h>

#include <mvutil/libmvutil.h>
#include <mvutil/errlog.h>
#include <mvutil/debug.h>
#include <mvutil/utime.h>

#include "netcap_trie.h"
#include "netcap_lru.h"
#include "netcap_interface.h"
#include "netcap_sched.h"

#define HIGH_WATER 832
#define LOW_WATER  512
#define SIEVE_SIZE 48

#define NUM_INSERT_THREADS 10
#define NUM_GET_THREADS    10

#define _LRU_TRASH_DELAY   SEC_TO_USEC( 1 )

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
    netcap_lru_t*  lru;
    int            mask_index;  /* Zero if unusued, otherwise the index this thread should use */
    volatile int   is_alive;
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
static void  _lru_empty_trash ( void* arg );
static int   _lru_add_trash   ( netcap_trie_line_t* line );

static in_addr_t _get_ip ( unsigned int* seed, int mask_index );
static unsigned long _get_sleep_value( unsigned int* seed );

#define TRASH_SIZE   1024

#define _NFO_ .line __LINE__

typedef struct {
    int size;
    netcap_trie_line_t* bucket[TRASH_SIZE];
} _lru_trash_bin_t;

static struct
{
    int index;

    netcap_lru_t lru;

    /* Iterate through all of the test vectors */
    netcap_trie_t trie;

    /* Range of IPs to use for the LRU test */
    in_addr_t mask[6];

    /* Trash bin */
    _lru_trash_bin_t* bin;

    /* Trie/LRU mutex */
    pthread_mutex_t mutex;

    _basic_test_t vectors[];
} _test = {
    .bin  = NULL,
    
    .mask = {
        0x0000FFF0,
        0x00FF0000,
        0x000FF000,
        0x0000FF10,
        0x0000000F,
        0x01010103, /* Random values that overlap, a lot */
    },
    
    .mutex = PTHREAD_MUTEX_INITIALIZER,

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
    pthread_t thread_sched;
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

    if ( pthread_create ( &thread_sched, NULL, netcap_sched_donate, NULL ) < 0 ) {
        return perrlog("pthread_create");
    }
    
    ret = _critical_section();

    // showMemStats( 1 );

    libmvutil_cleanup();

    if ( netcap_sched_cleanup_z ( NULL ) < 0 ) ret -= errlog ( ERR_CRITICAL, "netcap_sched_cleanup_z\n" );

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
    if ( netcap_trie_init( &trie, 0, NULL, 0, _basic_init, NULL ) < 0 ) {
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
    if ( new_netcap_trie_insert_and_get( trie, &ip, NULL, &line ) < 0 ) {
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
    if ( new_netcap_trie_remove( trie, &ip, NULL, &line ) < 0 ) {
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
        .trie       &_test.trie,
        .lru        &_test.lru,
        .is_alive   1,
        .mask_index 0
    };

    _lru_thread_t overlap_data = {
        .trie       &_test.trie,
        .lru        &_test.lru,
        .is_alive   1,
        .mask_index 5
    };

    
    int _critical_section( void ) {
        pthread_t thread_insert[NUM_INSERT_THREADS];
        pthread_t thread_get[NUM_GET_THREADS];
        pthread_t thread_lru;
        int c;

        for ( c = 0 ; c < NUM_INSERT_THREADS ; c++ ) {
            _lru_thread_t* _data = ( c < 2 ) ? &overlap_data : &data;
            if ( pthread_create ( &thread_insert[c], NULL, _lru_insert, _data ) < 0 ) {
                return perrlog("pthread_create");
            }
        }
        
        for ( c = 0 ; c < NUM_GET_THREADS ; c++ ) {
            _lru_thread_t* _data = ( c < 2 ) ? &overlap_data : &data;
            if ( pthread_create ( &thread_get[c], NULL, _lru_get, _data ) < 0 ) {
                return perrlog("pthread_create");
            }
        }
        
        if ( pthread_create ( &thread_lru, NULL, _lru_lru, &data ) < 0 ) return perrlog("pthread_create");

        /* The first part is where there is the greatest potential for error */
        sleep ( 600 );

        data.is_alive = 0;
        overlap_data.is_alive = 0;
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
    if ( netcap_trie_init( &_test.trie, 0, NULL, 0, _lru_init, NULL ) < 0 ) {
        return errlog( ERR_CRITICAL, "netcap_trie_init\n" );
    }
    
    /* setup the root node */
    _test.trie.root.base.data = calloc( 1, sizeof( _lru_trie_data_t ));
    if ( _test.trie.root.base.data == NULL ) { errlog( ERR_CRITICAL, "malloc\n" ); exit( -1 ); }
    

    if ( netcap_lru_init( &_test.lru, HIGH_WATER, LOW_WATER, SIEVE_SIZE, _lru_is_deletable, 
                          _lru_remove ) < 0 ) {
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
        ( netcap_lru_add( &_test.lru, &data->lru_node, item, NULL ) < 0 )) {
        errlog( ERR_CRITICAL, "netcap_lru_add\n" );
        exit( -1 );
    }

    item->data = data;

    debug_nodate( 11, "'[%d]%#010x'\n", item->depth, data->answer );
    
    return 0;
}

static void* _lru_insert  ( void* arg )
{
    _lru_thread_t* data = arg;

    netcap_trie_t* trie = data->trie;
    unsigned int seed = 0xABCDEF01;
    netcap_lru_t* lru = data->lru;  /* Used the LRU to move to the front */
    netcap_trie_line_t line;
    struct timeval tv;

    gettimeofday( &tv, NULL );
    seed += tv.tv_usec;

    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ));
        struct in_addr ip = { .s_addr _get_ip( &seed, data->mask_index ) };
        if ( new_netcap_trie_insert_and_get( trie, &ip, &_test.mutex, &line ) < 0 ) {
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
        
        /* Possibly move to the front of the lru */
        /* ( 1 in 16 chance ) */
        if ( rand_r( &seed ) < ( RAND_MAX >> 4 ) && line.is_bottom_up ) {
            _lru_trie_data_t* node_data = line.d[0].base->data;
            if ( netcap_lru_move_front( lru, &node_data->lru_node, &_test.mutex ) <  0 ) {
                errlog( ERR_CRITICAL, "netcap_lru_move_front\n" );
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
    struct timeval tv;

    unsigned int seed = 0x73C21C8D;

    netcap_trie_line_t line;

    gettimeofday( &tv, NULL );
    seed += tv.tv_usec;
    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ));
        struct in_addr ip = { .s_addr _get_ip( &seed, data->mask_index ) };
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
                errlog( ERR_CRITICAL, "Data mismatch, done %#010x != %#010x, %d\n", expected, answer, 
                        line.d[c].base->depth );
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

    struct timeval tv;

    gettimeofday( &tv, NULL );
    seed += tv.tv_usec;

    
    while ( data->is_alive ) {
        usleep( _get_sleep_value( &seed ) * 1024 );
        
        if ( netcap_lru_cut( lru, NULL, 0, &_test.mutex ) < 0 ) {
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

    /* Just remove the item from the trie, Don't use the mutex because it is
     * already locked by the LRU. */
    if ( new_netcap_trie_remove( &_test.trie, &data->ip, NULL, line ) < 0 ) {
        errlog( ERR_CRITICAL, "new_netcap_trie_remove\n" );
        exit( -1 );
    }
    
    /* XXX Probably want to accumulate a bunch of stuff, but for the test program who cares */
    _lru_add_trash( line );
    
    return 0;
}

static int   _lru_add_trash   ( netcap_trie_line_t* line )
{
    _lru_trash_bin_t* new_bin = NULL;

    if ( _test.bin == NULL || _test.bin->size >= TRASH_SIZE ) {
        if (( new_bin = calloc( 1, sizeof( _lru_trash_bin_t ))) == NULL ) {
            perrlog( "calloc" );
            exit( -1 );
        }
    }
    
    /* If necessary, schedule the trash to be taken out */
    if ( new_bin != NULL ) {
        if ( _test.bin != NULL && netcap_sched_event( _lru_empty_trash, _test.bin, _LRU_TRASH_DELAY ) < 0 ) {
            errlog( ERR_CRITICAL, "netcap_sched_event\n" );
            exit( -1 );
        }
        _test.bin = new_bin;
    }
    
    /* Append the item to the trash bin */
    _test.bin->bucket[_test.bin->size++] = line;

    return 0;
}

static int   _lru_is_deletable( void* data )
{
    return 0;
}


static in_addr_t _get_ip ( unsigned int* seed, int mask_index )
{
    in_addr_t ip = ( rand_r( seed ) & 0xFFFF ) | (( rand_r( seed ) << 16 ) & 0xFFFF0000 );
    
    /* This must match the size of the masks in the global structure */
    int j;
    unsigned int base;

    if ( 0 == mask_index ) {
        j = (int)(5.0*rand_r( seed )/(RAND_MAX+1.0));
        base = 0;
    } else {
        j = mask_index;
        base = htonl( 0x80000000 );
    }

    /* Only get the necessary bits */
    return (( ip & htonl( _test.mask[j] )) | base );
}

static unsigned long _get_sleep_value( unsigned int* seed ) {
    return (unsigned long)( rand_r( seed ) & 0xFF );
}

static void _lru_empty_trash ( void* arg )
{
    _lru_trash_bin_t* bin = arg;
    
    int c;

    debug( 0, "EMPTY TRASH %d\n", bin->size );
    if ( bin->size > TRASH_SIZE ) {
        errlog( ERR_CRITICAL, "trash bin is too large %d\n", bin->size );
        bin->size = TRASH_SIZE;
    }
    
    for ( c = 0 ; c < bin->size ; c++ ) {
        if ( new_netcap_trie_line_raze( &_test.trie, bin->bucket[c] ) < 0 ) {
            errlog( ERR_CRITICAL, "new_netcap_trie_line_raze\n" );
        }
        bin->bucket[c] = NULL;
    }

    bin->size = 0;
    free( bin );
}



