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
#include <time.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"


int  netcap_trie_base_init  ( netcap_trie_t* trie, netcap_trie_base_t* base, netcap_trie_level_t* parent, 
                              u_char type, u_char pos, u_char depth )
{
    if ( trie == NULL || base == NULL ) return errlogargs();

    base->type     = type;
    base->pos      = pos;
    base->depth    = depth;
    base->data     = NULL;
    base->parent   = parent;
    base->lru_rdy  = 1;
    base->lru_node = NULL;

    if ( pthread_mutex_init ( &base->mutex, NULL ) < 0 ) return perrlog("pthread_mutex_init");
    
    return 0;
}

void netcap_trie_base_destroy ( netcap_trie_t* trie, netcap_trie_base_t* base )
{
    if ( trie == NULL || base == NULL ) {
        errlogargs();
        return;
    }

    /* Clear out all of the pointers */
    base->parent = NULL;
    
    /* If necessary free the associated item */
    _data_destroy ( trie, (netcap_trie_element_t)base );

    /* Delete the mutex */
    if ( pthread_mutex_destroy ( &base->mutex ) < 0 ) perrlog("pthread_mutex_destroy");
}

void netcap_trie_element_destroy ( netcap_trie_t* trie, netcap_trie_element_t element ) {
    if ( trie == NULL || element.base == NULL ) {
        errlogargs();
        return;
    }

    if ( element.level->base.type == NC_TRIE_BASE_LEVEL ) {
        netcap_trie_level_destroy ( trie, element.level );
    } else if ( element.level->base.type == NC_TRIE_BASE_ITEM ) {
        netcap_trie_item_destroy ( trie, element.item );
    } else {
        errlog(ERR_CRITICAL, "TRIE: Trying to raze an unknown structure: %d\n", element.base->type );
    }

}

void netcap_trie_element_raze    ( netcap_trie_t* trie, netcap_trie_element_t element ) {
    if ( trie == NULL || element.base == NULL ) {
        errlogargs();
        return;
    }

    if ( element.base->type == NC_TRIE_BASE_LEVEL ) {
        netcap_trie_level_raze ( trie, element.level );
    } else if ( element.base->type == NC_TRIE_BASE_ITEM ) {
        netcap_trie_item_raze ( trie, element.item );
    } else {
        errlog(ERR_CRITICAL, "TRIE: Trying to raze an unknown structure: %d\n", element.base->type );
    }
}

