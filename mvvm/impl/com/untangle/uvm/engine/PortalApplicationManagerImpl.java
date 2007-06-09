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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.LocalApplicationManager;
import org.apache.log4j.Logger;

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
     * Registers a new application.  Called when a portal transform is
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
            return new ArrayList(apps);
        }
    }

    public List<String> getApplicationNames()
    {
        List<String> result = new ArrayList(apps.size());

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
