/**
 * $Id: SystemManagerImpl.java,v 1.00 2012/05/30 14:17:00 dmorris Exp $
 */
package com.untangle.uvm.engine;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SystemManager;
import com.untangle.uvm.SystemSettings;
import com.untangle.uvm.UvmState;
import com.untangle.uvm.node.IPAddress;

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
            newSettings.setInsideHttpEnabled( true );
            newSettings.setOutsideHttpsEnabled( true );
            if (UvmContextFactory.context().isDevel())
                newSettings.setOutsideHttpsAdministrationEnabled( true );
            else
                newSettings.setOutsideHttpsAdministrationEnabled( false );
            newSettings.setOutsideHttpsQuarantineEnabled( true );
            newSettings.setOutsideHttpsReportingEnabled( false );
            newSettings.setOutsideHttpsEnabled( true );
            newSettings.setHttpsPort( 443 );

            newSettings.setPublicUrlMethod( SystemSettings.PUBLIC_URL_EXTERNAL_IP );
            newSettings.setPublicUrlAddress( "hostname.example.com" );
            newSettings.setPublicUrlPort( 443 );

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

    /**
     * @return the public url for the box, this is the address (may be hostname or ip address)
     */
    public String getPublicUrl()
    {
        String httpsPortStr = Integer.toString(this.settings.getHttpsPort());
        String primaryAddressStr = "unconfigured.example.com";
        
        if ( SystemSettings.PUBLIC_URL_EXTERNAL_IP.equals(this.settings.getPublicUrlMethod()) ) {
            IPAddress primaryAddress = UvmContextFactory.context().networkManager().getPrimaryAddress();
            if ( primaryAddress == null ) {
                logger.warn("No WAN IP found");
            } else {
                primaryAddressStr = primaryAddress.getAddr().getHostAddress();
            }
        } else if ( SystemSettings.PUBLIC_URL_HOSTNAME.equals(this.settings.getPublicUrlMethod()) ) {
            if ( UvmContextFactory.context().networkManager().getHostname() == null ) {
                logger.warn("No hostname is configured");
            } else {
                primaryAddressStr = UvmContextFactory.context().networkManager().getHostname();
            }
        } else if ( SystemSettings.PUBLIC_URL_ADDRESS_AND_PORT.equals(this.settings.getPublicUrlMethod()) ) {
            if ( this.settings.getPublicUrlAddress() == null ) {
                logger.warn("No public address configured");
            } else {
                primaryAddressStr = this.settings.getPublicUrlAddress();
                httpsPortStr = Integer.toString(this.settings.getPublicUrlPort());
            }
        } else {
            logger.warn("Unknown public URL method: " + this.settings.getPublicUrlMethod() );
        }
        
        return primaryAddressStr + ":" + httpsPortStr;
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

        /* install support if necessary */
        try {
            if ( this.settings.getSupportEnabled()) {
                UvmContextFactory.context().toolboxManager().install("untangle-support-agent");
            } else {
                UvmContextFactory.context().toolboxManager().uninstall("untangle-support-agent");
            }
        } catch ( Exception ex ) {
            logger.error( "Unable to enable support", ex );
        }

        /* rebind HTTPS port if necessary */
        int port = this.settings.getHttpsPort();
        try {
            logger.info("Rebinding HTTPS port: " + port);
            UvmContextFactory.context().localAppServerManager().rebindExternalHttpsPort( port );
            logger.info("Rebinding HTTPS port done.");
        } catch ( Exception e ) {
            if ( !UvmContextFactory.context().state().equals( UvmState.RUNNING )) {
                logger.info( "unable to rebind port at startup, expected. ");
            } else {
                logger.warn( "unable to rebind https to port: " + port, e );
            }
        }


        UvmContextImpl.context().networkManager().refreshNetworkConfig();

    }
}
