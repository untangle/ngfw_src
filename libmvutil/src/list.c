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
#include "mvutil/list.h"

#include <stdlib.h>
#include "mvutil/errlog.h"
#include "mvutil/debug.h"

#define LIST_RDLOCK(list)     if (pthread_rwlock_rdlock(&(list)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to rdlock list\n")
#define LIST_WRLOCK(list)     if (pthread_rwlock_wrlock(&(list)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to wrlock list\n")
#define LIST_UNLOCK(list)     if (pthread_rwlock_unlock(&(list)->lock)<0) \
                                    return errlog(ERR_CRITICAL,"Unable to unlock list\n")
#define LIST_RDLOCK_NULL(list)   if (pthread_rwlock_rdlock(&(list)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to rdlock list\n")
#define LIST_WRLOCK_NULL(list)   if (pthread_rwlock_wrlock(&(list)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to wrlock list\n")
#define LIST_UNLOCK_NULL(list)   if (pthread_rwlock_unlock(&(list)->lock)<0) \
                                       return errlog_null(ERR_CRITICAL,"Unable to unlock list\n")
#define LIST_RDLOCK_NR(list)   if (pthread_rwlock_rdlock(&(list)->lock)<0) \
                                       errlog(ERR_CRITICAL,"Unable to rdlock list\n")
#define LIST_WRLOCK_NR(list)   if (pthread_rwlock_wrlock(&(list)->lock)<0) \
                                       errlog(ERR_CRITICAL,"Unable to wrlock list\n")
#define LIST_UNLOCK_NR(list)   if (pthread_rwlock_unlock(&(list)->lock)<0) \
                                       errlog(ERR_CRITICAL,"Unable to unlock list\n")


static int          _list_destroy (list_t* ll);
static int          _list_length (list_t* ll);
static list_node_t* _list_head (list_t* ll);
static list_node_t* _list_tail (list_t* ll);
static list_node_t* _list_node_next (list_node_t* ln);
static list_node_t* _list_node_prev (list_node_t* ln);
static void*        _list_node_val  (list_node_t* ln);
static list_node_t* _list_add_head (list_t* ll, void* val);
static list_node_t* _list_add_tail (list_t* ll, void* val);
static int          _list_remove (list_t* ll, list_node_t* ln);
static int          _list_apply (list_t* ll, list_apply_func_t func);

static list_node_t* _list_node_create (void* val, list_t* mylist);
static list_node_t* _list_add_before (list_t* ll, list_node_t* beforeme, void* val);
static list_node_t* _list_add_after  (list_t* ll, list_node_t* afterme,  void* val);

list_t*      list_malloc ()
{
    list_t* tmp = malloc(sizeof(list_t));

    if (!tmp) 
        return errlogmalloc_null();

    return tmp;
}

list_t*      list_create (u_int flags)
{
    list_t* ll = list_malloc();

    if (!ll)
        return NULL;

    if (list_init(ll,flags)<0) {
        list_free(ll);
        return NULL;
    }

    return ll;
}

void         list_free (list_t* ll)
{
    if (!ll) {
        errlogargs();
        return;
    }

    free(ll);
}

int          list_init (list_t* ll, u_int flags)
{
    int num;
    if (!ll) {errno = EINVAL; return errlogargs();}

    ll->head   = NULL;
    ll->tail   = NULL;
    ll->length = 0;
    ll->flags  = flags;
 
    num = pthread_rwlock_init(&ll->lock,NULL);
    if (num)
        return perrlog("pthread_rwlock_init");

    return 0;
}

int          list_destroy (list_t* ll)
{
    if (!ll) {errno = EINVAL; return errlogargs();}

    LIST_WRLOCK(ll);
    return _list_destroy(ll);
}

int          list_raze (list_t* ll)
{
    int ret = 0;
    if (!ll) {errno = EINVAL; return errlogargs();}

    ret += list_destroy(ll);
    list_free(ll);

    return ret;
}

int          list_length (list_t* ll)
{
    int ret;

    if (!ll) {errno = EINVAL; return errlogargs();}

    LIST_RDLOCK(ll);
    ret = _list_length(ll);
    LIST_UNLOCK(ll);

    return ret;
}

list_node_t* list_head (list_t* ll)
{
    list_node_t* ret;

    if (!ll) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ll);
    ret = _list_head(ll);
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}

void*        list_head_val ( list_t* ll)
{
    list_node_t* node;
    void*        ret;

    if ( ll == NULL ) { errno = EINVAL; return errlogargs_null(); }

    LIST_RDLOCK_NULL(ll);
    node = _list_head( ll );
    ret  = ( node == NULL ) ? NULL : _list_node_val( node );
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}


list_node_t* list_tail (list_t* ll)
{
    list_node_t* ret;

    if (!ll) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ll);
    ret = _list_tail(ll);
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}

void*        list_tail_val ( list_t* ll)
{
    list_node_t* node;
    void*        ret;

    if ( ll == NULL ) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ll);
    node = _list_tail( ll );
    ret  = ( node == NULL ) ? NULL : _list_node_val( node );
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}

list_node_t* list_node_next (list_node_t* ln)
{
    list_node_t* ret;

    if (!ln) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ln->mylist);
    ret = _list_node_next(ln);
    LIST_UNLOCK_NULL(ln->mylist);
    
    return ret;
}

list_node_t* list_node_prev (list_node_t* ln)
{
    list_node_t* ret;

    if (!ln) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ln->mylist);
    ret = _list_node_prev(ln);
    LIST_UNLOCK_NULL(ln->mylist);
    
    return ret;
}

void*        list_node_val  (list_node_t* ln)
{
    void* ret;

    if (!ln) {errno = EINVAL; return errlogargs_null();}

    LIST_RDLOCK_NULL(ln->mylist);
    ret = _list_node_val(ln);
    LIST_UNLOCK_NULL(ln->mylist);
    
    return ret;
}

int          list_node_val_set  (list_node_t* ln, void* val)
{
    if (!ln) {errno = EINVAL; return errlogargs();}
    if (!val && !(ln->mylist->flags & LIST_FLAG_ALLOW_NULL))
        return errlog(ERR_CRITICAL,"Null not allowed\n");
    
    LIST_WRLOCK(ln->mylist);
    ln->val = val;
    LIST_UNLOCK(ln->mylist);
    
    return 0;
}

list_node_t* list_add_head (list_t* ll, void* val)
{
    list_node_t* ret;
    if (!ll) {errno = EINVAL; return errlogargs_null();}
    if (!val && !(ll->flags & LIST_FLAG_ALLOW_NULL))
        return errlog_null(ERR_CRITICAL,"Null not allowed\n");
    
    LIST_WRLOCK_NULL(ll);
    ret = _list_add_head(ll,val);
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}

list_node_t* list_add_tail (list_t* ll, void* val)
{
    list_node_t* ret;
    if (!ll) {errno = EINVAL; return errlogargs_null();}
    if (!val && !(ll->flags & LIST_FLAG_ALLOW_NULL))
        return errlog_null(ERR_CRITICAL,"Null not allowed\n");

    LIST_WRLOCK_NULL(ll);
    ret = _list_add_tail(ll,val);
    LIST_UNLOCK_NULL(ll);
    
    return ret;
}

list_node_t* list_add_before ( list_t* ll, list_node_t* beforeme, void* val )
{
    list_node_t* ret;
    if ( !ll ) {errno = EINVAL; return errlogargs_null();}
    if ( !val && !( ll->flags & LIST_FLAG_ALLOW_NULL ))
        return errlog_null(ERR_CRITICAL,"Null not allowed\n");

    LIST_WRLOCK_NULL( ll );
    ret = _list_add_before( ll, beforeme, val );
    LIST_UNLOCK_NULL( ll );
    
    return ret;
}

list_node_t* list_add_after  ( list_t* ll, list_node_t* afterme, void* val )
{
    list_node_t* ret;
    if ( !ll ) {errno = EINVAL; return errlogargs_null();}
    if ( !val && !( ll->flags & LIST_FLAG_ALLOW_NULL ))
        return errlog_null(ERR_CRITICAL,"Null not allowed\n");

    LIST_WRLOCK_NULL( ll );
    ret = _list_add_after( ll, afterme, val );
    LIST_UNLOCK_NULL( ll );
    
    return ret;
}


int          list_move_head  (list_t* ll, list_node_t** node, void* val )
{
    int ret = 0;

    if ( ll == NULL || node == NULL) { errno = EINVAL; return errlogargs(); }
    if ( !val && !(ll->flags & LIST_FLAG_ALLOW_NULL)) {
        return errlog(ERR_CRITICAL,"Null not allowed.\n");
    }
    
    LIST_WRLOCK( ll );
    if ( *node == NULL ) {
        return errlog(ERR_CRITICAL,"Attempting to move a NULL node\n");
    }

    /* If the item is already at the head just return */
    if ( _list_head ( ll ) == *node ) {
        LIST_UNLOCK( ll );
        return 0;
    }

    /* Remove the item first */
    /* Don't free the value, if replacing with the same one */
    if ( ll->flags & LIST_FLAG_FREE_VAL && _list_node_val ( *node ) == val ) {
        (*node)->val = NULL;
    }
    
    ret = _list_remove ( ll, *node );
    
    /* Add the item to the head (Only if there wasn't an error) */
    if ( (ret == 0 ) && ((*node = _list_add_head(ll,val)) == NULL)) ret = -1;
    LIST_UNLOCK( ll );
    
    return ret;
}

int          list_move_tail  (list_t* ll, list_node_t** node, void* val )
{
    int ret = 0;

    if ( ll == NULL || node == NULL) { errno = EINVAL; return errlogargs(); }
    if ( !val && !(ll->flags & LIST_FLAG_ALLOW_NULL)) {
        return errlog(ERR_CRITICAL,"Null not allowed.\n");
    }

    LIST_WRLOCK(ll);
    if ( *node == NULL ) {
        return errlog(ERR_CRITICAL,"Attempting to move a NULL node\n");
    }

    /* If the item is already at the tail just return */
    if ( _list_tail ( ll ) == *node ) {
        LIST_UNLOCK( ll );
        return 0;
    }

    /* Remove the item first */
    /* Don't free the value, if replacing with the same one */
    if ( ll->flags & LIST_FLAG_FREE_VAL && _list_node_val ( *node ) == val ) {
        (*node)->val = NULL;
    }
    
    ret = _list_remove ( ll, *node );
        
    /* Add the item to the tail (Only if there wasn't an error) */
    if ( (ret == 0 ) && ((*node = _list_add_tail(ll,val)) == NULL)) ret = -1;
    LIST_UNLOCK( ll );
    
    return ret;
}

int          list_remove (list_t* ll, list_node_t* ln)
{
    int ret;
    if (!ll || !ln) 
    {errno = EINVAL; return errlogargs();}

    LIST_WRLOCK(ll);
    ret = _list_remove(ll,ln);
    LIST_UNLOCK(ll);
    
    return ret;
}

int          list_remove_all (list_t* ll)
{
    list_node_t* node;
    list_node_t* next;
    int ret = -1;

    if (!ll) {errno = EINVAL; return errlogargs();}

    LIST_WRLOCK(ll);
    for (node = _list_head(ll); node; node = next) {
        next = _list_node_next(node);
        ret = _list_remove(ll,node);
    }
    LIST_UNLOCK(ll);
    
    return ret;
}

int          list_remove_val       (list_t* ll, void* val)
{
    list_node_t* node;
    int ret = -1;
    
    if (!ll) {errno = EINVAL; return errlogargs();}
    if (!val && !(ll->flags & LIST_FLAG_ALLOW_NULL))
        return errlog(ERR_CRITICAL,"Null not allowed\n");

    LIST_WRLOCK(ll);
    for (node = _list_head(ll); node; node = _list_node_next(node)) 
        if (_list_node_val(node) == val) {
            ret = _list_remove(ll,node);
            break;
        }
    LIST_UNLOCK(ll);

    return ret;
}

int          list_remove_val_dups  (list_t* ll, void* val)
{
    list_node_t* node;
    list_node_t* next;
    int ret = -1;

    if (!ll) {errno = EINVAL; return errlogargs();}
    if (!val && !(ll->flags & LIST_FLAG_ALLOW_NULL))
        return errlog(ERR_CRITICAL,"Null not allowed\n");

    LIST_WRLOCK(ll);
    for (node = _list_head(ll); node; node = next) {
        next = _list_node_next(node);
        if (_list_node_val(node) == val) 
            ret = _list_remove(ll,node);
    }
    LIST_UNLOCK(ll);

    return ret;

}

int          list_pop_head   ( list_t* ll, void** val )
{
    list_node_t* head;
    int          ret;

    if ( ll == NULL || val == NULL ) { errno = EINVAL; return errlogargs(); }

    LIST_WRLOCK ( ll );
    
    head = _list_head ( ll );
    
    if ( head != NULL ) {
        *val = _list_node_val(head);

        /* Do not free the value on removal */
        if ( ll->flags & LIST_FLAG_FREE_VAL ) head->val = NULL;
        
        ret = _list_remove ( ll, head );        
    }
    
    LIST_UNLOCK ( ll );

    return ret;
}

int          list_pop_tail   ( list_t* ll, void** val )
{
    list_node_t* tail;
    int          ret;

    if ( ll == NULL || val == NULL ) { errno = EINVAL; return errlogargs(); }

    LIST_WRLOCK ( ll );
    
    tail = _list_tail ( ll );
    
    if ( tail != NULL ) {
        *val = _list_node_val( tail );

        /* Do not free the value on removal */
        if ( ll->flags & LIST_FLAG_FREE_VAL ) tail->val = NULL;
        
        ret = _list_remove ( ll, tail );
    }
    
    LIST_UNLOCK ( ll );

    return ret;
}

int          list_apply (list_t* ll, list_apply_func_t func)
{
    int ret;
    if (!ll) {errno = EINVAL; return errlogargs();}

    LIST_WRLOCK(ll);
    ret = _list_apply(ll,func);
    LIST_UNLOCK(ll);
    
    return ret;
}

list_t*      list_dup (list_t* src)
{
    list_t* dst = list_create(0);
    list_node_t* node;
    int i;
    
    if (!dst)
        return NULL;

    LIST_WRLOCK_NR(dst);
    LIST_WRLOCK_NR(src);

    dst->flags  = src->flags;

    for (i=0, node = _list_head(src); i<_list_length(src) && node; i++, node = _list_node_next(node)) {
        if (!_list_add_tail(dst,node->val))
            errlog(ERR_WARNING,"list_dup missed element\n");
    }

    LIST_UNLOCK_NR(dst);
    LIST_UNLOCK_NR(src);

    return dst;
}

int          list_contains  (list_t* ll, void* val)
{
    list_node_t* node;

    if (!ll) {errno = EINVAL; return errlogargs();}

    LIST_WRLOCK(ll);
    for (node = _list_head(ll); node; node = _list_node_next(node)) {
        if (_list_node_val(node) == val) {
            LIST_UNLOCK(ll);
            return 1;
        }
    }
    LIST_UNLOCK(ll);

    return 0;
}



static int          _list_destroy (list_t* ll)
{
    int num = 0;
    list_node_t* node;
    list_node_t* next;

    for (node = _list_head(ll); node; node = next) {
        next = _list_node_next(node);
        if(_list_remove(ll,node)<0)
            errlog(ERR_WARNING,"list_remove failed in destroy\n");
        num++;
    }

    LIST_UNLOCK(ll);
    if (pthread_rwlock_destroy(&ll->lock)<0)
        return errlog(ERR_WARNING,"lock_destroy failed\n");

    return num;
}

static int          _list_length (list_t* ll)
{
    return ll->length;
}

static list_node_t* _list_head (list_t* ll)
{
    return ll->head;
}

static list_node_t* _list_tail (list_t* ll)
{
    return ll->tail;
}

static list_node_t* _list_node_next (list_node_t* ln)
{
    return ln->next;
}

static list_node_t* _list_node_prev (list_node_t* ln)
{
    return ln->prev;
}

static void*        _list_node_val  (list_node_t* ln)
{
    return ln->val;
}

static list_node_t* _list_add_head (list_t* ll, void* val)
{
    return _list_add_before(ll,ll->head,val);
}

static list_node_t* _list_add_tail (list_t* ll, void* val)
{
    return _list_add_after(ll,ll->tail,val);
}

static int          _list_remove (list_t* ll, list_node_t* ln)
{
    list_node_t* next = NULL;
    list_node_t* prev = NULL;

    if (ln->mylist != ll)
        return errlog(ERR_CRITICAL,"Node from different list\n");
    
    next = ln->next;
    prev = ln->prev;
    
    if (next) 
        next->prev = prev;
    if (prev) 
        prev->next = next;
    
    if (ll->length == 1) {
        /* last element */
        ll->head = NULL;
        ll->tail = NULL;
    }
    else {
        if (ll->head == ln)
            ll->head = next;
        if (ll->tail == ln)
            ll->tail = prev;
    }
    
    if (ll->flags & LIST_FLAG_FREE_VAL && (ln->val !=NULL ))
        free(ln->val);
    free(ln);
    
    ll->length--;
    if (ll->length < 0)
        return errlog(ERR_CRITICAL,"Negative Length\n");
    
    return 0;
}

static int          _list_apply (list_t* ll, list_apply_func_t func)
{
    list_node_t* node;
    
    for (node = _list_head(ll);node;node=_list_node_next(node)) 
        func(_list_node_val(node));

    return 0;
}

static list_node_t* _list_add_before (list_t* ll, list_node_t* beforeme, void* val)
{
    list_node_t* ln = _list_node_create(val,ll);
    list_node_t* next = NULL;
    list_node_t* prev = NULL;
    
    if (!ln) 
        return NULL;

    if (ll->length == 0) {
        ll->head = ln;
        ll->tail = ln;

        if (ll->flags & LIST_FLAG_CIRCULAR) {
            ln->next = ln;
            ln->prev = ln;
        } else {
            ln->next = NULL;
            ln->prev = NULL;
        }
    }
    else {
        next = beforeme;
        if (next)
            prev = next->prev;

        ln->next = next;
        ln->prev = prev;
    
        if (next) 
            next->prev = ln;
        if (prev) 
            prev->next = ln;
        if (next == ll->head)
            ll->head = ln;
    }
    
    ll->length++;
    return ln;
}

static list_node_t* _list_add_after (list_t* ll, list_node_t* afterme, void* val)
{
    list_node_t* ln = _list_node_create(val,ll);
    list_node_t* next = NULL;
    list_node_t* prev = NULL;
    
    if (!ln) 
        return NULL;

    if (ll->length == 0) {
        ll->head = ln;
        ll->tail = ln;

        if (ll->flags & LIST_FLAG_CIRCULAR) {
            ln->next = ln;
            ln->prev = ln;
        } else {
            ln->next = NULL;
            ln->prev = NULL;
        }
    }
    else {
        prev = afterme;
        if (prev)
            next = prev->next;

        ln->next = next;
        ln->prev = prev;
    
        if (next) 
            next->prev = ln;
        if (prev) 
            prev->next = ln;
        if (prev == ll->tail)
            ll->tail = ln;
    }
    
    ll->length++;
    return ln;
}

static list_node_t* _list_node_create (void* val, list_t* mylist)
{
    list_node_t* ln = calloc(1,sizeof(list_node_t));
    if (!ln) return errlogmalloc_null(); 
    
    ln->val = val;
    ln->mylist = mylist;
    
    return ln;
}
