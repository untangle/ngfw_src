/**
 * $Id$
 */
package com.untangle.app.http;

import java.io.Serializable;
import java.util.Map;

/**
 * Holds information about why a page was blocked.
 */
@SuppressWarnings("serial")
public class RedirectDetails implements Serializable
{
    private static final int MAX_LEN = 40;

    private String host;
    private String uri;
    protected String redirectUrl;
    protected Map<String,Object> redirectParameters;

    /**
     * Create a RedirectDetails instance for the following host and URI
     * @param host - the host (being redirected from )
     * @param uri - the URI (being redirected from)
     */
    public RedirectDetails(String host, String uri)
    {
        this.host = host;
        this.uri = uri;
        this.redirectUrl = null;
        this.redirectParameters = null;
    }

    /**
     * Create a RedirectDetails instance for the following host and URI
     * @param host - the host (being redirected from)
     * @param uri - the URI (being redirected from)
     * @param redirectUrl - URL to redirect session to
     * @param redirectParameters - Map of string names to object values.
     */
    public RedirectDetails(String host, String uri, String redirectUrl, Map<String,Object> redirectParameters)
    {
        this.host = host;
        this.uri = uri;
        this.redirectUrl = redirectUrl;
        this.redirectParameters = redirectParameters;
    }

    /**
     * Get the host for this RedirectDetails
     * @return the host
     */
    public String getHost()
    {
        return host;
    }

    /**
     * Get a pretty formatted host string
     * for display on the block page
     * @return the formatted host
     */
    public String getFormattedHost()
    {
        return null == host ? "" : truncateString(host, MAX_LEN);
    }
    
    /**
     * Returns the host that should be added to the unblock list
     * if the user unblocks this blocked request
     * This is just getHost() with the "www." removed if present
     * @return the host to unblock
     */
    public String getUnblockHost()
    {
        if (null == host) {
            return null;
        } if (host.startsWith("www.") && 4 < host.length()) {
            return host.substring(4);
        } else {
            return host;
        }
    }

    /**
     * Get the URI for this RedirectDetails
     * @return the URI
     */
    public String getUri()
    {
        return uri;
    }

    /**
     * Get the full URL for the block details
     * This is the host + uri or just the back button
     * if the host is not known
     * @return the full URL
     */
    public String getUrl()
    {
        if (host == null) {
            return "javascript:history.back()";
        } else {
            return "http://" + host + uri;
        }
    }

    /**
     * Get a pretty formatted URL string
     * for display on the block page
     * @return the pretty formatted URL string
     */
    public String getFormattedUrl()
     {
        if (host == null) {
            return "";
        } else if (uri == null) {
            return truncateString("http://" + host, MAX_LEN);
        } else {
            return truncateString("http://" + host + uri, MAX_LEN);
        }
    }

    /**
     * Return the redirectUrl
     * @return String of redirectUrl or null if not defined.
     */
    public String getRedirectUrl()
    {
        return redirectUrl;
    }

    /**
     * Set the redirectUrl
     * @param redirectUrl String of redirect url.
     */
    public void setRedirectUrl(String redirectUrl){
        this.redirectUrl = redirectUrl;
    }

    /**
     * Return the redirectParameters
     * @return Map of redirect parameters, string to value.
     */
    public Map<String,Object> getRedirectParameters()
    {
        return redirectParameters;
    }

    /**
     * Set the redirectParameters
     * @param redirectParameters Map of redirect parameters, string to value.
     */
    public void setRedirectParameters(Map<String,Object> redirectParameters){
        this.redirectParameters = redirectParameters;
    }
    
    /**
     * Truncate the provided strength to maxLen if it is longer
     * @param s - the original string
     * @param maxLen - the maximum length
     * @return the new (truncated) string
     */
    private String truncateString(String s, int maxLen)
    {
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

}
