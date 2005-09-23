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
#include "netcap_interface.h"
#include "netcap_sched.h"

/* These are threads for the LRU test */
#if 0
static void* _thread_insert  ( void* arg );
static void* _thread_get     ( void* arg );
static void* _thread_remove  ( void* arg );
#endif

static int   _validate_basics( void );

struct _basic_test;

typedef int  (_basic_func_t)    ( netcap_trie_t* trie, struct _basic_test* vector );

typedef struct _basic_test
{
    _basic_func_t* exec;
    in_addr_t      ip;
    unsigned int   expected;
    int            line;
} _basic_test_t;

/* The following functions for the basic test are not thread safe */
static int  _basic_init ( netcap_trie_item_t* item, in_addr_t ip );
static int  _insert     ( netcap_trie_t* trie, struct _basic_test* vector );
static int  _get        ( netcap_trie_t* trie, struct _basic_test* vector );
static int  _remove     ( netcap_trie_t* trie, struct _basic_test* vector );

#define _NFO_ .line __LINE__

static struct
{
    int index;
    _basic_test_t vectors[];
} _test = {
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

    showMemStats( 1 );

    libmvutil_cleanup();

    return ret;
}

#if 0
static void* _thread_insert  ( void* arg )
{
    return NULL;
}

static void* _thread_get     ( void* arg )
{
    return NULL;
}

static void* _thread_remove  ( void* arg )
{
    return NULL;
}

#endif

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

