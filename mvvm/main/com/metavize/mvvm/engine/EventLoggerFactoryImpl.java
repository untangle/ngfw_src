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
        return new EventLoggerImpl<E>();
    }

    public <E extends LogEvent> EventLogger<E> getEventLogger(TransformContext tctx)
    {
        return new EventLoggerImpl<E>(tctx);
    }
}
