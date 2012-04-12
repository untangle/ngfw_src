/*
 * $Id$
 */
package com.untangle.node.phish;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NodeSettings;

class PhishReplacementGenerator extends ReplacementGenerator<PhishBlockDetails>
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
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    PhishReplacementGenerator(NodeSettings tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(PhishBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHost(),
                             details.getUri(), uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeSettings tid)
    {
        return "http://" + host + "/phish/blockpage?nonce=" + nonce
            + "&tid=" + tid;
    }
}
