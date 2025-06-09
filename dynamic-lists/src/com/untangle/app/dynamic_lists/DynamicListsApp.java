/**
 * $Id$
 */

package com.untangle.app.dynamic_lists;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;


import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

import com.untangle.uvm.util.I18nUtil;


/**
 * The DynamicListsApp VPN application manages Dynamic Blocklists.
 */
public class DynamicListsApp extends AppBase
{
 
    private final Logger logger = LogManager.getLogger(getClass());

    private final String SettingsDirectory = "/dynamic-lists/";

   

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private DynamicListsSettings settings = null;
    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public DynamicListsApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
         //Manager for managing ipset logic
        //this. DynamicListsManager = new  DynamicListsManager(this);

    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public DynamicListsSettings getSettings()
    {
        return settings;
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
     *      The new settings
     * @param restart
     *      If true, restart
     */
    public void setSettings(final DynamicListsSettings newSettings, boolean restart)
    {

        this.settings = newSettings;

        try {
            UvmContextFactory.context().settingsManager().save( this.getSettingsFilename(), newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }
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

    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        DynamicListsSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/dynamic-lists/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(DynamicListsSettings.class, settingsFilename);
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

        }
            
    }


    /**
     * Called to uninitialize application settings
     */
    @Override
    protected void uninstall() {

    }

    /**
     * Called to initialize application settings
     */
    public void initializeSettings()
    {
      
        DynamicListsSettings settings = getDefaultSettings();
        setSettings(settings, true);
    }



 
    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private DynamicListsSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        DynamicListsSettings settings = new DynamicListsSettings();


        return settings;
    }


  
}
