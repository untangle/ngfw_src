/* $Id: mailbox.h,v 1.1 2004/11/09 19:39:56 dmorris Exp $ */
#ifndef __UTILMAILBOX_H
#define __UTILMAILBOX_H

#include <semaphore.h>
#include "lock.h"
#include "list.h"

typedef struct mailbox {
    
    list_t list;

    sem_t  list_size_sem;
    
    lock_t lock;
    
    int pipe[2];
    
    int    size; 
    
} mailbox_t;


int          mailbox_init (mailbox_t* ll);
int          mailbox_destroy (mailbox_t* ll);

void*        mailbox_get (mailbox_t* mb);
void*        mailbox_try_get (mailbox_t* mb);
void*        mailbox_timed_get  ( mailbox_t* mb, int sec);

/* micro-second get operation */
void*        mailbox_utimed_get ( mailbox_t* mb, struct timespec* ts ); 

int          mailbox_put (mailbox_t* mb, void* mail);
int          mailbox_size (mailbox_t* mb);

int          mailbox_get_pollable_event (mailbox_t* mb);
int          mailbox_clear_pollable_event (mailbox_t* mb);

#endif
