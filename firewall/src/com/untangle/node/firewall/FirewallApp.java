/**
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.PipelineConnector;

public class FirewallApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String STAT_BLOCK = "block";
    private static final String STAT_FLAG = "flag";
    private static final String STAT_PASS = "pass";
    
    private final EventHandler handler;
    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private FirewallSettings settings = null;

    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher FIREWALL_SESSION_MATCHER = new SessionMatcher() {
            
            /* Kill all sessions that should be blocked */
            public boolean isMatch( Integer policyId, short protocol,
                                    int clientIntf, int serverIntf,
                                    InetAddress clientAddr, InetAddress serverAddr,
                                    int clientPort, int serverPort,
                                    Map<String,Object> attachments )
            {
                if (handler == null)
                    return false;

                FirewallRule matchedRule = null;
                
                /**
                 * Find the matching rule compute block/log verdicts
                 */
                for (FirewallRule rule : settings.getRules()) {
                    if (rule.isMatch(protocol,
                                     clientIntf, serverIntf,
                                     clientAddr, serverAddr,
                                     clientPort, serverPort)) {
                        matchedRule = rule;
                        break;
                    }
                }
        
                if (matchedRule == null)
                    return false;

                logger.info("Firewall Save Setting Matcher: " +
                            clientAddr.getHostAddress().toString() + ":" + clientPort + " -> " +
                            serverAddr.getHostAddress().toString() + ":" + serverPort +
                            " :: block:" + matchedRule.getBlock());
                
                return matchedRule.getBlock();
            }
        };
    
    public FirewallApp( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler = new EventHandler(this);

        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new NodeMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("firewall", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -900, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    public FirewallSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final FirewallSettings settings)
    {
        this._setSettings(settings);
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

        setSettings(settings);
    }

    public void incrementBlockCount() 
    {
        this.incrementMetric(STAT_BLOCK);
    }

    public void incrementPassCount() 
    {
        this.incrementMetric(STAT_PASS);
    }

    public void incrementFlagCount() 
    {
        this.incrementMetric(STAT_FLAG);
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        this.reconfigure();
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        killAllSessions();
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        killAllSessions();
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        FirewallSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/firewall/" + "settings_" + nodeID + ".js";

        try {
            readSettings = settingsManager.load( FirewallSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
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
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    FirewallSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        /* A few sample settings */
        List<FirewallRule> ruleList = new LinkedList<FirewallRule>();
        LinkedList<FirewallRuleCondition> matcherList = null;
            
        /* example rule 1 */
        FirewallRuleCondition portMatch1 = new FirewallRuleCondition(FirewallRuleCondition.ConditionType.DST_PORT, "21");
        matcherList = new LinkedList<FirewallRuleCondition>();
        matcherList.add(portMatch1);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Block and flag all traffic destined to port 21"));
                             
        /* example rule 2 */
        FirewallRuleCondition addrMatch2 = new FirewallRuleCondition(FirewallRuleCondition.ConditionType.SRC_ADDR, "1.2.3.4/255.255.255.0");
        matcherList = new LinkedList<FirewallRuleCondition>();
        matcherList.add(addrMatch2);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Block and flag all TCP traffic from 1.2.3.0 netmask 255.255.255.0"));

        /* example rule 3 */
        FirewallRuleCondition addrMatch3 = new FirewallRuleCondition(FirewallRuleCondition.ConditionType.DST_ADDR, "1.2.3.4/255.255.255.0");
        FirewallRuleCondition portMatch3 = new FirewallRuleCondition(FirewallRuleCondition.ConditionType.DST_PORT, "1000-5000");
        matcherList = new LinkedList<FirewallRuleCondition>();
        matcherList.add(addrMatch3);
        matcherList.add(portMatch3);
        ruleList.add(new FirewallRule(false, matcherList, true, true, "Accept and flag all traffic to the range 1.2.3.1 - 1.2.3.10 to ports 1000-5000"));

        FirewallSettings settings = new FirewallSettings(ruleList);
        settings.setVersion(1);
        
        return settings;
    }

    private void reconfigure() 
    {
        logger.info("Reconfigure()");

        /* check for any sessions that should be killed according to new rules */
        this.killMatchingSessions(FIREWALL_SESSION_MATCHER);

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
         * We use 100,000 * nodeId as a starting point so rule IDs don't overlap with other firewall
         *
         * Also set flag to true if rule is blocked
         */
        int idx = this.getNodeSettings().getPolicyId().intValue() * 100000;
        for (FirewallRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);

            if (rule.getBlock())
                rule.setFlag(true);
        }
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "firewall/" + "settings_"  + nodeID + ".js", newSettings );
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
}
