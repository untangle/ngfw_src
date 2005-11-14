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

package com.metavize.tran.nat;


import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.FilterDesc;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.TransformContext;

public class NatRedirectEventHandler implements EventHandler<LogEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("NAT Redirect Events");

    private static final String WARM_QUERY
        = "FROM RedirectEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    NatRedirectEventHandler(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(LogEvent e)
    {
        return e instanceof RedirectEvent;
    }
}
