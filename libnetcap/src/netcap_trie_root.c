/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_trie_root.c,v 1.1 2004/11/09 19:40:00 dmorris Exp $
 */

#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"

netcap_trie_t* netcap_trie_malloc     ( void )
{
    netcap_trie_t* trie;

    if ( ( trie = calloc( sizeof(netcap_trie_t*), 1 )) == NULL ) return errlogmalloc_null();

    return trie;
}

int            netcap_trie_init       ( netcap_trie_t* trie, int flags, void* item, int item_size, 
                                        netcap_trie_init_t* init, netcap_trie_destroy_t* destroy,
                                        netcap_trie_check_t* check, int high_water, int low_water )
{
    if ( trie == NULL ) return errlogargs();
    
    trie->mem  = 0;
    trie->item_count = 0;
    
    /* Set the FREE flag if necessary */
    trie->flags = 0;

    if ( flags & NC_TRIE_COPY ) {
        if ( item_size == 0 ) return errlogargs();
        trie->flags |= NC_TRIE_COPY;
        if ( !(flags & NC_TRIE_FREE ) ) {
            errlog(ERR_WARNING,"TRIE: Not advisable to use NC_TRIE_COPY and not NC_TRIE_FREE\n");
        }
    }

    if ( flags & NC_TRIE_FREE    ) trie->flags |= NC_TRIE_FREE;    
    if ( flags & NC_TRIE_INHERIT ) trie->flags |= NC_TRIE_INHERIT;

    trie->item_size = item_size;

    trie->init = init;
    trie->destroy = destroy;
    
    if ( flags & NC_TRIE_LRU ) {
        trie->flags |= NC_TRIE_LRU;
        if ( low_water > high_water ) {
            return errlog ( ERR_CRITICAL,"TRIE: low_water > high_water\n");
        }

        if ( list_init ( &trie->lru_list, 0 ) < 0 ) {
            return errlog(ERR_CRITICAL,"list_init");
        }
        
        if ( pthread_mutex_init ( &trie->lru_mutex, NULL ) < 0 ) {
            return perrlog("pthread_mutex_init\n");
        }
        
        trie->lru_low_water  = low_water;
        trie->lru_high_water = high_water;
        trie->check          = check;

    } else {
        if ( low_water != 0 || high_water != 0 ) {
            errlog(ERR_WARNING,"TRIE: When the LRU is disabled high_water and low_water are ignored\n");
        }
        if ( check != NULL ) {
            errlog(ERR_WARNING,"TRIE: When the LRU is disabled, the check function is unused\n");
        }

        trie->lru_low_water = trie->lru_high_water = 0;
        trie->check = NULL;
    }
    
    if ( netcap_trie_level_init ( trie, &trie->root, NULL, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_level_init\n");
    }
            
    if ( netcap_trie_copy_data ( trie, &trie->root.base, item, 0, 0 ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_copy_item");
    }

    return 0;
}

netcap_trie_t* netcap_trie_create     ( int flags, void* item, int item_size, 
                                        netcap_trie_init_t* init,  netcap_trie_destroy_t* destroy,
                                        netcap_trie_check_t* check, int high_water, int low_water )
{
    netcap_trie_t* trie;
    
    if (( trie = netcap_trie_malloc()) == NULL ) {
        return errlog_null ( ERR_CRITICAL,"netcap_trie_malloc\n");
    }
    
    if ( netcap_trie_init ( trie, flags, item, item_size, init, destroy, check, high_water, low_water ) < 0 ) {
        netcap_trie_free ( trie );
        return errlog_null ( ERR_CRITICAL,"netcap_trie_init\n");
    }
    
    return trie;
}

void           netcap_trie_free       ( netcap_trie_t* trie )
{
    if ( trie == NULL ) return(void)errlogargs();

    free ( trie );
}

void           netcap_trie_destroy    ( netcap_trie_t* trie )
{
    if ( trie == NULL ) return (void)errlogargs();

    if ( trie->flags & NC_TRIE_LRU ) {
        if ( netcap_trie_lru_clean ( trie ) < 0 ) errlog ( ERR_CRITICAL, "netcap_trie_lru_clean\n" );

        if ( list_destroy ( &trie->lru_list ) < 0 ) perrlog ( "list_destroy" );

        if ( pthread_mutex_destroy ( &trie->lru_mutex ) < 0 ) perrlog("pthread_mutex_destroy");
    }

    netcap_trie_level_destroy ( trie, &trie->root );

    if ( trie->mem != 0 ) errlog (ERR_WARNING, "TRIE: root memory mismatch: %d\n", trie->mem);
    if ( trie->item_count != 0 ) errlog ( ERR_WARNING, "TRIE: root count mismatch: %d\n", trie->item_count );
}

void           netcap_trie_raze       ( netcap_trie_t* trie )
{
    if ( trie == NULL ) return(void)errlogargs();

    netcap_trie_destroy ( trie );
    
    netcap_trie_free ( trie );
}

void*          netcap_trie_data       ( netcap_trie_t* trie )
{
    if ( trie == NULL ) return errlogargs_null();
    
    return trie->root.base.data;
}

int            netcap_trie_insert     ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    int status;

    if ( trie == NULL || element.base == NULL ) return errlogargs();

    switch ( element.base->type ) {
    case NC_TRIE_BASE_LEVEL: trie->mem += sizeof(netcap_trie_level_t); break;
    case NC_TRIE_BASE_ITEM: trie->item_count++; break;        
    default:
        return errlog( ERR_WARNING,"TRIE: Uknown item type inserted (%d)\n", status );
    }

    return 0;
}

int            netcap_trie_remove_all ( netcap_trie_t* trie )
{
    if ( trie == NULL ) return errlogargs();
    
    _data_destroy ( trie, (netcap_trie_element_t)&trie->root );
    
    if ( netcap_trie_level_remove_all ( trie, &trie->root ) < 0 ) {
        return errlog(ERR_CRITICAL,"netcap_trie_level_remove_all\n");
    }
    
    return 0;
}

int            netcap_trie_remove     ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    int status;

    if ( trie == NULL || element.base == NULL ) return errlogargs();

    switch ( element.base->type ) {
    case NC_TRIE_BASE_LEVEL:
        if ( trie->mem  < sizeof (netcap_trie_level_t)) {
            errlog(ERR_WARNING,"TRIE: Invalid amount of memory used\n");
        }
        
        trie->mem = MAX( 0, trie->mem - sizeof(netcap_trie_level_t));
        break;

    case NC_TRIE_BASE_ITEM:
        if ( trie->item_count  < 1) errlog(ERR_WARNING,"TRIE: Invalid item_count\n");        
        trie->item_count = MAX ( 0, trie->item_count -1);
        break;
        
    default:
        return errlog( ERR_WARNING,"TRIE: Uknown item type removed (%d)\n", status );
    }

    return 0;
}


