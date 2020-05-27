/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.network.NetworkSettings;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

/**
 * ReplacementGenerator for Virus.
 */
class VirusReplacementGenerator extends ReplacementGenerator<VirusBlockDetails>
{
    private static final HashMap<String, Object> CustomBlockRedirectParameters;
    static {
        CustomBlockRedirectParameters = new HashMap<>();
        CustomBlockRedirectParameters.put("reason", null);
        CustomBlockRedirectParameters.put("appid", null);
        CustomBlockRedirectParameters.put("appname", null);
        CustomBlockRedirectParameters.put("host", null);
        CustomBlockRedirectParameters.put("url", null);
        CustomBlockRedirectParameters.put("clientAddress", null);
    };
// THIS IS FOR ECLIPSE - @formatter:off
    private static final String BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<center><b>%s</b></center>"
        + "<p>This site is blocked because it contains a virus.</p>"
        + "<p>Host: %s</p>"
        + "<p>URI: %s</p>"
        + "<p>Please contact %s</p>"
        + "</BODY></HTML>";
// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Constructor
     *
     * @param tid
     *        Application settings
     */
    VirusReplacementGenerator(AppSettings tid)
    {
        super(tid);
        this.redirectUri.setPath("/virus/blockpage");
    }

    /**
     * Get the replacement
     *
     * @param details
     *        The block details
     * @return The replacement
     */
    @Override
    protected String getReplacement(VirusBlockDetails details)
    {
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getVendor(), details.getHost(), details.getUri(), details.getReason(), uvm.brandingManager().getContactHtml());
    }

    /**
     * If using a global custom block page, use that uri instead of default.
     *
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri() {
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();

        Boolean globalRedirectEnabled = networkSettings.getGlobalCustomBlockPageEnabled();
        String globalRedirectUrl = networkSettings.getGlobalCustomBlockPageUrl();

        if (globalRedirectEnabled) {
            URIBuilder redirectUri = null;
            try {
                redirectUri = new URIBuilder();
                redirectUri = new URIBuilder(globalRedirectUrl);
            } catch (Exception e) {
            }
            return redirectUri;
        } else {
            return super.getRedirectUri();
        }
    }

    /**
     * If using a custom block page, use set of custom parameters instead of
     * defaults.
     *
     * @return New copy of map for parameters.
     */
    protected Map<String, Object> getRedirectParameters() {
        if (UvmContextFactory.context().networkManager().getNetworkSettings().getGlobalCustomBlockPageEnabled()) {
            return new HashMap<String, Object>(CustomBlockRedirectParameters);
        } else {
            return super.getRedirectParameters();
        }
    }
}
