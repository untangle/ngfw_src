/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;

/**
 * Holds information about why a page was blocked.
 */
@SuppressWarnings("serial")
public class BlockDetails implements Serializable
{
    private static final int MAX_LEN = 40;

    private final String host;
    private final String uri;

    /**
     * Create a BlockDetails instance for the following host and URI
     * @param host - the host (being blocked)
     * @param uri - the URI (being blocked)
     */
    public BlockDetails(String host, String uri)
    {
        this.host = host;
        this.uri = uri;
    }

    /**
     * Get the host for this BlockDetails
     * @return the host
     */
    public String getHost()
    {
        return host;
    }

    /**
     * Get a pretty formatted host string
     * for display on the block page
     * @return the formatted host
     */
    public String getFormattedHost()
    {
        return null == host ? "" : truncateString(host, MAX_LEN);
    }
    
    /**
     * Returns the host that should be added to the unblock list
     * if the user unblocks this blocked request
     * This is just getHost() with the "www." removed if present
     * @return the host to unblock
     */
    public String getUnblockHost()
    {
        if (null == host) {
            return null;
        } if (host.startsWith("www.") && 4 < host.length()) {
            return host.substring(4);
        } else {
            return host;
        }
    }

    /**
     * Get the URI for this BlockDetails
     * @return the URI
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Get the full URL for the block details
     * This is the host + uri or just the back button
     * if the host is not known
     * @return the full URL
     */
    public String getUrl()
    {
        if (host == null) {
            return "javascript:history.back()";
        } else {
            return "http://" + host + uri;
        }
    }

    /**
     * Get a pretty formatted URL string
     * for display on the block page
     * @return the pretty formatted URL string
     */
    public String getFormattedUrl()
     {
        if (host == null) {
            return "";
        } else if (uri == null) {
            return truncateString("http://" + host, MAX_LEN);
        } else {
            return truncateString("http://" + host + uri, MAX_LEN);
        }
    }
    
    /**
     * Truncate the provided strength to maxLen if it is longer
     * @param s - the original string
     * @param maxLen - the maximum length
     * @return the new (truncated) string
     */
    private String truncateString(String s, int maxLen)
    {
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

}
