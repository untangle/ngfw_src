/**
 * $Id$
 */
package com.untangle.node.wan_balancer;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.network.NetworkSettingsListener;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

public class WanBalancerApp extends NodeBase
{
    private static final String WAN_METRIC_PREFIX = "wan_";

    private final Logger logger = Logger.getLogger(getClass());

    private final EventHandler handler = new EventHandler( this );

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final NetworkListener listener;

    private WanBalancerSettings settings = null;
        
    private boolean inReactivation = false;

    public WanBalancerApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.listener = new NetworkListener();
        updateNodeMetrics( );

        /* premium = false because the handler is just used for monitoring & stats so it should still handle traffic even for hosts over the limit */
        this.connector = UvmContextFactory.context().pipelineFoundry().create("wan-balancer", this, null, this.handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 100, false );
        this.connectors = new PipelineConnector[] { connector };
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
    
    @Override
    public void initializeSettings()
    {
        WanBalancerSettings settings = defaultSettings();
        setSettings(settings);
    }
    
    public WanBalancerSettings getSettings()
    {
        return settings;
    }
    
    public synchronized void setSettings(final WanBalancerSettings newSettings)
    {
        /**
         * Number the rules
         */
        int idx = 0;
        for (RouteRule rule : newSettings.getRouteRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-node-wan-balancer/" + "settings_"  + nodeID + ".js";
        try {
            settingsManager.save( settingsFilename, newSettings );
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
         * Sync the new settings
         */
        syncToSystem();
    }

    @Override
    protected void preStart() 
    {
        if ( ! isLicenseValid()) {
            throw new RuntimeException( "Invalid License." );
        }

        UvmContextFactory.context().networkManager().registerListener( this.listener );

        syncToSystem();
    }

    @Override
    protected void postStop() 
    {
        removeIptablesRules();

        UvmContextFactory.context().networkManager().unregisterListener( this.listener );
    }
    
    @Override
    protected void postInit() 
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        WanBalancerSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-wan-balancer/" + "settings_" + nodeID + ".js";
        
        try {
            readSettings = settingsManager.load( WanBalancerSettings.class, settingsFileName );
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

    /**
     * Increments the corresponding interface counter on the faceplate in the UI
     */
    protected void incrementDstInterfaceMetric( int serverIntf )
    {
        if (( serverIntf < IntfConstants.MIN_INTF ) || ( serverIntf >= IntfConstants.MAX_INTF )) {
            logger.warn("Invalid interface: " + serverIntf);
            return;
        }

        NodeMetric metric = this.getMetric( WAN_METRIC_PREFIX + serverIntf );
        if ( metric == null ) {
            //this is true for all non-WAN dst interfaces, just return
            return;
        }

        Long value = metric.getValue();
        if (value == null)
            value = 0L;
        value = value + 1L;
        metric.setValue( value );
    }
    
    private WanBalancerSettings defaultSettings()
    {
        WanBalancerSettings settings = new WanBalancerSettings();
        int[] weights = new int[255];
        for ( int i = 0 ; i < weights.length ; i++ ) { weights[i] = 50; }
        
        settings.setWeights(weights);
        settings.setRouteRules(new LinkedList<RouteRule>());

        return settings;
    }

    private void networkSettingsEvent( NetworkSettings settings ) throws Exception
    {
        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Syncing new settings...");
        syncToSystem();

        // update faceplate metrics
        updateNodeMetrics();
    }

    private void updateNodeMetrics(  )
    {
        try {
            String uplinkName[] = new String[1 + IntfConstants.MAX_INTF];

            Map<String,String> i18n_map = UvmContextFactory.context().languageManager().getTranslations( "untangle" );

            I18nUtil.marktr( "Sessions on {0}" );
        
            /* Delete all of the interfaces that are not included. */
            for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
                int interfaceIndex = intf.getInterfaceId();
                NodeMetric metric = this.getMetric( WAN_METRIC_PREFIX + interfaceIndex );
            
                if ( intf.getIsWan() ) {
                    if ( metric == null) {
                        String metricName = ( intf.getName() == null ) ? "unknown" : intf.getName();
                        String metricDisplayName = I18nUtil.tr( "Sessions on {0}", metricName, i18n_map );
                        metric = new NodeMetric( WAN_METRIC_PREFIX + interfaceIndex, metricDisplayName );
                        logger.info("Adding Metric [" + interfaceIndex + "]: " + metricDisplayName);
                        this.addMetric(metric);
                    }
                } else {
                    if ( metric != null ) {
                        logger.info("Removing Metric [" + interfaceIndex + "]: " + metric.getDisplayName());
                        this.removeMetric( metric );
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception updating metrics.", e);
        }
    }

    private boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WAN_BALANCER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WAN_BALANCER_OLDNAME))
            return true;
        return false;
    }

    private void syncToSystem( )
    {
        /**
         * First we write a new 330-wan-balancer iptables script with the current settings
         */
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-node-wan-balancer/" + "settings_"  + nodeID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/wan-balancer-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String output = UvmContextFactory.context().execManager().execOutput(scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename);
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Sync Settings: " + line);

        /**
         * Run the iptables script
         */
        //remove old name
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle-netd/iptables-rules.d/330-splitd");
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle-netd/iptables-rules.d/330-wan-balancer");
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Adding wan-balancer iptables: " + line);

        /**
         * Run the route script
         */
        //remove old name
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle-netd/post-network-hook.d/040-splitd");
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle-netd/post-network-hook.d/040-wan-balancer");
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Adding wan-balancer routes  : " + line);
        
    }

    private void removeIptablesRules( )
    {
        /**
         * First we write a new 330-wan-balancer iptables script that just deletes existing rules and inserts no new rules
         */
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-node-wan-balancer/" + "settings_"  + nodeID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/wan-balancer-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        // -d options means disabled
        String output = UvmContextFactory.context().execManager().execOutput(scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename + " -d");
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("UnSync Settings: " + line);

        /**
         * Run the iptables script
         */
        //remove old name
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle-netd/iptables-rules.d/330-splitd");
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle-netd/iptables-rules.d/330-wan-balancer");
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Remove wan-balancer iptables: " + line);

        /**
         * Run the route script
         */
        //remove old name
        UvmContextFactory.context().execManager().exec("rm -f /etc/untangle-netd/post-network-hook.d/040-splitd");
        output = UvmContextFactory.context().execManager().execOutput("/etc/untangle-netd/post-network-hook.d/040-wan-balancer");
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Remove wan-balancer routes  : " + line);

    }
    
    private class NetworkListener implements NetworkSettingsListener
    {
        public void event( NetworkSettings settings )
        {
            if ( logger.isDebugEnabled()) logger.debug( "network settings changed:" + settings );
            try {
                networkSettingsEvent( settings );
            } catch( Exception e ) {
                logger.error( "Unable to reconfigure the NAT node" );
            }
        }
    }
}
