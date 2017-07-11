/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.util.Random;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.PipelineConnector;

public class TunnelVpnApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String IMPORT_CLIENT_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-import-config";
    
    private final PipelineConnector[] connectors = new PipelineConnector[] { };

    private TunnelVpnSettings settings = null;

    private boolean isWebAppDeployed = false;
    
    public TunnelVpnApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );
    }

    public TunnelVpnSettings getSettings()
    {
        return settings;
    }

    public void setSettings(final TunnelVpnSettings newSettings)
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
        this.reconfigure();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        this.reconfigure();
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebApp();
    }
        
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        TunnelVpnSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/tunnel-vpn/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( TunnelVpnSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /* Deploy the web app on init so its available for configuration when off */
        deployWebApp();

        this.reconfigure();
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        TunnelVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    public void importClientConfig( String filename )
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( IMPORT_CLIENT_SCRIPT + " \""  + filename + "\"");

        String sitename = "siteName-" + (new Random().nextInt(10000));
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( IMPORT_CLIENT_SCRIPT + ": ");
            for ( String line : lines ) {
                logger.info( IMPORT_CLIENT_SCRIPT + ": " + line);

                if (line.contains("SiteName: ")) {
                    String[] tokens = line.split(" ");
                    if (tokens.length > 1)
                        sitename = tokens[1];
                }
            }
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to import client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to import client config");
        }

        /**
         * Add a new server in settings, if it does not exist
         */
        TunnelVpnSettings settings = getSettings();
        List<TunnelVpnTunnelSettings> tunnels = settings.getTunnels();
        for ( TunnelVpnTunnelSettings tunnelSettings : tunnels ) {
            if (sitename.equals(tunnelSettings.getName()))
                return;
        }
        TunnelVpnTunnelSettings tunnelSettings = new TunnelVpnTunnelSettings();
        tunnelSettings.setName( sitename );
        tunnelSettings.setEnabled( true );
        tunnels.add( tunnelSettings );
        settings.setTunnels( tunnels );
        setSettings( settings );

        logger.warn("FIXME");
        // /**
        //  * Restart the daemon
        //  */
        // this.openVpnManager.configure( this.settings );
        // this.openVpnManager.restart();
        
        return;
    }
    
    private TunnelVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        TunnelVpnSettings settings = new TunnelVpnSettings();
        
        return settings;
    }

    private void reconfigure() 
    {
        logger.info("Reconfigure()");
    }

    private synchronized void deployWebApp()
    {
        if ( !isWebAppDeployed ) {
            if (UvmContextFactory.context().tomcatManager().loadServlet( "/tunnel-vpn", "tunnel-vpn", true) != null) {
                logger.debug( "Deployed tunnel-vpn web app" );
            }
            else logger.warn( "Unable to deploy tunnel-vpn web app" );
        }
        isWebAppDeployed = true;
    }

    private synchronized void unDeployWebApp()
    {
        if ( isWebAppDeployed ) {
            if( UvmContextFactory.context().tomcatManager().unloadServlet( "/tunnel-vpn" )) {
                logger.debug( "Unloaded tunnel-vpn web app" );
            } else logger.warn( "Unable to unload tunnel-vpn web app" );
        }
        isWebAppDeployed = false;
    }
    
}
