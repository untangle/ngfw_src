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


#include <stdlib.h>
#include <unistd.h>
#include <semaphore.h>
#include <pthread.h>

#include <time.h>
#include <sys/time.h>
#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>

#include "netcap_lru.h"

#define _LRU_MIN_LENGTH 4

#define _LRU_DEBUG_HIGH 11
#define _LRU_DEBUG_LOW  7

#define _CHECK_AND_LOCK( mutex ) \
  if (((mutex) != NULL ) && ( pthread_mutex_lock((mutex)) < 0 )) return perrlog( "pthread_mutex_lock\n" )

#define _CHECK_AND_UNLOCK( mutex ) \
  if (((mutex) != NULL ) && ( pthread_mutex_unlock((mutex)) < 0 )) return perrlog( "pthread_mutex_unlock\n" )

static int _lru_remove( netcap_lru_t* lru, netcap_lru_node_t* node );

static __inline__ int _validate_args( int high_water, int low_water, int sieve_size )
{
    if (( high_water < 0 ) || ( low_water < 0 )) return errlogargs();

    if ( sieve_size < 0 ) return errlogargs();
    
    if ( high_water < low_water ) return errlogargs();

    return 0;
}

static __inline__ int _validate_lru( netcap_lru_t* lru )
{
    if ( _validate_args( lru->high_water, lru->low_water, lru->sieve_size ) < 0 ) {
        return errlog( ERR_CRITICAL, "_validate_args\n" );
    }

    if ( lru->remove == NULL ) return errlogargs();

    return 0;
}


int netcap_lru_init( netcap_lru_t* lru, int high_water, int low_water, int sieve_size, 
                     netcap_lru_check_t* is_deletable, netcap_lru_remove_t* remove )
{
    if ( lru == NULL || remove == NULL ) return errlogargs();

    if ( is_deletable == NULL ) errlog( ERR_WARNING, "NULL is_deletable, unable to sift old nodes\n" );

    if ( _validate_args( high_water, low_water, sieve_size ) < 0 ) {
        return errlog( ERR_CRITICAL, "_validate_args\n" );
    }

    /* Initialize the two lists maintained by the LRU */
    if (( list_init( &lru->lru_list, 0 ) < 0 ) || ( list_init( &lru->permanent_list, 0 ) < 0 )) {
        return errlog( ERR_CRITICAL, "list_init\n" );
    }

    lru->length       = 0;
    lru->high_water   = high_water;
    lru->low_water    = low_water;
    lru->sieve_size   = sieve_size;
    lru->is_deletable = is_deletable;
    lru->remove       = remove;
         
    return 0;
}

int netcap_lru_config( netcap_lru_t* lru, int high_water, int low_water, int sieve_size, 
                       pthread_mutex_t* mutex )
{
    if ( lru == NULL ) return errlogargs();
    
    if ( _validate_args( high_water, low_water, sieve_size ) < 0 ) {
        return errlog( ERR_CRITICAL, "_validate_args\n" );
    }

    /* Get the mutex lock */
    _CHECK_AND_LOCK( mutex );
    
    lru->low_water  = low_water;
    lru->high_water = high_water;
    lru->sieve_size = sieve_size;

    _CHECK_AND_UNLOCK( mutex );
    
    return 0;
}

/* Add a node to the front of the LRU, node is updated to contain the necessary information for the LRU */
int netcap_lru_add( netcap_lru_t* lru, netcap_lru_node_t* node, void* data, pthread_mutex_t* mutex )
{
    if ( lru == NULL || node == NULL || data == NULL ) return errlogargs();

    _validate_lru( lru );
    
    int _critical_section( void ) {
        if (( node->state == _LRU_PERMANENT ) && ( node->list_node != NULL )) {
            return errlog( ERR_CRITICAL, "Node is on the permanent list, cannot be added to LRU\n" );
        }
        
        if ( node->state == _LRU_REMOVED ) {
            return errlog( ERR_CRITICAL, "Node has already been removed, cannot be added to LRU\n" );
        }

        node->state     = _LRU_READY;
        node->data      = data;
        if (( node->list_node = list_add_head( &lru->lru_list, node )) == NULL ) {
            return errlog( ERR_CRITICAL, "list_add_head\n" );
        }
        return 0;
    }
    
    int ret;
    
    _CHECK_AND_LOCK( mutex );
    ret = _critical_section();
    _CHECK_AND_UNLOCK( mutex );

    return ret;
}

int netcap_lru_permanent_add( netcap_lru_t* lru, netcap_lru_node_t* node, void* data, 
                              pthread_mutex_t* mutex  )
{
    if ( lru == NULL || node == NULL || data == NULL ) return errlogargs();

    if ( _validate_lru( lru ) < 0 ) return errlog( ERR_CRITICAL, "_validate_lru\n" );
    
    int _critical_section( void ) {
        if ( node->list_node != NULL ) {
            if ( data != node->data ) {
                errlog( ERR_WARNING, "Data mismatch %#10x != %#10x, could be harmless\n", data, node->data );
            }
            switch( node->state ) {
            case _LRU_READY:
                /* Must remove this node */
                /* Remove the node from the list */
                if ( list_remove( &lru->lru_list, node->list_node ) < 0 ) perrlog( "list_remove\n" );
                
                /* Null out the list node */
                node->list_node = NULL; 
                break;
                
            case _LRU_PERMANENT:
                /* Already there, nothing left to do */
                errlog( ERR_WARNING, "Node[%#10x] was already on the permanent list\n", data );
                return 0;

            case _LRU_REMOVED:
                errlog( ERR_CRITICAL, "Node[%#10x] already removed, cannot put on permanent list\n", data );
                return 0;

            default:
                debug( _LRU_DEBUG_HIGH, "LRU: Node[%#10x] being added to permanent list\n", data );
            }
        }

        if (( node->list_node = list_add_head( &lru->permanent_list, node )) == NULL ) {
            return errlog( ERR_CRITICAL, "list_add_head\n" );
        }
        
        node->state = _LRU_PERMANENT;
        return 0;
    }

    int ret;

    _CHECK_AND_LOCK( mutex );
    ret = _critical_section();
    _CHECK_AND_UNLOCK( mutex );
    
    return ret;    
}

/* Remove all of the nodes on the permanent list and add them to the LRU */
int netcap_lru_permanent_clear( netcap_lru_t* lru, pthread_mutex_t* mutex )
{
    if ( lru == NULL ) return errlogargs();
    
    if ( _validate_lru( lru ) < 0 ) return errlog( ERR_CRITICAL, "_validate_lru\n" );

    int _critical_section( void ) {
        int length = 0;

        if (( length = list_length ( &lru->permanent_list )) < 0 ) {
            return errlog(ERR_CRITICAL,"list_length\n");
        }

        while ( length-- > 0 ) {
            netcap_lru_node_t* node;
            
            node = NULL;
            /* Move the node off the permanent */
            if (( list_pop_head( &lru->permanent_list, (void**)&node ) < 0 ) || node == NULL ) {
                errlog( ERR_CRITICAL, "list_pop_head\n" );
                continue;
            }
            
            if ( node->state != _LRU_PERMANENT ) errlog( ERR_WARNING, "Node not in the permanent state\n" );

            /* Move onto the LRU */
            node->state = _LRU_READY;
            if (( node->list_node = list_add_head( &lru->lru_list, node )) == NULL ) {
                return errlog( ERR_CRITICAL, "list_add_head\n" );
            }
        }
        
        return 0;
    }

    int ret;

    _CHECK_AND_LOCK( mutex );
    ret = _critical_section();
    _CHECK_AND_UNLOCK( mutex );
    
    return ret;    
}

/* Move a node to the front of the LRU, node should be a value returned from a previous
 * execution of lru_add */
int netcap_lru_move_front( netcap_lru_t* lru, netcap_lru_node_t* node, pthread_mutex_t* mutex )
{
    if ( lru == NULL || node == NULL ) return errlogargs();

    _validate_lru( lru );
    
    int _critical_section( void ) {
        /* Node is not ready, can't use it yet anymore, check before locking the
         * mutex to avoid unecessary locks, check after because the value can change
         * only when you have the mutex. */
        if ( node->state != _LRU_READY ) {
            debug( _LRU_DEBUG_HIGH, "LRU: Node has already been removed\n" );
            return 0;
        }
        
        /* Move the node to the front of the list */
        if ( list_move_head( &lru->lru_list, &node->list_node, node ) < 0 ) {
            return perrlog( "list_move_head\n" );
        }
        return 0;
    }

    int ret = 0;

    /* Node is not ready, can't use it yet anymore */
    if ( node->state != _LRU_READY ) {
        debug( _LRU_DEBUG_HIGH, "LRU: Node has already been removed\n" );
        return 0;
    }
    
    _CHECK_AND_LOCK( mutex );
    ret = _critical_section();
    _CHECK_AND_UNLOCK( mutex );
    
    return ret;
}


/* Cut any excessive nodes, and place the results into the node_array */
int netcap_lru_cut( netcap_lru_t* lru, netcap_lru_node_t** node_array, int node_array_size, 
                    pthread_mutex_t* mutex )
{
    if ( lru == NULL || node_array_size < 0 ) return errlogargs();
    
    int _critical_section( void ) {
        int c;
        int length = 0;
        int node_count = 0;

        if (( length = list_length ( &lru->lru_list )) < 0 ) return errlog(ERR_CRITICAL,"list_length\n");

        debug( _LRU_DEBUG_LOW, "LRU: Update - length %d items\n", length );

        /* Make sure the LRU doesn't get too short */
        if ( length < _LRU_MIN_LENGTH ) return 0;
        
        if ( length < lru->high_water ) {
            /* No check function, no way to sift nodes */
            if ( lru->is_deletable == NULL ) {
                return 0;
            }
            
            /* Leave at most _LRU_MIN_LENGTH nodes on the LRU */
            c = length - _LRU_MIN_LENGTH;
            
            /* Max it out at the size of the sieve */
            c = ( c < lru->sieve_size ) ? c : lru->sieve_size;

            debug( _LRU_DEBUG_HIGH, "LRU: Update - sifting %d items\n", c );
            
            for ( ; c-- > 0 ; ) {
                int is_deletable;
                netcap_lru_node_t* node;

                if (( node = list_tail_val( &lru->lru_list )) == NULL ) {
                    return errlog( ERR_CRITICAL, "list_tail_val\n" );
                }
                
                if (( is_deletable = lru->is_deletable( node->data )) < 0 ) {
                    return errlog( ERR_CRITICAL, "lru->is_deletable\n" );
                }
                
                if ( NC_LRU_DONT_DELETE == is_deletable ) {
                    debug( _LRU_DEBUG_LOW, "LRU: Non-deletable node, continuing to sift\n" );
                } else {
                    if ( _lru_remove( lru, node ) < 0 ) return errlog( ERR_CRITICAL, "_lru_remove\n" );
                    if (( node_array != NULL ) && ( node_count < node_array_size )) {
                        node_array[node_count++] = node;
                    } else node_count++;
                }
            }
        } else {
            /* XXX May need to check for is_deletable */
            c = length - lru->low_water;
            debug ( _LRU_DEBUG_LOW, "TRIE: LRU Update - cutting %d items\n", c );
            
            for (  ; c-- > 0 ; ) {
                netcap_lru_node_t* node;
                
                if (( node = list_tail_val( &lru->lru_list )) == NULL ) {
                    return errlog( ERR_CRITICAL, "list_tail_val\n" ); 
                }
                
                if ( _lru_remove( lru, node ) < 0 ) return errlog( ERR_CRITICAL, "_lru_remove\n" );
                
                if (( node_array != NULL ) &&  ( node_count < node_array_size )) {
                    node_array[node_count++] = node;
                } else node_count++;
            }
        }

        return node_count;
    }

    if ( _validate_lru( lru ) < 0 ) return errlog( ERR_CRITICAL, "_validate_lru\n" );
    
    if ( node_array != NULL ) bzero( node_array, node_array_size );
    
    /* convert from bytes to words */
    node_array_size = node_array_size / sizeof( netcap_lru_node_t* );

    int ret = 0;
    
    _CHECK_AND_LOCK( mutex );
    ret = _critical_section();
    _CHECK_AND_UNLOCK( mutex );
    
    return ret;
}

static int _lru_remove( netcap_lru_t* lru, netcap_lru_node_t* node )
{
    list_node_t* list_node = node->list_node;
    
    if (( NULL == list_node ) || ( node->state != _LRU_READY )) {
        return errlog( ERR_CRITICAL, "Item is not on LRU or LRU is corrupted\n" );
    }

    /* indicate that the state is removed */
    node->state = _LRU_REMOVED;

    /* Call the removal function on the data */
    if (( lru->remove != NULL ) && ( lru->remove( node->data ) < 0 )) errlog( ERR_CRITICAL, "lru->remove\n" );
    
    /* Actually remove the node from the list */
    if ( list_remove( &lru->lru_list, list_node ) < 0 ) perrlog( "list_remove\n" );

    /* Null out the list node */
    node->list_node = NULL;

    return 0;
}
