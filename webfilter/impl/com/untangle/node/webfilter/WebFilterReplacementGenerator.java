/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: WebFilterImpl.java 8987 2007-02-27 19:35:47Z amread $
 */

package com.untangle.node.webfilter;

import com.untangle.uvm.security.Tid;
import com.untangle.node.http.ReplacementGenerator;

class WebFilterReplacementGenerator
    extends ReplacementGenerator<WebFilterBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "<HR>"
        + "<ADDRESS>Untangle</ADDRESS>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    WebFilterReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    @Override
    protected String getReplacement(WebFilterBlockDetails details)
    {
        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             details.getContact());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/webfilter/blockpage.jsp?nonce=" + nonce
            + "&tid=" + tid;
    }
}
