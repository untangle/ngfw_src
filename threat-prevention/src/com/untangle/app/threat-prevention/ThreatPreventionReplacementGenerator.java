/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.HashMap;

/**
 * ReplacementGenerator for WebFilter.
 */
public class ThreatPreventionReplacementGenerator extends ReplacementGenerator<ThreatPreventionBlockDetails>
{
    public static final HashMap<String,Object> BLOCK_PARAMETERS;
    static {
        BLOCK_PARAMETERS = new HashMap<>();
        BLOCK_PARAMETERS.put("nonce", null);
        BLOCK_PARAMETERS.put("appid", null);
    };

    private String uriBase = null;
    private String urlBase = null;

    private static final String BLOCK_URI = "/threat-prevention/blockpage";

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
    public ThreatPreventionReplacementGenerator(AppSettings appId)
    {
        super(appId);
    }

    /**
     * Get the replacement page
     * 
     * @param details
     *      The block details
     * @return The replacement
     */
    @Override
    protected String getReplacement(ThreatPreventionBlockDetails details)
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
        return "http://" + host + "/threat-prevention/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
    }

    /**
     * Get redirect URL using details redirectUrl and redirectParameters.
     *
     * @param details WebFilterRedirectDetails.
     * @param host Host address for url if defined.
     * @param appSettings Application settings.
     * @return         Formatted URL with parameters
     */
    protected String getRedirectUrl(ThreatPreventionBlockDetails details, String host, AppSettings appSettings){
        details.setRedirectUrl("http://" + host + BLOCK_URI );
        details.setRedirectParameters(new HashMap<String,Object>(BLOCK_PARAMETERS));
        return super.getRedirectUrl(details, host, appSettings);
    }
}
