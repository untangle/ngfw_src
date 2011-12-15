/*
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SessionMatcherFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.Validator;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.PortMatcher;
import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.node.util.SimpleExec;

public class FirewallImpl extends AbstractNode implements Firewall
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/firewall-convert-settings.py";

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

    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher FIREWALL_SESSION_MATCHER = new SessionMatcher() {
            
            /* Kill all sessions that should be blocked */
            public boolean isMatch(Policy sessionPolicy, IPSessionDesc client, IPSessionDesc server)
            {
                if (handler == null)
                    return false;

                FirewallRule matchedRule = null;
                
                /**
                 * Find the matching rule compute block/log verdicts
                 */
                for (FirewallRule rule : settings.getRules()) {
                    if (rule.isMatch(client.protocol(),
                                     client.clientIntf(), server.serverIntf(),
                                     client.clientAddr(),  client.serverAddr(),
                                     client.clientPort(), client.serverPort(),
                                     null)) {
                        matchedRule = rule;
                        break;
                    }
                }
        
                if (matchedRule == null)
                    return false;

                logger.info("Firewall Save Setting Matcher: " +
                            client.clientAddr() + ":" + client.clientPort() + " -> " +
                            server.serverAddr() + ":" + server.serverPort() + " :: block:" + matchedRule.getBlock());
                
                return matchedRule.getBlock();
            }
        };
    
    public FirewallImpl()
    {
        this.handler = new EventHandler(this);
        this.statisticManager = new FirewallStatisticManager(getNodeContext());

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec("firewall", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 3);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
        eventLogger = EventLoggerFactory.factory().getEventLogger(getNodeContext());

        SimpleEventFilter<FirewallEvent> ef = new FirewallAllFilter();
        eventLogger.addSimpleEventFilter(ef);
        ef = new FirewallBlockedFilter();
        eventLogger.addSimpleEventFilter(ef);

        MessageManager lmm = UvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Sessions passed"), null, I18nUtil.marktr("PASS"));
        loggedBlinger = c.addActivity("log", I18nUtil.marktr("Sessions logged"), null, I18nUtil.marktr("LOG"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Sessions blocked"), null, I18nUtil.marktr("BLOCK"));
        lmm.setActiveMetricsIfNotSet(getNodeId(), passBlinger, loggedBlinger, blockBlinger);
    }

    public EventManager<FirewallEvent> getEventManager()
    {
        return eventLogger;
    }

    public FirewallSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final FirewallSettings settings)
    {
        this._setSettings(settings);

        /* check for any sessions that should be killed according to new rules */
        this.killMatchingSessions(FIREWALL_SESSION_MATCHER);

        this.reconfigure();
    }

    public List<FirewallRule> getRules()
    {
        if (getSettings() == null)
            return null;
        
        return getSettings().getRules();
    }

    public void setRules( List<FirewallRule> rules )
    {
        FirewallSettings set = getSettings();

        if (set == null) {
            logger.warn("NULL settings");
            return;
        }

        set.setRules(rules);
        setSettings(set);
    }
    
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        FirewallSettings settings = getDefaultSettings();

        this.setSettings(settings);

        statisticManager.stop();
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

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void preStart() throws Exception
    {
        try {
            reconfigure();
        } catch (Exception e) {
            throw new Exception(e);
        }

        statisticManager.start();
    }

    protected void postStart()
    {
        /* Kill all active sessions */
        this.killMatchingSessions(SessionMatcherFactory.makePolicyInstance(getPolicy()));
    }

    protected void postStop()
    {
        /* Kill all active sessions */
        this.killMatchingSessions(SessionMatcherFactory.makePolicyInstance(getPolicy()));

        statisticManager.stop();
    }

    protected void postInit(String[] args)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        FirewallSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-firewall/" + "settings_" + nodeID;

        try {
            readSettings = settingsManager.load( FirewallSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                SimpleExec.SimpleExecResult result = null;
                logger.warn("Running: " + SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + settingsFileName + ".js");
                result = SimpleExec.exec( SETTINGS_CONVERSION_SCRIPT, new String[] { nodeID.toString() , settingsFileName + ".js"}, null, null, true, true, 1000*60, logger, true);
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( FirewallSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.info("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    // package protected methods -----------------------------------------------

    void log(FirewallEvent logEvent)
    {
        eventLogger.log(logEvent);
    }

    FirewallSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        /* A few sample settings */
        List<FirewallRule> ruleList = new LinkedList<FirewallRule>();
        LinkedList<FirewallRuleMatcher> matcherList = null;
            
        /* example rule 1 */
        FirewallRuleMatcher portMatch1 = new FirewallRuleMatcher(FirewallRuleMatcher.MatcherType.DST_PORT, "21");
        matcherList = new LinkedList<FirewallRuleMatcher>();
        matcherList.add(portMatch1);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Block and log all traffic destined to port 21"));
                             
        /* example rule 2 */
        FirewallRuleMatcher addrMatch2 = new FirewallRuleMatcher(FirewallRuleMatcher.MatcherType.SRC_ADDR, "1.2.3.4/255.255.255.0");
        matcherList = new LinkedList<FirewallRuleMatcher>();
        matcherList.add(addrMatch2);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Block all TCP traffic from 1.2.3.0 netmask 255.255.255.0"));

        /* example rule 3 */
        FirewallRuleMatcher addrMatch3 = new FirewallRuleMatcher(FirewallRuleMatcher.MatcherType.DST_ADDR, "1.2.3.4/255.255.255.0");
        FirewallRuleMatcher portMatch3 = new FirewallRuleMatcher(FirewallRuleMatcher.MatcherType.DST_PORT, "1000-5000");
        matcherList = new LinkedList<FirewallRuleMatcher>();
        matcherList.add(addrMatch3);
        matcherList.add(portMatch3);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Accept and log all traffic to the range 1.2.3.1 - 1.2.3.10 to ports 1000-5000"));

        FirewallSettings settings = new FirewallSettings(ruleList);
        settings.setVersion(1);
        
        return settings;
    }

    // private methods ---------------------------------------------------------

    private void reconfigure() 
    {
        logger.info("Reconfigure()");

        if (settings == null) {
            logger.warn("Invalid settings: null");
        } else {
            handler.configure(settings);
        }
    }

    private void updateToCurrent(FirewallSettings settings)
    {
        if (settings == null) {
            logger.error("NULL Firewall Settings");
            return;
        }

        logger.info("Update Settings Complete");
    }

    private void _setSettings( FirewallSettings newSettings )
    {
        /**
         * set the new ID of each rule
         */
        int idx = 0;
        for (FirewallRule rule : newSettings.getRules()) {
            rule.setId(++idx);
        }
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeId().getId().toString();
        try {
            settingsManager.save(FirewallSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-firewall/" + "settings_"  + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }
}
