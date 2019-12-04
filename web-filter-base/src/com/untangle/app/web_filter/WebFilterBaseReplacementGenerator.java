/**
 * $Id$
 */

package com.untangle.app.web_filter;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.HashMap;

/**
 * ReplacementGenerator for WebFilter.
 */
public class WebFilterBaseReplacementGenerator extends ReplacementGenerator<WebFilterRedirectDetails>
{
    public static final HashMap<String,Object> BLOCK_PARAMETERS;
    public static final HashMap<String,Object> CUSTOM_BLOCK_PARAMETERS;
    static {
        BLOCK_PARAMETERS = new HashMap<>();
        BLOCK_PARAMETERS.put("nonce", null);
        BLOCK_PARAMETERS.put("appid", null);

        CUSTOM_BLOCK_PARAMETERS = new HashMap<>();
        CUSTOM_BLOCK_PARAMETERS.put("reason", null);
        CUSTOM_BLOCK_PARAMETERS.put("appid", null);
        CUSTOM_BLOCK_PARAMETERS.put("appname", null);
        CUSTOM_BLOCK_PARAMETERS.put("host", null);
        CUSTOM_BLOCK_PARAMETERS.put("url", null);
        CUSTOM_BLOCK_PARAMETERS.put("clientAddress", null);
    };

    private String uriBase = null;
    private String urlBase = null;

    private static final String BLOCK_URI = "/web-filter/blockpage";

    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because it violates network policy.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Reason: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";

    /**
     * Constructor
     *  
     * @param appId
     *      The application ID
     */
    public WebFilterBaseReplacementGenerator(AppSettings appId)
    {
        super(appId);

        this.uriBase = BLOCK_URI;
    }

    /**
     * Get the simple replacement page
     * 
     * @param details
     *      The block details
     * @return The replacement
     */
    @Override
    protected String getReplacement(WebFilterRedirectDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    /**
     * Get the redirect URL
     * 
     * @param nonce
     *      The nonce
     * @param host
     *      The host
     * @param appSettings
     *      The application settings
     * @return The redirect URL
     */
    @Override
    protected String getRedirectUrl(String nonce, String host, AppSettings appSettings)
    {
        return "http://" + host + BLOCK_URI + "?nonce=" + nonce + "&appid=" + appSettings.getId();
    }

    /**
     * Get redirect URL using details redirectUrl and redirectParameters.
     *
     * @param details WebFilterRedirectDetails.
     * @param host Host address for url if defined.
     * @param appSettings Application settings.
     * @return         Formatted URL with parameters
     */
    protected String getRedirectUrl(WebFilterRedirectDetails details, String host, AppSettings appSettings){
        if(details.getBlocked()){
            if(details.getSettings().getCustomBlockPageEnabled()){
                details.setRedirectUrl(details.getSettings().getCustomBlockPageUrl());
                details.setRedirectParameters(new HashMap<String,Object>(CUSTOM_BLOCK_PARAMETERS));
            }else{
                details.setRedirectUrl("http://" + host + BLOCK_URI );
                details.setRedirectParameters(new HashMap<String,Object>(BLOCK_PARAMETERS));
            }
        }
        return super.getRedirectUrl(details, host, appSettings);
    }
}
