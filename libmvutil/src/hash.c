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
#include "mvutil/hash.h"

#include <stdlib.h>
#include <sys/types.h>
#include <string.h>
#include <errno.h>
#include "mvutil/lock.h"
#include "mvutil/errlog.h"
#include "mvutil/debug.h"

#define _HASH_SUCCESS 0
#define _HASH_FAILURE -1
#define TABLE_RDLOCK(table)     if ((table)->lock_on && pthread_rwlock_rdlock(&(table)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to rdlock hash table\n")
#define TABLE_WRLOCK(table)     if ((table)->lock_on && pthread_rwlock_wrlock(&(table)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to wrlock hash table\n")
#define TABLE_UNLOCK(table)     if ((table)->lock_on && pthread_rwlock_unlock(&(table)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to unlock hash table\n")
#define TABLE_RDLOCK_NULL(table)   if ((table)->lock_on && pthread_rwlock_rdlock(&(table)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to rdlock hash table\n")
#define TABLE_WRLOCK_NULL(table)   if ((table)->lock_on && pthread_rwlock_wrlock(&(table)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to wrlock hash table\n")
#define TABLE_UNLOCK_NULL(table)   if ((table)->lock_on && pthread_rwlock_unlock(&(table)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to unlock hash table\n")
#define _LOOKUP(table,key)    (table)->buckets[((table)->hash_func((key)))%((table)->size)]


static int   _ht_add(ht_t* table,void* key,void* contents);
static int   _ht_destroy(ht_t* table);
static int   _ht_remove(ht_t* table,void* key);
static int   _ht_add_replace(ht_t* table,void* key,void* contents);
static int   _ht_print_table (ht_t* table);
static void* _ht_lookup(ht_t* table,void* key);
static void* _ht_lookup_key(ht_t* table,void* key);
static list_t* _ht_get_content_list (ht_t* table, int content_or_key);
static u_char fake_equ_func (const void* input,const void* input2);
static u_long  fake_hash_func (const void* input);

static int  _remove_all_buckets (ht_t* table);
static void _remove_bucket(ht_t* table,bucket_t* buck);



ht_t*   ht_create ()
{
    ht_t* new_table;

    new_table = (ht_t*)malloc(sizeof(struct hash_table));

    if (!new_table) {
        errno = ENOMEM;
        return errlogmalloc_null ();
    }
    
    return new_table;
}

int     ht_init (ht_t* table,int size,ht_hash_func_t h_func,ht_equal_func_t e_func,u_int flags)
{
    int num;
    
    if (!table || !size || !h_func || !e_func) 
        return errlog(ERR_CRITICAL,"Invalid arguments\n");
    
    num = pthread_rwlock_init(&table->lock,NULL);
    if (num)
        return perrlog("pthread_rwlock_init");

    if (flags & HASH_FLAG_KEEP_LIST)
        if (list_init(&table->list,0)<0)
            return perrlog("list_init");
    
    table->buckets = (bucket_t**)calloc(1,size* sizeof(void*));
    if (!table->buckets) {
        errno = ENOMEM; /* some mallocs don't set */
        return errlogmalloc();
    }
    
    table->size = size;
    table->num_entries = 0;
    table->equal_func = e_func;
    table->hash_func  = h_func;

    table->free_key_flag      = (flags & HASH_FLAG_FREE_KEY);
    table->free_contents_flag = (flags & HASH_FLAG_FREE_CONTENTS);
    table->allow_dups_flag    = (flags & HASH_FLAG_ALLOW_DUPS);
    table->keep_list_flag     = (flags & HASH_FLAG_KEEP_LIST);

    if (flags & HASH_FLAG_NO_LOCKS)
        table->lock_on = 0;
    else
        table->lock_on = 1;
    
    return 0;
}

ht_t*   ht_create_and_init (int size,ht_hash_func_t h_func,ht_equal_func_t e_func,u_int flags)
{
    ht_t* table = ht_create();
    if (!table) 
        return NULL;
    
    if (ht_init(table,size,h_func,e_func,flags)<0) {
        free(table);
        return NULL;
    }
    
    return table;
}

int     ht_destroy (ht_t* table)
{
    if (!table)
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    TABLE_WRLOCK(table);
    return _ht_destroy(table);
}

int     ht_free (ht_t* table)
{
    int ret;

    if (!table)
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    TABLE_WRLOCK(table);
    ret = _ht_destroy(table);
    free(table);
    return ret;
}
    
int     ht_add (ht_t* table,void* key,void* contents)
{
    int ret;
    
    if (!table)
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}
    
    TABLE_WRLOCK(table);
    ret = _ht_add(table,key,contents);
    TABLE_UNLOCK(table);
    
    return ret;
}

int     ht_add_replace (ht_t* table,void* key,void* contents)
{
    int ret;
    
    if (!table)
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}
    
    TABLE_WRLOCK(table);
    ret = _ht_add_replace(table,key,contents);
    TABLE_UNLOCK(table);
    
    return ret;
}

int     ht_remove (ht_t* table,void* key)
{
    int ret;
    
    if (!table)
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}
    
    TABLE_WRLOCK(table);
    ret =  _ht_remove(table,key);
    TABLE_UNLOCK(table);

    return ret;
}

void*   ht_lookup (ht_t* table,void* key)
{
    void* ret;

    if (!table) {
        errno = EINVAL;
        return errlog_null(ERR_CRITICAL,"Invalid arguments\n");
    } else
        errno = 0;
    
    TABLE_RDLOCK_NULL(table);
    ret = _ht_lookup(table,key);
    TABLE_UNLOCK_NULL(table);

    return ret;
}

void*   ht_lookup_key (ht_t* table,void* key)
{
    void* ret;
    
    if (!table) {
        errno = EINVAL;
        return errlog_null(ERR_CRITICAL,"Invalid arguments\n");
    } else
        errno = 0;
 
    TABLE_RDLOCK_NULL(table);
    ret = _ht_lookup_key(table,key);
    TABLE_UNLOCK_NULL(table);
    
    return ret;
}

int     ht_size (ht_t* table)
{
    if (!table) 
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    return table->size;
}

int     ht_num_entries (ht_t* table)
{
    if (!table) 
    {errno = EINVAL; return errlog(ERR_CRITICAL,"Invalid arguments\n");}

    return table->num_entries;
}

list_t* ht_get_bucket_list (ht_t* table)
{
    if (!table) 
    {errno = EINVAL; return errlogargs_null();}

    if (!table->keep_list_flag)
        return NULL;
    else
        return list_dup(&table->list);
}

list_t* ht_get_content_list (ht_t* table)
{
    return _ht_get_content_list(table,1);
}

list_t* ht_get_key_list (ht_t* table)
{
    return _ht_get_content_list(table,0);
}


u_long  string_hash_func (const void* input)
{
    char* sn = (char* )input;
    int i;
    int len = strlen(sn);
    
    u_long total = 1;

    for(i=0;i<len && i<5;i++)
        total*= sn[i];
        
    return total;
}

u_char string_equ_func (const void* input,const void* input2)
{
    if (!input2 && input) return 0;
    if (!input && input2) return 0;
    return !(strcmp((char*)input,(char*)input2));
}

u_long  int_hash_func (const void* input)
{
    return (u_long)input;
}

u_char int_equ_func (const void* input,const void* input2)
{
    if (input == input2) return 1;
    else return 0;
}

static u_long  fake_hash_func (const void* input)
{
    errlog(ERR_CRITICAL,"fake_hash_func called!\n");
    return 0L;
}

static u_char fake_equ_func (const void* input,const void* input2)
{
    errlog(ERR_CRITICAL,"fake_equ_func called!\n");
    return 0;
}

static int   _ht_destroy(ht_t* table)
{
    int ret;
    
    /**
     * free all the elements in each bucket 
     */
    ret = _remove_all_buckets(table);
    free(table->buckets);

    /**
     * free the list
     */
    if (table->keep_list_flag) 
        if (list_destroy(&table->list)<0)
            errlog(ERR_CRITICAL,"list_destroy failed\n");

    /**
     * and the ht itself
     */
    if (pthread_rwlock_destroy(&table->lock)<0)
        errlog(ERR_CRITICAL,"Unable to destroy hash table lock\n");

    table->buckets = NULL;
    table->hash_func = fake_hash_func;
    table->equal_func = fake_equ_func;
    table->num_entries = 0;
    
    return ret;
}

static int   _ht_add (ht_t* table,void* key,void* contents)
{
    int modkey;
    bucket_t* newbuck;
    void* res;
    u_char add_front = 1;
    
    modkey = table->hash_func(key) % table->size;

    /**
     * if its a dup but its not allowed - return error
     */
    if ((res = _ht_lookup(table,key)) && !table->allow_dups_flag) {
        errno = EPERM;
        return _HASH_FAILURE; //entry alread present
    }
    
    /**
     * if its a dup but its allowed - add it at the end 
     */
    if (res && table->allow_dups_flag) 
        add_front = 0;
  
    newbuck = (bucket_t*) calloc(1,sizeof(bucket_t));
    if (!newbuck) {
        errno = ENOMEM;
        return errlogmalloc();
    }

    newbuck->contents = contents;
    newbuck->key = key;

    if (table->keep_list_flag) {
        list_node_t* node = list_add_head(&table->list,newbuck);
        if (!node)
            errlog(ERR_CRITICAL,"list_add_head failed\n");
        newbuck->list_node = node;
    }
    
    if (add_front) {
        /**
         * put it first on the list
         */
        newbuck->next = table->buckets[modkey]; 
        table->buckets[modkey] = newbuck;
    }
    else {
        /**
         * put it last on the list
         */
        bucket_t* step = table->buckets[modkey];
        if (!step) {
            free(newbuck);
            return _HASH_FAILURE;
        }
        
        for(;step->next;) step = step->next;
        step->next = newbuck;
        
    }

    table->num_entries++;

    return _HASH_SUCCESS;
}

static int   _ht_add_replace (ht_t* table,void* key,void* contents)
{
    if (_ht_lookup(table,key)) _ht_remove(table,key);
    return _ht_add(table,key,contents);
}

static int   _ht_remove (ht_t* table,void* key)
{
    int modkey;
    bucket_t* firstbucket;
    bucket_t* nextbucket;

    modkey = table->hash_func(key) % table->size;
    firstbucket = table->buckets[modkey];
  
    if (!firstbucket)  //no buckets for that key
        return _HASH_FAILURE;
    
    if (table->equal_func(key,firstbucket->key)) {
        //its the first in the table
        table->buckets[modkey] = firstbucket->next;
        _remove_bucket(table,firstbucket);
        table->num_entries--;
        return _HASH_SUCCESS;
    }

    nextbucket=firstbucket->next;
    while(nextbucket)
    {
        if (table->equal_func(key,nextbucket->key))
        {
            firstbucket->next=nextbucket->next; //remove it
            _remove_bucket(table,nextbucket);
            table->num_entries--;
            return _HASH_SUCCESS;
        }
      
        firstbucket=nextbucket;
        nextbucket=nextbucket->next;
    }

    return _HASH_FAILURE;
}

static void* _ht_lookup (ht_t* table,void* key)
{
    bucket_t* buck;

#ifdef DEBUG_ON
    u_long idx;

    if (!table->hash_func || !table->equal_func) {
        errlog(ERR_CRITICAL,"Constraint failed: NULL func! (0x%08x,0x%08x)\n",table->hash_func,table->equal_func);
        _ht_print_table(table);
        pthread_kill(pthread_self(),11);
        return NULL;
    }    
        
    idx = table->hash_func(key);
    buck = table->buckets[(idx % table->size)];
#else
    buck = _LOOKUP(table,key);
#endif
    
    while(buck) {
        if (table->equal_func(key,buck->key)) {
            return buck->contents;
        }
        else buck=buck->next;
    }
  
    return NULL;
}

static void* _ht_lookup_key (ht_t* table,void* key)
{
    bucket_t* buck;
    
#ifdef DEBUG_ON
    bucket_t** buckets = table->buckets;
    int size = table->size;
    int idx = (table->hash_func(key))%size;
    buck = buckets[idx];
#else
    buck = _LOOKUP(table,key); 
#endif
    
    while(buck) {
        if (table->equal_func(key,buck->key)) 
            return buck->key;
        else buck=buck->next;
    }
  
    return NULL;
}

static list_t* _ht_get_content_list (ht_t* table, int content_or_key)
{
    list_t* ll = ht_get_bucket_list(table);
    list_node_t* step;
    bucket_t* buck;
    
    if (!ll)
        return NULL;

    for (step = list_head(ll) ; step ; step = list_node_next(step)) {
        buck = list_node_val(step);

        if (!buck) {
            list_destroy(ll);
            list_free(ll);
            return errlog_null(ERR_CRITICAL,"Constraint Failed: null bucket\n");
        }
        else {
            if (content_or_key)
                list_node_val_set(step,buck->contents);
            else
                list_node_val_set(step,buck->key);
        }
    }

    return ll;
}

static int   _ht_print_table (ht_t* table)
{
    if (!table)
        return -1;
    
    errlog(ERR_INFORM,"table           : 0x%08x\n",table);
    errlog(ERR_INFORM,"table->size     : %i\n",table->size);
    errlog(ERR_INFORM,"table->buckets  : 0x%08x\n",table->buckets);
    errlog(ERR_INFORM,"table->num_entries  : %i\n",table->num_entries);
    errlog(ERR_INFORM,"table->hash_func    : 0x%08x\n",table->hash_func);
    errlog(ERR_INFORM,"table->equal_func   : 0x%08x\n",table->equal_func);
    errlog(ERR_INFORM,"flags: free_key:%i free_contents:%i allow_dups:%i keep_list:%i\n",
           table->free_key_flag,table->free_contents_flag,table->allow_dups_flag,table->keep_list_flag);

    return 0;
}


static int   _remove_all_buckets (ht_t* table)
{
    bucket_t* buck;
    bucket_t* nextbuck;
    int i;
    int rm_count = 0;
    
    /**
     * if there is a list, use it, otherwise
     * run down the buckets array
     */
    if (table->keep_list_flag) {
        list_node_t* step;
        list_node_t* next;

        for (step = list_head(&table->list) ; step ; step = next) {
            next = list_node_next(step);
            _remove_bucket(table,list_node_val(step));
            rm_count++;
        }
    }
    else {
        int size = table->size;

        for(i=0;i<size;i++) 
            for(buck = table->buckets[i] ; buck ; buck = nextbuck ) {
                nextbuck = buck->next; 
                _remove_bucket(table,buck);
                rm_count++;
            }
    }

    return rm_count;
}

static void  _remove_bucket (ht_t* table,bucket_t* buck)
{
    if (table->free_key_flag)
        free(buck->key);
    if (table->free_contents_flag)
        free(buck->contents);
    if (table->keep_list_flag)
        if (buck->list_node && (list_remove(&table->list,buck->list_node)<0))
            errlog(ERR_CRITICAL,"list_remove failed\n");
    
    free(buck);
}

