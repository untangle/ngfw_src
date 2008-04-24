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
#ifndef __MVPOLL_H_
#define __MVPOLL_H_

#include <pthread.h>

#include <sys/types.h>
#include <sys/epoll.h>
#include "list.h"
#include "hash.h"

#define MVPOLL_CTL_ADD 1
#define MVPOLL_CTL_DEL 2
#define MVPOLL_CTL_MOD 3

enum MVPOLL_EVENTS {
    MVPOLLIN = 0x001,
#define MVPOLLIN MVPOLLIN
    MVPOLLOUT = 0x004,
#define MVPOLLOUT MVPOLLOUT
    MVPOLLERR = 0x008,
#define MVPOLLERR MVPOLLERR
    MVPOLLHUP = 0x010,
#define MVPOLLHUP MVPOLLHUP
};

typedef struct mvpoll*           mvpoll_id_t;
typedef struct mvpoll            mvpoll_t;
typedef struct mvpoll_event      mvpoll_event_t;
typedef struct mvpoll_key        mvpoll_key_t;
typedef u_int32_t                eventmask_t;
typedef enum   mvpoll_key_type   {
    mvpoll_key_type_fd = 1,
    /**
     * anything greater than type_fd is a userspace event
     * the user can use any integer if they wish to tell the difference
     */
    mvpoll_key_type_userspace
} mvpoll_key_type_t;

/**
 * The mvpoll struct,
 * This is equivalent to the epoll FD in epoll
 */
struct mvpoll {
    list_t    rdy;

    list_t    keys;

    ht_t      keystate_table; 

    int       epfd;

    int       notify_pipe[2];
#define       event_fd notify_pipe[0]

    pthread_mutex_t mutex;
    
    int (*notify_status) (mvpoll_t* mvp, mvpoll_key_t* key, int evstate);
};

/**
 * The basic mvpoll_key, all keys must start like this
 * This is equivalent an FD in epoll
 */
struct mvpoll_key {
    /**
     * This function must return the currect event mask of the resource
     */
    eventmask_t (*poll) (mvpoll_key_t* key);
    /**
     * This is function is called in mvpoll_key_raze.
     * Users implementing their own keys should put a function
     * here that frees any additional resources.  This must destroy
     * additional resources, but not the key itself.
     * Otherwise it can be null
     */
    int         (*special_destroy) (mvpoll_key_t* key);
    /**
     * The type must be unique for every key type.
     * 1 is an FD Key
     * 100-200 is reserved for keys in mvutil
     * the rest are free to be used by the user
     */
    mvpoll_key_type_t type;
    /**
     * Free for use by the user, can be assigned for FD key if you wish
     */
    void* arg;
    /**
     * Data, is generic storage place for a key
     * for an FD Key it stores the file descriptor.
     * for other keys can use it as they wish
     * (usually a pointer to the resource or unused)
     */
    union epoll_data data;
    /**
     * List of observers, all keys must keep track of observers for notify
     */
    list_t observers;
    
    /**
     * The event mask returned for this key for this TICK.  This value should
     * ONLY be used to determine vector received a HUP or ERR on this key.
     */
    eventmask_t events;
};

/**
 * This represents an event itself
 */
struct mvpoll_event {
    eventmask_t events;
    mvpoll_key_t* key;
};

mvpoll_id_t mvpoll_create (int size);
int         mvpoll_raze (mvpoll_id_t);

int         mvpoll_wait (mvpoll_id_t mvp, struct mvpoll_event* events, int maxevents, int timeout);
int         mvpoll_ctl (mvpoll_id_t mvp, int op, struct mvpoll_key* key, eventmask_t events);
int         mvpoll_raze (mvpoll_id_t mvp);
list_t*     mvpoll_get_keylist (mvpoll_id_t mvp);
int         mvpoll_key_exists (mvpoll_id_t mvp, struct mvpoll_key* key);


int           mvpoll_key_fd_init   ( mvpoll_key_t* key, int fd );
mvpoll_key_t* mvpoll_key_fd_create (int fd);

int           mvpoll_key_base_init   ( mvpoll_key_t* key );
mvpoll_key_t* mvpoll_key_base_create (void);


int  mvpoll_key_register_observer (mvpoll_key_t* key, mvpoll_t* mvp);
int  mvpoll_key_unregister_observer (mvpoll_key_t* key, mvpoll_t* mvp);
void mvpoll_key_notify_observers (mvpoll_key_t* key, eventmask_t state);
int  mvpoll_key_expire (mvpoll_key_t* key);
int  mvpoll_key_destroy (mvpoll_key_t* key);
int  mvpoll_key_raze (mvpoll_key_t* key);


#endif
