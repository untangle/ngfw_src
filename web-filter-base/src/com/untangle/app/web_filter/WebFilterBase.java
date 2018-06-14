/**
 * $Id$
 */

package com.untangle.app.web_filter;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;

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
    private static int web_filter_deployCount = 0;

    protected static final Logger logger = Logger.getLogger(WebFilterBase.class);
    private final int policyId = getAppSettings().getPolicyId().intValue();

    protected final PipelineConnector connector;
    protected final PipelineConnector[] connectors;

    protected final WebFilterBaseReplacementGenerator replacementGenerator;

    protected volatile WebFilterSettings settings;

    protected final UnblockedSitesMonitor unblockedSitesMonitor;

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
        if (readSettings.getVersion() < 2) {

            List<WebFilterRule> rlist = new LinkedList<WebFilterRule>();
            List<WebFilterRuleCondition> clist = null;
            WebFilterRule rule = null;
            int ruleNumber = 1;

            // convert V1 mime type rules to V2 web filter rules
            if (readSettings.V1_getBlockedMimeTypes() != null) {

                for (GenericRule org : readSettings.V1_getBlockedMimeTypes()) {
                    // we don't care about rules that aren't doing anything
                    if ((org.getBlocked() == false) && (org.getFlagged() == false)) continue;

                    rule = new WebFilterRule();
                    clist = new LinkedList<WebFilterRuleCondition>();
                    rule.setDescription(org.getDescription());
                    rule.setEnabled(true);
                    rule.setBlocked(org.getBlocked() == null ? false : org.getBlocked());
                    rule.setFlagged(org.getFlagged() == null ? false : org.getFlagged());
                    rule.setRuleId(ruleNumber++);
                    clist.add(new WebFilterRuleCondition(RuleCondition.ConditionType.WEB_FILTER_RESPONSE_CONTENT_TYPE, org.getString()));
                    rule.setConditions(clist);
                    rlist.add(rule);
                    logger.debug("Converted mime type rule: " + rule.toString());
                }
            }

            // convert V1 file extension rules to V2 web filter rules
            if (readSettings.V1_getBlockedExtensions() != null) {
                if (rlist == null) rlist = new LinkedList<WebFilterRule>();

                for (GenericRule org : readSettings.V1_getBlockedExtensions()) {
                    // we don't care about rules that aren't doing anything
                    if ((org.getBlocked() == false) && (org.getFlagged() == false)) continue;

                    rule = new WebFilterRule();
                    clist = new LinkedList<WebFilterRuleCondition>();
                    rule.setDescription(org.getDescription());
                    rule.setEnabled(true);
                    rule.setBlocked(org.getBlocked() == null ? false : org.getBlocked());
                    rule.setFlagged(org.getFlagged() == null ? false : org.getFlagged());
                    rule.setRuleId(ruleNumber++);
                    clist.add(new WebFilterRuleCondition(RuleCondition.ConditionType.WEB_FILTER_RESPONSE_FILE_EXTENSION, org.getString()));
                    rule.setConditions(clist);
                    rlist.add(rule);
                    logger.debug("Converted file ext rule: " + rule.toString());
                }
            }
            readSettings.setFilterRules(rlist);
            readSettings.setVersion(2);
            _setSettings(readSettings);
            logger.debug("Converted settings: " + this.settings.toJSONString());
            return;
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
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }
    }

    /**
     * This is a utility function to reassure that all the current categories
     * are in the settings Returns true if the category was added, false
     * otherwise
     * 
     * @param categories
     *        The categories
     * @param string
     *        The string
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
    private boolean addCategory(List<GenericRule> categories, String string, String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        if (categories == null) {
            logger.warn("Invalid arguments: categories is null");
            return false;
        }
        if (string == null) {
            logger.warn("Invalid arguments: string is null");
            return false;
        }
        if (name == null) {
            logger.warn("Invalid arguments: name is null");
            return false;
        }

        for (GenericRule rule : categories) {
            if (rule.getString().equals(string)) return false;
        }

        boolean newBlocked = blocked;
        boolean newFlagged = flagged;

        logger.info("Adding Category: ( " + string + ", " + name + ", " + newBlocked + ", " + newFlagged + " )");
        categories.add(new GenericRule(string, name, category, description, enabled, newBlocked, newFlagged));
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

        added |= addCategory(categories, "200", "Abortion", null, "Web pages that discuss abortion from a historical, medical, legal, or other not overtly biased point of view.", true, false, false);
        added |= addCategory(categories, "201", "Abortion - Pro Choice", null, "Web pages that push the pro-choice viewpoint or otherwise overtly encourage abortions.", true, false, false);
        added |= addCategory(categories, "202", "Abortion - Pro Life", null, "Web pages that condemn abortion or otherwise overtly push a pro-life agenda.", true, false, false);
        added |= addCategory(categories, "243", "Advocacy Groups & Trade Associations", null, "Web pages dedicated to industry trade groups, lobbyists, unions, special interest groups, professional organizations and other associations comprised of members with common goals.", true, false, false);
        added |= addCategory(categories, "203", "Agriculture", null, "Web pages devoted to the science, art, and business of cultivating soil, producing crops, raising livestock, and products, services, tips, tricks, etc. related to farming.", true, false, false);
        added |= addCategory(categories, "16", "Alcohol", null, "Web pages that promote, advocate or sell alcohol including beer, wine and hard liquor.", true, false, false);
        added |= addCategory(categories, "12", "Anonymizer", null, "Web pages that promote proxies and anonymizers for surfing websites with the intent of circumventing filters.", true, true, true);
        added |= addCategory(categories, "204", "Architecture & Construction", null, "Web pages which involve construction, contractors, structural design, architecture and all businesses or services related to the design, building or engineering of structures and environments.", true, false, false);
        added |= addCategory(categories, "205", "Arts", null, "Web pages related to the development or display of the visual arts.", true, false, false);
        added |= addCategory(categories, "206", "Astrology & Horoscopes", null, "Web pages related to astrology, horoscopes, divination according to the stars, or the zodiac.", true, false, false);
        added |= addCategory(categories, "207", "Atheism & Agnosticism", null, "Web pages that pursue an anti-religion agenda or that challenge religious, spiritual, metaphysical, or supernatural beliefs.", true, false, false);
        added |= addCategory(categories, "208", "Auctions & Marketplaces", null, "Web pages devoted to person to person selling or trading of goods and services through classifieds, online auctions, or other means not including \"traditional\" online business-to-consumer models.", true, false, false);
        added |= addCategory(categories, "209", "Banking", null, "Web pages operated by or all about banks and credit unions, particularly online banking web applications, but excludes online brokerages.", true, false, false);
        added |= addCategory(categories, "210", "Biotechnology", null, "Web pages which include genetics research, biotechnology firms and research institutions.", true, false, false);
        added |= addCategory(categories, "211", "Botnet", null, "Web pages or compromised web servers running software that is used by hackers to send spam, phishing attacks and denial of service attacks.", true, true, true);
        added |= addCategory(categories, "212", "Businesses & Services (General)", null, "Web pages that include Businesses and Services, generally used unless there is a more specific category that better describes the actual business or service.", true, false, false);
        added |= addCategory(categories, "213", "Cartoons, Anime & Comic Books", null, "Web pages dedicated to animated TV shows and movies or to comic books and graphic novels.", true, false, false);
        added |= addCategory(categories, "214", "Catalogs", null, "Web pages that have product listings and catalogs but do not have an online shopping option.", true, false, false);
        added |= addCategory(categories, "216", "Chat", null, "Web pages with real-time chat rooms and messaging allowing strangers and friends to chat in groups both in public and private chats.", true, false, false);
        added |= addCategory(categories, "217", "Child Abuse Images", null, "Web pages that show the physical or sexual abuse / exploitation of children.", true, true, true);
        added |= addCategory(categories, "297", "Child Inappropriate", null, "Includes tasteless content and material such as web pages that show cruelty (e.g. to animals), bathroom humor, tasteless material or other material potentially inappropriate for children.", true, false, false);
        added |= addCategory(categories, "218", "Command and Control Centers", null, "Internet servers used to send commands to infected machines called \"bots.\"", true, true, true);
        added |= addCategory(categories, "306", "Community Forums", null, "Web pages dedicated to forums, newsgroups, email archives, bulletin boards, and other community-driven content.", true, false, false);
        added |= addCategory(categories, "1", "Compromised", null, "Web pages that have been compromised by someone other than the site owner, which appear to be legitimate, but house malicious code.", true, true, true);
        added |= addCategory(categories, "219", "Content Servers", null, "Web servers without any navigable web pages typically used to host images and other media files with the purpose of improving web page performance and site scalability.", true, false, false);
        added |= addCategory(categories, "220", "Contests & Surveys", null, "Web pages devoted to online sweepstakes, contests, giveaways and raffles typically designed to obtain consumer information and demographics, but also used as part of various marketing efforts.", true, false, false);
        added |= addCategory(categories, "222", "Coupons", null, "Web pages dedicated to listing promotional codes, coupons, etc., either for printing for retail use or codes for online shopping.", true, false, false);
        added |= addCategory(categories, "223", "Criminal Skills", null, "Web pages providing information on how to perpetrate illegal activities such as burglary, murder, bomb-making, lock picking, etc.", true, false, false);
        added |= addCategory(categories, "23", "Dating", null, "Web pages that promote relationships such as dating and marriage.", true, false, false);
        added |= addCategory(categories, "225", "Educational Institutions", null, "Web pages for schools with an online presence including Universities, private and public schools and other real-world places of learning.", true, false, false);
        added |= addCategory(categories, "226", "Educational Materials & Studies", null, "Web pages with academic publications, journals, published research findings, curriculum, online learning courses and materials or study guides.", true, false, false);
        added |= addCategory(categories, "227", "Entertainment News & Celebrity Sites", null, "Web pages including news and gossip about celebrities, television shows, movies and show business in general.", true, false, false);
        added |= addCategory(categories, "228", "Entertainment Venues & Events", null, "Web pages devoted to venues used for entertainment including comedy clubs, night clubs, discos, festivals, theaters, playhouses, etc.", true, false, false);
        added |= addCategory(categories, "229", "Fashion & Beauty", null, "Web pages devoted to fashion and beauty information and tips. Includes web pages that market products or services related to fashion including clothing, jewelry, cosmetics and perfume.", true, false, false);
        added |= addCategory(categories, "230", "File Repositories", null, "Web pages including collections of shareware, freeware, open source, and other software downloads.", true, false, false);
        added |= addCategory(categories, "233", "Finance (General)", null, "Includes web pages that discuss economics, investing strategies, money management, retirement planning and tax planning.", true, false, false);
        added |= addCategory(categories, "231", "Fitness & Recreation", null, "Web pages with tips and information on fitness or recreational activities.", true, false, false);
        added |= addCategory(categories, "310", "Food & Restaurants", null, "Web pages related to food from restaurants and dining, to cooking and recipes.", true, false, false);
        added |= addCategory(categories, "20", "Gambling", null, "Web pages which promote gambling, lotteries, casinos and betting agencies involving chance.", true, false, false);
        added |= addCategory(categories, "21", "Games", null, "Web pages consisting of computer games, game producers and online gaming.", true, false, false);
        added |= addCategory(categories, "232", "Gay, Lesbian or Bisexual", null, "Web pages that cater to or discuss the gay, lesbian, bisexual or transgender lifestyle.", true, false, false);
        added |= addCategory(categories, "235", "Government Sponsored", null, "Web pages devoted to Government organizations, departments, or agencies. Includes police, fire (when employed by a city), elections commissions, elected representatives, government sponsored programs and research.", true, false, false);
        added |= addCategory(categories, "237", "Hacking", null, "Web pages with information or tools specifically intended to assist in online crime such as the unauthorized access to computers, but also pages with tools and information that enables fraud and other online crime.", true, false, false);
        added |= addCategory(categories, "3", "Hate Speech", null, "Web pages that promote extreme right/left wing groups, sexism, racism, religious hate and other discrimination.", true, false, false);
        added |= addCategory(categories, "238", "Health & Medical", null, "Web pages dedicated to personal health, medical services, medical equipment, procedures, mental health, finding and researching doctors, hospitals and clinics.", true, false, false);
        added |= addCategory(categories, "239", "Hobbies & Leisure", null, "Web pages which include tips and information about crafts, and hobbies such as sewing, stamp collecting, model airplane building, etc.", true, false, false);
        added |= addCategory(categories, "241", "Home & Office Furnishings", null, "Web pages that include furniture makers, retail furniture outlets, desks, couches, chairs, cabinets, etc.", true, false, false);
        added |= addCategory(categories, "240", "Home, Garden & Family", null, "Web pages which cover activities in the home and pertaining to the family. Includes tips and information about parenting, interior decorating , gardening, cleaning, family and entertaining.", true, false, false);
        added |= addCategory(categories, "37", "Humor", null, "Web pages which include comics, jokes and other humorous content.", true, false, false);
        added |= addCategory(categories, "4", "Illegal Drugs", null, "Web pages that promote the use or information of common illegal drugs and the misuse of prescription drugs and compounds.", true, false, false);
        added |= addCategory(categories, "305", "Image Search", null, "Web pages and internet search engines used to search pictures and photos found across the Internet where the returned results include thumbnails of the found images.", true, false, false);
        added |= addCategory(categories, "244", "Information Security", null, "Web pages and companies that provide computer and network security services, hardware, software or information.", true, false, false);
        added |= addCategory(categories, "245", "Instant Messenger", null, "Instant messaging software and web pages that typically involve staying in touch with a list of \"buddies\" via messaging services.", true, false, false);
        added |= addCategory(categories, "246", "Insurance", null, "Web pages the cover any type of insurance, insurance company, or government insurance program from Medicare to car insurance to life insurance.", true, false, false);
        added |= addCategory(categories, "247", "Internet Phone & VOIP", null, "Web pages that allow users to make calls via the web or to download software that allows users to make calls over the Internet.", true, false, false);
        added |= addCategory(categories, "51", "Job Search", null, "Web pages devoted to job searches or agencies, career planning and human resources.", true, false, false);
        added |= addCategory(categories, "248", "Kid's Pages", null, "Web pages specifically intended for young children (under 10) including entertainment, games, and recreational pages built with young children in mind.", true, false, false);
        added |= addCategory(categories, "311", "Legislation, Politics & Law", null, "Web pages covering legislation, the legislative process, politics, political parties, elections, elected officials and opinions on these topics.", true, false, false);
        added |= addCategory(categories, "249", "Lingerie, Suggestive & Pinup", null, "Web pages that refer specifically to photos and videos where the person who is the subject of the photo is wearing sexually provocative clothing such as lingerie.", true, false, false);
        added |= addCategory(categories, "250", "Literature & Books", null, "Web pages for published writings including fiction and non-fiction novels, poems and biographies.", true, false, false);
        added |= addCategory(categories, "251", "Login Screens", null, "Web pages which are used to login to a wide variety of services where the actual service is not known, but could be any of several categories (e.g. Yahoo and Google login pages).", true, false, false);
        added |= addCategory(categories, "252", "Malware Call-Home", null, "Web pages identified as spyware which report information back to a particular URL.", true, true, true);
        added |= addCategory(categories, "253", "Malware Distribution Point", null, "Web pages that host viruses, exploits, and other malware.", true, true, true);
        added |= addCategory(categories, "254", "Manufacturing", null, "Web pages devoted to businesses involved in manufacturing and industrial production.", true, false, false);
        added |= addCategory(categories, "255", "Marijuana", null, "Web pages about the plant or about smoking the marijuana plant. Includes web pages on legalizing marijuana and using marijuana for medicinal purposes, marijuana facts and info pages.", true, false, false);
        added |= addCategory(categories, "308", "Marketing Services", null, "Web pages dedicated to advertising agencies and other marketing services that don't include online banner ads.", true, false, false);
        added |= addCategory(categories, "30", "Military", null, "Web pages sponsored by the armed forces and government controlled agencies.", true, false, false);
        added |= addCategory(categories, "54", "Miscellaneous", null, "Web pages that do not clearly fall into any other category.", true, false, false);
        added |= addCategory(categories, "256", "Mobile Phones", null, "Web pages which contain content for Mobile phone manufacturers and mobile phone companies' websites. Also includes sites that sell mobile phones and accessories.", true, false, false);
        added |= addCategory(categories, "309", "Motorized Vehicles", null, "Web pages which contain information about motorized vehicles including selling, promotion, or discussion. Includes motorized vehicle manufacturers and sites dedicated to the buying and selling of those vehicles.", true, false, false);
        added |= addCategory(categories, "38", "Music", null, "Web pages that include internet radio and streaming media, musicians, bands, MP3 and media downloads.", true, false, false);
        added |= addCategory(categories, "236", "Nature & Conservation", null, "Web pages with information on environmental issues, sustainable living, ecology, nature and the environment.", true, false, false);
        added |= addCategory(categories, "39", "News", null, "Web pages with general news information such as newspapers and magazines.", true, false, false);
        added |= addCategory(categories, "257", "No Content Found", null, "Web pages which contain no discernable content which can be used for classification purposes.", true, false, false);
        added |= addCategory(categories, "258", "Non-traditional Religion & Occult", null, "Web pages for religions outside of the mainstream or not in the top ten religions practiced in the world. Also includes occult and supernatural, extraterrestrial, folk religions, mysticism, cults and sects.", true, false, false);
        added |= addCategory(categories, "7", "Nudity", null, "Web pages that display full or partial nudity with no sexual references or intent.", true, false, false);
        added |= addCategory(categories, "259", "Nutrition & Diet", null, "Web pages on losing weight and eating healthy, diet plans, weight loss programs and food allergies.", true, false, false);
        added |= addCategory(categories, "49", "Online Ads", null, "Companies, web pages, and sites responsible for hosting online advertisements including advertising graphics, banners, and pop-up content. Also includes web pages that host source code for dynamically generated ads and pop-ups.", true, false, false);
        added |= addCategory(categories, "260", "Online Financial Tools & Quotes", null, "Web pages for investment quotes, online portfolio tracking, financial calculation tools such as mortgage calculators, online tax preparation software, online bill payment and online budget tracking software.", true, false, false);
        added |= addCategory(categories, "261", "Online Information Management", null, "Web pages devoted to online personal information managers such as web applications that manage to-do lists, calendars, address books, etc.", true, false, false);
        added |= addCategory(categories, "262", "Online Shopping", null, "Websites and web pages that provide a means to purchase online.", true, false, false);
        added |= addCategory(categories, "263", "Online Stock Trading", null, "Investment brokerage web pages that allow online trading of stocks, mutual funds and other securities.", true, false, false);
        added |= addCategory(categories, "99", "Parked", null, "Web pages that have been purchased to reserve the name but do not have any real content.", true, false, false);
        added |= addCategory(categories, "264", "Parks, Rec Facilities & Gyms", null, "Web pages which include parks and other areas designated for recreational activities such as swimming, skateboarding, rock climbing, as well as for non-professional sports such as community athletic fields.", true, false, false);
        added |= addCategory(categories, "265", "Pay To Surf", null, "Web sites that offer cash to users who install their software which displays ads and tracks browsing habits effectively allowing users to be paid while surfing the web.", true, false, false);
        added |= addCategory(categories, "266", "Peer-to-Peer", null, "Web pages that provide peer-to-peer (P2P) file sharing software.", true, false, false);
        added |= addCategory(categories, "312", "Personal Pages & Blogs", null, "Web pages including blogs, or a format for individuals to share news, opinions, and information about themselves. Also includes personal web pages about an individual or that individual's family.", true, false, false);
        added |= addCategory(categories, "267", "Personal Storage", null, "Web sites used for remote storage of files, sharing of large files, and remote Internet backups.", true, false, false);
        added |= addCategory(categories, "268", "Pets & Animals", null, "Web pages with information or products and services for pets and other animals including birds, fish, and insects.", true, false, false);
        added |= addCategory(categories, "18", "Pharmacy", null, "Web pages which include prescribed medications and information about approved drugs and their medical use.", true, false, false);
        added |= addCategory(categories, "269", "Philanthropic Organizations", null, "Web pages with information regarding charities and other non-profit philanthropic organizations and foundations dedicated to altruistic activities.", true, false, false);
        added |= addCategory(categories, "5", "Phishing/Fraud", null, "Manipulated web pages and emails used for fraudulent purposes, also known as phishing.", true, true, true);
        added |= addCategory(categories, "270", "Photo Sharing", null, "Web pages that host digital photographs or allow users to upload, search, and exchange photos and images online.", true, false, false);
        added |= addCategory(categories, "273", "Physical Security", null, "Web pages devoted to businesses and services related to security products or other security aspects excluding computer security.", true, false, false);
        added |= addCategory(categories, "271", "Piracy & Copyright Theft", null, "Web pages that provide access to illegally obtained files such as pirated software (aka warez), pirated movies, pirated music, etc.", true, false, false);
        added |= addCategory(categories, "272", "Pornography", null, "Web pages which contain images or videos depicting sexual acts, sexual arousal, or explicit nude imagery intended to be sexual in nature.", true, true, true);
        added |= addCategory(categories, "47", "Portal Sites", null, "General web pages with customized personal portals, including white/yellow pages.", true, false, false);
        added |= addCategory(categories, "274", "Private IP Address", null, "Web pages for Private IP addresses are those reserved for use internally in corporations or homes.", true, false, false);
        added |= addCategory(categories, "275", "Product Reviews & Price Comparisons", null, "Web pages dedicated to helping consumers comparison shop or choose products or stores, but don't offer online purchasing options.", true, false, false);
        added |= addCategory(categories, "276", "Profanity", null, "Web pages that use either frequent profanity or serious profanity.", true, false, false);
        added |= addCategory(categories, "277", "Professional Networking", null, "Social networking web pages intended for professionals and business relationship building.", true, false, false);
        added |= addCategory(categories, "278", "R-Rated", null, "Web pages whose primary purpose and majority of content is child appropriate, but who have regular or irregular sections of the site with sexually themed, non-educational material.", true, false, false);
        added |= addCategory(categories, "52", "Real Estate", null, "Web pages possessing information about renting, purchasing, selling or financing real estate including homes, apartments, office space, etc.", true, false, false);
        added |= addCategory(categories, "279", "Redirect", null, "Web pages that redirect to other pages on other web sites.", true, false, false);
        added |= addCategory(categories, "280", "Reference Materials & Maps", null, "Web pages which contain reference materials and are specific to data compilations and reference shelf material such as atlases, dictionaries, encyclopedias, census and other reference data.", true, false, false);
        added |= addCategory(categories, "281", "Religions", null, "Web pages which cover main-stream popular religions world-wide as well as general religion topics and theology.", true, false, false);
        added |= addCategory(categories, "307", "Remote Access", null, "Web pages that provide remote access to private computers or networks, internal network file shares, and internal web applications.", true, false, false);
        added |= addCategory(categories, "282", "Retirement Homes & Assisted Living", null, "Web pages containing information on retirement homes and communities including nursing care and hospice care.", true, false, false);
        added |= addCategory(categories, "283", "School Cheating", null, "Web pages that contain test answers, pre-written term papers and essays, full math problem solvers that show the work and similar web sites that can be used to cheat on homework and tests.", true, false, false);
        added |= addCategory(categories, "48", "Search Engines", null, "Web pages supporting the searching of web, newsgroups, pictures, directories, and other online content.", true, false, false);
        added |= addCategory(categories, "284", "Self-help & Addiction", null, "Web pages which include sites with information and help on gambling, drug, and alcohol addiction as well as sites helping with eating disorders such as anorexia, bulimia, and over-eating.", true, false, false);
        added |= addCategory(categories, "285", "Sex & Erotic", null, "Web pages with sexual content or products or services related to sex, but without nudity or other explicit pictures on the page.", true, true, true);
        added |= addCategory(categories, "286", "Sex Education & Pregnancy", null, "Web pages with educational materials and clinical explanations of sex, safe sex, birth control, pregnancy, and similar topics aimed at teens and children.", true, false, false);
        added |= addCategory(categories, "287", "Shipping & Logistics", null, "Web pages that promote management of inventory including transportation, warehousing, distribution, storage, order fulfillment and shipping.", true, false, false);
        added |= addCategory(categories, "288", "Social and Affiliation Organizations", null, "Web pages built around communities of people where users \"connect\" to other users.", true, false, false);
        added |= addCategory(categories, "45", "Social Networking", null, "Social networking web pages and online communities built around communities of people where users \"connect\" to other users.", true, false, false);
        added |= addCategory(categories, "289", "Software, Hardware & Electronics", null, "Web pages with information about or makers of computer equipment, computer software, hardware, peripherals, data networks, computer services and electronics.", true, false, false);
        added |= addCategory(categories, "53", "Spam", null, "Products and web pages promoted through spam techniques.", true, false, false);
        added |= addCategory(categories, "313", "Sport Fighting", null, "Web pages dedicated to training and contests involving fighting disciplines and multi-person combat sports such as martial arts, boxing, wrestling, and fencing.", true, false, false);
        added |= addCategory(categories, "290", "Sport Hunting", null, "Web pages covering recreational hunting of live animals.", true, false, false);
        added |= addCategory(categories, "291", "Sports", null, "Web pages covering competitive sports in which multiple people or teams compete in both athletic (e.g. football) and non-athletic competitions (e.g. billiards).", true, false, false);
        added |= addCategory(categories, "292", "Spyware & Questionable Software", null, "Web pages containing software that reports information back to a central server such as spyware or keystroke loggers.", true, true, true);
        added |= addCategory(categories, "293", "Streaming & Downloadable Audio", null, "Web pages with repositories of music or that provide streaming music or other audio files that may pose a bandwidth risk to companies.", true, false, false);
        added |= addCategory(categories, "294", "Streaming & Downloadable Video", null, "Web pages with repositories of videos or that provide in-browser streaming videos that may pose a bandwidth risk to companies.", true, false, false);
        added |= addCategory(categories, "295", "Supplements & Compounds", null, "Web pages containing information on vitamins and other over-the-counter unregulated supplements and compounds.", true, false, false);
        added |= addCategory(categories, "296", "Swimsuits", null, "Web pages containing pictures of people wearing swimsuits. Does not include pictures of swimsuits on manikins or by themselves.", true, false, false);
        added |= addCategory(categories, "234", "Technology (General)", null, "Web pages which include web design, internet standards (such as RFCs), protocol specifications, and other broad technology discussions or news.", true, false, false);
        added |= addCategory(categories, "298", "Television & Movies", null, "Web pages about television shows and movies including reviews, show times, plot summaries, discussions, teasers, marketing sites, etc.", true, false, false);
        added |= addCategory(categories, "316", "Text Messaging & SMS", null, "Web pages used to send or receive simple message service (SMS) text messages between a web page and a mobile phone.", true, false, false);
        added |= addCategory(categories, "19", "Tobacco", null, "Web pages promoting the use of tobacco related products (cigarettes, cigars, pipes).", true, false, false);
        added |= addCategory(categories, "299", "Torrent Repository", null, "Web pages that host repositories of torrent files, which are the instruction file for allowing a bit torrent client to download large files from peers.", true, false, false);
        added |= addCategory(categories, "300", "Toys", null, "Web pages dedicated to manufacturers of toys, including toy selling or marketing sites.", true, false, false);
        added |= addCategory(categories, "15", "Translator", null, "Web pages which translate languages from one to another.", true, false, false);
        added |= addCategory(categories, "28", "Travel", null, "Web pages which provide travel and tourism information, online booking or travel services such as airlines, car rentals, and hotels.", true, false, false);
        added |= addCategory(categories, "301", "Unreachable", null, "Web pages that give an error such as, \"Network Timeout\", \"The server at example.com is taking too long to respond,\" or \"Address Not Found\".", true, false, false);
        added |= addCategory(categories, "10", "Violence", null, "Web pages that promote questionable activities such as violence and militancy.", true, false, false);
        added |= addCategory(categories, "11", "Weapons", null, "Web pages that include guns and weapons when not used in a violent manner.", true, false, false);
        added |= addCategory(categories, "302", "Web Hosting, ISP & Telco", null, "Web pages for web hosting and blog hosting sites, Internet Service Providers (ISPs) and telecommunications (phone) companies.", true, false, false);
        added |= addCategory(categories, "46", "Web-based Email", null, "Web pages which enable users to send and/or receive email through a web accessible email account.", true, false, false);
        added |= addCategory(categories, "303", "Web-based Greeting Cards", null, "Web pages that allow users to send or receive online greeting cards.", true, false, false);
        added |= addCategory(categories, "304", "Wikis", null, "Web pages or websites in which a community maintains a set of informational documents where anyone in the community can update the content.", true, false, false);

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
