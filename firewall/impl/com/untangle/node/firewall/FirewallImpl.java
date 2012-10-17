/**
 * $Id$
 */
package com.untangle.node.firewall;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.PortMatcher;
import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.node.SessionTuple;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeSession;

public class FirewallImpl extends NodeBase implements Firewall
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String STAT_BLOCK = "block";
    private static final String STAT_FLAG = "flag";
    private static final String STAT_PASS = "pass";
    
    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private EventLogQuery allEventsQuery;
    private EventLogQuery flaggedEventsQuery;
    private EventLogQuery blockedEventsQuery;
    
    private FirewallSettings settings = null;

    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher FIREWALL_SESSION_MATCHER = new SessionMatcher() {
            
            /* Kill all sessions that should be blocked */
            public boolean isMatch(Long sessionPolicyId, SessionTuple client, SessionTuple server, Map<String,Object> attachments)
            {
                if (handler == null)
                    return false;

                FirewallRule matchedRule = null;
                
                /**
                 * Find the matching rule compute block/log verdicts
                 */
                for (FirewallRule rule : settings.getRules()) {
                    if (rule.isMatch(client.getProtocol(),
                                     client.getClientIntf(), server.getServerIntf(),
                                     client.getClientAddr(),  client.getServerAddr(),
                                     client.getClientPort(), client.getServerPort(),
                                     (String)attachments.get(NodeSession.KEY_PLATFORM_USERNAME))) {
                        matchedRule = rule;
                        break;
                    }
                }
        
                if (matchedRule == null)
                    return false;

                logger.info("Firewall Save Setting Matcher: " +
                            client.getClientAddr() + ":" + client.getClientPort() + " -> " +
                            server.getServerAddr() + ":" + server.getServerPort() + " :: block:" + matchedRule.getBlock());
                
                return matchedRule.getBlock();
            }
        };

    /**
     * nodeInstanceCount stores the number of this node type initialized thus far
     * nodeInstanceNum stores the number of this given node type
     * This is done so each node of this type has a unique sequential identifier
     */
    private static int nodeInstanceCount = 0;
    private final int  nodeInstanceNum;
    
    public FirewallImpl( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        synchronized(getClass()) { this.nodeInstanceNum = nodeInstanceCount++; };

        this.handler = new EventHandler(this);

        /* Have to figure out pipeline ordering, this should always
         * next to towards the outside, then there is OpenVpn and then Nat */
        this.pipeSpec = new SoloPipeSpec("firewall", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 3);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };

        this.allEventsQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                                                "SELECT * FROM reports.sessions " + 
                                                "WHERE policy_id = :policyId " +
                                                "AND firewall_rule_index IS NOT NULL " +
                                                "ORDER BY time_stamp DESC");   

        this.flaggedEventsQuery = new EventLogQuery(I18nUtil.marktr("Flagged Events"),
                                                    "SELECT * FROM reports.sessions " + 
                                                    "WHERE policy_id = :policyId " +
                                                    "AND firewall_flagged IS TRUE " +
                                                    "ORDER BY time_stamp DESC");

        this.blockedEventsQuery = new EventLogQuery(I18nUtil.marktr("Blocked Events"),
                                                    "SELECT * FROM reports.sessions " + 
                                                    "WHERE policy_id = :policyId " +
                                                    "AND firewall_blocked IS TRUE " +
                                                    "ORDER BY time_stamp DESC");

        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new NodeMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventsQuery, this.flaggedEventsQuery, this.blockedEventsQuery };
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
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    protected void preStart()
    {
        this.reconfigure();
    }

    protected void postStart()
    {
        killSessions();
    }

    protected void postStop()
    {
        killSessions();
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        FirewallSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-firewall/" + "settings_" + nodeID;

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
            logger.info("Settings: " + this.settings.toJSONString());
        }

        this.reconfigure();
    }

    // package protected methods -----------------------------------------------

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
         * We use 1000*nodeInstanceNum as a starting point so rule IDs don't overlap with other firewall
         *
         * Also set flag to true if rule is blocked
         */
        int idx = (this.nodeInstanceNum * 1000);
        for (FirewallRule rule : newSettings.getRules()) {
            rule.setId(++idx);

            if (rule.getBlock())
                rule.setFlag(true);
        }
        
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(FirewallSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-firewall/" + "settings_"  + nodeID, newSettings);
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

    private void killSessions()
    {
        /* Kill all active sessions */
        if (getNodeSettings().getPolicyId() == null)
            this.killMatchingSessions(new SessionMatcher() { public boolean isMatch( Long policyId, SessionTuple client, SessionTuple server, Map<String, Object> attachments ) { return true; } });
        else 
            this.killMatchingSessions(new SessionMatcher() {
                    public boolean isMatch( Long policyId, SessionTuple client, SessionTuple server, Map<String, Object> attachments )
                    {
                        if (getNodeSettings().getPolicyId().equals( policyId ))
                            return true;
                        else
                            return false;
                    }
                });
    }
}
