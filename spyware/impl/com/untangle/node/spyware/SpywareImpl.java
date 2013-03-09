/**
 * $Id$
 */
package com.untangle.node.spyware;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;

public class SpywareImpl extends NodeBase implements Spyware
{
    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/spyware-get-last-update";

    private static final String COOKIE_LIST = "/usr/share/untangle-webfilter-init/spyware-cookie";
    private static final String SUBNET_LIST = "/usr/share/untangle-webfilter-init/spyware-subnet";

    private static final String STAT_SCAN = "scan";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_PASS = "pass";

    private final Logger logger = Logger.getLogger(getClass());

    private static int deployCount = 0;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(this, factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private EventLogQuery cookieQuery;
    private EventLogQuery blacklistQuery;
    private EventLogQuery suspiciousQuery;

    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spyware-http", this, tokenAdaptor, Fitting.HTTP_TOKENS, Affinity.SERVER, 3),
        new SoloPipeSpec("spyware-byte", this, streamHandler, Fitting.OCTET_STREAM, Affinity.SERVER, 3)
    };

    private final Map<InetAddress, Set<String>> unblockedSites = new HashMap<InetAddress, Set<String>>();

    private volatile SpywareSettings settings;

    private volatile Map<String, GenericRule> cookieDomainMap;
    private volatile Set<String> passedUrls;

    private final SpywareReplacementGenerator replacementGenerator;

    // constructors ------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public SpywareImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        replacementGenerator = new SpywareReplacementGenerator(getNodeSettings());

        this.suspiciousQuery = new EventLogQuery(I18nUtil.marktr("Suspicious Events"),
                                                 "SELECT * FROM reports.sessions " + 
                                                 "WHERE policy_id = :policyId " +
                                                 "AND sw_access_ident != '' " +
                                                 "ORDER BY time_stamp DESC");
        
        this.blacklistQuery = new EventLogQuery(I18nUtil.marktr("Blacklisted Events"),
                                                "SELECT * from reports.n_http_events " +
                                                " WHERE sw_blacklisted IS TRUE" + 
                                                " AND policy_id = :policyId" + 
                                                " ORDER BY time_stamp DESC");

        this.cookieQuery = new EventLogQuery(I18nUtil.marktr("Cookie Events"),
                                             "SELECT * from reports.n_http_events " +
                                             " WHERE sw_cookie_ident IS NOT NULL" + 
                                             " AND policy_id = :policyId" + 
                                             " ORDER BY time_stamp DESC");

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Pages scanned")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Pages blocked")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Pages passed")));
    }

    // SpywareNode methods -----------------------------------------------------

    public SpywareSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final SpywareSettings settings)
    {
        _setSettings(settings);
    }
    
    public List<GenericRule> getCookies()
    {
        return settings.getCookies();
    }

    public void setCookies( List<GenericRule> newCookies )
    {
        this.settings.setCookies(newCookies);
        _setSettings(this.settings);
    }
    
    public List<GenericRule> getSubnets()
    {
        return settings.getSubnets();
    }

    public void setSubnets( List<GenericRule> newSubnets )
    {
        this.settings.setSubnets(newSubnets);
        _setSettings(this.settings);
    }
    
    public List<GenericRule> getPassedUrls()
    {
        return settings.getPassedUrls();
    }

    public void setPassedUrls( List<GenericRule> newPassedUrls )
    {
        this.settings.setPassedUrls(newPassedUrls);
        _setSettings(this.settings);
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
                    Set<String> wl = unblockedSites.get(addr);
                    if (null == wl) {
                        wl = new HashSet<String>();
                        unblockedSites.put(addr, wl);
                    }
                    wl.add(site);
                }

                return true;
            }
        }
    }

    public EventLogQuery[] getCookieEventQueries()
    {
        return new EventLogQuery[] {  cookieQuery };
    }

    public EventLogQuery[] getUrlEventQueries()
    {
        return new EventLogQuery[] {  blacklistQuery };
    }

    public EventLogQuery[] getSuspiciousEventQueries()
    {
        return new EventLogQuery[] { suspiciousQuery };
    }

    public Date getLastSignatureUpdate()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput( GET_LAST_SIGNATURE_UPDATE );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } 
    }

    // Node methods ------------------------------------------------------------

    // NodeBase methods ----------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings(SpywareSettings settings)
    {
        logger.info("Initializing settings...");

        Set<String> cookieList = initList(COOKIE_LIST);
        List<GenericRule> cookieRules = new LinkedList<GenericRule>();
        for ( String cookie : cookieList) {
            String cook = cookie;
            /* ignore / at end if present */
            if (cookie.charAt(cookie.length()-1) == '/') 
                cook = cookie.substring(0,cookie.length()-1);
            cookieRules.add(new GenericRule(cook, cook, null, null, true));
        }
        settings.setCookies(cookieRules);

        Set<String> subnetList = initList(SUBNET_LIST);
        List<GenericRule> subnetRules = new LinkedList<GenericRule>();
        for ( String line : subnetList) {
            GenericRule rule = null;
            rule = makeGenericSubnetRule(line);
            if (rule == null)
                logger.warn("Failed to parse rule (null): " + line);
            else
                subnetRules.add(rule);
        }
        settings.setSubnets(subnetRules);
        
        return;
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        SpywareSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-spyware/" + "settings_" + nodeID;

        try {
            readSettings = settingsManager.load( SpywareSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
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
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
        
        deployWebAppIfRequired(logger);
    }

    @Override
    protected void preStart()
    {}

    @Override
    protected void postStop()
    {}

    @Override
    protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
    }

    // package private methods -------------------------------------------------

    void incrementSubnetScan()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementSubnetBlock()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementHttpScan()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementHttpWhitelisted()
    {
        this.incrementMetric(STAT_PASS);
    }

    void incrementHttpBlockedDomain()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementHttpPassed()
    {
        this.incrementMetric(STAT_PASS);
    }

    void incrementHttpClientCookieScan()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementHttpClientCookieBlock()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementHttpClientCookiePass()
    {
        this.incrementMetric(STAT_PASS);
    }

    void incrementHttpServerCookieScan()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementHttpServerCookieBlock()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementHttpServerCookiePass()
    {
        this.incrementMetric(STAT_PASS);
    }

    Token[] generateResponse(SpywareBlockDetails bd, NodeTCPSession sess, String uri, Header header, boolean persistent)
    {
        String n = replacementGenerator.generateNonce(bd);
        return replacementGenerator.generateResponse(n, sess, uri, header, persistent);
    }

    String generateNonce(String host, String uri, InetAddress addr)
    {
        SpywareBlockDetails bd = new SpywareBlockDetails(host, uri, addr);

        return replacementGenerator.generateNonce(bd);
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
                Set<String> l = unblockedSites.get(clientAddr);
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

        domain = domain.startsWith(".") && 1 < domain.length() ? domain.substring(1) : domain;

        if (null == cookieDomainMap || !settings.getScanCookies()) {
            return false;
        }

        boolean match = false;

        for (String d = domain; !match && null != d; d = nextHost(d)) {
            GenericRule sr = cookieDomainMap.get(d);
            if (sr != null) {
                if (sr.getBlocked() != null)
                    match = sr.getBlocked();
                else if (sr.getEnabled() != null) /* if Block is null, use enabled instead */
                    match = sr.getEnabled();
            }
        }

        return match;
    }

    // private methods ---------------------------------------------------------

    private boolean findMatch(Set<String> rules, String domain)
    {
        if (rules == null) {
            return false;
        }
        
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
            InputStream is = new FileInputStream(file);
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

        if (null != UvmContextFactory.context().tomcatManager().loadServlet("/spyware", "spyware")) {
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

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/spyware")) {
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
            this.cookieDomainMap = s;
        } else {
            this.cookieDomainMap = null;
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
    
    private void _setSettings( SpywareSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(SpywareSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-spyware/" + "settings_"  + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}


        /**
         * Reconfigure
         */
        this.reconfigure();
    }

    private GenericRule makeGenericSubnetRule(String line) 
    {
        StringTokenizer tok = new StringTokenizer(line, ",");

        String addr = tok.nextToken();
        String description = tok.nextToken();
        String name = tok.hasMoreTokens() ? tok.nextToken() : "[no name]";

        
        IPMaskedAddress maddr;
        try {
            maddr = IPMaskedAddress.parse( addr );

            if (maddr == null) {
                logger.warn("Invalid Masked Address: " + addr);
                return null;
            }
        } catch (Exception e) {
            logger.warn("Invalid Masked Address: " + addr, e);
            return null;
        }
        
        GenericRule rule = new GenericRule(addr, name, "[no category]", description, true);
        rule.setFlagged(true);
        rule.setBlocked(false);

        return rule;
    }

}
