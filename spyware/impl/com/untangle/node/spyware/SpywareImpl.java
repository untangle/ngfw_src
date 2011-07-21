/*
 * $Id$
 */
package com.untangle.node.spyware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.IPMaskedAddressRule;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.node.util.SimpleExec;
import org.apache.catalina.Valve;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

public class SpywareImpl extends AbstractNode implements Spyware
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/spyware-convert-settings.py";

    private static final String COOKIE_LIST = "com/untangle/node/spyware/cookie.txt";
    private static final String SUBNET_LIST = "com/untangle/node/spyware/subnet.txt";
    private static final String MALWARE_SITE_DB_FILE  = "/usr/share/untangle-webfilter-init/spyware-url";

    private static int deployCount = 0;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(this, factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final EventLogger<SpywareEvent> eventLogger;

    private static HashSet<String> urlDatabase = null;

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", this, tokenAdaptor,
                           Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", this, streamHandler,
                           Fitting.OCTET_STREAM, Affinity.SERVER, 0) };

    private final Map<InetAddress, Set<String>> hostWhitelists = new HashMap<InetAddress, Set<String>>();

    private final Logger logger = Logger.getLogger(getClass());

    private final PartialListUtil listUtil = new PartialListUtil();

    private final BlingBlinger scanBlinger;
    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;

    private volatile SpywareSettings settings;

    private volatile Map<String, GenericRule> cookieRules;
    private volatile Set<String> passedUrls;

    private final SpywareReplacementGenerator replacementGenerator;

    final SpywareStatisticManager statisticManager;

    // constructors ------------------------------------------------------------

    @SuppressWarnings("unchecked")
	public SpywareImpl()
    {
        replacementGenerator = new SpywareReplacementGenerator(getNodeId());

        NodeContext nodeContext = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(nodeContext);
        statisticManager = new SpywareStatisticManager(nodeContext);

        SimpleEventFilter ef = new SpywareAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareAccessFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareBlacklistFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareCookieFilter();
        eventLogger.addSimpleEventFilter(ef);

        MessageManager lmm = LocalUvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Pages scanned"), null, I18nUtil.marktr("SCAN"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Pages blocked"), null, I18nUtil.marktr("BLOCK"));
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Pages passed"), null, I18nUtil.marktr("PASS"));

        lmm.setActiveMetricsIfNotSet(getNodeId(), scanBlinger, blockBlinger, passBlinger);
    }

    // SpywareNode methods -----------------------------------------------------

    public SpywareSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final SpywareSettings settings)
    {
        _setSettings(settings);

        this.reconfigure();
    }

    public String getUnblockMode()
    {
        return settings.getUnblockMode();
    }

    public SpywareBlockDetails getBlockDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public boolean unblockSite(String nonce, boolean global)
    {
        SpywareBlockDetails bd = replacementGenerator.removeNonce(nonce);

        if (SpywareSettings.UNBLOCK_MODE_NONE.equals(getUnblockMode())) {
            logger.debug("attempting to unblock in NONE");
            return false;
        } else if (SpywareSettings.UNBLOCK_MODE_HOST.equals(getUnblockMode())) {
            if (global) {
                logger.debug("attempting to unblock global in HOST");
                return false;
            }
        } else if (SpywareSettings.UNBLOCK_MODE_GLOBAL.equals(getUnblockMode())) {
            // its all good
        } else {
            logger.error("missing case: " + getUnblockMode());
        }

        if (null == bd) {
            logger.debug("no BlockDetails for nonce");
            return false;
        } else if (global) {
            String site = bd.getWhitelistHost();
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
            String site = bd.getWhitelistHost();
            if (null == site) {
                logger.warn("cannot unblock null host");
                return false;
            } else {
                logger.warn("temporarily unblocking site: " + site);
                InetAddress addr = bd.getClientAddress();

                synchronized (this) {
                    Set<String> wl = hostWhitelists.get(addr);
                    if (null == wl) {
                        wl = new HashSet<String>();
                        hostWhitelists.put(addr, wl);
                    }
                    wl.add(site);
                }

                return true;
            }
        }
    }

    public EventManager<SpywareEvent> getEventManager()
    {
        return eventLogger;
    }

    // Node methods ------------------------------------------------------------

    // AbstractNode methods ----------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings(SpywareSettings settings)
    {
        Set<String> cookieList = initList(COOKIE_LIST);
        List<GenericRule> cookieRules = new LinkedList<GenericRule>();
        for ( String cookie : cookieList) {
            cookieRules.add(new GenericRule(cookie, cookie, null, null, true));
        }
        settings.setCookies(cookieRules);

        Set<String> subnetList = initList(SUBNET_LIST);
        List<GenericRule> subnetRules = new LinkedList<GenericRule>();
        for ( String subnet : subnetList) {
            subnetRules.add(new GenericRule(subnet, subnet, null, null, true));
        }
        
        return;
    }

    protected void postInit(String[] args)
    {
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        SpywareSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-spyware/" + "settings_" + nodeID;

        try {
            readSettings = settingsManager.load( SpywareSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                SimpleExec.SimpleExecResult result = null;
                logger.warn("Running: " + SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFileName + ".js");
                result = SimpleExec.exec( SETTINGS_CONVERSION_SCRIPT, new String[] { nodeID.toString() , settingsFileName + ".js"}, null, null, true, true, 1000*60, logger, true);
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( SpywareSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            SpywareSettings settings = new SpywareSettings();

            this.initializeSettings(settings);

            _setSettings(settings);

        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.info("Settings: " + this.settings.toJSONString());
        }

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void preStart()
    {
        if (urlDatabase == null) {
            initializeMalwareUrlList();
        }

        statisticManager.start();
    }

    @Override
    protected void postStop()
    {
        statisticManager.stop();
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
    }

    // package private methods -------------------------------------------------

    void incrementSubnetScan()
    {
        scanBlinger.increment();
    }

    void incrementSubnetBlock()
    {
        blockBlinger.increment();
    }

    void incrementHttpScan()
    {
        scanBlinger.increment();
    }

    void incrementHttpWhitelisted()
    {
        passBlinger.increment();
    }

    void incrementHttpBlockedDomain()
    {
        blockBlinger.increment();
    }

    void incrementHttpPassed()
    {
        passBlinger.increment();
    }

    void incrementHttpClientCookieScan()
    {
        scanBlinger.increment();
    }

    void incrementHttpClientCookieBlock()
    {
        blockBlinger.increment();
    }

    void incrementHttpClientCookiePass()
    {
        passBlinger.increment();
    }

    void incrementHttpServerCookieScan()
    {
        scanBlinger.increment();
    }

    void incrementHttpServerCookieBlock()
    {
        blockBlinger.increment();
    }

    void incrementHttpServerCookiePass()
    {
        passBlinger.increment();
    }

    Token[] generateResponse(SpywareBlockDetails bd, TCPSession sess, String uri, Header header, boolean persistent)
    {
        String n = replacementGenerator.generateNonce(bd);
        return replacementGenerator.generateResponse(n, sess, uri, header, persistent);
    }

    String generateNonce(String host, String uri, InetAddress addr)
    {
        SpywareBlockDetails bd = new SpywareBlockDetails(host, uri, addr);

        return replacementGenerator.generateNonce(bd);
    }

    boolean isUrlBlocked(String domain, URI uri)
    {
        if (!settings.getScanUrls()) {
            return false;
        }

        domain = null == domain ? null : domain.toLowerCase();

        if (urlDatabase.contains(domain + uri))
            return true;

        /**
         * Also check to see if the entire domain (or subdomain) is blocked
         */
        for ( String dom = domain ; dom != null ; dom = nextHost(dom) ) {
            logger.error("Searching for: \"" + dom + "/" + "\""); 
            if (urlDatabase.contains(dom + "/"))
                return true;
        }

        return false;
    }

    boolean isDomainPasslisted(String domain, InetAddress clientAddr)
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            if (findMatch(passedUrls, domain)) {
                return true;
            } else {
                Set<String> l = hostWhitelists.get(clientAddr);
                if (null == l) {
                    return false;
                } else {
                    return findMatch(l, domain);
                }
            }
        }
    }

    boolean isCookieBlocked(String domain)
    {
        if (null == domain) {
            logger.warn("null domain for cookie");
            return false;
        }

        domain = domain.startsWith(".") && 1 < domain.length()
            ? domain.substring(1) : domain;

        if (null == cookieRules || !settings.getScanCookies()) {
            return false;
        }

        boolean match = false;

        for (String d = domain; !match && null != d; d = nextHost(d)) {
            GenericRule sr = cookieRules.get(d);
            match = null != sr && sr.getEnabled();
        }

        return match;
    }

    void log(SpywareEvent se)
    {
        eventLogger.log(se);
    }

    // private methods ---------------------------------------------------------

    private boolean findMatch(Set<String> rules, String domain)
    {
        for (String d = domain; null != d; d = nextHost(d)) {
            if (rules.contains(d)) {
                return true;
            }
        }

        return false;
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    // settings intialization --------------------------------------------------

    private Set<String> initList(String file)
    {
        Set<String> s = new HashSet<String>();

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            for (String l = br.readLine(); null != l; l = br.readLine()) {
                s.add(l);
            }
        } catch (IOException exn) {
            logger.error("could not read list: " + file, exn);
        }

        return s;
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        LocalUvmContext uvmContext = LocalUvmContextFactory.context();
        LocalAppServerManager asm = uvmContext.localAppServerManager();

        Valve v = new OutsideValve()
            {
                protected boolean isInsecureAccessAllowed()
                {
                    return true;
                }

                /* Unified way to determine which parameter to check */
                protected boolean isOutsideAccessAllowed()
                {
                    return false;
                }
            };

        if (null != asm.loadInsecureApp("/spyware", "spyware", v)) {
            logger.debug("Deployed Spyware WebApp");
        } else {
            logger.error("Unable to deploy Spyware WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger)
    {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext uvmContext = LocalUvmContextFactory.context();
        LocalAppServerManager asm = uvmContext.localAppServerManager();

        if (asm.unloadWebApp("/spyware")) {
            logger.debug("Unloaded Spyware WebApp");
        } else {
            logger.warn("Unable to unload Spyware WebApp");
        }
    }

    public void reconfigure()
    {
        logger.info("Reconfigure.");
        if (this.settings.getScanSubnets()) {
            streamHandler.subnetList(this.settings.getSubnets());
        }

        List<GenericRule> cookieList = settings.getCookies();
        if (cookieList != null) {
            Map<String, GenericRule> s = new HashMap<String, GenericRule>();
            for (GenericRule sr : cookieList) {
                s.put(sr.getString(), sr);
            }
            cookieRules = s;
        } else {
            cookieRules = null;
        }

        Set<String> urlList = new HashSet<String>();
        List<GenericRule> passedUrls = settings.getPassedUrls();
        for (GenericRule urlRule : passedUrls) {
            if (urlRule.getEnabled()) {
                String str = normalizeDomain(urlRule.getString());

                urlList.add(str);
            }
        }
        this.passedUrls = urlList;
    }

    private String normalizeDomain(String dom)
    {
        String url = dom.toLowerCase();
        String uri = url.startsWith("http://")
            ? url.substring("http://".length()) : url;

        while (0 < uri.length()
               && ('*' == uri.charAt(0) || '.' == uri.charAt(0))) {
            uri = uri.substring(1);
        }

        if (uri.startsWith("www.")) {
            uri = uri.substring("www.".length());
        }

        int i = uri.indexOf('/');
        if (0 <= i) {
            uri = uri.substring(0, i);
        }

        return uri;
    }

    private void initializeMalwareUrlList()
    {
        synchronized(this) {
            if (this.urlDatabase != null)
                return;
            else
                this.urlDatabase = new HashSet<String>();
        }
        
        try {
            BufferedReader in = new BufferedReader(new FileReader(MALWARE_SITE_DB_FILE));
            String url;
            while ((url = in.readLine()) != null) {
                urlDatabase.add(url); //ignore return value (false if already present)
            }
            in.close();
        }
        catch (IOException e) {
            logger.error("Error loading category from file: " + MALWARE_SITE_DB_FILE, e);
        }
        
    }

    // private methods --------------------------------------------------------

    /**
     * Set the current settings to new Settings
     * And save the settings to disk
     */
    private void _setSettings( SpywareSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = LocalUvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        try {
            settingsManager.save(SpywareSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-spyware/" + "settings_"  + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.info("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }
    
}
