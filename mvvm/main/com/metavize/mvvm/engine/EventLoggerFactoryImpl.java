/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.Map;
import java.util.WeakHashMap;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.TransformContext;

public class EventLoggerFactoryImpl extends EventLoggerFactory
{
    private static final EventLoggerFactoryImpl FACTORY
        = new EventLoggerFactoryImpl();

    private final Map<EventLoggerImpl, Object> loggers
        = new WeakHashMap<EventLoggerImpl, Object>();

    public static EventLoggerFactoryImpl factory()
    {
        return FACTORY;
    }

    public <E extends LogEvent> EventLogger<E> getEventLogger()
    {
        EventLoggerImpl el = new EventLoggerImpl<E>();
        synchronized (loggers) {
            loggers.put(el, null);
        }
        return el;
    }

    public <E extends LogEvent> EventLogger<E> getEventLogger(TransformContext tctx)
    {
        EventLoggerImpl el = new EventLoggerImpl<E>(tctx);
        synchronized (loggers) {
            loggers.put(el, null);
        }
        return el;
    }
}
