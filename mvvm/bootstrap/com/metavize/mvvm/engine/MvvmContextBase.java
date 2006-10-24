/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

/**
 * This class is for Main to manipulate MvvmContext without resorting
 * to reflection and allowing package protection for sensitive methods.
 *
 * @author <a href="mailto:amread@untanglenetworks.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class MvvmContextBase
{
    private Main m_main;

    protected abstract void init();
    protected abstract void destroy();
    protected abstract void postInit();
    protected abstract InvokerBase getInvoker();

    void doInit(Main main)
    {
        m_main = main;
        init();
    }

    void doPostInit()
    {
        postInit();
    }

    void doDestroy()
    {
        destroy();
    }
}
