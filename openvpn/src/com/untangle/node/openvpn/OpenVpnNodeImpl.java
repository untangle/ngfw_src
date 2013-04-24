/**
 * $Id: VpnNodeImpl.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.openvpn;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.util.I18nUtil;
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

    private static final String STAT_PASS = "pass";
    private static final String STAT_CONNECT = "connect";

    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private final EventHandler handler;
    
    private EventLogQuery connectEventsQuery;

    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnManager openVpnManager = new OpenVpnManager();

    private OpenVpnSettings settings;
    
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
    }

    @Override
    protected void preStart()
    {
        super.preStart();

        try {
            this.openVpnManager.configure( settings );
            this.openVpnManager.start( settings );
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

    @Override
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        setSettings(getDefaultSettings());
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

    public String getAdminDownloadLink( String clientName, ConfigFormat format )
    {
        return "FIXME";
        // boolean foundClient = false;
        // for ( final VpnClient client : this.settings.trans_getCompleteClientList()) {
        //     if ( !client.trans_getInternalName().equals( clientName ) &&
        //          !client.getName().equals( clientName )) continue;

        //     clientName = client.trans_getInternalName();

        //     /* Clear out the distribution email */
        //     client.setDistributionEmail( null );
        //     distributeRealClientConfig( client );
        //     foundClient = true;
        //     break;
        // }

        // if ( !foundClient ) {
        //     throw new Exception( "Unable to download unsaved clients <" + clientName +">" );
        // }

        // generateAdminClientKey();

        // String fileName = null;
        // switch ( format ) {
        // case SETUP_EXE : fileName = "setup.exe";  break;
        // case ZIP: fileName = "config.zip"; break;
        // }

        // String key = "";
        // String client = "";
        // try {
        //     key = URLEncoder.encode( this.adminDownloadClientKey , "UTF-8");
        //     client = URLEncoder.encode( clientName , "UTF-8");
        // } catch(java.io.UnsupportedEncodingException e) {
        //     logger.warn("Unsupported Encoding:",e);
        // }

        // return WEB_APP_PATH + "/" + fileName +
        //     "?" + Constants.ADMIN_DOWNLOAD_CLIENT_KEY + "=" + key +
        //     "&" + Constants.ADMIN_DOWNLOAD_CLIENT_PARAM + "=" + client;
    }


    private OpenVpnSettings getDefaultSettings()
    {
        OpenVpnSettings newSettings = new OpenVpnSettings();

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
    }
    
}
