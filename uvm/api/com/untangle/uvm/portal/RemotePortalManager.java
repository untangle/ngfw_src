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

package com.untangle.uvm.portal;

import java.util.List;

public interface RemotePortalManager
{
    RemoteApplicationManager applicationManager();

    PortalSettings getPortalSettings();

    void setPortalSettings(PortalSettings settings);

    List<PortalLogin> getActiveLogins();

    void forceLogout(PortalLogin login);
}
