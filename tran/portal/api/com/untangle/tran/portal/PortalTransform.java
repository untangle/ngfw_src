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

package com.untangle.tran.portal;

import java.util.List;

import com.untangle.mvvm.portal.Application;
import com.untangle.mvvm.portal.PortalEvent;
import com.untangle.mvvm.portal.PortalSettings;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.LogEvent;

public interface PortalTransform
{
    List<Application> getApplications();

    List<String> getApplicationNames();

    Application getApplication(String name);

    PortalSettings getPortalSettings();

    void setPortalSettings(PortalSettings settings);

    EventManager<PortalEvent> getEventManager();
}

