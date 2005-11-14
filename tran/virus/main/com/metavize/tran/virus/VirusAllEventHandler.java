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
import com.metavize.mvvm.tran.TransformContext;

public class VirusAllEventHandler implements EventHandler<VirusEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("All Events");

    private static final String HTTP_QUERY
        = "FROM VirusHttpEvent evt WHERE evt.requestLine.httpRequestEvent.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String FTP_QUERY
        = "FROM VirusLogEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String MAIL_QUERY
        = "FROM VirusMailEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String SMTP_QUERY
        = "FROM VirusSmtpEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    VirusAllEventHandler(TransformContext transformContext)
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
        return new String[] { HTTP_QUERY, FTP_QUERY, MAIL_QUERY, SMTP_QUERY };
    }

    public boolean accept(VirusEvent e)
    {
        return true;
    }
}
