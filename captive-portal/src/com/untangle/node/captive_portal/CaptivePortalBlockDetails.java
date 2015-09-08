/**
 * $Id$
 */

package com.untangle.node.captive_portal;

import com.untangle.node.http.BlockDetails;

@SuppressWarnings("serial")
public class CaptivePortalBlockDetails extends BlockDetails
{
    private final String method;

    // constructor ------------------------------------------------------------

    public CaptivePortalBlockDetails(String host, String uri, String method)
    {
        super(host, uri);
        this.method = method;
    }

    // public methods ---------------------------------------------------------
    public String getMethod()
    {
        return method;
    }

    public String toString()
    {
        return ("HOST:" + getHost() + " URI:" + getUri() + " METHOD:" + getMethod());
    }
}
