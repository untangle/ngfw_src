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
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <errno.h>
#include <semaphore.h>
#include "libmvutil.h"
#include "hash.h"
#include "errlog.h"
#include "debug.h"


#define HT_ADD_NULL(table,key,con)     if (ht_add((table),(void*)(key),(void*)(con))<0) \
                                           return errlog_null(ERR_CRITICAL,"ht_add failed: (key:%i) (errstr:%s) (pthread:%i)\n",key,errstr,pthread_self())
#define HT_ADD(table,key,con)          if (ht_add((table),(void*)(key),(void*)(con))<0) \
                                           return errlog(ERR_CRITICAL,"ht_add failed (key:%i) (errstr:%s) (pthread:%i)\n",key,errstr,pthread_self())
#define HT_RM(table,key)               if (ht_remove((table),(void*)(key))<0) \
                                           return errlog(ERR_CRITICAL,"ht_remove failed (key:%i) (errstr:%s) (pthread:%i)\n",key,errstr,pthread_self())
#define HT_RM_NULL(table,key)          if (ht_remove((table),(void*)(key))<0) \
                                           return errlog_null(ERR_CRITICAL,"ht_remove failed (key:%i) (errstr:%s) (pthread:%i)\n",key,errstr,pthread_self())

#define NUMINSERTS 500
#define NUMTHREADS 4


static sem_t sem;
static ht_t* gtable;


static void* insert_and_remove_lots (void* basev)
{
    int base = (int)basev;
    int i;
    
    for(i=0;i<NUMINSERTS;i++) {
        HT_ADD_NULL(gtable,base+i,100);

        if ((i % 25) == 0) 
            pthread_yield(); /* helps mix things up a bit */
    }
        
    
    for(i=0;i<NUMINSERTS;i++) 
        if (ht_remove(gtable,(void*)base+i)<0) 
            return (void*)errlog(ERR_CRITICAL,"ht_remove failed\n");

    return NULL;
}

static void* insert_lots (void* basev)
{
    int base = (int)basev;
    int i;
    
    for(i=0;i<NUMINSERTS;i++) 
        HT_ADD_NULL(gtable,base+i,100);

    return NULL;
}

static void* remove_lots (void* basev)
{
    int base = (int)basev;
    int i;
    
    for(i=0;i<NUMINSERTS;i++) 
        HT_RM_NULL(gtable,base+i);

    return NULL;
}

static void* producer (void* basev)
{
    int base = (int)basev;
    int i;
    
    usleep(10000);
    for(i=0;i<NUMINSERTS;i++) {
        HT_ADD_NULL(gtable,base+i,100);
        if (sem_post(&sem)<0)
            return perrlog_null("sem_post");
    }

    return NULL;
}

static void* consumer (void* basev)
{
    int base = (int)basev;
    int i;
    
    for(i=0;i<NUMINSERTS;i++) {
        if (sem_wait(&sem)<0)
            return (void*)perrlog("sem_wait");
        HT_RM_NULL(gtable,base+i);
   }
    
    return NULL;
}



static int   run_thread_test (u_int flags)
{
    int i;
    pthread_t id[2*NUMTHREADS];
    int retval;
    
    if ((gtable = ht_create())==NULL)
        return errlog(ERR_CRITICAL,"ht_create failed\n");
    
    if (ht_init(gtable,31337,int_hash_func,int_equ_func,flags)<0)
        return errlog(ERR_CRITICAL,"ht_init failed\n");

    debug_nodate(10," ... ");
    
    for (i=0;i<NUMTHREADS;i++)
        pthread_create(&id[i],NULL,insert_and_remove_lots,(void*)(1000*(i+1)));
    
    for (i=0;i<NUMTHREADS;i++) {
        pthread_join(id[i],(void **)&retval);
        if (retval < 0)
            return errlog(ERR_CRITICAL,"failed %i\n",i);
    }

    debug_nodate(10," ... ");

    for (i=0;i<NUMTHREADS;i++) 
        pthread_create(&id[i],NULL,insert_lots,(void*)(1000*(i+1)));

    for (i=0;i<NUMTHREADS;i++) {
        pthread_join(id[i],(void **)&retval);
        if (retval < 0)
            return errlog(ERR_CRITICAL,"failed %i\n",id[i]);
    }
    
    for (i=0;i<NUMTHREADS;i++) 
        pthread_create(&id[i],NULL,remove_lots,(void*)(1000*(i+1)));

    for (i=0;i<NUMTHREADS;i++) {
        pthread_join(id[i],(void **)&retval);
        if (retval < 0)
            return errlog(ERR_CRITICAL,"failed %i\n",id[i]);
    }


    debug_nodate(10," ... \n");

    if (sem_init(&sem,0,0)<0)
        return perrlog("sem_init");

    pthread_create(&id[0],NULL,producer,(void*)(1000*(i+1)));
    pthread_create(&id[1],NULL,consumer,(void*)(1000*(i+1)));

    pthread_join(id[0],(void **)&retval);
    if (retval < 0)
        return errlog(ERR_CRITICAL,"failed %i\n",id[0]);
    pthread_join(id[1],(void **)&retval);
    if (retval < 0)
        return errlog(ERR_CRITICAL,"failed %i\n",id[1]);

    if (sem_destroy(&sem)<0)
        return perrlog("sem_destroy");
    if (ht_free(gtable)!=0)
        return perrlog("ht_free");

    return 0;
}

static int   run_basic_test (u_int flags)
{
    ht_t* table;
    int i;
    
    if ((table = ht_create_and_init(31337,int_hash_func,int_equ_func,flags))==NULL)
        return errlog(ERR_CRITICAL,"ht_create failed\n");

    for(i=0;i<5;i++)
        HT_ADD(table,i,100);

    if (ht_num_entries(table)!=5)
        return errlog(ERR_CRITICAL,"Incorrect num entries %i != %i\n",ht_num_entries(table),5);
    
    for(i=0;i<5;i++)
        if (!ht_lookup(table,(void*)i))
            return errlog(ERR_CRITICAL,"Missing entry %i\n",i);

    for(i=5;i<10;i++)
        if (ht_lookup(table,(void*)i))
            return errlog(ERR_CRITICAL,"Entry %i is present, but shouldnt be\n",i);

    for(i=0;i<5;i++)
        HT_RM(table,i);

    if (ht_free(table)!=0)
            return errlog(ERR_CRITICAL,"ht_free failed\n");

    return 0;
}

static int   run_dup_test ()
{
    ht_t* table;
    int i;
    
    debug(10,"Dup Test...\n");
    if ((table = ht_create_and_init(31337,int_hash_func,int_equ_func,0))==NULL)
        return errlog(ERR_CRITICAL,"ht_create failed\n");

    if (ht_add(table,(void*)1,(void*)1)<0) 
        return errlog(ERR_CRITICAL,"ht_add failed\n");
    if (ht_add(table,(void*)1,(void*)1)==0) 
        return errlog(ERR_CRITICAL,"ht_add should have failed\n");

    if (ht_free(table)!=1)
            return errlog(ERR_CRITICAL,"ht_free should have returned 1\n");

    if ((table = ht_create_and_init(31337,int_hash_func,int_equ_func,HASH_FLAG_ALLOW_DUPS))==NULL)
        return errlog(ERR_CRITICAL,"ht_create failed\n");

    if (ht_add(table,(void*)1,(void*)1)<0) 
        return errlog(ERR_CRITICAL,"ht_add failed\n");
    if (ht_add(table,(void*)1,(void*)1)<0) 
        return errlog(ERR_CRITICAL,"ht_add failed\n");

    if ((i=ht_free(table))!=2)
            return errlog(ERR_CRITICAL,"ht_free should have returned 2\n",i);

    return 0;
}

static int   run_list_test ()
{
    int retval;
    pthread_t id;
    list_t* list;
    list_node_t* step;
    int num;
    
    if ((gtable = ht_create())==NULL)
        return errlog(ERR_CRITICAL,"ht_create failed\n");
    
    if (ht_init(gtable,31337,int_hash_func,int_equ_func,HASH_FLAG_KEEP_LIST)<0)
        return errlog(ERR_CRITICAL,"ht_init failed\n");

    pthread_create(&id,NULL,producer,(void*)1000);
    pthread_join(id,(void **)&retval);
    if (retval < 0)
        return errlog(ERR_CRITICAL,"failed %i\n",id);

    list = ht_get_bucket_list(gtable);
    for (step = list_tail(list), num=1000 ; step ; step = list_node_prev(step), num++) {
        int foo = (int)((struct bucket*)list_node_val(step))->key;
        if (foo != num)
            return errlog(ERR_CRITICAL,"list mismatch (%i != %i)\n",foo,num);
    }

    pthread_create(&id,NULL,consumer,(void*)1000);
    pthread_join(id,(void **)&retval);
    if (retval < 0)
        return errlog(ERR_CRITICAL,"failed %i\n",id);

    if (ht_free(gtable)!=0)
        return errlog(ERR_CRITICAL,"ht_free failed\n");

    return 0;
}

int main ()
{
    libmvutil_init();

    debug_set_mylevel(10);
    debug(10,"Starting...\n");
    
    if (run_dup_test()<0)
        return -1;

    debug(10,"Basic  Test 0...\n");
    if (run_basic_test(0)<0)
        return -1;
    debug(10,"Basic  Test 1...\n");
    if (run_basic_test(HASH_FLAG_ALLOW_DUPS)<0)
        return -1;
    debug(10,"Basic  Test 2...\n");
    if (run_basic_test(HASH_FLAG_KEEP_LIST)<0)
        return -1;


    debug(10,"List   Test 1...\n");
    if (run_list_test()<0)
        return -1;

    debug(10,"Thread Test 0...");
    if (run_thread_test(0)<0)
        return -1;
    debug(10,"Thread Test 1...");
    if (run_thread_test(HASH_FLAG_ALLOW_DUPS)<0)
        return -1;
    debug(10,"Thread Test 2...");
    if (run_thread_test(HASH_FLAG_KEEP_LIST)<0)
        return -1;
    
    return 0;
}
