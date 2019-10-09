/**
 * $Id$
 */

package com.untangle.app.ip_reputation;

import java.net.InetAddress;

import com.untangle.app.http.BlockDetails;

/**
 * BlockDetails for IpReputation.
 */
@SuppressWarnings("serial")
public class IpReputationBlockDetails extends BlockDetails
{
    private final IpReputationSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String appTitle;

    /**
     * Constructor
     * 
     * @param settings
     *        The weeb filter settings
     * @param host
     *        The host
     * @param uri
     *        The URI
     * @param reason
     *        The reason
     * @param clientAddr
     *        The client address
     */
    // public IpReputationBlockDetails(IpReputationSettings settings, String host, String uri, String reason, InetAddress clientAddr, String appTitle)
    public IpReputationBlockDetails(IpReputationSettings settings, String host, String uri, String reason, InetAddress clientAddr)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        // this.appTitle = appTitle;
        this.appTitle = getAppTitle();
    }

    /**
     * Get the header
     * 
     * @return The header
     */
    public String getHeader()
    {
        return "IP Reputation";
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
     * Get the app title
     * 
     * @return The title
     */
    public String getAppTitle()
    {
        // return appTitle;
        return "IP Reputation";
    }

    /**
     * Get the client address
     * 
     * @return The client address
     */
    public InetAddress getClientAddress()
    {
        return clientAddr;
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public IpReputationSettings getSettings()
    {
        return settings;
    }
}
