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

package com.untangle.tran.nat;


import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class NatRedirectFilter implements SimpleEventFilter<LogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("NAT Redirect Events");

    private static final String WARM_QUERY
        = "FROM RedirectEvent evt ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(LogEvent e)
    {
        return e instanceof RedirectEvent;
    }
}
