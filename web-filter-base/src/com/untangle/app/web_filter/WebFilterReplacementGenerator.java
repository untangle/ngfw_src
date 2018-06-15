/**
 * $Id: WebFilterReplacementGenerator.java 41284 2015-09-18 07:03:39Z dmorris $
 */

package com.untangle.app.web_filter;

import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for Web Filter.
 */
public class WebFilterReplacementGenerator extends WebFilterBaseReplacementGenerator
{
    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     */
    public WebFilterReplacementGenerator(AppSettings appSettings)
    {
        super(appSettings);
    }

    /**
     * Get the redirect URL
     * 
     * @param nonce
     *        The nonce
     * @param host
     *        The host
     * @param appSettings
     *        The application settings
     * @return The redirect URL
     */
    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        return "http://" + host + "/web-filter/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
    }
}
