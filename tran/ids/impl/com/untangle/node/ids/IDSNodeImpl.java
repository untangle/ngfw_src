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

package com.untangle.node.ids;

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

public class IDSNodeImpl extends AbstractNode implements IDSNode {
    private final Logger logger = Logger.getLogger(getClass());

    private static final boolean DO_TEST = false;

    private final EventLogger<IDSLogEvent> eventLogger;

    private IDSSettings settings = null;
    final IDSStatisticManager statisticManager;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private IDSDetectionEngine engine;

    public IDSNodeImpl() {
        engine = new IDSDetectionEngine(this);
        handler = new EventHandler(this);
        statisticManager = new IDSStatisticManager(getNodeContext());
        // Put the octet stream close to the server so that it is after the http processing.
        octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(this, new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };

        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter<IDSLogEvent> ef = new IDSLogFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new IDSBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        List<RuleClassification> classifications = FileLoader.loadClassifications();
        engine.setClassifications(classifications);
    }

    @Override
    protected PipeSpec[] getPipeSpecs() {
        logger.debug("Getting PipeSpec");
        return pipeSpecs;
    }

    public IDSSettings getIDSSettings() {
        if( this.settings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return this.settings;
    }

    public void setIDSSettings(final IDSSettings settings) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    IDSNodeImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (NodeException exn) {
            logger.error("Could not save IDS settings", exn);
        }
    }

    public EventManager<IDSLogEvent> getEventManager()
    {
        return eventLogger;
    }

    public void initializeSettings() {

        logger.info("Loading Variables...");
        IDSSettings settings = new IDSSettings(getTid());
        settings.setVariables(IDSRuleManager.getDefaultVariables());
        settings.setImmutableVariables(IDSRuleManager.getImmutableVariables());

        logger.info("Loading Rules...");
        IDSRuleManager manager = new IDSRuleManager(this); // A fake one for now.  XXX
        List<IDSRule> ruleList = FileLoader.loadAllRuleFiles(manager);

        settings.setMaxChunks(engine.getMaxChunks());
        settings.setRules(ruleList);

        setIDSSettings(settings);
        logger.info(ruleList.size() + " rules loaded");

        statisticManager.stop();
    }

    private void queryDBForSettings() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from IDSSettings ids where ids.tid = :tid");
                    q.setParameter("tid", getTid());
                    IDSNodeImpl.this.settings = (IDSSettings)q.uniqueResult();
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
        if (IDSNodeImpl.this.settings == null) {
            logger.warn("No settings found.  Creating anew.");
            initializeSettings();
        }

        reconfigure();

    }

    protected void preStart() throws NodeStartException {
        logger.info("Pre Start");
        if (DO_TEST) {
            logger.error("Running test...");
            IDSTest test = new IDSTest();
            if(!test.runTest())
                throw new NodeStartException("IDS Test failed"); // */
        }

        statisticManager.start();
    }

    public IDSDetectionEngine getEngine() {
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
        List<IDSRule> rules = (List<IDSRule>) settings.getRules();
        for(IDSRule rule : rules) {
            engine.updateRule(rule);
        }
        if (logger.isDebugEnabled())
            engine.dumpRules();
        //remove all deleted rules XXXX
    }

    void log(IDSLogEvent ile)
    {
        eventLogger.log(ile);
    }

    //XXX soon to be deprecated ------------------------------------------

    public Object getSettings() { return getIDSSettings(); }

    public void setSettings(Object obj) { setIDSSettings((IDSSettings)obj); }
}
