/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;


import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

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
