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
import com.metavize.mvvm.logging.FilterDesc;

public class VirusHttpEventHandler implements EventHandler<VirusEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("HTTP Events");

    private final String warmQuery;

    // constructors -----------------------------------------------------------

    VirusHttpEventHandler(String vendorName)
    {
        warmQuery = "FROM VirusHttpEvent evt "
        + "WHERE evt.vendorName = '" + vendorName + "' "
        + "AND evt.requestLine.httpRequestEvent.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp";
    }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { warmQuery };
    }

    public boolean accept(VirusEvent e)
    {
        return e instanceof VirusHttpEvent;
    }
}
