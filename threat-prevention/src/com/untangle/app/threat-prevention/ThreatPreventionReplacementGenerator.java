/**
 * $Id$
 */

package com.untangle.app.ip_reputation;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * ReplacementGenerator for WebFilter.
 */
public class IpReputationReplacementGenerator extends ReplacementGenerator<IpReputationBlockDetails>
{
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
    public IpReputationReplacementGenerator(AppSettings appId)
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
    protected String getReplacement(IpReputationBlockDetails details)
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
        // return "http://" + host + "/web-filter/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
        return "http://" + host + "/ip-reputation/blockpage?nonce=" + nonce + "&appid=" + appSettings.getId();
    }
}
