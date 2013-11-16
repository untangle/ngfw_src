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
