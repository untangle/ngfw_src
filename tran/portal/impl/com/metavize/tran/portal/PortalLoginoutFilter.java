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

package com.metavize.tran.portal;

import com.metavize.mvvm.portal.*;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

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
