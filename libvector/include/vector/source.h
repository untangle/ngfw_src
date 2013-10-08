/**
 * $Id: source.h 35573 2013-08-08 19:43:35Z dmorris $
 */
#ifndef __SOURCE_H
#define __SOURCE_H

#include <mvutil/mvpoll.h>
#include "event.h"

typedef struct source {
    event_t*      (*get_event) (struct source* src);
    mvpoll_key_t* (*get_event_key) (struct source* src);
    int           (*shutdown) (struct source* src);
    void          (*raze) (struct source* src);
} source_t;

#endif
