/**
 * $Id$
 */
package com.untangle.node.bandwidth_control;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.net.InetAddress;
import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeBase;

public class BandwidthControlApp extends NodeBase
{
    public static final String STAT_PRIORITIZE = "prioritize";
    public static final String STAT_TAGGED = "tagged";

    private final Logger logger = Logger.getLogger(getClass());

    private boolean shownExpiredWarning = false;
    
    private final BandwidthControlEventHandler handler = new BandwidthControlEventHandler( this );

    private BandwidthControlSettings settings;

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final HostTaggedHook hostTaggedHook = new HostTaggedHook();
    private final HostQuotaGivenHook hostQuotaGivenHook = new HostQuotaGivenHook();
    private final HostQuotaExceededHook hostQuotaExceededHook = new HostQuotaExceededHook();
    private final HostQuotaRemovedHook hostQuotaRemovedHook = new HostQuotaRemovedHook();
    private final UserQuotaGivenHook userQuotaGivenHook = new UserQuotaGivenHook();
    private final UserQuotaExceededHook userQuotaExceededHook = new UserQuotaExceededHook();
    private final UserQuotaRemovedHook userQuotaRemovedHook = new UserQuotaRemovedHook();
   
    public BandwidthControlApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.addMetric(new NodeMetric(STAT_PRIORITIZE, I18nUtil.marktr("Session prioritized")));
        this.addMetric(new NodeMetric(STAT_TAGGED, I18nUtil.marktr("Host tagged")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("bandwidth", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 20, true);
        this.connectors = new PipelineConnector[] { connector };
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        BandwidthControlSettings readSettings = null;
        try {
            readSettings = settingsManager.load( BandwidthControlSettings.class, System.getProperty("uvm.settings.dir") + "/bandwidth-control/" + "settings_" + nodeID + ".js" );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        if (readSettings == null) {
            logger.warn("Initializing new settings (no settings found)...");
            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            this._setSettings(readSettings, false);
        }
    }
        
    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);

        if ( ! isLicenseValid() ) {
            throw new RuntimeException( i18nUtil.tr( "Unable to start an node: invalid license" ));
        }
        
        if ( settings == null ) {
            throw new RuntimeException( i18nUtil.tr( "Settings not found - an internal error has occurred." ));
        }

        if ( ( settings.getConfigured() == null ) || !settings.getConfigured() ) {
            throw new RuntimeException( i18nUtil.tr( "Bandwidth Control must be configured before it can be enabled.") + "<br/>" +
                                        i18nUtil.tr( "This can be done using the Setup Wizard (in the settings)." ));
        }

        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.HOST_TABLE_TAGGED, this.hostTaggedHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_GIVEN, this.hostQuotaGivenHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_EXCEEDED, this.hostQuotaExceededHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_REMOVED, this.hostQuotaRemovedHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_GIVEN, this.userQuotaGivenHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_EXCEEDED, this.userQuotaExceededHook );
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_REMOVED, this.userQuotaRemovedHook );
    }

    @Override
    protected void preStop( boolean isPermanentTransition ) 
    {
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.HOST_TABLE_TAGGED, this.hostTaggedHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_GIVEN, this.hostQuotaGivenHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_EXCEEDED, this.hostQuotaExceededHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.HOST_TABLE_QUOTA_REMOVED, this.hostQuotaRemovedHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_GIVEN, this.userQuotaGivenHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_EXCEEDED, this.userQuotaExceededHook );
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.USER_TABLE_QUOTA_REMOVED, this.userQuotaRemovedHook );
    }

    @Override
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        this.settings = new BandwidthControlSettings();
        List<BandwidthControlRule> rules = new LinkedList<BandwidthControlRule>();

        this.settings.setRules(rules);
        this.setSettings(this.settings);
    }

    public BandwidthControlSettings getSettings()
    {
        return this.settings;
    }
    
    public void setSettings( BandwidthControlSettings newSettings )
    {
        this._setSettings(newSettings, true);
    }

    public List<BandwidthControlRule> getRules()
    {
        return this.settings.getRules();
    }
    
    public void setRules( List<BandwidthControlRule> rules )
    {
        BandwidthControlSettings newSettings = getSettings();
        newSettings.setRules(rules);
        this.setSettings(newSettings);
    }

    public void wizardLoadDefaults( String defaultConfiguration )
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        BandwidthControlSettings readSettings = null;

        logger.info("Loading new Configuration: " + defaultConfiguration);
        
        try {
            readSettings = settingsManager.load( BandwidthControlSettings.class, System.getProperty("uvm.lib.dir") + "/bandwidth-control/defaults_" + defaultConfiguration + ".js" );
        } catch (SettingsManager.SettingsException e) {
            e.printStackTrace();
        }

        if (readSettings == null) {
            logger.warn("Configuration not found: name:" + defaultConfiguration + " file: " + System.getProperty("uvm.lib.dir") + "/" + "bandwidth-control/" + "defaults_" + defaultConfiguration + ".js");
        }
        else {
            logger.info("Loading new Defaults..." + defaultConfiguration);
            this.setSettings(readSettings);
        }
    }

    public void wizardAddHostQuotaRules(int quotaTimeSec, long quotaBytes, int overQuotaPriority)
    {
        /**
         * Create the quota-assigning rule
         */
        BandwidthControlRule newRule0 = new BandwidthControlRule();
        BandwidthControlRuleAction newRule0Action = new BandwidthControlRuleAction();
        newRule0Action.setActionType(BandwidthControlRuleAction.ActionType.GIVE_HOST_QUOTA);
        newRule0Action.setQuotaTime(quotaTimeSec);
        newRule0Action.setQuotaBytes(quotaBytes);
        BandwidthControlRuleCondition newRule0Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.HOST_HAS_NO_QUOTA, null);
        BandwidthControlRuleCondition newRule0Matcher2 = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.SRC_INTF, "non_wan");
        List<BandwidthControlRuleCondition> newRule0matchers = new LinkedList<BandwidthControlRuleCondition>();
        newRule0matchers.add(newRule0Matcher);
        newRule0matchers.add(newRule0Matcher2);
        newRule0.setAction(newRule0Action);
        newRule0.setConditions(newRule0matchers);
        newRule0.setDescription("Give Host a Quota if no Quota");
        newRule0.setEnabled(true);

        /**
         * Create the quota-enforcement rule
         */
        BandwidthControlRule newRule1 = new BandwidthControlRule();
        BandwidthControlRuleAction newRule1Action = new BandwidthControlRuleAction();
        newRule1Action.setActionType(BandwidthControlRuleAction.ActionType.SET_PRIORITY);
        newRule1Action.setPriority(new Integer(overQuotaPriority));
        BandwidthControlRuleCondition newRule1Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.HOST_QUOTA_EXCEEDED, null);
        List<BandwidthControlRuleCondition> newRule1matchers = new LinkedList<BandwidthControlRuleCondition>();
        newRule1matchers.add(newRule1Matcher);
        newRule1.setAction(newRule1Action);
        newRule1.setConditions(newRule1matchers);
        newRule1.setDescription("Penalize Hosts over Quota");
        newRule1.setEnabled(true);

        List<BandwidthControlRule> currentRules = this.getRules();
        currentRules.add(0,newRule1);
        currentRules.add(0,newRule0);
        this.setRules(currentRules);

        return;
    }

    public void wizardAddUserQuotaRules(int quotaTimeSec, long quotaBytes, int overQuotaPriority)
    {
        /**
         * Create the quota-assigning rule
         */
        BandwidthControlRule newRule0 = new BandwidthControlRule();
        BandwidthControlRuleAction newRule0Action = new BandwidthControlRuleAction();
        newRule0Action.setActionType(BandwidthControlRuleAction.ActionType.GIVE_USER_QUOTA);
        newRule0Action.setQuotaTime(quotaTimeSec);
        newRule0Action.setQuotaBytes(quotaBytes);
        BandwidthControlRuleCondition newRule0Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.USER_HAS_NO_QUOTA, null);
        BandwidthControlRuleCondition newRule0Matcher2 = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.SRC_INTF, "non_wan");
        List<BandwidthControlRuleCondition> newRule0matchers = new LinkedList<BandwidthControlRuleCondition>();
        newRule0matchers.add(newRule0Matcher);
        newRule0matchers.add(newRule0Matcher2);
        newRule0.setAction(newRule0Action);
        newRule0.setConditions(newRule0matchers);
        newRule0.setDescription("Give User a Quota if no Quota");
        newRule0.setEnabled(true);

        /**
         * Create the quota-enforcement rule
         */
        BandwidthControlRule newRule1 = new BandwidthControlRule();
        BandwidthControlRuleAction newRule1Action = new BandwidthControlRuleAction();
        newRule1Action.setActionType(BandwidthControlRuleAction.ActionType.SET_PRIORITY);
        newRule1Action.setPriority(new Integer(overQuotaPriority));
        BandwidthControlRuleCondition newRule1Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.USER_QUOTA_EXCEEDED, null);
        List<BandwidthControlRuleCondition> newRule1matchers = new LinkedList<BandwidthControlRuleCondition>();
        newRule1matchers.add(newRule1Matcher);
        newRule1.setAction(newRule1Action);
        newRule1.setConditions(newRule1matchers);
        newRule1.setDescription("Penalize Users over Quota");
        newRule1.setEnabled(true);

        List<BandwidthControlRule> currentRules = this.getRules();
        currentRules.add(0,newRule1);
        currentRules.add(0,newRule0);
        this.setRules(currentRules);

        return;
    }
    
    public void incrementCount( String stat, long delta )
    {
        this.adjustMetric( stat, delta );
    }

    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.BANDWIDTH_CONTROL))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.BANDWIDTH_CONTROL_OLDNAME))
            return true;
        return false;
    }

    /**
     * This forces the app to reevaluate all sessions
     * of the specified addr.
     * This is useful when hosts are tagged or when quotas have expired
     */
    public void reprioritizeHostSessions(InetAddress addr, String reason)
    {
        if ( addr == null )
            return;
        logger.info("Reproritizing sessions for host " + addr.getHostAddress() + " because \"" + reason + "\"");
        this.handler.reprioritizeHostSessions(addr, reason);
    }

    /**
     * This forces the app to reevaluate all sessions
     * of the specified addr.
     * This is useful when hosts are tagged or when quotas have expired
     */
    public void reprioritizeUserSessions(String username, String reason)
    {
        if ( username == null )
            return;
        logger.info("Reproritizing sessions for user " + username + " because \"" + reason + "\"");
        this.handler.reprioritizeUserSessions(username, reason);
    }
    
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Set the current settings to new Settings
     * Also save the settings to disk if save is true
     */
    private void _setSettings(BandwidthControlSettings newSettings, boolean save)
    {
        _processSettings(newSettings);
        
        if (save) {
            /**
             * Save the settings
             */
            SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
            String nodeID = this.getNodeSettings().getId().toString();
            try {
                settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "bandwidth-control" + "/" + "settings_" + nodeID + ".js", newSettings );
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to save settings.",e);
                return;
            }
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    /**
     * Resets IDs to unique IDs
     * Also sets node parameter and other in-memory meta-data on Rules
     */
    private void _processSettings(BandwidthControlSettings settings)
    {
        List<BandwidthControlRule> rules = settings.getRules();

        /**
         * This section computes all the metadata
         * Since this object has likely been passed from the UI
         * It likely hasn't been fully initialized
         */

        /**
         * set the new ID of each rule
         * We use 100,000 * nodeId as a starting point so rule IDs don't overlap with other firewall
         */
        int idx = this.getNodeSettings().getPolicyId().intValue() * 100000;
        for (BandwidthControlRule rule : rules) {
            idx++;
            rule.setRuleId(idx);
            rule.getAction().setNode(this);
        }
    }

    private class HostTaggedHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-tagged"; }
        public void callback( Object o ) { reprioritizeHostSessions((InetAddress)o, "host tagged"); }
    }

    private class HostQuotaGivenHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-given-hook"; }
        public void callback( Object o ) { reprioritizeHostSessions((InetAddress)o, "quota given"); }
    }

    private class HostQuotaExceededHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-exceeded-hook"; }
        public void callback( Object o ) { reprioritizeHostSessions((InetAddress)o, "quota exceeded"); }
    }

    private class HostQuotaRemovedHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-removed-hook"; }
        public void callback( Object o ) { reprioritizeHostSessions((InetAddress)o, "quota removed"); }
    }

    private class UserQuotaGivenHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-given-hook"; }
        public void callback( Object o ) { reprioritizeUserSessions((String)o, "quota given"); }
    }

    private class UserQuotaExceededHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-exceeded-hook"; }
        public void callback( Object o ) { reprioritizeUserSessions((String)o, "quota exceeded"); }
    }

    private class UserQuotaRemovedHook implements HookCallback
    {
        public String getName() { return "bandwidth-control-quota-removed-hook"; }
        public void callback( Object o ) { reprioritizeUserSessions((String)o, "quota removed"); }
    }
    
}

    
