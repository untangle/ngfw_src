/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.URLEncoder;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

public class OpenVpnNodeImpl extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String GENERATE_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-certs";
    private static final String GENERATE_CLIENT_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-client-certs";
    private static final String IMPORT_CLIENT_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-import-config";

    private static final String STAT_PASS = "pass";
    private static final String STAT_CONNECT = "connect";

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final EventHandler handler;
    
    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnManager openVpnManager = new OpenVpnManager();

    private final OpenVpnHookCallback openVpnHookCallback;
    
    private OpenVpnSettings settings;

    private boolean isWebAppDeployed = false;
    
    public OpenVpnNodeImpl( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.handler          = new EventHandler( this );
        this.openVpnMonitor   = new OpenVpnMonitor( this );
        this.openVpnHookCallback = new OpenVpnHookCallback();

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new AppMetric(STAT_CONNECT, I18nUtil.marktr("Clients Connected")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("openvpn", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 10, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        OpenVpnSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/openvpn/" + "settings_" + nodeID + ".js";

        try {
            readSettings = settingsManager.load( OpenVpnSettings.class, settingsFileName );
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
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * In OpenVPN deploy the webapp on init instead of start
         * because the webapp is needed for configuration while openvpn is off
         */
        deployWebApp();
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
    {
        try {
            this.openVpnManager.configure( settings );
            this.openVpnManager.restart();
        } catch( Exception e ) {
            logger.error("Error during startup", e);
            try {
                this.openVpnManager.stop();
            } catch ( Exception stopException ) {
                logger.error( "Unable to stop the openvpn process", stopException );
            }
            throw new RuntimeException(e);
        }

        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.openVpnHookCallback );

        this.openVpnMonitor.start();
        this.openVpnMonitor.enable();
    }
    
    @Override
    protected void preStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.openVpnHookCallback );
        
        try {
            this.openVpnMonitor.disable();
        } catch ( Exception e ) {
            logger.warn( "Error disabling openvpn monitor", e );
        }

        try {
            this.openVpnManager.stop();
        } catch ( Exception e ) {
            logger.warn( "Error stopping openvpn manager", e );
        }
    }

    @Override
    protected void postDestroy()
    {
        unDeployWebApp();
        
        // purge all settings files (but not the actual settings json file)
        UvmContextFactory.context().execManager().exec( "rm -f " + "/etc/openvpn/address-pool-assignments.txt" );
        UvmContextFactory.context().execManager().exec( "rm -f " + "/etc/openvpn/keys/*" );
        UvmContextFactory.context().execManager().exec( "rm -rf " + "/etc/openvpn/untangle-vpn" );
        UvmContextFactory.context().execManager().exec( "rm -rf " + "/etc/openvpn/data/*" );
        UvmContextFactory.context().execManager().exec( "rm -rf " + "/etc/openvpn/ccd/*" );
        UvmContextFactory.context().execManager().exec( "rm -f " + "/etc/openvpn/server.conf" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/remote-clients/*" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.key" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.pem" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.crt" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/index*" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/serial*" );
        UvmContextFactory.context().execManager().exec( "rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/server*" );
        UvmContextFactory.context().execManager().exec( "rm -rf " + System.getProperty("uvm.settings.dir") + "/openvpn/remote-servers/*" );
    }
    
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        setSettings(getDefaultSettings());

        ExecManagerResult result = UvmContextFactory.context().execManager().exec( GENERATE_CERTS_SCRIPT );

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_CERTS_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( GENERATE_CERTS_SCRIPT + ": " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to generate CA (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to generate CA");
        }
    }

    public OpenVpnSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings( OpenVpnSettings newSettings )
    {
        /**
         * Verify Settings
         */
        sanityCheckSettings( newSettings );

        /**
         * Sanitize Settings
         */
        sanitizeSettings( newSettings );

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "openvpn/" + "settings_"  + nodeID + ".js", newSettings );
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
         * Sync those settings
         */
        this.openVpnManager.configure( this.settings );
        
        /**
         * Restart the daemon
         */
        try {
            if ( getRunState() == AppSettings.AppState.RUNNING ) {
                this.openVpnManager.restart();
            }
        } catch ( Exception exn ) {
            logger.error( "Could not save VPN settings", exn );
        }
    }
    
    public void incrementPassCount()
    {
        this.incrementMetric(OpenVpnNodeImpl.STAT_PASS);
    }

    public void incrementConnectCount()
    {
        this.incrementMetric(OpenVpnNodeImpl.STAT_CONNECT);
    }

    public List<OpenVpnStatusEvent> getActiveClients()
    {
        return this.openVpnMonitor.getOpenConnectionsAsEvents();
    }

    public List<JSONObject> getRemoteServersStatus()
    {
        return _getRemoteServersStatus();
    }
    
    public String getClientDistributionDownloadLink( String clientName, String format /* "zip", "exe", "onc" */ )
    {
        /**
         * Find the client by that name
         */
        OpenVpnRemoteClient client = null;
        for ( final OpenVpnRemoteClient iclient : this.settings.getRemoteClients()) {
            if ( iclient.getName().equals( clientName )) client = iclient;
        }
        if ( client == null ) {
            throw new RuntimeException( "Client \"" + clientName +"\" not found." );
        }

        /**
         * Generate the certs ( if they already exist it will just return )
         */
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( GENERATE_CLIENT_CERTS_SCRIPT + " \""  + client.getName() + "\"" );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_CLIENT_CERTS_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( GENERATE_CLIENT_CERTS_SCRIPT + ": " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to generate client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to generate client config");
        }

        /**
         * Return the proper link for the requested format
         * and Generate the zip and exec files
         */
        String fileName = null;
        if ( "exe".equals(format) ) {
            this.openVpnManager.createClientDistributionExe( settings, client );
            fileName = "setup.exe";
        }
        else if ( "zip".equals(format) ) {
            this.openVpnManager.createClientDistributionZip( settings, client );
            fileName = "config.zip";
        }
        else if ( "onc".equals(format) ) {
            this.openVpnManager.createClientDistributionOnc( settings, client );
            fileName = "chrome.onc";
        }
        else {
            throw new RuntimeException("Unknown format: " + format);
        }

        String key = "";
        String clientNameStr = "";
        try {
            clientNameStr = URLEncoder.encode( clientName , "UTF-8" );
        } catch(java.io.UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding:",e);
        }

        return "/openvpn" + "/" + fileName + "?" + "client" + "=" + clientNameStr;
    }

    public String getClientDistributionUploadLink()
    {
        return "/openvpn" + "/uploadConfig?";
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
        OpenVpnSettings settings = getSettings();
        List<OpenVpnRemoteServer> servers = settings.getRemoteServers();
        for ( OpenVpnRemoteServer server : servers ) {
            if (sitename.equals(server.getName()))
                return;
        }
        OpenVpnRemoteServer server = new OpenVpnRemoteServer();
        server.setName( sitename );
        server.setEnabled( true );
        servers.add( server );
        settings.setRemoteServers( servers );
        setSettings( settings );

        /**
         * Restart the daemon
         */
        this.openVpnManager.configure( this.settings );
        this.openVpnManager.restart();
        
        return;
    }

    private OpenVpnSettings getDefaultSettings()
    {
        OpenVpnSettings newSettings = new OpenVpnSettings();

        newSettings.setSiteName( UvmContextFactory.context().networkManager().getNetworkSettings().getHostName() + "-" + (new Random().nextInt(10000)));
        
        /**
         * create a list of default exports - use all static non-WANs by default
         */
        List<OpenVpnExport> exports = new LinkedList<OpenVpnExport>();
        for( InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
            if ( intfSettings.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                continue;
            if ( intfSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                continue;
            if ( intfSettings.getIsWan() )
                continue;
            
            OpenVpnExport export = new OpenVpnExport();
            export.setEnabled( true );
            export.setName( intfSettings.getName() + " " + I18nUtil.marktr("primary network") );
            export.setNetwork( new IPMaskedAddress( intfSettings.getV4StaticAddress(), intfSettings.getV4StaticNetmask() ) );
            exports.add( export );
        }
        newSettings.setExports( exports );

        /**
         * create a list of default groups (just one)
         */
        List<OpenVpnGroup> groups = new LinkedList<OpenVpnGroup>();
        OpenVpnGroup group = new OpenVpnGroup();
        group.setGroupId(1);
        group.setName(I18nUtil.marktr("Default Group"));
        group.setFullTunnel( false );
        group.setPushDns( true );
        group.setPushDnsSelf( true );
        groups.add( group );
        newSettings.setGroups( groups );

        /**
         * Find an address pool that doesn't intersect anything
         */
        List<IPMaskedAddress> possibleAddressPools = new LinkedList<IPMaskedAddress>();
        Random rand = new Random();
        possibleAddressPools.add( new IPMaskedAddress( "172.16." + rand.nextInt( 250 ) + ".0/24") );
        possibleAddressPools.add( new IPMaskedAddress( "172.16." + rand.nextInt( 250 ) + ".0/24") );
        possibleAddressPools.add( new IPMaskedAddress( "172.16." + rand.nextInt( 250 ) + ".0/24") );
        possibleAddressPools.add( new IPMaskedAddress( "172.16.0.0/16" ) );
        possibleAddressPools.add( new IPMaskedAddress( "10.10.0.0/16" ) );
        possibleAddressPools.add( new IPMaskedAddress( "192.168.0.0/16" ) );
        possibleAddressPools.add( new IPMaskedAddress( "172.16.16.0/24" ) );
        possibleAddressPools.add( new IPMaskedAddress( "192.168.168.0/24" ) );
        possibleAddressPools.add( new IPMaskedAddress( "1.2.3.0/24" ) );
        for( IPMaskedAddress possibleAddressPool : possibleAddressPools ) {

            boolean foundConflict = false;
            for( InterfaceStatus intfStatus : UvmContextFactory.context().networkManager().getInterfaceStatus() ) {
                if ( intfStatus.getV4Address() == null || intfStatus.getV4Netmask() == null )
                    continue;
                IPMaskedAddress intfMaskedAddress = new IPMaskedAddress( intfStatus.getV4Address(), intfStatus.getV4PrefixLength() );
                if ( intfMaskedAddress.isIntersecting( possibleAddressPool ) )
                    foundConflict = true;
            }

            if ( ! foundConflict ) {
                newSettings.setAddressSpace( possibleAddressPool );
                break;
            }
        }
        
        return newSettings;
    }

    private void sanitizeSettings( OpenVpnSettings newSettings )
    {
        /**
         * Set group IDs for any new groups
         * New groups have ID of -1, set them to unused IDs
         */
        int highestKnownGroupId = 1;
        for ( OpenVpnGroup group : newSettings.getGroups() ) {
            if ( group.getGroupId() > highestKnownGroupId )
                highestKnownGroupId = group.getGroupId();
        }
        for ( OpenVpnGroup group : newSettings.getGroups() ) {
            if ( group.getGroupId() < 0 ) {
                group.setGroupId( highestKnownGroupId + 1 );
                highestKnownGroupId++;
            }
        }
    }

    private void sanityCheckSettings( OpenVpnSettings newSettings )
    {
        /**
         * Verify no lists are null
         */
        if ( newSettings.getGroups() == null)
            throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": null groups list");
        if ( newSettings.getRemoteClients() == null)
            throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": null remote clients list");

        /**
         * Check each client.
         * Check that it has a good name, and the mapped group exist
         */
        List<IPMaskedAddress> exportedNetworks = new LinkedList<IPMaskedAddress>();
        exportedNetworks.add( newSettings.getAddressSpace() );
        for ( OpenVpnRemoteClient client : newSettings.getRemoteClients() ) {
            if ( client.getName() == null || client.getName().contains(" ") )
                throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": Illegal client name: " + client.getName());
            boolean foundGroup = false;
            for ( OpenVpnGroup group : newSettings.getGroups() ) {
                if ( group.getGroupId() == client.getGroupId() )
                    foundGroup = true;
            }
            if (!foundGroup)
                throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": Missing Group " + client.getGroupId() + " for client: " + client.getName());

            if ( client.getExport() ) {
                String networks = client.getExportNetwork();
                for ( String network : networks.split(",") ) {
                    exportedNetworks.add( new IPMaskedAddress( network ) );
                }
            }
        }

        /**
         * Check that exported remote network do not conflict with Untangle addresses and other exports
         */
        List<IPMaskedAddress> currentlyUsed = UvmContextFactory.context().networkManager().getCurrentlyUsedNetworks( true, true, false );
        for ( IPMaskedAddress export : exportedNetworks ) {
            for ( IPMaskedAddress used : currentlyUsed ) {
                if ( export.isIntersecting( used ) ) {
                    throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": " + export + " " + I18nUtil.marktr("conflicts with address") + " " + used );
                }
            }
            for ( IPMaskedAddress export2 : exportedNetworks ) {
                if ( export == export2 )
                    continue;
                if ( export.isIntersecting( export2 ) ) {
                    throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": " + export + " " + I18nUtil.marktr("conflicts with address") + " " + export2 );
                }
            }
        }
        
    }
    
    private synchronized void deployWebApp()
    {
        if ( !isWebAppDeployed ) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet( "/openvpn", "openvpn", true)) {
                logger.debug( "Deployed openvpn web app" );
            }
            else logger.warn( "Unable to deploy openvpn web app" );
        }
        isWebAppDeployed = true;
    }

    private synchronized void unDeployWebApp()
    {
        if ( isWebAppDeployed ) {
            if( UvmContextFactory.context().tomcatManager().unloadServlet( "/openvpn" )) {
                logger.debug( "Unloaded openvpn web app" );
            } else logger.warn( "Unable to unload openvpn web app" );
        }
        isWebAppDeployed = false;
    }

    private void networkSettingsEvent( NetworkSettings settings ) throws Exception
    {
        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Syncing new settings...");

        // Several openvpn settings rely on network settings.
        // As such when the network settings change, re-sync the openvpn settings
        // They aren't critical though so don't restart the server.
        this.openVpnManager.configure( this.settings );
    }
    
    private List<JSONObject> _getRemoteServersStatus()
    {
        List<JSONObject> results = new LinkedList<JSONObject>();

        for ( OpenVpnRemoteServer server : settings.getRemoteServers() ) {
            try {
                JSONObject result = new JSONObject();
                File statusFile = new File( "/var/run/openvpn/" + server.getName() + ".status" );

                result.put( "name", server.getName() );
                result.put( "connected", false );
                result.put( "bytesRead", 0 );
                result.put( "bytesWritten", 0 );
            
                if ( ! statusFile.exists() ) {
                    results.add( result );
                    continue;
                } 

                BufferedReader reader = new BufferedReader(new FileReader(statusFile));
                String currentLine;
            
                while((currentLine = reader.readLine()) != null) {

                    // Look for TCP/UDP read bytes line
                    if ( currentLine.matches("^TCP/UDP read bytes,.*") ) {
                        String[] parts = currentLine.split(",");
                        if ( parts.length < 2 ) {
                            logger.warn("Malformed line in openvpn status: " + currentLine );
                            continue;
                        }
                    
                        long i;
                        try {
                            i = Long.parseLong( parts[1] );
                        } catch ( Exception e) {
                            logger.warn("Malformed int in openvpn status: " + currentLine );
                            continue;
                        }

                        if ( i == 0 ) {
                            // not connected
                            continue;
                        } else {
                            result.put( "connected", true );
                            result.put( "bytesRead", i );
                        }
                    }

                    // Look for TCP/UDP read bytes line
                    if ( currentLine.matches("^TCP/UDP write bytes,.*") ) {
                        String[] parts = currentLine.split(",");
                        if ( parts.length < 2 ) {
                            logger.warn("Malformed line in openvpn status: " + currentLine );
                            continue;
                        }
                    
                        long i;
                        try {
                            i = Long.parseLong( parts[1] );
                        } catch ( Exception e) {
                            logger.warn("Malformed int in openvpn status: " + currentLine );
                            continue;
                        }

                        result.put( "bytesWritten", i );
                    }
                }

                results.add( result );
            }
            catch ( Exception e ) {
                logger.warn("Malformed openvpn status file: " + "/var/run/openvpn/" + server.getName() + ".status", e );
            }
        }

        return results;
    }

    private class OpenVpnHookCallback implements HookCallback
    {
        public String getName()
        {
            return "openvpn-network-settings-change-hook";
        }
        
        public void callback( Object o )
        {
            if ( ! (o instanceof NetworkSettings) ) {
                logger.warn( "Invalid network settings: " + o);
                return;
            }
                 
            NetworkSettings settings = (NetworkSettings)o;

            if ( logger.isDebugEnabled())
                logger.debug( "network settings changed:" + settings );
            
            try {
                networkSettingsEvent( settings );
            } catch( Exception e ) {
                logger.error( "Unable to reconfigure the NAT node" );
            }
        }
    }
}
