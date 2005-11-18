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

import com.metavize.mvvm.logging.EventFilter;
import com.metavize.mvvm.logging.RepositoryDesc;

public class VirusSmtpFilter implements EventFilter<VirusEvent>
{
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("SMTP Events");

    private final String warmQuery;

    // constructors -----------------------------------------------------------

    VirusSmtpFilter(String vendorName)
    {
        warmQuery = "FROM VirusSmtpEvent evt "
            + "WHERE evt.vendorName = '" + vendorName + "' "
            + "AND evt.messageInfo.pipelineEndpoints.policy = :policy "
            + "ORDER BY evt.timeStamp";
    }

    // EventFilter methods ----------------------------------------------------

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
        return e instanceof VirusSmtpEvent;
    }
}
