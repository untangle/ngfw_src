/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Arrays;
import java.util.Iterator;
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
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;

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

        /**
         * Synchronize settings with NetworkSettings
         * 1) Any tunnel interfaces that exists that aren't in network settings should be added
         * 2) Any tunnel interfaces that exist in network settings but not in tunnel VPN
         *    because they have been removed should be removed from network settins
         */
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        List<TunnelVpnTunnelSettings> missing = findTunnelsMissingFromNetworkSettings();
        if (missing.size() > 0) {
            for( TunnelVpnTunnelSettings tunnelSettings : missing ) {
                /**
                 * Set Network Settings (add new virtual interface)
                 */
                InterfaceSettings virtualIntf = new InterfaceSettings(tunnelSettings.getTunnelId(),tunnelSettings.getName());
                virtualIntf.setIsVirtualInterface(true);
                virtualIntf.setIsWan(true);
                virtualIntf.setConfigType(null);
                virtualIntf.setV4ConfigType(null);
                virtualIntf.setV4Aliases(null);
                virtualIntf.setV6ConfigType(null);
                virtualIntf.setV6Aliases(null);
                virtualIntf.setVrrpAliases(null);
                virtualInterfaces.add(virtualIntf);
                logger.info("Adding new virtual interface: " + tunnelSettings.getTunnelId() + " " + tunnelSettings.getName());

            }
            UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
        }
        List<InterfaceSettings> extra = findExtraVirtualInterfaces();
        if (extra.size() > 0) {
            for (Iterator<InterfaceSettings> i = virtualInterfaces.iterator(); i.hasNext();) {
                InterfaceSettings virtualIntf = i.next();
                Optional<InterfaceSettings> is = extra.stream().filter(x -> x.getInterfaceId() == virtualIntf.getInterfaceId()).findFirst();
                if(is.isPresent()) {
                    logger.info("Removing unused virtual interface: " + virtualIntf.getInterfaceId() + " " + virtualIntf.getName());
                    i.remove();
                }
            }
            UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
        }

        /**
         * Write the external resources & scripts
         */
        this.tunnelVpnManager.writeIptablesFiles( settings );
        if(this.getRunState() == AppSettings.AppState.RUNNING)
            this.tunnelVpnManager.restartProcesses();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
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

    /**
     * This finds all the tunnels that do not have corresponding virtual interfaces
     * in the current network settings.
     *
     * @returns a list of the tunnels missing virtual interfaces (never null)
     */
    private List<TunnelVpnTunnelSettings> findTunnelsMissingFromNetworkSettings()
    {
        List<TunnelVpnTunnelSettings> missing = new LinkedList<TunnelVpnTunnelSettings>();

        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        for(TunnelVpnTunnelSettings tunnelSettings : settings.getTunnels()) {
            Optional<InterfaceSettings> is = virtualInterfaces.stream().filter(x -> x.getInterfaceId() == tunnelSettings.getTunnelId()).findFirst();
            if(!is.isPresent()) {
                missing.add(tunnelSettings);
            }
        }
        return missing;
    }

    /**
     * This finds all the tunnel virtual interfaces in the current network settings
     * That do not have corresponding tunnel settings in the tunnel VPN settings
     *
     * @returns a list of the extra virtual interfaces (never null)
     */
    private List<InterfaceSettings> findExtraVirtualInterfaces()
    {
        List<InterfaceSettings> extra = new LinkedList<InterfaceSettings>();

        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        for(InterfaceSettings interfaceSetings : virtualInterfaces) {
            if ( !interfaceSetings.getIsWan() ) //ignore all the non-wan virtual interfaces (openvpn, ipsec, etc)
                continue;
            Optional<TunnelVpnTunnelSettings> is = settings.getTunnels().stream().filter(x -> x.getTunnelId() == interfaceSetings.getInterfaceId()).findFirst();
            if(!is.isPresent()) {
                extra.add(interfaceSetings);
            }
        }
        
        return extra;
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
