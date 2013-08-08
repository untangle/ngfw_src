/**
 * $Id$
 */
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
