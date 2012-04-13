package com.untangle.node.webfilter;

import java.net.InetAddress;

import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;

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
        + "<p>This site is blocked because it violates network policy.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    public WebFilterReplacementGenerator(NodeSettings tid)
    {
        super(tid);
    }

    @Override
    protected String getReplacement(WebFilterBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    @Override
    protected String getRedirectUrl(String nonce, String host, NodeSettings tid)
    {
        return "http://" + host + "/webfilter/blockpage?nonce=" + nonce + "&tid=" + tid;
    }
}
