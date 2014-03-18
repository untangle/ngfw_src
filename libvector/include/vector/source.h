/**
 * $Id$
 */
#ifndef __SOURCE_H
#define __SOURCE_H

#include <mvutil/mvpoll.h>
#include "event.h"
#include "sink.h"

/**
 * Source defines a "source" for events, which can be back by a file descriptor or function
 *
 * get_event defines a function to get an event from the source
 ** Note: the intended sink is provided as a argument so the splice() optimization can take place
 * get_event_key gets the mvpoll key for the source so mvpoll knows when it is readable
 * shutdown shuts down the source
 * raze kills the source and frees its resources
 */
typedef struct source {
    event_t*      (*get_event) ( struct source* src, struct sink* snk );
    mvpoll_key_t* (*get_event_key) ( struct source* src );
    int           (*shutdown) ( struct source* src );
    void          (*raze) ( struct source* src );
} source_t;

#endif
