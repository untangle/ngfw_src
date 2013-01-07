/**
 * $Id: NewNetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NewNetworkManager;
import com.untangle.uvm.network.NetworkSettings;

/**
 * The Network Manager handles all the network configuration
 */
public class NewNetworkManagerImpl implements NewNetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private NetworkSettings networkSettings = new NetworkSettings();

    protected NewNetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network";

        try {
            readSettings = settingsManager.load( NetworkSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setNetworkSettings(defaultSettings());
        }
        else {
            this.networkSettings = readSettings;
            logger.debug("Loading Settings: " + this.networkSettings.toJSONString());
        }

        logger.info("Initialized NewNetworkManager");
    }
    
    /**
     * Get the network settings
     */
    public NetworkSettings getNetworkSettings()
    {
        return this.networkSettings;
    }

    /**
     * Set the network settings
     */
    public void setNetworkSettings( NetworkSettings newSettings )
    {
        this.networkSettings = newSettings;
        this._setSettings( this.networkSettings );
        
    }

    private void _setSettings( NetworkSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(NetworkSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.networkSettings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.networkSettings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }
    
    private NetworkSettings defaultSettings()
    {
        return new NetworkSettings();
    }

    private void reconfigure() 
    {
        logger.info("reconfigure()");
    }
    
}
