/**
 * $Id: VpnNodeImpl.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.openvpn;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.EventLogQuery;
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

    @Override protected PipeSpec[] getPipeSpecs()
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
