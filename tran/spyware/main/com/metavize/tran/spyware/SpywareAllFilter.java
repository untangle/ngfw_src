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

import com.metavize.mvvm.logging.EventFilter;
import com.metavize.mvvm.logging.RepositoryDesc;

public class SpywareAllFilter implements EventFilter<SpywareEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private static final String ACCESS_QUERY
        = "FROM SpywareAccessEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String ACTIVEX_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String BLACKLIST_QUERY
        = "FROM SpywareBlacklistEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String COOKIE_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    // EventFilter methods ----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { ACCESS_QUERY, ACTIVEX_QUERY, BLACKLIST_QUERY,
                              COOKIE_QUERY };

    }

    public boolean accept(SpywareEvent e)
    {
        return true;
    }
}
