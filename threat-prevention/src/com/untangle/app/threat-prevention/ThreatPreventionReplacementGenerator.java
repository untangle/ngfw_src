/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

import org.apache.http.client.utils.URIBuilder;

/**
 * ReplacementGenerator for ThreatPrevention.
 */
public class ThreatPreventionReplacementGenerator extends ReplacementGenerator<ThreatPreventionBlockDetails>
{
    private ThreatPreventionApp threatpreventionApp = null;
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
     * @param appId The application ID
     * @param app   The application instance that created us
     */
    public ThreatPreventionReplacementGenerator(AppSettings appId, ThreatPreventionApp app)
    {
        super(appId);
        this.redirectUri.setPath("/threat-prevention/blockpage");
        this.threatpreventionApp = app;
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
     * If using a custom block page, use that uri instead of default.
     *
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri() {
        if (threatpreventionApp.getSettings().getCustomBlockPageEnabled()) {
            URIBuilder redirectUri = null;
            try {
                redirectUri = new URIBuilder(threatpreventionApp.getSettings().getCustomBlockPageUrl());
            } catch (Exception e) {
            }
            return redirectUri;
        } else {
            return super.getRedirectUri();
        }
    }
}
