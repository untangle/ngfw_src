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

import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.logging.RepositoryDesc;

public class IDSLogFilter implements SimpleEventFilter<IDSLogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private static final String WARM_QUERY = "FROM IDSLogEvent evt "
        + "WHERE evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp DESC";

    // constructors -----------------------------------------------------------

    IDSLogFilter() { }

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
        return true;
    }
}
