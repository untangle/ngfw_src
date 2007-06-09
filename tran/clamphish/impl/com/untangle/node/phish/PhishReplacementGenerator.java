/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: PhishNode.java 8965 2007-02-23 20:54:04Z cng $
 */

package com.untangle.node.phish;

import com.untangle.uvm.security.Tid;
import com.untangle.node.http.ReplacementGenerator;

class PhishReplacementGenerator
    extends ReplacementGenerator<PhishBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This web page was blocked because it may be designed to steal"
        + " personal information.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<HR>"
        + "<ADDRESS>Untangle</ADDRESS>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    PhishReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    @Override
    protected String getReplacement(PhishBlockDetails details)
    {
        return String.format(BLOCK_TEMPLATE, details.getHost(),
                             details.getUri());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/idblocker/blockpage.jsp?nonce=" + nonce
            + "&tid=" + tid;
    }
}
