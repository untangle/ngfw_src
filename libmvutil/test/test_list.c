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
#include <string.h>
#include <errno.h>
#include "libmvutil.h"
#include "list.h"
#include "errlog.h"
#include "debug.h"

#define LIST_ADD_HEAD(list,a)      if(list_add_head((list),(void*)(a))==NULL) \
                                       return errlog(ERR_CRITICAL,"list_add_head failed\n")
#define LIST_ADD_TAIL(list,a)      if(list_add_tail((list),(void*)(a))==NULL) \
                                       return errlog(ERR_CRITICAL,"list_add_tail failed\n")
#define LIST_LENGTH_TEST(list,a)   if(list_length((list)) != (a)) \
                                       return errlog(ERR_CRITICAL,"list_length failed\n")
#define NUMINSERTS 500
#define NUMTHREADS 4

list_t* global_list;


void* insert_lots(void* basev)
{
    int base = (int)basev;
    list_node_t* nodes[NUMINSERTS+1];
    int i;
    
    for(i=0;i<NUMINSERTS;i++) {
        if ((i % 2) == 1) {
            if ((nodes[i] = list_add_head(global_list,(void*)(base+i)))==NULL) {
                errlog(ERR_CRITICAL,"list_add_head failed\n");
                exit(-1);
            }
        }
        else 
            if ((nodes[i] = list_add_tail(global_list,(void*)(base+i)))==NULL) {
                errlog(ERR_CRITICAL,"list_add_head failed\n");
                exit(-1);
            }
               
        if ((i % 25) == 0) 
            pthread_yield(); /* helps mix things up a bit */
    }
        
    for(i=0;i<NUMINSERTS;i++) {
        if (list_remove(global_list,nodes[i])<0) {
            errlog(ERR_CRITICAL,"list_remove failed\n");
            exit(-1);
        }
    }

    return 0;
}

int run_thread_test(u_int flags)
{
    int i;
    pthread_t id[NUMTHREADS];
    
    global_list = (list_t*)malloc(sizeof(list_t));
    if (list_init(global_list,flags)<0)
        return errlog(ERR_CRITICAL,"list_init failed\n");

    LIST_LENGTH_TEST(global_list,0);
    for (i=0;i<NUMTHREADS;i++)
        pthread_create(&id[i],NULL,insert_lots,(void*)(1000*(i+1)));
    
    for (i=0;i<NUMTHREADS;i++) {
        int retval;
        pthread_join(id[i],(void **)&retval);
        if((retval) < 0)
            return -1;
    }
    LIST_LENGTH_TEST(global_list,0);

    if (list_destroy(global_list)<0)
        return errlog(ERR_CRITICAL,"list_destroy failed\n");
    free(global_list);
    return 0;
}

int run_basic_test(u_int flags)
{
    int i;
    list_node_t* node;
    list_t* list = (list_t*)malloc(sizeof(list_t));
    
    if (!list)
        return -1;
    
    if (list_init(list,flags)<0)
        return errlog(ERR_CRITICAL,"list_init failed\n");

    LIST_LENGTH_TEST(list,0);
    LIST_ADD_HEAD(list,3);
    LIST_ADD_HEAD(list,2);
    LIST_ADD_HEAD(list,1);
    LIST_ADD_TAIL(list,4);
    LIST_ADD_TAIL(list,5);
    LIST_LENGTH_TEST(list,5);
        
    for (i=1,node = list_head(list);node && i<6;node = list_node_next(node),i++) {
        if (i != (int)list_node_val(node))
            return errlog(ERR_CRITICAL,"invalid value (%i != %i)\n",i,(int)list_node_val(node));
    }
        
    if (i != 6) 
        return errlog(ERR_CRITICAL,"Incorrect actual length: %i\n",i);
    LIST_LENGTH_TEST(list,5);

    if (list_remove(list,list_head(list))<0)
        return errlog(ERR_CRITICAL,"list_remove failed\n");
    if (list_remove(list,list_tail(list))<0)
        return errlog(ERR_CRITICAL,"list_remove failed\n");

    LIST_LENGTH_TEST(list,3);

    for (i=2,node = list_head(list);node && i<5;node = list_node_next(node),i++) {
        if (i != (int)list_node_val(node))
            return errlog(ERR_CRITICAL,"invalid value (%i != %i)\n",i,(int)list_node_val(node));
    }

    if (i != 5) 
        return errlog(ERR_CRITICAL,"Incorrect actual length: %i\n",i);

    for (node = list_head(list);node;node = list_head(list)) {
        if (list_remove(list,node)<0)
            return errlog(ERR_CRITICAL,"list_remove failed\n");
    }
    LIST_LENGTH_TEST(list,0);

    if (list_destroy(list)<0)
        return errlog(ERR_CRITICAL,"list_destroy failed\n");
    
    free(list);
    return 0;
}

int run_dup_test(u_int flags)
{
    int i;
    list_node_t* node;
    list_t* list = (list_t*)malloc(sizeof(list_t));
    list_t* dup;
    
    if (!list)
        return -1;
    
    if (list_init(list,flags)<0)
        return errlog(ERR_CRITICAL,"list_init failed\n");

    LIST_LENGTH_TEST(list,0);
    LIST_ADD_HEAD(list,3);
    LIST_ADD_HEAD(list,2);
    LIST_ADD_HEAD(list,1);
    LIST_ADD_TAIL(list,4);
    LIST_ADD_TAIL(list,5);
    LIST_LENGTH_TEST(list,5);

    dup = list_dup(list);

    for (i=1,node = list_head(list);node && i<6;node = list_node_next(node),i++) {
        if (i != (int)list_node_val(node))
            return errlog(ERR_CRITICAL,"invalid value (%i != %i)\n",i,(int)list_node_val(node));
    }
    for (i=1,node = list_head(dup);node && i<6;node = list_node_next(node),i++) {
        if (i != (int)list_node_val(node))
            return errlog(ERR_CRITICAL,"invalid value (%i != %i)\n",i,(int)list_node_val(node));
    }

    return 0;

}

int main(int argc, char** argv)
{
    libmvutil_init();

    debug_set_mylevel(10);

    if (run_basic_test(0)<0)
        return -1;
    if (run_basic_test(LIST_FLAG_CIRCULAR)<0)
        return -1;
    if (run_thread_test(0)<0)
        return -1;
    if (run_thread_test(LIST_FLAG_CIRCULAR)<0)
        return -1;
    if (run_dup_test(0)<0)
        return -1;
    if (run_dup_test(LIST_FLAG_CIRCULAR)<0)
        return -1;
    
    return 0;
}




