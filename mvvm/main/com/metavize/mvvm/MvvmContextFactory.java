/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;


/**
 * Factory to get the MvvmContext for an MVVM instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmContextFactory
{
    private static MvvmLocalContext MVVM_CONTEXT;

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
                    MVVM_CONTEXT = MvvmContextImpl.context();
                }
            }
        }

        return MVVM_CONTEXT;
    }
}
