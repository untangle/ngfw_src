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

import com.untangle.mvvm.portal.*;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class PortalLoginoutFilter implements SimpleEventFilter<PortalEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("User Events");

    private static final String WARM_QUERY_LOGIN
        = "FROM PortalLoginEvent evt ORDER BY evt.timeStamp DESC";

    private static final String WARM_QUERY_LOGOUT
        = "FROM PortalLogoutEvent evt ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY_LOGIN, WARM_QUERY_LOGOUT };
    }

    public boolean accept(PortalEvent e)
    {
        return (e instanceof PortalLoginEvent || e instanceof PortalLogoutEvent);
    }
}
