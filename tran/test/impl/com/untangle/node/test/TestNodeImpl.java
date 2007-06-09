/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.test;

import com.untangle.uvm.tapi.AbstractNode;
import com.untangle.uvm.tapi.Affinity;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.PipeSpec;
import com.untangle.uvm.tapi.Protocol;
import com.untangle.uvm.tapi.SoloPipeSpec;
import com.untangle.uvm.tapi.Subscription;
import com.untangle.uvm.node.IPMaddr;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class TestNodeImpl extends AbstractNode
    implements TestNode
{
    private final Logger logger = Logger.getLogger(TestNodeImpl.class);

    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private Subscription tcpSub;
    private Subscription udpSub;

    private boolean noTCP = false;
    private boolean noUDP = false;

    private int minPort = 1;
    private int maxPort = 65535;

    private TestSettings settings;

    // constructor ------------------------------------------------------------

    public TestNodeImpl()
    {
        this.handler = new EventHandler(this, new TestSettings());
        pipeSpec = new SoloPipeSpec
            ("test", this, handler, Fitting.OCTET_STREAM,
             Affinity.SERVER, 0);
        pipeSpecs = new SoloPipeSpec[] { pipeSpec };
    }

    public void initializeSettings()
    {
        TestSettings settings = new TestSettings(this.getTid());
        logger.info("Initializing Settings...");

        setTestSettings(settings);
    }

    // TestNode methods --------------------------------------------------

    public void setTestSettings(final TestSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    TestNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    public TestSettings getTestSettings()
    {
        return settings;
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    // lifecycle --------------------------------------------------------------

    protected void postInit(final String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from TestSettings ts where ts.tid = :tid");
                    q.setParameter("tid", getTid());

                    settings = (TestSettings)q.uniqueResult();
                    parseArgs(args);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    protected void preStart()
    {
        if (this.settings == null) {
            String[] args = {""};
            postInit(args);
        }
        reconfigure();
    }

    private void reconfigure()
    {
        handler.setSettings(settings);

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
                (Protocol.TCP, true, true,
                 IPMaddr.anyAddr, PortRange.ANY, IPMaddr.anyAddr,
                 new PortRange(minPort, maxPort));
            pipeSpec.addSubscription(tcpSub);
        }

        if (!noUDP && (udpSub == null)) {
            logger.debug("Adding UDP Sub");
            udpSub = new Subscription
                (Protocol.UDP, true, true,
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
