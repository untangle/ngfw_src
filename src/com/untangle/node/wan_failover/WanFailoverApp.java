/*
 * $Id$
 */
package com.untangle.node.wan_failover;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManager;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.network.NetworkSettingsListener;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;

public class WanFailoverApp extends NodeBase
{    
    private final Logger logger = Logger.getLogger(getClass());

    private static final String PINGABLE_HOSTS_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/wan-failover-pingable-hosts.sh";

    protected static final String STAT_CONNECTED = "connected";
    protected static final String STAT_DISCONNECTED = "disconnected";
    protected static final String STAT_CHANGE = "changed";
    protected static final String STAT_DISCONNECTS = "disconnects";
    protected static final String STAT_RECONNECTS = "reconnections";

    private static final long UPTIME_WINDOW_MAX = 31l * 24l * 60l * 60l * 1000l;

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private final NetworkListener listener;

    private WanFailoverSettings settings = null;

    protected static ExecManager execManager = null;
    
    private WanFailoverTesterMonitor wanFailoverTesterMonitor = null;
    
    private boolean inReactivation = false;
    
    public WanFailoverApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.addMetric(new NodeMetric(STAT_CONNECTED, I18nUtil.marktr("Connected WANs")));
        this.addMetric(new NodeMetric(STAT_DISCONNECTED, I18nUtil.marktr("Disconnected WANs")));
        this.addMetric(new NodeMetric(STAT_CHANGE, I18nUtil.marktr("WAN Change")));
        this.addMetric(new NodeMetric(STAT_RECONNECTS, I18nUtil.marktr("Reconnects")));
        this.addMetric(new NodeMetric(STAT_DISCONNECTS, I18nUtil.marktr("Disconnects")));

        this.listener = new NetworkListener();
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }
    
    @Override
    public void initializeSettings()
    {
        WanFailoverSettings settings = new WanFailoverSettings();
        _setSettings(settings);
    }
    
    public String runTest( WanTestSettings test )
    {
        return this.wanFailoverTesterMonitor.runTest( test );
    }

    public List<String> getPingableHosts( int uplinkID )
    {
        InterfaceSettings intfSettings = UvmContextFactory.context().networkManager().findInterfaceId( uplinkID );

        if ( intfSettings == null ) {
            throw new RuntimeException( "Invalid interface id: " + uplinkID );
        }

        String output = UvmContextFactory.context().execManager().execOutput( PINGABLE_HOSTS_SCRIPT + " " + uplinkID + " " + intfSettings.getSymbolicDev() );
        
        if ( output.trim().length() == 0 ) {
            throw new RuntimeException( "Unable to determine pingable hosts." );
        }
        
        return Arrays.asList( output.split( "\n" ));
    }

    public List<WanStatus> getWanStatus( )
    {
        return this.wanFailoverTesterMonitor.getWanStatus();
    }

    public WanFailoverSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings( final WanFailoverSettings settings )
    {
        _setSettings(settings);
    }
    
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        WanFailoverSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-wan-failover/" + "settings_" + nodeID + ".js";

        try {
            readSettings = settingsManager.load( WanFailoverSettings.class, settingsFileName );
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

    }

    @Override
    protected synchronized void preStart()
    {
        if ( ! isLicenseValid()) {
            throw new RuntimeException( "Invalid License." );
        }

        /* Register a listener, this should hang out until the node is removed dies. */
        UvmContextFactory.context().networkManager().registerListener( this.listener );

        if (WanFailoverApp.execManager == null) {
            WanFailoverApp.execManager = UvmContextFactory.context().createExecManager();
            WanFailoverApp.execManager.setLevel( org.apache.log4j.Level.INFO );
        }

        if (this.wanFailoverTesterMonitor == null) 
            this.wanFailoverTesterMonitor = new WanFailoverTesterMonitor( this );

        this.wanFailoverTesterMonitor.start();
    }

    @Override
    protected synchronized void postStop()
    {
        UvmContextFactory.context().networkManager().unregisterListener( this.listener );

        if (this.wanFailoverTesterMonitor != null) {
            this.wanFailoverTesterMonitor.stop();
            this.wanFailoverTesterMonitor = null;
        }
        if (WanFailoverApp.execManager != null) {
            WanFailoverApp.execManager.close();
            WanFailoverApp.execManager = null;
        }
    }
    
    protected boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WAN_FAILOVER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.WAN_FAILOVER_OLDNAME))
            return true;
        return false;
    }

    // private methods --------------------------------------------------------
    
    private void _setSettings(final WanFailoverSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-node-wan-failover/" + "settings_"  + nodeID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        
        if ( this.wanFailoverTesterMonitor != null) 
            this.wanFailoverTesterMonitor.reconfigure();
    }

    public void networkSettingsEvent( NetworkSettings settings ) throws Exception
    {
        if ( this.wanFailoverTesterMonitor != null) 
            this.wanFailoverTesterMonitor.reconfigure();
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

    private static class WanFailoverEventComparator implements Comparator<WanFailoverEvent>
    {
        /* order of the comparator, use a positive int for oldest
         * first, and a negative one for newest first */
        private final int order;

        private WanFailoverEventComparator( int order )
        {
            this.order = order;
        }
        
        public int compare( WanFailoverEvent event1, WanFailoverEvent event2 )
        {
            long diff = event1.getTimeStamp().getTime() - event2.getTimeStamp().getTime();

            /* Simple long to int conversion for huge time differences */
            if ( diff < 0 ) return -1 * this.order;
            if ( diff > 0 ) return 1 * this.order;
            return 0;
        }
    }
}
