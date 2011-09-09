/*
 * $Id$
 */
package com.untangle.node.ips;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.node.util.PartialListUtil.Handler;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;


public class IpsNodeImpl extends AbstractNode implements IpsNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private final EventLogger<IpsLogEvent> eventLogger;

    private IpsSettings settings = null;
    final IpsStatisticManager statisticManager;

    private final EventHandler handler;
    private final SoloPipeSpec octetPipeSpec, httpPipeSpec;
    private final PipeSpec[] pipeSpecs;

    private IpsDetectionEngine engine;

    private final PartialListUtil listUtil = new PartialListUtil();
    
    private final IpsVariableHandler variableHandler = new IpsVariableHandler();
    private final IpsRuleHandler ruleHandler = new IpsRuleHandler();

    private final BlingBlinger scanBlinger;
    private final BlingBlinger detectBlinger;
    private final BlingBlinger blockBlinger;

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


        MessageManager lmm = LocalUvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        scanBlinger = c.addActivity("scan", I18nUtil.marktr("Sessions scanned"), null, I18nUtil.marktr("SCAN"));
        detectBlinger = c.addActivity("detect", I18nUtil.marktr("Sessions logged"), null, I18nUtil.marktr("LOG"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Sessions blocked"), null, I18nUtil.marktr("BLOCK"));
        lmm.setActiveMetricsIfNotSet(getNodeId(), scanBlinger, detectBlinger, blockBlinger);
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
        TransactionWork<Object> tw = new TransactionWork<Object>() {
            public boolean doWork(Session s) {
                settings.setBaseSettings(baseSettings);
                settings = (IpsSettings)s.merge(settings);
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
//	eventLogger.log(new IpsLogEvent(null, 0, "test", "test", true));
        return eventLogger;
    }

    public void initializeSettings()
    {
        logger.info("Loading Variables...");
        IpsSettings settings = new IpsSettings(getNodeId());
        settings.setVariables(IpsRuleManager.getDefaultVariables());
        settings.setImmutableVariables(IpsRuleManager.getImmutableVariables());

        logger.info("Loading Rules...");
        IpsRuleManager manager = new IpsRuleManager(this); // A fake one for now.  XXX
        Set<IpsRule> ruleSet = FileLoader.loadAllRuleFiles(manager);

        settings.getBaseSettings().setMaxChunks(engine.getMaxChunks());
        settings.setRules(ruleSet);

        setIpsSettings(settings);
        logger.info(ruleSet.size() + " rules loaded");

        statisticManager.stop();
    }

    public IpsDetectionEngine getEngine() {
        return engine;
    }

    @SuppressWarnings("unchecked") //getItems
    public List<IpsRule> getRules(final int start, final int limit, final String... sortColumns)
    {
        List<IpsRule> rules = listUtil.getItems("select s.rules from IpsSettings s where s.nodeId = :tid ",
                                 getNodeContext(), getNodeId(), start, limit,
                                 sortColumns);
        if (rules == null) {
            logger.warn("No rules found in database");
            rules = new LinkedList<IpsRule>();
        } else {
            for(IpsRule rule : rules) {
                engine.addRule(rule);
            }
        }
        
        return rules;
    }

    public void updateRules(List<IpsRule> added, List<Long> deleted, List<IpsRule> modified)
    {
        updateRules(settings.getRules(), added, deleted, modified);
    }

    @SuppressWarnings("unchecked") //getItems
    public List<IpsVariable> getVariables(final int start, final int limit, final String... sortColumns)
    {
        return listUtil.getItems("select s.variables from IpsSettings s where s.nodeId = :tid ",
                                 getNodeContext(), getNodeId(), start, limit,
                                 sortColumns);
    }

    public void updateVariables(List<IpsVariable> added, List<Long> deleted, List<IpsVariable> modified)
    {
	updateVariables(settings.getVariables(), added, deleted, modified);
    }

    @SuppressWarnings("unchecked") //getItems
    public List<IpsVariable> getImmutableVariables(final int start, final int limit, final String... sortColumns)
    {
        return listUtil.getItems("select s.immutableVariables from IpsSettings s where s.nodeId = :tid ",
                                 getNodeContext(), getNodeId(), start, limit,
                                 sortColumns);
    }

    public void updateImmutableVariables(List<IpsVariable> added, List<Long> deleted, List<IpsVariable> modified)
    {
	updateVariables(settings.getImmutableVariables(), added, deleted, modified);
    }
    
    public void updateAll(final IpsBaseSettings baseSettings, final List<IpsRule>[] rules, final List<IpsVariable>[] variables, final List<IpsVariable>[] immutableVariables)
    	{

		TransactionWork<Object> tw = new TransactionWork<Object>() {
			public boolean doWork(Session s) {
				if (baseSettings != null) {
					settings.setBaseSettings(baseSettings);
				}

				listUtil.updateCachedItems(settings.getRules(), ruleHandler, rules);
				listUtil.updateCachedItems(settings.getVariables(), variableHandler, variables);
				listUtil.updateCachedItems(settings.getVariables(), variableHandler, immutableVariables);

				settings = (IpsSettings)s.merge(settings);

				return true;
			}

			public Object getResult() {
				return null;
			}
		};
		getNodeContext().runTransaction(tw);

		reconfigure();
	}
    
    
    // protected methods -------------------------------------------------------

    protected void postStop()
    {
        statisticManager.stop();
        engine.stop();
    }

    protected void preStart() throws Exception
    {
        logger.info("Pre Start");

        statisticManager.start();
    }

    protected void postInit(String args[]) throws Exception
    {
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

    private void updateRules(final Set<?> rules, final List<?> added, final List<Long> deleted, final List<?> modified)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    listUtil.updateCachedItems(rules, added, deleted, modified);

                    settings = (IpsSettings)s.merge(settings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }
    
    @SuppressWarnings("unchecked")
    private void updateVariables(final Set rules, final List added, final List<Long> deleted, final List modified)
    {
	TransactionWork<Object> tw = new TransactionWork<Object>()
	{
	    public boolean doWork(Session s)
	    {
	    	listUtil.updateCachedItems(rules, variableHandler, added, deleted, modified);

	    	settings = (IpsSettings)s.merge(settings);

	    	return true;
	    }

	    public Object getResult() { return null; }
	};
	getNodeContext().runTransaction(tw);
    }
    
    private void setIpsSettings(final IpsSettings settings)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
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
        Set<IpsRule> rules = settings.getRules();
        engine.clearRules();
        for(IpsRule rule : rules) {
            engine.addRule(rule);
        }
    }

    private void queryDBForSettings()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from IpsSettings ips where ips.nodeId = :tid");
                    q.setParameter("nodeId", getNodeId());
                    IpsNodeImpl.this.settings = (IpsSettings)q.uniqueResult();
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    /* Utility handler for the most common case (rules) */
    public static class IpsRuleHandler implements Handler<IpsRule>
    {
        public Long getId( IpsRule rule )
        {
            return rule.getId();
        }

        public void update( IpsRule current, IpsRule newRule )
        {
            current.update( newRule );
        }
    }

    private static class IpsVariableHandler implements PartialListUtil.Handler<IpsVariable>
    {
        public Long getId( IpsVariable var )
        {
            return var.getId();
        }

        public void update( IpsVariable current, IpsVariable newVar )
        {
            current.updateVariable( newVar );
        }
    }

    public void incrementScanCount() 
    {
	scanBlinger.increment();
    }

    public void incrementDetectCount() 
    {
	detectBlinger.increment();
    }

    public void incrementBlockCount() 
    {
	blockBlinger.increment();
    }
}

