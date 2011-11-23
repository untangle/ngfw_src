/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.security.NodeId;

public class SpywareReplacementGenerator extends ReplacementGenerator<SpywareBlockDetails>
{
    private static final String SIMPLE_BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<script id='metavizeDetect' type='text/javascript'>\n"
        + "var e = document.getElementById(\"metavizeDetect\")\n"
        + "if (window == window.top && e.parentNode.tagName == \"BODY\") {\n"
        + "  document.writeln(\"<center><b>Untangle Spyware Blocker</b></center>\")\n"
        + "  document.writeln(\"<p>This site blocked because it may be a spyware site.</p>\")\n"
        + "  document.writeln(\"<p>Host: %s</p>\")\n"
        + "  document.writeln(\"<p>URI: %s</p>\")\n"
        + "  document.writeln(\"<p>Please contact %s.</p>\")\n"
        + "}\n"
        + "</script>"
        + "</BODY></HTML>";

    public SpywareReplacementGenerator(NodeId tid)
    {
        super(tid);
    }

    protected String getReplacement(SpywareBlockDetails bd)
    {
        UvmContext uvm = UvmContextFactory.context();
        String contactHtml = uvm.brandingManager().getContactHtml();
        return String.format(SIMPLE_BLOCK_TEMPLATE, bd.getHost(), bd.getUrl(), contactHtml);
    }

    protected String getRedirectUrl(String nonce, String host, NodeId tid)
    {
        return "http://" + host + "/spyware/detect.jsp?nonce=" + nonce
            + "&tid=" + tid;
    }

}
