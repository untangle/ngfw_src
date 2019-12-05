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
    private final Boolean blocked;

    /**
     * Constructor designed for local blocks.
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
     */
    public WebFilterRedirectDetails(WebFilterSettings settings, String host, String uri, String reason, InetAddress clientAddr, String appTitle)
    {
        super(host, uri);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = appTitle;
        this.blocked = true;
    }

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
     * @param blocked
     *        If true, blocked, otherwise redirect
     * @param redirectUrl
     *        Full host + uri url to redirect this session to.
     * @param redirectParameters
     */
    public WebFilterRedirectDetails(WebFilterSettings settings, String host, String uri, String reason, InetAddress clientAddr, String appTitle, Boolean blocked, String redirectUrl, Map<String,Object> redirectParameters)
    {
        super(host, uri, redirectUrl, redirectParameters);
        this.settings = settings;
        this.reason = reason;
        this.clientAddr = clientAddr;
        this.appTitle = appTitle;
        this.blocked = blocked;
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
     * Get block for this redirect.
     * @return Boolean where if true, this is a blocking url, otherwise a reirect to another site.
     */
    public Boolean getBlocked()
    {
        return blocked;
    }

}
