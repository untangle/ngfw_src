/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

import com.metavize.mvvm.engine.MvvmContextImpl;

/**
 * Factory to get the MvvmContext for an MVVM instance.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmContextFactory
{
    private static MvvmLocalContext MVVM_CONTEXT
        = MvvmContextImpl.context();

    /**
     * Get the <code>MvvmContext</code> from this classloader.
     * used by Transforms to get the context internally.
     *
     * @return the <code>MvvmLocalContext</code>.
     */
    public static MvvmLocalContext context()
    {
        return MVVM_CONTEXT;
    }
}
