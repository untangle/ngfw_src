package com.untangle.node.webfilter;

import java.net.InetAddress;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.security.NodeId;

/**
 * ReplacementGenerator for WebFilter.
 */
public class WebFilterReplacementGenerator extends ReplacementGenerator<WebFilterBlockDetails>
{
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because of inappropriate content</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    public WebFilterReplacementGenerator(NodeId tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(WebFilterBlockDetails details)
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeId tid)
    {
        return "http://" + host + "/webfilter/blockpage?nonce=" + nonce + "&tid=" + tid;
    }
}
