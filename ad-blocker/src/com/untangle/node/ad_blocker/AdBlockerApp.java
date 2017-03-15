/*
 * $Id$
 */
package com.untangle.node.ad_blocker;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.AppMetric;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.AppProperties;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.node.AppBase;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Token;
import com.untangle.node.http.BlockDetails;
import com.untangle.node.http.HeaderToken;

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
    protected Map<String, GenericRule> cache = new ConcurrentHashMap<String, GenericRule>();

    public static final String LIST_URL = "https://easylist-downloads.adblockplus.org/easylist.txt";

    private volatile Map<String, GenericRule> cookieDomainMap;

    // constructor ------------------------------------------------------------
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

    @Override
    public void initializeSettings()
    {
        AdBlockerSettings settings = new AdBlockerSettings();

        logger.info("Loading Filters...");
        RulesLoader.loadRules(settings);
        RulesLoader.loadCookieListGhostery(settings);
        logger.info(settings._getBlockingRules().size() + " url blocking rules loaded");
        logger.info(settings._getPassingRules().size() + " url passing rules loaded");

        this._setSettings(settings);
    }

    // AdBlockerApp methods --------------------------------------------------

    public AdBlockerSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(AdBlockerSettings settings)
    {
        this._setSettings(settings);
    }

    public List<GenericRule> getPassedClients()
    {
        return settings.getPassedClients();
    }

    public void setPassedClients(List<GenericRule> passedClients)
    {
        this.settings.setPassedClients(passedClients);

        _setSettings(this.settings);
    }

    public List<GenericRule> getPassedUrls()
    {
        return settings.getPassedUrls();
    }

    public void setPassedUrls(List<GenericRule> passedUrls)
    {
        this.settings.setPassedUrls(passedUrls);

        _setSettings(this.settings);
    }

    public List<GenericRule> getCookies()
    {
        return settings.getCookies();
    }

    public void setCookies(List<GenericRule> newCookies)
    {
        this.settings.setCookies(newCookies);
        _setSettings(this.settings);
    }

    public UrlMatcher getBlockingUrlMatcher()
    {
        return blockingUrlMatcher;
    }

    public UrlMatcher getPassingUrlMatcher()
    {
        return passingUrlMatcher;
    }

    public Map<String, GenericRule> getCache()
    {
        return cache;
    }

    // package protected methods ----------------------------------------------

    String generateNonce(BlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    Token[] generateResponse(String nonce, AppTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateSimpleResponse(nonce, session, uri, header);
    }

    void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        AdBlockerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/ad-blocker/" + "settings_" + nodeID + ".js";

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

    private void reconfigure()
    {
        if (settings == null) {
            logger.warn("NULL Settings");
            return;
        }

        List<GenericRule> cookieList = settings.getCookies();
        List<GenericRule> userCookieList = settings.getUserCookies();
        if (cookieList != null || userCookieList != null) {
            Map<String, GenericRule> newCookieMap = new ConcurrentHashMap<String, GenericRule>();
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

    private void _setSettings(AdBlockerSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "ad-blocker/" + "settings_" + nodeID + ".js", newSettings );
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

    boolean isCookieBlocked(String domain)
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

        this._setSettings(settings);

    }

    private void renameListFile(File src, File dest)
    {
        if (dest.exists()){
            dest.delete();
        }
        src.renameTo(dest);
    }
    
    public String getListLastUpdate()
    {
        return settings.getLastUpdate();
    }
}
