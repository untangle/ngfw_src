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
#include "mvutil/debug.h"

#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <sys/time.h>
#include <time.h>
#include <stdlib.h>
#include <execinfo.h>
#include "mvutil/errlog.h"

#define DATE_DEFAULT  1
#define LEVEL_DEFAULT 0
#define OUT_DEFAULT   stdout

struct debug_pkgs {
    int level;
    FILE* output;
    int date;
};

static struct debug_pkgs pkgs[DEBUG_MAX_PKGS];

int  _debug(int pkg, int level, char *lpszFmt, ...)
{
    if (pkgs[pkg].level >= level)
    {
        va_list argptr;
        if (!pkgs[pkg].output) return 0;

        va_start(argptr, lpszFmt);

        OUT_LOCK();

        if (pkgs[pkg].date) {
            struct timeval tv;
            struct tm tm;
            
            gettimeofday(&tv,NULL);
            if (!localtime_r(&tv.tv_sec,&tm))
                perrlog("gmtime_r");
            
            fprintf (pkgs[pkg].output,"%02i-%02i %02i:%02i:%02i.%06li| ",tm.tm_mon+1,tm.tm_mday,tm.tm_hour,tm.tm_min,tm.tm_sec,(long)tv.tv_usec);
        }
          
        vfprintf(pkgs[pkg].output,lpszFmt, argptr);

        va_end(argptr);

        fflush(pkgs[pkg].output);

        OUT_UNLOCK();
    }

	return 0;
}

int _debug_backtrace( int pkg, int level, char *lpszFmt, ... )
{
    if (pkgs[pkg].level >= level)
    {
        void* trace[16];
        int trace_size;
        char** messages = NULL;
        int c;


        va_list argptr;
        if (!pkgs[pkg].output) return 0;
        
        va_start(argptr, lpszFmt);

        OUT_LOCK();

        if (pkgs[pkg].date) {
            struct timeval tv;
            struct tm tm;
            
            gettimeofday(&tv,NULL);
            if (!localtime_r(&tv.tv_sec,&tm))
                perrlog("gmtime_r");
            
            fprintf( pkgs[pkg].output,"%02i-%02i %02i:%02i:%02i.%06li| ",tm.tm_mon+1,tm.tm_mday,tm.tm_hour,tm.tm_min,tm.tm_sec,(long)tv.tv_usec);
        }
                
        vfprintf( pkgs[pkg].output,lpszFmt, argptr);
        
        trace_size = backtrace( trace, 16 );
        if (trace_size > 1 && (( messages = backtrace_symbols( trace, trace_size )) != NULL )) {
            /* Skip one for the debug function */
            fprintf( pkgs[pkg].output, "Stack trace: %d\n", trace_size - 1 );
            for ( c = 1 ; c < trace_size ; c++ ) {
                fprintf( pkgs[pkg].output, "bt[%d] %s\n", c - 1, messages[c] );
            }
            free( messages );
        } else {
            fprintf( pkgs[pkg].output, "ERROR: backtrace_symbols error\n" );
        }

        va_end(argptr);

        fflush(pkgs[pkg].output);

        OUT_UNLOCK();
    }

	return 0;

}


int  _debug_nodate(int pkg, int level, char *lpszFmt, ...)
{

    if (pkgs[pkg].level >= level)
    {
        va_list argptr;
        if (!pkgs[pkg].output) return 0;

        va_start(argptr, lpszFmt);
          
        OUT_LOCK();

        vfprintf(pkgs[pkg].output,lpszFmt, argptr);

        va_end(argptr);

        fflush(pkgs[pkg].output);

        OUT_UNLOCK();
    }

	return 1;
}

void _debug_set_output(int pkg, FILE * out)
{
    pkgs[pkg].output=out;
}

void _debug_date_toggle(int pkg, int onoff)
{
    pkgs[pkg].date = onoff;  
}

void _debug_set_level(int pkg, int lev)
{
    pkgs[pkg].level = lev;
}

int  _debug_get_level(int pkg)
{
    return pkgs[pkg].level;
}

int  _debug_init()
{
    int i;
    
    for (i=0;i<DEBUG_MAX_PKGS;i++) {
        pkgs[i].level  = LEVEL_DEFAULT;
        pkgs[i].date   = DATE_DEFAULT;
        pkgs[i].output = OUT_DEFAULT;
    }

    return 0;
}

