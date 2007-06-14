/*
 * $HeadURL:$
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

package com.untangle.node.ips;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.List;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.tapi.AbstractNode;
import com.untangle.uvm.tapi.Affinity;
import com.untangle.uvm.tapi.Fitting;
import com.untangle.uvm.tapi.PipeSpec;
import com.untangle.uvm.tapi.SoloPipeSpec;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.node.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class IPSNodeImpl extends AbstractNode implements IPSNode {
    private final Logger logger = Logger.getLogger(getClass());

    private static final boolean DO_TEST = false;

    private final EventLogger<IPSLogEvent> eventLogger;

    private IPSSettings settings = null;
    final IPSStatisticManager statisticManager;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private IPSDetectionEngine engine;

    public IPSNodeImpl() {
        engine = new IPSDetectionEngine(this);
        handler = new EventHandler(this);
        statisticManager = new IPSStatisticManager(getNodeContext());
        // Put the octet stream close to the server so that it is after the http processing.
        octetPipeSpec = new SoloPipeSpec("ips-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ips-http", this, new TokenAdaptor(this, new IPSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };

        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter<IPSLogEvent> ef = new IPSLogFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new IPSBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        List<RuleClassification> classifications = FileLoader.loadClassifications();
        engine.setClassifications(classifications);
    }

    @Override
    protected PipeSpec[] getPipeSpecs() {
        logger.debug("Getting PipeSpec");
        return pipeSpecs;
    }

    public IPSSettings getIPSSettings() {
        if( this.settings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return this.settings;
    }

    public void setIPSSettings(final IPSSettings settings) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    IPSNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (NodeException exn) {
            logger.error("Could not save IPS settings", exn);
        }
    }

    public EventManager<IPSLogEvent> getEventManager()
    {
        return eventLogger;
    }

    public void initializeSettings() {

        logger.info("Loading Variables...");
        IPSSettings settings = new IPSSettings(getTid());
        settings.setVariables(IPSRuleManager.getDefaultVariables());
        settings.setImmutableVariables(IPSRuleManager.getImmutableVariables());

        logger.info("Loading Rules...");
        IPSRuleManager manager = new IPSRuleManager(this); // A fake one for now.  XXX
        List<IPSRule> ruleList = FileLoader.loadAllRuleFiles(manager);

        settings.setMaxChunks(engine.getMaxChunks());
        settings.setRules(ruleList);

        setIPSSettings(settings);
        logger.info(ruleList.size() + " rules loaded");

        statisticManager.stop();
    }

    private void queryDBForSettings() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from IPSSettings ips where ips.tid = :tid");
                    q.setParameter("tid", getTid());
                    IPSNodeImpl.this.settings = (IPSSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    protected void postInit(String args[]) throws NodeException {
        logger.info("Post init");
        queryDBForSettings();

        // Upgrade to 3.2 will have nuked the settings.  Recreate them
        if (IPSNodeImpl.this.settings == null) {
            logger.warn("No settings found.  Creating anew.");
            initializeSettings();
        }

        reconfigure();

    }

    protected void preStart() throws NodeStartException {
        logger.info("Pre Start");
        if (DO_TEST) {
            logger.error("Running test...");
            IPSTest test = new IPSTest();
            if(!test.runTest())
                throw new NodeStartException("IPS Test failed"); // */
        }

        statisticManager.start();
    }

    public IPSDetectionEngine getEngine() {
        return engine;
    }

    protected void postStop() {
        statisticManager.stop();
        engine.stop();
    }

    private void reconfigure() throws NodeException {
        engine.setSettings(settings);
        engine.onReconfigure();
        engine.setMaxChunks(settings.getMaxChunks());
        List<IPSRule> rules = (List<IPSRule>) settings.getRules();
        for(IPSRule rule : rules) {
            engine.updateRule(rule);
        }
        if (logger.isDebugEnabled())
            engine.dumpRules();
        //remove all deleted rules XXXX
    }

    void log(IPSLogEvent ile)
    {
        eventLogger.log(ile);
    }

    //XXX soon to be deprecated ------------------------------------------

    public Object getSettings() { return getIPSSettings(); }

    public void setSettings(Object obj) { setIPSSettings((IPSSettings)obj); }
}
