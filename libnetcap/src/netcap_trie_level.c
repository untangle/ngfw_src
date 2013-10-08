/**
 * $Id: netcap_trie_level.c 35571 2013-08-08 18:37:27Z dmorris $
 */
#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"

 
netcap_trie_level_t* netcap_trie_level_malloc     ( netcap_trie_t* trie )
{
    netcap_trie_level_t* level;

    if ( (level = calloc(1,sizeof(netcap_trie_level_t))) == NULL ) return errlogmalloc_null();

    return level;
}

int                  netcap_trie_level_init       ( netcap_trie_t* trie, netcap_trie_level_t* level, 
                                                    netcap_trie_level_t* parent, u_char pos, u_char depth )
{
    if ( level == NULL || trie == NULL ) return errlogargs();
    
    bzero( level, sizeof(netcap_trie_level_t) );
    
    /* Redundant due to bzero */
    level->num_children = 0;
    
    /* Initialize the base */
    if ( netcap_trie_base_init( trie, &level->base, parent, NC_TRIE_BASE_LEVEL, pos, depth ) < 0 ) {
        return errlog(ERR_CRITICAL, "netcap_trie_base_init\n");
    }
   
    level->count = 0;
    
    return 0;
}

netcap_trie_level_t* netcap_trie_level_create     ( netcap_trie_t* trie, netcap_trie_level_t* parent,
                                                    u_char pos, u_char depth )
{
    netcap_trie_level_t* level;

    if ( ( level = netcap_trie_level_malloc( trie ) ) == NULL ) {
        return errlog_null(ERR_CRITICAL,"netcap_trie_level_malloc");
    }
    
    if ( netcap_trie_level_init( trie, level, parent, pos, depth ) < 0 ) {
        return errlog_null(ERR_CRITICAL,"netcap_trie_level_init");
    }
    
    return level;
}
 
void                 netcap_trie_level_free       ( netcap_trie_t* trie, netcap_trie_level_t* level )
{
    if ( trie == NULL || level == NULL ) return (void)errlogargs();

    if ( level->base.type != NC_TRIE_BASE_LEVEL ) {
        return (void)errlog(ERR_CRITICAL,"TRIE: Freeing a non-level as an item\n");
    }

    
    free( level );
}

void                 netcap_trie_level_destroy    ( netcap_trie_t* trie, netcap_trie_level_t* level )
{
    if ( trie == NULL || level == NULL ) return (void)errlogargs();

    if ( level->base.type != NC_TRIE_BASE_LEVEL ) {
        return (void)errlog(ERR_CRITICAL,"TRIE: Destroying a non-level as a level\n");
    }

    level->base.parent = NULL;

    /* It is assumed that all of the items have already been removed */
    // netcap_trie_level_remove_all ( trie, level );
    
    /* Destroy the base */
    netcap_trie_base_destroy ( trie, &level->base );
}

void                 netcap_trie_level_raze       ( netcap_trie_t* trie, netcap_trie_level_t* level )
{
    if ( trie == NULL || level == NULL ) return (void)errlogargs();

    if ( level->base.type != NC_TRIE_BASE_LEVEL ) {
        return (void)errlog(ERR_CRITICAL,"TRIE: Razing a non-level as a level\n");
    }

    netcap_trie_level_destroy ( trie, level );

    netcap_trie_level_free( trie, level );
}

int                  netcap_trie_level_insert     ( netcap_trie_t* trie, netcap_trie_level_t* level, 
                                                    netcap_trie_element_t element, u_char pos )
{
    int ret = 0;
    
    if ( trie == NULL || level == NULL || element.base == NULL ) return errlogargs();

    if ( level->base.type != NC_TRIE_BASE_LEVEL ) {
        return errlog( ERR_CRITICAL, "TRIE: Inserting an item into a non-level\n");
    }

    if ( level->r[pos].base == NULL ) {
        if ( level->count >= NC_TRIE_ROW_SIZE ) {
            errlog(ERR_CRITICAL,"TRIE: invalid level count\n");
        } else {
            ret = 1;
            level->count++;
        }

        netcap_trie_insert ( trie, element );
    } else {
        netcap_trie_element_raze ( trie, level->r[pos] );
    }
    
    level->r[pos] = element;
    return ret;
}

int                  netcap_trie_level_remove     ( netcap_trie_t* trie, netcap_trie_level_t* level,
                                                    u_char pos )
{
    if ( trie == NULL || level == NULL ) return errlogargs();

    if ( level->base.type != NC_TRIE_BASE_LEVEL ) {
        return errlog( ERR_CRITICAL, "TRIE: Unable to remove an item from a non-level\n");
    }
    
    if ( level->r[pos].base == NULL ) {
        return errlog(ERR_WARNING, "TRIE: element at position %d does not exist\n",pos);
    } else {
        netcap_trie_remove ( trie, level->r[pos] );
        
        netcap_trie_element_raze( trie, level->r[pos] );

        level->r[pos].base = NULL;
        if ( level->count < 0 ) {
            errlog( ERR_WARNING, "TRIE: invalid level count\n");
            level->count = 0;
        } else level->count--;
    }
    
    return 0;
}

int                  netcap_trie_level_remove_all ( netcap_trie_t* trie, netcap_trie_level_t* level )
{
    int c;
    int ret = 0;

    if ( trie == NULL || level == NULL ) return errlogargs();
    
    /* Remove all of the children */
    for ( c = NC_TRIE_ROW_SIZE ; ( c-- > 0) && ( level->count > 0 ) ; ) {
        if (( level->r[c].base != NULL ) && ( netcap_trie_level_remove ( trie, level, c ) < 0 )) {
            ret -= errlog(ERR_CRITICAL,"netcap_trie_level_remove\n");
        }
    }

    return ret;
}


