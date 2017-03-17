/*
 * $HeadURL$
 */
package com.untangle.app.web_filter;

import java.net.InetAddress;

import com.untangle.app.http.BlockDetails;

/**
 * BlockDetails for WebFilter.
 */
@SuppressWarnings("serial")
public class WebFilterBlockDetails extends BlockDetails
{
    private final WebFilterSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String appTitle;

    // constructor ------------------------------------------------------------

    public WebFilterBlockDetails(WebFilterSettings settings, String host,
                                 String uri, String reason,
                                 InetAddress clientAddr,
                                 String appTitle)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = appTitle;
    }

    // public methods ---------------------------------------------------------

    public String getHeader()
    {
        return "Web Filter";
    }

    public String getReason()
    {
        return reason;
    }

    public String getAppTitle()
    {
        return appTitle;
    }

    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

    public WebFilterSettings getSettings()
    {
        return settings;
    }
}
