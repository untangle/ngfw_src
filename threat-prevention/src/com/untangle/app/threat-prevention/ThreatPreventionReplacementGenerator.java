/**
 * $Id$
 */

package com.untangle.app.threat_prevention;

import com.untangle.app.web_filter.WebFilterBase;

import com.untangle.app.http.ReplacementGenerator;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppManager;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppSettings.AppState;

import java.util.List;
import org.apache.http.client.utils.URIBuilder;

/**
 * ReplacementGenerator for Threat Prevention.
 */
public class ThreatPreventionReplacementGenerator extends ReplacementGenerator<ThreatPreventionBlockDetails>
{
    private ThreatPreventionApp tpApp = null;
    private WebFilterBase wfApp = null;

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
     * @param tpApp This Threat Prevention application
     */
    public ThreatPreventionReplacementGenerator(AppSettings appId, ThreatPreventionApp tpApp)
    {
        super(appId);
        this.redirectUri.setPath("/threat-prevention/blockpage");
        this.tpApp = tpApp;
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
     * Use WebFilter custom block page if defined
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri() {
        UvmContext uvm = UvmContextFactory.context();
        AppManager appManager = uvm.appManager();
        Integer tpAppPolicyId = this.tpApp.getAppSettings().getPolicyId();

        // Get all WebFilter instances
        List<App> wfInstances = appManager.appInstances("web-filter");

        /**
         * Find a running WebFilter instance matching same policy id as Threat Prevention instance
         */
        for (App wfInstance : wfInstances) {
            Integer wfAppPolicyId = wfInstance.getAppSettings().getPolicyId();
            AppSettings.AppState wfAppRunState = wfInstance.getRunState();

            if (tpAppPolicyId.equals(wfAppPolicyId) && wfAppRunState.equals(AppState.RUNNING) ) {
                // cast the running WebFilter app
                this.wfApp = (WebFilterBase) appManager.app(wfInstance.getAppSettings().getId());
            }
        }

        /**
         * Use default redirect if no WebFilter instance is running
         * and matching Threat Prevention instance policy id
         */
        if (this.wfApp == null) {
            return super.getRedirectUri();
        }

        /**
         * Use WebFilter custom block page for Threat Prevention too
         */
        if (this.wfApp.getSettings().getCustomBlockPageEnabled()) {
            URIBuilder redirectUri = null;
            try {
                redirectUri = new URIBuilder();
                redirectUri = new URIBuilder(this.wfApp.getSettings().getCustomBlockPageUrl());
            } catch (Exception e) {
            }
            return redirectUri;
        } else {
            return super.getRedirectUri();
        }
    }
}
