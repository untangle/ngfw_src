/**
 * $Id$
 */
package com.untangle.app.shield;

import java.util.Set;
import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * ShieldApp is the hidden "shield" application
 */
public class ShieldApp extends AppBase
{
    private final Logger logger = Logger.getLogger(ShieldApp.class);

    private static final String SHIELD_RULES_FILE = "/etc/untangle/iptables-rules.d/600-shield";

    private final PipelineConnector[] connectors;

    private ShieldSettings settings;

    /**
     * ShieldApp creates a new ShieldApp
     * @param appSettings
     * @param appProperties
     */
    public ShieldApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.connectors = new PipelineConnector[] { };
    }

    /**
     * setSettings sets the current settings
     * @param newSettings
     */
    public void setSettings(final ShieldSettings newSettings)
    {
        /**
         * Set the Rule IDs
         */
        int idx = 0;
        for (ShieldRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "shield/" + "settings_"  + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Sync the settings to the system
         */
        syncToSystem( true );
    }

    /**
     * getSettings gets the current settings
     * @return ShieldSettings
     */
    public ShieldSettings getSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + this.getRunState() );

        return settings;
    }

    /**
     * getConnectors gets the current PipelineConnectors
     * @return PipelineConnector[]
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * initializeSettings initializes new settings
     */
    public void initializeSettings()
    {
        ShieldSettings settings = new ShieldSettings();
        logger.info("Initializing Settings...");
        settings.setVersion( Integer.valueOf(1) ); /* Current version */
        setSettings( settings );
    }

    /**
     * postInit hook
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        ShieldSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/shield/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( ShieldSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            ShieldSettings settings = new ShieldSettings();

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( settingsFileName );
            File interfacesFile = new File( SHIELD_RULES_FILE );
            if (settingsFile.lastModified() > interfacesFile.lastModified() ) {
                logger.warn("Settings file newer than rules files, Syncing...");
                syncToSystem( true );
            }
        }
    }

    /**
     * postStop hook
     * @param isPermanentTransition
     */
    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        if ( isPermanentTransition ) {
            // if its a permanent transiiton - disable the shield permanently
            syncToSystem( false );
        }
    }

    /**
     * postDestroy hook
     */
    @Override
    protected void postDestroy()
    {
        syncToSystem( false );
    }

    /**
     * syncToSystem the current settings 
     * @param enabled
     */
    private void syncToSystem( boolean enabled )
    {
        /**
         * First we write a new SHIELD_RULES_FILE iptables script with the current settings
         */
        String appID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "shield/" + "settings_"  + appID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/shield-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String cmd = scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename;
        if ( !enabled || this.settings.isShieldEnabled() != true )
            cmd += " -d"; // disable
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Sync Settings: " + line);

        /**
         * Run the iptables script
         */
        output = UvmContextFactory.context().execManager().execOutput(SHIELD_RULES_FILE);
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info(SHIELD_RULES_FILE + ": " + line);
    }
}
