/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;

/**
 * ReplacementGenerator for Virus.
 */
class VirusReplacementGenerator extends ReplacementGenerator<VirusBlockDetails>
{
    private static final HashMap<String, Object> CustomBlockRedirectParameters;
    static {
        CustomBlockRedirectParameters = new HashMap<>();
        CustomBlockRedirectParameters.put("host", null);
        CustomBlockRedirectParameters.put("url", null);
    };

    private VirusBlockerBaseApp virusblockerApp = null;

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

    /**
     * Constructor
     *
     * @param appId The application ID
     * @param app The application instance that created us
     */
    VirusReplacementGenerator(AppSettings appId, VirusBlockerBaseApp app)
    {
        super(appId);
        this.redirectUri.setPath("/virus/blockpage");
        this.virusblockerApp = app;
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
     * If using a custom block page, use that uri instead of default.
     *
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri() {
        if (virusblockerApp.getSettings().getCustomBlockPageEnabled()) {
            URIBuilder redirectUri = null;
            try {
                redirectUri = new URIBuilder();
                redirectUri = new URIBuilder(virusblockerApp.getSettings().getCustomBlockPageUrl());
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
        if (virusblockerApp.getSettings().getCustomBlockPageEnabled()) {
            return new HashMap<String, Object>(CustomBlockRedirectParameters);
        } else {
            return super.getRedirectParameters();
        }
    }
}
