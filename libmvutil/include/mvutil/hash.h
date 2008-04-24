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
#ifndef __HASH_H
#define __HASH_H

#include <sys/types.h>
#include "list.h"

/**
 * The type of a hash function
 */
typedef u_long  (*ht_hash_func_t)(const void *);

/**
 * The type of a equals test (compares two keys)
 */
typedef u_char (*ht_equal_func_t)(const void *,const void *);

typedef struct bucket {
    void* contents;
    void* key;
    list_node_t*    list_node;
    struct bucket * next;
} bucket_t;

typedef struct hash_table {
    int size;
    struct bucket ** buckets;
    int num_entries;
    
    ht_hash_func_t  hash_func;
    ht_equal_func_t equal_func;

    u_char free_key_flag; 
    u_char free_contents_flag; 
    u_char allow_dups_flag;
    u_char keep_list_flag;

    pthread_rwlock_t lock;
    list_t list;

    int lock_on;
    
} ht_t;

typedef struct hash_table hash_table_t;

/**
 * call free() on key upon freeing the containing bucket
 */
#define HASH_FLAG_FREE_KEY      1

/**
 * call free() on contents upon freeing the containing bucket
 */
#define HASH_FLAG_FREE_CONTENTS 2

/**
 * Allow duplicate keys in the Table
 */
#define HASH_FLAG_ALLOW_DUPS    4

/**
 * Keep a list of all entries in the table
 * 1) Allows for quick destroying (to remove all buckets)
 * 2) Enables ht_get_bucket_list (to retrieve all entries)
 * but decreases efficiency
 */
#define HASH_FLAG_KEEP_LIST     8

/**
 * Do not lock the table
 * This will be slightly faster, but is not thread safe
 */
#define HASH_FLAG_NO_LOCKS      16

/*! \brief creates a uninitialized hash table
 * 
 * \param size             size of table (number of buckets) \n
 * \param ht_hash_func_t   function pointer for the hash() func \n
 * \param ht_equal_func_t  function pointer for the equal() func - this should return 1 if two keys are equal \n
 * \param flags            the flags (any of the follwing) \n
 *   HASH_FLAG_FREE_KEY        flag on whether or not the key will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_FREE_CONTENTS   flag on whether or not the contents will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_ALLOW_DUPS      flag on whether or not to allow duplicate entries in table \n
 *   HASH_FLAG_KEEP_LIST       flag on whether or not to keep a list of entries \n
 * \return  the hash table or NULL upon failure
 */ 
ht_t*  ht_create (void);

/*! \brief initializes a hash table
 *
 * \param size             size of table (number of buckets) \n
 * \param ht_hash_func_t   function pointer for the hash() func \n
 * \param ht_equal_func_t  function pointer for the equal() func - this should return 1 if two keys are equal \n
 * \param flags            the flags (any of the follwing) \n
 *   HASH_FLAG_FREE_KEY        flag on whether or not the key will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_FREE_CONTENTS   flag on whether or not the contents will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_ALLOW_DUPS      flag on whether or not to allow duplicate entries in table \n
 *   HASH_FLAG_KEEP_LIST       flag on whether or not to keep a list of entries \n
 * \return  0 on success, -1 otherwise
 */
extern int    ht_init (ht_t* table,int size,ht_hash_func_t h_func,ht_equal_func_t e_func,u_int flags);

/*! \brief creates and initializes a hash table
 *
 * \param size             size of table (number of buckets) \n
 * \param ht_hash_func_t   function pointer for the hash() func \n
 * \param ht_equal_func_t  function pointer for the equal() func - this should return 1 if two keys are equal \n
 * \param flags            the flags (any of the follwing) \n
 *   HASH_FLAG_FREE_KEY        flag on whether or not the key will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_FREE_CONTENTS   flag on whether or not the contents will be automatically freed upon removal from the ht \n
 *   HASH_FLAG_ALLOW_DUPS      flag on whether or not to allow duplicate entries in table \n
 *   HASH_FLAG_KEEP_LIST       flag on whether or not to keep a list of entries \n
 * \return  the hash table or NULL upon failure
 */
extern ht_t*  ht_create_and_init (int size,ht_hash_func_t h_func,ht_equal_func_t e_func,u_int flags);

/*! \brief frees all the resources of a hash table
 * 
 *  this will free all the keys if HASH_FLAG_FREE_KEY is set \n
 *  this will free all the contents if HASH_FLAG_FREE_CONTENTS is set \n
 *  it returns the number of entries that were in the table
 * 
 * \warning as of now this is slow  -> O(size + elements)
 * \param table   the hash table itself 
 * \return  the number of entries in the table, or -1 upon error
 */ 
extern int    ht_destroy (ht_t* table);

/*! \brief frees all the resources of a hash table and the table itself
 * 
 *  this will free all the keys if HASH_FLAG_FREE_KEY is set \n
 *  this will free all the contents if HASH_FLAG_FREE_CONTENTS is set \n
 *  it returns the number of entries that were in the table
 * 
 * \warning as of now this is slow  -> O(size + elements)
 * \param table   the hash table itself 
 * \return  the number of entries in the table, or -1 upon error
 */ 
extern int    ht_free (ht_t* table);

/*! \brief adds and entry to the hash table 
 * both key and contents are stored in location hash(key)%table_size \n
 * the content can be the key (if you are just storing stuff) \n
 * or the content can be something associated with the key \n
 *
 * \param table          the table
 * \param key            a pointer to the key or the key
 * \param contents       the contents (can be a pointer or an int) 
 * \return  -1 upon failure or collision(unless ALLOW_DUPS is specified)
 */ 
extern int    ht_add (ht_t* table,void* key,void* contents);

/*! \brief same as ht_add - but replaces a collision
 * instead of returning -1 \n
 *
 * \param table          the table
 * \param key            a pointer to the key or the key
 * \param contents       the contents (can be a pointer or an int) 
 * \return  -1 upon failure 
 */ 
extern int    ht_add_replace (ht_t* table,void* key,void* contents);

/*! \brief removes an element from the table
 *
 * \param table          the table
 * \param key            a pointer to the key or the key
 * \return  -1 upon failure 
 */ 
extern int    ht_remove (ht_t* table,void* key);

/*! \brief returns the *contents* associated with the given key
 *
 * \param table          the table
 * \param key            a pointer to the key or the key
 * \return  the contents or NULL with errno set upon failure
 */ 
extern void*  ht_lookup (ht_t* table,void* key);

/*! \brief returns the *key* associated with the given key
 *  this returns the actual key you passed in ht_add
 *  you may need to get access for freeing data
 *
 * \param table          the table
 * \param key            a pointer to the key or the key
 * \return  the contents or NULL with errno set upon failure
 */ 
extern void*  ht_lookup_key (ht_t* table,void* key);

/*! \brief returns the size of the hash table
 * This is NOT the number of entries
 *
 * \param table          the table
 * \return  the size of the table or -1 on error
 */ 
extern int    ht_size (ht_t* table);

/*! \brief returns the number of entries in the table
 *
 * \param table          the table
 * \return  the size of the table or -1 on error
 */ 
extern int    ht_num_entries (ht_t* table);

/*! \brief returns the list of hash buckets
 *  This is only valid when HASH_FLAG_KEEP_LIST is specified
 *  This returns a list of the table entries
 *  You must call list_destroy and list_free on this list
 *
 * \param table          the table
 * \return  the list or NULL upon error
 */ 
extern list_t* ht_get_bucket_list (ht_t* table);

/*! \brief returns the list of hash contents
 *  This is only valid when HASH_FLAG_KEEP_LIST is specified
 *  This returns a list of the table entries
 *  You must call list_destroy and list_free on this list
 *
 * \param table          the table
 * \return  the list or NULL upon error
 */ 
extern list_t* ht_get_content_list (ht_t* table);

/*! \brief returns the list of hash keys
 *  This is only valid when HASH_FLAG_KEEP_LIST is specified
 *  This returns a list of the table entries
 *  You must call list_destroy and list_free on this list
 *
 * \param table          the table
 * \return  the list or NULL upon error
 */ 
extern list_t* ht_get_key_list (ht_t* table);



/*! \brief hash equality function for strings
 * this is included because it is very commonly used \n
 * 
 * \param input        a char *
 * \param input2       a char *
 * \return 1 if equal 0 otherwise
 */
extern u_char string_equ_func (const void* input,const void* input2);

/*! \brief returns a hash of the string
 * this is included because it is very commonly used \n
 *
 * \param input        a char *
 * \return the hash
 */
extern u_long  string_hash_func (const void* input);

/*! \brief hash equality function for strings
 * this is the int equality function
 * useful for when your keys are ints
 * 
 * \param input        an int
 * \param input2       an int
 * \return 1 if equal 0 otherwise
 */
extern u_char int_equ_func (const void* input,const void* input2);

/*! \brief returns a hash of the int
 * this is the identity function
 * useful for when your keys are just int's.
 *
 * \param input        a int
 * \return the hash
 */
extern u_long  int_hash_func (const void* input);




#endif




