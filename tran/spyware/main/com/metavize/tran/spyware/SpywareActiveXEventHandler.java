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

public class SpywareActiveXEventHandler implements EventHandler<SpywareEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("ActiveX Events");

    private static final String WARM_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpywareActiveXEventHandler(TransformContext transformContext)
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

    public boolean accept(SpywareEvent e)
    {
        return e instanceof SpywareActiveXEvent;
    }
}
