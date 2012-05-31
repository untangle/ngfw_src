/**
 * $Id: SystemManagerImpl.java,v 1.00 2012/05/30 14:17:00 dmorris Exp $
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SystemManager;
import com.untangle.uvm.SystemSettings;

/**
 * The Manager for system-related settings
 */
public class SystemManagerImpl implements SystemManager
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/untangle-vm-convert-system-settings.py";

    private final Logger logger = Logger.getLogger(this.getClass());

    private SystemSettings settings;

    protected SystemManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        SystemSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "system";

        try {
            readSettings = settingsManager.load( SystemSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + settingsFileName + ".js";
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( SystemSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            SystemSettings newSettings = new SystemSettings();
            newSettings.setIsInsideInsecureEnabled( true );
            newSettings.setIsOutsideHttpsEnabled( true );
            if (UvmContextFactory.context().isDevel())
                newSettings.setIsOutsideAdministrationEnabled( true );
            else
                newSettings.setIsOutsideAdministrationEnabled( false );
            newSettings.setIsOutsideQuarantineEnabled( true );
            newSettings.setIsOutsideReportingEnabled( false );
            newSettings.setIsOutsideHttpsEnabled( true );
            newSettings.setHttpsPort( 443 );

            this.setSettings(newSettings);
        }
        else {
            this.settings = readSettings;
            logger.info("Loading Settings: " + this.settings.toJSONString());
        }

        logger.info("Initialized SystemManager");
    }

    public SystemSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final SystemSettings settings)
    {
        this._setSettings( settings );
    }

    private void _setSettings( SystemSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(SystemSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "system", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }

    private void reconfigure() 
    {
        logger.info("reconfigure()");

        setSupportAccess( this.settings );
        UvmContextImpl.context().networkManager().refreshNetworkConfig();
    }

    private void setSupportAccess( SystemSettings systemSettings )
    {
        if ( systemSettings == null ) {
            logger.warn( "unable to set support access, address settings are not initialized." );            
            return;
        }
        
        try {
            if ( systemSettings.getIsSupportEnabled()) {
                UvmContextFactory.context().toolboxManager().install("untangle-support-agent");
            } else {
                UvmContextFactory.context().toolboxManager().uninstall("untangle-support-agent");
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to enable support", ex );
        }
    }
}
