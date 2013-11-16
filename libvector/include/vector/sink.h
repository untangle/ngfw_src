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
/* @author: Dirk Morris <dmorris@untangle.com> */
#ifndef __SINK_H
#define __SINK_H

#include <mvutil/mvpoll.h>
#include "event.h"

#define _EVENT_ACTION_ERROR    -1
#define _EVENT_ACTION_NOTHING   0
#define _EVENT_ACTION_DEQUEUE   1
#define _EVENT_ACTION_SHUTDOWN  2

typedef enum {
    EVENT_ACTION_ERROR    = _EVENT_ACTION_ERROR, /* will shutdown sink and free event */
    EVENT_ACTION_NOTHING  = _EVENT_ACTION_NOTHING,
    EVENT_ACTION_DEQUEUE  = _EVENT_ACTION_DEQUEUE, /* will dequeue and free event */
    EVENT_ACTION_SHUTDOWN = _EVENT_ACTION_SHUTDOWN /* will shutdown sink and free event */
} event_action_t;

#define ALWAYS_WRITABLE_FD -2

typedef struct sink {
    event_action_t (*send_event) (struct sink* snk, event_t* event);
    mvpoll_key_t*  (*get_event_key) (struct sink* snk);
    int            (*shutdown) (struct sink* snk);
    void           (*raze) (struct sink* snk);
} sink_t;

#endif
