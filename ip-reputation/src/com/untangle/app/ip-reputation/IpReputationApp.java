/**
 * $Id$
 */
package com.untangle.app.ip_reputation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONObject;

import com.untangle.app.webroot.WebrootQuery;
import com.untangle.app.webroot.WebrootDaemon;

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
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.IntMatcher;

/** FirewalApp is the IP Reputation Application implementation */
public class IpReputationApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    public List<IPMaskedAddress> localNetworks = null;
    public WebrootQuery webrootQuery = null;

    private static final String STAT_BLOCK = "block";
    private static final String STAT_FLAG = "flag";
    private static final String STAT_PASS = "pass";
    private static final String STAT_LOOKUP_AVG = "lookup_avg";
    
    private final IpReputationEventHandler handler;
    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private static final HookCallback WebrootQueryGetUrlInfoHook;

    public static final Map<Integer, Integer> UrlCatThreatMap;
    static {
        WebrootQueryGetUrlInfoHook = new IpReputationWebrootQueryGetUrlInfoHook();
        UvmContextFactory.context().hookManager().registerCallback( HookManager.WEBFILTER_BASE_CATEGORIZE_SITE, WebrootQueryGetUrlInfoHook );

        UrlCatThreatMap = new HashMap<>();
        UrlCatThreatMap.put(71, 1);         // Spam
        UrlCatThreatMap.put(67, 16);        // Botnets
        UrlCatThreatMap.put(57, 256);       // Phishing
        UrlCatThreatMap.put(58, 512);       // Proxy
        UrlCatThreatMap.put(49, 655362);    // Keyloggers
        UrlCatThreatMap.put(56, 131072);    // Malware
        UrlCatThreatMap.put(59, 262144);    // Spyware
    }

    private IpReputationSettings settings = null;

    /**
     * This is used to reset sessions that are blocked by ip reputation when they switch policy
     */
    private final SessionMatcher IP_REPUTATION_SESSION_MATCHER = new SessionMatcher() {
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
                                    Map<String,Object> attachments )
            {
                logger.warn("in isMatch");
                logger.warn(handler);
                if (handler == null)
                    return false;

                IpReputationPassRule matchedRule = null;
                
                /**
                 * Find the matching rule compute block/log verdicts
                 */
                // for (IpReputationRule rule : settings.getPassRules()) {
                //     if (rule.isMatch(protocol,
                //                      clientIntf, serverIntf,
                //                      clientAddr, serverAddr,
                //                      clientPort, serverPort)) {
                //         matchedRule = rule;
                //         break;
                //     }
                // }
        
                if (matchedRule == null)
                    return false;

                // logger.info("IP Reputation Save Setting Matcher: " +
                //             clientAddr.getHostAddress().toString() + ":" + clientPort + " -> " +
                //             serverAddr.getHostAddress().toString() + ":" + serverPort +
                //             " :: block:" + matchedRule.getBlock());
                
                // return matchedRule.getBlock();
                return false;
            }
    };
    
    /**
     * IP Reputation App constructor
     * @param appSettings - the AppSettings
     * @param appProperties the AppProperties
     */
    public IpReputationApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );

        // Calculate home networks as a uvm network function
        //  // Just pull context?  Would have to contentw with chnges, right?
        // this.homeNetworks = this.calculateHomeNetworks( UvmContextFactory.context().networkManager().getNetworkSettings());
        
        // getlocalNetworks
        // this.networkSettingsChangeHook = new IntrusionPreventionNetworkSettingsHook();
        //      this should just get local network list for us.
        localNetworks = UvmContextFactory.context().networkManager().getLocalNetworks();


        this.handler = new IpReputationEventHandler(this);

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new AppMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        this.addMetric(new AppMetric(STAT_LOOKUP_AVG, I18nUtil.marktr("Lookup time average"), 0L, AppMetric.Type.AVG_TIME, I18nUtil.marktr("ms"), true));
        // this.addMirroredMetrics( WebrootDaemon );
        // this.addMirroredMetrics( WebrootQuery );

        // !!! underscore, single word, or dash?
        this.connector = UvmContextFactory.context().pipelineFoundry().create("ip_reputation", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -2000, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Get the current IP Reputation Settings
     * @return IpReputationSettings
     */
    public IpReputationSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the current IP Reputation settings
     * @param newSettings
     */
    public void setSettings(final IpReputationSettings newSettings)
    {
        /**
         * set the new ID of each rule
         * We use 100,000 * appId as a starting point so rule IDs don't overlap with other ip reputation
         *
         * Also set flag to true if rule is blocked
         */
        int idx = this.getAppSettings().getPolicyId().intValue() * 100000;
        for (IpReputationPassRule rule : newSettings.getPassRules()) {
            rule.setRuleId(++idx);

            // if (rule.getBlock())
            //     rule.setFlag(true);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "ip-reputation/" + "settings_"  + appID + ".js", newSettings );
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
    public List<IpReputationPassRule> getPassRules()
    {
        if (getSettings() == null)
            return null;
        
        return getSettings().getPassRules();
    }

    /**
     * Set the current ruleset
     * @param rules - the new rules
     */
    public void setRules( List<IpReputationPassRule> rules )
    {
        IpReputationSettings set = getSettings();

        if (set == null) {
            logger.warn("NULL settings");
            return;
        }

        set.setPassRules(rules);
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
     * Add new time to total time counter.
     * @param time Long of time to add.
     */
    public void adjustLookupAverage(long time)
    {
        this.adjustMetric(STAT_LOOKUP_AVG, time);
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
        IpReputationSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/ip-reputation/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( IpReputationSettings.class, settingsFileName );
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
    private IpReputationSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        /* A few sample settings */
        List<IpReputationPassRule> ruleList = new LinkedList<>();
        LinkedList<IpReputationPassRuleCondition> matcherList = null;
            
        // /* example rule 1 */
        // IpReputationRuleCondition portMatch1 = new IpReputationRuleCondition(IpReputationRuleCondition.ConditionType.DST_PORT, "21");
        // matcherList = new LinkedList<>();
        // matcherList.add(portMatch1);
        // ruleList.add(new IpReputationRule(false, matcherList, true, true, "Block and flag all traffic destined to port 21"));
                             
        // /* example rule 2 */
        // IpReputationRuleCondition addrMatch2 = new IpReputationRuleCondition(IpReputationRuleCondition.ConditionType.SRC_ADDR, "1.2.3.4/255.255.255.0");
        // matcherList = new LinkedList<>();
        // matcherList.add(addrMatch2);
        // ruleList.add(new IpReputationRule(false, matcherList, true, true, "Block and flag all TCP traffic from 1.2.3.0 netmask 255.255.255.0"));

        // /* example rule 3 */
        // IpReputationRuleCondition addrMatch3 = new IpReputationRuleCondition(IpReputationRuleCondition.ConditionType.DST_ADDR, "1.2.3.4/255.255.255.0");
        // IpReputationRuleCondition portMatch3 = new IpReputationRuleCondition(IpReputationRuleCondition.ConditionType.DST_PORT, "1000-5000");
        // matcherList = new LinkedList<>();
        // matcherList.add(addrMatch3);
        // matcherList.add(portMatch3);
        // ruleList.add(new IpReputationRule(false, matcherList, true, false, "Accept and flag all traffic to the range 1.2.3.1 - 1.2.3.10 to ports 1000-5000"));

        IpReputationSettings settings = new IpReputationSettings(ruleList);
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
        this.killMatchingSessions(IP_REPUTATION_SESSION_MATCHER);

        if (settings == null) {
            logger.warn("Invalid settings: null");
        } else {
            handler.configure(settings);
        }
    }

    /**
     * Hook into network setting saves.
     */
    static private class IpReputationWebrootQueryGetUrlInfoHook implements HookCallback
    {
        private static final Logger hookLogger = Logger.getLogger(IpReputationWebrootQueryGetUrlInfoHook.class);
        /**
        * @return Name of callback hook
        */
        public String getName()
        {
            return "ip-reputation-categorize-site";
        }

        /**
         * Callback documentation
         *
         * @param args  Args to pass
         */
        public void callback( Object... args )
        {
            AppTCPSession sess = (AppTCPSession) args[0];
            Integer reputation = (Integer) args[1];
            @SuppressWarnings("unchecked")
            List<Integer> categories = (List<Integer>) args[2];

            if(sess == null || reputation == null || categories == null){
                return;
            }
            if ( ! (sess instanceof AppTCPSession) ) {
                hookLogger.warn( "Invalid session: " + sess);
                return;
            }
            int threatmask = 0;
            for(Integer category : categories){
                if(UrlCatThreatMap.get(category) != null){
                    threatmask += UrlCatThreatMap.get(category);
                }
            }

            sess.globalAttach(AppSession.KEY_IP_REPUTATION_SERVER_REPUTATION, reputation);
            sess.globalAttach(AppSession.KEY_IP_REPUTATION_SERVER_THREATMASK, threatmask);

        }
    }

}
