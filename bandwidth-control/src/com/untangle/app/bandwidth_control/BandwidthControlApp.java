/**
 * $Id$
 */
package com.untangle.app.bandwidth_control;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.net.InetAddress;
import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppBase;

/**
 * The Bandwidth Control App is an app dedicated to shaping and controlling bandwidth usage
 */
public class BandwidthControlApp extends AppBase
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
   
    /**
     * Create an BandwidthControlApp instance
     * @param appSettings The appSettings object
     * @param appProperties The appProperties object
     */
    public BandwidthControlApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.addMetric(new AppMetric(STAT_PRIORITIZE, I18nUtil.marktr("Session prioritized")));
        this.addMetric(new AppMetric(STAT_TAGGED, I18nUtil.marktr("Host tagged")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("bandwidth", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 20, true);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * The postInit() hook
     * This loads the settings from file or initializes them if necessary
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        BandwidthControlSettings readSettings = null;
        try {
            readSettings = settingsManager.load( BandwidthControlSettings.class, System.getProperty("uvm.settings.dir") + "/bandwidth-control/" + "settings_" + appID + ".js" );
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
        
    /**
     * The preStart() hook
     * This registers the callbacks and does some sanity checks
     * @param isPermanentTransition - true if this is a permanent transition
     */
    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);

        if ( ! isLicenseValid() ) {
            throw new RuntimeException( i18nUtil.tr( "Unable to start an app: invalid license" ));
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

    /**
     * The preStop() hook
     * This unregisters all the hooks
     * @param isPermanentTransition - true if this is a permanent transition
     */
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

    /**
     * Initializes the settings to the default settings
     */
    @Override
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        this.settings = new BandwidthControlSettings();
        List<BandwidthControlRule> rules = new LinkedList<>();

        this.settings.setRules(rules);
        this.setSettings(this.settings);
    }

    /**
     * Get the current settings
     * @return settings
     */
    public BandwidthControlSettings getSettings()
    {
        return this.settings;
    }
    
    /**
     * Set the current settings
     * @param newSettings - the new settings
     */
    public void setSettings( BandwidthControlSettings newSettings )
    {
        this._setSettings(newSettings, true);
    }

    /**
     * Get only the current rules from the current settings
     * @return the rules
     */
    public List<BandwidthControlRule> getRules()
    {
        return this.settings.getRules();
    }

    /**
     * Overwrite the existing rules in the settings with the specified rules
     * And save the new settings
     * @param rules - the new rules
     */
    public void setRules( List<BandwidthControlRule> rules )
    {
        BandwidthControlSettings newSettings = getSettings();
        newSettings.setRules(rules);
        this.setSettings(newSettings);
    }

    /**
     * Load the set the current settings to the suggested defaults
     * for the type of organization specified.
     * This is called by the setup wizard
     * @param defaultConfiguration (ie "school")
     */
    public void wizardLoadDefaults( String defaultConfiguration )
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        BandwidthControlSettings readSettings = null;

        logger.info("Loading new Configuration: " + defaultConfiguration);
        
        try {
            readSettings = settingsManager.load( BandwidthControlSettings.class, System.getProperty("uvm.lib.dir") + "/bandwidth-control/defaults_" + defaultConfiguration + ".json" );
        } catch (SettingsManager.SettingsException e) {
            e.printStackTrace();
        }

        if (readSettings == null) {
            logger.warn("Configuration not found: name:" + defaultConfiguration + " file: " + System.getProperty("uvm.lib.dir") + "/" + "bandwidth-control/" + "defaults_" + defaultConfiguration + ".json");
        }
        else {
            logger.info("Loading new Defaults..." + defaultConfiguration);
            this.setSettings(readSettings);
        }
    }

    /**
     * Add Host Quota rules to the existing ruleset.
     * This is called by the setup wizard
     * @param quotaTimeSec - the quota lifetime
     * @param quotaBytes - the quota size
     * @param overQuotaPriority - the priority of traffic over-quota
     */
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
        BandwidthControlRuleCondition newRule0Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.HOST_HAS_NO_QUOTA, "true");
        BandwidthControlRuleCondition newRule0Matcher2 = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.SRC_INTF, "non_wan");
        List<BandwidthControlRuleCondition> newRule0matchers = new LinkedList<>();
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
        BandwidthControlRuleCondition newRule1Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.HOST_QUOTA_EXCEEDED, "true");
        List<BandwidthControlRuleCondition> newRule1matchers = new LinkedList<>();
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

    /**
     * Add User Quota rules to the existing ruleset.
     * This is called by the setup wizard
     * @param quotaTimeSec - the quota lifetime
     * @param quotaBytes - the quota size
     * @param overQuotaPriority - the priority of traffic over-quota
     */
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
        BandwidthControlRuleCondition newRule0Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.USER_HAS_NO_QUOTA, "true");
        BandwidthControlRuleCondition newRule0Matcher2 = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.SRC_INTF, "non_wan");
        List<BandwidthControlRuleCondition> newRule0matchers = new LinkedList<>();
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
        BandwidthControlRuleCondition newRule1Matcher = new BandwidthControlRuleCondition(BandwidthControlRuleCondition.ConditionType.USER_QUOTA_EXCEEDED, "true");
        List<BandwidthControlRuleCondition> newRule1matchers = new LinkedList<>();
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
    
    /**
     * Increment the count of the specified mentric by the specified amount
     * @param stat - the name of the metric
     * @param delta - the amount to increment metric
     */
    public void incrementCount( String stat, long delta )
    {
        this.adjustMetric( stat, delta );
    }

    /**
     * Check the license
     * @return true if license is valid, false otherwise
     */
    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.BANDWIDTH_CONTROL))
            return true;
        return false;
    }

    /**
     * This forces the app to reevaluate all sessions of the specified host address.
     * This is useful when hosts are tagged or when quotas have expired
     * @param addr - the address
     * @param reason - this is just printed in log statements for logging/debugging
     */
    public void reprioritizeHostSessions(InetAddress addr, String reason)
    {
        if ( addr == null )
            return;
        logger.info("Reproritizing sessions for host " + addr.getHostAddress() + " because \"" + reason + "\"");
        this.handler.reprioritizeHostSessions(addr, reason);
    }

    /**
     * This forces the app to reevaluate all sessions of the specified username.
     * This is useful when hosts are tagged or when quotas have expired.
     * @param username - the username
     * @param reason - this is just printed in log statements for logging/debugging
     */
    public void reprioritizeUserSessions(String username, String reason)
    {
        if ( username == null )
            return;
        logger.info("Reproritizing sessions for user " + username + " because \"" + reason + "\"");
        this.handler.reprioritizeUserSessions(username, reason);
    }
    
    /**
     * Get the pipeline connectors for this ad blocker
     * @return the pipelineconnectors array
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Set the current settings to new Settings
     * Also save the settings to disk if save is specified
     * 
     * @param newSettings - the new settings
     * @param save - if true, save to disk, if false just set current settings
     */
    private void _setSettings(BandwidthControlSettings newSettings, boolean save)
    {
        _processSettings(newSettings);
        
        if (save) {
            /**
             * Save the settings
             */
            SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
            String appID = this.getAppSettings().getId().toString();
            try {
                settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "bandwidth-control" + "/" + "settings_" + appID + ".js", newSettings );
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
     * Resets rule IDs to unique IDs
     * Also sets app parameter and other in-memory meta-data on Rules
     * @param settings - the settings to fixup
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
         * We use 100,000 * appId as a starting point so rule IDs don't overlap with other firewall
         */
        int idx = this.getAppSettings().getPolicyId().intValue() * 100000;
        for (BandwidthControlRule rule : rules) {
            idx++;
            rule.setRuleId(idx);
            rule.getAction().setApp(this);
        }
    }

    /**
     * HostTaggedHook is a hook registered for when a host gets a tag (in the host table)
     * This proceeds to reprioritize existing sessions for said host in case they change
     */
    private class HostTaggedHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-tagged-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeHostSessions((InetAddress)o, "host tagged"); }
    }

    /**
     * HostQuotaGivenHook is a hook registered for when a host gets a quota (in the host table)
     * This proceeds to reprioritize existing sessions for said host in case they change
     */
    private class HostQuotaGivenHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-given-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeHostSessions((InetAddress)o, "quota given"); }
    }

    /**
     * HostQuotaExceededHook is a hook registered for when a host exceeds a quota (in the host table)
     * This proceeds to reprioritize existing sessions for said host in case they change
     */
    private class HostQuotaExceededHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-exceeded-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeHostSessions((InetAddress)o, "quota exceeded"); }
    }

    /**
     * HostQuotaRemovedHook is a hook registered for when a host has a quota removed (in the host table)
     * This proceeds to reprioritize existing sessions for said host in case they change
     */
    private class HostQuotaRemovedHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-removed-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeHostSessions((InetAddress)o, "quota removed"); }
    }

    /**
     * UserQuotaGivenHook is a hook registered for when a user gets a quota (in the user table)
     * This proceeds to reprioritize existing sessions for said user in case they change
     */
    private class UserQuotaGivenHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-given-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeUserSessions((String)o, "quota given"); }
    }

    /**
     * UserQuotaExceededHook is a hook registered for when a user exceeds a quota (in the user table)
     * This proceeds to reprioritize existing sessions for said user in case they change
     */
    private class UserQuotaExceededHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-exceeded-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeUserSessions((String)o, "quota exceeded"); }
    }

    /**
     * UserQuotaRemovedHook is a hook registered for when a user has a quota removed (in the user table)
     * This proceeds to reprioritize existing sessions for said user in case they change
     */
    private class UserQuotaRemovedHook implements HookCallback
    {
        /**
         * @return the Name of the hook
         */
        public String getName() { return "bandwidth-control-quota-removed-hook-" + getAppSettings().getId().toString(); }

        /**
         * The callback function
         * @param args - the hook args
         */
        public void callback( Object... args ) { Object o = args[0]; reprioritizeUserSessions((String)o, "quota removed"); }
    }
    
}

    
