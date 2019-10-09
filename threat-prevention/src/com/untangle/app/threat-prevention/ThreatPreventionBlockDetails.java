/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import java.net.InetAddress;

import com.untangle.app.http.BlockDetails;

/**
 * BlockDetails for ThreatPrevention.
 */
@SuppressWarnings("serial")
public class ThreatPreventionBlockDetails extends BlockDetails
{
    private final ThreatPreventionSettings settings;
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
    public ThreatPreventionBlockDetails(ThreatPreventionSettings settings, String host, String uri, String reason, InetAddress clientAddr)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = getAppTitle();
    }

    /**
     * Get the header
     * 
     * @return The header
     */
    public String getHeader()
    {
        return "Threat Prevention";
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
        return "Threat Prevention";
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
    public ThreatPreventionSettings getSettings()
    {
        return settings;
    }
}
