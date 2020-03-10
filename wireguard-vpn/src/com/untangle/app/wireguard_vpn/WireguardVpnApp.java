/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.util.LinkedList;
import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.network.NetworkSettings;

/**
 * The Wireguard VPN application connects to 3rd party VPN tunnel providers.
 */
public class WireguardVpnApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private WireguardVpnSettings settings = null;
    private WireguardVpnMonitor WireguardVpnMonitor;

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WireguardVpnApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public WireguardVpnSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final WireguardVpnSettings newSettings)
    {
        /**
         * Save the settings
         */
        String appID = this.getAppSettings().getId().toString();
        try {
            UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/" + "settings_"  + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    /**
     * Get the list of pipeline connectors
     * 
     * @return List of pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called after the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        this.WireguardVpnMonitor.start();
    }

    /**
     * Called before the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStop(boolean isPermanentTransition)
    {
        this.WireguardVpnMonitor.stop();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        WireguardVpnSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/tunnel-vpn/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(WireguardVpnSettings.class, settingsFilename);
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
     * Called to initialize application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        WireguardVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private WireguardVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        WireguardVpnSettings settings = new WireguardVpnSettings();

        return settings;
    }

}
