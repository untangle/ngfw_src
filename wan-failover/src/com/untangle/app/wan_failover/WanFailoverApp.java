/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

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
    private final Logger logger = Logger.getLogger(getClass());

    private static final String PINGABLE_HOSTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/wan-failover-pingable-hosts.sh";

    protected static final String STAT_CONNECTED = "connected";
    protected static final String STAT_DISCONNECTED = "disconnected";
    protected static final String STAT_CHANGE = "changed";
    protected static final String STAT_DISCONNECTS = "disconnects";
    protected static final String STAT_RECONNECTS = "reconnections";

    private static final long UPTIME_WINDOW_MAX = 31l * 24l * 60l * 60l * 1000l;

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private final WanFailoverNetworkHookCallback networkHookCallback = new WanFailoverNetworkHookCallback();

    private WanFailoverSettings settings = null;

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
        if (!isLicenseValid()) {
            throw new RuntimeException("Invalid License.");
        }

        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);

        if (WanFailoverApp.execManager == null) {
            WanFailoverApp.execManager = UvmContextFactory.context().createExecManager();
            WanFailoverApp.execManager.setLevel(org.apache.log4j.Level.INFO);
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

        if (this.wanFailoverTesterMonitor != null) {
            this.wanFailoverTesterMonitor.stop();
            this.wanFailoverTesterMonitor = null;
        }
        if (WanFailoverApp.execManager != null) {
            WanFailoverApp.execManager.close();
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
            long diff = event1.getTimeStamp().getTime() - event2.getTimeStamp().getTime();

            /* Simple long to int conversion for huge time differences */
            if (diff < 0) return -1 * this.order;
            if (diff > 0) return 1 * this.order;
            return 0;
        }
    }
}
