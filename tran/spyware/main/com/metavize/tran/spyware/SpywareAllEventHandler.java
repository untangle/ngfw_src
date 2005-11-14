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


import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.FilterDesc;
import com.metavize.mvvm.tran.TransformContext;

public class SpywareAllEventHandler implements EventHandler<SpywareEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("All Events");

    private static final String ACCESS_QUERY
        = "FROM SpywareAccessEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String ACTIVEX_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String BLACKLIST_QUERY
        = "FROM SpywareBlacklistEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String COOKIE_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpywareAllEventHandler(TransformContext transformContext)
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
        return new String[] { ACCESS_QUERY, ACTIVEX_QUERY, BLACKLIST_QUERY,
                              COOKIE_QUERY };

    }

    public boolean accept(SpywareEvent e)
    {
        return true;
    }
}
