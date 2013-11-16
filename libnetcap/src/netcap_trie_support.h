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

#ifndef __NETCAP_TRIE_SUPPORT_H_
#define __NETCAP_TRIE_SUPPORT_H_

#include "netcap_trie.h"

#define MAX(a,b) ((a) > (b)) ? (a) : (b)
#define MIN(a,b) ((a) < (b)) ? (a) : (b)
     
int netcap_trie_insert     ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_remove     ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_remove_all ( netcap_trie_t* trie );

static __inline__ void _data_destroy ( netcap_trie_t* trie, netcap_trie_element_t element )
{
    if  ( element.base->data != NULL ) { 
        if ( trie->destroy != NULL ) trie->destroy ( element.item );
        if ( trie->flags & NC_TRIE_FREE ) free ( element.base->data );
        element.base->data = NULL;
    }
}

/* Item */
netcap_trie_item_t*  netcap_trie_item_malloc   ( netcap_trie_t* trie );

int                  netcap_trie_item_init     ( netcap_trie_t* trie, netcap_trie_item_t* item,
                                                 netcap_trie_level_t* parent, u_char pos, u_char depth );

netcap_trie_item_t*  netcap_trie_item_create   ( netcap_trie_t* trie, netcap_trie_level_t* parent,
                                                 u_char pos, u_char depth );

void                 netcap_trie_item_free     ( netcap_trie_t* trie, netcap_trie_item_t* item );

void                 netcap_trie_item_destroy  ( netcap_trie_t* trie, netcap_trie_item_t* item );

void                 netcap_trie_item_raze     ( netcap_trie_t* trie, netcap_trie_item_t* item );

/* This copies the data itself, not a netcap_trie_item */
int                  netcap_trie_init_data     ( netcap_trie_t* trie, netcap_trie_base_t* dest, void* src, 
                                                 struct in_addr* ip, int depth );

netcap_trie_level_t* netcap_trie_level_malloc     ( netcap_trie_t* trie );

int                  netcap_trie_level_init       ( netcap_trie_t* trie, netcap_trie_level_t* level, 
                                                    netcap_trie_level_t* parent, u_char pos, u_char depth );

netcap_trie_level_t* netcap_trie_level_create     ( netcap_trie_t* trie, netcap_trie_level_t* parent,
                                                    u_char pos, u_char depth );
 
void                 netcap_trie_level_free       ( netcap_trie_t* trie, netcap_trie_level_t* level );

void                 netcap_trie_level_destroy    ( netcap_trie_t* trie, netcap_trie_level_t* level );

void                 netcap_trie_level_raze       ( netcap_trie_t* trie, netcap_trie_level_t* level );

int                  netcap_trie_level_insert     ( netcap_trie_t* trie, netcap_trie_level_t* level, 
                                                    netcap_trie_element_t element, u_char pos );

int                  netcap_trie_level_remove     ( netcap_trie_t* trie, netcap_trie_level_t* level, 
                                                    u_char pos );

int                  netcap_trie_level_remove_all ( netcap_trie_t* trie, netcap_trie_level_t* level );

/* Base */
int                  netcap_trie_base_init     ( netcap_trie_t* trie, netcap_trie_base_t* base, 
                                                 netcap_trie_level_t* parent, u_char if_level, 
                                                 u_char pos, u_char depth );

void                 netcap_trie_base_destroy  ( netcap_trie_t* trie, netcap_trie_base_t* base );

/* Elements */
void                 netcap_trie_element_destroy ( netcap_trie_t* trie, netcap_trie_element_t element );

void                 netcap_trie_element_raze    ( netcap_trie_t* trie, netcap_trie_element_t element );

/**
 * Returns the number of children that an item has (1 for terminal nodes),      *
 * this doesn't use the mutex and the value may not be "correct", but it should *
 * be within one of the value.                                                  *
 **/
int                  netcap_trie_element_children( netcap_trie_element_t element );


/* LRU */
int netcap_trie_lru_add   ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_lru_del   ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_lru_front ( netcap_trie_t* trie, netcap_trie_element_t element );

int netcap_trie_lru_trash ( netcap_trie_t* trie, netcap_trie_element_t trash );

/* put all of the nodes into the trash, and then wait for the trash to be emptied */
int netcap_trie_lru_clean ( netcap_trie_t* trie );

#endif // #ifndef __NETCAP_TRIE_SUPPORT_H_
