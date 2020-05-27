/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.network.NetworkSettings;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;

/**
 * ReplacementGenerator for WebFilter.
 */
public class ThreatPreventionReplacementGenerator extends ReplacementGenerator<ThreatPreventionBlockDetails>
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
        this.redirectUri.setPath("/threat-prevention/blockpage");
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
        if(details == null){
            return "";
        }
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
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
