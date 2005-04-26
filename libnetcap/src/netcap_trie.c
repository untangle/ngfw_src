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

#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"

#define _IF_CREATE_NEW -1

#define _trie_verify_depth( depth )  if ((depth)<0||(depth)>NC_TRIE_DEPTH_TOTAL ) \
        return errlog(ERR_CRITICAL,"TRIE: Invalid level: %d", (depth))

#define _trie_verify_depth_null( depth )  if ((depth)<0||(depth)>NC_TRIE_DEPTH_TOTAL ) \
        return errlog_null(ERR_CRITICAL,"TRIE: Invalid level: %d", (depth))

static netcap_trie_item_t*   _get             ( netcap_trie_t* trie, in_addr_t _ip, int depth );

static netcap_trie_item_t*   _set             ( netcap_trie_t* trie, in_addr_t _ip, void *item, 
                                                netcap_trie_func_t* func, void* arg, int depth );

static int                   _delete          ( netcap_trie_t* trie, in_addr_t _ip, int depth );

static netcap_trie_element_t _create_element ( netcap_trie_t* trie,  netcap_trie_level_t* parent, void* data,
                                               netcap_trie_func_t* func, void* arg, in_addr_t _ip, int depth,
                                               int depth_target );

static int                   _update_element ( netcap_trie_t* trie, netcap_trie_element_t element, 
                                               void* data, netcap_trie_func_t* func, void* arg,
                                               in_addr_t _ip, int depth );
                                               

void* netcap_trie_get             ( netcap_trie_t* trie, in_addr_t ip )
{
    return _get( trie, ip, NC_TRIE_DEPTH_TOTAL );
}

void* netcap_trie_get_close       ( netcap_trie_t* trie, in_addr_t ip )
{
    return _get( trie, ip, _IF_CREATE_NEW );
}


void* netcap_trie_get_depth      ( netcap_trie_t* trie, in_addr_t ip, int depth )
{
    if ( trie == NULL ) return errlogargs_null();
    
    _trie_verify_depth_null( depth );
    
    return _get( trie, ip, depth );
}

netcap_trie_item_t* netcap_trie_apply       ( netcap_trie_t* trie, in_addr_t ip, netcap_trie_func_t* func, 
                                              void* arg ) 
{
    return netcap_trie_apply_depth ( trie, ip, func, arg, NC_TRIE_DEPTH_TOTAL );
}

netcap_trie_item_t* netcap_trie_apply_close ( netcap_trie_t* trie, in_addr_t ip, netcap_trie_func_t* func,
                                              void* arg )
{
    if ( func == NULL ) return errlogargs_null();
    return _set ( trie, ip, NULL, func, arg, _IF_CREATE_NEW );
}
                                             

netcap_trie_item_t* netcap_trie_apply_depth ( netcap_trie_t* trie, in_addr_t ip, netcap_trie_func_t* func, 
                                              void* arg, int depth ) 
{
    if ( func == NULL ) return errlogargs_null();

    _trie_verify_depth_null( depth );

    return _set ( trie, ip, NULL, func, arg, depth );
}

netcap_trie_item_t* netcap_trie_set         ( netcap_trie_t* trie, in_addr_t ip, void *item )
{
    return netcap_trie_set_depth( trie, ip, item, NC_TRIE_DEPTH_TOTAL );
}

netcap_trie_item_t* netcap_trie_set_depth   ( netcap_trie_t* trie, in_addr_t ip, void *data, int depth )
{
    if ( trie == NULL ) return errlogargs_null();

    _trie_verify_depth_null( depth );
    
    if ( !(trie->flags & NC_TRIE_INHERIT ) && data == NULL ) return errlogargs_null();
    
    return _set ( trie, ip, data, NULL, NULL, depth );
}

int   netcap_trie_delete       ( netcap_trie_t* trie, in_addr_t _ip )
{
    return netcap_trie_delete_depth ( trie, _ip, NC_TRIE_DEPTH_TOTAL );
}

int   netcap_trie_delete_depth ( netcap_trie_t* trie, in_addr_t _ip, int depth )
{
    if ( trie == NULL ) return errlogargs();

    if ( trie->flags & NC_TRIE_LRU ) return errlog(ERR_CRITICAL,"TRIE: Cannot delete with LRU flag set\n");
    
    _trie_verify_depth ( depth );
    
    return _delete ( trie, _ip, depth );
}

static netcap_trie_item_t* _get   (netcap_trie_t* trie, in_addr_t _ip,  int depth )
{
    u_char* ip;
    netcap_trie_element_t child;
    netcap_trie_level_t* parent;
    int c;
    int if_lock = trie->flags & NC_TRIE_LRU;
    int if_get_close = 0;

    if ( depth == _IF_CREATE_NEW ) { depth = NC_TRIE_DEPTH_TOTAL;  if_get_close = 1; }

    child.level = parent = &trie->root;

    ip = (u_char*)&_ip;

    debug(NC_TRIE_DEBUG_HIGH,"_trie_get(%d): 0x%08x", depth, _ip); 
    
    /* Grab the trie */
    for ( c = 0 ; c < depth ; c++ ) {
        if ( parent->base.depth != c ) {
            errlog(ERR_WARNING, "TRIE: Incorrect depth (%d,%d)\n", c, parent->base.depth );
        }

        debug_nodate (NC_TRIE_DEBUG_HIGH," -(%d-%d)- ", ip[c], parent->count);

        if ( if_lock && pthread_mutex_lock ( &parent->base.mutex ) < 0 ) {
            return perrlog_null("pthread_mutex_lock");
        }

        child = parent->r[ip[c]];

        if ( if_lock && pthread_mutex_unlock ( &parent->base.mutex ) < 0 ) {
            return perrlog_null("pthread_mutex_unlock");
        }
        
        /* Item does not exist */
        if ( child.base == NULL ) {
            debug_nodate(NC_TRIE_DEBUG_HIGH,"\n");
            if ( if_get_close )  return (netcap_trie_item_t*)parent;
            return NULL;
        }

        if ( child.base->type != NC_TRIE_BASE_LEVEL ) break;

        parent = child.level;
    }

    debug_nodate(NC_TRIE_DEBUG_HIGH,"\n");

    return child.item;
}

static netcap_trie_item_t* _set ( netcap_trie_t* trie, in_addr_t _ip, void *data, 
                                  netcap_trie_func_t* func, void* arg, int depth )
{
    int c;
    int if_create_new = 1;
    u_char *ip;
    u_char pos;
    netcap_trie_element_t parent;
    netcap_trie_element_t element;
    pthread_mutex_t*      mutex;       
    int if_lock = trie->flags & NC_TRIE_LRU;
    
    ip = (u_char*)&_ip;

    debug( NC_TRIE_DEBUG_HIGH, "_trie_set(%d): 0x%08x\n", depth, _ip );
    
    if ( depth == _IF_CREATE_NEW ) { depth = NC_TRIE_DEPTH_TOTAL;  if_create_new = 0; }

    parent.level = &trie->root;

    if ( depth == 0 ) {
        if ( _update_element ( trie, parent, data, func, arg, _ip, depth ) < 0 ) {
            return errlog_null(ERR_CRITICAL,"_update_element\n");
        }
        return parent.item;
    }

    for ( c = 0 ; c < depth ; c++ )  {
        pos = ip[c];

        if ( ( parent.base->data != NULL ) && ( func != NULL )) {
            if ( func ( parent.item, arg, _ip ) < 0 ) return errlog_null(ERR_CRITICAL, "TRIE: Apply func\n");
        }

        if ( parent.base->type == NC_TRIE_BASE_LEVEL ) {
            mutex = &parent.base->mutex;

            /* XXX Change to a do while ( 0 ) */
            if ( if_lock && pthread_mutex_lock ( mutex ) < 0 ) {
                return perrlog_null("pthread_mutex_lock");
            }

            /* Check if the item exists */
            if ( parent.level->r[pos].level == NULL ) {
                if ( if_create_new == 0 ) {
                    if ( if_lock ) pthread_mutex_unlock ( mutex );
                    break;
                }
                
                element = _create_element ( trie, parent.level, data, func, arg, _ip, c+1, depth );
                
                if ( element.base == NULL ) {
                    if ( if_lock ) pthread_mutex_unlock ( mutex );
                    return errlog_null(ERR_CRITICAL,"_create_element\n");
                }

                /* Before adding the item, check to see if this level is on the LRU */
                if ( ( trie->flags & NC_TRIE_LRU ) && ( parent.base->lru_node != NULL ) ) {
                    /* If so, remove it */
                    if ( netcap_trie_lru_del ( trie, parent ) < 0 ) {
                        if ( if_lock ) pthread_mutex_unlock ( mutex );
                        return errlog_null ( ERR_CRITICAL, "netcap_trie_lru_remove\n" );
                    }
                }
                
                if ( netcap_trie_level_insert( trie, parent.level, element, pos ) < 0 ) {
                    if ( if_lock ) pthread_mutex_unlock ( mutex );
                    return errlog_null(ERR_CRITICAL,"netcap_trie_insert\n");
                }
            } else if (( c + 1 ) == depth ) {
                if ( _update_element ( trie, parent.level->r[pos], data, func, arg, _ip, depth ) < 0 ) {
                    if ( if_lock ) pthread_mutex_unlock ( mutex );
                    return errlog_null(ERR_CRITICAL,"_update_element\n");
                }
            }

            /* Do this before unlocking the element */
            parent.level = parent.level->r[pos].level;

            if ( if_lock && pthread_mutex_unlock ( mutex ) < 0 ) {
                return perrlog_null("pthread_mutex_unlock");
            }
        } else {
            break;
        }
    }
    
    return parent.item;
}

static int                   _delete          ( netcap_trie_t* trie, in_addr_t _ip, int depth )
{
    u_char* ip;
    netcap_trie_element_t child;
    netcap_trie_level_t*   parent;
    u_char pos;
    
    ip = (u_char*)&_ip;

    parent = &trie->root;

    child.item = _get ( trie, _ip, depth );

    /* Nothing to delete */
    if ( child.item == NULL ) return 0;
    
    for ( depth = child.base->depth ; depth >= 0 ; depth-- ) {

        if ( ( parent = child.item->parent ) == NULL ) {
            if ( depth == 0 ) {
                if ( netcap_trie_remove_all ( trie ) < 0 ) {
                    return errlog(ERR_CRITICAL,"netcap_trie_remove_all\n");
                }
            } else {
                netcap_trie_element_raze ( trie, child );
            }
            break;
        }

        pos = child.base->pos;
        
        if ( parent->r[pos].item != child.item ) {
            netcap_trie_element_raze ( trie, child );
            return errlog( ERR_CRITICAL, "TRIE: Child no longer belongs to this parent\n" );
        }
        
        if ( netcap_trie_level_remove ( trie, parent, pos ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_trie_level_remove\n" );
        }

        if ( parent->count == 0 ) child.level = parent;
        else  break;
    }

    return 0;
}

static netcap_trie_element_t _create_element ( netcap_trie_t* trie,  netcap_trie_level_t* parent, void* data,
                                               netcap_trie_func_t* func, void* arg, in_addr_t _ip, int depth,
                                               int depth_target )
{
    netcap_trie_element_t element;
    u_char* ip = (u_char*)&_ip;
    u_char pos;
    void *src;

    pos = ip[depth-1];

    if ( depth == NC_TRIE_DEPTH_TOTAL ) {
        element.item  = netcap_trie_item_create  ( trie, parent, pos, depth );
        if ( element.item == NULL ) {
            errlog ( ERR_CRITICAL, "netcap_trie_item_create\n" );
            return element;
        }
    } else {
        element.level = netcap_trie_level_create ( trie, parent, pos, depth );
        if ( element.level == NULL ) {
            errlog_null ( ERR_CRITICAL, "netcap_trie_level_create\n" );
            return element;
        }
    }
    
    if ( trie->flags & NC_TRIE_INHERIT ) {
        if ( depth == depth_target && data != NULL ) {
            src = data;
        } else {
            src = parent->base.data;
        }
        
        if ( netcap_trie_copy_data ( trie, element.base, src, _ip, depth ) < 0 ) {
            netcap_trie_element_raze ( trie, element );
            errlog (ERR_CRITICAL, "netcap_trie_copy_data\n" );
            element.item = NULL; return element;
        }
    } else {
        if ( ( depth == depth_target )  &&
             ( netcap_trie_copy_data ( trie, element.base, data, _ip, depth ) < 0 )) {
            netcap_trie_element_raze ( trie, element );
            errlog ( ERR_CRITICAL, "netcap_trie_copy_data\n" );
            element.item = NULL; return element;
        }
    }

    /* XXX Small hole.  This should read depth == depth_target.  because, if the user creates a node
     * that doesn't reach the TOTAL depth, and then never creates any children, that node will never 
     * get onto the LRU.  Until there are new test cases in place, leave this as is. (That case will
     * never happen in the shield) */
    if ( depth == NC_TRIE_DEPTH_TOTAL  && ( trie->flags & NC_TRIE_LRU ) ) {
        if ( netcap_trie_lru_add ( trie, element ) < 0 ) {
            netcap_trie_element_raze ( trie, element );
            errlog_null( ERR_CRITICAL,"netcap_trie_lru_add\n" );
            element.item = NULL; return element;
        }
    }

    return element;
}

static int                   _update_element ( netcap_trie_t* trie, netcap_trie_element_t element, 
                                               void* data, netcap_trie_func_t* func, void* arg,
                                               in_addr_t _ip, int depth )
{
    if ( data != NULL ) {
        /* If necessary free the associated item */
        _data_destroy ( trie, element );
        
        if ( netcap_trie_copy_data ( trie, element.item, data, _ip, depth ) < 0 ) {
            return errlog( ERR_CRITICAL, "netcap_trie_copy_data\n" );
        }
    }
        
    if ( ( func != NULL )  && ( func ( element.item, arg, _ip ) < 0) ) {
        return errlog( ERR_CRITICAL, "TRIE: Apply function\n" );
    }

    return 0;
}

