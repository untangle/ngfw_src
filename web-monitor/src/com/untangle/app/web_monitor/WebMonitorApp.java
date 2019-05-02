/**
 * $Id: WebMonitorApp.java 43848 2016-07-22 22:05:42Z mahotz $
 */

package com.untangle.app.web_monitor;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.PasswordUtil;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.app.GenericRule;
import com.untangle.app.web_filter.DecisionEngine;
import com.untangle.app.web_filter.WebFilterBase;
import com.untangle.app.web_filter.WebFilterReplacementGenerator;
import com.untangle.app.web_filter.WebFilterSettings;
import com.untangle.app.web_filter.WebFilterRule;
import com.untangle.app.web_filter.WebFilterDecisionEngine;
import com.untangle.app.web_filter.WebFilterHttpsSniHandler;
import com.untangle.app.web_filter.WebFilterQuicHandler;
import com.untangle.app.web_filter.WebFilterHandler;

/**
 * The Web Monitor application. The bulk of the functionality lives in the
 * WebFilterbase class.
 * 
 * @author mahotz
 * 
 */
public class WebMonitorApp extends WebFilterBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final WebFilterDecisionEngine engine = new WebFilterDecisionEngine(this);

    private final WebFilterHttpsSniHandler sniHandler = new WebFilterHttpsSniHandler(this);
    private final WebFilterQuicHandler quicHandler = new WebFilterQuicHandler(this);

    private final Subscription httpsSub = new Subscription(Protocol.TCP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));
    private final Subscription quicSub = new Subscription(Protocol.UDP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));

    private final PipelineConnector httpConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-http", this, null, new WebFilterHandler(this), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 2, true);
    private final PipelineConnector httpsSniConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-https-sni", this, httpsSub, sniHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true);
    private final PipelineConnector quicConnector = UvmContextFactory.context().pipelineFoundry().create("web-filter-quic", this, quicSub, quicHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 2, true);
    private final PipelineConnector[] connectors = new PipelineConnector[] { httpConnector, httpsSniConnector, quicConnector };

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WebMonitorApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);
    }

    /**
     * Called to unblock a blocked site
     * 
     * @param nonce
     *        The nonce from the web request
     * @param global
     *        The global flag
     * @param password
     *        The unblock password
     * @return True if unblocked, otherwise false
     */
    public boolean unblockSite(String nonce, boolean global, String password)
    {
        if (!this.verifyPassword(password)) {
            if (this.logger.isInfoEnabled()) {
                logger.info("Unable to verify the password for nonce: '" + nonce + "'");
            }
            return false;
        } else {
            if (this.logger.isInfoEnabled()) {
                logger.info("Verified the password for nonce: '" + nonce + "'");
            }
            return super.unblockSite(nonce, global);
        }
    }

    /**
     * Clears the decision engine cache
     */
    public void clearCache()
    {
        this.engine.clearCache();
    }

    /**
     * Called to lookup a specific site
     * 
     * @param url
     *        The site to lookup
     * @return The list of categories
     */
    public List<Integer> lookupSite(String url)
    {
        return this.engine.lookupSite(url);
    }

    /**
     * Called to submit request to recategorize site
     * 
     * @param url
     *        The site to be recategorized
     * @param category
     *        The new category
     * @return The result
     */
    public int recategorizeSite(String url, int category)
    {
        return this.engine.recategorizeSite(url, category);
    }

    /**
     * Called to check the unblock password
     * 
     * @param password
     *        to check
     * @return True if password is enabled and valid, otherwise false
     */
    private boolean verifyPassword(String password)
    {
        WebFilterSettings settings = getSettings();

        if (settings == null) {
            logger.info("Settings are null, assuming password is not required.");
            return true;
        }

        if (!settings.getUnblockPasswordEnabled()) {
            return true;
        }

        if (password == null) {
            return false;
        }

        if (settings.getUnblockPasswordAdmin()) {
            AdminSettings as = UvmContextFactory.context().adminManager().getSettings();
            for (AdminUserSettings user : as.getUsers()) {
                if (user.getUsername().equals("admin")) {
                    if (PasswordUtil.check(password, user.trans_getPasswordHash())) {
                        return true;
                    }

                    return false;
                }
            }

            return false;
        }

        if (password.equals(settings.getUnblockPassword())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Called to get our decision engine instance
     * 
     * @return The decision engine
     */
    @Override
    public DecisionEngine getDecisionEngine()
    {
        return engine;
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {

        getDecisionEngine().start();

        super.preStart(isPermanentTransition);
    }

    /**
     * Called after the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        super.postStart(isPermanentTransition);
    }

    /**
     * Called after the application has stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        getDecisionEngine().stop();

        super.postStop(isPermanentTransition);
    }

    /**
     * Called to get pipeline connectors
     * 
     * @return Pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Create a replacement generator
     * 
     * @return Replacement generator
     */
    @Override
    protected WebFilterReplacementGenerator buildReplacementGenerator()
    {
        return new WebFilterReplacementGenerator(getAppSettings());
    }

    /**
     * Called to get the application title
     * 
     * @return The application title
     */
    @Override
    public String getAppTitle()
    {
        return "Web Monitor";
    }

    /**
     * Called to get app name of the package
     * 
     * @return The app package name
     */
    @Override
    public String getName()
    {
        return "web_monitor";
    }

    /**
     * Called to get the application name
     * 
     * @return The application name
     */
    @Override
    public String getAppName()
    {
        return "web-monitor";
    }

    /**
     * Called to determine if the app is free or premium
     * 
     * @return
     */
    @Override
    public boolean isPremium()
    {
        return false;
    }

    /**
     * Called to initialize application settings
     * 
     * @param settings
     *        The new settings
     */
    @Override
    public void initializeSettings(WebFilterSettings settings)
    {
        LinkedList<GenericRule> categories = new LinkedList<GenericRule>();

        addCategories(categories);
        settings.setCategories(categories);
    }

    /**
     * This hook function is called just before the base app writes and
     * activates new settings, so this is where we look for and disable the
     * block flag on all rules since we do monitoring only.
     * 
     * @param argSettings
     *        The application settings
     */
    @Override
    public void fixupSetSettings(WebFilterSettings argSettings)
    {
        super.fixupSetSettings(argSettings);

        List<WebFilterRule> filterRules = argSettings.getFilterRules();
        for (WebFilterRule item : filterRules) {
            if (item.getBlocked()) {
                item.setBlocked(false);
                item.setFlagged(true);
            }
        }

        List<GenericRule> categories = argSettings.getCategories();
        for (GenericRule item : categories) {
            if (item.getBlocked()) {
                item.setBlocked(false);
                item.setFlagged(true);
            }
        }

        List<GenericRule> blockedUrls = argSettings.getBlockedUrls();
        for (GenericRule item : blockedUrls) {
            if (item.getBlocked()) {
                item.setBlocked(false);
                item.setFlagged(true);
            }
        }
    }
}
