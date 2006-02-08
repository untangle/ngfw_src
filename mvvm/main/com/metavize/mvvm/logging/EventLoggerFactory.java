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

package com.metavize.mvvm.logging;

import com.metavize.mvvm.engine.EventLoggerFactoryImpl;
import com.metavize.mvvm.tran.TransformContext;

public abstract class EventLoggerFactory
{
    private static final EventLoggerFactory FACTORY
        = EventLoggerFactoryImpl.factory();

    public static EventLoggerFactory factory()
    {
        return FACTORY;
    }

    public abstract <E extends LogEvent> EventLogger<E> getEventLogger();
    public abstract <E extends LogEvent> EventLogger<E> getEventLogger(TransformContext tctx);
}
