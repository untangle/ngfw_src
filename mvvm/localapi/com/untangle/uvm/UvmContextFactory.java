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

package com.untangle.mvvm;

import java.lang.reflect.Method;

/**
 * Factory to get the MvvmContext for an MVVM instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmContextFactory
{
    private static MvvmLocalContext MVVM_CONTEXT = null;

    /**
     * Gets the current state of the MVVM.  This provides a way to get
     * the state without creating the MvvmLocalContext in case we're
     * calling this at a very early stage.
     *
     * @return a <code>MvvmState</code> enumerated value
     */
    public static MvvmState state()
    {
        if (MVVM_CONTEXT == null) {
            return MvvmState.LOADED;
        } else {
            return MVVM_CONTEXT.state();
        }
    }

    /**
     * Get the <code>MvvmContext</code> from this classloader.
     * used by Transforms to get the context internally.
     *
     * @return the <code>MvvmLocalContext</code>.
     */
    public static MvvmLocalContext context()
    {
        if (null == MVVM_CONTEXT) {
            synchronized (MvvmContextFactory.class) {
                if (null == MVVM_CONTEXT) {
                    try {
                        Class c = Class.forName("com.untangle.mvvm.engine.MvvmContextImpl");
                        Method m = c.getMethod("context");
                        MVVM_CONTEXT = (MvvmLocalContext)m.invoke(null);
                    } catch ( Exception e ) {
                        System.err.println( "No class or method for the MVVM context" );
                        e.printStackTrace();
                    }
                }
            }
        }
        return MVVM_CONTEXT;
    }
}
