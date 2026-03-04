/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * Wan Failover Application
 */
public class WanFailoverApp extends AppBase
{
    private final Logger logger = LogManager.getLogger(getClass());

    private static final String PINGABLE_HOSTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/wan-failover-pingable-hosts.sh";

    protected static final String STAT_CONNECTED = "connected";
    protected static final String STAT_DISCONNECTED = "disconnected";
    protected static final String STAT_CHANGE = "changed";
    protected static final String STAT_DISCONNECTS = "disconnects";
    protected static final String STAT_RECONNECTS = "reconnections";

    private static final long UPTIME_WINDOW_MAX = 31l * 24l * 60l * 60l * 1000l;

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private final WanFailoverNetworkHookCallback networkHookCallback = new WanFailoverNetworkHookCallback();
    private final WanFailoverWanBalancerHookCallback wanBalancerHookCallback = new WanFailoverWanBalancerHookCallback();
    private final ActiveWanIdHookCallback activeWanIdHookCallback = new ActiveWanIdHookCallback();

    private WanFailoverSettings settings = null;

    // Initialize to empty array instead of null so we never get null pointer exceptions
    // Will be populated by WAN Balancer via the WAN_BALANCER_CHANGE hook when available
    protected static int[] wanBalancerWeights = new int[0];
    protected static ExecManager execManager = null;

    private WanFailoverTesterMonitor wanFailoverTesterMonitor = null;

    private boolean inReactivation = false;

    /**
     * Constructor
     * 
     * @param appSettings
     *        Application settings
     * @param appProperties
     *        Application properties
     */
    public WanFailoverApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.addMetric(new AppMetric(STAT_CONNECTED, I18nUtil.marktr("Connected WANs")));
        this.addMetric(new AppMetric(STAT_DISCONNECTED, I18nUtil.marktr("Disconnected WANs")));
        this.addMetric(new AppMetric(STAT_CHANGE, I18nUtil.marktr("WAN Change")));
        this.addMetric(new AppMetric(STAT_RECONNECTS, I18nUtil.marktr("Reconnects")));
        this.addMetric(new AppMetric(STAT_DISCONNECTS, I18nUtil.marktr("Disconnects")));
    }

    /**
     * Get the pipeline connectors
     * 
     * @return The pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Initialize application settings
     */
    @Override
    public void initializeSettings()
    {
        setSettings(new WanFailoverSettings());
    }

    /**
     * Run a WAN test
     * 
     * @param test
     *        The wan test to run
     * @return The test result
     */
    public String runTest(WanTestSettings test)
    {
        return this.wanFailoverTesterMonitor.runTest(test);
    }

    /**
     * Get a list of pingable hosts
     * 
     * @param uplinkID
     *        The interface ID
     * @return A list of pingable hosts
     */
    public List<String> getPingableHosts(int uplinkID)
    {
        InterfaceSettings intfSettings = UvmContextFactory.context().networkManager().findInterfaceId(uplinkID);

        if (intfSettings == null) {
            throw new RuntimeException("Invalid interface id: " + uplinkID);
        }

        String output = UvmContextFactory.context().execManager().execOutput(PINGABLE_HOSTS_SCRIPT + " " + uplinkID + " " + intfSettings.getSymbolicDev());

        if (output.trim().length() == 0) {
            throw new RuntimeException("Unable to determine pingable hosts.");
        }

        return Arrays.asList(output.split("\n"));
    }

    /**
     * Get the WAN status
     * 
     * @return The WAN status
     */
    public List<WanStatus> getWanStatus()
    {
        if (this.wanFailoverTesterMonitor == null) return new LinkedList<WanStatus>();
        return this.wanFailoverTesterMonitor.getWanStatus();
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public WanFailoverSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final WanFailoverSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/" + "wan-failover/" + "settings_" + appID + ".js", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;

        if (this.wanFailoverTesterMonitor != null) this.wanFailoverTesterMonitor.reconfigure();
    }

    /**
     * Called after the application is initialized
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        WanFailoverSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/wan-failover/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(WanFailoverSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        } else {
            logger.info("Loading Settings...");

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected synchronized void preStart(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.WAN_BALANCER_CHANGE, this.wanBalancerHookCallback);
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.REQUEST_ACTIVE_WAN_ID, this.activeWanIdHookCallback);

        if (WanFailoverApp.execManager == null) {
            WanFailoverApp.execManager = UvmContextFactory.context().createExecManager();
            WanFailoverApp.execManager.setLevel(Level.INFO);
        }

        if (this.wanFailoverTesterMonitor == null) this.wanFailoverTesterMonitor = new WanFailoverTesterMonitor(this);

        this.wanFailoverTesterMonitor.start();
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected synchronized void postStop(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.WAN_BALANCER_CHANGE, this.wanBalancerHookCallback);
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.REQUEST_ACTIVE_WAN_ID, this.activeWanIdHookCallback);

        if (this.wanFailoverTesterMonitor != null) {
            this.wanFailoverTesterMonitor.stop();
            this.wanFailoverTesterMonitor = null;
        }
        if (WanFailoverApp.execManager != null) {
            WanFailoverApp.execManager.close();
            WanFailoverApp.execManager.closeSafe();
            WanFailoverApp.execManager = null;
        }
    }

    /**
     * Called when network settings have changed
     * 
     * @param settings
     *        The new settings
     * @throws Exception
     */

    public void networkSettingsEvent(NetworkSettings settings) throws Exception
    {
        if (this.wanFailoverTesterMonitor != null) this.wanFailoverTesterMonitor.reconfigure();
    }

    /**
     * Callback hook for network settings changes notifications
     */
    private class WanFailoverNetworkHookCallback implements HookCallback
    {
        /**
         * Get the name of the callback
         * 
         * @return The callback name
         */
        public String getName()
        {
            return "wan-failover-network-settings-change-hook";
        }

        /**
         * The main callback function
         * 
         * @param args
         *        The callback arguments
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof NetworkSettings)) {
                logger.warn("Invalid network settings: " + o);
                return;
            }

            NetworkSettings settings = (NetworkSettings) o;

            if (logger.isDebugEnabled()) logger.debug("network settings changed:" + settings);
            try {
                networkSettingsEvent(settings);
            } catch (Exception e) {
                logger.error("Unable to reconfigure the NAT app");
            }
        }
    }
    /**
     * Called when WAN Balancer settings have changed
     * Updates the active WAN interface ID based on the new balancer configuration
     */
    public void wanBalancerSettingsEvent()
    {
        if (this.wanFailoverTesterMonitor != null) {
            logger.info("Updating active WAN ID");
            this.wanFailoverTesterMonitor.updateActiveWanId();
        }
    }

    /**
     * Callback hook for changes to WAN Balancer settings
     * Monitors WAN Balancer configuration changes and updates failover behavior accordingly
     */
    private class WanFailoverWanBalancerHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         *
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "wan-failover-wan-balancer-settings-change-hook";
        }

        /**
         * Callback handler invoked when WAN Balancer settings change
         * Processes weight updates from WAN Balancer and triggers active WAN ID re-evaluation
         *
         * @param args
         *        The callback arguments - expects an int array of WAN weights as args[0]
         *        Empty arrays can be used to trigger re-evaluation without changing weights
         */
        public void callback(Object... args)
        {
            if (args.length > 0 && args[0] instanceof int[]) {
                int[] weights = (int[]) args[0];
                //set wanFailover wanbalancerweights as per wanBalancer passed argument
                WanFailoverApp.wanBalancerWeights = weights;
                logger.info("WAN Balancer weights updated (length= {} )", weights.length);
                wanBalancerSettingsEvent();
            }
        }
    }
    /**
     * Callback hook for requesting the active WAN IP
     * Responds to requests for the current active WAN interface by re-evaluating and returning the active WAN ID
     */
    private class ActiveWanIdHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         *
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "request-active-wan-ip";
        }

        /**
         * Callback handler invoked when active WAN IP is requested
         * Triggers active WAN ID re-evaluation to determine the current active WAN interface
         *
         * @param args
         *        The callback arguments (not used - can be called with no arguments)
         */
        public void callback(Object... args)
        {
            wanBalancerSettingsEvent();
        }
    }

    /**
     * Used to compare wan failover events
     */
    private static class WanFailoverEventComparator implements Comparator<WanFailoverEvent>
    {
        /*
         * order of the comparator, use a positive int for oldest first, and a
         * negative one for newest first
         */
        private final int order;

        /**
         * Constructor
         * 
         * @param order
         *        The order
         */
        private WanFailoverEventComparator(int order)
        {
            this.order = order;
        }

        /**
         * Comparison two wan failover events
         * 
         * @param event1
         *        First event to compare
         * @param event2
         *        Second event to compare
         * @return 0 if the events are the same
         */
        public int compare(WanFailoverEvent event1, WanFailoverEvent event2)
        {
            long diff = event1.getTimeStamp() - event2.getTimeStamp();

            /* Simple long to int conversion for huge time differences */
            if (diff < 0) return -1 * this.order;
            if (diff > 0) return 1 * this.order;
            return 0;
        }
    }
}
