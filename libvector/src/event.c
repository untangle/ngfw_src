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
#include <vector/event.h>

#include <stdlib.h>
#include <mvutil/errlog.h>

event_t* event_create ( event_type_t type )
{
    event_t* ev = malloc(sizeof(event_t));
    if (!ev)
        return errlogmalloc_null();

    ev->type = type;
    ev->raze = event_raze;
    
    return ev;
}

void     event_raze ( event_t* ev )
{
    if (ev) free(ev);
}
