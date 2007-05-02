/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * @author: Dirk Morris <dmorris@untangle.com>
 * $Id$
 */
#ifndef __SOURCE_H
#define __SOURCE_H

#include <mvutil/mvpoll.h>
#include "event.h"

typedef struct source {
    event_t*      (*get_event) (struct source* src);
    mvpoll_key_t* (*get_event_key) (struct source* src);
    int           (*shutdown) (struct source* src);
    void          (*raze) (struct source* src);
} source_t;

#endif
