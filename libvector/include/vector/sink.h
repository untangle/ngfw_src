/**
 * $Id$
 */
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
