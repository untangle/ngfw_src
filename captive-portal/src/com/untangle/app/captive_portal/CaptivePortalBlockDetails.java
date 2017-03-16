/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import com.untangle.app.http.BlockDetails;

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
