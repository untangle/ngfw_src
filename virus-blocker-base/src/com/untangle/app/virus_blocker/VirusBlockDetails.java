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

    /**
     * Constructor
     * 
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @param reason
     *        The reason the content was blocked
     * @param vendor
     *        The vendor that detected the virus
     */
    public VirusBlockDetails(String host, String uri, String reason, String vendor)
    {
        super(host, uri);
        this.reason = reason;
        this.vendor = vendor;
    }

    /**
     * Get the reason
     * 
     * @return The reason
     */
    public String getReason()
    {
        return reason;
    }

    /**
     * Get the vendor
     * 
     * @return The vendor
     */
    public String getVendor()
    {
        return vendor;
    }
}
