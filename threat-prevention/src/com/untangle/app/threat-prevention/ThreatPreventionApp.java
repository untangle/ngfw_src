/**
 * $Id$
 */
package com.untangle.app.threat_prevention;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.app.webroot.WebrootQuery;
import com.untangle.app.webroot.WebrootDaemon;

import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.vnet.Subscription;


import com.untangle.uvm.HookCallback;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.SessionAttachments;
import com.untangle.uvm.vnet.Token;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.app.IntMatcher;

/** Threat Prevention application */
public class ThreatPreventionApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    public List<IPMaskedAddress> localNetworks = null;
    public WebrootQuery webrootQuery = null;

    public static final String ACTION_BLOCK = "block";
    public static final String ACTION_PASS = "pass";

    private static AtomicInteger AppCount = new AtomicInteger();
    private final ThreatPreventionDecisionEngine engine = new ThreatPreventionDecisionEngine(this);

    private final ThreatPreventionEventHandler otherHandler;
    private final PipelineConnector[] connectors;

    private static final String STAT_BLOCK = "block";
    private static final String STAT_FLAG = "flag";
    private static final String STAT_PASS = "pass";
    private static final String STAT_THREAT_NO_REPUTATION = "none";
    private static final String STAT_THREAT_HIGH_RISK = "high";
    private static final String STAT_THREAT_SUSPICIOUS = "suspicious";
    private static final String STAT_THREAT_MODERATE_RISK = "moderate";
    private static final String STAT_THREAT_LOW_RISK = "low";
    private static final String STAT_THREAT_TRUSTWORTHY = "trustworthy";
    private static final String STAT_LOOKUP_AVG = "lookup_avg";

    protected final ThreatPreventionReplacementGenerator replacementGenerator;
    
    private final Subscription httpsSub = new Subscription(Protocol.TCP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));

    public static final Map<Integer, Integer> UrlCatThreatMap;
    public static final Map<Integer, String> ReputationThreatMap;
    static {
        UrlCatThreatMap = new HashMap<>();
        UrlCatThreatMap.put(71, 1);         // Spam
        UrlCatThreatMap.put(67, 16);        // Botnets
        UrlCatThreatMap.put(57, 256);       // Phishing
        UrlCatThreatMap.put(58, 512);       // Proxy
        UrlCatThreatMap.put(49, 655362);    // Keyloggers
        UrlCatThreatMap.put(56, 131072);    // Malware
        UrlCatThreatMap.put(59, 262144);    // Spyware

        // !!! translate
        ReputationThreatMap = new HashMap<>();
        ReputationThreatMap.put(0, "No reputation");
        ReputationThreatMap.put(20, "High Risk");
        ReputationThreatMap.put(40, "Suspicious");
        ReputationThreatMap.put(60, "Moderate Risk");
        ReputationThreatMap.put(80, "Low Risk");
        ReputationThreatMap.put(100, "Trustworthy");
    }

    private ThreatPreventionSettings settings = null;

    /**
     * This is used to reset sessions that are blocked by threat prevention when they switch policy
     */
    private final SessionMatcher THREAT_PREVENTION_SESSION_MATCHER = new SessionMatcher() {
            /**
             * isMatch returns true if the session matches a block rule
             * @param policyId
             * @param protocol
             * @param clientIntf
             * @param serverIntf
             * @param clientAddr
             * @param serverAddr
             * @param clientPort
             * @param serverPort
             * @param attachments
             * @return true if the session should be reset
             */
            public boolean isMatch( Integer policyId, short protocol,
                                    int clientIntf, int serverIntf,
                                    InetAddress clientAddr, InetAddress serverAddr,
                                    int clientPort, int serverPort,
                                    SessionAttachments attachments )
            {
                // logger.warn(handler);
                // if (handler == null)
                //     return false;

                ThreatPreventionRule matchedRule = null;
                
                /**
                 * Find the matching rule compute block/log verdicts
                 */
                for (ThreatPreventionRule rule : settings.getRules()) {
                    if (rule.isMatch(protocol,
                                     clientIntf, serverIntf,
                                     clientAddr, serverAddr,
                                     clientPort, serverPort,
                                     attachments)) {
                        matchedRule = rule;
                        break;
                    }
                }
        
                if (matchedRule == null)
                    return false;

                logger.info("Threat Prevention Save Setting Matcher: " +
                            clientAddr.getHostAddress().toString() + ":" + clientPort + " -> " +
                            serverAddr.getHostAddress().toString() + ":" + serverPort +
                            " :: action:" + matchedRule.getAction());

                return matchedRule.getAction() == "pass";
            }
    };
    
    /**
     * IP Reputation App constructor
     * @param appSettings - the AppSettings
     * @param appProperties the AppProperties
     */
    public ThreatPreventionApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.replacementGenerator = buildReplacementGenerator();

        this.otherHandler = new ThreatPreventionEventHandler(this);
        // Calculate home networks as a uvm network function
        //  // Just pull context?  Would have to contentw with chnges, right?
        // this.homeNetworks = this.calculateHomeNetworks( UvmContextFactory.context().networkManager().getNetworkSettings());
        
        // getlocalNetworks
        // this.networkSettingsChangeHook = new IntrusionPreventionNetworkSettingsHook();
        //      this should just get local network list for us.
        localNetworks = UvmContextFactory.context().networkManager().getLocalNetworks();
        localNetworks.add(new IPMaskedAddress("192.168.0.0/16"));
        localNetworks.add(new IPMaskedAddress("172.16.0.0/12"));
        localNetworks.add(new IPMaskedAddress("10.0.0.0/8"));

        // this.handler = new ThreatPreventionEventHandler(this);

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new AppMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        this.addMetric(new AppMetric(STAT_THREAT_NO_REPUTATION, I18nUtil.marktr("Threat: None")));
        this.addMetric(new AppMetric(STAT_THREAT_HIGH_RISK, I18nUtil.marktr("Threat: High Risk")));
        this.addMetric(new AppMetric(STAT_THREAT_SUSPICIOUS, I18nUtil.marktr("Threat: Suspicious")));
        this.addMetric(new AppMetric(STAT_THREAT_MODERATE_RISK, I18nUtil.marktr("Threat: Moderate Risk")));
        this.addMetric(new AppMetric(STAT_THREAT_LOW_RISK, I18nUtil.marktr("Threat: Low Risk")));
        this.addMetric(new AppMetric(STAT_THREAT_TRUSTWORTHY, I18nUtil.marktr("Threat: Trustworthy Sessions")));
        this.addMetric(new AppMetric(STAT_LOOKUP_AVG, I18nUtil.marktr("Lookup time average"), 0L, AppMetric.Type.AVG_TIME, I18nUtil.marktr("ms"), true));

        // this.addMirroredMetrics( WebrootDaemon );
        // this.addMirroredMetrics( WebrootQuery );
        this.connectors = new PipelineConnector[] {
            UvmContextFactory.context().pipelineFoundry().create("threat-prevention-http", this, null, new ThreatPreventionHttpHandler(this), Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, -2000, true),
            UvmContextFactory.context().pipelineFoundry().create("threat-prevention-https-sni", this, httpsSub, new ThreatPreventionHttpsSniHandler(this), Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -2000, true),
            UvmContextFactory.context().pipelineFoundry().create("threat-prevention-other", this, null, otherHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -2000, false)
        };
    }

    /**
     * Called to get our decision engine instance
     * 
     * @return The decision engine
     */
    public ThreatPreventionDecisionEngine getDecisionEngine()
    {
        return engine;
    }

    /**
     * Get the current IP Reputation Settings
     * @return ThreatPreventionSettings
     */
    public ThreatPreventionSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the current IP Reputation settings
     * @param newSettings
     */
    public void setSettings(final ThreatPreventionSettings newSettings)
    {
        /**
         * set the new ID of each rule
         * We use 100,000 * appId as a starting point so rule IDs don't overlap with other threat prevention
         *
         * Also set flag to true if rule is blocked
         */
        int idx = this.getAppSettings().getPolicyId().intValue() * 100000;
        for (ThreatPreventionRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "threat-prevention/" + "settings_"  + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }

    /**
     * Get the current ruleset
     * @return the list
     */
    public List<ThreatPreventionRule> getRules()
    {
        if (getSettings() == null)
            return null;
        
        return getSettings().getRules();
    }

    /**
     * Set the current ruleset
     * @param rules - the new rules
     */
    public void setRules( List<ThreatPreventionRule> rules )
    {
        ThreatPreventionSettings set = getSettings();

        if (set == null) {
            logger.warn("NULL settings");
            return;
        }

        set.setRules(rules);
        setSettings(set);
    }

    /**
     * Increment the block stat
     */
    public void incrementBlockCount() 
    {
        this.incrementMetric(STAT_BLOCK);
    }

    /**
     * Increment the pass stat
     */
    public void incrementPassCount() 
    {
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Increment the flag stat
     */
    public void incrementFlagCount() 
    {
        this.incrementMetric(STAT_FLAG);
    }

    /**
     * Increment high risk stat counter.
     * @param reputation integer of Reputation.
     */
    public void incrementThreatCount(int reputation) 
    {
        if(reputation == 0){
            this.incrementMetric(STAT_THREAT_NO_REPUTATION);
        }else{
            reputation = reputation - (reputation % 20) + 20;
            switch(reputation){
                case 100:
                    this.incrementMetric(STAT_THREAT_TRUSTWORTHY);
                    break;
                case 80:
                    this.incrementMetric(STAT_THREAT_LOW_RISK);
                    break;
                case 60:
                    this.incrementMetric(STAT_THREAT_MODERATE_RISK);
                    break;
                case 40:
                    this.incrementMetric(STAT_THREAT_SUSPICIOUS);
                    break;
                case 20:
                    this.incrementMetric(STAT_THREAT_HIGH_RISK);
                    break;
            }
        }
    }

    /**
     * Add new time to total time counter.
     * @param time Long of time to add.
     */
    public void adjustLookupAverage(long time)
    {
        this.adjustMetric(STAT_LOOKUP_AVG, time);
    }

    /**
     * Return various local valus for use with reports.
     *
     * @param  key String of key in settings.
     * @param arguments Array of String arguments to pass.
     * @return     List of JSON objects for the settings.
     */
    public JSONArray getReportInfo(String key, String... arguments){
        JSONArray result = null;
        int index = 0;

        if(key.equals("localNetworks")){
            result = new JSONArray();
            try{
                for(IPMaskedAddress address : localNetworks){
                    JSONObject jo = new JSONObject(address);
                    jo.remove("class");
                    result.put(index++, jo);
                }
            }catch(Exception e){
                logger.warn("getReportnfo:", e);
            }
        }else if(key.equals("getUrlHistory")){
            return webrootQuery.getUrlHistory(arguments);
        }else if(key.equals("getIpHistory")){
            return webrootQuery.getIpHistory(arguments);
        }else if(key.equals("rules")){
            result = new JSONArray();
            try{
                for(ThreatPreventionRule rule : getSettings().getRules()){
                    JSONObject jo = new JSONObject(rule);
                    jo.remove("class");
                    result.put(index++, jo);
                }
            }catch(Exception e){
                logger.warn("getReportnfo:", e);
            }
        }

        return result;
    }

    /**
     * Generate a response
     * 
     * @param redirectDetails
     *        ThreatPreventionBlockDetails to build rediect.
     * @param session
     *        The session
     * @param uri
     *        The URI
     * @param header
     *        The header
     * @return The response token
     */
    public Token[] generateResponse(ThreatPreventionBlockDetails redirectDetails, AppTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateResponse(redirectDetails, session, uri, header);
    }

    /**
     * Get the upper bound of numeric threat from specified threat.
     * @param  reputation Integer of threat.
     * @return            The highest band of reputation that this threat belongs to.
     */
    String getThreatFromReputation(Integer reputation)
    {
        return ReputationThreatMap.get(reputation > 0 ? ( reputation - (reputation % 20) + 20 ) : 0);
    }

    /**
     * Build a replacement generator
     * 
     * @return The replacement generator
     */
    protected ThreatPreventionReplacementGenerator buildReplacementGenerator()
    {
        return new ThreatPreventionReplacementGenerator(getAppSettings());
    }

    /**
     * Get the block details for the argumented nonce
     * 
     * @param nonce
     *        The nonce to search
     * @return Block details
     */
    public ThreatPreventionBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    /**
     * Get the Pipeline connectors
     * @return the pipeline connectors array
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * preStart
     * @param isPermanentTransition
     */
    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        this.reconfigure();
        WebrootDaemon.getInstance().start();
        webrootQuery = WebrootQuery.getInstance();
    }

    /**
     * postStart()
     * @param isPermanentTransition
     */
    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        startServlet(logger);
        killAllSessions();
    }

    /**
     * preStop()
     * @param isPermanentTransition
     */
    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        webrootQuery = null;
        WebrootDaemon.getInstance().stop();
        stopServlet(logger);
    }

    /**
     * postStop()
     * @param isPermanentTransition
     */
    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        killAllSessions();
    }

    /**
     * postInit()
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        ThreatPreventionSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/threat-prevention/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( ThreatPreventionSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            setSettings(getDefaultSettings());
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    /**
     * Create new default settings
     * @return the default settings
     */
    private ThreatPreventionSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        /* A few sample settings */
        List<ThreatPreventionRule> ruleList = new LinkedList<>();
        LinkedList<ThreatPreventionRuleCondition> matcherList = null;
            
        // /* example rule 1 */
        // ThreatPreventionRuleCondition portMatch1 = new ThreatPreventionRuleCondition(ThreatPreventionRuleCondition.ConditionType.DST_PORT, "21");
        // matcherList = new LinkedList<>();
        // matcherList.add(portMatch1);
        // ruleList.add(new ThreatPreventionRule(false, matcherList, true, true, "Block and flag all traffic destined to port 21"));
                             
        // /* example rule 2 */
        // ThreatPreventionRuleCondition addrMatch2 = new ThreatPreventionRuleCondition(ThreatPreventionRuleCondition.ConditionType.SRC_ADDR, "1.2.3.4/255.255.255.0");
        // matcherList = new LinkedList<>();
        // matcherList.add(addrMatch2);
        // ruleList.add(new ThreatPreventionRule(false, matcherList, true, true, "Block and flag all TCP traffic from 1.2.3.0 netmask 255.255.255.0"));

        // /* example rule 3 */
        // ThreatPreventionRuleCondition addrMatch3 = new ThreatPreventionRuleCondition(ThreatPreventionRuleCondition.ConditionType.DST_ADDR, "1.2.3.4/255.255.255.0");
        // ThreatPreventionRuleCondition portMatch3 = new ThreatPreventionRuleCondition(ThreatPreventionRuleCondition.ConditionType.DST_PORT, "1000-5000");
        // matcherList = new LinkedList<>();
        // matcherList.add(addrMatch3);
        // matcherList.add(portMatch3);
        // ruleList.add(new ThreatPreventionRule(false, matcherList, true, false, "Accept and flag all traffic to the range 1.2.3.1 - 1.2.3.10 to ports 1000-5000"));

        ThreatPreventionSettings settings = new ThreatPreventionSettings(ruleList);
        settings.setVersion(1);
        
        return settings;
    }

    /**
     * Call reconfigure() after setting settings to
     * affect all new settings
     */
    private void reconfigure() 
    {
        logger.info("Reconfigure()");

        /* check for any sessions that should be killed according to new rules */
        this.killMatchingSessions(THREAT_PREVENTION_SESSION_MATCHER);

        if (settings == null) {
            logger.warn("Invalid settings: null");
        } else {
            otherHandler.configure(settings);
        }
    }

    /**
     * Deploy the web app
     *
     * @param logger
     *        The logger
     */
    private static synchronized void startServlet(Logger logger)
    {
        boolean firstIn = AppCount.get() == 0;
        AppCount.incrementAndGet();
        if(firstIn == false){
            return;
        }

        if (UvmContextFactory.context().tomcatManager().loadServlet("/threat-prevention", "threat-prevention") != null) {
            logger.debug("Deployed ThreatPrevention WebApp");
        } else {
            logger.error("Unable to deploy ThreatPrevention WebApp");
        }
    }

    /**
     * Undeploy the web app
     * 
     * @param logger
     *        The logger
     */
    private static synchronized void stopServlet(Logger logger)
    {
        boolean lastOut = AppCount.decrementAndGet() == 0;
        if(lastOut  == false){
            return;
        }

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/threat-prevention")) {
            logger.debug("Unloaded ThreatPrevention WebApp");
        } else {
            logger.warn("Unable to unload ThreatPrevention WebApp");
        }
    }

}
