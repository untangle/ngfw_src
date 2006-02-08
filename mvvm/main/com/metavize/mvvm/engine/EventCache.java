/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventRepository;
import com.metavize.mvvm.logging.LogEvent;

abstract class EventCache<E extends LogEvent> implements EventRepository<E>
{
    public abstract void log(E e);

    public abstract void checkCold();

    /**
     * Sets a reference to the EventLogger as soon
     * as this cache is added to an EventLogger
     */
    public abstract void setEventLogger(EventLoggerImpl<E> eventLogger);
}
