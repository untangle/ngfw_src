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


import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.RepositoryDesc;

public class VirusLogEventHandler implements EventHandler<VirusEvent>
{
    private static final RepositoryDesc FILTER_DESC = new RepositoryDesc("FTP Events");

    private final String warmQuery;

    // constructors -----------------------------------------------------------

    VirusLogEventHandler(String vendorName)
    {
        warmQuery = "FROM VirusLogEvent evt "
        + "WHERE evt.vendorName = '" + vendorName + "' "
        + "AND evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp";
    }

    // EventCache methods -----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return FILTER_DESC;
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
