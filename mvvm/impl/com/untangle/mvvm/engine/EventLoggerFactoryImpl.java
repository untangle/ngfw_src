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

package com.untangle.mvvm.engine;

import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.tran.TransformContext;

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
