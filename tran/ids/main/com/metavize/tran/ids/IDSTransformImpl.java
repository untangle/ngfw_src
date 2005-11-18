package com.metavize.tran.ids;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;

import com.metavize.mvvm.logging.EventFilter;
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
        octetPipeSpec = new SoloPipeSpec("ids-octet", this, handler,Fitting.OCTET_STREAM, Affinity.SERVER,10);
        httpPipeSpec = new SoloPipeSpec("ids-http", this, new TokenAdaptor(this, new IDSHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER,0);
        pipeSpecs = new PipeSpec[] { httpPipeSpec, octetPipeSpec };

        eventLogger = new EventLogger<IDSLogEvent>(getTransformContext());

        EventFilter<IDSLogEvent> ef = new IDSLogFilter();
        eventLogger.addEventFilter(ef);
        ef = new IDSBlockedFilter();
        eventLogger.addEventFilter(ef);
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
        List<IDSRule> ruleList = new ArrayList<IDSRule>();

        logger.info("Loading Rules...");
        IDSSettings settings = new IDSSettings(getTid());
        settings.setVariables(IDSRuleManager.defaultVariables);
        settings.setImmutableVariables(IDSRuleManager.immutableVariables);

        logger.info("Settings was null, loading from file");
        String path =  System.getProperty("bunnicula.home");
        File file = new File(path+"/idsrules");
        visitAllFiles(file, ruleList);

        settings.setMaxChunks(engine.getMaxChunks());
        settings.setRules(ruleList);

        setIDSSettings(settings);
        logger.info(ruleList.size() + " rules loaded");

        statisticManager.stop();
    }

    /** Temp subroutines for loading local snort rules.
     */
    private void visitAllFiles(File file, List<IDSRule> result) {
        if (file.isDirectory()) {
            String[] children = file.list();
            for (int i=0; i<children.length; i++)
                visitAllFiles(new File(file, children[i]), result);
        }
        else
            processFile(file, result);
    }

    /** Temp subroutines for loading local snort rules.
     */
    private void processFile(File file, List<IDSRule> result) {
        IDSRuleManager manager = new IDSRuleManager(engine);
        try {
            String category = file.getName().replaceAll(".rules",""); //Should move this to script land
            category = category.replace("bleeding-",""); //Should move this to script land
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                IDSRule rule = manager.createRule(str.trim(), category);
                if (rule != null)
                    result.add(rule);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        logger.error("Running test...");
        IDSTest test = new IDSTest();
        logger.info("Pre Start");
        if(!test.runTest())
          throw new TransformStartException("IDS Test failed"); // */

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
    }

    public void reconfigure() throws TransformException {
        engine.setSettings(settings);
        engine.onReconfigure();
        engine.setMaxChunks(settings.getMaxChunks());
        List<IDSRule> rules = (List<IDSRule>) settings.getRules();
        for(IDSRule rule : rules) {
            engine.updateRule(rule);
        }
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
