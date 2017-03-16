/*
 * $Id$
 */
package com.untangle.app.wan_failover;

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
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

public class WanFailoverApp extends AppBase
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

    private final WanFailoverNetworkHookCallback networkHookCallback = new WanFailoverNetworkHookCallback();

    private WanFailoverSettings settings = null;

    protected static ExecManager execManager = null;
    
    private WanFailoverTesterMonitor wanFailoverTesterMonitor = null;
    
    private boolean inReactivation = false;
    
    public WanFailoverApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.addMetric(new AppMetric(STAT_CONNECTED, I18nUtil.marktr("Connected WANs")));
        this.addMetric(new AppMetric(STAT_DISCONNECTED, I18nUtil.marktr("Disconnected WANs")));
        this.addMetric(new AppMetric(STAT_CHANGE, I18nUtil.marktr("WAN Change")));
        this.addMetric(new AppMetric(STAT_RECONNECTS, I18nUtil.marktr("Reconnects")));
        this.addMetric(new AppMetric(STAT_DISCONNECTS, I18nUtil.marktr("Disconnects")));
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
        if ( this.wanFailoverTesterMonitor == null )
            return new LinkedList<WanStatus>();
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
        String nodeID = this.getAppSettings().getId().toString();
        WanFailoverSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/wan-failover/" + "settings_" + nodeID + ".js";

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
    protected synchronized void preStart( boolean isPermanentTransition )
    {
        if ( ! isLicenseValid()) {
            throw new RuntimeException( "Invalid License." );
        }

        UvmContextFactory.context().hookManager().registerCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback );

        if (WanFailoverApp.execManager == null) {
            WanFailoverApp.execManager = UvmContextFactory.context().createExecManager();
            WanFailoverApp.execManager.setLevel( org.apache.log4j.Level.INFO );
        }

        if (this.wanFailoverTesterMonitor == null) 
            this.wanFailoverTesterMonitor = new WanFailoverTesterMonitor( this );

        this.wanFailoverTesterMonitor.start();
    }

    @Override
    protected synchronized void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().hookManager().unregisterCallback( com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback );

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
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "wan-failover/" + "settings_"  + nodeID + ".js", newSettings );
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

    private class WanFailoverNetworkHookCallback implements HookCallback
    {
        public String getName()
        {
            return "wan-failover-network-settings-change-hook";
        }

        public void callback( Object o )
        {
            if ( ! (o instanceof NetworkSettings) ) {
                logger.warn( "Invalid network settings: " + o);
                return;
            }
                 
            NetworkSettings settings = (NetworkSettings)o;

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
