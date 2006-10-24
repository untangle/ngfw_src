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

package com.metavize.tran.spam;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class SpamAllFilter implements SimpleEventFilter<SpamEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private final String logQuery;
    private final String smtpQuery;

    // constructors -----------------------------------------------------------

    public SpamAllFilter(String vendor)
    {
        logQuery = "FROM SpamLogEvent evt WHERE evt.vendorName = '" + vendor + "' AND evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
        smtpQuery = "FROM SpamSmtpEvent evt WHERE evt.vendorName = '" + vendor + "' AND evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { logQuery, smtpQuery };
    }

    public boolean accept(SpamEvent e)
    {
        return true;
    }
}
