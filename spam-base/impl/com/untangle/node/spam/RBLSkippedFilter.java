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

package com.untangle.node.spam;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

public class RBLSkippedFilter implements SimpleEventFilter<SpamSMTPRBLEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Skipped Events");

    private final String skippedQuery;

    // constructors -----------------------------------------------------------

    public RBLSkippedFilter()
    {
        skippedQuery = "FROM SpamSMTPRBLEvent evt WHERE evt.skipped = true AND evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { skippedQuery };
    }

    public boolean accept(SpamSMTPRBLEvent e)
    {
        return e.getSkipped();
    }
}
