/**
 * $Id$
 */
#ifndef __FD_SINK_H
#define __FD_SINK_H

#include <netinet/in.h>
#include "sink.h"
#include "event.h"

typedef struct fd_sink {

    sink_t base;

    int fd;

    mvpoll_key_t* key;
    
} fd_sink_t;

sink_t* fd_sink_create ( int fd );
event_action_t fd_sink_send_event ( struct sink* snk, event_t* event );
mvpoll_key_t*  fd_sink_get_event_key ( struct sink* snk );
int  fd_sink_shutdown ( struct sink* snk );
void fd_sink_raze ( struct sink* snk );

#endif
