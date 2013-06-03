/**
 * $Id: VpnNodeImpl.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.openvpn;

import java.net.URLEncoder;
import java.util.Random;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class OpenVpnNodeImpl extends NodeBase implements OpenVpnNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String GENERATE_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-certs";
    private static final String GENERATE_CLIENT_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-client-certs";
    private static final String IMPORT_CLIENT_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-import-config";

    private static final String STAT_PASS = "pass";
    private static final String STAT_CONNECT = "connect";

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final EventHandler handler;
    
    private EventLogQuery connectEventsQuery;

    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnManager openVpnManager = new OpenVpnManager();

    private OpenVpnSettings settings;

    private boolean isWebAppDeployed = false;
    
    public OpenVpnNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler          = new EventHandler( this );
        this.openVpnMonitor   = new OpenVpnMonitor( this );

        this.pipeSpec = new SoloPipeSpec( "openvpn", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 2);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };

        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new NodeMetric(STAT_CONNECT, I18nUtil.marktr("Clients Connected")));

        this.connectEventsQuery = new EventLogQuery(I18nUtil.marktr("Connections"), "SELECT start_time,client_name,max(end_time) as end_time,sum(rx_bytes) as rx_bytes,sum(tx_bytes) as tx_bytes,max(host(remote_address)) as remote_address FROM reports.openvpn_stats GROUP BY start_time,client_name ORDER BY start_time DESC");
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        OpenVpnSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/" + "settings_" + nodeID;

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

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        deployWebApp();
    }

    @Override
    protected void preStart()
    {
        super.preStart();

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

        this.openVpnMonitor.enable();
    }
    
    @Override
    protected void preStop()
    {
        super.preStop();

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

    @Override protected void postDestroy()
    {
        super.postDestroy();

        unDeployWebApp();
    }
    
    @Override
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
        this._setSettings( newSettings );
    }
    
    public void incrementPassCount()
    {
        this.incrementMetric(this.STAT_PASS);
    }

    public void incrementConnectCount()
    {
        this.incrementMetric(this.STAT_CONNECT);
    }

    public EventLogQuery[] getStatusEventsQueries()
    {
        return new EventLogQuery[] { this.connectEventsQuery };
    }

    public List<OpenVpnStatusEvent> getActiveClients()
    {
        return this.openVpnMonitor.getOpenConnectionsAsEvents();
    }

    public String getClientDistributionDownloadLink( String clientName, String format /* "zip", "exe" */ )
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
         * Generate the zip and exec files
         */
        this.openVpnManager.createClientDistribution( settings, client );
        
        /**
         * Return the proper link for the requested format
         */
        String fileName = null;
        if ( "exe".equals(format) ) fileName = "setup.exe";
        else if ( "zip".equals(format) ) fileName = "config.zip";
        else throw new RuntimeException("Unknown format: " + format);

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
        groups.add( group );
        newSettings.setGroups( groups );

        /**
         * Find an address pool that doesn't intersect anything
         */
        List<IPMaskedAddress> possibleAddressPools = new LinkedList<IPMaskedAddress>();
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

    private void _setSettings( OpenVpnSettings newSettings )
    {
        /**
         * Verify Settings
         */
        verifySettings( newSettings );

        /**
         * Sanitize Settings
         */
        sanitizeSettings( newSettings );

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(OpenVpnSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-openvpn/" + "settings_"  + nodeID, newSettings);
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
            if ( getRunState() == NodeSettings.NodeState.RUNNING ) {
                this.openVpnManager.restart();
            }
        } catch ( Exception exn ) {
            logger.error( "Could not save VPN settings", exn );
        }

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

    private void verifySettings( OpenVpnSettings newSettings )
    {
        /**
         * Verify no lists are null
         */
        if ( newSettings.getGroups() == null)
            throw new RuntimeException("Invalid Settings: null groups list");
        if ( newSettings.getRemoteClients() == null)
            throw new RuntimeException("Invalid Settings: null remote clients list");

        /**
         * Check each client.
         * Check that it has a good name, and the mapped group exist
         */
        for ( OpenVpnRemoteClient client : newSettings.getRemoteClients() ) {
            if ( client.getName() == null || client.getName().contains(" ") )
                throw new RuntimeException("Invalid Settings: Illegal client name: " + client.getName());
            boolean foundGroup = false;
            for ( OpenVpnGroup group : newSettings.getGroups() ) {
                if ( group.getGroupId() == client.getGroupId() )
                    foundGroup = true;
            }
            if (!foundGroup)
                throw new RuntimeException("Invalid Settings: Missing Group " + client.getGroupId() + " for client: " + client.getName());
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
    
}
