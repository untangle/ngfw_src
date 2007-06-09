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

package com.untangle.node.webfilter;


import com.untangle.node.http.BlockDetails;

public class WebFilterBlockDetails extends BlockDetails
{
    private final WebFilterSettings settings;
    private final String reason;

    // constructor ------------------------------------------------------------

    public WebFilterBlockDetails(WebFilterSettings settings, String host,
                                   String uri, String reason)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
    }

    // public methods ---------------------------------------------------------

    public String getHeader()
    {
        return settings.getBlockTemplate().getHeader();
    }

    public String getContact()
    {
        return settings.getBlockTemplate().getContact();
    }

    public String getReason()
    {
        return reason;
    }
}
