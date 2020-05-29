/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

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
 * ReplacementGenerator for Virus.
 */
class VirusReplacementGenerator extends ReplacementGenerator<VirusBlockDetails>
{
    private VirusBlockerBaseApp vbApp = null;
    private WebFilterBase wfApp = null;

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
     * @param vbApp This Virus Blocker application
     */
    public VirusReplacementGenerator(AppSettings appId, VirusBlockerBaseApp vbApp)
    {
        super(appId);
        this.redirectUri.setPath("/virus/blockpage");
        this.vbApp = vbApp;
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
     * Use WebFilter custom block page if defined
     *
     * @return URIBuilder of uri to redirect client toward.
     */
    protected URIBuilder getRedirectUri() {
        UvmContext uvm = UvmContextFactory.context();
        AppManager appManager = uvm.appManager();
        Integer vbAppPolicyId = this.vbApp.getAppSettings().getPolicyId();

        // Get all WebFilter instances
        List<App> wfInstances = appManager.appInstances("web-filter");

        /**
         * Find a running WebFilter instance matching same policy id as
         * Virus Blocker instance
         */
        for (App wfInstance : wfInstances) {
            Integer wfAppPolicyId = wfInstance.getAppSettings().getPolicyId();
            AppSettings.AppState wfAppRunState = wfInstance.getRunState();

            if (vbAppPolicyId.equals(wfAppPolicyId) && wfAppRunState.equals(AppState.RUNNING)) {
                // cast the running WebFilter app
                this.wfApp = (WebFilterBase) appManager.app(wfInstance.getAppSettings().getId());
            }
        }

        /**
         * Use default redirect if no WebFilter instance is running and matching
         * Virus Blocker instance policy id
         */
        if (this.wfApp == null) {
            return super.getRedirectUri();
        }

        /**
         * Use WebFilter custom block page for Virus Blocker too
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
