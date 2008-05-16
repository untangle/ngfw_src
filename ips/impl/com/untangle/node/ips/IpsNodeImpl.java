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

package com.untangle.node.ips;

import java.util.List;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.NodeStartException;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

public class IpsNodeImpl extends AbstractNode implements IpsNode {
    private final Logger logger = Logger.getLogger(getClass());

    private final EventLogger<IpsLogEvent> eventLogger;

    private IpsSettings settings = null;
    final IpsStatisticManager statisticManager;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private IpsDetectionEngine engine;

    public IpsNodeImpl() {
        engine = new IpsDetectionEngine(this);
        handler = new EventHandler(this);
        statisticManager = new IpsStatisticManager(getNodeContext());
        // Put the octet stream close to the server so that it is after the http processing.
        octetPipeSpec = new SoloPipeSpec("ips-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ips-http", this, new TokenAdaptor(this, new IpsHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };

        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter<IpsLogEvent> ef = new IpsLogFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new IpsBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        List<RuleClassification> classifications = FileLoader.loadClassifications();
        engine.setClassifications(classifications);
    }

    @Override
    protected PipeSpec[] getPipeSpecs() {
        logger.debug("Getting PipeSpec");
        return pipeSpecs;
    }

    public IpsBaseSettings getBaseSettings()
    {
        return settings.getBaseSettings();
    }

    public void setBaseSettings(final IpsBaseSettings baseSettings)
    {
        TransactionWork tw = new TransactionWork() {
            public boolean doWork(Session s) {
                settings.setBaseSettings(baseSettings);
                s.merge(settings);
                return true;
            }

            public Object getResult() {
                return null;
            }
        };
        getNodeContext().runTransaction(tw);
    }

    public EventManager<IpsLogEvent> getEventManager()
    {
        return eventLogger;
    }

    public void initializeSettings()
    {
        logger.info("Loading Variables...");
        IpsSettings settings = new IpsSettings(getTid());
        settings.setVariables(IpsRuleManager.getDefaultVariables());
        settings.setImmutableVariables(IpsRuleManager.getImmutableVariables());

        logger.info("Loading Rules...");
        IpsRuleManager manager = new IpsRuleManager(this); // A fake one for now.  XXX
        List<IpsRule> ruleList = FileLoader.loadAllRuleFiles(manager);

        settings.getBaseSettings().setMaxChunks(engine.getMaxChunks());
        settings.setRules(ruleList);

        setIpsSettings(settings);
        logger.info(ruleList.size() + " rules loaded");

        statisticManager.stop();
    }

    public IpsDetectionEngine getEngine() {
        return engine;
    }

    // protected methods -------------------------------------------------------

    protected void postStop() {
        statisticManager.stop();
        engine.stop();
    }

    protected void preStart() throws NodeStartException {
        logger.info("Pre Start");

        statisticManager.start();
    }

    protected void postInit(String args[]) throws NodeException {
        logger.info("Post init");
        queryDBForSettings();

        // Upgrade to 3.2 will have nuked the settings.  Recreate them
        if (IpsNodeImpl.this.settings == null) {
            logger.warn("No settings found.  Creating anew.");
            initializeSettings();
        }

        reconfigure();
    }

    // package protected methods -----------------------------------------------

    void log(IpsLogEvent ile)
    {
        eventLogger.log(ile);
    }

    // private methods ---------------------------------------------------------

    private void setIpsSettings(final IpsSettings settings)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    IpsNodeImpl.this.settings = (IpsSettings)s.merge(settings);
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    private void reconfigure()
    {
        engine.setSettings(settings);
        engine.onReconfigure();
        engine.setMaxChunks(settings.getBaseSettings().getMaxChunks());
        List<IpsRule> rules = (List<IpsRule>) settings.getRules();
        engine.clearRules();
        for(IpsRule rule : rules) {
            engine.addRule(rule);
        }
    }

    private void queryDBForSettings() {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from IpsSettings ips where ips.tid = :tid");
                    q.setParameter("tid", getTid());
                    IpsNodeImpl.this.settings = (IpsSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }
}
