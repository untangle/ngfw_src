/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Collections;
import java.io.InputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppMetric;
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

    private static final String TUNNEL_LOG = "/var/log/uvm/tunnel.log";

    private TunnelVpnSettings settings = null;
    private TunnelVpnManager tunnelVpnManager = new TunnelVpnManager(this);

    private final TunnelVpnNetworkHookCallback networkHookCallback = new TunnelVpnNetworkHookCallback();
    
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
         * Number the rules
         */
        int idx = 0;
        for (TunnelVpnRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);
        }

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

        boolean networkSettingsChanged = false;
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
            networkSettingsChanged = true;
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
            networkSettingsChanged = true;
        }

        /**
         * sync these settings to the filesystem
         */
        syncToSystem((this.getRunState() == AppSettings.AppState.RUNNING));

        if (networkSettingsChanged) {
            UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
            // processes will be automatically restarted after this is complete by the network settings hook
        } else {
            // restart tunnels
            if(this.getRunState() == AppSettings.AppState.RUNNING)
                this.tunnelVpnManager.restartProcesses();
        }
        
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback );
    }

    @Override
    protected void postStart( boolean isPermanentTransition )
    {
        insertIptablesRules();
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
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback );
    }

    @Override
    protected void postDestroy()
    {
        syncToSystem( false );
        removeAllTunnelVirtualInterfaces();
    }
        
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        TunnelVpnSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/tunnel-vpn/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load( TunnelVpnSettings.class, settingsFilename );
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

            /**
             * If the settings file date is newer than the system files, re-sync them
             */
            if ( ! UvmContextFactory.context().isDevel() ) {
                File settingsFile = new File( settingsFilename );
                File outputFile = new File("/etc/untangle-netd/iptables-rules.d/350-tunnel-vpn");
                if (settingsFile.lastModified() > outputFile.lastModified() ) {
                    logger.warn("Settings file newer than interfaces files, Syncing...");
                    this.setSettings( readSettings );
                } 
            }
            
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
    
    public int getNewTunnelId()
    {
        return this.tunnelVpnManager.getNewTunnelId();
    }

    public String getLogFile()
    {
        File f = new File( TUNNEL_LOG );
        if (f.exists()) {
            String output = UvmContextFactory.context().execManager().execOutput("tail -n 200 " + TUNNEL_LOG);
            if ( output == null )
                return null;

            // remove strings that we don't want to show the users
            // these are strings/errors that are normal but scary to the user
            String[] lines = output.split("\\n");
            List<String> list = new LinkedList<String>();
            Collections.addAll(list, lines);
            for (Iterator<String> i = list.iterator(); i.hasNext();) {
                String str = i.next();
                if (str.contains("unable to redirect default gateway -- Cannot read current default gateway from system"))
                    i.remove();
                if (str.contains("MANAGEMENT:"))
                    i.remove();
            }
            String finalstr = list.stream().collect(Collectors.joining("\n"));
            return finalstr;
        }else{
            return null;
        }
    }

    private TunnelVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        TunnelVpnSettings settings = new TunnelVpnSettings();

        List<TunnelVpnRule> rules = new LinkedList<TunnelVpnRule>();
        TunnelVpnRule rule;
        List<TunnelVpnRuleCondition> conditions;
        TunnelVpnRuleCondition condition1;
        
        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Route all traffic over any available Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<TunnelVpnRuleCondition>();
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route all hosts tagged with \"tunnel\" over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<TunnelVpnRuleCondition>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.CLIENT_TAGGED,"tunnel");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route all hosts tagged with \"bittorrent-usage\" over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<TunnelVpnRuleCondition>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.CLIENT_TAGGED,"bittorrent-usage");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route TCP port 80 and port 443 over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<TunnelVpnRuleCondition>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.DST_PORT,"80,443");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);
        
        settings.setRules(rules);
        return settings;
    }

    public void importTunnelConfig(String filename, String provider, int tunnelId)
    {
        this.tunnelVpnManager.importTunnelConfig( filename, provider, tunnelId);
    }

    public List<org.json.JSONObject> getTunnelStates()
    {
        return this.tunnelVpnManager.getTunnelStates();
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

    /**
     * This removes all tunnels from network settings
     */
    private void removeAllTunnelVirtualInterfaces()
    {
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> nonTunnelInterfaces = networkSettings.getVirtualInterfaces().stream().filter(x -> !x.getIsWan()).collect(Collectors.toList());
        networkSettings.setVirtualInterfaces(nonTunnelInterfaces);
        UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
    }
    
    /**
     * Sync the settings to the filesystem
     */
    private void syncToSystem( boolean enabled )
    {
        logger.info("syncToSystem()...");

        /**
         * First we write a new 350-tunnel-vpn iptables script with the current settings
         */
        String appID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/" + "settings_"  + appID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String output = UvmContextFactory.context().execManager().execOutput(scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename);
        if ( !enabled )
            output += " -d";
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Sync Settings: " + line);

        if ( enabled )
            insertIptablesRules();
    }

    private void insertIptablesRules()
    {
        /**
         * Run the iptables script
         */
        String output = UvmContextFactory.context().execManager().execOutput("/etc/untangle-netd/iptables-rules.d/350-tunnel-vpn");
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Adding tunnel-vpn iptables: " + line);

    }

    private void networkSettingsEvent( NetworkSettings settings ) throws Exception
    {
        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Restarting tunnels...");

        if(this.getRunState() == AppSettings.AppState.RUNNING)
            this.tunnelVpnManager.restartProcesses();
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
                return new ExecManagerResult(1, "Tunnel VPN is missing the file" + ": " + fileItem.getName());
            }

            InputStream inputStream = fileItem.getInputStream();
            if ( inputStream == null ) {
                logger.info( "UploadTunnel is missing the file." );
                return new ExecManagerResult(1, "Tunnel VPN is missing the file" + ": " + fileItem.getName());
            }

            logger.info("Uploaded new tunnel config: " + fileItem.getName() + " " + argument );
            
            /* Write out the file. */
            File temp = null;
            OutputStream outputStream = null;
            try {
                String filename = fileItem.getName();
                if ( filename.endsWith(".zip") ) {
                    temp = File.createTempFile( "tunnel-vpn-newconfig-", ".zip" );
                } else if ( filename.endsWith(".conf") ) {
                    temp = File.createTempFile( "tunnel-vpn-newconfig-", ".conf" );
                } else if ( filename.endsWith(".ovpn") ) {
                    temp = File.createTempFile( "tunnel-vpn-newconfig-", ".ovpn" );
                } else {
                    return new ExecManagerResult(1, "Unknown file extension for Tunnel VPN" + ": " + fileItem.getName());
                }

                temp.deleteOnExit();
                outputStream = new FileOutputStream( temp );
            
                byte[] data = new byte[1024];
                int len = 0;
                while (( len = inputStream.read( data )) > 0 ) outputStream.write( data, 0, len );
            } catch ( IOException e ) {
                logger.warn( "Unable to validate client file.", e  );
                return new ExecManagerResult(1, e.getMessage() + ": " + fileItem.getName() );
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
                tunnelVpnManager.validateTunnelConfig( temp.getPath(), argument );
            } catch ( Exception e ) {
                logger.warn( "Unable to validate the client configuration", e );
                return new ExecManagerResult(1, e.getMessage() + ": " + fileItem.getName() );
            }
            
            return new ExecManagerResult(0, temp.getPath() + '&' + "Validated" + ": " + fileItem.getName() );
        }
    }

    private class TunnelVpnNetworkHookCallback implements HookCallback
    {
        public String getName()
        {
            return "tunnel-vpn-network-settings-change-hook";
        }

        public void callback( Object... args )
        {
            Object o = args[0];
            if ( ! (o instanceof NetworkSettings) ) {
                logger.warn( "Invalid network settings: " + o);
                return;
            }

            try {
                NetworkSettings settings = (NetworkSettings)o;
                networkSettingsEvent( settings );
            } catch( Exception e ) {
                logger.error( "Unable to reconfigure the NAT app" );
            }
        }
    }

}
