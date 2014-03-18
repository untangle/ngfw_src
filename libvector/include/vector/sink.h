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


/**
 * Sink defines a "sink" for events, which can be back by a file descriptor or event handler
 *
 * send_event defines a function that will be called when a new event is ready to be handled.
 * get_event_key gets the mvpoll key for the sink so mvpoll knows when it is writable
 * shutdown shuts down the sink
 * raze kills the sink and frees its resources
 */
typedef struct sink {
    event_action_t (*send_event) ( struct sink* snk, event_t* event );
    mvpoll_key_t*  (*get_event_key) ( struct sink* snk );
    int            (*shutdown) ( struct sink* snk );
    void           (*raze) ( struct sink* snk );
} sink_t;

#endif
