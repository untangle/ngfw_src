/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: source.h,v 1.2 2004/11/10 20:56:44 dmorris Exp $
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
