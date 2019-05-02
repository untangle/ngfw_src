/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;

import java.util.HashMap;
import java.util.Map;


import org.apache.log4j.Logger;

import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.PasswordUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.app.http.HeaderToken;

/**
 * The base implementation of the Web Filter. The web filter lite and web filter
 * implementation inherit this
 */
public abstract class WebFilterBase extends AppBase implements WebFilter
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_FLAG = "flag";
    private static final String STAT_PASS = "pass";
    private static final String STAT_CACHE_COUNT = "cache_count";
    private static int web_filter_deployCount = 0;

    protected static final Logger logger = Logger.getLogger(WebFilterBase.class);
    private final int policyId = getAppSettings().getPolicyId().intValue();

    protected final PipelineConnector connector;
    protected final PipelineConnector[] connectors;

    protected final WebFilterBaseReplacementGenerator replacementGenerator;

    protected volatile WebFilterSettings settings;

    protected final UnblockedSitesMonitor unblockedSitesMonitor;

    /*
     * Mapping from old category engine ids to new.  Can be removed after 14.2 releease cycle.
     */
    private static final Map<Integer, Integer> categoryConversionMap;
    static{
        categoryConversionMap = new HashMap<>();

        categoryConversionMap.put(200, 68); // Abortion
        categoryConversionMap.put(201, 68); // Abortion - Pro Choice
        categoryConversionMap.put(202, 68); // Abortion - Pro Life
        categoryConversionMap.put(243, 35); // Advocacy Groups & Trade Associations
        categoryConversionMap.put(203, 4);  // Agriculture
        categoryConversionMap.put(16, 76);  // Alcohol
        categoryConversionMap.put(12, 58);  // Anonymizer
        categoryConversionMap.put(204, 4); // Architecture & Construction
        categoryConversionMap.put(205, 21); // Arts
        categoryConversionMap.put(206, 8); // Astrology & Horoscopes
        categoryConversionMap.put(207, 20); // Atheism & Agnosticism
        categoryConversionMap.put(208, 6); // Auctions & Marketplaces
        categoryConversionMap.put(209, 3); // Banking
        categoryConversionMap.put(210, 69); // Biotechnology
        categoryConversionMap.put(211, 67); // Botnet
        categoryConversionMap.put(212, 4); // Businesses & Services (General)
        categoryConversionMap.put(213, 21); // Cartoons, Anime & Comic Books
        categoryConversionMap.put(214, 7); // Catalogs
        categoryConversionMap.put(216, 66); // Chat
        categoryConversionMap.put(217, 48); // Child Abuse Images
        categoryConversionMap.put(297, 48); // Child Inappropriate
        categoryConversionMap.put(218, 56); // Command and Control Centers
        categoryConversionMap.put(306, 80); // Community Forums
        categoryConversionMap.put(1, 56); // Compromised
        categoryConversionMap.put(219, 65); // Content Servers
        categoryConversionMap.put(220, 27); // Contests & Surveys
        categoryConversionMap.put(222, 7); // Coupons
        categoryConversionMap.put(223, 64); // Criminal Skills
        categoryConversionMap.put(23, 18); // Dating
        categoryConversionMap.put(225, 40); // Educational Institutions
        categoryConversionMap.put(226, 17); // Educational Materials & Studies
        categoryConversionMap.put(227, 21); // Entertainment News & Celebrity Sites
        categoryConversionMap.put(228, 21); // Entertainment Venues & Events
        categoryConversionMap.put(229, 79); // Fashion & Beauty
        categoryConversionMap.put(230, 47); // File Repositories
        categoryConversionMap.put(233, 3); // Finance (General)
        categoryConversionMap.put(231, 80); // Fitness & Recreation
        categoryConversionMap.put(310, 21); // Food & Restaurants
        categoryConversionMap.put(20, 27); // Gambling
        categoryConversionMap.put(21, 34); // Games
        categoryConversionMap.put(232, 35); // Gay, Lesbian or Bisexual
        categoryConversionMap.put(235, 61); // Government Sponsored
        categoryConversionMap.put(237, 33); // Hacking
        categoryConversionMap.put(3, 46); // Hate Speech
        categoryConversionMap.put(238, 69); // Health & Medical
        categoryConversionMap.put(239, 80); // Hobbies & Leisure
        categoryConversionMap.put(241, 12); // Home & Office Furnishings
        categoryConversionMap.put(240, 12); // Home, Garden & Family
        categoryConversionMap.put(37, 21); // Humor
        categoryConversionMap.put(4, 10); // Illegal Drugs
        categoryConversionMap.put(305, 78); // Image Search
        categoryConversionMap.put(244, 2); // Information Security
        categoryConversionMap.put(245, 66); // Instant Messenger
        categoryConversionMap.put(246, 3); // Insurance
        categoryConversionMap.put(247, 66); // Internet Phone & VOIP
        categoryConversionMap.put(51, 26); // Job Search
        categoryConversionMap.put(248, 45); // Kid's Pages
        categoryConversionMap.put(311, 23); // Legislation, Politics & Law
        categoryConversionMap.put(249, 43); // Lingerie, Suggestive & Pinup
        categoryConversionMap.put(250, 21); // Literature & Books
        categoryConversionMap.put(251, 49); // Login Screens
        categoryConversionMap.put(252, 56); // Malware Call-Home
        categoryConversionMap.put(253, 56); // Malware Distribution Point
        categoryConversionMap.put(254, 4); // Manufacturing
        categoryConversionMap.put(255, 32); // Marijuana
        categoryConversionMap.put(308, 4); // Marketing Services
        categoryConversionMap.put(30, 13); // Military
        categoryConversionMap.put(54, 0); // Miscellaneous
        categoryConversionMap.put(256, 5); // Mobile Phones
        categoryConversionMap.put(309, 81); // Motorized Vehicles
        categoryConversionMap.put(38, 60); // Music
        categoryConversionMap.put(236, 35); // Nature & Conservation
        categoryConversionMap.put(39, 63); // News
        categoryConversionMap.put(257, 15); // No Content Found
        categoryConversionMap.put(258, 8); // Non-traditional Religion & Occult
        categoryConversionMap.put(7, 62); // Nudity
        categoryConversionMap.put(259, 69); // Nutrition & Diet
        categoryConversionMap.put(49, 52); // Online Ads
        categoryConversionMap.put(260, 3); // Online Financial Tools & Quotes
        categoryConversionMap.put(261, 5); // Online Information Management
        categoryConversionMap.put(262, 7); // Online Shopping
        categoryConversionMap.put(263, 16); // Online Stock Trading
        categoryConversionMap.put(99, 75); // Parked
        categoryConversionMap.put(264, 80); // Parks, Rec Facilities & Gyms
        categoryConversionMap.put(265, 37); // Pay To Surf
        categoryConversionMap.put(266, 31); // Peer-to-Peer
        categoryConversionMap.put(312, 22); // Personal Pages & Blogs
        categoryConversionMap.put(267, 47); // Personal Storage
        categoryConversionMap.put(268, 80); // Pets & Animals
        categoryConversionMap.put(18, 69); // Pharmacy
        categoryConversionMap.put(269, 39); // Philanthropic Organizations
        categoryConversionMap.put(5, 57); // Phishing/Fraud
        categoryConversionMap.put(270, 78); // Photo Sharing
        categoryConversionMap.put(273, 4); // Physical Security
        categoryConversionMap.put(271, 33); // Piracy & Copyright Theft
        categoryConversionMap.put(272, 11); // Pornography
        categoryConversionMap.put(47, 51); // Portal Sites
        categoryConversionMap.put(274, 0); // Private IP Address
        categoryConversionMap.put(275, 7); // Product Reviews & Price Comparisons
        categoryConversionMap.put(276, 44); // Profanity
        categoryConversionMap.put(277, 14); // Professional Networking
        categoryConversionMap.put(278, 11); // R-Rated
        categoryConversionMap.put(52, 1); // Real Estate
        categoryConversionMap.put(279, 49); // Redirect
        categoryConversionMap.put(280, 29); // Reference Materials & Maps
        categoryConversionMap.put(281, 20); // Religions
        categoryConversionMap.put(307, 66); // Remote Access
        categoryConversionMap.put(282, 69); // Retirement Homes & Assisted Living
        categoryConversionMap.put(283, 53); // School Cheating
        categoryConversionMap.put(48, 50); // Search Engines
        categoryConversionMap.put(284, 69); // Self-help & Addiction
        categoryConversionMap.put(285, 11); // Sex & Erotic
        categoryConversionMap.put(286, 19); // Sex Education & Pregnancy
        categoryConversionMap.put(287, 4); // Shipping & Logistics
        categoryConversionMap.put(288, 14); // Social and Affiliation Organizations
        categoryConversionMap.put(45, 14); // Social Networking
        categoryConversionMap.put(289, 2); // Software, Hardware & Electronics
        categoryConversionMap.put(53, 71); // Spam
        categoryConversionMap.put(313, 42); // Sport Fighting
        categoryConversionMap.put(290, 38); // Sport Hunting
        categoryConversionMap.put(291, 42); // Sports
        categoryConversionMap.put(292, 59); // Spyware & Questionable Software
        categoryConversionMap.put(293, 25); // Streaming & Downloadable Audio
        categoryConversionMap.put(294, 25); // Streaming & Downloadable Video
        categoryConversionMap.put(295, 69); // Supplements & Compounds
        categoryConversionMap.put(296, 43); // Swimsuits
        categoryConversionMap.put(234, 5); // Technology (General)
        categoryConversionMap.put(298, 21); // Television & Movies
        categoryConversionMap.put(316, 66); // Text Messaging & SMS
        categoryConversionMap.put(19, 76); // Tobacco
        categoryConversionMap.put(299, 31); // Torrent Repository
        categoryConversionMap.put(300, 21); // Toys
        categoryConversionMap.put(15, 28); // Translator
        categoryConversionMap.put(28, 9); // Travel
        categoryConversionMap.put(301, 15); // Unreachable
        categoryConversionMap.put(10, 48); // Violence
        categoryConversionMap.put(11, 36); // Weapons
        categoryConversionMap.put(302, 82); // Web Hosting, ISP & Telco
        categoryConversionMap.put(46, 55); // Web-based Email
        categoryConversionMap.put(303, 41); // Web-based Greeting Cards
        categoryConversionMap.put(304, 29); // Wikis
    }

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WebFilterBase(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.replacementGenerator = buildReplacementGenerator();

        this.addMetric(new AppMetric(STAT_SCAN, I18nUtil.marktr("Pages scanned")));

        if (getAppName().equals("web-filter")) {
            this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Pages blocked")));
        }

        this.addMetric(new AppMetric(STAT_FLAG, I18nUtil.marktr("Pages flagged")));
        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Pages passed")));
        this.addMetric(new AppMetric(STAT_CACHE_COUNT, I18nUtil.marktr("Cache count")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("web-filter", this, null, new WebFilterBaseHandler(this), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, 3, isPremium());
        this.connectors = new PipelineConnector[] { connector };

        String appName = this.getName();

        this.unblockedSitesMonitor = new UnblockedSitesMonitor(this);
    }

    /**
     * Unblock a site
     * 
     * @param nonce
     *        The nonce
     * @param global
     *        Global flag
     * @param password
     *        The password
     * @return Result
     */
    public boolean unblockSite(String nonce, boolean global, String password)
    {
        if (!this.verifyPassword(password)) {
            if (logger.isInfoEnabled()) {
                logger.info("Unable to verify the password for nonce: '" + nonce + "'");
            }
            return false;
        } else {
            if (logger.isInfoEnabled()) {
                logger.info("Verified the password for nonce: '" + nonce + "'");
            }
            return unblockSite(nonce, global);
        }
    }

    /**
     * Verify password
     * 
     * @param password
     *        The password
     * @return True for verify success, otherwise false
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
     * Get the unblock mode
     * 
     * @return The unblock mode
     */
    public String getUnblockMode()
    {
        return settings.getUnblockMode();
    }

    /**
     * Check if HTTPS SNI is enabled
     * 
     * @return True if enabled, otherwise false
     */
    public boolean isHttpsEnabledSni()
    {
        return settings.getEnableHttpsSni();
    }

    /**
     * Check if HTTPS SNI cert fallback is enabled
     * 
     * @return True if enabled, otherwise false
     */
    public boolean isHttpsEnabledSniCertFallback()
    {
        return settings.getEnableHttpsSniCertFallback();
    }

    /**
     * Check if HTTP SNI IP fallback is enabled
     * 
     * @return True if enabled, otherwise false
     */
    public boolean isHttpsEnabledSniIpFallback()
    {
        return settings.getEnableHttpsSniIpFallback();
    }

    /**
     * Get the block details for the argumented nonce
     * 
     * @param nonce
     *        The nonce to search
     * @return Block details
     */
    public WebFilterBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    /**
     * Unblock a site
     * 
     * @param nonce
     *        The nonce
     * @param global
     *        Global flag
     * @return Result of the operation
     */
    public boolean unblockSite(String nonce, boolean global)
    {
        WebFilterBlockDetails bd = replacementGenerator.removeNonce(nonce);

        if (WebFilterSettings.UNBLOCK_MODE_NONE.equals(settings.getUnblockMode())) {
            logger.debug("attempting to unblock in WebFilterSettings.UNBLOCK_MODE_NONE");
            return false;
        } else if (WebFilterSettings.UNBLOCK_MODE_HOST.equals(settings.getUnblockMode())) {
            if (global) {
                logger.debug("attempting to unblock global in WebFilterSettings.UNBLOCK_MODE_HOST");
                return false;
            }
        } else if (WebFilterSettings.UNBLOCK_MODE_GLOBAL.equals(settings.getUnblockMode())) {
            // its all good
        } else {
            logger.error("missing case: " + settings.getUnblockMode());
        }

        if (null == bd) {
            logger.debug("no BlockDetails for nonce");
            return false;
        } else if (global) {
            String site = bd.getUnblockHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.warn("permanently unblocking site: " + site);
                GenericRule sr = new GenericRule(site, site, "user unblocked", "unblocked by user", true);
                settings.getPassedUrls().add(sr);
                _setSettings(settings);

                return true;
            }
        } else {
            String site = bd.getUnblockHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.info("Temporarily unblocking site: " + site);
                InetAddress addr = bd.getClientAddress();

                unblockedSitesMonitor.addUnblockedSite(addr, site);
                getDecisionEngine().addUnblockedSite(addr, site);

                return true;
            }
        }
    }

    /**
     * Flush all unblocked sites
     */
    public void flushAllUnblockedSites()
    {
        logger.warn("Flushing all Unblocked sites...");
        getDecisionEngine().removeAllUnblockedSites();
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public WebFilterSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the application settings
     * 
     * @param settings
     *        The new settings
     */
    public void setSettings(WebFilterSettings settings)
    {
        _setSettings(settings);
    }

    /**
     * Get the categories
     * 
     * @return The categories
     */
    public List<GenericRule> getCategories()
    {
        return settings.getCategories();
    }

    /**
     * Set the categories
     * 
     * @param newCategories
     *        The new list
     */
    public void setCategories(List<GenericRule> newCategories)
    {
        this.settings.setCategories(newCategories);
        _setSettings(this.settings);
    }

    /**
     * Get the blocked URL's
     * 
     * @return The blocked URL's
     */
    public List<GenericRule> getBlockedUrls()
    {
        return settings.getBlockedUrls();
    }

    /**
     * Set the blocked URL's
     * 
     * @param blockedUrls
     *        The new list
     */
    public void setBlockedUrls(List<GenericRule> blockedUrls)
    {
        this.settings.setBlockedUrls(blockedUrls);
        _setSettings(this.settings);
    }

    /**
     * Get the passed clients
     * 
     * @return The passed clients
     */
    public List<GenericRule> getPassedClients()
    {
        return settings.getPassedClients();
    }

    /**
     * Set the passed clients
     * 
     * @param passedClients
     *        The new list
     */
    public void setPassedClients(List<GenericRule> passedClients)
    {
        this.settings.setPassedClients(passedClients);
        _setSettings(this.settings);
    }

    /**
     * Get the passed URL's
     * 
     * @return The passed URL's
     */
    public List<GenericRule> getPassedUrls()
    {
        return settings.getPassedUrls();
    }

    /**
     * Set the passed URL's
     * 
     * @param passedUrls
     *        The new list
     */
    public void setPassedUrls(List<GenericRule> passedUrls)
    {
        this.settings.setPassedUrls(passedUrls);
        _setSettings(this.settings);
    }

    /**
     * Get the filter rules
     * 
     * @return The filter rules
     */
    public List<WebFilterRule> getFilterRules()
    {
        return settings.getFilterRules();
    }

    /**
     * Set the filter rules
     * 
     * @param filterRules
     *        The new list
     */
    public void setFilterRules(List<WebFilterRule> filterRules)
    {
        this.settings.setFilterRules(filterRules);
        _setSettings(this.settings);
    }

    /**
     * Get the decision engine
     * 
     * @return The decision engine
     */
    public abstract DecisionEngine getDecisionEngine();

    /**
     * Get the app title
     * 
     * @return The app title
     */
    public abstract String getAppTitle();

    /**
     * Get the name
     * 
     * @return The name
     */
    public abstract String getName();

    /**
     * Get the app name
     * 
     * @return The app name
     */
    public abstract String getAppName();

    /**
     * Get the premium flag
     * 
     * @return The premium flag
     */
    public abstract boolean isPremium();

    /**
     * Generate a response
     * 
     * @param nonce
     *        The nonce
     * @param session
     *        The session
     * @param uri
     *        The URI
     * @param header
     *        The header
     * @return The response token
     */
    public Token[] generateResponse(String nonce, AppTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateResponse(nonce, session, uri, header);
    }

    /**
     * Initialize application settings
     * 
     * @param settings
     *        The settings
     */
    public abstract void initializeSettings(WebFilterSettings settings);

    /**
     * Fix settings
     * 
     * @param settings
     *        The settings
     */
    public void fixupSetSettings(WebFilterSettings settings)
    {
    }

    /**
     * Initialize common settings
     * 
     * @param settings
     *        The settings
     */
    public void initializeCommonSettings(WebFilterSettings settings)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(getAppSettings() + " init settings");
        }
    }

    /**
     * Increment the scan counter
     */
    public void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    /**
     * Increment the block counter
     */
    public void incrementBlockCount()
    {
        if (getAppName().equals("web-filter")) {
            this.incrementMetric(STAT_BLOCK);
        }
    }

    /**
     * Increment the flagged counter
     */
    public void incrementFlagCount()
    {
        this.incrementMetric(STAT_FLAG);
    }

    /**
     * Increment the passed counter
     */
    public void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Set the cache size.
     * @param count Long of current number of entries.
     */
    public void setCacheCount(long count){
        this.setMetric(STAT_CACHE_COUNT, count);
    }

    /**
    /**
     * Build a replacement generator
     * 
     * @return The replacement generator
     */
    protected WebFilterBaseReplacementGenerator buildReplacementGenerator()
    {
        return new WebFilterBaseReplacementGenerator(getAppSettings());
    }

    /**
     * Get the pipline connectors
     * 
     * @return The pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called after application initialization.
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        WebFilterSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/" + this.getAppName() + "/" + "settings_" + appID + ".js";

        /**
         * First we try to load the existing settings
         */
        try {
            readSettings = settingsManager.load(WebFilterSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are no settings initialize with defaults and return
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            WebFilterSettings settings = new WebFilterSettings();

            this.initializeCommonSettings(settings);
            this.initializeSettings(settings);

            _setSettings(settings);
            logger.debug("Default Settings: " + this.settings.toJSONString());
            return;
        }

        /**
         * If we found older settings do conversion, save, and return
         */
        if (readSettings.getVersion() < 3) {
            List<GenericRule> oldCategories = readSettings.getCategories();
            initializeSettings(readSettings);
            GenericRule newCat = null;
            for (GenericRule oldCat : oldCategories){
                try {
                    newCat = readSettings.getCategory(categoryConversionMap.get(Integer.parseInt(oldCat.getString())));
                    if(newCat != null){
                        newCat.setEnabled(oldCat.getEnabled());
                        newCat.setBlocked(oldCat.getBlocked());
                        newCat.setFlagged(oldCat.getFlagged());
                    }
                } catch (Exception e){
                    logger.warn("Unable to convert", e);
                }
            }

            readSettings.setVersion(3);
            _setSettings(readSettings);
            logger.debug("Converted settings: " + this.settings.toJSONString());
        }

        // existing settings loaded and no conversion was needed
        this.settings = readSettings;
        logger.debug("Loaded Settings: " + this.settings.toJSONString());
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
        getDecisionEngine().removeAllUnblockedSites();
        unblockedSitesMonitor.start();
    }

    /**
     * Called after the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        super.postStart(isPermanentTransition);

        e_deployWebAppIfRequired(logger);
    }

    /**
     * Called before the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStop(boolean isPermanentTransition)
    {
        super.postStop(isPermanentTransition);

        e_unDeployWebAppIfRequired(logger);
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
        unblockedSitesMonitor.stop();
        getDecisionEngine().removeAllUnblockedSites();
    }

    /**
     * Generate a nonce
     * 
     * @param details
     *        The block details
     * @return The nonce
     */
    protected String generateNonce(WebFilterBlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    /**
     * Generate a response
     * 
     * @param nonce
     *        The nonce
     * @param session
     *        The session
     * @return The response
     */
    protected Token[] generateResponse(String nonce, AppTCPSession session)
    {
        return replacementGenerator.generateResponse(nonce, session);
    }

    /**
     * Set the current settings to new Settings And save the settings to disk
     * 
     * @param newSettings
     *        The settings
     */
    protected void _setSettings(WebFilterSettings newSettings)
    {
        /**
         * Prepare settings for saving This makes sure certain things are always
         * true, such as flagged == true if blocked == true
         */
        if (newSettings.getCategories() != null) {
            for (GenericRule rule : newSettings.getCategories()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }
        if (newSettings.getBlockedUrls() != null) {
            for (GenericRule rule : newSettings.getBlockedUrls()) {
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }
        if (newSettings.getFilterRules() != null) {
            int idx = 0;
            for (WebFilterRule rule : newSettings.getFilterRules()) {
                rule.setRuleId(++idx);
                if (rule.getBlocked()) rule.setFlagged(Boolean.TRUE);
            }
        }

        fixupSetSettings(newSettings);

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/" + "" + this.getAppName() + "/" + "settings_" + appID + ".js", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString());
        } catch (Exception e) {
        }

        getDecisionEngine().reconfigure(this.settings);

    }

    /**
     * This is a utility function to reassure that all the current categories
     * are in the settings Returns true if the category was added, false
     * otherwise
     * 
     * @param categories
     *        The categories
     * @param id
     *        Category id from provider
     * @param name
     *        The name
     * @param category
     *        The category
     * @param description
     *        The description
     * @param enabled
     *        The enabled flag
     * @param blocked
     *        The blocked flag
     * @param flagged
     *        The flagged flag
     * @return The category
     */
    private boolean addCategory(List<GenericRule> categories, Integer id, String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        if (categories == null) {
            logger.warn("Invalid arguments: categories is null");
            return false;
        }
        if (name == null) {
            logger.warn("Invalid arguments: name is null");
            return false;
        }

        for (GenericRule rule : categories) {
            if (rule.getId().equals(id)) return false;
        }

        boolean newBlocked = blocked;
        boolean newFlagged = flagged;

        logger.info("Adding Category: ( " + id + ", " + name + ", " + newBlocked + ", " + newFlagged + " )");
        categories.add(new GenericRule(id, name, category, description, enabled, newBlocked, newFlagged));
        return true;
    }

    /**
     * Adds the new v3 categories to the list returns true if any were added
     * 
     * @param categories
     *        The categories to add
     * @return True if any were added, otherwise false
     */
    protected boolean addCategories(List<GenericRule> categories)
    {
        boolean added = false;

        added |= addCategory(categories, 0, "Uncategorized", "Misc", "Sites that have not been categornized", true, false, false);
        added |= addCategory(categories, 1, "Real Estate", "Productivity", "Information on renting, buying, or selling real estate or properties.  Tips on buying or selling a home.  Real estate agents, rental or relocation services, and property improvement.", true, false, false);
        added |= addCategory(categories, 2, "Computer and Internet Security", "Productivity", "Computer/Internet security, security discussion groups.", true, false, false);
        added |= addCategory(categories, 3, "Financial Services", "Privacy", "Banking services and other types of financial information, such as loans, accountancy, actuaries, banks, mortgages, and general insurance companies. Does not include sites that offer market information, brokerage or trading services.", true, false, false);
        added |= addCategory(categories, 4, "Business and Economy", "Productivity", "Business firms, corporate websites , business information, economics, marketing, management, and entrepreneurship.", true, false, false);
        added |= addCategory(categories, 5, "Computer and Internet Info", "Productivity", "General computer and Internet sites, technical information. SaaS sites and other URLs that deliver internet services.", true, false, false);
        added |= addCategory(categories, 6, "Auctions", "Productivity", "Sites that support the offering and purchasing of goods between individuals as their main purpose. Does not include classified advertisements.", true, false, false);
        added |= addCategory(categories, 7, "Shopping", "Productivity", "Department stores, retail stores, company catalogs and other sites that allow online consumer or business shopping and the purchase of goods and services.", true, false, false);
        added |= addCategory(categories, 8, "Cult and Occult", "Sensitive", "Methods, means of instruction, or other resources to interpret, affect or influence real events through the use of astrology, spells, curses, magic powers, satanic or supernatural beings. Includes horoscope sites.", true, false, false);
        added |= addCategory(categories, 9, "Travel", "Productivity", "Airlines and flight booking agencies. Travel planning, reservations, vehicle rentals, descriptions of travel destinations, or promotions for hotels or casinos. Car rentals.", true, false, false);
        added |= addCategory(categories, 10, "Abused Drugs", "Sensitive", "Discussion or remedies for illegal, illicit, or abused drugs such as heroin, cocaine, or other street drugs. Information on 'legal highs' : glue sniffing, misuse of prescription drugs or abuse of other legal substances.", true, false, false);
        added |= addCategory(categories, 11, "Adult and Pornography", "Sensitive", "Sexually explicit material for the purpose of arousing a sexual or prurient interest. Adult products including sex toys, CD-ROMs, and videos. Online groups, including newsgroups and forums, that are sexually explicit in nature. Erotic stories and textual descriptions of sexual acts. Adult services including videoconferencing, escort services, and strip clubs. Sexually explicit art.", true, true, true);
        added |= addCategory(categories, 12, "Home and Garden", "Productivity", "Home issues and products, including maintenance, home safety, decor, cooking, gardening, home electronics, design, etc.", true, false, false);
        added |= addCategory(categories, 13, "Military", "Productivity", "Information on military branches, armed services, and military history.", true, false, false);
        added |= addCategory(categories, 14, "Social Networking", "Productivity", "These are social networking sites that have user communities where users interact, post messages, pictures, and otherwise communicate. These sites were formerly part of Personal Sites and Blogs but have been removed to this new category to provide differentiation and more granular policy.", true, false, false);
        added |= addCategory(categories, 15, "Dead Sites", "Misc", "These are dead sites that do not respond to http queries. Policy engines should usually treat these as 'Uncategorized' sites.", true, false, false);
        added |= addCategory(categories, 16, "Individual Stock Advice and Tools", "Productivity", "Promotion and facilitation of securities trading and management of investment assets. Also includes information on financial investment strategies, quotes, and news.", true, false, false);
        added |= addCategory(categories, 17, "Training and Tools", "Productivity", "Distance education and trade schools, online courses, vocational training, software training, skills training.", true, false, false);
        added |= addCategory(categories, 18, "Dating", "Sensitive", "Dating websites focused on establishing personal relationships.", true, false, false);
        added |= addCategory(categories, 19, "Sex Education", "Sensitive", "Information on reproduction, sexual development, safe sex practices, sexually transmitted diseases, sexuality, birth control, sexual development, tips for better sex as well as products used for sexual enhancement, and contraceptives.", true, false, false);
        added |= addCategory(categories, 20, "Religion", "Sensitive", "Conventional or unconventional religious or quasi-religious subjects, as well as churches, synagogues, or other houses of worship.", true, false, false);
        added |= addCategory(categories, 21, "Entertainment and Arts", "Productivity", "Motion pictures, videos, television, music and programming guides, books, comics, movie theatres, galleries, artists or reviews on entertainment. Performing arts (theatre, vaudeville, opera, symphonies, etc.). Museums, galleries, artist sites (sculpture, photography, etc.).", true, false, false);
        added |= addCategory(categories, 22, "Personal sites and Blogs", "Productivity", "Personal websites posted by individuals or groups, as well as blogs.", true, false, false);
        added |= addCategory(categories, 23, "Legal", "Privacy", "Legal websites, law firms, discussions and analysis of legal issues.", true, false, false);
        added |= addCategory(categories, 24, "Local Information", "Productivity", "City guides and tourist information, including restaurants, area/regional information, and local points of interest.", true, false, false);
        added |= addCategory(categories, 25, "Streaming Media", "IT Resources", "Sales, delivery, or streaming of audio or video content, including sites that provide downloads for such viewers.", true, false, false);
        added |= addCategory(categories, 26, "Job Search", "Productivity", "Assistance in finding employment, and tools for locating prospective employers, or employers looking for employees.", true, false, false);
        added |= addCategory(categories, 27, "Gambling", "Sensitive", "Gambling or lottery web sites that invite the use of real or virtual money. Information or advice for placing wagers, participating in lotteries, gambling, or running numbers. Virtual casinos and offshore gambling ventures. Sports picks and betting pools. Virtual sports and fantasy leagues that offer large rewards or request significant wagers. Hotel and Resort sites that do not enable gambling on the site are categorized in Travel or Local Information.", true, false, false);
        added |= addCategory(categories, 28, "Translation", "Sensitive", "URL and language translation sites that allow users to see URL pages in other languages. These sites can also allow users to circumvent filtering as the target page's content is presented within the context of the translator's URL. These sites were formerly part of Proxy Avoidance and Anonymizers, but have been removed to this new category to provide differentiation and more granular policy.", true, false, false);
        added |= addCategory(categories, 29, "Reference and Research", "Productivity", "Personal, professional, or educational reference material, including online dictionaries, maps, census, almanacs, library catalogues, genealogy, and scientific information.", true, false, false);
        added |= addCategory(categories, 30, "Shareware and Freeware", "IT Resources", "Software, screensavers, icons, wallpapers, utilities, ringtones. Includes downloads that request a donation, and open source projects.", true, false, false);
        added |= addCategory(categories, 31, "Peer to Peer", "IT Resources", "Peer to peer clients and access. Includes torrents, music download programs.", true, false, false);
        added |= addCategory(categories, 32, "Marijuana", "Sensitive", "Marijuana use, cultivation, history, culture, legal issues.", true, false, false);
        added |= addCategory(categories, 33, "Hacking", "Sensitive", "Illegal or questionable access to or the use of communications equipment/software. Development and distribution of programs that may allow compromise of networks and systems. Avoidance of licensing and fees for computer programs and other systems.", true, false, false);
        added |= addCategory(categories, 34, "Games", "Productivity", "Game playing or downloading, video games, computer games, electronic games, tips, and advice on games or how to obtain cheat codes. Also includes sites dedicated to selling board games as well as journals and magazines dedicated to game playing. Includes sites that support or host online sweepstakes and giveaways. Includes fantasy sports sites that also host games or game-playing.", true, false, false);
        added |= addCategory(categories, 35, "Philosophy and Political Advocacy", "Productivity", "Politics, philosophy, discussions, promotion of a particular viewpoint or stance in order to further a cause.", true, false, false);
        added |= addCategory(categories, 36, "Weapons", "Sensitive", "Sales, reviews, or descriptions of weapons such as guns, knives or martial arts devices, or provide information on their use, accessories, or other modifications.", true, false, false);
        added |= addCategory(categories, 37, "Pay to Surf", "Productivity", "Sites that pay users in the form of cash or prizes, for clicking on or reading specific links, email, or web pages.", true, false, false);
        added |= addCategory(categories, 38, "Hunting and Fishing", "Productivity", "Sport hunting, gun clubs, and fishing.", true, false, false);
        added |= addCategory(categories, 39, "Society", "Productivity", "A variety of topics, groups, and associations relevant to the general populace, broad issues that impact a variety of people, including safety, children, societies, and philanthropic groups.", true, false, false);
        added |= addCategory(categories, 40, "Educational Institutions", "Productivity", "Pre-school, elementary, secondary, high school, college, university, and vocational school and other educational content and information,including enrollment, tuition, and syllabus.", true, false, false);
        added |= addCategory(categories, 41, "Online Greeting Cards", "IT Resources", "Online Greeting card sites.", true, false, false);
        added |= addCategory(categories, 42, "Sports", "Productivity", "Team or conference web sites, international, national, college, professional scores and schedules; sports-related online magazines or newsletters, fantasy sports and virtual sports leagues.", true, false, false);
        added |= addCategory(categories, 43, "Swimsuits and Intimate Apparel", "Sensitive", "Swimsuits, intimate apparel or other types of suggestive clothing.", true, false, false);
        added |= addCategory(categories, 44, "Questionable", "Sensitive", "Tasteless humor, 'get rich quick' sites, and sites that manipulate the browser user experience or client in some unusual, unexpected, or suspicious manner.", true, true, true);
        added |= addCategory(categories, 45, "Kids", "Productivity", "Sites designed specifically for children and teenagers.", true, false, false);
        added |= addCategory(categories, 46, "Hate and Racism", "Sensitive", "Sites that contain content and language in support of hate crimes and racism such as Nazi, neo-Nazi, Ku Klux Klan, etc.", true, false, false);
        added |= addCategory(categories, 47, "Personal Storage", "IT Resources", "Online storage and posting of files, music, pictures, and other data.", true, false, false);
        added |= addCategory(categories, 48, "Violence", "Sensitive", "Sites that advocate violence, depictions, and methods, including game/comic violence and suicide.", true, false, false);
        added |= addCategory(categories, 49, "Keyloggers and Monitoring", "Security", "Downloads and discussion of software agents that track a user's keystrokes or monitor their web surfing habits.", true, true, true);
        added |= addCategory(categories, 50, "Search Engines", "Productivity", "Search interfaces using key words or phrases. Returned results may include text, websites, images, videos, and files.", true, false, false);
        added |= addCategory(categories, 51, "Internet Portals", "Productivity", "Web sites that aggregate a broader set of Internet content and topics, and which typically serve as the starting point for an end user.", true, false, false);
        added |= addCategory(categories, 52, "Web Advertisements", "IT Resources", "Advertisements, media, content, and banners.", true, false, false);
        added |= addCategory(categories, 53, "Cheating", "Sensitive", "Sites that support cheating and contain such materials, including free essays, exam copies, plagiarism, etc.", true, false, false);
        added |= addCategory(categories, 54, "Gross", "Sensitive", "Vomit and other bodily functions, bloody clothing, etc.", true, false, false);
        added |= addCategory(categories, 55, "Web-based Email", "Privacy", "Sites offering web based email and email clients.", true, false, false);
        added |= addCategory(categories, 56, "Malware Sites", "Security", "Malicious content including executables, drive-by infection sites, malicious scripts, viruses, trojans, and code.", true, true, true);
        added |= addCategory(categories, 57, "Phishing and Other Frauds", "Security", "Phishing, pharming, and other sites that pose as a reputable site, usually to harvest personal information from a user. These sites are typically quite short-lived, so examples don?t last long. Please contact us if you need fresh data.", true, true, true);
        added |= addCategory(categories, 58, "Proxy Avoidance and Anonymizers", "Security", " Proxy servers and other methods to gain access to URLs in any way that bypasses URL filtering or monitoring. Web-based translation sites that circumvent filtering.", true, true, true);
        added |= addCategory(categories, 59, "Spyware and Adware", "Security", "Spyware or Adware sites that provide or promote information gathering or tracking that is unknown to, or without the explicit consent of, the end user or the organization, also unsolicited advertising popups and programs that may be installed on a user's computer.", true, true, true);
        added |= addCategory(categories, 60, "Music", "Productivity", "Music sales, distribution, streaming, information on musical groups and performances, lyrics, and the music business.", true, false, false);
        added |= addCategory(categories, 61, "Government", "Privacy", "Information on government, government agencies and government services such as taxation, public, and emergency services. Also includes sites that discuss or explain laws of various governmental entities. Includes local, county, state, and national government sites.", true, false, false);
        added |= addCategory(categories, 62, "Nudity", "Sensitive", "Nude or seminude depictions of the human body. These depictions are not necessarily sexual in intent or effect, but may include sites containing nude paintings or photo galleries of artistic nature. This category also includes nudist or naturist sites that contain pictures of nude individuals.", true, false, false);
        added |= addCategory(categories, 63, "News and Media", "Productivity", "Current events or contemporary issues of the day. Also includes radio stations and magazines, newspapers online, headline news sites, newswire services, personalized news services, and weather sites", true, false, false);
        added |= addCategory(categories, 64, "Illegal", "Sensitive", "Criminal activity, how not to get caught, copyright and intellectual property violations, etc.", true, false, false);
        added |= addCategory(categories, 65, "Content Delivery Networks", "IT Resources", "Delivery of content and data for third parties, including ads, media, files, images, and video.", true, false, false);
        added |= addCategory(categories, 66, "Internet Communications", "IT Resources", "Internet telephony, messaging, VoIP services and related businesses.", true, false, false);
        added |= addCategory(categories, 67, "Bot Nets", "Security", "These are URLs, typically IP addresses, which are determined to be part of a Bot network, from which network attacks are launched. Attacks may include SPAM messages, DOS, SQL injections, proxy jacking, and other unsolicited contacts.", true, true, true);
        added |= addCategory(categories, 68, "Abortion", "Sensitive", "Abortion topics, either pro-life or pro-choice.", true, false, false);
        added |= addCategory(categories, 69, "Health and Medicine", "Privacy", "General health, fitness, well-being, including traditional and non-traditional methods and topics. Medical information on ailments, various conditions, dentistry, psychiatry, optometry, and other specialties. Hospitals and doctor offices. Medical insurance. Cosmetic surgery.", true, false, false);
        added |= addCategory(categories, 71, "SPAM URLs", "Security", "URLs contained in SPAM", true, false, false);
        added |= addCategory(categories, 74, "Dynamically Generated Content", "Productivity", "Dynamically Generated Content", true, false, false);
        added |= addCategory(categories, 75, "Parked Domains", "Sensitive", "Parked domains are URLs which host limited content or click-through ads which may generate revenue for the hosting entities but generally do not contain content useful to the end user. Also includes Under Construction, folders, and web server default home pages.", true, false, false);
        added |= addCategory(categories, 76, "Alcohol and Tobacco", "Sensitive", "Sites that provide information on, promote, or support the sale of alcoholic beverages or tobacco products and associated paraphernalia.", true, false, false);
        added |= addCategory(categories, 78, "Image and Video Search", "Sensitive", "Photo and image searches, online photo albums/digital photo exchange, image hosting.", true, false, false);
        added |= addCategory(categories, 79, "Fashion and Beauty", "Productivity", "Fashion or glamour magazines, beauty, clothes, cosmetics, style.", true, false, false);
        added |= addCategory(categories, 80, "Recreation and Hobbies", "Productivity", "Information, associations, forums and publications on recreational pastimes such as collecting, kit airplanes, outdoor activities such as hiking, camping, rock climbing, specific arts, craft, or techniques; animal and pet related information, including breed-specifics, training, shows and humane societies.", true, false, false);
        added |= addCategory(categories, 81, "Motor Vehicles", "Productivity", "Car reviews, vehicle purchasing or sales tips, parts catalogs. Auto trading, photos, discussion of vehicles including motorcycles, boats, cars, trucks and RVs. Journals and magazines on vehicle modifications.", true, false, false);
        added |= addCategory(categories, 82, "Web Hosting", "IT Resources", "Free or paid hosting services for web pages and information concerning their development, publication and promotion.", true, false, false);
        return added;
    }
    /**
     * Deploy the web app
     *
     * @param logger
     *        The logger
     */
    private static synchronized void e_deployWebAppIfRequired(Logger logger)
    {
        web_filter_deployCount = web_filter_deployCount + 1;
        if (web_filter_deployCount != 1) {
            return;
        }

        if (UvmContextFactory.context().tomcatManager().loadServlet("/web-filter", "web-filter") != null) {
            logger.debug("Deployed WebFilter WebApp");
        } else {
            logger.error("Unable to deploy WebFilter WebApp");
        }
    }

    /**
     * Undeploy the web app
     * 
     * @param logger
     *        The logger
     */
    private static synchronized void e_unDeployWebAppIfRequired(Logger logger)
    {
        web_filter_deployCount = web_filter_deployCount - 1;
        if (web_filter_deployCount != 0) {
            return;
        }

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/web-filter")) {
            logger.debug("Unloaded WebFilter WebApp");
        } else {
            logger.warn("Unable to unload WebFilter WebApp");
        }
    }
}
