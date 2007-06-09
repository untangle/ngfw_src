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

package com.untangle.uvm;

import java.lang.reflect.Method;

/**
 * Factory to get the UvmContext for an UVM instance.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmContextFactory
{
    private static UvmLocalContext UVM_CONTEXT = null;

    /**
     * Gets the current state of the UVM.  This provides a way to get
     * the state without creating the UvmLocalContext in case we're
     * calling this at a very early stage.
     *
     * @return a <code>UvmState</code> enumerated value
     */
    public static UvmState state()
    {
        if (UVM_CONTEXT == null) {
            return UvmState.LOADED;
        } else {
            return UVM_CONTEXT.state();
        }
    }

    /**
     * Get the <code>UvmContext</code> from this classloader.
     * used by Nodes to get the context internally.
     *
     * @return the <code>UvmLocalContext</code>.
     */
    public static UvmLocalContext context()
    {
        if (null == UVM_CONTEXT) {
            synchronized (UvmContextFactory.class) {
                if (null == UVM_CONTEXT) {
                    try {
                        Class c = Class.forName("com.untangle.uvm.engine.UvmContextImpl");
                        Method m = c.getMethod("context");
                        UVM_CONTEXT = (UvmLocalContext)m.invoke(null);
                    } catch ( Exception e ) {
                        System.err.println( "No class or method for the UVM context" );
                        e.printStackTrace();
                    }
                }
            }
        }
        return UVM_CONTEXT;
    }
}
