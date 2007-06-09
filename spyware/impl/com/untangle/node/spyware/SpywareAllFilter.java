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

package com.untangle.node.spyware;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

public class SpywareAllFilter implements SimpleEventFilter<SpywareEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private static final String ACCESS_QUERY
        = "FROM SpywareAccessEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    private static final String ACTIVEX_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    private static final String BLACKLIST_QUERY
        = "FROM SpywareBlacklistEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    private static final String COOKIE_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

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
