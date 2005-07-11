/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.test;

import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tapi.Subscription;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.Interface;
import com.metavize.mvvm.tran.PortRange;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class TestTransformImpl extends AbstractTransform
    implements TestTransform
{
    private final Logger logger = Logger.getLogger(TestTransformImpl.class);

    private EventHandler handler;
    private SoloPipeSpec pipeSpec;
    private SoloPipeSpec[] pipeSpecs;

    private Subscription tcpSub;
    private Subscription udpSub;

    private boolean noTCP = false;
    private boolean noUDP = false;

    private int minPort = 1;
    private int maxPort = 65535;

    private TestSettings settings;

    // constructor ------------------------------------------------------------

    public TestTransformImpl() { }

    protected void initializeSettings()
    {
        TestSettings settings = new TestSettings(this.getTid());
        logger.info("Initializing Settings...");

        setTestSettings(settings);
    }

    // TestTransform methods --------------------------------------------------

    public void setTestSettings(TestSettings settings)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            s.saveOrUpdateCopy(settings);
            this.settings = settings;

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn(exn); // XXX TransExn
            }
        }

        reconfigure();
    }

    public TestSettings getTestSettings()
    {
        return settings;
    }

    // AbstractTransform methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    protected void postInit(String[] args)
    {
        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            Query q = s.createQuery
                ("from TestSettings ts where ts.tid = :tid");
            q.setParameter("tid", getTid());


            settings = (TestSettings)q.uniqueResult();
            parseArgs(args);

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn(exn);
        } finally {
            try {
                if (null != s) {
                    s.close();
                }
            } catch (HibernateException exn) {
                logger.warn(exn);
            }
        }
    }

    protected void preStart()
    {
        pipeSpec = new SoloPipeSpec
            ("test", this, handler, Fitting.OCTET_STREAM, Affinity.SERVER, 0);
        pipeSpecs = new SoloPipeSpec[] { pipeSpec };

        reconfigure();

        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }

        this.handler = new EventHandler(settings);
    }

    public void reconfigure()
    {
        if (null == pipeSpec) { return; }

        if (noTCP && (tcpSub != null)) {
            logger.debug("Removing TCP Sub");
            pipeSpec.removeSubscription(tcpSub);
            tcpSub = null;
        }

        if (noUDP && (udpSub != null)) {
            logger.debug("Removing UDP Sub");
            pipeSpec.removeSubscription(udpSub);
            udpSub = null;
        }

        if (!this.noTCP && (this.tcpSub == null)) {
            logger.debug("Adding TCP Sub");
            tcpSub = new Subscription
                (Protocol.TCP, Interface.ANY, Interface.ANY,
                 IPMaddr.anyAddr, PortRange.ANY, IPMaddr.anyAddr,
                 new PortRange(minPort, maxPort));
            pipeSpec.addSubscription(tcpSub);
        }

        if (!noUDP && (udpSub == null)) {
            logger.debug("Adding UDP Sub");
            udpSub = new Subscription
                (Protocol.UDP, Interface.ANY, Interface.ANY,
                 IPMaddr.anyAddr, PortRange.ANY, IPMaddr.anyAddr,
                 new PortRange(minPort,maxPort));
            pipeSpec.addSubscription(udpSub);
        }
    }

    // private methods -------------------------------------------------------

    private void parseArgs(String[] args)
    {
        boolean cleared = false;

        if (args.length > 0) {
            for (int i=0; i<args.length; i++) {
                if ("--release".equals(args[i])) {
                    if (!cleared) { clearModes(settings); cleared = true; }
                    settings.setRelease(true);
                } else if ("--normal".equals(args[i])) {
                    if (!cleared) { clearModes(settings); cleared = true; }
                    settings.setNormal(true);
                }
                /* else if ("--double-endpoint".equals(args[i])) {
                   if (!cleared) { clearModes(settings); cleared = true; }
                   settings.setDoubleEnded(true);
                   } */
                else if ("--buffered".equals(args[i])) {
                    if (!cleared) { clearModes(settings); cleared = true; }
                    settings.setBuffered(true);
                } else if ("--no-tcp".equals(args[i])) {
                    noTCP = true;
                } else if ("--no-udp".equals(args[i])) {
                    noUDP = true;
                } else if ("--only-port".equals(args[i])) {
                    i++;
                    try {
                        this.minPort = (Integer.decode(args[i])).intValue();
                        this.maxPort = this.minPort;
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid Port Argument: "
                                           + args[i] + " : " + e);
                    }
                } else if ("--quiet".equals(args[i]) || "-q".equals(args[i])) {
                    settings.setQuiet(true);
                } else {
                    logger.debug("Unknown argument: " + args[i]);
                }
            }
        }
    }

    private static void clearModes(TestSettings ts)
    {
        ts.resetSettings();
    }

    // XXX soon to be deprecated ----------------------------------------------

    public Object getSettings()
    {
        return getTestSettings();
    }

    public void setSettings(Object settings)
    {
        setTestSettings((TestSettings)settings);
    }
}
