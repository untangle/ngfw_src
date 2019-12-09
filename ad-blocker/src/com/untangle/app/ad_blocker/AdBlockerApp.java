/**
 * $Id$
 */
package com.untangle.app.ad_blocker;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Token;
import com.untangle.app.http.BlockDetails;
import com.untangle.app.http.HeaderToken;

/**
 * The Main Ad Blocker Application class
 */
public class AdBlockerApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final AdBlockerReplacementGenerator replacementGenerator;

    private AdBlockerSettings settings = null;

    private static final String STAT_SCAN = "scan";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_PASS = "pass";

    private UrlMatcher blockingUrlMatcher = new UrlMatcher();
    private UrlMatcher passingUrlMatcher = new UrlMatcher();

    // maybe use cache? not for now
    public static final boolean USE_CACHE = false;
    public static final int MAX_CACHED_ENTRIES = 1000;
    protected Map<String, GenericRule> cache = new ConcurrentHashMap<>();

    public static final String LIST_URL = "https://easylist-downloads.adblockplus.org/easylist.txt";

    private volatile Map<String, GenericRule> cookieDomainMap;

    /**
     * Instantiate an ad blocker app with the provided appSettings and appProperties
     * @param appSettings the application settings
     * @param appProperties the applicaiton properties
     */
    public AdBlockerApp( AppSettings appSettings, AppProperties appProperties )
    {
        super(appSettings, appProperties);

        replacementGenerator = new AdBlockerReplacementGenerator(getAppSettings());

        this.addMetric(new AppMetric(STAT_SCAN, I18nUtil.marktr("Pages scanned")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Ads blocked")));
        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Pages passed")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("ad-blocker-http", this, null, new AdBlockerHandler( this ), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 5, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Initialize new settings and save the new settings
     */
    @Override
    public void initializeSettings()
    {
        AdBlockerSettings settings = new AdBlockerSettings();

        logger.info("Loading Filters...");
        RulesLoader.loadRules(settings);
        RulesLoader.loadCookieListGhostery(settings);
        logger.info(settings._getBlockingRules().size() + " url blocking rules loaded");
        logger.info(settings._getPassingRules().size() + " url passing rules loaded");

        this.setSettings(settings);
    }

    /**
     * Get the current settings
     * @return the settings
     */
    public AdBlockerSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the settings
     * @param newSettings The new settings
     */
    public void setSettings(AdBlockerSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "ad-blocker/" + "settings_" + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        this.reconfigure();
    }

    /**
     * Get the current blocking URL matcher for the current settings
     * @return the current blocking UrlMatcher
     */
    public UrlMatcher getBlockingUrlMatcher()
    {
        return blockingUrlMatcher;
    }

    /**
     * Set the current passing URL matcher for the current settings
     * @return the current passing UrlMatcher
     */
    public UrlMatcher getPassingUrlMatcher()
    {
        return passingUrlMatcher;
    }

    /**
     * This downloads a new list from the cloud and loads it into the rules and saves the new settings
     * This is called from the administration interface
     */
    public void updateList()
    {
        int exitCode = 0;
        File rulesFile = new File(RulesLoader.RULE_FILE);
        File tmpFile = new File(RulesLoader.RULE_FILE_BACKUP);
        try {
            renameListFile(rulesFile, tmpFile);
            exitCode = UvmContextFactory.context().execManager().execResult("wget --no-check-certificate " + LIST_URL + " -O " + RulesLoader.RULE_FILE);
        } catch (Exception e) {
            renameListFile(tmpFile, rulesFile);
            throw new RuntimeException(e);
        }
        if (exitCode != 0) {
            renameListFile(tmpFile, rulesFile);
            throw new RuntimeException("Could not update the rules list! (Exit code: " + exitCode + ")");
        }
        logger.info("Reloading Filters...");
        RulesLoader.loadRules(settings);
        logger.info(settings._getBlockingRules().size() + " url blocking rules loaded");
        logger.info(settings._getPassingRules().size() + " url passing rules loaded");

        this.setSettings(settings);
    }

    /**
     * Get the current cache
     * @return the cache
     */
    public Map<String, GenericRule> getCache()
    {
        return cache;
    }

    /**
     * Get the last update time as a string
     * @return the last update time
     */
    public String getListLastUpdate()
    {
        return settings.getLastUpdate();
    }

    /**
     * Generate a response for a block
     * @param redirectDetails Block details.
     * @param session The session
     * @param uri The URI being blocked
     * @param header The HeaderToken of the request being blocked
     * @return the token response array
     */
    protected Token[] generateResponse(BlockDetails redirectDetails, AppTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateSimpleResponse(redirectDetails, session, uri, header);
    }

    /**
     * Increment the scan count metric
     */
    protected void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    /**
     * Increment the block count metric
     */
    protected void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    /**
     * Increment the pass count metric
     */
    protected void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Get the pipeline connectors for this ad blocker
     * @return the pipelineconnectors array
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Checks if a cookie for the specified domain is blocked according to settings
     * @param domain The domain to lookup
     * @return true if blocked, false otherwise
     */
    protected boolean isCookieBlocked(String domain)
    {
        if (null == domain) {
            logger.warn("null domain for cookie");
            return false;
        }

        domain = domain.startsWith(".") && 1 < domain.length() ? domain.substring(1) : domain;

        if (null == cookieDomainMap || !settings.getScanCookies()) {
            return false;
        }

        for ( String d = domain ; d != null ; d = UrlMatchingUtil.nextHost(d) ) {
            GenericRule sr = cookieDomainMap.get(d);
            if (sr != null) {
                /* if rule is in cookieDomainMap it is enabled and blocked */
                return true;
            }
        }

        return false;
    }

    /**
     * The postInit() hook
     * This loads the settings from file or initializes them if necessary
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        AdBlockerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/ad-blocker/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(AdBlockerSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            AdBlockerSettings settings = new AdBlockerSettings();

            this.initializeSettings();
        } else {
            logger.info("Loading Settings...");

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    /**
     * This initalizes all the in-memory data structures for the current settings.
     * It should be called after changing and/or saving settings.
     */
    private void reconfigure()
    {
        if (settings == null) {
            logger.warn("NULL Settings");
            return;
        }

        List<GenericRule> cookieList = settings.getCookies();
        List<GenericRule> userCookieList = settings.getUserCookies();
        if (cookieList != null || userCookieList != null) {
            Map<String, GenericRule> newCookieMap = new ConcurrentHashMap<>();
            for (GenericRule sr : cookieList) {
                if ( sr.getEnabled() != null && sr.getEnabled() )
                    newCookieMap.put(sr.getString(), sr);
            }
            for (GenericRule sr : userCookieList) {
                if ( sr.getEnabled() != null && sr.getEnabled() )
                    newCookieMap.put(sr.getString(), sr);
            }
            this.cookieDomainMap = newCookieMap;
        } else {
            this.cookieDomainMap = null;
        }

        // reconfigure url matchers
        blockingUrlMatcher.clear();
        passingUrlMatcher.clear();
        cache.clear();
        for (GenericRule rule : settings._getBlockingRules()) {
            if ( rule.getEnabled() != null && rule.getEnabled() )
                blockingUrlMatcher.addRule(rule);
        }
        for (GenericRule rule : settings._getUserBlockingRules()) {
            if ( rule.getEnabled() != null && rule.getEnabled() )
                blockingUrlMatcher.addRule(rule);
        }
        for (GenericRule rule : settings._getPassingRules()) {
            if ( rule.getEnabled() != null && rule.getEnabled() )
                passingUrlMatcher.addRule(rule);
        }
        for (GenericRule rule : settings._getUserPassingRules()) {
            if ( rule.getEnabled() != null && rule.getEnabled() )
                passingUrlMatcher.addRule(rule);
        }
    }

    /**
     * Utility to rename a src file to dest
     * This will delete the existing destination if it exists
     * @param src The source file
     * @param dst The destination file
     */
    private void renameListFile(File src, File dst)
    {
        if (dst.exists()){
            dst.delete();
        }
        src.renameTo(dst);
    }
}
