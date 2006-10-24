/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.util.List;

import com.metavize.mvvm.portal.PortalLogin;
import com.metavize.mvvm.portal.PortalSettings;
import com.metavize.mvvm.portal.RemoteApplicationManager;
import com.metavize.mvvm.portal.RemotePortalManager;

class RemotePortalManagerImpl implements RemotePortalManager
{
    private final PortalManagerImpl pmi;

    RemotePortalManagerImpl(PortalManagerImpl pmi)
    {
        this.pmi = pmi;
    }

    public RemoteApplicationManager applicationManager()
    {
        return pmi.remoteApplicationManager();
    }

    public PortalSettings getPortalSettings()
    {
        return pmi.getPortalSettings();
    }

    public void setPortalSettings(PortalSettings settings)
    {
        pmi.setPortalSettings(settings);
    }

    public List<PortalLogin> getActiveLogins()
    {
        return pmi.getActiveLogins();
    }

    public void forceLogout(PortalLogin login)
    {
        pmi.forceLogout(login);
    }
}
