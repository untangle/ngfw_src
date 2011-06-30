/*
 * $HeadURL$
 */
package com.untangle.node.webfilter;

import java.net.InetAddress;

import com.untangle.node.http.BlockDetails;

/**
 * BlockDetails for WebFilter.
 */
@SuppressWarnings("serial")
public class WebFilterBlockDetails extends BlockDetails
{
    private final WebFilterSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String nodeTitle;
    private final String uid;

    // constructor ------------------------------------------------------------

    public WebFilterBlockDetails(WebFilterSettings settings, String host,
                                 String uri, String reason,
                                 InetAddress clientAddr,
                                 String nodeTitle, String uid)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.nodeTitle = nodeTitle;
        this.uid = uid;
    }

    // public methods ---------------------------------------------------------

    public String getHeader()
    {
        return settings.getBlockTemplate().getHeader();
    }

    public String getReason()
    {
        return reason;
    }

    public String getNodeTitle()
    {
        return nodeTitle;
    }

    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

    public WebFilterSettings getSettings()
    {
        return settings;
    }

    public String getUid()
    {
        return uid;
    }
}
