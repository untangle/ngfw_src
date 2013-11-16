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
#ifndef __FD_SOURCE_H_
#define __FD_SOURCE_H_

#include "source.h"

typedef struct fd_source {

    source_t base;

    int fd;

    mvpoll_key_t* key;
    
} fd_source_t;

source_t* fd_source_create ( int fd );
event_t*  fd_source_get_event ( source_t* src );
mvpoll_key_t* fd_source_get_event_key ( source_t* src );
int       fd_source_shutdown ( source_t* src );
void      fd_source_raze ( source_t* src );

#endif
