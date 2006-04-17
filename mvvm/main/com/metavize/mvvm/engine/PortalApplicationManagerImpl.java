/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.log4j.Logger;
import com.metavize.mvvm.portal.*;

/**
 * Implementation of the ApplicationManager
 *
 */
public class PortalApplicationManagerImpl
  implements ApplicationManager {

    private static final Application[] protoArr = new Application[] { };

    private static final PortalApplicationManagerImpl APPLICATION_MANAGER = new PortalApplicationManagerImpl();

    private final Logger logger = Logger.getLogger(PortalApplicationManagerImpl.class);

    private SortedMap<Integer, Application> apps;

    private PortalApplicationManagerImpl() {
        apps = new TreeMap<Integer, Application>();

        logger.info("Initialized ApplicationManager");
    }

    /**
     * Do not call this directly, instead go through <code>MvvmLocalContext</code>
     */
    static PortalApplicationManagerImpl applicationManager() {
        return APPLICATION_MANAGER;
    }

    
    /**
     * Registers a new application.  Called when a portal transform is loaded
     * to make a new application available for bookmarks, launching, etc.
     * An application can only be registered once, if reregistered an
     * <code>IllegalArgumentException</code> is thrown.
     *
     * @return the registered <code>Application</code>
     */
    public Application registerApplication(String name, String description, boolean isHostService,
                                           Application.Validator validator, int sortPosition)
    {
        if (getApplication(name) != null)
            throw new IllegalArgumentException("Application " + name + " already registered");
        if (name == null || description == null || validator == null)
            throw new IllegalArgumentException("null in registerApplication");

        Application newApp = new Application(name, description, isHostService, validator);
        apps.put(sortPosition, newApp);
        return newApp;
    }
        
    public Application[] getApplications()
    {
        return apps.values().toArray(protoArr);
    }

    public String[] getApplicationNames()
    {
        String[] result = new String[apps.size()];
        int i = 0;
        for (Iterator<Application> iter = apps.values().iterator(); iter.hasNext(); i++) {
            Application app = iter.next();
            result[i] = app.getName();
        }
        return result;
    }

    public Application getApplication(String name)
    {
        for (Iterator<Application> iter = apps.values().iterator(); iter.hasNext();) {
            Application app = iter.next();
            if (name.equals(app.getName()))
                return app;
        }
        return null;
    }
        
}
