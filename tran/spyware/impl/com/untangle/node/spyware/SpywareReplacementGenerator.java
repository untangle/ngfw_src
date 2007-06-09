/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareImpl.java 8986 2007-02-27 18:48:09Z amread $
 */

package com.untangle.node.spyware;

import com.untangle.uvm.security.Tid;
import com.untangle.node.http.ReplacementGenerator;

public class SpywareReplacementGenerator
    extends ReplacementGenerator<SpywareBlockDetails>
{
    private static final String SIMPLE_BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<script id='metavizeDetect' type='text/javascript'>\n"
        + "var e = document.getElementById(\"metavizeDetect\")\n"
        + "if (window == window.top && e.parentNode.tagName == \"BODY\") {\n"
        + "  document.writeln(\"<center><b>Metavize Spyware Blocker</b></center>\")\n"
        + "  document.writeln(\"<p>This site blocked because it may be a spyware site.</p>\")\n"
        + "  document.writeln(\"<p>Host: %s</p>\")\n"
        + "  document.writeln(\"<p>URI: %s</p>\")\n"
        + "  document.writeln(\"<p>Please contact your network administrator.</p>\")\n"
        + "  document.writeln(\"<HR>\")\n"
        + "  document.writeln(\"<ADDRESS>Untangle</ADDRESS>\")\n"
        + "}\n"
        + "</script>"
        + "</BODY></HTML>";

    // constructors -----------------------------------------------------------

    public SpywareReplacementGenerator(Tid tid)
    {
        super(tid);
    }

    // ReplacementGenerator methods -------------------------------------------

    protected String getReplacement(SpywareBlockDetails bd)
    {
        return String.format(SIMPLE_BLOCK_TEMPLATE, bd.getHost(), bd.getUrl());
    }

    protected String getRedirectUrl(String nonce, String host, Tid tid)
    {
        return "http://" + host + "/spyware/detect.jsp?nonce=" + nonce
            + "&tid=" + tid;
    }
}
