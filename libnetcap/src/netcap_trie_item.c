/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: netcap_trie_item.c,v 1.1 2004/11/09 19:40:00 dmorris Exp $
 */

#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"

void*                netcap_trie_item_data ( netcap_trie_item_t* item )
{
    if ( item == NULL ) return errlogargs_null();

    return item->data;
    
}

netcap_trie_item_t*  netcap_trie_item_malloc   ( netcap_trie_t* trie )
{
    netcap_trie_item_t* item;

    if ( (item = calloc(1,sizeof(netcap_trie_item_t))) == NULL ) return errlogmalloc_null();

    return item;
}

int                  netcap_trie_item_init     ( netcap_trie_t* trie, netcap_trie_item_t* item,
                                                 netcap_trie_level_t* parent, u_char pos, u_char depth )
{
    if ( item == NULL || trie == NULL ) return errlogargs();
    
    bzero( item, sizeof(netcap_trie_item_t) );
    
    /* Initialize the base */
    if ( netcap_trie_base_init( trie, item, parent, NC_TRIE_BASE_ITEM, pos, depth ) < 0 ) {
        return errlog(ERR_CRITICAL, "netcap_trie_base_init\n");
    }
   
    return 0;
}

netcap_trie_item_t*  netcap_trie_item_create   ( netcap_trie_t* trie, netcap_trie_level_t* parent,
                                                 u_char pos, u_char depth )
{
    netcap_trie_item_t* item;
    
    if ( ( item = netcap_trie_item_malloc( trie ) ) == NULL ) {
        return errlog_null(ERR_CRITICAL,"netcap_trie_item_malloc");
    }
    
    if ( netcap_trie_item_init( trie, item, parent, pos, depth ) < 0 ) {
        netcap_trie_item_free( trie, item );
        return errlog_null(ERR_CRITICAL,"netcap_trie_item_init");
    }
    
    return item;
}
 
void                 netcap_trie_item_free     ( netcap_trie_t* trie, netcap_trie_item_t* item )
{
    if ( trie == NULL || item == NULL ) return (void)errlogargs();

    if ( item->type != NC_TRIE_BASE_ITEM ) {
        errlog(ERR_CRITICAL,"TRIE: Freeing a non-item as an item\n");
    }
    
    free(item);
}

void                 netcap_trie_item_destroy  ( netcap_trie_t* trie, netcap_trie_item_t* item )
{
    if ( trie == NULL || item == NULL ) return (void)errlogargs();

    if ( item->type != NC_TRIE_BASE_ITEM ) {
        return (void)errlog(ERR_CRITICAL,"TRIE: Destroying a non-item as an item\n");
    }

    /* Clear out the parent */
    item->parent = NULL;
    
    /* Destroy the base */
    netcap_trie_base_destroy ( trie, item );
}

void                 netcap_trie_item_raze     ( netcap_trie_t* trie, netcap_trie_item_t* item )
{
    if ( trie == NULL || item == NULL ) return (void)errlogargs();

    if ( item->type != NC_TRIE_BASE_ITEM ) {
        return (void)errlog(ERR_CRITICAL,"TRIE: Razing a non-item as an item\n");
    }

    netcap_trie_item_destroy ( trie, item );

    netcap_trie_item_free( trie, item );
}

/* This copies the item itself, not a netcap_trie_item */
int                  netcap_trie_copy_data     ( netcap_trie_t* trie, netcap_trie_item_t* dest, void* src, 
                                                 in_addr_t _ip, int depth )
{
    void *temp;

    if ( dest == NULL ) return errlogargs();
    
    if ( src == NULL ) { dest->data = NULL; return 0; }
    
    if ( trie->flags & NC_TRIE_COPY ) {
        if (( temp = malloc ( trie->item_size )) == NULL ) {
            return errlogmalloc();
        }
        
        memcpy( temp, src, trie->item_size);
        src = temp;
    }

    dest->data = src;

    /* Execute the inititalization function */
    if ( ( trie->init != NULL ) && ( trie->init( dest, _ip ) < 0 )) {
        if ( trie->flags & NC_TRIE_COPY ) free ( temp );
        dest->data = NULL;
        return errlog(ERR_WARNING,"trie->init");
    }
    
    return 0;
}
