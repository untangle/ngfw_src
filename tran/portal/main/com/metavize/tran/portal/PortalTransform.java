/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal;

import com.metavize.mvvm.portal.*;

public interface PortalTransform
{
    Application[] getApplications();

    String[] getApplicationNames();

    Application getApplication(String name);

    PortalSettings getPortalSettings();

    void setPortalSettings(PortalSettings settings);
}
