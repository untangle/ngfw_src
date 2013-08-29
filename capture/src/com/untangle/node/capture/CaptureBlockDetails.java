/**
 * $Id: CaptureBlockDetails.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.node.http.BlockDetails;

@SuppressWarnings("serial")
public class CaptureBlockDetails extends BlockDetails
{
    private final String method;

    // constructor ------------------------------------------------------------

    public CaptureBlockDetails(String host, String uri, String method)
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
