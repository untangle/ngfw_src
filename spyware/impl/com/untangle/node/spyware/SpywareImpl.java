/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spyware;

import java.io.BufferedReader;
import java.io.IOException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.sleepycat.je.DatabaseException;
import com.untangle.node.http.UserWhitelistMode;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.node.util.PrefixUrlList;
import com.untangle.node.util.UrlDatabase;
import com.untangle.node.util.UrlDatabaseResult;
import com.untangle.node.util.UrlList;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.toolbox.ToolboxManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.TCPSession;
import org.apache.catalina.Valve;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class SpywareImpl extends AbstractNode implements Spyware
{
    private static final String ACTIVEX_LIST
        = "com/untangle/node/spyware/activex.txt";
    private static final String ACTIVEX_DIFF_BASE
        = "com/untangle/node/spyware/activex-diff-";
    private static final String COOKIE_LIST
        = "com/untangle/node/spyware/cookie.txt";
    private static final String COOKIE_DIFF_BASE
        = "com/untangle/node/spyware/cookie-diff-";
    private static final String SUBNET_LIST
        = "com/untangle/node/spyware/subnet.txt";
    private static final String SUBNET_DIFF_BASE
        = "com/untangle/node/spyware/subnet-diff-";

    private static final URL BLACKLIST_HOME;

    private static final int HTTP = 0;
    private static final int BYTE = 1;

    private static int deployCount = 0;

    static {
        try {
            BLACKLIST_HOME = new URL("http://webupdates.untangle.com/diffserver");
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn);
        }
    }

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(this, factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final EventLogger<SpywareEvent> eventLogger;

    private final UrlDatabase urlDatabase = new UrlDatabase();

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", this, tokenAdaptor,
                           Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", this, streamHandler,
                           Fitting.OCTET_STREAM, Affinity.SERVER, 0) };

    private final Map<InetAddress, Set<String>> hostWhitelists
        = new HashMap<InetAddress, Set<String>>();

    private final Logger logger = Logger.getLogger(getClass());

    private final PartialListUtil listUtil = new PartialListUtil();

    private final BlingBlinger scanBlinger;
    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;
    // private final BlingBlinger spyPagesDetectedBlinger;

    private volatile SpywareSettings settings;

    private volatile Map<String, StringRule> activeXRules;
    private volatile Map<String, StringRule> cookieRules;
    private volatile Set<String> domainWhitelist;

    /* the signatures are updated at startup, so using new Date() is
     * not that far off. */
    private Date lastSignatureUpdate = new Date();
    private String signatureVersion;

    private final SpywareReplacementGenerator replacementGenerator;

    final SpywareStatisticManager statisticManager;

    // constructors ------------------------------------------------------------

    public SpywareImpl()
    {
        replacementGenerator = new SpywareReplacementGenerator(getTid());

        final LocalUvmContext uvm = LocalUvmContextFactory.context();

        Thread t = new Thread(new Runnable() {
                public void run()
                {
                    boolean fail = true;

                    while (fail) {
                        Map m = new HashMap();
                        m.put("key", uvm.getActivationKey());
                        ToolboxManager tm = uvm.toolboxManager();
                        m.put("client-version", uvm.getFullVersion());

                        try {
                            for (String list : getSpywareLists()) {
                                if (list.startsWith("spyware-")
                                    && (list.endsWith("dom")
                                        || list.endsWith("url"))) {
                                    UrlList l
                                        = new PrefixUrlList(BLACKLIST_HOME,
                                                            "spyware", list, m,
                                                            null);
                                    urlDatabase.addBlacklist(list, l);
                                }
                            }
                            urlDatabase.updateAll(false);
                            fail = false;
                        } catch (IOException exn) {
                            logger.warn("could not set up database", exn);
                        } catch (DatabaseException exn) {
                            logger.warn("could not set up database", exn);
                        }

                        if (fail) {
                            logger.info("failed to update lists, retrying in 15 minutes");
                            try {
                                Thread.currentThread().sleep(15 * 60 * 1000);
                            } catch (InterruptedException exn) {
                                logger.warn("could not sleep", exn);
                            }
                        }
                    }
                }
            }, "spyware-init");
        t.setDaemon(true);
        t.start();

        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        statisticManager = new SpywareStatisticManager(tctx);

        SimpleEventFilter ef = new SpywareAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareAccessFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareActiveXFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareBlacklistFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new SpywareCookieFilter();
        eventLogger.addSimpleEventFilter(ef);

        LocalMessageManager lmm = LocalUvmContextFactory.context()
            .localMessageManager();
        Counters c = lmm.getCounters(getTid());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Pages scanned"), null, I18nUtil.marktr("SCAN"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Pages blocked"), null, I18nUtil.marktr("BLOCK"));
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Pages passed"), null, I18nUtil.marktr("PASS"));
        // What was this supposed to be?
        // spyPagesDetectedBlinger = c.addMetric("spydetected", I18nUtil.marktr("Spyware Pages Detected"), null);
        lmm.setActiveMetricsIfNotSet(getTid(), scanBlinger, blockBlinger, passBlinger);
    }

    // SpywareNode methods -----------------------------------------------------

    public List<StringRule> getActiveXRules(final int start, final int limit,
                                            final String... sortColumns)
    {
        return listUtil.getItems("select s.activeXRules from SpywareSettings s where s.tid = :tid ",
                                 getNodeContext(), getTid(), start, limit,
                                 sortColumns);

    }

    public void updateActiveXRules(List<StringRule> added, List<Long> deleted,
                                   List<StringRule> modified)
    {
        updateRules(settings.getActiveXRules(), added, deleted, modified);
    }

    public List<StringRule> getCookieRules(int start, int limit,
                                           String... sortColumns)
    {
        return listUtil.getItems("select s.cookieRules from SpywareSettings s where s.tid = :tid ",
                                 getNodeContext(), getTid(), start, limit,
                                 sortColumns);
    }

    public void updateCookieRules(List<StringRule> added, List<Long> deleted,
                                  List<StringRule> modified)
    {
        updateRules(settings.getCookieRules(), added, deleted, modified);
    }

    public List<IPMaddrRule> getSubnetRules(int start, int limit,
                                            String... sortColumns)
    {
        return listUtil.getItems("select s.subnetRules from SpywareSettings s where s.tid = :tid ",
                                 getNodeContext(), getTid(), start, limit,
                                 sortColumns);
    }

    public void updateSubnetRules(List<IPMaddrRule> added, List<Long> deleted,
                                  List<IPMaddrRule> modified)
    {
        updateRules(settings.getSubnetRules(), added, deleted, modified);
    }

    public List<StringRule> getDomainWhitelist(int start, int limit,
                                               String... sortColumns)
    {
        return listUtil.getItems("select s.domainWhitelist from SpywareSettings s where s.tid = :tid ",
                                 getNodeContext(), getTid(), start, limit,
                                 sortColumns);
    }

    public void updateDomainWhitelist(List<StringRule> added,
                                      List<Long> deleted,
                                      List<StringRule> modified)
    {
        updateRules(settings.getDomainWhitelist(), added, deleted, modified);
    }

    public SpywareBaseSettings getBaseSettings()
    {
        SpywareBaseSettings baseSettings = settings.getBaseSettings();
        /* Insert the last update information */
        baseSettings.setLastUpdate(this.lastSignatureUpdate);
        /* Have to figure out how to calculate the version string. */
        return baseSettings;
    }

    public void setBaseSettings(final SpywareBaseSettings baseSettings)
    {
        TransactionWork tw = new TransactionWork() {
                public boolean doWork(Session s) {
                    settings.setBaseSettings(baseSettings);
                    s.merge(settings);
                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);
    }

    public UserWhitelistMode getUserWhitelistMode()
    {
        return settings.getBaseSettings().getUserWhitelistMode();
    }

    public SpywareBlockDetails getBlockDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public boolean unblockSite(String nonce, boolean global)
    {
        SpywareBlockDetails bd = replacementGenerator.removeNonce(nonce);

        switch (settings.getBaseSettings().getUserWhitelistMode()) {
        case NONE:
            logger.debug("attempting to unblock in UserWhitelistMode.NONE");
            return false;
        case USER_ONLY:
            if (global) {
                logger.debug("attempting to unblock global in UserWhitelistMode.USER_ONLY");
                return false;
            }
        case USER_AND_GLOBAL:
            // its all good
            break;
        default:
            logger.error("missing case: " + settings.getBaseSettings().getUserWhitelistMode());
            break;
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
                StringRule sr = new StringRule(site, site, "user unblocked",
                                               "unblocked by user", true);
                settings.getDomainWhitelist().add(sr);
                setSpywareSettings(settings);

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

    public void initializeSettings()
    {
        SpywareSettings settings = new SpywareSettings(getTid());

        updateActiveX(settings);
        updateCookie(settings);
        updateSubnet(settings);

        setSpywareSettings(settings);

        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from SpywareSettings ss where ss.tid = :tid");
                    q.setParameter("tid", getTid());
                    SpywareImpl.this.settings = (SpywareSettings)q.uniqueResult();

                    updateActiveX(SpywareImpl.this.settings);
                    updateCookie(SpywareImpl.this.settings);
                    updateSubnet(SpywareImpl.this.settings);

                    return true;
                }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();

        deployWebAppIfRequired(logger);
    }

    @Override
    protected void preStart()
    {
        statisticManager.start();
        urlDatabase.startUpdateTimer();
    }

    @Override
    protected void postStop()
    {
        statisticManager.stop();
        urlDatabase.stopUpdateTimer();
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

    void incrementHttpActiveXScan()
    {
        scanBlinger.increment();
    }

    void incrementHttpActiveXBlock()
    {
        blockBlinger.increment();
    }

    void incrementHttpActiveXPass()
    {
        passBlinger.increment();
    }

    Token[] generateResponse(SpywareBlockDetails bd, TCPSession sess,
                             String uri, Header header, boolean persistent)
    {
        String n = replacementGenerator.generateNonce(bd);
        return replacementGenerator.generateResponse(n, sess, uri, header,
                                                     persistent);
    }

    String generateNonce(String host, String uri, InetAddress addr)
    {
        SpywareBlockDetails bd = new SpywareBlockDetails(host, uri, addr);

        return replacementGenerator.generateNonce(bd);
    }

    boolean isBlacklistDomain(String domain, URI uri)
    {
        if (!settings.getBaseSettings().getUrlBlacklistEnabled()) {
            return false;
        }

        boolean match = false;

        domain = null == domain ? null : domain.toLowerCase();
        for (String d = domain; !match && null != d; d = nextHost(d)) {
            UrlDatabaseResult udr = urlDatabase.search("http", d, "/");
            match = null != udr && udr.blacklisted();
        }

        return match;
    }

    boolean isWhitelistedDomain(String domain, InetAddress clientAddr)
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            if (findMatch(domainWhitelist, domain)) {
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

    boolean isBlockedCookie(String domain)
    {
        if (null == domain) {
            logger.warn("null domain for cookie");
            return false;
        }

        domain = domain.startsWith(".") && 1 < domain.length()
            ? domain.substring(1) : domain;

        if (null == cookieRules || !settings.getBaseSettings().getCookieBlockerEnabled()) {
            return false;
        }

        boolean match = false;

        for (String d = domain; !match && null != d; d = nextHost(d)) {
            StringRule sr = cookieRules.get(d);
            match = null != sr && sr.isLive();
        }

        return match;
    }

    StringRule getBlockedActiveX(String clsId)
    {
        return null == activeXRules ? null : activeXRules.get(clsId);
    }

    void log(SpywareEvent se)
    {
        eventLogger.log(se);
    }

    public void updateAll(final SpywareBaseSettings baseSettings,
                          final List[] activeXRules, final List[] cookieRules,
                          final List[] subnetRules, final List[] domainWhitelist) {

        TransactionWork tw = new TransactionWork() {
                public boolean doWork(Session s) {
                    if (baseSettings != null) {
                        settings.setBaseSettings(baseSettings);
                    }

                    listUtil.updateCachedItems(settings.getActiveXRules(), activeXRules);

                    listUtil.updateCachedItems(settings.getCookieRules(), cookieRules);

                    listUtil.updateCachedItems(settings.getSubnetRules(), subnetRules);

                    listUtil.updateCachedItems(settings.getDomainWhitelist(), domainWhitelist);

                    settings = (SpywareSettings)s.merge(settings);

                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    public Validator getValidator() {
        return new SpywareValidator();
    }

    // private methods ---------------------------------------------------------

    private void updateRules(final Set rules, final List added,
                             final List<Long> deleted, final List modified)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    listUtil.updateCachedItems(rules, added, deleted, modified);

                    settings = (SpywareSettings)s.merge(settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    private boolean findMatch(Set<String> rules, String domain)
    {
        for (String d = domain; null != d; d = nextHost(d)) {
            if (rules.contains(d)) {
                return true;
            }
        }

        return false;
    }

    // XXX factor this shit out!
    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    // settings intialization --------------------------------------------------

    private void updateActiveX(SpywareSettings settings)
    {
        int ver = settings.getActiveXVersion();

        if (0 > ver || null == settings.getActiveXRules()) {
            Set l = settings.getActiveXRules();
            if (null != l) {
                l.clear();
            }
            Set<String> add = initList(ACTIVEX_LIST);
            Set<String> remove = Collections.emptySet();
            updateActiveX(settings, add, remove);
            settings.setActiveXVersion(latestVer(ACTIVEX_DIFF_BASE));
        } else {
            Set<String> add = new HashSet<String>();
            Set<String> remove = new HashSet<String>();
            ver = diffSets(ACTIVEX_DIFF_BASE, ver, add, remove);
            updateActiveX(settings, add, remove);
            settings.setActiveXVersion(ver);
        }
    }

    private void updateActiveX(SpywareSettings settings, Set<String> add,
                               Set<String> remove)
    {
        Set<StringRule> rules = (Set<StringRule>)settings.getActiveXRules();
        if (null == rules) {
            rules = new HashSet<StringRule>();
            settings.setActiveXRules(rules);
        }

        for (Iterator<StringRule> i = rules.iterator(); i.hasNext(); ) {
            StringRule sr = i.next();
            if (remove.contains(sr.getString())) {
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("removing activex: " + sr.getString());
                }
            } else {
                remove.remove(sr);
                if (logger.isDebugEnabled()) {
                    logger.debug("not removing activex: " + sr.getString());
                }
            }
        }

        for (String s : add) {
            logger.debug("adding activex: " + s);
            rules.add(new StringRule(s));
        }
    }

    private void updateCookie(SpywareSettings settings)
    {
        int ver = settings.getCookieVersion();

        if (0 > ver || null == settings.getCookieRules()) {
            Set l = settings.getCookieRules();
            if (null != l) {
                l.clear();
            }
            Set<String> add = initList(COOKIE_LIST);
            Set<String> remove = Collections.emptySet();
            updateCookie(settings, add, remove);
            settings.setCookieVersion(latestVer(COOKIE_DIFF_BASE));
        } else {
            Set<String> add = new HashSet<String>();
            Set<String> remove = new HashSet<String>();
            ver = diffSets(COOKIE_DIFF_BASE, ver, add, remove);
            updateCookie(settings, add, remove);
            settings.setCookieVersion(ver);
        }
    }

    private void updateCookie(SpywareSettings settings, Set<String> add,
                              Set<String> remove)
    {
        Set<StringRule> rules = (Set<StringRule>)settings.getCookieRules();
        if (null == rules) {
            rules = new HashSet<StringRule>();
            settings.setCookieRules(rules);
        }

        for (Iterator<StringRule> i = rules.iterator(); i.hasNext(); ) {
            StringRule sr = i.next();
            if (remove.contains(sr.getString())) {
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("removing cookie: " + sr.getString());
                }
            } else {
                remove.remove(sr);
                if (logger.isDebugEnabled()) {
                    logger.debug("not cookie: " + sr.getString());
                }
            }
        }

        for (String s : add) {
            rules.add(new StringRule(s));
            if (logger.isDebugEnabled()) {
                logger.debug("added cookie: " + s);
            }
        }
    }

    private void updateSubnet(SpywareSettings settings)
    {
        int ver = settings.getSubnetVersion();

        if (0 > ver || null == settings.getSubnetRules()) {
            Set l = settings.getSubnetRules();
            if (null != l) {
                l.clear();
            }

            Set<String> add = initList(SUBNET_LIST);
            Set<String> remove = Collections.emptySet();
            updateSubnet(settings, add, remove);
            settings.setSubnetVersion(latestVer(SUBNET_DIFF_BASE));
        } else {
            Set<String> add = new HashSet<String>();
            Set<String> remove = new HashSet<String>();
            ver = diffSets(SUBNET_DIFF_BASE, ver, add, remove);
            updateSubnet(settings, add, remove);
            settings.setSubnetVersion(ver);
        }
    }

    private void updateSubnet(SpywareSettings settings, Set<String> add,
                              Set<String> rem)
    {
        Set<IPMaddrRule> remove = new HashSet<IPMaddrRule>();
        for (String s : rem) {
            IPMaddrRule imr = makeIPMAddrRule(s);
            if (null != imr) {
                remove.add(imr);
            }
        }

        Set<IPMaddrRule> rules = (Set<IPMaddrRule>)settings.getSubnetRules();
        if (null == rules) {
            rules = new HashSet<IPMaddrRule>();
            settings.setSubnetRules(rules);
        }

        for (Iterator<IPMaddrRule> i = rules.iterator(); i.hasNext(); ) {
            IPMaddrRule imr = i.next();

            if (remove.contains(imr)) {
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("removed subnet: " + imr.getIpMaddr());
                }
            } else {
                remove.remove(imr);
                if (logger.isDebugEnabled()) {
                    logger.debug("not removed subnet: " + imr.getIpMaddr());
                }
            }
        }

        for (String s : add) {
            IPMaddrRule imr = makeIPMAddrRule(s);
            if (null != imr) {
                rules.add(imr);
                if (logger.isDebugEnabled()) {
                    logger.debug("added subnet: " + s);
                }
            }
        }
    }

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

    private IPMaddrRule makeIPMAddrRule(String line)
    {
        StringTokenizer tok = new StringTokenizer(line, ":,");

        String addr = tok.nextToken();
        String description = tok.nextToken();
        String name = tok.hasMoreTokens() ? tok.nextToken() : "[no name]";

        IPMaddr maddr;
        try {
            maddr = IPMaddr.parse(addr);
            int i = maddr.maskNumBits(); /* if bad subnet throws exception */
        } catch (Exception e) {
            return null;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("ADDING subnet Rule: " + addr);
        }
        IPMaddrRule rule = new IPMaddrRule(maddr, name, "[no category]", description);
        rule.setLog(true);
        rule.setLive(false);

        return rule;
    }

    private int diffSets(String diffBase, int startVersion,
                         Set<String> add, Set<String> remove)
    {
        for (int i = startVersion + 1; ; i++) {
            String r = diffBase + i;
            InputStream is = getClass().getClassLoader().getResourceAsStream(r);

            if (null == is) {
                return i - 1;
            }

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                for (String l = br.readLine(); null != l; l = br.readLine()) {
                    if (l.startsWith("<")) {
                        String s = l.substring(2);
                        add.remove(s);
                        remove.add(s);
                    } else if (l.startsWith(">")) {
                        String s = l.substring(2);
                        add.add(s);
                        remove.remove(s);
                    }
                }
            } catch (IOException exn) {
                logger.error("could not make diffs: " + diffBase, exn);
            }
        }
    }

    private int latestVer(String diffBase)
    {
        for (int i = 0; ; i++) {
            URL u = getClass().getClassLoader().getResource(diffBase + i);
            if (null == u) {
                return i;
            }
        }
    }

    // XXX factor out this shit
    private static synchronized void deployWebAppIfRequired(Logger logger) {
        if (0 != deployCount++) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

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

    // XXX factor out this shit
    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        if (asm.unloadWebApp("/spyware")) {
            logger.debug("Unloaded Spyware WebApp");
        } else {
            logger.warn("Unable to unload Spyware WebApp");
        }
    }

    public void reconfigure()
    {
        logger.info("Reconfigure.");
        if (this.settings.getBaseSettings().getSpywareEnabled()) {
            streamHandler.subnetList(this.settings.getSubnetRules());
        }

        Set<StringRule> l = (Set<StringRule>)settings.getActiveXRules();
        if (null != l) {
            Map<String, StringRule> s = new HashMap<String, StringRule>();
            for (StringRule sr : l) {
                s.put(sr.getString(), sr);
            }
            activeXRules = s;
        } else {
            activeXRules = null;
        }

        l = (Set<StringRule>)settings.getCookieRules();
        if (null != l) {
            Map<String, StringRule> s = new HashMap<String, StringRule>();
            for (StringRule sr : l) {
                s.put(sr.getString(), sr);
            }
            cookieRules = s;
        } else {
            cookieRules = null;
        }

        Set<String> s = new HashSet<String>();
        l = (Set<StringRule>)settings.getDomainWhitelist();
        for (StringRule sr : l) {
            if (sr.isLive()) {
                String str = normalizeDomain(sr.getString());

                s.add(str);
            }
        }
        domainWhitelist = s;
    }

    private void setSpywareSettings(final SpywareSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    SpywareImpl.this.settings = (SpywareSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
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

    private List<String> getSpywareLists()
    {
        List<String> l = new ArrayList<String>();

        try {
            HttpClient hc = new HttpClient();
            String url = BLACKLIST_HOME.toString() + "/list";
            HttpMethod get = new GetMethod(url);
            int rc = hc.executeMethod(get);
            InputStream is = get.getResponseBodyAsStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            for (String s = br.readLine(); null != s; s = br.readLine()) {
                l.add(s);
            }
        } catch (IOException exn) {
            logger.warn("could not get listing", exn);
        }

        return l;
    }

    /* Probably would be better if URL database took a listener. */
    private class SpywareUrlDatabase extends UrlDatabase
    {
        public void updateAll(boolean async)
        {
            super.updateAll(async);

            SpywareImpl.this.lastSignatureUpdate = new Date();
        }
    }
}
