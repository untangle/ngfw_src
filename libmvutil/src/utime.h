/* $Id: utime.h,v 1.1 2004/11/09 19:39:57 dmorris Exp $ */
#ifndef __UTIME_H
#define __UTIME_H

#include <sys/time.h>
#include <semaphore.h>

/**
 * used as an argument to utime_timer_start_sem
 */
struct utime_timer {
    int    usec;
    sem_t* sem_to_post;
};

/**
 * returns the number of microseconds between the two times
 */
unsigned long utime_usec_diff (struct timeval* earlier, struct timeval* later);

/**
 * utime_usec_diff(earlier,NOW)
 */
unsigned long utime_usec_diff_now (struct timeval* earlier);

/**
 * adds microsec microseconds to ti
 */
unsigned long utime_usec_add (struct timeval* ti, long microsec);

/**
 * starts a thread that waits usec microseconds and then posts the semaphore 
 */
void*         utime_timer_start_sem(void* utime_timer_struct);

#endif
