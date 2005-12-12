/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FirewallAccessEventHandler.java 3373 2005-11-12 00:13:48Z amread $
 */

package com.metavize.tran.protofilter;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class ProtoFilterAllFilter implements SimpleEventFilter<ProtoFilterLogEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Protocol Events");

    private static final String WARM_QUERY
        = "FROM ProtoFilterLogEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(ProtoFilterLogEvent e)
    {
        return true;
    }
}
