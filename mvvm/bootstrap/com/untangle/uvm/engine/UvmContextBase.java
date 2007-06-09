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
 * to reflection and allowing package protection of sensitive methods.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class MvvmContextBase
{
    protected Main main;

    // abstract methods -------------------------------------------------------

    /**
     * Initialize the MVVM, starting up base services.
     */
    protected abstract void init();

    /**
     * Do final initialization, begin processing traffic.
     */
    protected abstract void postInit();

    /**
     * Destroy the MVVM, stopping all services.
     */
    protected abstract void destroy();

    // protected methods ------------------------------------------------------

    /**
     * Gives MvvmContextImpl access to the Main object.
     *
     * @return the Main object given to {@link #doInit(Main)}.
     */
    protected Main getMain()
    {
        return main;
    }

    // package protected methods ----------------------------------------------

    /**
     * Method for bootstrap access to MvvmContext.
     *
     * @param main the Main object that started the boot process.
     */
    void doInit(Main main)
    {
        this.main = main;
        init();
    }

    /**
     * Method for bootstrap access to MvvmContext.
     */
    void doPostInit()
    {
        postInit();
    }

    /**
     * Method for bootstrap access to MvvmContext.
     */
    void doDestroy()
    {
        destroy();
    }
}
