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

package com.untangle.tran.ids;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.List;

import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.tapi.AbstractTransform;
import com.untangle.mvvm.tapi.Affinity;
import com.untangle.mvvm.tapi.Fitting;
import com.untangle.mvvm.tapi.PipeSpec;
import com.untangle.mvvm.tapi.SoloPipeSpec;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.TransformStartException;
import com.untangle.mvvm.util.TransactionWork;
import com.untangle.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class IDSTransformImpl extends AbstractTransform implements IDSTransform {
    private final Logger logger = Logger.getLogger(getClass());

    private static final boolean DO_TEST = false;

    private final EventLogger<IDSLogEvent> eventLogger;

    private IDSSettings settings = null;
    final IDSStatisticManager statisticManager;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private IDSDetectionEngine engine;

    public IDSTransformImpl() {
        engine = new IDSDetectionEngine(this);
        handler = new EventHandler(this);
        statisticManager = new IDSStatisticManager(getTransformContext());
        // Put the octet stream close to the server so that it is after the http processing.
        octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(this, new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };

        eventLogger = EventLoggerFactory.factory().getEventLogger(getTransformContext());

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
            logger.error("Settings not yet initialized. State: " + getTransformContext().getRunState() );
        return this.settings;
    }

    public void setIDSSettings(final IDSSettings settings) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    IDSTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);

        try {
            reconfigure();
        }
        catch (TransformException exn) {
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
                    IDSTransformImpl.this.settings = (IDSSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    protected void postInit(String args[]) throws TransformException {
        logger.info("Post init");
        queryDBForSettings();

        // Upgrade to 3.2 will have nuked the settings.  Recreate them
        if (IDSTransformImpl.this.settings == null) {
            logger.warn("No settings found.  Creating anew.");
            initializeSettings();
        }

        reconfigure();

    }

    protected void preStart() throws TransformStartException {
        logger.info("Pre Start");
        if (DO_TEST) {
            logger.error("Running test...");
            IDSTest test = new IDSTest();
            if(!test.runTest())
                throw new TransformStartException("IDS Test failed"); // */
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

    private void reconfigure() throws TransformException {
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
