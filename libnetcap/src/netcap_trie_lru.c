/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_trie_lru.c,v 1.1 2004/11/09 19:40:00 dmorris Exp $
 */

#include <stdlib.h>
#include <unistd.h>
#include <semaphore.h>
#include <time.h>
#include <sys/time.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>
#include <mvutil/mailbox.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"
#include "netcap_sched.h"

#define _LRU_TRASH_EMPTY_DELAY 2
#define SEC_TO_USEC(sec)   ((sec)  * 1000000)

typedef struct {
    netcap_trie_t* trie;
    netcap_trie_element_t element;
} _trash_t;

#define _verify_lru_is_set(trie)  if (!((trie)->flags&NC_TRIE_LRU)) \
        return errlog(ERR_CRITICAL,"TRIE: LRU flag not set\n")

#define _verify_lru_is_set_null(trie)  if (!((trie)->flags&NC_TRIE_LRU)) \
        return errlog_null(ERR_CRITICAL,"TRIE: LRU flag not set\n")

static void _delete ( void *trash );

static void _empty  ( void* sem_empty );

int netcap_trie_lru_add ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    int ret=0;

    if ( trie == NULL || element.base == NULL ) return errlogargs();

    _verify_lru_is_set( trie );

    /* Add the element to the LRU */
    if ( pthread_mutex_lock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_lock\n");
    }
    
    /* Only add the node if it is not on the LRU and the ready flag is set */
    if ( element.base->lru_node == NULL && element.base->lru_rdy ) {  
        element.base->lru_node = list_add_head ( &trie->lru_list, (void*)element.base );
        
        if ( element.base->lru_node == NULL ) ret = perrlog("list_add_head");
        
        /* Clear out the LRU ready flag */
        element.base->lru_rdy = 0;
    }
    
    if ( pthread_mutex_unlock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_unlock\n");
    }
    
    return ret;
}

int netcap_trie_lru_config ( netcap_trie_t* trie, int high_water, int low_water )
{
    if ( trie == NULL ) return errlogargs();
    
    if ( high_water < low_water ) return errlog ( ERR_CRITICAL, "Shield: High water < Low water\n" );

    /* Get the mutex lock */
    if ( pthread_mutex_lock ( &trie->lru_mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );
    
    trie->lru_low_water  = low_water;
    trie->lru_high_water = high_water;

    if ( pthread_mutex_unlock ( &trie->lru_mutex ) < 0 ) return perrlog ( "pthread_mutex_unlock" );

    return 0;
}

/* Have to obtain the lock outside of the function */
int netcap_trie_lru_del ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    if ( trie == NULL || element.base == NULL ) return errlogargs();

    if ( element.base->lru_node == NULL ) {
        return errlog ( ERR_CRITICAL, "TRIE: Attempt to remove a node that is not\n" );
    }

    if ( list_remove ( &trie->lru_list, element.base->lru_node ) < 0 ) {
        return errlog ( ERR_CRITICAL, "list_remove\n" );
    }
    
    element.base->lru_node = NULL;
    element.base->lru_rdy  = 1;
    
    return 0;
}

/* Move the node to the front of the trie */
int netcap_trie_lru_front ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    if ( trie == NULL || element.base == NULL ) return errlogargs();

    _verify_lru_is_set( trie );

    if ( pthread_mutex_lock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_lock");
    }

    /* Only update if the node is non-null */
    if ( element.base->lru_node != NULL ) {
        if ( list_move_head ( &trie->lru_list, &element.base->lru_node, element.base ) < 0 ) {
            perrlog("list_move_head");
            if ( pthread_mutex_unlock ( &element.base->mutex ) < 0 ) {
                perrlog("pthread_mutex_unlock");
            }
            return -1;
        }
    }

    if ( pthread_mutex_unlock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_unlock\n");
    }
    
    return 0;
}

int netcap_trie_lru_trash ( netcap_trie_t* trie, netcap_trie_element_t trash )
{
    list_node_t* lru_node;
    netcap_trie_level_t* parent;
    netcap_trie_element_t element;
    _trash_t* event;
    int c;
    int pos;
    int check;
    enum {
        _PARENT,         /* Keep traversing up the trie */
        _SEVER,          /* Sever this node from the parent */
        _SEVER_ADD_LRU,  /* Sever this node, and add the parent to the LRU */
        _NONE,           /* Everything necessary has been done, stop executing */
        _ERROR = -1      /* An error occured */
    } action = 0;

    element = trash;

    if ( trie == NULL || element.base == NULL ) return errlogargs();

    if (( event = malloc ( sizeof ( _trash_t ))) == NULL ) errlogmalloc();

    _verify_lru_is_set( trie );

    if ( pthread_mutex_lock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_lock\n");
    }

    if ( ( lru_node = element.base->lru_node ) == NULL ) {
        /* This simply means that the item was on the LRU and in the time it took to 
         * call this function it was removed */
        errlog( ERR_WARNING, "TRIE: Item is not on the LRU\n" ); /* XXX This should be a debugging message */
        action = _NONE;
    } else {
        element.base->lru_node = NULL;
        element.base->lru_rdy  = 0;
    }

    if ( pthread_mutex_unlock ( &element.base->mutex ) < 0 ) {
        return perrlog("pthread_mutex_unlock\n");
    }
    
    if ( action == _NONE ) return 0;
    
    for ( c = element.base->depth ; c-- >= 0 ; ) {
        if ( ( parent = element.base->parent ) == NULL ) break;

        pos = element.base->pos;

        if ( parent->base.type != NC_TRIE_BASE_LEVEL ) {
            errlog( ERR_CRITICAL, "TRIE: Non-level parent(%d)\n", parent->base.type);
            break;
        }

        action = _ERROR;

        if ( pthread_mutex_lock ( &parent->base.mutex ) < 0 ) return perrlog ("pthread_mutex_lock");
        
        do {
            if ( parent->r[pos].base == element.base ) { 
                if ( parent->base.lru_node != NULL || parent->count > 1 || parent == &trie->root ) {
                    action = _SEVER; 
                    break; 
                }

                if ( trie->check != NULL ) {
                    /* Check to see if the parent is deletable */
                    if ( (check = trie->check ( (netcap_trie_item_t*)parent )) < 0 ) {
                        errlog( ERR_CRITICAL, "TRIE: trie->check\n" );
                        break;
                    } else if ( check != NC_TRIE_IS_DELETABLE ) {
                        action = _SEVER_ADD_LRU;
                        break;
                    }
                }
                action = _PARENT;
                                
            } else {
                errlog( ERR_WARNING, "TRIE: Item overwritten with LRU enabled\n");
                element.base->parent = NULL;
                action = _NONE;
                break;
            }
        } while ( 0 );
        
        if ( action == _PARENT ) parent->base.lru_rdy = 0;
        else if ( action == _SEVER_ADD_LRU || action == _SEVER ) {
            /* Sever the link between this node and its parent */
            parent->r[pos].base = NULL;
            parent->count = MAX ( parent->count - 1 , 0 );
            element.base->parent = NULL;

            if ( action == _SEVER_ADD_LRU && parent->base.lru_node == NULL  ) {
                if ( ( parent->base.lru_node = list_add_head ( &trie->lru_list, (void*)parent ) ) == NULL ) {
                    action = perrlog("list_add_head");
                }
                                
                /* Clear out the LRU ready flag */
                parent->base.lru_rdy = 0;
            }
        }
        
        if ( pthread_mutex_unlock ( &parent->base.mutex ) < 0 ) action = perrlog ("pthread_mutex_unlock");
        
        if ( action == _PARENT ) { element.level = parent; }
        else if ( action == _SEVER || action == _SEVER_ADD_LRU || action == _NONE ) { break; }
        else { return -1; }
    }

    if ( list_remove ( &trie->lru_list, lru_node ) < 0 ) {
        /* Not a fatal error, must make sure this gets into the trash */
        perrlog("list_remove\n");
    }

    event->trie = trie;
    event->element = trash;

    if ( netcap_sched_event ( _delete, event, SEC_TO_USEC(_LRU_TRASH_EMPTY_DELAY) ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_sched_event\n" );
    }
    
    return 0;
}

int netcap_trie_lru_clean ( netcap_trie_t* trie )
{
    sem_t sem_empty;
    struct timespec ts;
    struct timeval  tv;
    
    if ( trie == NULL ) return errlogargs();

    trie->lru_low_water  = 0;
    trie->lru_high_water = 0;
    trie->check          = NULL;

    if ( netcap_trie_lru_update ( trie )  < 0 ) {
        errlog ( ERR_CRITICAL, "netcap_trie_lru_update\n");
    }
    
    if ( sem_init ( &sem_empty, 0, 0 ) < 0 ) return perrlog ( "sem_init" );
    
    if ( netcap_sched_event ( _empty, &sem_empty, SEC_TO_USEC( _LRU_TRASH_EMPTY_DELAY ) ) < 0 ) {
        return errlog ( ERR_CRITICAL, "netcap_sched_event\n" );
    }
    
    if ( gettimeofday ( &tv, NULL ) < 0 ) return perrlog ( "gettimeofday" );

    /* Two times the empty trash delay */
    ts.tv_sec  = tv.tv_sec + (_LRU_TRASH_EMPTY_DELAY << 1);
    ts.tv_nsec = 0;

    /* Wait until there is a timeout or all of the trash was emptied */
    if (( sem_timedwait( &sem_empty, &ts ) != 0 ) && (errno != ETIMEDOUT)) perrlog("sem_timedwait");

    if ( sem_destroy ( &sem_empty ) < 0 ) perrlog ( "sem_destroy" );

    return 0;
}

int netcap_trie_lru_update ( netcap_trie_t* trie )
{
    int c;
    int length;
    netcap_trie_element_t element;
    int ret = 0;

    if ( trie == NULL ) return errlogargs();

    _verify_lru_is_set( trie );

    if ( ( trie->lru_high_water < 0 ) || ( trie->lru_low_water < 0 ) ) return errlogcons();

    if ( pthread_mutex_lock ( &trie->lru_mutex ) < 0 ) return perrlog("pthread_mutex_lock");

    do {
        if ( ( length = list_length ( &trie->lru_list )) < 0 ) { 
            ret = errlog(ERR_CRITICAL,"list_length\n");
            break;
        }
        
        if ( length < trie->lru_high_water ) break;

        c = length - trie->lru_low_water;
        debug ( NC_TRIE_DEBUG_LOW, "TRIE: LRU Update - cutting %d items\n", c );
        
        for (  ; ret == 0 && c-- > 0 ; ) {
            if ( ( element.base = list_tail_val ( &trie->lru_list )) == NULL ) {
                ret = errlog(ERR_CRITICAL,"list_tail\n"); 
                break;
            }
            
            if ( netcap_trie_lru_trash ( trie, element ) < 0 ) {
                ret = errlog(ERR_CRITICAL,"netcap_trie_lru_trash\n");
                break;
            }
        }
    } while (0);
    
    if ( pthread_mutex_unlock ( &trie->lru_mutex ) < 0 ) perrlog("pthread_mutex_unlock");

    return ret;
}

static void _delete ( void* v_trash )
{
    netcap_trie_level_t* parent;
    int pos;
    int depth;
    netcap_trie_element_t element;
    netcap_trie_t* trie;
    
    if ( v_trash == NULL ) return (void)errlogargs();

    element = ((_trash_t*)v_trash)->element;
    trie    = ((_trash_t*)v_trash)->trie;
    free ( v_trash );

    if ( element.base  == NULL || trie == NULL ) return (void)errlogargs();

    debug( NC_TRIE_DEBUG_LOW, "TRIE: LRU delete\n" );
    
    while (( element.base != NULL ) && ( element.level != &trie->root )) {
        pos = element.base->pos;
        depth = element.base->depth;

        debug( NC_TRIE_DEBUG_HIGH, "TRIE: LRU deleting item at pos %d, depth %d.\n", pos, depth );

        parent = element.base->parent;

        if ( parent == NULL ) {
            netcap_trie_remove ( trie, element );
            netcap_trie_element_raze ( trie, element );
            break;
        }

        if ( parent->base.type != NC_TRIE_BASE_LEVEL ) {
            netcap_trie_remove ( trie, element );
            netcap_trie_element_raze ( trie, element );
            errlog(ERR_CRITICAL, "TRIE: Non-level parent\n");
            break;
        }
        
        if ( parent->r[pos].base != element.base ) {
            netcap_trie_remove ( trie, element );
            netcap_trie_element_raze ( trie, element );
            break;
        }

        /* This is the correct parent */
        /* Remove the node from the parent, no need to lock, nothing
         * else should be working inside of this node */
        if ( netcap_trie_level_remove ( trie, parent, pos ) < 0 ) {
            return (void)errlog( ERR_CRITICAL, "netcap_trie_level_remove\n" );
        }
        
        if ( parent->count > 0 ) break;

        /* Keep going if the parent has no other children are more nodes */
        element.level = parent;
    }
}

static void _empty  ( void* sem_empty ) {
    if ( sem_empty == NULL ) return (void)errlogargs();
    
    sem_post ( sem_empty );
}

    
    


