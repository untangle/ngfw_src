/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TransformContextFactory.java,v 1.1 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.TransformContext;

// XXX move to engine package?
public class TransformContextFactory
{
    /**
     * Get the TransformContext for this thread's context ClassLoader.
     *
     * @return a TransformContext implementation.
     */
    public static TransformContext context()
    {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return MvvmContextFactory.context().transformContext(cl);
    }
}
