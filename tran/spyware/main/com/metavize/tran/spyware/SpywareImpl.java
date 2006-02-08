/*
 * copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.AsyncSettings;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SettingsChangeListener;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;

public class SpywareImpl extends AbstractTransform implements Spyware
{
    final SpywareStatisticManager statisticManager;

    private static final String ACTIVEX_LIST
        = "com/metavize/tran/spyware/activex.txt";
    private static final String ACTIVEX_DIFF_BASE
        = "com/metavize/tran/spyware/activex-diff-";
    private static final String COOKIE_LIST
        = "com/metavize/tran/spyware/cookie.txt";
    private static final String COOKIE_DIFF_BASE
        = "com/metavize/tran/spyware/cookie-diff-";
    private static final String SUBNET_LIST
        = "com/metavize/tran/spyware/subnet.txt";
    private static final String SUBNET_DIFF_BASE
        = "com/metavize/tran/spyware/subnet-diff-";

    private final Logger logger = Logger.getLogger(getClass());

    private static final int HTTP = 0;
    private static final int BYTE = 1;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(this, factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final SettingsChangeListener<SpywareSettings> CHANGE_LISTENER
        = new SettingsChangeListener<SpywareSettings>()
    {
        public void newSettings(SpywareSettings settings) {
            if (null == settings) {
                initializeSettings();
            } else {
                updateActiveX(settings);
                updateCookie(settings);
                updateSubnet(settings);
            }

            doReconfigure();
        }
    };

    private final EventLogger<SpywareEvent> eventLogger;

    private final Set urlBlacklist;

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", this, tokenAdaptor,
                           Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", this, streamHandler,
                           Fitting.OCTET_STREAM, Affinity.SERVER, 0) };

    private final AsyncSettings<SpywareSettings> settings
        = new AsyncSettings<SpywareSettings>();

    private volatile Map<String, StringRule> activeXRules = Collections.emptyMap();
    private volatile Map<String, StringRule> cookieRules = Collections.emptyMap();
    private volatile Set<String> domainWhitelist = Collections.emptySet();

    // constructors -----------------------------------------------------------

    public SpywareImpl()
    {
        urlBlacklist = SpywareCache.cache().getUrls();

        TransformContext tctx = getTransformContext();
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
    }

    // SpywareTransform methods -----------------------------------------------

    public SpywareSettings getSpywareSettings()
    {
        return settings.getSettings();
    }

    public void setSpywareSettings(SpywareSettings settings)
    {
        this.settings.setSettings(settings);
    }

    public EventManager<SpywareEvent> getEventManager()
    {
        return eventLogger;
    }

    // Transform methods ------------------------------------------------------

    // XXX avoid
    private void doReconfigure()
    {
        SpywareSettings settings = this.settings.getSettings();

        logger.info("Reconfigure.");
        if (settings.getSpywareEnabled()) {
            streamHandler.subnetList(settings.getSubnetRules());
        }

        List<StringRule> l = (List<StringRule>)settings.getActiveXRules();
        if (null != l) {
            Map<String, StringRule> s = new HashMap<String, StringRule>();
            for (StringRule sr : l) {
                s.put(sr.getString(), sr);
            }
            activeXRules = s;
        } else {
            activeXRules = null;
        }

        l = (List<StringRule>)settings.getCookieRules();
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
        l = (List<StringRule>)settings.getDomainWhitelist();
        for (StringRule sr : l) {
            if (sr.isLive()) {
                String str = sr.getString().toLowerCase();
                if (str.startsWith("http://")) {
                    try {
                        URL url = new URL(str);
                        s.add(url.getHost());
                    } catch (MalformedURLException exn) {
                        logger.warn("skipping non-url: " + s, exn);
                    }
                } else {
                    s.add(str);
                }
            }
        }
        domainWhitelist = s;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
        protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void initializeSettings()
    {
        SpywareSettings settings = new SpywareSettings(getTid());
        settings.setActiveXRules(new ArrayList());
        settings.setCookieRules(new ArrayList());
        settings.setSubnetRules(new ArrayList());

        updateActiveX(settings);
        updateCookie(settings);
        updateSubnet(settings);

        this.settings.init(getTransformContext(), settings, CHANGE_LISTENER);

        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        if (!settings.isInitialized()) {
            settings.init(getTransformContext(),
                          "from SpywareSettings ss where ss.tid = :tid",
                          CHANGE_LISTENER);
        }
    }

    @Override
    protected void preStart()
    {
        eventLogger.start();
        statisticManager.start();
    }

    @Override
    protected void postStop()
    {
        eventLogger.stop();
        statisticManager.stop();
    }

    // package private methods ------------------------------------------------

    boolean isBlacklistDomain(String domain, URI uri)
    {
        if (!settings.getSettings().getUrlBlacklistEnabled()) {
            return false;
        }

        boolean match = false;

        domain = null == domain ? null : domain.toLowerCase();
        for (String d = domain; !match && null != d; d = nextHost(d)) {
            match = urlBlacklist.contains(d);
        }

        return match;
    }

    boolean isWhitelistDomain(String domain)
    {
        boolean match = false;

        domain = null == domain ? null : domain.toLowerCase();
        for (String d = domain; !match && null != d; d = nextHost(d)) {
            match = domainWhitelist.contains(d);
        }

        return match;
    }

    boolean isBlockedCookie(String domain)
    {
        if (null == domain) {
            logger.warn("null domain for cookie");
            return false;
        }

        domain = domain.startsWith(".") && 1 < domain.length()
            ? domain.substring(1) : domain;

        if (null == cookieRules && !settings.getSettings().getCookieBlockerEnabled()) {
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

    // private methods --------------------------------------------------------

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    // settings intialization -------------------------------------------------

    private void updateActiveX(SpywareSettings settings)
    {
        int ver = settings.getActiveXVersion();

        if (0 > ver || null == settings.getActiveXRules()) {
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
        List<StringRule> rules = (List<StringRule>)settings.getActiveXRules();
        if (null == rules) {
            rules = new LinkedList<StringRule>();
            settings.setActiveXRules(rules);
        }

        for (Iterator<StringRule> i = rules.iterator(); i.hasNext(); ) {
            StringRule sr = i.next();
            if (remove.contains(sr.getString())) {
                i.remove();
            } else {
                remove.remove(sr);
            }
        }

        for (String s : add) {
            rules.add(new StringRule(s));
        }
    }

    private void updateCookie(SpywareSettings settings)
    {
        int ver = settings.getCookieVersion();

        if (0 > ver || null == settings.getCookieRules()) {
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
        List<StringRule> rules = (List<StringRule>)settings.getCookieRules();
        if (null == rules) {
            rules = new LinkedList<StringRule>();
            settings.setCookieRules(rules);
        }

        for (Iterator<StringRule> i = rules.iterator(); i.hasNext(); ) {
            StringRule sr = i.next();
            if (remove.contains(sr.getString())) {
                i.remove();
            } else {
                remove.remove(sr);
            }
        }

        for (String s : add) {
            rules.add(new StringRule(s));
        }
    }

    private void updateSubnet(SpywareSettings settings)
    {
        int ver = settings.getSubnetVersion();

        if (0 > ver || null == settings.getSubnetRules()) {
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

        List<IPMaddrRule> rules = (List<IPMaddrRule>)settings.getSubnetRules();
        if (null == rules) {
            rules = new LinkedList<IPMaddrRule>();
            settings.setSubnetRules(rules);
        }

        for (Iterator<IPMaddrRule> i = rules.iterator(); i.hasNext(); ) {
            IPMaddrRule imr = i.next();

            if (remove.contains(imr)) {
                i.remove();
            } else {
                remove.remove(imr);
            }
        }

        for (String s : add) {
            IPMaddrRule imr = makeIPMAddrRule(s);
            if (null != imr) {
                rules.add(imr);
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

    public int latestVer(String diffBase)
    {
        for (int i = 0; ; i++) {
            URL u = getClass().getClassLoader().getResource(diffBase + i);
            if (null == u) {
                return i;
            }
        }
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getSpywareSettings();
    }

    public void setSettings(Object settings)
    {
        setSpywareSettings((SpywareSettings)settings);
    }
}
