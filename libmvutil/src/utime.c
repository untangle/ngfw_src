/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: utime.c,v 1.1 2004/11/09 19:39:57 dmorris Exp $
 */
#include "utime.h"

#include <sys/types.h>
#include <unistd.h>
#include "errlog.h"

unsigned long utime_usec_diff (struct timeval* earlier, struct timeval* later)
{
    u_long usec_diff = 0;
    
    if (!earlier || !later) return 0;
    
    usec_diff  = (later->tv_sec - earlier->tv_sec)*1000000;
    usec_diff += (later->tv_usec) - (earlier->tv_usec);
    
    return usec_diff;
}

unsigned long utime_usec_diff_now (struct timeval* earlier)
{
    struct timeval tv;

    if (gettimeofday(&tv,NULL)<0)
        return 0;
    
    return utime_usec_diff(earlier,&tv);
}

unsigned long utime_usec_add (struct timeval* ti, long microsec)
{
    u_long sec;
    u_long usec;
    
    if (!ti) return -1;
    
    sec =  microsec/1000000;
    usec = microsec%1000000;

    ti->tv_sec += sec;
    ti->tv_usec += usec;
    
    return 0;
}

void*         utime_timer_start_sem(void* utime_timer_struct)
{
    struct utime_timer* t = (struct utime_timer*) utime_timer_struct;
    
    usleep(t->usec);
    sem_post(t->sem_to_post);
    return NULL;
}
