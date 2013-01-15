/**
 * $Id: NewNetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NewNetworkManager;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.RouteRule;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.PortForwardRule;

/**
 * The Network Manager handles all the network configuration
 */
public class NewNetworkManagerImpl implements NewNetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private NetworkSettings networkSettings;

    protected NewNetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network";

        try {
            readSettings = settingsManager.load( NetworkSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setNetworkSettings(defaultSettings());
        }
        else {
            this.networkSettings = readSettings;
            logger.debug("Loading Settings: " + this.networkSettings.toJSONString());
        }

        logger.info("Initialized NewNetworkManager");
    }
    
    /**
     * Get the network settings
     */
    public NetworkSettings getNetworkSettings()
    {
        return this.networkSettings;
    }

    /**
     * Set the network settings
     */
    public void setNetworkSettings( NetworkSettings newSettings )
    {
        this._setSettings( newSettings );
        
    }

    private synchronized void _setSettings( NetworkSettings newSettings )
    {
        this.networkSettings = newSettings;

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(NetworkSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.networkSettings = newSettings;
        try {logger.warn("New Settings: \n" + new org.json.JSONObject(this.networkSettings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }
    
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();

        LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();
        interfaces.add( new InterfaceSettings( 1, "External", "eth0", "br.eth0", "dhcp", true) );
        interfaces.add( new InterfaceSettings( 2, "Internal", "eth1", "eth1", "static", false) );
        interfaces.add( new InterfaceSettings( 3, "DMZ", "eth2", "br.eth0", "bridged", false) );
        interfaces.add( new InterfaceSettings( 3, "Wireless", "eth3", "eth3", "static", false) );
        interfaces.add( new InterfaceSettings( 100, "External VLAN 2", "eth0.1", "eth0.1", "static", false) );
        newSettings.setInterfaces(interfaces);

        LinkedList<PortForwardRule> portForwards = new LinkedList<PortForwardRule>();
        newSettings.setPortForwards( portForwards );

        LinkedList<NatRule> natRules = new LinkedList<NatRule>();
        newSettings.setNatRules( natRules );

        LinkedList<BypassRule> bypassRules = new LinkedList<BypassRule>();
        newSettings.setBypassRules( bypassRules );

        LinkedList<RouteRule> routeRules = new LinkedList<RouteRule>();
        newSettings.setRouteRules( routeRules );

        LinkedList<StaticRoute> staticRoutes = new LinkedList<StaticRoute>();
        newSettings.setStaticRoutes( staticRoutes );
        
        return newSettings;
    }

    private void reconfigure() 
    {
        logger.info("reconfigure()");
    }
    
}
