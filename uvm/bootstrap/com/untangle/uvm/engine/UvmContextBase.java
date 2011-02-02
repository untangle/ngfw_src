/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Map;

/**
 * This class is for Main to manipulate UvmContext without resorting
 * to reflection and allowing package protection of sensitive methods.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class UvmContextBase
{
    protected Main main;

    // public methods ---------------------------------------------------------

    public abstract Map<String, String> getTranslations(String module);
    public abstract String getCompanyName();
    // abstract methods -------------------------------------------------------

    /**
     * Initialize the UVM, starting up base services.
     */
    protected abstract void init();

    /**
     * Do final initialization, begin processing traffic.
     */
    protected abstract void postInit();

    /**
     * Destroy the UVM, stopping all services.
     */
    protected abstract void destroy();

    // protected methods ------------------------------------------------------

    /**
     * Gives UvmContextImpl access to the Main object.
     *
     * @return the Main object given to {@link #doInit(Main)}.
     */
    protected Main getMain()
    {
        return main;
    }

    // package protected methods ----------------------------------------------

    /**
     * Method for bootstrap access to UvmContext.
     *
     * @param main the Main object that started the boot process.
     */
    void doInit(Main main)
    {
        this.main = main;
        init();
    }

    /**
     * Method for bootstrap access to UvmContext.
     */
    void doPostInit()
    {
        postInit();
    }

    /**
     * Method for bootstrap access to UvmContext.
     */
    void doDestroy()
    {
        destroy();
    }
}
