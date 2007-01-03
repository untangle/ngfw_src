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

import java.util.List;

import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.RemoteApplicationManager;

class RemotePortalApplicationManagerImpl implements RemoteApplicationManager
{
    private final PortalApplicationManagerImpl am;

    RemotePortalApplicationManagerImpl(PortalApplicationManagerImpl am)
    {
        this.am = am;
    }

    public List<Application> getApplications()
    {
        return am.getApplications();
    }

    public List<String> getApplicationNames()
    {
        return am.getApplicationNames();
    }

    public Application getApplication(String name)
    {
        return am.getApplication(name);
    }
}
