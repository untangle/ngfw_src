/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MvvmContextBase.java,v 1.2 2005/01/27 09:53:36 amread Exp $
 */

package com.metavize.mvvm.engine;

/**
 * This class is for Main to manipulate MvvmContext without resorting
 * to reflection and allowing package protection for sensitive methods.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class MvvmContextBase
{
    protected abstract void init();
    protected abstract void destroy();
    protected abstract void postInit();
    protected abstract InvokerBase getInvoker();

    void doInit(Main main)
    {
        init();
    }

    void doDestroy()
    {
        destroy();
    }

    void doPostInit()
    {
        postInit();
    }

    InvokerBase getInvokerBase()
    {
        return getInvoker();
    }
}
