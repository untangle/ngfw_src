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
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.tran.LocalTransformManager;

public class TransformContextSwitcher<T>
{
    private final TransformContext transformContext;

    public TransformContextSwitcher( TransformContext transformContext )
    {
        this.transformContext = transformContext;
    }
    
    public void run( Event<T> event, T argument )
    {
        LocalTransformManager transformManager = MvvmContextFactory.context().transformManager();
        ClassLoader classLoader = this.transformContext.getClassLoader();
        Thread ct = Thread.currentThread();
        ClassLoader oldCl = ct.getContextClassLoader();

        // entering TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        ct.setContextClassLoader( classLoader );
        try {
            transformManager.registerThreadContext( this.transformContext );
            event.handle( argument );
        } finally {
            transformManager.deregisterThreadContext();
            ct.setContextClassLoader( oldCl );
            // left TransformClassLoader ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
    }
    
    public static interface Event<V>
    {
        public void handle( V argument );
    }
}
