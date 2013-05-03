/**
 * $Id: NetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;
import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.NetworkSettingsListener;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.DeviceSettings;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.FilterRule;
import com.untangle.uvm.network.FilterRuleMatcher;
import com.untangle.uvm.network.QosSettings;
import com.untangle.uvm.network.QosRule;
import com.untangle.uvm.network.QosRuleMatcher;
import com.untangle.uvm.network.QosPriority;
import com.untangle.uvm.network.DnsSettings;
import com.untangle.uvm.network.DnsStaticEntry;
import com.untangle.uvm.network.DnsLocalServer;
import com.untangle.uvm.node.IPMaskedAddress;

/**
 * The Network Manager handles all the network configuration
 */
public class NetworkManagerImpl implements NetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private final String updateRulesScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-update-rules.sh";
    private final String deviceStatusScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-device-status.sh";

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network";
    private final String settingsFilenameBackup = "/etc/untangle-netd/network";
    
    private NetworkSettings networkSettings;

    /* networkListeners stores parties interested in being notified of network settings change */
    private LinkedList<NetworkSettingsListener> networkListeners = new LinkedList<NetworkSettingsListener>();
    
    protected NetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;

        try {
            readSettings = settingsManager.load( NetworkSettings.class, this.settingsFilename );
        } catch ( SettingsManager.SettingsException e ) {
            logger.warn( "Failed to load settings:", e );
        }

        /**
         * If its the development environment, try loading settings from /etc
         * We do this because we frequently nuke dist/ in the development environment
         * and this assures we keep the networking settings by saving them outside dist/
         */
        if ( readSettings == null && UvmContextFactory.context().isDevel() ) {
            try {
                // check for "backup" settings in /etc
                logger.info("Reading Network Settings from " + this.settingsFilenameBackup);
                readSettings = settingsManager.load( NetworkSettings.class, this.settingsFilenameBackup );
                logger.info("Reading Network Settings from " + this.settingsFilenameBackup + " = " + readSettings);
                
                if (readSettings == null) {
                    // check for "backup" settings in /usr/share/untangle/settings.backup/
                    String rootLocation = "/usr/share/untangle/settings.backup/untangle-vm/network";
                    logger.info("Reading Network Settings from " + rootLocation);
                    readSettings = settingsManager.load( NetworkSettings.class, rootLocation );
                    logger.info("Reading Network Settings from " + rootLocation + " = " + readSettings);
                }
                    
                if (readSettings != null)
                    settingsManager.save( NetworkSettings.class, this.settingsFilename, readSettings );
                    
            } catch ( SettingsManager.SettingsException e ) {
                logger.warn( "Failed to load settings:", e );
            }
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn( "No settings found - Initializing new settings." );
            this.setNetworkSettings( defaultSettings() );
        }
        else {
            //FIXME can remove me later - for dev box
            if ( readSettings.getQosSettings() == null )
                readSettings.setQosSettings( defaultQosSettings() );
            //FIXME can remove me later - for dev box
            if ( readSettings.getForwardFilterRules() == null )
                readSettings.setForwardFilterRules( defaultForwardFilterRules() );
            //FIXME can remove me later - for dev box
            if ( readSettings.getInputFilterRules() == null )
                readSettings.setInputFilterRules( defaultInputFilterRules() );
            //FIXME can remove me later - for dev box
            if ( readSettings.getDnsSettings() == null ) {
                DnsSettings dnsSettings = new DnsSettings();
                LinkedList<DnsStaticEntry> staticEntries = new LinkedList<DnsStaticEntry>();
                LinkedList<DnsLocalServer> localServers = new LinkedList<DnsLocalServer>();
                try {
                    staticEntries.add( new DnsStaticEntry( "chef" , InetAddress.getByName("10.0.0.10")) ); // XXX for testing
                    staticEntries.add( new DnsStaticEntry( "chef.metaloft.com" , InetAddress.getByName("10.0.0.10")) ); // XXX for testing
                    localServers.add( new DnsLocalServer( "metaloft.com", InetAddress.getByName("10.0.0.1")) ); // XXX for testing
                } catch (Exception e) {}
                dnsSettings.setStaticEntries( staticEntries );
                dnsSettings.setLocalServers( localServers );
                readSettings.setDnsSettings( dnsSettings );
            }

            checkForNewDevices( readSettings );
            
            this.networkSettings = readSettings;
            logger.debug( "Loading Settings: " + this.networkSettings.toJSONString() );
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        File settingsFile = new File(this.settingsFilename + ".js");
        File interfacesFile = new File("/etc/network/interfaces");
        if (settingsFile.lastModified() > interfacesFile.lastModified() ) {
            logger.warn("Settings file newer than interfaces files, Syncing...");
            this.setNetworkSettings( this.networkSettings );
        }
        
        logger.info( "Initialized NetworkManager" );
        this.getInterfaceStatus(1);
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

        // notify interested parties that the settings have changed
        callNetworkListeners();
    }

    /**
     * Insert the iptables rules for capturing traffic
     */
    protected void insertRules( )
    {
        int retCode = UvmContextFactory.context().execManager().execResult( "ln -fs " + this.updateRulesScript + " /etc/untangle-netd/iptables-rules.d/800-uvm" );
        if ( retCode < 0 )
            logger.warn("Unable to link iptables hook to update-rules script");
        
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( this.updateRulesScript );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("insert rules: ");
            for ( String line : lines )
                logger.info("insert rules: " + line);
        } catch (Exception e) {}
    }
    
    /**
     * Register a listener for network settings changes
     */
    public void registerListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.add( networkListener );
    }

    /**
     * Unregister a listener for network settings changes
     */
    public void unregisterListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.remove( networkListener );
    }

    public List<InterfaceSettings> getEnabledInterfaces()
    {
        LinkedList<InterfaceSettings> newList = new LinkedList<InterfaceSettings>();

        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null )
            return newList;
        
        for ( InterfaceSettings intf: this.networkSettings.getInterfaces() ) {
            if ( ! intf.getDisabled() )
                newList.add(intf);
        }

        return newList;
    }

    /**
     * Get the IP address of the first WAN interface
     */
    public InetAddress getFirstWanAddress()
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null ) {
            return null;
        }
        
        for ( InterfaceSettings intfSettings : this.networkSettings.getInterfaces() ) {
            if ( !intfSettings.getDisabled() && intfSettings.getIsWan() ) {
                return getInterfaceStatus( intfSettings.getInterfaceId() ).getV4Address();
            }
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified Id
     */
    public InterfaceSettings findInterfaceId( int interfaceId )
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
            if ( intf.getInterfaceId() == interfaceId )
                return intf;
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified systemDev
     */
    public InterfaceSettings findInterfaceSystemDev( String systemDev )
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
            if ( intf.getSystemDev().equals( systemDev ) )
                return intf;
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the first WAN
     */
    public InterfaceSettings findInterfaceFirstWan( )
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
            if ( !intf.getDisabled() && intf.getIsWan() )
                return intf;
        }

        return null;
    }
    
    /**
     * This method returns an address where the host should be able to access HTTP.
     * If HTTP is not reachable on this interface (like all WANs), it returns null.
     * If any error occurs it returns null.
     */
    public InetAddress getInterfaceHttpAddress( int clientIntfId )
    {
        int intfId = clientIntfId;
        
        if ( this.networkSettings == null ) {
            logger.warn("Missing network configuration");
            return null;
        }

        /**
         * FIXME: OpenVPN
         */
        // if ( intfId == 250) {
            // FIXME how to handle OpenVPN?
            //             OpenVpn openvpn = (OpenVpn) UvmContextFactory.context().nodeManager().node("untangle-node-openvpn");
            //             InetAddress addr = openvpn.getVpnServerAddress().getIp();
            //             return addr;
        // }

        
        InterfaceSettings intfSettings = findInterfaceId( intfId );
        if ( intfSettings == null ) {
            logger.warn("Failed to find interface " + intfId);
            return null;
        }

        /* WAN ports never have HTTP open */
        InterfaceSettings.ConfigType configType = intfSettings.getConfigType();
        boolean isWan = intfSettings.getIsWan();
        if ( configType == InterfaceSettings.ConfigType.ADDRESSED && isWan ) {
            //this is normal no error logged
            return null;
        }
        
        /**
         * If this interface is bridged with another, use the addr from the other
         */
        if ( configType == InterfaceSettings.ConfigType.BRIDGED ) {
            Integer bridgedTo = intfSettings.getBridgedTo();
            intfSettings = findInterfaceId( bridgedTo );

            if ( intfSettings == null ) {
                logger.warn("No Interface found for name: " + bridgedTo );
                return null;
            } 

            intfId = intfSettings.getInterfaceId();
        }

        InetAddress address = getInterfaceStatus( intfId ).getV4Address();
        return address;
    }
    
    /**
     * Returns the InterfaceStatus of the specified interface.
     * If there is an error or the status is unknown it returns an InterfaceStatus
     * with all null attributes.
     */
    public InterfaceStatus getInterfaceStatus( int interfaceId )
    {
        InterfaceStatus status = null;
        String filename = "/var/lib/untangle-netd/interface-" + interfaceId + "-status";

        try {
            status = UvmContextFactory.context().settingsManager().load( InterfaceStatus.class,  filename);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
            return null;
        }

        if (status == null) {
        	status = new InterfaceStatus(); // never return null
        }
        status.setInterfaceId(interfaceId); //Interface id must be set in all cases. It is not stored in interface-<interfaceId>-status.js file
        return status;
    }

    /**
     * Return the status of all addressed interfaces
     */
    public List<InterfaceStatus> getInterfaceStatus( )
    {
        List<InterfaceStatus> statuses = new LinkedList<InterfaceStatus>();
        for (InterfaceSettings intfSettings : this.networkSettings.getInterfaces()) {
            if (InterfaceSettings.ConfigType.ADDRESSED.equals(intfSettings.getConfigType())) {
                statuses.add( getInterfaceStatus( intfSettings.getInterfaceId() ) );
            }
        }
        return statuses;
    }
    
    /**
     * Returns a list of all the current device status'
     */
    @SuppressWarnings("unchecked") //JSON
    public List<DeviceStatus> getDeviceStatus( )
    {
        String argStr = "";
        for (InterfaceSettings intfSettings : this.networkSettings.getInterfaces()) {
            argStr = argStr + " " + intfSettings.getPhysicalDev() + " ";
        }

        String output = UvmContextFactory.context().execManager().execOutput(deviceStatusScript + argStr);
        List<DeviceStatus> entryList = null;
        try {
            entryList = (List<DeviceStatus>) ((UvmContextImpl)UvmContextFactory.context()).getSerializer().fromJSON(output);
        } catch (Exception e) {
            logger.warn("Unable to parse device status: ", e);
            logger.warn("Unable to parse device status: " + output);
            return null;
        }
        return entryList;
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
        sanitizeNetworkSettings( newSettings );

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(NetworkSettings.class, this.settingsFilename, newSettings);

            /**
             * If its the dev env also save to /etc
             */
            if ( UvmContextFactory.context().isDevel() ) {
                settingsManager.save(NetworkSettings.class, this.settingsFilenameBackup, newSettings);
            }
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

    private void checkForNewDevices( NetworkSettings netSettings )
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort " );
        String deviceNames[] = result.getOutput().split("\\r?\\n");

        /**
         * For each physical device look for the settings in interfaces
         * If not found, create some reasonable defaults
         */
        for ( String deviceName : deviceNames ) {
            boolean foundMatchingInterface = false;
            if ( netSettings.getInterfaces() != null ) {
                for ( InterfaceSettings interfaceSettings : netSettings.getInterfaces() ) {
                    if ( deviceName.equals( interfaceSettings.getPhysicalDev() ) )
                        foundMatchingInterface = true;
                }
            }
            if ( ! foundMatchingInterface ) {
                logger.warn("Found unmapped new physical device: " + deviceName);
                logger.warn("Creating new InterfaceSettings for " + deviceName + ".");

                InterfaceSettings interfaceSettings = new InterfaceSettings();
                interfaceSettings.setInterfaceId( nextFreeInterfaceId( netSettings ));
                interfaceSettings.setPhysicalDev( deviceName );
                interfaceSettings.setSystemDev( deviceName );
                interfaceSettings.setSymbolicDev( deviceName );
                interfaceSettings.setIsWan( false );
                interfaceSettings.setConfigType( InterfaceSettings.ConfigType.DISABLED );

                List<InterfaceSettings> currentList = netSettings.getInterfaces();
                if (currentList == null) currentList = new LinkedList<InterfaceSettings>();
                currentList.add( interfaceSettings );
                netSettings.setInterfaces( currentList );
            }
        }

        /**
         * For each physical device look for the settings in devices
         * If not found, create some reasonable defaults
         */
        for ( String deviceName : deviceNames ) {
            boolean foundMatchingDevice = false;
            if ( netSettings.getDevices() != null ) {
                for ( DeviceSettings deviceSettings : netSettings.getDevices() ) {
                    if ( deviceName.equals( deviceSettings.getDeviceName() ) )
                        foundMatchingDevice = true;
                }
            }
            if ( ! foundMatchingDevice ) {
                logger.warn("Found unmapped new physical device: " + deviceName);
                logger.warn("Creating new DeviceSettings for " + deviceName + ".");

                DeviceSettings deviceSettings = new DeviceSettings();
                deviceSettings.setDeviceName( deviceName );

                List<DeviceSettings> currentList = netSettings.getDevices();
                if (currentList == null) currentList = new LinkedList<DeviceSettings>();
                currentList.add( deviceSettings );
                netSettings.setDevices( currentList );
            }
        }
    }
    
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();

        newSettings.setHostName( UvmContextFactory.context().oemManager().getOemName().toLowerCase() );
        newSettings.setDomainName( "example.com" );
                                
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort " );
        String deviceNames[] = result.getOutput().split("\\r?\\n");
        
        try {
            LinkedList<DeviceSettings> devices = new LinkedList<DeviceSettings>();
            for (String deviceName : deviceNames) {
                DeviceSettings deviceSettings = new DeviceSettings();
                deviceSettings.setDeviceName( deviceName );
                devices.add( deviceSettings );
            }
            newSettings.setDevices( devices );
            
            LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();

            if (deviceNames.length > 0) {
                InterfaceSettings external = new InterfaceSettings();
                external.setInterfaceId( 1 );
                external.setName( "Extern\u00e1l" );
                external.setIsWan( true );
                external.setPhysicalDev( deviceNames[0] );
                external.setSystemDev( deviceNames[0] );
                external.setSymbolicDev( deviceNames[0] );
                external.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                external.setV4ConfigType( InterfaceSettings.V4ConfigType.AUTO );
                external.setV6ConfigType( InterfaceSettings.V6ConfigType.AUTO );
                external.setV4NatEgressTraffic( true );
                interfaces.add( external );
            }
        
            if (deviceNames.length > 1) {
                InterfaceSettings internal = new InterfaceSettings();
                internal.setInterfaceId( 2 );
                internal.setName( "Intern\u00e1l" );
                internal.setIsWan( false );
                internal.setPhysicalDev( deviceNames[1] );
                internal.setSystemDev( deviceNames[1] );
                internal.setSymbolicDev( deviceNames[1] );
                internal.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                internal.setV4ConfigType( InterfaceSettings.V4ConfigType.STATIC );
                internal.setV4StaticAddress( InetAddress.getByName("192.168.2.1") );
                internal.setV4StaticPrefix( 24 );
                internal.setDhcpEnabled( true );
                internal.setV6ConfigType( InterfaceSettings.V6ConfigType.STATIC ); 
                internal.setV6StaticAddress( InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7334") ); // FIXME what to set IPv6 to?
                internal.setV6StaticPrefixLength( 64 );
                internal.setBridgedTo( 1 );
                interfaces.add(internal);
            }

            for (int i = 2 ; i < deviceNames.length ; i++ ) {
                String[] greekNames = new String[]{"Alpha","Beta","Gamma","Delta","Epsilon","Zeta","Eta","Theta","Iota","Kappa","Lambda","Mu"};
                
                InterfaceSettings intf = new InterfaceSettings();
                intf.setInterfaceId( i + 1 );
                try {
                    intf.setName("Interface " + greekNames[i + 1]);
                } catch (Exception e) {
                    intf.setName("Interface " + (i + 1));
                }
                intf.setPhysicalDev(deviceNames[i]);
                intf.setSystemDev(deviceNames[i]);
                intf.setSymbolicDev(deviceNames[i]);
                intf.setConfigType( InterfaceSettings.ConfigType.DISABLED );
                interfaces.add( intf );
            }

            newSettings.setInterfaces(interfaces);

            LinkedList<PortForwardRule> portForwardRules = new LinkedList<PortForwardRule>();
            newSettings.setPortForwardRules( portForwardRules );

            LinkedList<NatRule> natRules = new LinkedList<NatRule>();
            newSettings.setNatRules( natRules );

            LinkedList<BypassRule> bypassRules = new LinkedList<BypassRule>();
            newSettings.setBypassRules( bypassRules );

            LinkedList<StaticRoute> staticRoutes = new LinkedList<StaticRoute>();
            newSettings.setStaticRoutes( staticRoutes );

            newSettings.setQosSettings( defaultQosSettings() );

            newSettings.setDnsSettings( new DnsSettings() );

            newSettings.setForwardFilterRules( defaultForwardFilterRules() );
            newSettings.setInputFilterRules( defaultInputFilterRules() );
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

    private void sanitizeNetworkSettings( NetworkSettings networkSettings)
    {
        /**
         * Fix rule IDs
         */
        int idx = 0;
        for (NatRule rule : networkSettings.getNatRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (PortForwardRule rule : networkSettings.getPortForwardRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (BypassRule rule : networkSettings.getBypassRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (StaticRoute rule : networkSettings.getStaticRoutes()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (FilterRule rule : networkSettings.getInputFilterRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (FilterRule rule : networkSettings.getForwardFilterRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Reset all symbolic devs to system devs
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            intf.setSystemDev( intf.getPhysicalDev() );
            intf.setSymbolicDev( intf.getSystemDev() );
        }


        /**
         * If hostname is null, set it to the default
         * Make sure hostname is not fully qualified
         */
        if (networkSettings.getHostName() == null)
            networkSettings.setHostName( UvmContextFactory.context().oemManager().getOemName().toLowerCase() );
        networkSettings.setHostName( networkSettings.getHostName().replaceAll("\\..*","") );
        
        /**
         * Set system names
         */
        int pppCount = 0;
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if ( InterfaceSettings.V4ConfigType.PPPOE.equals( intf.getV4ConfigType() ) ) {
                //String ethNum = intf.getPhysicalDev().replaceAll( "[^\\d]", "" ); /* remove all alpha characters */
                //intf.setSystemDev( "ppp" + ethNum );
                intf.setSystemDev("ppp" + pppCount);
                intf.setSymbolicDev("ppp" + pppCount);
                pppCount++;
            }
        }
        
        /**
         * Determine if the interface is a bridge. If so set the symbolic device name
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( InterfaceSettings.ConfigType.BRIDGED.equals( intf2.getConfigType() ) &&
                     intf2.getBridgedTo() != null &&
                     intf2.getBridgedTo().equals( intf.getInterfaceId() ) ) {
                        /* found an interface bridged to intf */
                        intf.setSymbolicDev("br." + intf.getPhysicalDev());
                        intf2.setSymbolicDev("br." + intf.getPhysicalDev());
                }
            }
        }

        /**
         * Sanitize the interface Settings
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            sanitizeInterfaceSettings( intf );
        }
        
    }

    private void sanitizeInterfaceSettings( InterfaceSettings interfaceSettings)
    {
        /**
         * If DHCP settings are enabled, but settings arent picked, fill in reasonable defaults
         */
        if ( interfaceSettings.getDhcpEnabled() != null &&
             interfaceSettings.getDhcpEnabled() == Boolean.TRUE &&
             interfaceSettings.getDhcpRangeStart() == null ) {
            initializeDhcpDefaults( interfaceSettings );
        }

        /**
         * If its bridge, unset isWan
         * This is a bummer because if you change to bridge and back to static in the UI
         * It will not have its previous configuration, but this is safer because is many places
         * we check the value of isWan without checking the configuration
         */
        if (interfaceSettings.getConfigType() !=  InterfaceSettings.ConfigType.ADDRESSED)
            interfaceSettings.setIsWan( false );
    }

    /**
     * This chooses "reasonable" DHCP defaults if dhcp is enabled on the given interface
     */
    private void initializeDhcpDefaults( InterfaceSettings interfaceSettings )
    {
        if (! interfaceSettings.getDhcpEnabled())
            return;

        try {
            InetAddress addr = interfaceSettings.getV4StaticAddress();
            InetAddress mask = interfaceSettings.getV4StaticNetmask();
            if (addr == null || mask == null) {
                logger.warn("Missing interface settings, disabling DHCP");
                interfaceSettings.setDhcpEnabled( false );
            }

            InetAddress start;
            InetAddress end;
            IPMaskedAddress maddr = new IPMaskedAddress( addr, mask );

            // This will only configure /24 or larger network, the logic for
            // smaller networks is complicated and isn't really worth it.
            if (maddr.getPrefixLength() <= 24) {
                start = InetAddress.getByName("0.0.0.100");
                end   = InetAddress.getByName("0.0.0.200");
            } else {
                start = InetAddress.getByName("0.0.0.16");
                end   = InetAddress.getByName("0.0.0.99");
            }

            // bitwise OR the selected start and end with the base address
            InetAddress baseAddr = maddr.getMaskedAddress();
            byte[] maskBytes = mask.getAddress();
            byte[] baseAddrStartBytes = baseAddr.getAddress();
            byte[] baseAddrEndBytes   = baseAddr.getAddress();
            byte[] startBytes = start.getAddress();
            byte[] endBytes   = end.getAddress();
            for ( int i = 0 ; i < baseAddrStartBytes.length ; i++ ) {
                baseAddrStartBytes[i] = (byte) ( baseAddrStartBytes[i] | ( startBytes[i] & (~maskBytes[i])) );
                baseAddrEndBytes[i]   = (byte) ( baseAddrEndBytes[i] | ( endBytes[i] & (~maskBytes[i])) );
            }

            InetAddress suggestedStart = InetAddress.getByAddress( baseAddrStartBytes );
            InetAddress suggestedEnd   = InetAddress.getByAddress( baseAddrEndBytes );

            interfaceSettings.setDhcpRangeStart( suggestedStart );
            interfaceSettings.setDhcpRangeEnd( suggestedEnd );
            interfaceSettings.setDhcpLeaseDuration( 60*60 ); // 1 hours (dnsmasq doc suggested value)
        }
        catch (Exception e) {
            logger.warn("Exception initializing DHCP Address: ",e);
            interfaceSettings.setDhcpEnabled( false );
        }
    }

    /**
     * Call all the networkListeners
     */
    private void callNetworkListeners()
    {
        logger.debug( "Calling network listeners." );
        for ( NetworkSettingsListener listener : this.networkListeners ) {
            if ( logger.isDebugEnabled()) logger.debug( "Calling listener: " + listener );

            try {
                listener.event( this.networkSettings );
            } catch ( Exception e ) {
                logger.error( "Exception calling listener", e );
            }
        }
        logger.debug( "Done calling network listeners." );
    }

    private QosSettings defaultQosSettings()
    {
        QosSettings qosSettings = new QosSettings();

        qosSettings.setQosEnabled( false );
        qosSettings.setPingPriority( 1 );
        qosSettings.setDnsPriority( 1 );
        qosSettings.setSshPriority( 0 );
        qosSettings.setOpenvpnPriority( 0 );

        List<QosRule> qosRules = new LinkedList<QosRule>();

        /**
         * Add SIP rule
         */
        QosRule qosRule1 = new QosRule();
        qosRule1.setEnabled( true );
        qosRule1.setDescription( "VoIP (SIP) Traffic" );
        qosRule1.setPriority( 1 );
        
        List<QosRuleMatcher> rule1Matchers = new LinkedList<QosRuleMatcher>();
        QosRuleMatcher rule1Matcher1 = new QosRuleMatcher();
        rule1Matcher1.setMatcherType(QosRuleMatcher.MatcherType.DST_PORT);
        rule1Matcher1.setValue("5060");
        QosRuleMatcher rule1Matcher2 = new QosRuleMatcher();
        rule1Matcher2.setMatcherType(QosRuleMatcher.MatcherType.PROTOCOL);
        rule1Matcher2.setValue("tcp,udp");
        rule1Matchers.add(rule1Matcher1);
        rule1Matchers.add(rule1Matcher2);

        qosRule1.setMatchers( rule1Matchers );


        /**
         * Add IAX rule
         */
        QosRule qosRule2 = new QosRule();
        qosRule2.setEnabled( true );
        qosRule2.setDescription( "VoIP (IAX) Traffic" );
        qosRule2.setPriority( 1 );
        
        List<QosRuleMatcher> rule2Matchers = new LinkedList<QosRuleMatcher>();
        QosRuleMatcher rule2Matcher1 = new QosRuleMatcher();
        rule2Matcher1.setMatcherType(QosRuleMatcher.MatcherType.DST_PORT);
        rule2Matcher1.setValue("4569");
        QosRuleMatcher rule2Matcher2 = new QosRuleMatcher();
        rule2Matcher2.setMatcherType(QosRuleMatcher.MatcherType.PROTOCOL);
        rule2Matcher2.setValue("tcp,udp");
        rule2Matchers.add(rule2Matcher1);
        rule2Matchers.add(rule2Matcher2);

        qosRule2.setMatchers( rule2Matchers );

        /**
         * Add Rules
         */
        qosRules.add( qosRule1 );
        qosRules.add( qosRule2 );
        qosSettings.setQosRules( qosRules );

        /**
         * Create priorities
         */
        List<QosPriority> qosPriorities = new LinkedList<QosPriority>();

        qosPriorities.add( new QosPriority( 1, "Very High",      50, 100, 50, 100) );
        qosPriorities.add( new QosPriority( 2, "High",           25, 100, 25, 100) );
        qosPriorities.add( new QosPriority( 3, "Medium",         12, 100, 12, 100) );
        qosPriorities.add( new QosPriority( 4, "Low",             6, 100,  6, 100) );
        qosPriorities.add( new QosPriority( 5, "Limited",         3,  75,  3, 75) );
        qosPriorities.add( new QosPriority( 6, "Limited More",    2,  50,  2, 50) );
        qosPriorities.add( new QosPriority( 7, "Limited Severly", 2,  10,  2, 10) );

        qosSettings.setQosPriorities( qosPriorities );
        
        
        return qosSettings;
    }

    private List<FilterRule> defaultInputFilterRules()
    {
        List<FilterRule> rules = new LinkedList<FilterRule>();

        // enabled in dev env
        // disabled but there in normal env
        FilterRule filterRuleSsh = new FilterRule();
        filterRuleSsh.setEnabled( UvmContextFactory.context().isDevel() );
        filterRuleSsh.setDescription( "Allow SSH" );
        filterRuleSsh.setBlocked( false );
        List<FilterRuleMatcher> ruleSshMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleSshMatcher1 = new FilterRuleMatcher();
        ruleSshMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleSshMatcher1.setValue("22");
        FilterRuleMatcher ruleSshMatcher2 = new FilterRuleMatcher();
        ruleSshMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleSshMatcher2.setValue("tcp");
        ruleSshMatchers.add(ruleSshMatcher1);
        ruleSshMatchers.add(ruleSshMatcher2);
        filterRuleSsh.setMatchers( ruleSshMatchers );

        FilterRule filterRulePing = new FilterRule();
        filterRulePing.setEnabled( true );
        filterRulePing.setDescription( "Allow PING" );
        filterRulePing.setBlocked( false );
        List<FilterRuleMatcher> rulePingMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher rulePingMatcher1 = new FilterRuleMatcher();
        rulePingMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        rulePingMatcher1.setValue("icmp");
        rulePingMatchers.add(rulePingMatcher1);
        filterRulePing.setMatchers( rulePingMatchers );
        
        FilterRule filterRuleHttp = new FilterRule();
        filterRuleHttp.setEnabled( true );
        filterRuleHttp.setDescription( "Allow HTTP on non-WANs" );
        filterRuleHttp.setBlocked( false );
        List<FilterRuleMatcher> ruleHttpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleHttpMatcher1 = new FilterRuleMatcher();
        ruleHttpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleHttpMatcher1.setValue("80");
        FilterRuleMatcher ruleHttpMatcher2 = new FilterRuleMatcher();
        ruleHttpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleHttpMatcher2.setValue("tcp");
        FilterRuleMatcher ruleHttpMatcher3 = new FilterRuleMatcher();
        ruleHttpMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleHttpMatcher3.setValue("non_wan");
        ruleHttpMatchers.add(ruleHttpMatcher1);
        ruleHttpMatchers.add(ruleHttpMatcher2);
        ruleHttpMatchers.add(ruleHttpMatcher3);
        filterRuleHttp.setMatchers( ruleHttpMatchers );

        FilterRule filterRuleDns = new FilterRule();
        filterRuleDns.setEnabled( true );
        filterRuleDns.setDescription( "Allow DNS on non-WANs" );
        filterRuleDns.setBlocked( false );
        List<FilterRuleMatcher> ruleDnsMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleDnsMatcher1 = new FilterRuleMatcher();
        ruleDnsMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleDnsMatcher1.setValue("53");
        FilterRuleMatcher ruleDnsMatcher2 = new FilterRuleMatcher();
        ruleDnsMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleDnsMatcher2.setValue("tcp,udp");
        FilterRuleMatcher ruleDnsMatcher3 = new FilterRuleMatcher();
        ruleDnsMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleDnsMatcher3.setValue("non_wan");
        ruleDnsMatchers.add(ruleDnsMatcher1);
        ruleDnsMatchers.add(ruleDnsMatcher2);
        ruleDnsMatchers.add(ruleDnsMatcher3);
        filterRuleDns.setMatchers( ruleDnsMatchers );

        FilterRule filterRuleDhcp = new FilterRule();
        filterRuleDhcp.setEnabled( true );
        filterRuleDhcp.setDescription( "Allow DHCP on non-WANs" );
        filterRuleDhcp.setBlocked( false );
        List<FilterRuleMatcher> ruleDhcpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleDhcpMatcher1 = new FilterRuleMatcher();
        ruleDhcpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleDhcpMatcher1.setValue("67");
        FilterRuleMatcher ruleDhcpMatcher2 = new FilterRuleMatcher();
        ruleDhcpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleDhcpMatcher2.setValue("udp");
        FilterRuleMatcher ruleDhcpMatcher3 = new FilterRuleMatcher();
        ruleDhcpMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleDhcpMatcher3.setValue("non_wan");
        ruleDhcpMatchers.add(ruleDhcpMatcher1);
        ruleDhcpMatchers.add(ruleDhcpMatcher2);
        ruleDhcpMatchers.add(ruleDhcpMatcher3);
        filterRuleDhcp.setMatchers( ruleDhcpMatchers );
        
        FilterRule filterRuleHttps = new FilterRule();
        filterRuleHttps.setEnabled( true );
        filterRuleHttps.setDescription( "Allow HTTPS" );
        filterRuleHttps.setBlocked( false );
        List<FilterRuleMatcher> ruleHttpsMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleHttpsMatcher1 = new FilterRuleMatcher();
        ruleHttpsMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleHttpsMatcher1.setValue("443");
        FilterRuleMatcher ruleHttpsMatcher2 = new FilterRuleMatcher();
        ruleHttpsMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleHttpsMatcher2.setValue("tcp");
        ruleHttpsMatchers.add(ruleHttpsMatcher1);
        ruleHttpsMatchers.add(ruleHttpsMatcher2);
        filterRuleHttps.setMatchers( ruleHttpsMatchers );

        FilterRule filterRuleBlock = new FilterRule();
        filterRuleBlock.setEnabled( true );
        filterRuleBlock.setDescription( "Block All" );
        filterRuleBlock.setBlocked( true );
        List<FilterRuleMatcher> rule4Matchers = new LinkedList<FilterRuleMatcher>();
        filterRuleBlock.setMatchers( rule4Matchers );
        
        rules.add( filterRuleSsh );
        rules.add( filterRulePing );
        rules.add( filterRuleDns );
        rules.add( filterRuleDhcp );
        rules.add( filterRuleHttp );
        rules.add( filterRuleHttps );
        rules.add( filterRuleBlock );

        return rules;
    }

    private List<FilterRule> defaultForwardFilterRules()
    {
        List<FilterRule> rules = new LinkedList<FilterRule>();
        return rules;
    }

    private int nextFreeInterfaceId( NetworkSettings netSettings)
    {
        if (netSettings == null)
            return 1;
        int free = 1;
        for ( InterfaceSettings intfSettings : netSettings.getInterfaces() ) {
            if ( free <= intfSettings.getInterfaceId() )
                free = intfSettings.getInterfaceId() + 1;
        }
        return free;
    }
}
