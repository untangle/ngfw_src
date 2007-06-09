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

package com.untangle.node.virus;

import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

public class VirusInfectedFilter implements SimpleEventFilter<VirusEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Infected Events");

    private final String httpQuery;
    private final String ftpQuery;
    private final String mailQuery;
    private final String smtpQuery;

    // constructors -----------------------------------------------------------

    VirusInfectedFilter(String vendorName)
    {
        httpQuery = "FROM VirusHttpEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.requestLine.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        ftpQuery = "FROM VirusLogEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        mailQuery = "FROM VirusMailEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";

        smtpQuery = "FROM VirusSmtpEvent evt "
            + "WHERE evt.result.clean = false "
            + "AND evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp DESC";
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
        return e.isInfected();
    }
}
