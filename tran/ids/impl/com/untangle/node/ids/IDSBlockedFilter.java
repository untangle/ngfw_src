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

package com.untangle.tran.ids;

import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;

public class IDSBlockedFilter implements SimpleEventFilter<IDSLogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Blocked Events");

    private static final String WARM_QUERY = "FROM IDSLogEvent evt "
        + "WHERE evt.blocked = true AND evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp DESC";

    // constructors -----------------------------------------------------------

    IDSBlockedFilter () { }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(IDSLogEvent e)
    {
        return e.isBlocked();
    }
}
