/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.util.List;
import java.util.LinkedList;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

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
import com.untangle.uvm.servlet.UploadHandler;

public class TunnelVpnApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] { };

    private TunnelVpnSettings settings = null;
    private TunnelVpnManager tunnelVpnManager = new TunnelVpnManager(this);
    
    public TunnelVpnApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );

        UvmContextFactory.context().servletFileManager().registerUploadHandler(new TunnelUploadHandler());
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
        this.tunnelVpnManager.launchProcesses();
    }

    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        this.tunnelVpnManager.killProcesses();
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
    }

    @Override
    protected void postDestroy()
    {
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

        this.reconfigure();
    }

    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        TunnelVpnSettings settings = getDefaultSettings();

        setSettings(settings);
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

    private class TunnelUploadHandler implements UploadHandler
    {
        @Override
        public String getName()
        {
            return "tunnel_vpn";
        }

        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            if (fileItem == null) {
                logger.info( "UploadTunnel is missing the file." );
                return new ExecManagerResult(1, "UploadTunnel is missing the file.");
            }
            InputStream inputStream = fileItem.getInputStream();
            if ( inputStream == null ) {
                logger.info( "UploadTunnel is missing the file." );
                return new ExecManagerResult(1, "UploadTunnel is missing the file.");
            }

            logger.info("Uploaded new tunnel config: " + fileItem.getName() + " " + argument );
            
            /* Write out the file. */
            File temp = null;
            OutputStream outputStream = null;
            try {
                temp = File.createTempFile( "tunnel-vpn-newconfig-", ".zip" );
                temp.deleteOnExit();
                outputStream = new FileOutputStream( temp );
            
                byte[] data = new byte[1024];
                int len = 0;
                while (( len = inputStream.read( data )) > 0 ) outputStream.write( data, 0, len );
            } catch ( IOException e ) {
                logger.warn( "Unable to validate client file.", e  );
                return new ExecManagerResult(1, e.getMessage());
            } finally {
                try {
                    if ( outputStream != null ) outputStream.close();
                } catch ( Exception e ) {
                    logger.warn( "Error closing output stream", e );
                }

                try {
                    if ( inputStream != null ) inputStream.close();
                } catch ( Exception e ) {
                    logger.warn( "Error closing input stream", e );
                }
            }

            try {
                tunnelVpnManager.importTunnelConfig( temp.getPath(), argument );
            } catch ( Exception e ) {
                logger.warn( "Unable to install the client configuration", e );
                return new ExecManagerResult(1, e.getMessage());
            }
            
            return new ExecManagerResult(0, fileItem.getName());
        }
    }
}
