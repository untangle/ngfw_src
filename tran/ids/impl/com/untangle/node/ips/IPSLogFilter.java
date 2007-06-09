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

package com.untangle.node.ips;

import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.logging.RepositoryDesc;

public class IPSLogFilter implements SimpleEventFilter<IPSLogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private static final String WARM_QUERY = "FROM IPSLogEvent evt "
        + "WHERE evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp DESC";

    // constructors -----------------------------------------------------------

    IPSLogFilter() { }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(IPSLogEvent e)
    {
        return true;
    }
}
