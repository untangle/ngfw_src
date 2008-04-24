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
#include "mvutil/mvpoll.h"

#include <pthread.h>

#include <stdlib.h>
#include <unistd.h>
#include "mvutil/hash.h"
#include "mvutil/list.h"
#include "mvutil/unet.h"
#include "mvutil/errlog.h"
#include "mvutil/debug.h"

/**
 * This is a (set x key) storage struct
 * It is stored in the keystate table
 */
typedef struct mvpoll_keystate {
    /**
     * The set for this keystate
     */
    mvpoll_t* mvp;

    /**
     * The key for this keystate
     */
    mvpoll_key_t* key;
    
    /**
     * This stores the listening event mask of this userspace key
     */
    eventmask_t eventmask;

    /**
     * This points to a pertaining event in the rdy list if it exist
     * otherwise it is null
     */
    list_node_t* node;

    /**
     * The event struct for this key, we don't dynamically allocate these
     * since there can only be one per key
     */
    mvpoll_event_t event;
} mvpoll_keystate_t;

static int _mvpoll_collect_events ( mvpoll_t* mvp, mvpoll_event_t* event, int maxevent, int timeout );
static int _mvpoll_notify_status (mvpoll_t* mvp, mvpoll_key_t* key, int event);
static int _mvpoll_update_status (mvpoll_t* mvp, mvpoll_keystate_t* keystate, int evstate);
static int _mvpoll_ctl_add (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event);
static int _mvpoll_ctl_del (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event);
static int _mvpoll_ctl_mod (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event);
static int _mvpoll_clear_event_fd (mvpoll_t* mvp);
static int _mvpoll_wake (mvpoll_t* mvp);
static u_long  _key_hash_func (const void* input);
static u_char _key_equ_func (const void* input,const void* input2);
static eventmask_t _null_poll (mvpoll_key_t* key);
static char* MVPOLL_CTL_STR[] = { "NONE", "ADD" , "DEL" , "MOD" , "MAX" };


mvpoll_id_t mvpoll_create (int size)
{
    mvpoll_t* mvp;
    struct epoll_event ev;

    if (( mvp = malloc( sizeof( mvpoll_t ))) == NULL ) return errlogmalloc_null();
    
    if ((mvp->epfd = epoll_create(256))<0)
        return perrlog_null("epoll_create");
    debug(8,"MVPOLL: epoll_create: %i\n",mvp->epfd);
    
    if (list_init(&mvp->keys,0)<0)
        return perrlog_null("list_init");
    if (list_init(&mvp->rdy,0)<0)
        return perrlog_null("list_init");

    mvp->notify_status = _mvpoll_notify_status;

    if (pipe(mvp->notify_pipe)<0)
        return perrlog_null("pipe");

    memset(&ev,0,sizeof(ev));
    ev.events   = EPOLLIN;
    ev.data.fd  = mvp->event_fd;
    if (ev.data.fd < 0)
        return perrlog_null("pipe");
        
    if (ht_init(&mvp->keystate_table,size+1,_key_hash_func,_key_equ_func,HASH_FLAG_NO_LOCKS|HASH_FLAG_KEEP_LIST)<0)
        return perrlog_null("hash_init");

    debug(8,"MVPOLL: Adding msg pipe, epoll_ctl(%i,%s,%i,0x%08x)\n",mvp->epfd,MVPOLL_CTL_STR[MVPOLL_CTL_ADD],ev.data.fd,EPOLLIN);
    if (epoll_ctl(mvp->epfd,EPOLL_CTL_ADD,ev.data.fd,&ev)<0)
        return perrlog_null("epoll_ctl");

    if ( pthread_mutex_init( &mvp->mutex, NULL ) < 0 ) return perrlog_null( "pthread_mutex_init\n" );
    
    return mvp;
}

int         mvpoll_wait (mvpoll_id_t mvp, mvpoll_event_t* event, int maxevent, int timeout)
{
    if (!mvp || !event || !maxevent)
        return errlogargs();

    /**
     * Collect events and place them into the event array
     */
    return _mvpoll_collect_events ( mvp, event, maxevent, timeout );
}

int         mvpoll_ctl (mvpoll_id_t mvp, int op, mvpoll_key_t* key, eventmask_t event)
{
    int ret;
    
    if (!mvp || !key)
        return errlogargs();

    switch (op) {
    case MVPOLL_CTL_ADD:
        ret = _mvpoll_ctl_add(mvp,key,event);
        break;
    case MVPOLL_CTL_DEL:
        ret = _mvpoll_ctl_del(mvp,key,event);
        break;
    case MVPOLL_CTL_MOD:
        ret = _mvpoll_ctl_mod(mvp,key,event);
        break;
    default:
        return errlogargs();
    }

    if (key->type == mvpoll_key_type_fd) {
        struct epoll_event ev;

        memset(&ev,0,sizeof(ev));
        ev.data.ptr = key;
        ev.events = event;
        
        debug(8,"MVPOLL: epoll_ctl(%i,%s,%i,0x%08x)\n",mvp->epfd,MVPOLL_CTL_STR[op],key->data,event);
        if (epoll_ctl(mvp->epfd,op,key->data.fd,&ev)<0)
            ret = perrlog("epoll_ctl");
    }

    return ret;
}

int         mvpoll_raze (mvpoll_id_t mvp)
{
    list_node_t* step;
    
    if (!mvp)
        return errlogargs();

    for (step = list_head(&mvp->keys) ; step ; step = list_node_next(step)) {
        mvpoll_key_t* key = (mvpoll_key_t*)list_node_val(step);
        mvpoll_key_unregister_observer(key,mvp);
    }

    if (close(mvp->epfd)<0)
        perrlog("close");
    
    if (list_destroy(&mvp->keys)<0)
        perrlog("list_destroy");
    if (list_destroy(&mvp->rdy)<0)
        perrlog("list_destroy");
    if (ht_destroy(&mvp->keystate_table)<0)
        perrlog("ht_destroy");
    if (( mvp->notify_pipe[0] >= 0 ) && ( close( mvp->notify_pipe[0] ) < 0 ))
        perrlog("close");
    if (( mvp->notify_pipe[1] >= 0 ) && ( close( mvp->notify_pipe[1] ) < 0 ))
        perrlog("close");
            
    free(mvp);

    return 0;
}

list_t*     mvpoll_get_keylist (mvpoll_id_t mvp)
{
    if (!mvp)
        return errlogargs_null();
    
    return ht_get_key_list(&mvp->keystate_table);
}

int         mvpoll_key_exists (mvpoll_id_t mvp, struct mvpoll_key* key)
{
    if (!mvp || !key)
        return errlogargs();
                      
    if (ht_lookup(&mvp->keystate_table,key))
        return 1;
    else
        return 0;
}

int           mvpoll_key_base_init   ( mvpoll_key_t* key )
{
    if ( key == NULL )
        return errlogargs();

    if (list_init(&key->observers,0)<0)
        return perrlog("list_init");

    key->type            = 0;
    key->special_destroy = NULL;
    key->poll            = _null_poll;
    
    return 0;
}


mvpoll_key_t* mvpoll_key_base_create ()
{
    mvpoll_key_t* key = malloc(sizeof(mvpoll_key_t));

    if (!key)
        return errlogmalloc_null();

    if ( mvpoll_key_base_init( key ) < 0 )
        return errlog_null( ERR_CRITICAL, "mvpoll_key_base_init\n" );

    return (mvpoll_key_t*)key;
}

int           mvpoll_key_fd_init   ( mvpoll_key_t* key, int fd )
{
    if ( key == NULL )
        return errlogargs();

    if (list_init(&key->observers,0)<0)
        return perrlog("list_init");

    key->type            = mvpoll_key_type_fd;
    key->special_destroy = NULL;
    key->poll            = _null_poll;
    key->data.fd         = fd;
    key->arg             = NULL;
    
    return 0;
}


mvpoll_key_t* mvpoll_key_fd_create (int fd)
{
    mvpoll_key_t* key = malloc(sizeof(mvpoll_key_t));

    if (!key)
        return errlogmalloc_null();

    if ( mvpoll_key_fd_init( key, fd ) < 0 )
        return errlog_null( ERR_CRITICAL, "mvpoll_key_fd_init\n" );

    return (mvpoll_key_t*)key;
}

int  mvpoll_key_register_observer (mvpoll_key_t* key, mvpoll_t* mvp)
{
    if (!key || !mvp)
        return errlogargs();

    if (list_add_tail(&key->observers,mvp)<0)
        return perrlog("list_add");

    return 0;
}

int  mvpoll_key_unregister_observer (mvpoll_key_t* key, mvpoll_t* mvp)
{
    if (!key || !mvp)
        return errlogargs();

    if (list_remove_val(&key->observers,mvp)<0)
        return perrlog("list_add");

    return 0;
}

void mvpoll_key_notify_observers (mvpoll_key_t* key, eventmask_t state)
{
    list_node_t* step;

    for (step = list_head(&key->observers) ; step ; step = list_node_next(step)) {
        mvpoll_t* mvp = (mvpoll_t*) list_node_val(step);

        if (!mvp || !mvp->notify_status)
            errlogcons();
        else
            mvp->notify_status(mvp,key,state);
    }

    return;
}

int  mvpoll_key_expire (mvpoll_key_t* key)
{
    list_node_t* step;
    list_node_t* next;
    int ret = 0;
    
    if (!key)
        return errlogargs();

    for (step = list_head(&key->observers) ; step ; step = next) {
        mvpoll_t* mvp = (mvpoll_t*) list_node_val(step);

        next = list_node_next(step);

        if (mvp) {
            if (mvpoll_ctl(mvp,MVPOLL_CTL_DEL,key,0)<0)
                perrlog("mvpoll_ctl");
        }
        else {
            ret = errlogcons();
        }
    }

    return ret;
}

int  mvpoll_key_destroy (mvpoll_key_t* key)
{
    int ret = 0;

    if (!key) 
        return errlogargs();

    if (key->special_destroy)
        ret += key->special_destroy(key);
    
    ret += mvpoll_key_expire(key);

    if (list_destroy(&key->observers)<0)
        ret += perrlog("list_destroy");

    return ret;
}

int  mvpoll_key_raze (mvpoll_key_t* key)
{
    int ret;

    if (!key) 
        return errlogargs();

    if (( ret = mvpoll_key_destroy( key )) < 0 ) errlog( ERR_CRITICAL, "mvpoll_key_destroy\n" );

    free(key);

    return ret;
}



/**
 * This collects all epoll and userspace events into the event array
 */
static int _mvpoll_collect_events ( mvpoll_t* mvp, mvpoll_event_t* event, int maxevent, int timeout )
{
    int if_rdy = 0;
    int evcount,step,evstep=0;
    int maxkeys = list_length(&mvp->keys);  /* FIXME are fd's in keys? */
    struct epoll_event localevent[maxkeys+1]; /* +1 for mailbox */
    
    debug(10,"MVPOLL: epoll_wait(epfd=%i,timeout=%i)\n",mvp->epfd,timeout);

    if ( list_length( &mvp->rdy ) > 0 ) {
        timeout = 0;
        if_rdy  = 1;
    }

    if ((evcount = epoll_wait(mvp->epfd,localevent,maxkeys+1,timeout))<0)
        perrlog("epoll_wait");
    else {
        for ( step=0 ; step<evcount && evstep<maxevent ; step++ ) {

            if (localevent[step].data.fd != mvp->event_fd) {
                /**
                 * an fd event, copy it to the event
                 */
                event[evstep].events    = (uint32_t) localevent[step].events;
                event[evstep].key  = (mvpoll_key_t*) localevent[step].data.ptr;
                evstep++;
            }
            else {
                /* An event on the ready list, indicate to copy out the events */
                if_rdy = 1;
            }
        }
    }
    
    /**
     * theres stuff in the mailbox, copy to event
     */
    if ( if_rdy == 1 ) {
        mvpoll_event_t* ev;
        list_node_t* node_step;
        
        if ( pthread_mutex_lock( &mvp->mutex ) < 0 ) perrlog( "pthread_mutex_lock\n" );

        for ( node_step = list_head(&mvp->rdy) ; node_step ; node_step = list_node_next(node_step)) {
            if (!(ev = list_node_val(node_step)))
                errlogcons();
            else {
                event[evstep].events   = ev->events;
                event[evstep].key = ev->key;
                evstep++;
            }
        }

        if ( pthread_mutex_unlock( &mvp->mutex ) < 0 ) perrlog( "pthread_mutex_unlock\n" );

    }

    return evstep;
}

/**
 * This is the callback userspace event sources call
 */
static int _mvpoll_notify_status (mvpoll_t* mvp, mvpoll_key_t* key, int evstate)
{
    mvpoll_keystate_t* keystate;
    
    if (!mvp || !key)
        return errlogargs();

    if (!(keystate = ht_lookup(&mvp->keystate_table,key))) {
        return errlog(ERR_CRITICAL,"Invalid key\n");
    }

    return _mvpoll_update_status(mvp,keystate,evstate);
}

/**
 * This is updates the status of a key in case it has changed
 */
static int _mvpoll_update_status (mvpoll_t* mvp, mvpoll_keystate_t* keystate, int evstate)
{
    int _critical_section() {
        u_int32_t ev;
        
        if (!mvp || !keystate)
            return errlogargs();
        
        ev = (evstate & keystate->eventmask);
        
        debug(10,"MVPOLL: update_status (0x%08x) ev = 0x%08x, node = 0x%08x\n",mvp,ev,keystate->node);
        
        /**
         * If there is some event and its not in the list, add one
         */
        if (ev && !keystate->node) {
            
            keystate->event.key = keystate->key;
            keystate->event.events   = ev;
            
            /**
             * Add the event and wakeup mvp if necessary
             */
            if (!(keystate->node = list_add_tail(&mvp->rdy,&keystate->event)))
                return perrlog("mailbox_put");
            
            /**
             * If this is the first event, we need to send a wakeup signal
             */
            if (list_size(&mvp->rdy) == 1)
                _mvpoll_wake(mvp);
            
            debug(10,"MVPOLL: Add to rdy list\n");
            
            return 0;
        }
        /**
         * Else if there is no event and its in the list
         */
        else if  (!ev && keystate->node) {
            list_node_t* node = keystate->node;
            
            keystate->node = NULL;
            keystate->event.key = NULL;
            keystate->event.events   = 0;
            
            /**
             * Remove from the ready event list
             */
            if (list_remove(&mvp->rdy,node)<0)
                perrlog("list_remove");
            
            debug(10,"MVPOLL: Remove from rdy list\n");
            
            /**
             * If there are no ready events, clear the wakeup 
             */
            if (list_size(&mvp->rdy) == 0)
                _mvpoll_clear_event_fd(mvp);
            
        }
        /**
         * Else 1) its in the list and there is an event
         * or   2) its not in the list and there are no events
         * so just set events correctly for case 1
         */
        else {
            keystate->event.events = ev;
        }

        return 0;
    }
    
    int ret = 0;
    
    if ( pthread_mutex_lock( &mvp->mutex ) < 0 ) return perrlog( "pthread_mutex_lock\n" );
    ret = _critical_section();
    if ( pthread_mutex_unlock( &mvp->mutex ) < 0 ) return perrlog( "pthread_mutex_unlock\n" );

    return ret;
}

/**
 * MVPOLL_CTL_ADD handler
 * checks for duplicate
 * creates and adds a keystate,
 * updates the new status
 * registers as an observer of this key
 */
static int _mvpoll_ctl_add (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event)
{
    mvpoll_keystate_t* keystate;

    if (list_contains(&mvp->keys,key))
        return errlog(ERR_CRITICAL,"Duplicate key\n");

    if (!(keystate = malloc(sizeof(mvpoll_keystate_t)))) 
        return errlogmalloc();

    keystate->mvp = mvp;
    keystate->key = key;
    keystate->node = NULL;
    keystate->eventmask = event;
    keystate->event.events = 0;
    keystate->event.key = NULL;

    if (!list_add_tail(&mvp->keys,key))
        perrlog("list_add_tail");

    if (ht_add(&mvp->keystate_table,key,keystate)<0)
        perrlog("ht_add");

    _mvpoll_update_status(mvp,keystate,key->poll(key));

    if (mvpoll_key_register_observer(key,mvp)<0)
        perrlog("register_observer");

    debug(5,"MVPOLL: mvpoll_ctl(mvp: 0x%08x, ADD, key: 0x%08x, event: 0x%08x)\n",mvp,key,event);
    return 0;
}

/**
 * MVPOLL_CTL_DEL handler
 * checks for existence,
 * unregisters as an observer of this key
 * removes all status
 * removes key from list, keystate, and frees keystate
 */
static int _mvpoll_ctl_del (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event)
{
    mvpoll_keystate_t* keystate;

    if (!(keystate = ht_lookup(&mvp->keystate_table,key)))
        return errlog(ERR_CRITICAL,"Key not found\n");
    if (!list_contains(&mvp->keys,key))
        return errlog(ERR_CRITICAL,"Key not found\n");

    if (mvpoll_key_unregister_observer(key,mvp)<0)
        perrlog("register_observer");

    _mvpoll_update_status(mvp,keystate,0);

    if (list_remove_val(&mvp->keys,key)<0)
        perrlog("list_remove_val");
    if (ht_remove(&mvp->keystate_table,key)<0)
        perrlog("ht_remove");
    free(keystate);

    debug(5,"MVPOLL: mvpoll_ctl(mvp: 0x%08x, DEL, key: 0x%08x, event: 0x%08x)\n",mvp,key,event);
    return 0;
}

/**
 * MVPOLL_CTL_MOD
 * modifies that status of this key
 */
static int _mvpoll_ctl_mod (mvpoll_t* mvp, mvpoll_key_t* key, eventmask_t event)
{
    mvpoll_keystate_t* keystate;
    eventmask_t poll_event;

    if (!(keystate = ht_lookup(&mvp->keystate_table,key)))
        return errlog(ERR_CRITICAL,"Key not found\n");

    keystate->eventmask = event;
    poll_event = key->poll( key );
    _mvpoll_update_status( mvp, keystate, poll_event );

    debug(5,"MVPOLL: mvpoll_ctl(mvp: 0x%08x, MOD, key: 0x%08x, eventmask: 0x%08x, event: 0x%08x)\n",
          mvp, key, event, poll_event );
    return 0;
}

/**
 * writes to notify pipe so epoll_wait will be non-blocking
 * and it will awaken if asleep
 */
static int _mvpoll_wake (mvpoll_t* mvp)
{
    char a = 'x';
    if (write(mvp->notify_pipe[1],&a,sizeof(a))<1)
        perrlog("write");
    return 0;
}

/**
 * empties the notify pipe, so epoll_wait will block
 */
static int _mvpoll_clear_event_fd (mvpoll_t* mvp)
{
    char buf[50];
    while(unet_read_timeout(mvp->notify_pipe[0],buf,50,0)>0);
    return 0;
}

/**
 * key hash function
 */
static u_long  _key_hash_func (const void* input)
{
    mvpoll_key_t* key = (mvpoll_key_t*)input;
    return (u_long)key ^ (u_long)key->type ^ (u_long)key->poll;
}

/**
 * key equal function - not that the address must be the same
 */
static u_char _key_equ_func (const void* input,const void* input2)
{
    if (input == input2) return 1;
    else return 0;
}

/**
 * fd key poll function
 */
static eventmask_t _null_poll (mvpoll_key_t* key)
{
    // errlog(ERR_CRITICAL,"Null function called\n");
    return 0;
}

