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

package com.untangle.tran.clamphish;

import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class PhishHttpBlockedFilter implements SimpleEventFilter<PhishHttpEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Blocked Phish HTTP Traffic");

    private static final String WARM_QUERY
        = "FROM PhishHttpEvent evt WHERE evt.action = 'B' AND evt.requestLine.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(PhishHttpEvent e)
    {
        return e.isPersistent() && Action.BLOCK == e.getAction();
    }
}
