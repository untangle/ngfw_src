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

package com.untangle.uvm.engine;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.LogEvent;

abstract class EventCache<E extends LogEvent> implements EventRepository<E>
{
    public abstract void log(E e);

    /**
     * Sets a reference to the EventLogger as soon
     * as this cache is added to an EventLogger
     */
    public abstract void setEventLogger(EventLoggerImpl<E> eventLogger);
}
