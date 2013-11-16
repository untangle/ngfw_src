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

/* $Id$ */
#include "mvutil/errlog.h"

#include <stdio.h>
#include <stdarg.h>
#include <stdlib.h>
#include <sys/time.h>
#include <time.h>
#include <semaphore.h>

#define ERROR_FATAL_LVL     0
#define ERROR_CRITICAL_LVL  1
#define ERROR_WARNING_LVL   2
#define ERROR_INFORM_LVL    3

#define SECOND_PREFIX "%s: "

pthread_mutex_t _output_mutex = PTHREAD_MUTEX_INITIALIZER;
// sem_t* output_lock;

static char* err_strs[4]={"FATAL","CRITICAL ERROR","WARNING","INFORMATION"};
static FILE* errlog_output=NULL;
static int    errlog_date = 1;
static errlog_fatal_func_t fatal_func = (errlog_fatal_func_t)exit;


int _errlog (char* fmt, char* file, int lineno, int level, char*lpszFmt, ...)
{
    va_list argptr;

    if (!errlog_output) return -1;

    if (errlog_date) {
        struct timeval tv;
        struct tm tm;

        gettimeofday(&tv,NULL);
        if (!localtime_r(&tv.tv_sec,&tm))
            perrlog("gmtime_r");

        OUT_LOCK();

        fprintf (errlog_output,"%02i-%02i %02i:%02i:%02i.%06li| ",tm.tm_mon+1,tm.tm_mday,tm.tm_hour,tm.tm_min,tm.tm_sec,(long)tv.tv_usec);
    }
    else {
        OUT_LOCK();
    }


    fprintf(errlog_output,fmt,file,lineno);
    fprintf(errlog_output,SECOND_PREFIX,err_strs[level]);
  
    va_start(argptr, lpszFmt);
    vfprintf(errlog_output,lpszFmt, argptr);
    va_end(argptr);
    fflush(errlog_output);

    OUT_UNLOCK();

    if (level==ERROR_FATAL_LVL)fatal_func((void*)1);

    return -1;
}

int _errlog_noprefix (char* fmt, char* file, int lineno, int level, char*lpszFmt, ...)
{
    va_list argptr;

    if (!errlog_output) return -1;

    OUT_LOCK();

    va_start(argptr, lpszFmt);
    vfprintf(errlog_output,lpszFmt, argptr);
    va_end(argptr);
    fflush(errlog_output);

    OUT_UNLOCK();

    if (level==ERROR_FATAL_LVL)fatal_func((void*)1);

    return -1;
}

void _errlog_set_output (FILE* out)
{
    errlog_output=out;
}

void _errlog_date_toggle (int onoff)
{
    errlog_date = onoff;  
}

int  _errlog_init (void)
{
    errlog_output = stderr; 


    return 0;
}

void _errlog_set_fatal_func (errlog_fatal_func_t func)
{
    fatal_func = func;
}

