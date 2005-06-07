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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.IPSessionDesc;
import com.metavize.mvvm.tapi.MPipe;
import com.metavize.mvvm.tapi.MPipeManager;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tapi.event.SessionEventListener;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.token.TokenAdaptor;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

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

    private static final Logger logger = Logger.getLogger(SpywareImpl.class);

    private static final int HTTP = 0;
    private static final int BYTE = 1;

    private final SpywareHttpFactory factory = new SpywareHttpFactory(this);
    private final TokenAdaptor tokenAdaptor = new TokenAdaptor(factory);
    private final SpywareEventHandler streamHandler = new SpywareEventHandler(this);

    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        { new SoloPipeSpec("spyware-http", new Subscription(Protocol.TCP),
                             Fitting.HTTP_TOKENS, Affinity.SERVER, 0),
          new SoloPipeSpec("spyware-byte", new Subscription(Protocol.TCP),
                             Fitting.OCTET_STREAM, Affinity.SERVER, 0) };
    private final MPipe[] mPipes = new MPipe[2];
    private final SessionEventListener[] listeners = new SessionEventListener[]
        { tokenAdaptor, streamHandler };

    private SpywareSettings spySettings;

    // constructors -----------------------------------------------------------

    public SpywareImpl() { }

    // SpywareTransform methods -----------------------------------------------

    public SpywareSettings getSpywareSettings()
    {
        return this.spySettings;
    }

    public void setSpywareSettings(SpywareSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.spySettings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close hibernate sessino", exn);
            }
        }

        reconfigure();
    }

    // Transform methods ------------------------------------------------------

    // XXX aviod

    public void reconfigure()
    {
        logger.info("Reconfigure.");
        if (this.spySettings.getSpywareEnabled()) {
            streamHandler.subnetList(this.spySettings.getSubnetRules());
        }
    }

    public void dumpSessions()
    {
        // XXX implement
    }

    public IPSessionDesc[] liveSessionDescs()
    {
        // XXX implement
        return null;
    }

    // AbstractTransform methods ----------------------------------------------

    protected void connectMPipe()
    {
        for (int i = 0; i < pipeSpecs.length; i++) {
            mPipes[i] = MPipeManager.manager().plumbLocal(this, pipeSpecs[i]);
            mPipes[i].setSessionEventListener(listeners[i]);
            MvvmContextFactory.context().pipelineFoundry().registerMPipe(mPipes[i]);
        }
    }

    protected void disconnectMPipe()
    {
        for (int i = 0; i < mPipes.length; i++) {
            if ( mPipes[i] != null ) {
                MvvmContextFactory.context().pipelineFoundry().deregisterMPipe(mPipes[i]);
                mPipes[i].destroy();
            } else {
                logger.warn("Disconnecting null mPipe[" + i + "]");
            }
            mPipes[i] = null;
        }
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
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from SpywareSettings ss where ss.tid = :tid");
            q.setParameter("tid", getTid());
            this.spySettings = (SpywareSettings)q.uniqueResult();

            updateActiveX(this.spySettings);
            updateCookie(this.spySettings);
            updateSubnet(this.spySettings);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        reconfigure();
    }

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

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                Matcher matcher = ACTIVEX_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String clsid = matcher.group(1);

                    if (!ruleHash.contains(clsid)) {
                        logger.debug("ADDING activeX Rule: " + clsid);
                        rules.add(new StringRule(clsid));
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

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                if (!ruleHash.contains(line)) {
                    logger.debug("ADDING cookie Rule: " + line);
                    rules.add(new StringRule(line));
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
            ruleHash.add(rule.getIpMaddr());
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                StringTokenizer tok = new StringTokenizer(line, ":");

                String addr = tok.nextToken();
                String description = tok.nextToken();
                String name = tok.nextToken();

                if (!ruleHash.contains(IPMaddr.parse(addr))) {
                    logger.debug("ADDING subnet Rule: " + addr);
                    rules.add(new IPMaddrRule(IPMaddr.parse(addr), name, "[no category]", description));
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
