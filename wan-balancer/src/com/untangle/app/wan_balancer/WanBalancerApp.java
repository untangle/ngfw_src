/**
 * $Id$
 */

package com.untangle.app.wan_balancer;

import java.util.LinkedList;
import java.util.Map;
import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * The Wan Balancer Application
 */
public class WanBalancerApp extends AppBase
{
    private static final String WAN_METRIC_PREFIX = "wan_";

    private final Logger logger = Logger.getLogger(getClass());

    private final EventHandler handler = new EventHandler(this);

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final WanBalancerNetworkHookCallback networkHookCallback = new WanBalancerNetworkHookCallback();

    private WanBalancerSettings settings = null;

    private boolean inReactivation = false;

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WanBalancerApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        updateAppMetrics();

        /*
         * premium = false because the handler is just used for monitoring &
         * stats so it should still handle traffic even for hosts over the limit
         */
        this.connector = UvmContextFactory.context().pipelineFoundry().create("wan-balancer", this, null, this.handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 100, false);
        this.connectors = new PipelineConnector[] { connector };
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
        WanBalancerSettings settings = defaultSettings();
        setSettings(settings);
    }

    /**
     * Get application settings
     * 
     * @return The application settings
     */
    public WanBalancerSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public synchronized void setSettings(final WanBalancerSettings newSettings)
    {
        /**
         * Number the rules
         */
        int idx = 0;
        for (RouteRule rule : newSettings.getRouteRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "wan-balancer/" + "settings_" + appID + ".js";
        try {
            settingsManager.save(settingsFilename, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        /**
         * Sync the new settings
         */
        syncToSystem(true);
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        if (!isLicenseValid()) {
            throw new RuntimeException("Invalid License.");
        }

        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);

        // if this is permanent write the enabled version of the scripts and run them
        if (isPermanentTransition) syncToSystem(true);
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);

        // if this is permanent write the disabled version of the scripts and run them
        if (isPermanentTransition) syncToSystem(false);
    }

    /**
     * Called after the application is removed
     */
    @Override
    protected void postDestroy()
    {
        syncToSystem(false);
    }

    /**
     * Called after the application is initialized
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        WanBalancerSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/wan-balancer/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(WanBalancerSettings.class, settingsFilename);
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

            /**
             * If the settings file date is newer than the system files, re-sync
             * them
             */
            if (!UvmContextFactory.context().isDevel()) {
                File settingsFile = new File(settingsFilename);
                File outputFile = new File("/etc/untangle/iptables-rules.d/330-wan-balancer-rules");
                if (settingsFile.lastModified() > outputFile.lastModified()) {
                    logger.warn("Settings file newer than interfaces files, Syncing...");
                    this.setSettings(readSettings);
                }
            }

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    /**
     * Increments the corresponding interface counter on the faceplate in the UI
     * 
     * @param serverIntf
     *        The interface
     */
    protected void incrementDstInterfaceMetric(int serverIntf)
    {
        if ((serverIntf < InterfaceSettings.MIN_INTERFACE_ID) || (serverIntf >= InterfaceSettings.MAX_INTERFACE_ID)) {
            logger.warn("Invalid interface: " + serverIntf);
            return;
        }

        AppMetric metric = this.getMetric(WAN_METRIC_PREFIX + serverIntf);
        if (metric == null) {
            //this is true for all non-WAN dst interfaces, just return
            return;
        }

        Long value = metric.getValue();
        if (value == null) value = 0L;
        value = value + 1L;
        metric.setValue(value);
    }

    /**
     * Create the default application settings
     * 
     * @return The settings
     */
    private WanBalancerSettings defaultSettings()
    {
        WanBalancerSettings settings = new WanBalancerSettings();
        int[] weights = new int[255];
        for (int i = 0; i < weights.length; i++) {
            weights[i] = 50;
        }

        settings.setWeights(weights);
        settings.setRouteRules(new LinkedList<RouteRule>());

        return settings;
    }

    /**
     * Called when network settings have changed
     * 
     * @param settings
     *        The new settings
     * @throws Exception
     */
    private void networkSettingsEvent(NetworkSettings settings) throws Exception
    {
        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Syncing new settings...");
        syncToSystem(true);

        // update faceplate metrics
        updateAppMetrics();
    }

    /**
     * Update application metrics
     */
    private void updateAppMetrics()
    {
        try {
            Map<String, String> i18n_map = UvmContextFactory.context().languageManager().getTranslations("untangle");

            I18nUtil.marktr("Sessions on {0}");

            /* Delete all of the interfaces that are not included. */
            for (InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces()) {
                int interfaceIndex = intf.getInterfaceId();
                AppMetric metric = this.getMetric(WAN_METRIC_PREFIX + interfaceIndex);

                if (intf.getIsWan()) {
                    if (metric == null) {
                        String metricName = (intf.getName() == null) ? "unknown" : intf.getName();
                        String metricDisplayName = I18nUtil.tr("Sessions on {0}", metricName, i18n_map);
                        metric = new AppMetric(WAN_METRIC_PREFIX + interfaceIndex, metricDisplayName);
                        logger.info("Adding Metric [" + interfaceIndex + "]: " + metricDisplayName);
                        this.addMetric(metric);
                    }
                } else {
                    if (metric != null) {
                        logger.info("Removing Metric [" + interfaceIndex + "]: " + metric.getDisplayName());
                        this.removeMetric(metric);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception updating metrics.", e);
        }
    }

    /**
     * Syncs settings to the system files and scipts
     * 
     * @param enabled
     *        Application enabled flag
     */
    private void syncToSystem(boolean enabled)
    {
        /**
         * First we write a new 330-wan-balancer iptables script with the
         * current settings
         */
        String appID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "wan-balancer/" + "settings_" + appID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/wan-balancer-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String output = UvmContextFactory.context().execManager().execOutput(scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename);
        if (!enabled) output += " -d";
        String lines[] = output.split("\\r?\\n");
        for (String line : lines)
            logger.info("Sync Settings: " + line);

        /**
         * Run the iptables script
         */
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle/iptables-rules.d/330-splitd"); //remove old name
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle/iptables-rules.d/330-wan-balancer");
        lines = output.split("\\r?\\n");
        for (String line : lines)
            logger.info("Adding wan-balancer iptables: " + line);

        /**
         * Run the route script
         */
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle/post-network-hook.d/040-splitd"); //remove old name
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle/post-network-hook.d/040-wan-balancer");
        lines = output.split("\\r?\\n");
        for (String line : lines)
            logger.info("Adding wan-balancer routes  : " + line);

    }

    /**
     * Callback for receiving notifications when settings have changed
     */
    private class WanBalancerNetworkHookCallback implements HookCallback
    {
        /**
         * Get the name of the callback
         * 
         * @return The callback name
         */
        public String getName()
        {
            return "wan-balancer-network-settings-change-hook";
        }

        /**
         * The callback
         * 
         * @param args
         *        The arguments
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
}
