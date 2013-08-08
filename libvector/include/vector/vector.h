/**
 * $Id$
 */
#ifndef __VECTOR_H
#define __VECTOR_H

#include <mvutil/mvpoll.h>

#define _VECTOR_MSG_SHUTDOWN   1

typedef enum {
    VECTOR_MSG_SHUTDOWN = _VECTOR_MSG_SHUTDOWN,  /* shutdown the vectoring */
    VECTOR_MSG_NULL
} vector_msg_t;

typedef struct vector {

    list_t* chain;

    int msg_pipe[2];

    mvpoll_key_t* msg_key;
    
    mvpoll_id_t mvp;

    int    live_relay_count;
    
    list_t dead_relays;

    int    max_timeout;
    
} vector_t;

vector_t* vector_malloc ( void );
int       vector_init ( vector_t* vec, list_t* chain );
vector_t* vector_create ( list_t* chain );

int       vector_free ( vector_t* vec );
int       vector_destroy ( vector_t* vec );
int       vector_raze ( vector_t* vec );

int       vector ( vector_t* vec );
int       vector_send_msg ( vector_t* vec, vector_msg_t msg, void* arg );
void      vector_set_timeout ( vector_t* vec, int timeout_sec );

#endif
