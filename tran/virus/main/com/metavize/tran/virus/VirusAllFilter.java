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

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.logging.RepositoryDesc;

public class VirusAllFilter implements SimpleEventFilter<VirusEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("All Events");

    private final String httpQuery;
    private final String ftpQuery;
    private final String mailQuery;
    private final String smtpQuery;

    // constructors -----------------------------------------------------------

    VirusAllFilter(String vendorName)
    {
        httpQuery = "FROM VirusHttpEvent evt "
            + "WHERE evt.vendorName = '" + vendorName + "' "
            + "AND evt.requestLine.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp";

        ftpQuery = "FROM VirusLogEvent evt "
            + "WHERE evt.vendorName = '" + vendorName + "' "
            + "AND evt.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp";

        mailQuery = "FROM VirusMailEvent evt "
            + "WHERE evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp";

        smtpQuery = "FROM VirusSmtpEvent evt "
            + "WHERE evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp";
    }

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { httpQuery, ftpQuery, mailQuery, smtpQuery };
    }

    public boolean accept(VirusEvent e)
    {
        return true;
    }
}
