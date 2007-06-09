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

package com.untangle.node.httpblocker;


import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

public class HttpBlockerBlockedFilter implements SimpleEventFilter<HttpBlockerEvent>
{
    private static final RepositoryDesc REPO_DESC
        = new RepositoryDesc("Blocked HTTP Traffic");

    private static final String WARM_QUERY
        = "FROM HttpBlockerEvent evt WHERE evt.action = 'B' AND evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(HttpBlockerEvent e)
    {
        return e.isPersistent() && Action.BLOCK == e.getAction();
    }
}
