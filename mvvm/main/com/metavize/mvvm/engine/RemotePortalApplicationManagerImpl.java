/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.portal.Application;
import com.metavize.mvvm.portal.RemoteApplicationManager;

class RemotePortalApplicationManagerImpl implements RemoteApplicationManager
{
    private final PortalApplicationManagerImpl am;

    RemotePortalApplicationManagerImpl(PortalApplicationManagerImpl am)
    {
        this.am = am;
    }

    public Application[] getApplications()
    {
        return am.getApplications();
    }

    public String[] getApplicationNames()
    {
        return am.getApplicationNames();
    }

    public Application getApplication(String name)
    {
        return am.getApplication(name);
    }
}
