/**
 * $Id: CaptureBlockDetails.java,v 1.00 2011/12/27 09:42:36 mahotz Exp $
 */

package com.untangle.node.capture;

import com.untangle.node.http.BlockDetails;

@SuppressWarnings("serial")
public class CaptureBlockDetails extends BlockDetails
{
    private final String reason;

    // constructor ------------------------------------------------------------

    public CaptureBlockDetails(String host, String uri, String reason)
    {
        super(host, uri);
        this.reason = reason;
    }

    // public methods ---------------------------------------------------------
    public String getReason()
    {
        return reason;
    }
}
