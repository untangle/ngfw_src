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

package com.untangle.mvvm.tran;

import com.untangle.mvvm.MvvmContextFactory;

public class TransformContextSwitcher<T>
{
    private final TransformContext transformContext;

    public TransformContextSwitcher(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    public void run(Event<T> event, T argument)
    {
        LocalTransformManager tm = MvvmContextFactory.context().transformManager();
        try {
            tm.registerThreadContext(this.transformContext);
            event.handle(argument);
        } finally {
            tm.deregisterThreadContext();
        }
    }

    public static interface Event<V>
    {
        public void handle(V argument);
    }
}
