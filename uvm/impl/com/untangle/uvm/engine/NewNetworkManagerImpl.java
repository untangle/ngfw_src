/**
 * $Id: NewNetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NewNetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.PortForwardRule;

/**
 * The Network Manager handles all the network configuration
 */
public class NewNetworkManagerImpl implements NewNetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network";
    
    private NetworkSettings networkSettings;

    protected NewNetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;

        try {
            readSettings = settingsManager.load( NetworkSettings.class, settingsFilename );
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
        ExecManagerResult result;
        
        // stop interfaces
        result = UvmContextFactory.context().execManager().exec( "ifdown -a --exclude=lo" );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("ifdown -a: ");
            for ( String line : lines )
                logger.info("ifdown: " + line);
        } catch (Exception e) {}
    
        // Now sync those settings to the OS
        String cmd = "/usr/share/untangle-netd/bin/sync-settings.py -v -f " + settingsFilename + ".js";
        result = UvmContextFactory.context().execManager().exec( cmd );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Syncing settings to O/S: ");
            for ( String line : lines )
                logger.info("sync-settings.py: " + line);
        } catch (Exception e) {}

        // start interfaces
        result = UvmContextFactory.context().execManager().exec( "ifup -a --exclude=lo" );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("ifup -a: ");
            for ( String line : lines )
                logger.info("ifup: " + line);
        } catch (Exception e) {}
        
    }

    private synchronized void _setSettings( NetworkSettings newSettings )
    {
        /**
         * TODO:
         * validate settings
         * validatU: routes must route traffic to reachable destinations
         * validate: routes can not route traffic to self
         * validate: two interfaces can't have the identical subnet (192.168.1.2/24 external and 192.168.1.3/24 internal)
         */

        /**
         * TODO:
         * calculate system dev based on settings of each dev
         */
        sanitizeSettings( newSettings );

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
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.networkSettings).toString(2));} catch (Exception e) {}

        this.reconfigure();
    }
    
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();

        try {
            LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();
            InterfaceSettings external = new InterfaceSettings();
            external.setInterfaceId(1);
            external.setName("External");
            external.setPhysicalDev("eth0");
            external.setSystemDev("eth0");
            external.setSymbolicDev("br.eth0");
            external.setConfig("addressed");
            external.setV4ConfigType("static");
            external.setV4StaticAddress(InetAddress.getByName("172.16.2.60"));
            external.setV4StaticNetmask(InetAddress.getByName("255.255.0.0"));
            external.setV4StaticGateway(InetAddress.getByName("172.16.2.1"));
            external.setV4StaticDns1(InetAddress.getByName("172.16.2.1"));
            //external.setV4StaticAddress(InetAddress.getByName("10.0.0.60"));
            //external.setV4StaticNetmask(InetAddress.getByName("255.0.0.0"));
            //external.setV4StaticGateway(InetAddress.getByName("10.0.0.1"));
            //external.setV4StaticDns1(InetAddress.getByName("10.0.0.1"));
            external.setV6ConfigType("static");
            external.setIsWan(true);
        
            InterfaceSettings internal = new InterfaceSettings();
            internal.setInterfaceId(2);
            internal.setName("Internal");
            internal.setPhysicalDev("eth1");
            internal.setSystemDev("eth1");
            internal.setSymbolicDev("br.eth0");
            internal.setConfig("bridged");
            internal.setBridgedTo(1);

            InterfaceSettings foo3 = new InterfaceSettings();
            foo3.setInterfaceId(3);
            foo3.setName("Foo3");
            foo3.setPhysicalDev("eth2");
            foo3.setSystemDev("eth2");
            foo3.setSymbolicDev("br.eth0");
            foo3.setConfig("bridged");
            foo3.setBridgedTo(1);

            InterfaceSettings foo4 = new InterfaceSettings();
            foo4.setInterfaceId(4);
            foo4.setName("Foo4");
            foo4.setPhysicalDev("eth3");
            foo4.setSystemDev("eth3");
            foo4.setSymbolicDev("br.eth0");
            foo4.setConfig("bridged");
            foo4.setBridgedTo(1);

            interfaces.add(external);
            interfaces.add(internal);
            interfaces.add(foo3);
            interfaces.add(foo4);
        
            newSettings.setInterfaces(interfaces);

            LinkedList<PortForwardRule> portForwardRules = new LinkedList<PortForwardRule>();
            newSettings.setPortForwardRules( portForwardRules );

            LinkedList<NatRule> natRules = new LinkedList<NatRule>();
            newSettings.setNatRules( natRules );

            LinkedList<BypassRule> bypassRules = new LinkedList<BypassRule>();
            newSettings.setBypassRules( bypassRules );

            LinkedList<StaticRoute> staticRoutes = new LinkedList<StaticRoute>();
            newSettings.setStaticRoutes( staticRoutes );
        }
        catch (Exception e) {
            logger.error("Error creating Network Settings",e);
        }
        
        return newSettings;
    }

    private void reconfigure() 
    {
        logger.info("reconfigure()");
    }

    private void sanitizeSettings( NetworkSettings networkSettings)
    {
        /**
         * Fix NAT rule IDs
         */
        int idx = 0;
        for (NatRule rule : networkSettings.getNatRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Reset all symbolic devs to system devs
         * This is temporary XXX FIXME or is it?
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            intf.setSystemDev( intf.getPhysicalDev() );
            intf.setSymbolicDev( intf.getSystemDev() );
        }


        /**
         * Set system names
         */
        int pppCount = 0;
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if ( "pppoe".equals(intf.getV4ConfigType()) ) {
                //String ethNum = intf.getPhysicalDev().replaceAll( "[^\\d]", "" ); /* remove all alpha characters */
                //intf.setSystemDev( "ppp" + ethNum );
                intf.setSystemDev("ppp" + pppCount);
                pppCount++;
            }
        }
        
        /**
         * Determine if the interface is a bridge. If so set the symbolic device name
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( "bridged".equals(intf2.getConfig()) &&
                     intf2.getBridgedTo() != null &&
                     intf2.getBridgedTo().equals( intf.getInterfaceId() ) ) {
                        /* found an interface bridged to intf */
                        intf.setSymbolicDev("br." + intf.getPhysicalDev());
                        intf2.setSymbolicDev("br." + intf.getPhysicalDev());
                }
            }
        }
    }
}
