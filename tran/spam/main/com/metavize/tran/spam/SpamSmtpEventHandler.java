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

package com.metavize.tran.spam;


import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.FilterDesc;
import com.metavize.mvvm.tran.TransformContext;

public class SpamSmtpEventHandler implements EventHandler<SpamEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("SMTP Events");
    private static final String WARM_QUERY
        = "FROM SpamSmtpEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpamSmtpEventHandler(TransformContext transformContext)
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

    public boolean accept(SpamEvent e)
    {
        return e instanceof SpamSmtpEvent;
    }
}
