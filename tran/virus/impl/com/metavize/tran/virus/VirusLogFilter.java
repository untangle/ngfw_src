/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class VirusLogFilter implements SimpleEventFilter<VirusEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("FTP Events");

    private final String warmQuery;

    // constructors -----------------------------------------------------------

    VirusLogFilter(String vendorName)
    {
        warmQuery = "FROM VirusLogEvent evt "
        + "WHERE evt.vendorName = '" + vendorName + "' "
        + "AND evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { warmQuery };
    }

    public boolean accept(VirusEvent e)
    {
        return e instanceof VirusLogEvent;
    }
}
