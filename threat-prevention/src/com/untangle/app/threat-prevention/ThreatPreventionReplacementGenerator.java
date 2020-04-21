/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;

import com.untangle.app.web_filter.WebFilterBase;

import java.util.List;
import org.apache.http.client.utils.URIBuilder;
import java.util.HashMap;

/**
 * ReplacementGenerator for Threat Prevention.
 */
public class ThreatPreventionReplacementGenerator extends ReplacementGenerator<ThreatPreventionBlockDetails>
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
        UvmContext uvm = UvmContextFactory.context();

        return String.format(BLOCK_TEMPLATE, details.getHeader(),
                             details.getHost(), details.getUri(),
                             details.getReason(),
                             uvm.brandingManager().getContactHtml());
    }

    /**
     * If using a custom block page in WebFilter, use that uri instead of default.
     *
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri()
    {
        UvmContext uvm = UvmContextFactory.context();
        AppManager nm = uvm.appManager();
        List<App> wfInstances = nm.appInstances("web-filter");

        if (wfInstances.size() == 0) {
            return super.getRedirectUri();
        }

        App app = wfInstances.get(0);
        WebFilterBase wfApp = (WebFilterBase) nm.app(app.getAppSettings().getId());
        if (wfApp.getSettings().getCustomBlockPageEnabled()) {
            URIBuilder redirectUri = null;
            try {
                redirectUri = new URIBuilder(wfApp.getSettings().getCustomBlockPageUrl());
            } catch(Exception e) {}
            return redirectUri;
        } else {
            return super.getRedirectUri();
        }
    }
}
