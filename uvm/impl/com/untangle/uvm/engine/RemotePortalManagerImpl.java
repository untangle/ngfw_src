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

package com.untangle.uvm.engine;

import java.util.List;

import com.untangle.uvm.portal.PortalLogin;
import com.untangle.uvm.portal.PortalSettings;
import com.untangle.uvm.portal.RemoteApplicationManager;
import com.untangle.uvm.portal.RemotePortalManager;

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
