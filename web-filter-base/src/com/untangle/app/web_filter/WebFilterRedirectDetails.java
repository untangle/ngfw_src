/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.util.Map;

import com.untangle.app.http.RedirectDetails;

/**
 * RedirectDetails for WebFilter.
 */
@SuppressWarnings("serial")
public class WebFilterRedirectDetails extends RedirectDetails
{
    static private final String HEADER = "Web Filter";
    private final WebFilterSettings settings;
    private final String reason;
    private final InetAddress clientAddr;
    private final String appTitle;
    private final String blockVal;
    private final Reason blockType;

    /**
     * Constructor for redirects.
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
     * @param appTitle
     *        The application title
     * @param blockType
     *        The type of block
     * @param blockVal
     *        The blocked value   
     */
    public WebFilterRedirectDetails(WebFilterSettings settings, String host, String uri, String reason, InetAddress clientAddr, String appTitle, Reason blockType, String blockVal)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = appTitle;
        this.blockType = blockType;
        this.blockVal = blockVal;
    }

    /**
     * Get the header
     * 
     * @return The header
     */
    public String getHeader()
    {
        return HEADER;
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
        return appTitle;
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
    public WebFilterSettings getSettings()
    {
        return settings;
    }

     /**
     * Get the Type of block this is
     * 
     * @return The Reason type, representing what type of block this is
     */
    public Reason getBlockType()
    {
        return blockType;
    }

     /**
     * Get the Blocked Value
     * 
     * @return The value of the block
     */
    public String getBlockVal()
    {
        return blockVal;
    }
}
