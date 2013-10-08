/* $HeadURL: svn://chef/work/src/libnetcap/src/netcap_trie.c $ */

#include <stdlib.h>

#include <mvutil/debug.h>
#include <mvutil/errlog.h>
#include <mvutil/list.h>
#include <mvutil/unet.h>

#include "netcap_trie.h"
#include "netcap_trie_support.h"

#define _trie_verify_depth( depth )  if ((depth)<0||(depth)>NC_TRIE_DEPTH_TOTAL ) \
        return errlog(ERR_CRITICAL,"TRIE: Invalid level: %d", (depth))

#define _trie_verify_depth_null( depth )  if ((depth)<0||(depth)>NC_TRIE_DEPTH_TOTAL ) \
        return errlog_null(ERR_CRITICAL,"TRIE: Invalid level: %d", (depth))

#define _validate_depth( expected_depth, actual_depth ) \
if (( expected_depth ) != ( actual_depth )) \
    errlog( ERR_WARNING, "Depth mismatch [%d,%d]\n", ( expected_depth ), ( actual_depth ))

static netcap_trie_element_t _create_element( netcap_trie_t* trie, netcap_trie_level_t* parent, 
                                              struct in_addr* ip, u_char pos );

static int _set_line( netcap_trie_line_t* line, netcap_trie_base_t* base );

/* Decrements num_children for all of parent, and all of its parents */
static int _decrement_children( netcap_trie_t* trie, netcap_trie_level_t* parent, struct in_addr* _ip );

/* Get the closest item to ip, this never creates a new item  */
int new_netcap_trie_get            ( netcap_trie_t* trie, struct in_addr* _ip, netcap_trie_line_t* line )
{
    if ( trie == NULL || _ip == NULL || line == NULL ) return errlogargs();
    
    netcap_trie_level_t* parent = &trie->root;
    netcap_trie_level_t* child;
    int c;
    u_char* ip = (u_char*)&_ip->s_addr;
    
    bzero( line->d, sizeof( line->d ));
    /* There is at least the root */
    line->d[0].level = &trie->root;
    line->count = 1;
    line->is_bottom_up = 0;
    
    debug( NC_TRIE_DEBUG_HIGH, "new_netcap_trie_get: %#010x", _ip->s_addr ); 

    for ( c = 0 ; c < NC_TRIE_DEPTH_TOTAL ; c++ ) {
        if ( parent->base.depth != c ) {
            errlog( ERR_WARNING, "TRIE: Incorrect depth (%d,%d)\n", c, parent->base.depth );
        }

        debug_nodate ( NC_TRIE_DEBUG_HIGH," -(%d %d)- ", ip[c], parent->count );
        
        child = parent->r[ip[c]].level;
        
        if ( child == NULL ) {
            debug_nodate( NC_TRIE_DEBUG_HIGH,"\n");
            return 0;
        }
        
        /* Add the next item down to the lineage */
        line->d[line->count++].level = child;
        
        parent = child;
    }

    debug_nodate( NC_TRIE_DEBUG_HIGH,"\n");

    return 0;
}

/* Insert an item into at ip, and the fill the line with item, if the
 * item is already in the trie, this just returns its line. */
int new_netcap_trie_insert_and_get ( netcap_trie_t* trie, struct in_addr* _ip, pthread_mutex_t* mutex,
                                     netcap_trie_line_t* line )
{
    if ( trie == NULL || _ip == NULL || line == NULL ) return errlogargs();
    
    netcap_trie_item_t* item = NULL;

    int _critical_section( void ) {
        u_char* ip;
        ip = (u_char*)&_ip->s_addr;
        netcap_trie_level_t* parent = &trie->root;
        netcap_trie_element_t child;

        int depth;
        u_char pos;
        
        /* Try to lookup the item, once more, it could have been inserted while waiting 
         * for the mutex lock */
        if (( item = ht_lookup( &trie->ip_element_table, (void*)(long)_ip->s_addr )) != NULL ) {
            if ( _set_line( line, (netcap_trie_base_t*)item ) < 0 ) {
                return errlog( ERR_CRITICAL, "_set_line\n" );
            }
            return 0;
        }

        bzero( line->d, sizeof( line->d ));

        /* There is at least the root */
        line->d[0].level = &trie->root;
        line->count = 1;
        line->is_bottom_up = 0;
        
        debug( NC_TRIE_DEBUG_HIGH, "new_netcap_trie_insert_and_get: %#010x", _ip->s_addr );

        /* Use two just in case */
        int max_size = ht_num_entries( &trie->ip_element_table ) + 2;

        for ( depth = 0 ; depth < NC_TRIE_DEPTH_TOTAL ; depth++ ) {
            /* Increase the number of children in the parent */
            if ( parent->num_children < 0 ) {
                errlog( ERR_WARNING, "Parent at %#010x/%d has too many children[%d], set to 1\n.", 
                        _ip->s_addr, depth, parent->num_children );
                parent->num_children = 1;
            } else if ( parent->num_children > max_size ) {
                errlog( ERR_WARNING, "Parent at %#010x/%d has too many children[%d], set to %d\n.", 
                        _ip->s_addr, depth, parent->num_children, max_size );
                parent->num_children = max_size;
            } else {
                parent->num_children++;
            }
            
            _validate_depth( depth, parent->base.depth );

            pos = ip[depth];
            
            debug_nodate ( NC_TRIE_DEBUG_HIGH," -(%d %d)- ", pos, parent->count );
            
            child = parent->r[pos];
            
            if ( child.base == NULL ) {
                child = _create_element( trie, parent, _ip, pos );
                if ( child.base == NULL ) return errlog( ERR_CRITICAL, "_create_element\n" );
            }
            
            line->d[line->count++].level = child.level;
            parent = child.level;
        }

        debug_nodate ( NC_TRIE_DEBUG_HIGH, "\n" );

        /* Insert the node into the hash table */
        if ( ht_add( &trie->ip_element_table, (void*)(long)_ip->s_addr, child.base ) < 0 ) {
            return errlog( ERR_CRITICAL, "ht_add\n" );
        }
        
        /* All done */
        return 0;
    }
        

    /* Try to lookup the item, if it is in there, then return it */
    if (( item = ht_lookup( &trie->ip_element_table, (void*)(long)_ip->s_addr )) != NULL ) {
        if ( _set_line( line, (netcap_trie_base_t*)item ) < 0 ) return errlog( ERR_CRITICAL, "_set_line\n" );
        return 0;
    }

    int ret = 0;

    /* Now lock the mutex, and check again */
    if ( mutex != NULL && pthread_mutex_lock( mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );
    
    ret = _critical_section();
    
    if ( mutex != NULL && pthread_mutex_unlock( mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
    
    return ret;    
}

/* Remove an item from the trie and return the memory it used in line.
 *   Depending on the application, this memory shouldn't be freed immediately
 *   as it may be used by other threads.
 */
int new_netcap_trie_remove         ( netcap_trie_t* trie, struct in_addr* _ip, pthread_mutex_t* mutex,
                                     netcap_trie_line_t* line )
{
    if ( trie == NULL || _ip == NULL || line == NULL ) return errlogargs();
    
    int ret = 0;

    int _critical_section( void ) {
        netcap_trie_level_t* parent = &trie->root;
        netcap_trie_element_t child;
        
        int depth;

        /* Set this before hand, this way the user knows if it didn't remove anything */
        line->is_bottom_up = 1;
        line->count = 0;
        bzero( line->d, sizeof( line->d ));
        
        /* Try to lookup the item, if it exists, then it needs to be removed,
         * if it doesn't then who cares */
        if (( child.base = ht_lookup( &trie->ip_element_table, (void*)(long)_ip->s_addr )) == NULL ) {
            debug_nodate( NC_TRIE_DEBUG_LOW, "\nTRIE: ignoring remove for ip %#010x\n", _ip->s_addr );
            return 0;
        }

        line->d[line->count++] = child;

        /* Remove the item right away, ??? return the error */
        if ( ht_remove( &trie->ip_element_table, (void*)(long)_ip->s_addr ) < 0 ) {
            errlog( ERR_CRITICAL, "ht_remove\n" );
        }
        
        for ( depth = NC_TRIE_DEPTH_TOTAL ; depth-- > 0 ; ) {
            u_char pos = child.base->pos;

            if (( parent = child.base->parent ) == NULL ) {
                errlog( ERR_CRITICAL, "Disconnected node in trie for ip %#010x\n", _ip->s_addr );
                return 0;
            }
            
            _validate_depth( depth, parent->base.depth );

            /* Check if the correct item is being pointed to here */
            if ( parent->r[pos].base != child.base ) {
                errlog( ERR_CRITICAL, "ignoring, node mismatch depth %d parent: %#010x child: %#010x\n",
                        depth, parent->r[pos], child );
                child.base->parent = NULL;
                return 0;
            }

            /* XXX May want to check if parent->count is always positive. */
            if ( parent->count > 1 || depth == 0 ) {
                /* Decrement children of all of the parent nodes. */
                _decrement_children( trie, parent, _ip );

                /* Otherwise remove the child and decrement the number of nodes. */
                parent->r[pos].base = NULL;
                parent->count--;
                
                child.base->parent = NULL;
                return 0;
            }

            child.level =parent;
            line->d[line->count++] = child;
        }
        
        /* This line is pretty much unreachable (if statement if ( parent->count > 1 || depth == 0 )) */
        errlog( ERR_CRITICAL, "unexpected statement\n" );

        return 0;
    }

    if ( mutex != NULL && pthread_mutex_lock( mutex ) < 0 ) return perrlog( "pthread_mutex_lock" );

    debug( NC_TRIE_DEBUG_HIGH, "new_netcap_trie_remove: %#010x", _ip->s_addr );
    
    ret = _critical_section();

    debug_nodate( NC_TRIE_DEBUG_HIGH, "\n" );
    
    if ( mutex != NULL && pthread_mutex_unlock( mutex ) < 0 ) return perrlog( "pthread_mutex_unlock" );
    
    return ret;    
}


int new_netcap_trie_line_free      ( netcap_trie_line_t* line )
{
    if ( line == NULL ) return errlogargs();

    free( line );
    return 0;
}

int new_netcap_trie_line_destroy   ( netcap_trie_t* trie, netcap_trie_line_t* line )
{
    /* Raze a line, this deletes all of the elements in it,  */
    if ( trie == NULL || line == NULL ) return errlogargs();
    
    int count = line->count;
    
    if ( count > NC_TRIE_DEPTH_TOTAL ) {
        errlog( ERR_WARNING, "line has too many nodes %d\n", line->count );
        count = NC_TRIE_DEPTH_TOTAL;
    }

    debug( NC_TRIE_DEBUG_HIGH, "Destroying %d items\n", count );
    
    /* Destroy the item */
    int c;
    
    for ( c = 0 ; c < count ; c++ ) {
        if ( line->d[c].level == &trie->root ) {
            errlog( ERR_CRITICAL, "REMOVE contains root node\n" );
            continue;
        }

        debug( NC_TRIE_DEBUG_HIGH, "razing:  %#010x\n", line->d[c].base );
        netcap_trie_element_raze( trie, line->d[c] );
        line->d[c].item = NULL;
    }
    /* Check if any of the other items are non-null, if they are report an error */
    for ( ; c < NC_TRIE_DEPTH_TOTAL + 1 ; c++ ) {
        if ( line->d[c].item != NULL ) errlog( ERR_WARNING, "Non-null item in line at index %d\n", c );
    }
    
    return 0;
}

int new_netcap_trie_line_raze      ( netcap_trie_t* trie, netcap_trie_line_t* line )
{
    /* Raze a line, this deletes all of the elements in it,  */
    if ( trie == NULL || line == NULL ) return errlogargs();
    
    if ( new_netcap_trie_line_destroy( trie, line ) < 0 ) {
        errlog( ERR_CRITICAL, "new_netcap_trie_line_destroy\n" );
    }

    if ( new_netcap_trie_line_free( line ) < 0 ) {
        errlog( ERR_CRITICAL, "new_netcap_trie_line_raze\n" );
    }
    
    return 0;
}

/* This copies the item itself, not a netcap_trie_item */
int netcap_trie_init_data          ( netcap_trie_t* trie, netcap_trie_item_t* dest, void* src, 
                                     struct in_addr* ip, int depth )
{
    void *temp = NULL;

    if ( dest == NULL || ip == NULL ) return errlogargs();
    
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
    if (( trie->init != NULL ) && ( trie->init( dest, ip ) < 0 )) {
        if ( trie->flags & NC_TRIE_COPY ) free ( temp );
        dest->data = NULL;
        return errlog( ERR_WARNING, "trie->init" );
    }
    
    return 0;
}

static netcap_trie_element_t _create_element( netcap_trie_t* trie, netcap_trie_level_t* parent, 
                                              struct in_addr* ip, u_char pos )
{
    int depth = parent->base.depth + 1;
    netcap_trie_element_t child;
    child.base = NULL;

    debug_nodate ( NC_TRIE_DEBUG_HIGH, "n" );
    
    /* Have to create a new item */
    if ( NC_TRIE_DEPTH_TOTAL == depth ) {
        if (( child.item = netcap_trie_item_create( trie, parent, pos, depth )) == NULL ) {
            debug_nodate ( NC_TRIE_DEBUG_HIGH,"\n" );
            errlog( ERR_CRITICAL, "netcap_trie_item_create\n" );
            return child;
        }
    } else {
        if (( child.level = netcap_trie_level_create( trie, parent, pos, depth )) == NULL ) {
            debug_nodate ( NC_TRIE_DEBUG_HIGH,"\n" );
            errlog( ERR_CRITICAL, "netcap_trie_level_create\n" );
            return child;
        }
    }

    /* Initialize the data */
    /* This always inherits from the parent */
    if ( netcap_trie_init_data( trie, child.item, parent->base.data, ip, depth ) < 0 ) {
        netcap_trie_element_raze( trie, child );
        errlog( ERR_CRITICAL, "netcap_trie_init_data\n" );
        child.base = NULL;
        return child;
    }
    
    /* Insert the new item */
    parent->r[pos] = child;
    
    /* Increment the number of items */
    parent->count++;

    return child;
}

static int _set_line( netcap_trie_line_t* line, netcap_trie_base_t* base )
{
    int c = base->depth;

    line->is_bottom_up = 1;
    line->count = 0;
    bzero( line->d, sizeof( line->d ));
    
    if ( c > NC_TRIE_DEPTH_TOTAL ) return errlog( ERR_CRITICAL, "Invalid depth %d\n", c );
    
    for (  ; c >= 0 && ( base != NULL ) ; c-- ) {
        _validate_depth( c, base->depth );
        line->d[line->count++].base = base;
        base = (netcap_trie_base_t*)base->parent;
    }
    
    return 0;
}

static int _decrement_children( netcap_trie_t* trie, netcap_trie_level_t* parent, struct in_addr* _ip )
{
    int depth;
    
    /* Use two just in case */
    int max_size = ht_num_entries( &trie->ip_element_table ) + 2;
    
    /* Decrement the depth after the first iteration of the loop (starts at the current node) */
    for ( depth = parent->base.depth; depth >= 0 ; depth-- ) {
        if ( parent == NULL ) {
            return errlog( ERR_WARNING, "Parent at %#010x/%d is NULL\n.", _ip->s_addr, depth );
        }

        _validate_depth( depth, parent->base.depth );

        /* Validate that the size is reasonable */
        if ( parent->num_children > max_size ) {
            errlog( ERR_WARNING, "Parent at %#010x/%d has too many children[%d], set to %d\n.", 
                    _ip->s_addr, depth, parent->num_children, max_size );
            parent->num_children = max_size;
        }

        if ( parent->num_children <= 0 ) {
            errlog( ERR_WARNING, "Parent at %#010x/%d has an invalid number of children[%d], set to 0.\n", 
                    _ip->s_addr, depth, parent->num_children );
            parent->num_children = 0;
        } else {
            parent->num_children--;
        }
        
        if ((( parent = parent->base.parent ) == NULL )) {
            if ( depth == 0 ) break;
            else              return errlog( ERR_CRITICAL, "Disconnected node for %#010x\n", _ip->s_addr );
        }
    }

    return 0;
}
