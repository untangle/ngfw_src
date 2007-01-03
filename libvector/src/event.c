/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
#include <vector/event.h>

#include <stdlib.h>
#include <mvutil/errlog.h>

event_t* event_create (event_type_t type)
{
    event_t* ev = malloc(sizeof(event_t));
    if (!ev)
        return errlogmalloc_null();

    ev->type = type;
    ev->raze = event_raze;
    
    return ev;
}

void     event_raze (event_t* ev)
{
    if (ev) free(ev);
}
