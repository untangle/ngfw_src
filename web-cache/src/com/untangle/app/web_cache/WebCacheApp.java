/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.util.Timer;
import java.util.LinkedList;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.util.I18nUtil;

/**
 * The Web Cache application provides caching of HTTP content by implementing an
 * interface between Squid and Untangle. We satisfy client requests using
 * content from cache when available. We request the content from the external
 * server and send it to both the client and the cache when we don't already
 * have it, making it available for subsequent requests.
 * 
 * @author mahotz
 * 
 */
public class WebCacheApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());
    private final Subscription webSub = new Subscription(Protocol.TCP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(80, 80));

    protected static final String STAT_HIT = "hit";
    protected static final String STAT_MISS = "miss";
    protected static final String STAT_USER_BYPASS = "user-bypass";
    protected static final String STAT_SYSTEM_BYPASS = "system-bypass";

    private final String CLEAR_CACHE_SCRIPT = System.getProperty("uvm.home") + "/bin/web-cache-clear";

    private final WebCacheStreamHandler s_handler = new WebCacheStreamHandler(this);

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    protected WebCacheStatistics statistics;
    protected WebCacheSettings settings;
    protected boolean highLoadBypass = false;
    protected Timer timer;

    // size of the buffer we use to assemble the complete HTTP request from the client
    protected final int CLIENT_BUFFSIZE = 0x8000;

    // size of the buffer we use to stream cache hits from squid to client
    protected final int STREAM_BUFFSIZE = 0x4000;

    // milliseconds to wait for squid to respond (hit) or wakeup (miss) a client request
    protected final int SELECT_TIMEOUT = 5000;

    // set this value to enable raw socket logging
    protected final boolean SOCKET_DEBUG = false;

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WebCacheApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        logger.debug("WebCache WebCacheApp()");

        this.addMetric(new AppMetric(STAT_HIT, I18nUtil.marktr("Cache hits")));
        this.addMetric(new AppMetric(STAT_MISS, I18nUtil.marktr("Cache misses")));
        this.addMetric(new AppMetric(STAT_USER_BYPASS, I18nUtil.marktr("User Bypass")));
        this.addMetric(new AppMetric(STAT_SYSTEM_BYPASS, I18nUtil.marktr("System Bypass")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("web-cache", this, webSub, s_handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 16, true);
        this.connectors = new PipelineConnector[] { connector };

        statistics = new WebCacheStatistics();
    }

    /**
     * Gets the cache statistics
     * 
     * @return The cache statistics
     */
    public WebCacheStatistics getStatistics()
    {
        logger.debug("WebCache getStatistics()");
        return (statistics);
    }

    /**
     * Gets the application settings
     * 
     * @return The application settings
     */
    public WebCacheSettings getSettings()
    {
        logger.debug("WebCache getSettings()");
        return (settings);
    }

    /**
     * Sets new application settings
     * 
     * @param newSettings
     *        The new application settings
     */
    public void setSettings(WebCacheSettings newSettings)
    {
        logger.debug("WebCache setSettings()");
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = getAppSettings().getId().toString();

        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/web-cache/settings_" + appID + ".js", newSettings);
        } catch (Exception exn) {
            logger.error("Unable to save settings", exn);
            return;
        }

        this.settings = newSettings;
    }

    /**
     * Gets the list of rules
     * 
     * @return The list of rules
     */
    public LinkedList<WebCacheRule> getRules()
    {
        logger.debug("WebCache getRules()");
        return (settings.getRules());
    }

    /**
     * Sets the list of rules
     * 
     * @param ruleList
     *        The list of rules to set
     */
    public void setRules(LinkedList<WebCacheRule> ruleList)
    {
        logger.debug("WebCache setRules()");
        settings.setRules(ruleList);
    }

    /**
     * Clears the Squid cache. We do this by stopping squid, getting the cache
     * directory from the squid configuration file, deleting all of the cache
     * directories, and restarting squid which will automatically recreate them.
     * 
     * @return 0 for success, any other value indicates an error occured
     */
    public int clearSquidCache()
    {
        logger.debug("WebCache ENTER clearSquidCache()");

        try {
            UvmContextFactory.context().execManager().exec(CLEAR_CACHE_SCRIPT);
        }

        catch (Exception exn) {
            logger.error("Unable to clear cache", exn);
            return (1);
        }

        logger.debug("WebCache LEAVE clearSquidCache()");
        return (0);
    }

    /**
     * Get our pipeline connectors
     * 
     * @return Our pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called before the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount("squid");

        WebCacheParent.INSTANCE.connect();
        timer = new Timer();
        timer.schedule(new WebCacheTimer(this), 60000, 60000);
    }

    /**
     * Called after the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag.
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount("squid");

        timer.cancel();
        WebCacheParent.INSTANCE.goodbye();
    }

    /**
     * Called after application initialization.
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = getAppSettings().getId().toString();
        WebCacheSettings readSettings = null;

        try {
            readSettings = settingsManager.load(WebCacheSettings.class, System.getProperty("uvm.settings.dir") + "/web-cache/settings_" + appID + ".js");

            if (readSettings == null) {
                settings = new WebCacheSettings();
                LinkedList<WebCacheRule> ruleList = new LinkedList<WebCacheRule>();
                ruleList.add(new WebCacheRule("maps.google.com", true));
                settings.setRules(ruleList);
            } else {
                this.settings = readSettings;
            }

            logger.debug("Settings:\n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception exn) {
            logger.error("Unable to load settings", exn);
        }
    }

    /**
     * Check for a valid license
     * 
     * @return True if license is valid, otherwise false
     */
    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WEB_CACHE)) return true;
        return false;
    }
}
