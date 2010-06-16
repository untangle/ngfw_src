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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.untangle.uvm.portal.Application;
import com.untangle.uvm.portal.LocalApplicationManager;

/**
 * A registry of Portal applications registered with the system.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class PortalApplicationManagerImpl implements LocalApplicationManager
{
    private static final PortalApplicationManagerImpl APPLICATION_MANAGER
        = new PortalApplicationManagerImpl();

    private final Logger logger = Logger.getLogger(getClass());

    private SortedSet<Application> apps = new TreeSet<Application>();

    // constructors -----------------------------------------------------------

    private PortalApplicationManagerImpl() {
        logger.info("Initialized ApplicationManager");
    }

    // static factories -------------------------------------------------------

    static PortalApplicationManagerImpl applicationManager() {
        return APPLICATION_MANAGER;
    }

    // LocalApplicationManager methods ----------------------------------------

    /**
     * Registers a new application.  Called when a portal node is
     * loaded to make a new application available for bookmarks,
     * launching, etc.  An application can only be registered once, if
     * reregistered an <code>IllegalArgumentException</code> is
     * thrown.
     *
     * @return the registered <code>Application</code>
     */
    public Application registerApplication(String name, String desc,
                                           String longDesc,
                                           Application.Destinator destinator,
                                           Application.Validator validator,
                                           int sortPosition, String appJsUrl)
    {
        Application newApp = new Application(name, desc, longDesc, destinator,
                                             validator, sortPosition, appJsUrl);

        synchronized (apps) {
            if (!apps.add(newApp)) {
                logger.warn("app already registered: " + name);
                newApp = getApplication(name);
            }
        }

        return newApp;
    }

    public boolean deregisterApplication(Application app)
    {
        synchronized (apps) {
            return apps.remove(app);
        }
    }

    public List<Application> getApplications()
    {
        synchronized (apps) {
            return new ArrayList<Application>(apps);
        }
    }

    public List<String> getApplicationNames()
    {
        List<String> result = new ArrayList<String>(apps.size());

        synchronized (apps) {
            for (Application app : apps) {
                result.add(app.getName());
            }
        }

        return result;
    }

    public Application getApplication(String name)
    {
        Application a = null;

        synchronized (apps) {
            for (Application app : apps) {
                if (name.equals(app.getName())) {
                    a = app;
                    break;
                }
            }
        }

        return a;
    }
}
