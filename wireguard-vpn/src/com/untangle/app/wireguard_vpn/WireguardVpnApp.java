/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.util.List;
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
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * The Wireguard VPN application connects to 3rd party VPN tunnel providers.
 */
public class WireguardVpnApp extends AppBase
{
    private final static String WIREGUARD_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/wireguard-status";

    private final Logger logger = Logger.getLogger(getClass());

    private final String SettingsDirectory = "/wireguard-vpn/";

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private WireguardVpnSettings settings = null;
    private WireguardVpnMonitor WireguardVpnMonitor = null;
    private WireguardVpnManager WireguardVpnManager = null;

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
        WireguardVpnMonitor = new WireguardVpnMonitor(this);
        this.WireguardVpnManager = new WireguardVpnManager(this);
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
     * Get the wireguard Vpn manager
     * 
     * @return wireguard manager
     */
    public WireguardVpnManager getWireguardVpnManager(){
        logger.warn(this.WireguardVpnManager);
        return this.WireguardVpnManager;
    }

    /**
     * Return the settings filename
     * @return String of filename
     */
    public String getSettingsFilename()
    {
        return System.getProperty("uvm.settings.dir") + SettingsDirectory + "settings_"  + this.getAppSettings().getId().toString() + ".js";
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
        try {
            UvmContextFactory.context().settingsManager().save( this.getSettingsFilename(), newSettings );
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
     * Add a tunnel by publicKey
     * @param publicKey String of public key to add.
     */
    public void addTunnel(String publicKey)
    {
        this.WireguardVpnManager.addTunnel(publicKey);
    }

    /**
     * Delete a tunnel by publicKey
     * @param publicKey String of public key to delete.
     */
    public void deleteTunnel(String publicKey)
    {
        this.WireguardVpnManager.deleteTunnel(publicKey);
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
        try {
            this.WireguardVpnManager.configure();
            this.WireguardVpnManager.start();
        } catch (Exception e) {
            logger.error("Error during startup", e);
            try {
                this.WireguardVpnManager.stop();
            } catch (Exception stopException) {
                logger.error("Unable to stop the wireguard process", stopException);
            }
            throw new RuntimeException(e);
        }

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

        this.WireguardVpnManager.stop();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        WireguardVpnSettings readSettings = null;

        try {
            readSettings = settingsManager.load(WireguardVpnSettings.class, this.getSettingsFilename());
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

        String privateKey = this.getWireguardVpnManager().createPrivateKey().trim();
        String publicKey = this.getWireguardVpnManager().getPublicKey(privateKey).trim();
        settings.setPrivateKey(privateKey);
        settings.setPublicKey(publicKey);

        settings.setAddressPool(new IPMaskedAddress("172.16.0.0/16"));

        settings.setTunnels(new LinkedList<WireguardVpnTunnel>());

        return settings;
    }

    /**
     * Returns a list with the status of all wireguard tunnels
     *
     * @return A JSON string returned by the wireguard-status script
     */
    public String getTunnelStatus()
    {
        String result = UvmContextFactory.context().execManager().execOutput(WIREGUARD_STATUS_SCRIPT);
        return (result);
    }
}
