/**
 * $Id$
 */
package com.untangle.node.virus;

import com.untangle.node.http.BlockDetails;

/**
 * BlockDetails for Virus.
 *
 */
@SuppressWarnings("serial")
public class VirusBlockDetails extends BlockDetails
{
    
    private final String reason;
    private final String vendor;

    // constructor ------------------------------------------------------------

    public VirusBlockDetails(String host,
                             String uri, String reason,
                             String vendor)
    {
        super(host, uri);
        this.reason = reason;
        this.vendor = vendor;
    }

    // public methods ---------------------------------------------------------
    public String getReason()
    {
        return reason;
    }

    public String getVendor()
    {
        return vendor;
    }
}
