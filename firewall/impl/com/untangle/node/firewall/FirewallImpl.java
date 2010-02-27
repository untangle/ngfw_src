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
package com.untangle.node.firewall;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.localapi.SessionMatcherFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class FirewallImpl extends AbstractNode implements Firewall
{
    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final EventLogger<FirewallEvent> eventLogger;

    private final Logger logger = Logger.getLogger(FirewallImpl.class);

    private FirewallSettings settings = null;
    final FirewallStatisticManager statisticManager;

    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;
    private final BlingBlinger loggedBlinger;

    public FirewallImpl()
    {
        this.handler = new EventHandler(this);
        this.statisticManager = new FirewallStatisticManager(getNodeContext());

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec("firewall", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 3);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter ef = new FirewallAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new FirewallBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        LocalMessageManager lmm = LocalUvmContextFactory.context().localMessageManager();
        Counters c = lmm.getCounters(getTid());
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Sessions passed"), null, I18nUtil.marktr("PASS"));
        loggedBlinger = c.addActivity("log", I18nUtil.marktr("Sessions logged"), null, I18nUtil.marktr("LOG"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Sessions blocked"), null, I18nUtil.marktr("BLOCK"));
        lmm.setActiveMetricsIfNotSet(getTid(), blockBlinger, passBlinger, loggedBlinger);
    }

    // Firewall methods --------------------------------------------------------

    public EventManager<FirewallEvent> getEventManager()
    {
        return eventLogger;
    }

    public FirewallBaseSettings getBaseSettings()
    {
        return settings.getBaseSettings();
    }

    public void setBaseSettings(final FirewallBaseSettings baseSettings)
    {
        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                settings.setBaseSettings(baseSettings);
                settings = (FirewallSettings)s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);

        shutdownMatchingSessions();
    }

    public List<FirewallRule> getFirewallRuleList()
    {
        return settings.getFirewallRuleList();
    }

    public void setFirewallRuleList(final List<FirewallRule> rules)
    {
        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                settings.setFirewallRuleList(rules);
                settings = (FirewallSettings)s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);

        shutdownMatchingSessions();
    }
    
    public void updateAll(final FirewallBaseSettings baseSettings,
    		final List<FirewallRule> rules)
    {
        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                settings.setBaseSettings(baseSettings);
                settings.setFirewallRuleList(rules);
                settings = (FirewallSettings)s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
        handler.configure(settings);

        shutdownMatchingSessions();
    }

    public Validator getValidator() {
        return new FirewallValidator();
    }

    // AbstractNode methods ----------------------------------------------------

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        FirewallSettings settings = getDefaultSettings();

        setFirewallSettings(settings);

        statisticManager.stop();
    }

    // protected methods -------------------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    /* Kill all sessions when starting or stopping this node */
    protected SessionMatcher sessionMatcher()
    {
        return SessionMatcherFactory.makePolicyInstance(getPolicy());
    }

    protected void preStart() throws NodeStartException
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new NodeStartException(e);
        }

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();
    }

    protected void postStop()
    {
        /* Kill all active sessions */
        shutdownMatchingSessions();

        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from FirewallSettings hbs where hbs.tid = :tid");
                    q.setParameter("tid", getTid());
                    FirewallImpl.this.settings = (FirewallSettings)q.uniqueResult();

                    updateToCurrent(FirewallImpl.this.settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    // package protected methods -----------------------------------------------

    void log(FirewallEvent logEvent)
    {
        eventLogger.log(logEvent);
    }

    FirewallSettings getDefaultSettings()
    {
        logger.info("Loading the default settings");
        FirewallSettings settings = new FirewallSettings(this.getTid());

        try {
            IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
            PortMatcherFactory pmf = PortMatcherFactory.getInstance();
            ProtocolMatcherFactory prmf = ProtocolMatcherFactory.getInstance();


            /* A few sample settings */
            settings.getBaseSettings().setQuickExit(true);
            settings.getBaseSettings().setRejectSilently(true);
            settings.getBaseSettings().setDefaultAccept(true);

            List<FirewallRule> firewallList = new LinkedList<FirewallRule>();

            IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
            IntfDBMatcher any = imf.getAllMatcher();
            IntfDBMatcher external = imf.getLessTrustedMatcher();
            IntfDBMatcher internal = imf.getMoreTrustedMatcher();

            FirewallRule tmp = new FirewallRule(false,
                                                prmf.getTCPAndUDPMatcher(),
                                                any, internal,
                                                ipmf.getAllMatcher(),
                                                ipmf.getAllMatcher(),
                                                pmf.getAllMatcher(),
                                                pmf.makeSingleMatcher(21),
                                                true);
            tmp.setLog(true);
            tmp.setDescription("Block and log all incoming traffic destined to port 21 (FTP)");
            firewallList.add(tmp);

            /* Block all traffic TCP traffic from the network 1.2.3.4/255.255.255.0 */
            tmp = new FirewallRule(false, prmf.getTCPMatcher(),
                                   any, external,
                                   IPMatcherFactory.parse("1.2.3.0/255.255.255.0"),
                                   ipmf.getAllMatcher(),
                                   pmf.getAllMatcher(), pmf.getAllMatcher(),
                                   true);
            tmp.setDescription("Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0");
            firewallList.add(tmp);

            tmp = new FirewallRule(false, prmf.getTCPAndUDPMatcher(),
                                   any, any,
                                   ipmf.getAllMatcher(),
                                   IPMatcherFactory.parse("1.2.3.1 - 1.2.3.10"),
                                   pmf.makeRangeMatcher(1000, 5000),
                                   pmf.getAllMatcher(),
                                   false);
            tmp.setLog(true);
            tmp.setDescription("Accept and log all traffic to the range 1.2.3.1 - 1.2.3.10 from ports 1000-5000");
            firewallList.add(tmp);

            for (Iterator<FirewallRule> iter = firewallList.iterator() ; iter.hasNext() ;) {
                iter.next().setCategory("[Sample]");
            }

            settings.setFirewallRuleList(firewallList);

        } catch (Exception e) {
            logger.error("This should never happen", e);
        }

        return settings;
    }

    // private methods ---------------------------------------------------------

    private void reconfigure() throws NodeException
    {
        logger.info("Reconfigure()");

        if (settings == null) {
            throw new NodeException("Failed to get Firewall settings: " + settings);
        }

        handler.configure(settings);
    }

    private void updateToCurrent(FirewallSettings settings)
    {
        if (settings == null) {
            logger.error("NULL Firewall Settings");
            return;
        }

        logger.info("Update Settings Complete");
    }

    private void setFirewallSettings(final FirewallSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    FirewallImpl.this.settings = (FirewallSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };

        getNodeContext().runTransaction(tw);
        
        try {
            reconfigure();
        } catch (NodeException exn) {
            logger.error("Could not save Firewall settings", exn);
        }
    }

    public void incrementBlockCount() 
    {
	blockBlinger.increment();
    }

    public void incrementPassCount() 
    {
	passBlinger.increment();
    }

    public void incrementLogCount() 
    {
	loggedBlinger.increment();
    }

}
