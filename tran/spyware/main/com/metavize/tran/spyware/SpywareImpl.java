/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareImpl.java,v 1.11 2005/02/09 20:38:31 jdi Exp $
 */

package com.metavize.tran.spyware;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
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
        { new PipeSpec("spyware-http", Fitting.HTTP_TOKENS,
                       new Subscription(Protocol.TCP), Affinity.END),
          new PipeSpec("spyware-byte", Fitting.OCTET_STREAM,
                       new Subscription(Protocol.TCP), Affinity.END) };
    private final MPipe[] mPipes = new MPipe[2];
    private final SessionEventListener[] listeners = new SessionEventListener[]
        { tokenAdaptor, streamHandler };

    private SpywareSettings settings;

    // constructors -----------------------------------------------------------

    public SpywareImpl() { }

    // SpywareTransform methods -----------------------------------------------

    public SpywareSettings getSpywareSettings()
    {
        return settings;
    }

    public void setSpywareSettings(SpywareSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

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
        if (settings.getSpywareEnabled()) {
            streamHandler.subnetList(settings.getSubnetRules());
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
            MvvmContextFactory.context().pipelineFoundry().deregisterMPipe(mPipes[i]);
            mPipes[i] = null;
        }
    }


    protected void initializeSettings()
    {
        SpywareSettings settings = new SpywareSettings(getTid());
        List s = initActiveX();
        settings.setActiveXRules(s);
        s = initCookie();
        settings.setCookieRules(s);
        s = initSubnet();
        settings.setSubnetRules(s);

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
            settings = (SpywareSettings)q.uniqueResult();

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

    private List initActiveX()
    {
        List l = new ArrayList();

        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(ACTIVEX_LIST);
        if (null == is) {
            logger.warn("could not find: " + ACTIVEX_LIST);
            return l;
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                Matcher matcher = ACTIVEX_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String clsid = matcher.group(1);
                    logger.debug("added clsid: " + clsid);
                    l.add(new StringRule(clsid));
                }
            }
        } catch (IOException exn) {
            logger.warn("could not read file", exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("could not close file", exn);
            }
        }

        return l;
    }

    private List initCookie()
    {
        List l = new ArrayList();

        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(COOKIE_LIST);
        if (null == is) {
            logger.warn("could not find: " + COOKIE_LIST);
            return l;
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                l.add(new StringRule(line));
            }
        } catch (IOException exn) {
            logger.warn("could not read file", exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("could not close file", exn);
            }
        }

        return l;
    }

    private List initSubnet()
    {
        List l = new ArrayList();

        InputStream is = getClass().getClassLoader()
            .getResourceAsStream(SUBNET_LIST);
        if (null == is) {
            logger.warn("could not find: " + SUBNET_LIST);
            return l;
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                StringTokenizer tok = new StringTokenizer(line, ":");

                String addr = tok.nextToken();
                String description = tok.nextToken();
                String name = tok.nextToken();

                l.add(new IPMaddrRule(IPMaddr.parse(addr), name, description));
            }
        } catch (IOException exn) {
            logger.warn("could not read file", exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("could not close file", exn);
            }
        }

        return l;
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
