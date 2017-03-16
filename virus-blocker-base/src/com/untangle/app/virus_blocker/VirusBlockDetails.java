/**
 * $Id$
 */
package com.untangle.app.virus_blocker;

import com.untangle.app.http.BlockDetails;

/**
 * BlockDetails for Virus.
 *
 */
@SuppressWarnings("serial")
public class VirusBlockDetails extends BlockDetails
{
    private final String reason;
    private final String vendor;

    public VirusBlockDetails(String host, String uri, String reason, String vendor)
    {
        super(host, uri);
        this.reason = reason;
        this.vendor = vendor;
    }

    public String getReason()
    {
        return reason;
    }

    public String getVendor()
    {
        return vendor;
    }
}
