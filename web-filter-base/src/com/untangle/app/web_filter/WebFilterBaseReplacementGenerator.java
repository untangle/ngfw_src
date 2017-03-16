package com.untangle.app.web_filter;

import java.net.InetAddress;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for WebFilter.
 */
public class WebFilterBaseReplacementGenerator extends ReplacementGenerator<WebFilterBlockDetails>
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

    public WebFilterBaseReplacementGenerator(AppSettings nodeId)
    {
        super(nodeId);
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
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        return "http://" + host + "/web-filter/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
    }
}
