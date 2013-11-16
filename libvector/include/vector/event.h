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
#ifndef __EVENT_H
#define __EVENT_H

#define EVENT_SHUTDOWN_MASK        0x1000
#define EVENT_SHUTDOWN_ERROR_MASK  ( EVENT_SHUTDOWN_MASK | 0x2000 )

#define EVENT_TYPE_MASK            0x0FFF

/* These are the base events, all sink must be able to send\
 * these events (shutdown and error) */
#define EVENT_BASE_TYPE            0x0001

typedef enum {
    EVENT_BASE_SHUTDOWN = EVENT_BASE_TYPE | EVENT_SHUTDOWN_MASK,
    EVENT_BASE_ERROR_SHUTDOWN = EVENT_BASE_TYPE | EVENT_SHUTDOWN_ERROR_MASK,
    EVENT_BASE_MAX = 100,
} event_type_t;

typedef struct event {
    event_type_t type;
    void (*raze) (struct event* ev);
} event_t;

typedef void (*event_free_func_t) (event_t* event);

event_t* event_create (event_type_t type);
void     event_raze (event_t* ev);

/**
 * Returns 1 if the type has the shutdown mask
 */
static __inline__ int event_is_shutdown( event_type_t type )
{
    if (( type & EVENT_SHUTDOWN_MASK ) == EVENT_SHUTDOWN_MASK ) {
        return 1;
    }
    
    return 0;
}

/**
 * Returns 1 if the type has the shutdown with error mask
 */
static __inline__ int event_is_shutdown_error( event_type_t type )
{
    if (( type & EVENT_SHUTDOWN_ERROR_MASK ) == EVENT_SHUTDOWN_ERROR_MASK ) {
        return 1;
    }
    
    return 0;
}

#endif
