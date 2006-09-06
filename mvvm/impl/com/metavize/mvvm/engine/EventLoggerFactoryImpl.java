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

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.TransformContext;

public class EventLoggerFactoryImpl extends EventLoggerFactory
{
    private static final EventLoggerFactoryImpl FACTORY
        = new EventLoggerFactoryImpl();

    public static EventLoggerFactoryImpl factory()
    {
        return FACTORY;
    }

    public <E extends LogEvent> EventLogger<E> getEventLogger()
    {
        EventLoggerImpl el = new EventLoggerImpl<E>();
        return el;
    }

    public <E extends LogEvent> EventLogger<E> getEventLogger(TransformContext tctx)
    {
        EventLoggerImpl el = new EventLoggerImpl<E>(tctx);
        return el;
    }
}
