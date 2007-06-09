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

package com.untangle.uvm.logging;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.untangle.uvm.node.NodeContext;

public abstract class EventLoggerFactory
{
    private static final String LOGGER_CLASS
        = "com.untangle.uvm.engine.EventLoggerFactoryImpl";

    private static EventLoggerFactory FACTORY;

    public static EventLoggerFactory factory()
    {
        if (null == FACTORY) {
            synchronized (EventLoggerFactory.class) {
                if (null == FACTORY) {
                    try {
                        Class c = Class.forName(LOGGER_CLASS);
                        Method m = c.getMethod("factory");
                        FACTORY = (EventLoggerFactory)m.invoke(null);
                    } catch (ClassNotFoundException exn) {
                        throw new RuntimeException(exn);
                    } catch (NoSuchMethodException exn) {
                        throw new RuntimeException(exn);
                    } catch (IllegalAccessException exn) {
                        throw new RuntimeException(exn);
                    } catch (InvocationTargetException exn) {
                        throw new RuntimeException(exn);
                    }
                }
            }
        }

        return FACTORY;
    }

    public abstract <E extends LogEvent> EventLogger<E> getEventLogger();
    public abstract <E extends LogEvent> EventLogger<E> getEventLogger(NodeContext tctx);
}
