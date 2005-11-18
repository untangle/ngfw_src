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

public class SpywareActiveXFilter implements EventFilter<SpywareEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("ActiveX Events");

    private static final String WARM_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    // EventFilter methods ----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(SpywareEvent e)
    {
        return e instanceof SpywareActiveXEvent;
    }
}
