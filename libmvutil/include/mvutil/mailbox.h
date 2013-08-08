/**
 * $Id$
 */
#ifndef __UTILMAILBOX_H
#define __UTILMAILBOX_H

#include <semaphore.h>
#include "lock.h"
#include "list.h"
#include "mvpoll.h"

/* XXX These all have to be consolidated somewhere */
#define MB_SRC_KEY_TYPE 0x4100

typedef struct mailbox
{
    list_t list;

    sem_t  list_size_sem;
    
    lock_t lock;
    
    int pipe[2];
    
    int    size;

    mvpoll_key_t* mv_key;
} mailbox_t;


int          mailbox_init (mailbox_t* ll);
int          mailbox_destroy (mailbox_t* ll);

void*        mailbox_get (mailbox_t* mb);
void*        mailbox_try_get (mailbox_t* mb);
void*        mailbox_timed_get  ( mailbox_t* mb, int sec);

/* nano-second get operation */
void*        mailbox_ntimed_get ( mailbox_t* mb, struct timespec* ts ); 

/* micro-second get operation */
void*        mailbox_utimed_get ( mailbox_t* mb, struct timeval* tv );


int          mailbox_put (mailbox_t* mb, void* mail);
int          mailbox_size (mailbox_t* mb);

int          mailbox_get_pollable_event (mailbox_t* mb);
int          mailbox_clear_pollable_event (mailbox_t* mb);
mvpoll_key_t* mailbox_get_mvpoll_src_key( mailbox_t* mb );


#endif
