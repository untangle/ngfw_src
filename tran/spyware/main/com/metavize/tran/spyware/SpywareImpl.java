/*
 * Copyright (c) 2004, 2005 Metavize Inc.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.logging.EventFilter;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class SpywareImpl extends AbstractTransform implements Spyware
{
    private static final String ACTIVEX_LIST
        = "com/metavize/tran/spyware/blocklist.reg";
    private static final String COOKIE_LIST
        = "com/metavize/tran/spyware/cookie.txt";
    private static final String SUBNET_LIST
        = "com/metavize/tran/spyware/subnet.txt";

    private static final Pattern ACTIVEX_PATTERN = Pattern
        .compile(".*\\{([a-fA-F0-9\\-]+)\\}.*");

    private final Logger logger = Logger.getLogger(getClass());

    private static final int HTTP = 0;
    private static final int BYTE = 1;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(this, factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final EventLogger<SpywareEvent> eventLogger;

    private final Set urlBlacklist;

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", this, tokenAdaptor,
                           Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", this, streamHandler,
                           Fitting.OCTET_STREAM, Affinity.SERVER, 0) };

    private volatile SpywareSettings settings;
    private volatile Map<String, StringRule> activeXRules;
    private volatile Map<String, StringRule> cookieRules;
    private volatile Set<String> domainWhitelist;

    // constructors -----------------------------------------------------------

    public SpywareImpl()
    {
        urlBlacklist = SpywareCache.cache().getUrls();

        TransformContext tctx = getTransformContext();
        eventLogger = new EventLogger<SpywareEvent>(tctx);

        EventFilter ef = new SpywareAllFilter();
        eventLogger.addEventFilter(ef);
        ef = new SpywareBlockedFilter();
        eventLogger.addEventFilter(ef);
        ef = new SpywareAccessFilter();
        eventLogger.addEventFilter(ef);
        ef = new SpywareActiveXFilter();
        eventLogger.addEventFilter(ef);
        ef = new SpywareBlacklistFilter();
        eventLogger.addEventFilter(ef);
        ef = new SpywareCookieFilter();
        eventLogger.addEventFilter(ef);
    }

    // SpywareTransform methods -----------------------------------------------

    public SpywareSettings getSpywareSettings()
    {
        return settings;
    }

    public void setSpywareSettings(final SpywareSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    SpywareImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        reconfigure();
    }

    public EventManager<SpywareEvent> getEventManager()
    {
        return eventLogger;
    }

    // Transform methods ------------------------------------------------------

    // XXX avoid
    public void reconfigure()
    {
        logger.info("Reconfigure.");
        if (this.settings.getSpywareEnabled()) {
            streamHandler.subnetList(this.settings.getSubnetRules());
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

        setSpywareSettings(settings);
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

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        reconfigure();
    }

    protected void preStart()
    {
        eventLogger.start();
    }

    protected void postStop()
    {
        eventLogger.stop();
    }

    // package private methods ------------------------------------------------

    boolean isBlacklistDomain(String domain, URI uri)
    {
        if (!settings.getUrlBlacklistEnabled()) {
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

        if (null == cookieRules && !settings.getCookieBlockerEnabled()) {
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

    private void updateActiveX(SpywareSettings settings)
    {
        List rules = settings.getActiveXRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(ACTIVEX_LIST);

        if (null == is) {
            logger.error("Could not find: " + ACTIVEX_LIST);
            return;
        }

        logger.info("Checking for activeX updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            StringRule rule = (StringRule) i.next();
            ruleHash.add(rule.getString());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String l = br.readLine(); null != l; l = br.readLine()) {
                Matcher matcher = ACTIVEX_PATTERN.matcher(l);
                if (matcher.matches()) {
                    String clsid = matcher.group(1);

                    if (!ruleHash.contains(clsid)) {
                        logger.debug("ADDING activeX Rule: " + clsid);
                        rules.add(new StringRule(clsid.intern()));
                    }
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + ACTIVEX_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + ACTIVEX_LIST, exn);
            }
        }

        return;
    }

    private void updateCookie(SpywareSettings settings)
    {
        List rules = settings.getCookieRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(COOKIE_LIST);

        if (null == is) {
            logger.error("Could not find: " + COOKIE_LIST);
            return;
        }

        logger.info("Checking for cookie  updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            StringRule rule = (StringRule) i.next();
            ruleHash.add(rule.getString());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String l = br.readLine(); null != l; l = br.readLine()) {
                if (!ruleHash.contains(l)) {
                    logger.debug("ADDING cookie Rule: " + l);
                    rules.add(new StringRule(l.intern()));
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + COOKIE_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + COOKIE_LIST, exn);
            }
        }

        return;
    }

    private void updateSubnet(SpywareSettings settings)
    {
        List rules = settings.getSubnetRules();
        InputStream is = getClass().getClassLoader().getResourceAsStream(SUBNET_LIST);

        if (null == is) {
            logger.warn("Could not find: " + SUBNET_LIST);
            return;
        }

        logger.info("Checking for subnet  updates...");

        HashSet ruleHash = new HashSet();
        for (Iterator i=rules.iterator() ; i.hasNext() ; ) {
            IPMaddrRule rule = (IPMaddrRule) i.next();
            rule.setLive(false);
            ruleHash.add(rule.getIpMaddr());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                StringTokenizer tok = new StringTokenizer(line, ":,");

                String addr = null;
                String description = null;
                String name = null;
                IPMaddr maddr = null;

                try {
                    addr = tok.nextToken();
                    description = tok.nextToken();
                    name = tok.nextToken();
                    maddr = IPMaddr.parse(addr);
                    int i = maddr.maskNumBits(); /* if bad subnet throws exception */
                }
                catch (Exception e) {
                    logger.warn("Invalid Subnet in " + SUBNET_LIST + ": " + line + ": " + e);
                    maddr = null;
                }

                if (maddr != null && !ruleHash.contains(maddr)) {
                    logger.debug("ADDING subnet Rule: " + addr);
                    IPMaddrRule rule = new IPMaddrRule(maddr, name, "[no category]", description);
                    rule.setLog(true);
                    rule.setLive(false);
                    rules.add(rule);
                }
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + SUBNET_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + SUBNET_LIST, exn);
            }
        }

        return;
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
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
