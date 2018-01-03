/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.app.http.BlockDetails;

/**
 * This is the implementation of the captive portal block details used when
 * generating the capture redirect.
 * 
 * @author mahotz
 * 
 */

@SuppressWarnings("serial")
public class CaptivePortalBlockDetails extends BlockDetails
{
    private final String method;

    /**
     * Our constructor.
     * 
     * @param host
     *        The host for the block page
     * @param uri
     *        The URI for the block page
     * @param method
     *        The request method for the block page
     */
    public CaptivePortalBlockDetails(String host, String uri, String method)
    {
        super(host, uri);
        this.method = method;
    }

    /**
     * @return The request method
     */
    public String getMethod()
    {
        return method;
    }

    /**
     * @return A string useful for logging block details
     */
    public String toString()
    {
        return ("HOST:" + getHost() + " URI:" + getUri() + " METHOD:" + getMethod());
    }
}
