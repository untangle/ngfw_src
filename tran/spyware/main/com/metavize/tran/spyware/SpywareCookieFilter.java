/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class SpywareCookieFilter implements SimpleEventFilter<SpywareEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Cookie Events");

    private static final String WARM_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(SpywareEvent e)
    {
        return e instanceof SpywareCookieEvent;
    }
}
