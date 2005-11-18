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

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.EventFilter;
import com.metavize.mvvm.logging.RepositoryDesc;

public class IDSLogFilter implements EventFilter<IDSLogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private static final String WARM_QUERY = "FROM IDSLogEvent evt "
        + "WHERE evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp";

    // constructors -----------------------------------------------------------

    IDSLogFilter() { }

    // EventFilter methods ----------------------------------------------------

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
