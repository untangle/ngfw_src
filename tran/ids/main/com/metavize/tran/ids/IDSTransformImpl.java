/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareSettings.java 3501 2005-11-21 10:12:33Z amread $
 */

package com.metavize.tran.ids;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.logging.SimpleEventFilter;
import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tapi.AbstractTransform;
import com.metavize.mvvm.tapi.Affinity;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.PipeSpec;
import com.metavize.mvvm.tapi.SoloPipeSpec;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.TransformStartException;
import com.metavize.mvvm.util.TransactionWork;
import com.metavize.tran.token.TokenAdaptor;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class IDSTransformImpl extends AbstractTransform implements IDSTransform {
    private static final Logger logger = Logger.getLogger(IDSTransformImpl.class);

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

        eventLogger = new EventLogger<IDSLogEvent>(getTransformContext());

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
        return this.settings;
    }

    public void setIDSSettings(final IDSSettings settings) {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.saveOrUpdate(settings);
                    IDSTransformImpl.this.settings = settings;
                    return true;
                }

                public Object getResult() { return null; }
            };
        getTransformContext().runTransaction(tw);
    }

    public EventManager<IDSLogEvent> getEventManager()
    {
        return eventLogger;
    }

    protected void initializeSettings() {

        logger.info("Loading Rules...");
        IDSSettings settings = new IDSSettings(getTid());
        settings.setVariables(IDSRuleManager.defaultVariables);
        settings.setImmutableVariables(IDSRuleManager.immutableVariables);

        logger.info("Settings was null, loading from file");
        IDSRuleManager manager = new IDSRuleManager(engine); // A fake one for now.  XXX
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

    protected void postInit(String args[]) {
        logger.info("Post init");
        queryDBForSettings();
    }

    protected void preStart() throws TransformStartException {
        logger.info("Pre Start");
        if (DO_TEST) {
            logger.error("Running test...");
            IDSTest test = new IDSTest();
            if(!test.runTest())
                throw new TransformStartException("IDS Test failed"); // */
        }

        try {
            reconfigure();
        }
        catch (Exception e) {
            throw new TransformStartException(e);
        }

        eventLogger.start();
        statisticManager.start();
    }

    public IDSDetectionEngine getEngine() {
        return engine;
    }

    protected void postStop() {
        statisticManager.stop();
        eventLogger.stop();
        engine.stop();
    }

    public void reconfigure() throws TransformException {
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
