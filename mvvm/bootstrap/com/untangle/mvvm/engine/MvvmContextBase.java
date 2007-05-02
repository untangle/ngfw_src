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

package com.untangle.mvvm.engine;


/**
 * This class is for Main to manipulate MvvmContext without resorting
 * to reflection and allowing package protection for sensitive methods.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class MvvmContextBase
{
    protected Main main;

    protected abstract void init();
    protected abstract void destroy();
    protected abstract void postInit();

    void doInit(Main main)
    {
        this.main = main;
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

    protected Main getMain()
    {
        return main;
    }
}
