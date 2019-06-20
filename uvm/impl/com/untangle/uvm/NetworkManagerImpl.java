/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.HookManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.DeviceSettings;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.BypassRuleCondition;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.NatRuleCondition;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.PortForwardRuleCondition;
import com.untangle.uvm.network.FilterRule;
import com.untangle.uvm.network.FilterRuleCondition;
import com.untangle.uvm.network.QosSettings;
import com.untangle.uvm.network.QosRule;
import com.untangle.uvm.network.QosRuleCondition;
import com.untangle.uvm.network.QosPriority;
import com.untangle.uvm.network.DnsSettings;
import com.untangle.uvm.network.DhcpStaticEntry;
import com.untangle.uvm.network.UpnpSettings;
import com.untangle.uvm.network.UpnpRule;
import com.untangle.uvm.network.UpnpRuleCondition;
import com.untangle.uvm.network.NetflowSettings;
import com.untangle.uvm.network.DynamicRoutingSettings;
import com.untangle.uvm.network.DynamicRouteBgpNeighbor;
import com.untangle.uvm.network.DynamicRouteNetwork;
import com.untangle.uvm.network.DynamicRouteOspfArea;
import com.untangle.uvm.network.DynamicRouteOspfInterface;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.servlet.DownloadHandler;

/**
 * The Network Manager handles all the network configuration
 */
public class NetworkManagerImpl implements NetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private final String updateRulesScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-update-rules.sh";
    private final String deviceStatusScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-device-status.sh";
    private final String upnpManagerScript = System.getProperty("uvm.bin.dir") + "/ut-upnp-manager";

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network.js";
    private final String settingsFilenameBackup = "/etc/untangle/network.js";
    
    /**
     * The current network settings
     */
    private NetworkSettings networkSettings;

    /**
     * This array holds the current interface Settings indexed by the interface ID.
     * This enabled fast lookups with iterating the list in findInterfaceId()
     */
    private InterfaceSettings[] interfaceSettingsById = new InterfaceSettings[255];

    /**
     * NetworkManagerImpl constructor
     */
    protected NetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;

        UvmContextFactory.context().servletFileManager().registerDownloadHandler( new NetworkTestDownloadHandler() );

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
                
                if (readSettings != null)
                    settingsManager.save( this.settingsFilename, readSettings );
                    
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
            checkForNewDevices( readSettings );
            
            this.networkSettings = readSettings;
            configureInterfaceSettingsArray();

            logger.debug( "Loading Settings: " + this.networkSettings.toJSONString() );
        }

        /**
         * Fix missing access rules NGFW-11503
         */
        if (this.networkSettings.getAccessRules() == null) {
            this.networkSettings.setAccessRules( defaultAccessRules() );
            this.setNetworkSettings( this.networkSettings, false );
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( this.settingsFilename );
            File interfacesFile = new File("/etc/network/interfaces");
            if (settingsFile.lastModified() > interfacesFile.lastModified() ) {
                logger.warn("Settings file newer than interfaces files, Syncing...");
                /* Do not run sanity checks on restored settings */
                this.setNetworkSettings( this.networkSettings, false );
            }
        }
        
        logger.info( "Initialized NetworkManager" );
    }
    
    /**
     * Get the network settings
     * @return NetworkSettings
     */
    public NetworkSettings getNetworkSettings()
    {
        return this.networkSettings;
    }

    /**
     * Set the network settings
     * @param newSettings
     */
    public void setNetworkSettings( NetworkSettings newSettings )
    {
        setNetworkSettings( newSettings, true );
    }

    /**
     * Set the network settings
     * @param newSettings
     * @param runSanityChecks - if true sanityCheckNetworkSettings is called
     */
    public void setNetworkSettings( NetworkSettings newSettings, boolean runSanityChecks )
    {
        String downCommand, upCommand;

        /**
         * validate settings
         */
        if ( runSanityChecks )
            sanityCheckNetworkSettings( newSettings );
        
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
            settingsManager.save( this.settingsFilename, newSettings );

            /**
             * If its the dev env also save to /etc
             */
            if ( UvmContextFactory.context().isDevel() ) {
                settingsManager.save( this.settingsFilenameBackup, newSettings, false );
            }
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.networkSettings = newSettings;
        configureInterfaceSettingsArray();
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.networkSettings).toString(2));} catch (Exception e) {}


        /**
         * Now actually sync the settings to the system
         */
        ExecManagerResult result;
        boolean errorOccurred = false;
        String errorStr = null;
        String cmd = "/usr/bin/sync-settings -f " + settingsFilename;
        result = UvmContextFactory.context().execManager().exec( cmd );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Syncing settings to O/S: ");
            for ( String line : lines )
                logger.info("sync-settings: " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            errorOccurred = true;
            errorStr = "sync-settings failed: returned " + result.getResult();
        }
        
        // notify interested parties that the settings have changed
        UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.NETWORK_SETTINGS_CHANGE, this.networkSettings );

        if ( errorOccurred ) {
            throw new RuntimeException(errorStr);
        }
    }

    /**
     * Renew the DHCP lease for the provided interface
     * @param interfaceId
     */
    public void renewDhcpLease( int interfaceId )
    {
        ExecManagerResult result;
        InterfaceSettings intfSettings = findInterfaceId( interfaceId );

        if ( intfSettings == null ) {
            logger.warn("Interface not found. Unable to renew DHCP lease on interface " + interfaceId);
            return;
        }
        String devName = intfSettings.getSymbolicDev();
        if ( devName == null ) {
            logger.warn("Interface missing systemDev. Unable to renew DHCP lease on interface " + interfaceId);
            return;
        }
        if ( intfSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.AUTO ) {
            logger.warn("Interface not type AUTO. Unable to renew DHCP lease on interface " + interfaceId);
            return;
        }
        
        // just bring the interface up and down 
        result = UvmContextFactory.context().execManager().exec( "ifdown " + devName );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("ifdown " + devName + ": ");
            for ( String line : lines )
                logger.info("ifdown: " + line);
        } catch (Exception e) {}

        result = UvmContextFactory.context().execManager().exec( "ifup " + devName );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("ifup " + devName + ": ");
            for ( String line : lines )
                logger.info("ifup: " + line);
        } catch (Exception e) {}
    }
        
    /**
     * Get a list of the settings for all enabled interfaces
     * @return list
     */
    public List<InterfaceSettings> getEnabledInterfaces()
    {
        LinkedList<InterfaceSettings> newList = new LinkedList<>();

        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null )
            return newList;
        
        for ( InterfaceSettings intf: this.networkSettings.getInterfaces() ) {
            if (!intf.igetDisabled())
                newList.add(intf);
        }

        return newList;
    }

    /**
     * Get the IP address of the first WAN interface
     * @return InetAddress
     */
    public InetAddress getFirstWanAddress()
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null ) {
            return null;
        }
        
        for ( InterfaceSettings intfSettings : this.networkSettings.getInterfaces() ) {
            if ( !intfSettings.igetDisabled() && intfSettings.getIsWan() ) {
                return getInterfaceStatus( intfSettings.getInterfaceId() ).getV4Address();
            }
        }

        return null;
    }

    /**
     * Get the IP address of the first non-WAN (LAN) interface
     * @return InetAddress
     */
    public InetAddress getFirstNonWanAddress()
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null ) {
            return null;
        }

        for ( InterfaceSettings intfSettings : this.networkSettings.getInterfaces() ) {
            if ( !intfSettings.igetDisabled() && !intfSettings.getIsWan() ) {
                return getInterfaceStatus( intfSettings.getInterfaceId() ).getV4Address();
            }
        }

        return null;
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified Id
     * @param interfaceId
     * @return InterfaceSettings
     */
    public InterfaceSettings findInterfaceId( int interfaceId )
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null) {
            logger.warn( "Missing network settings." );
            return null;
        }

        if ( interfaceId < 0 || interfaceId > 255 ) {
            logger.warn( "Invalid interface ID: " + interfaceId );
            return null;
        }

        return interfaceSettingsById[ interfaceId ];
    }

    /**
     * Convenience method to find the InterfaceSettings for the specified systemDev
     * @param systemDev
     * @return InterfaceSettings
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
     * @return InterfaceSettings
     */
    public InterfaceSettings findInterfaceFirstWan( )
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null)
            return null;
        
        for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
            if ( !intf.igetDisabled() && intf.getIsWan() )
                return intf;
        }

        return null;
    }

    /**
     * determines the WAN status of the specified interface
     * @param interfaceId
     * @return true if the interface is a WAN, false if not found or not a WAN
     */
    public boolean isWanInterface( int interfaceId )
    {
        if ( interfaceId < 0 )
            return false;

        InterfaceSettings intfSettings = findInterfaceId( interfaceId );
        if ( intfSettings == null ) {
            logger.warn("Unknown interface: " + interfaceId, new Exception());
            return false;
        }

        return intfSettings.getIsWan();
    }

    /**
     * returns a list of networks already used locally
     * This can be used for verification of settings in app settings
     * @param includeDynamic
     * @param includeL2tp
     * @param includeOpenvpn
     * @return list
     */
    public List<IPMaskedAddress> getCurrentlyUsedNetworks( boolean includeDynamic, boolean includeL2tp, boolean includeOpenvpn )
    {
        List<IPMaskedAddress> addresses = new LinkedList<IPMaskedAddress>();
        try {
        
            /**
             * Add static v4 addresses
             */
            for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
                if ( intf.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                    continue;
                if ( intf.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                    continue;
                if ( intf.getV4StaticAddress() == null || intf.getV4StaticPrefix() == null )
                    continue;

                IPMaskedAddress intfma = new IPMaskedAddress( intf.getV4StaticAddress(), intf.getV4StaticPrefix() );
                addresses.add( intfma );

                for ( InterfaceSettings.InterfaceAlias alias : intf.getV4Aliases() ) {
                    IPMaskedAddress aliasma = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                    addresses.add( aliasma );
                }

            }

            /**
             * Add dynamic v4 addresses
             */
            if ( includeDynamic ) {
                for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
                    if ( intf.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                        continue;
                    if ( intf.getV4ConfigType() != InterfaceSettings.V4ConfigType.AUTO )
                        continue;

                    InetAddress address = getInterfaceStatus( intf.getInterfaceId() ).getV4Address();
                    InetAddress netmask = getInterfaceStatus( intf.getInterfaceId() ).getV4Netmask();

                    if ( address == null || netmask == null )
                        continue;

                    IPMaskedAddress intfma = new IPMaskedAddress( address, netmask );
                    addresses.add( intfma );
                }
            }

            if ( includeL2tp ) {
                InetAddress l2tpAddress = getInterfaceStatus( 251 ).getV4Address();
                InetAddress l2tpNetmask = getInterfaceStatus( 251 ).getV4Netmask();
                if ( l2tpAddress != null && l2tpNetmask != null )
                    addresses.add( new IPMaskedAddress ( l2tpAddress, l2tpNetmask ) );
            }

            if ( includeOpenvpn ) {
                InetAddress openvpnAddress = getInterfaceStatus( 250 ).getV4Address();
                InetAddress openvpnNetmask = getInterfaceStatus( 250 ).getV4Netmask();
                if ( openvpnAddress != null && openvpnNetmask != null )
                    addresses.add( new IPMaskedAddress ( openvpnAddress, openvpnNetmask ) );
            }

        } catch ( Exception e ) {
            logger.warn( "Exception when computing local networks", e );
        }
        
        return addresses;
    }
    
    /**
     * This method returns an address where the host should be able to access HTTP.
     * If HTTP is not reachable on this interface (like all WANs), it returns null.
     * If any error occurs it returns null.
     * @param clientIntfId
     * @return InetAddress
     */
    public InetAddress getInterfaceHttpAddress( int clientIntfId )
    {
        int intfId = clientIntfId;
        
        if ( this.networkSettings == null ) {
            logger.warn("Missing network configuration");
            return null;
        }

        InterfaceSettings intfSettings = findInterfaceId( intfId );
        if ( intfSettings == null ) {
            logger.warn("Failed to find interface " + intfId);
            return null;
        }

        /* WAN ports never have HTTP open */
        InterfaceSettings.ConfigType configType = intfSettings.getConfigType();
        if ( configType == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan() ) {
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
     * @param interfaceId
     * @return InterfaceStatus
     */
    public InterfaceStatus getInterfaceStatus( int interfaceId )
    {
        InterfaceStatus status = null;
        String filename = "/var/lib/interface-status/interface-" + interfaceId + "-status.js";

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
     * Determines if the specified interface is currently the VRRP master
     * @param interfaceId
     * @return true if the given interfaceId is current the VRRP master of its VRRP group, false otherwise
     */
    public boolean isVrrpMaster( int interfaceId )
    {
        InterfaceSettings intfSettings = findInterfaceId( interfaceId );
        if ( intfSettings == null ) {
            logger.warn("Unable to find interface settings for interface " + interfaceId );
            return false;
        }
        if ( ! intfSettings.getVrrpEnabled() ) {
            logger.warn("VRRP not enabled on interface " + interfaceId );
            return false;
        }
        // find any vrrp address
        InetAddress vrrpAddress = null;
        for ( InterfaceSettings.InterfaceAlias vrrpAlias : intfSettings.getVrrpAliases() ) {
            if ( vrrpAlias.getStaticAddress() == null )
                continue;
            vrrpAddress = vrrpAlias.getStaticAddress();
            break;
        }

        if ( vrrpAddress == null ) {
            logger.warn("VRRP alias not found on interface " + interfaceId );
            return false;
        }

        String command = "ip add list " + intfSettings.getSymbolicDev() + " | grep '" + vrrpAddress.getHostAddress() + "/'";
        int retCode = UvmContextFactory.context().execManager().execResult( command );

        if ( retCode == 0 )
            return true;
        else
            return false;
    }
        
    /**
     * Return the status of all addressed interfaces
     * @return a list of InterfaceStatus for all interface
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
     * @return a list of DeviceStatus for all current devices
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
    
    /**
     * Insert the iptables rules for capturing traffic
     */
    protected void insertRules( )
    {
        int retCode = UvmContextFactory.context().execManager().execResult( "ln -fs " + this.updateRulesScript + " /etc/untangle/iptables-rules.d/800-uvm" );
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
     * Remove the iptables rules for capturing traffic
     */
    protected void removeRules( )
    {
        int retCode = UvmContextFactory.context().execManager().execResult( "ln -fs " + this.updateRulesScript + " /etc/untangle/iptables-rules.d/800-uvm" );
        if ( retCode < 0 )
            logger.warn("Unable to link iptables hook to update-rules script");
        
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( this.updateRulesScript + " -r" );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("remove rules: ");
            for ( String line : lines )
                logger.info("remove rules: " + line);
        } catch (Exception e) {}
    }

    /**
     * sets values in the interafceSettingsById map for quick lookups
     */
    private void configureInterfaceSettingsArray()
    {
        /**
         * Set interfaceSettingsById array for fast lookups
         */
        for ( int i = 0 ; i < interfaceSettingsById.length ; i++ ) {
            interfaceSettingsById[i] = null;
        }
        if ( this.networkSettings.getInterfaces() != null ) {
            for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
                interfaceSettingsById[intf.getInterfaceId()] = intf;
            }
        }
        if ( this.networkSettings.getVirtualInterfaces() != null ) {
            for ( InterfaceSettings intf : this.networkSettings.getVirtualInterfaces() ) {
                interfaceSettingsById[intf.getInterfaceId()] = intf;
            }
        }

    }

    /**
     * Checks for any new ethernet/wifi devices
     * IF found it will add new devices settings and interface settings for the device
     * @param netSettings
     */
    private void checkForNewDevices( NetworkSettings netSettings )
    {
        LinkedList<String> deviceNames = getEthernetDeviceNames();
        
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
                logger.warn("Creating new InterfaceSettings for " + deviceName);

                InterfaceSettings interfaceSettings = new InterfaceSettings();
                int interfaceId = getNextFreeInterfaceId( netSettings, 1 );
                interfaceSettings.setInterfaceId( interfaceId );
                interfaceSettings.setName("Interface " + interfaceId);
                interfaceSettings.setPhysicalDev( deviceName );
                interfaceSettings.setSystemDev( deviceName );
                interfaceSettings.setSymbolicDev( deviceName );
                interfaceSettings.setIsWan( false );
                interfaceSettings.setConfigType( InterfaceSettings.ConfigType.DISABLED );

                // Check for wireless interfaces
                if (deviceName.startsWith("wlan")) {
                    interfaceSettings.setIsWirelessInterface(true);
                    interfaceSettings.setWirelessChannel(6);
                } else {
                    interfaceSettings.setIsWirelessInterface(false);
                }

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
    
    /**
     * Create the default NetworkSettings
     * @return NetworkSettings
     */
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();
        
        try {
            newSettings.setVersion( 7 ); // Currently on v7 (as of v13.2.1)

            String hostname = UvmContextFactory.context().oemManager().getOemName().toLowerCase();
            try {
                /**
                 * If the OEM name contains multiple words, use just the first word for the hostname
                 */
                hostname = hostname.split("[^a-z]",2)[0];
            } catch (Exception e) {}

            newSettings.setHostName( hostname );
            newSettings.setDomainName( "example.com" );
            newSettings.setHttpPort( 80 );
            newSettings.setHttpsPort( 443 );

            newSettings.setPublicUrlMethod( NetworkSettings.PUBLIC_URL_EXTERNAL_IP );
            newSettings.setPublicUrlAddress( "hostname.example.com" );
            newSettings.setPublicUrlPort( 443 );

            LinkedList<String> deviceNames = getEthernetDeviceNames();

            String devName = null;
            LinkedList<DeviceSettings> devices = new LinkedList<DeviceSettings>();
            for (String deviceName : deviceNames) {
                DeviceSettings deviceSettings = new DeviceSettings();
                deviceSettings.setDeviceName( deviceName );
                devices.add( deviceSettings );
            }
            newSettings.setDevices( devices );
            
            LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();

            devName = deviceNames.poll();
            if ( devName != null ) {
                InterfaceSettings external = new InterfaceSettings();
                external.setInterfaceId( 1 );
                external.setName( "External" );
                external.setIsWan( true );
                external.setPhysicalDev( devName );
                external.setSystemDev( devName );
                external.setSymbolicDev( devName );
                external.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                external.setV4ConfigType( InterfaceSettings.V4ConfigType.AUTO );
                external.setV6ConfigType( InterfaceSettings.V6ConfigType.DISABLED );
                external.setV4NatEgressTraffic( true );
                if (devName.startsWith("wlan")) {
                    external.setIsWirelessInterface(true);
                    external.setWirelessChannel(6);
                }
                interfaces.add( external );
            }
        
            devName = deviceNames.poll();
            if ( devName != null ) {
                InterfaceSettings internal = new InterfaceSettings();
                internal.setInterfaceId( 2 );
                internal.setName( "Internal" );
                internal.setIsWan( false );
                internal.setPhysicalDev( devName );
                internal.setSystemDev( devName );
                internal.setSymbolicDev( devName );
                internal.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                internal.setV4ConfigType( InterfaceSettings.V4ConfigType.STATIC );
                internal.setV4StaticAddress( InetAddress.getByName("192.168.2.1") );
                internal.setV4StaticPrefix( 24 );
                internal.setDhcpEnabled( true );
                internal.setV6ConfigType( InterfaceSettings.V6ConfigType.STATIC ); 
                internal.setV6StaticAddress( null );
                internal.setV6StaticPrefixLength( 64 );
                internal.setBridgedTo( 1 );
                if (devName.startsWith("wlan")) {
                    internal.setIsWirelessInterface(true);
                    internal.setWirelessChannel(6);
                }
                interfaces.add(internal);
            }

            int i = 2;
            for ( devName = deviceNames.poll() ; devName != null ; devName = deviceNames.poll() ) {
                
                InterfaceSettings intf = new InterfaceSettings();
                int interfaceId = i+1;
                intf.setInterfaceId( interfaceId );
                intf.setName("Interface " + interfaceId);

                // Check for wireless interfaces
                if (devName.startsWith("wlan")) {
                    intf.setIsWirelessInterface(true);
                    intf.setWirelessChannel(6);
                }

                intf.setPhysicalDev( devName);
                intf.setSystemDev( devName );
                intf.setSymbolicDev( devName );
                intf.setConfigType( InterfaceSettings.ConfigType.DISABLED );
                interfaces.add( intf );
                i++;
            }

            newSettings.setInterfaces(interfaces);

            InterfaceSettings virtualIntf;
            LinkedList<InterfaceSettings> virtualInterfaces = new LinkedList<InterfaceSettings>();

            virtualIntf = new InterfaceSettings(InterfaceSettings.OPENVPN_INTERFACE_ID,"OpenVPN");
            virtualIntf.setIsVirtualInterface(true);
            virtualIntf.setConfigType(null);
            virtualIntf.setV4ConfigType(null);
            virtualIntf.setV4Aliases(null);
            virtualIntf.setV6ConfigType(null);
            virtualIntf.setV6Aliases(null);
            virtualIntf.setVrrpAliases(null);
            virtualInterfaces.add(virtualIntf);

            virtualIntf = new InterfaceSettings(InterfaceSettings.L2TP_INTERFACE_ID,"L2TP");
            virtualIntf.setIsVirtualInterface(true);
            virtualIntf.setConfigType(null);
            virtualIntf.setV4ConfigType(null);
            virtualIntf.setV4Aliases(null);
            virtualIntf.setV6ConfigType(null);
            virtualIntf.setV6Aliases(null);
            virtualIntf.setVrrpAliases(null);
            virtualInterfaces.add(virtualIntf);

            virtualIntf = new InterfaceSettings(InterfaceSettings.XAUTH_INTERFACE_ID,"XAUTH");
            virtualIntf.setIsVirtualInterface(true);
            virtualIntf.setConfigType(null);
            virtualIntf.setV4ConfigType(null);
            virtualIntf.setV4Aliases(null);
            virtualIntf.setV6ConfigType(null);
            virtualIntf.setV6Aliases(null);
            virtualIntf.setVrrpAliases(null);
            virtualInterfaces.add(virtualIntf);

            virtualIntf = new InterfaceSettings(InterfaceSettings.GRE_INTERFACE_ID,"GRE");
            virtualIntf.setIsVirtualInterface(true);
            virtualIntf.setConfigType(null);
            virtualIntf.setV4ConfigType(null);
            virtualIntf.setV4Aliases(null);
            virtualIntf.setV6ConfigType(null);
            virtualIntf.setV6Aliases(null);
            virtualIntf.setVrrpAliases(null);
            virtualInterfaces.add(virtualIntf);
            
            newSettings.setVirtualInterfaces(virtualInterfaces);
            
            newSettings.setPortForwardRules( new LinkedList<PortForwardRule>() );
            newSettings.setNatRules( new LinkedList<NatRule>() );
            newSettings.setBypassRules( defaultBypassRules() );
            newSettings.setStaticRoutes( new LinkedList<StaticRoute>() );
            newSettings.setQosSettings( defaultQosSettings() );
            newSettings.setUpnpSettings( defaultUpnpSettings() );
            newSettings.setNetflowSettings( new NetflowSettings() );
            newSettings.setDynamicRoutingSettings( defaultDynamicRoutingSettings() );
            newSettings.setDnsSettings( new DnsSettings() );
            newSettings.setFilterRules( new LinkedList<FilterRule>() );
            newSettings.setAccessRules( defaultAccessRules() );
            newSettings.setStaticDhcpEntries( new LinkedList<DhcpStaticEntry>() );

            /**
             * If this is a netboot (untangle local installation)
             * Copy the authorized keys to root's ssh
             */
            if ( UvmContextFactory.context().isNetBoot() ) {
                UvmContextFactory.context().execManager().exec( "if [ ! -d /root/.ssh ] ; then mkdir /root/.ssh ; chmod 700 /root/.ssh ; fi" );
                UvmContextFactory.context().execManager().exec( "if [ ! -f /root/.ssh/authorized_keys2 ] ; then cp -f /usr/share/untangle-support-keyring/authorized_keys2 /root/.ssh/ ; chmod 700 /root/.ssh/authorized_keys2 ; fi " );
            }
        }
        catch (Exception e) {
            logger.error("Error creating Network Settings",e);
        }
        
        return newSettings;
    }

    /**
     * sanityCheckNetworkSettings
     * Sanity checks the network settings.
     * If something doesn't make sense RuntimeException is thrown with the description
     * of the issue
     * @param networkSettings
     */
    private void sanityCheckNetworkSettings( NetworkSettings networkSettings )
    {
        if ( networkSettings == null ) {
                throw new RuntimeException("null settings");
        }
        
        boolean foundWan = false;
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if (intf.getV4ConfigType() == null)
                throw new RuntimeException("Missing V4 Config Type");
            if ( intf.getConfigType() != InterfaceSettings.ConfigType.DISABLED && intf.getIsWan() )
                foundWan = true;
        }
        if (!foundWan) {
            throw new RuntimeException("Must have at least one configured WAN interface.");
        }
        if ( networkSettings.getHttpsPort() == networkSettings.getHttpPort() ) {
            throw new RuntimeException("HTTP and HTTPS services can not use the same port.");
        }

        /**
         * Check that no two statically configured interfaces have the same masked address.
         * For example, don't let people put 192.168.1.100/24 on external and 192.168.1.101/24 on internal
         * This never makes sense if the netmasks are equal
         */
        for ( InterfaceSettings intf1 : networkSettings.getInterfaces() ) {
            if ( intf1.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                continue;
            if ( intf1.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                continue;
            if ( intf1.getV4StaticAddress() == null || intf1.getV4StaticPrefix() == null )
                continue;

            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( intf2.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                    continue;
                if ( intf2.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                    continue;
                if ( intf2.getV4StaticAddress() == null || intf2.getV4StaticPrefix() == null )
                    continue;

                if ( intf1.getInterfaceId() == intf2.getInterfaceId() )
                    continue;
                
                /**
                 * check intf1 against intf2 static address and all aliases
                 */
                IPMaskedAddress intf1ma;
                IPMaskedAddress intf2ma;
                
                intf1ma = new IPMaskedAddress( intf1.getV4StaticAddress(), intf1.getV4StaticPrefix() );
                intf2ma = new IPMaskedAddress( intf2.getV4StaticAddress(), intf2.getV4StaticPrefix() );
                if ( intf1ma.getMaskedAddress().equals( intf2ma.getMaskedAddress() ) ) {
                    throw new RuntimeException( intf1.getName() + " & " + intf2.getName() + " address conflict. " +
                                                intf1ma.getMaskedAddress().getHostAddress() + " = " + intf2ma.getMaskedAddress().getHostAddress() ); 
                }
                for ( InterfaceSettings.InterfaceAlias alias : intf2.getV4Aliases() ) {
                    intf1ma = new IPMaskedAddress( intf1.getV4StaticAddress(), intf1.getV4StaticPrefix() );
                    intf2ma = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                    if ( intf1ma.getMaskedAddress().equals( intf2ma.getMaskedAddress() ) ) {
                        throw new RuntimeException( intf1.getName() + " & " + intf2.getName() + " address conflict. " +
                                                    intf1ma.getMaskedAddress().getHostAddress() + " = " + intf2ma.getMaskedAddress().getHostAddress() ); 
                    }
                }
            }
        }

        /**
         * Check for duplicate static DHCP reservations
         */
        LinkedList<String> entries = new LinkedList<String>();
        for ( DhcpStaticEntry entry : networkSettings.getStaticDhcpEntries() ) {
            if ( entries.contains( entry.getMacAddress() ) ) {
                throw new RuntimeException( "Duplicate DHCP reservation: " + entry.getMacAddress() );
            }
            entries.add( entry.getMacAddress() );
        }
        LinkedList<InetAddress> addrEntries = new LinkedList<InetAddress>();
        for ( DhcpStaticEntry entry : networkSettings.getStaticDhcpEntries() ) {
            if ( addrEntries.contains( entry.getAddress() ) ) {
                throw new RuntimeException( "Duplicate DHCP reservation: " + ( entry.getAddress() == null ? "null" : entry.getAddress().getHostAddress() ) );
            }
            addrEntries.add( entry.getAddress() );
        }
        
        /**
         * Check that no IP is configured twice anywhere
         */
        List<InetAddress> addrs = new LinkedList<InetAddress>();
        for ( InterfaceSettings intf1 : networkSettings.getInterfaces() ) {
            if ( intf1.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                continue;
            if ( intf1.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                continue;
            if ( intf1.getV4StaticAddress() == null || intf1.getV4StaticPrefix() == null )
                continue;

            /**
             * Check v4 address and aliases for global uniqueness
             */
            if (addrs.contains( intf1.getV4StaticAddress() )) {
                throw new RuntimeException( intf1.getName() + " address conflict. " + intf1.getV4StaticAddress().getHostAddress() + " is configured multiple times.");
            }
            addrs.add( intf1.getV4StaticAddress() );

            for ( InterfaceSettings.InterfaceAlias alias : intf1.getV4Aliases() ) {
                if (addrs.contains( alias.getStaticAddress() )) {
                    throw new RuntimeException( intf1.getName() + " address conflict. " + alias.getStaticAddress().getHostAddress() + " is configured multiple times.");
                }
                addrs.add( alias.getStaticAddress() );
            }

            if ( intf1.getIsWan() ) {
                //only check the gateway if its a WAN
                if (addrs.contains( intf1.getV4StaticGateway() )) {
                    throw new RuntimeException( intf1.getName() + " address conflict. " + intf1.getV4StaticGateway().getHostAddress() + " is configured multiple times.");
                }
                addrs.add( intf1.getV4StaticGateway() );
            }
            
        }

        /**
         * Prevent users from choosing untangle.com (#7574)
         */
        if ( "untangle.com".equals(networkSettings.getDomainName()) ) {
            throw new RuntimeException( "untangle.com is not an allowed domain name." );
        }

        /**
         * Check that there are no routes that conflict with interface address or aliases
         * Check that there are no redundant routes
         */
        for ( StaticRoute route : networkSettings.getStaticRoutes() ) {
            if ( route.getNetwork() == null )
                continue;
            if ( route.getPrefix() == null )
                continue;

            /**
             * Check other routes
             */
            for ( StaticRoute route2 : networkSettings.getStaticRoutes() ) {
                if ( route2.getNetwork() == null )
                    continue;
                if ( route2.getPrefix() == null )
                    continue;
                if ( route.getRuleId() == route2.getRuleId() )
                    continue;

                IPMaskedAddress maskedAddr1 = new IPMaskedAddress( route.getNetwork(), route.getPrefix() );
                IPMaskedAddress maskedAddr2 = new IPMaskedAddress( route2.getNetwork(), route2.getPrefix() );;
                
                if ( maskedAddr1.getMaskedAddress().equals(maskedAddr2.getMaskedAddress()) && route.getPrefix().equals(route2.getPrefix()) ) {
                    throw new RuntimeException( "\"" + route.getDescription() + "\" & \"" + route2.getDescription() + "\" routes conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                }
                
            }
            
            /**
             * Check interface addresses and aliases
             */
            for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
                if ( intf.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                    continue;
                if ( intf.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                    continue;
                if ( intf.getV4StaticAddress() == null || intf.getV4StaticPrefix() == null )
                    continue;
                
                /**
                 * check intf1 against intf static address and all aliases
                 */
                IPMaskedAddress maskedAddr1 = new IPMaskedAddress( route.getNetwork(), route.getPrefix() );
                IPMaskedAddress maskedAddr2;
                
                maskedAddr2 = new IPMaskedAddress( intf.getV4StaticAddress(), intf.getV4StaticPrefix() );
                if ( maskedAddr1.getMaskedAddress().equals(maskedAddr2.getMaskedAddress()) && route.getPrefix() == maskedAddr2.getPrefixLength() ) {
                    throw new RuntimeException( route.getDescription() + " & " + intf.getName() + " route & address conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                }
                for ( InterfaceSettings.InterfaceAlias alias : intf.getV4Aliases() ) {
                    maskedAddr2 = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );

                    if ( maskedAddr1.getMaskedAddress().equals(maskedAddr2.getMaskedAddress()) && route.getPrefix() == maskedAddr2.getPrefixLength() ) {
                        throw new RuntimeException( route.getDescription() + " & " + intf.getName() + " route & address conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                    }
                }
            }

            for ( PortForwardRule rule : networkSettings.getPortForwardRules() ) {
                List<PortForwardRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
            for ( NatRule rule : networkSettings.getNatRules() ) {
                List<NatRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
            for ( BypassRule rule : networkSettings.getBypassRules() ) {
                List<BypassRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
            for ( QosRule rule : networkSettings.getQosSettings().getQosRules() ) {
                List<QosRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
            for ( FilterRule rule : networkSettings.getFilterRules() ) {
                List<FilterRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
            for ( FilterRule rule : networkSettings.getAccessRules() ) {
                List<FilterRuleCondition> conditions = rule.getConditions();
                if ( conditions != null ) 
                    for ( RuleCondition matcher : conditions ) {
                        if ( matcher.getInvert() && matcher.getValue() != null && matcher.getValue().contains(",") )
                            throw new RuntimeException( "Invalid condition on rule " + rule.getDescription() + ". Can not use \"is NOT\" (invert) with multiple values." );
                    }
            }
        }

        /**
         * Sanity check the interface Settings
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            sanityCheckInterfaceSettings( intf );
        }

    }

    /**
     * sanityCheckInterfaceSettings
     * Santicy check the interface settings
     * If something doesn't make sense RuntimeException with the description is thrown
     * @param intf
     */
    private void sanityCheckInterfaceSettings( InterfaceSettings intf )
    {
        if ( intf == null )
            return;

        /**
         * Check DHCP range and make sure it falls within the interface's address range
         */
        do {
            boolean found;

            if ( intf.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED )
                break;
            if ( intf.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                break;
            if ( intf.getIsWan() )
                break;
            if ( intf.getDhcpEnabled() == null || ! intf.getDhcpEnabled() )
                break;
            if ( intf.getDhcpRangeStart() == null || intf.getDhcpRangeEnd() == null )
                break;

            /**
             * build a list of all addresses for this interface
             * check that dhcp start and end are in this range
             */
            LinkedList<IPMaskedAddress> addrs = new LinkedList<IPMaskedAddress>();
            addrs.add( new IPMaskedAddress( intf.getV4StaticAddress(), intf.getV4StaticPrefix() ) );
            for ( InterfaceSettings.InterfaceAlias alias : intf.getV4Aliases() ) { 
                addrs.add( new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() ) );
            }

            found = false;
            for( IPMaskedAddress maskedAddr : addrs ) {
                if ( maskedAddr.contains( intf.getDhcpRangeStart() ) ) {
                    found = true;
                    break;
                }
            }
            if (!found)
                throw new RuntimeException( "Invalid DHCP Range Start: " + intf.getDhcpRangeStart().getHostAddress() );

            found = false;
            for( IPMaskedAddress maskedAddr : addrs ) {
                if ( maskedAddr.contains( intf.getDhcpRangeEnd() ) ) {
                    found = true;
                    break;
                }
            }
            if (!found)
                throw new RuntimeException( "Invalid DHCP Range End: " + intf.getDhcpRangeEnd().getHostAddress() );
        } while ( false );

        /**
         * If this interface is bridged - checked that its bridged to somewhere valid
         */
        if ( intf.getConfigType() == InterfaceSettings.ConfigType.BRIDGED ) {
            Integer bridgedTo = intf.getBridgedTo();
            if ( bridgedTo == null ) {
                throw new RuntimeException("Interface " +
                                           intf.getInterfaceId() +
                                           " bridged to null interface.");
            }
            InterfaceSettings bridgeIntf = findInterfaceId( bridgedTo );
            if ( bridgeIntf == null ) {
                throw new RuntimeException("Interface " +
                                           intf.getInterfaceId() +
                                           " bridged to missing interface.");
            }
            if ( bridgeIntf.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED ) {
                throw new RuntimeException("Interface " +
                                           intf.getInterfaceId() +
                                           " must be bridged to addressed interface. (currently bridged to " + bridgeIntf.getInterfaceId() + ")");
            }
        }
    }
    
    /**
     * sanitizeNetworkSettings This will "sanitize" settings It will
     * do some sanity checks and change settings as necessary to avoid
     * any possible confusion on the backend
     * @param networkSettings
     */
    private void sanitizeNetworkSettings( NetworkSettings networkSettings )
    {
        
        /**
         * Fix rule IDs
         */
        int idx = 0;
        if (networkSettings.getNatRules() != null) {
            for (NatRule rule : networkSettings.getNatRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getPortForwardRules() != null) {
            for (PortForwardRule rule : networkSettings.getPortForwardRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getBypassRules() != null) {
            for (BypassRule rule : networkSettings.getBypassRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getStaticRoutes() != null) {
            for (StaticRoute rule : networkSettings.getStaticRoutes()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getAccessRules() != null) {
            for (FilterRule rule : networkSettings.getAccessRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getFilterRules() != null) {
            for (FilterRule rule : networkSettings.getFilterRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getQosSettings() != null && networkSettings.getQosSettings().getQosRules() != null) {
            for (QosRule rule : networkSettings.getQosSettings().getQosRules()) {
                rule.setRuleId(++idx);
            }
        }
        idx = 0;
        if (networkSettings.getUpnpSettings() != null && networkSettings.getUpnpSettings().getUpnpRules() != null) {
            for (UpnpRule rule : networkSettings.getUpnpSettings().getUpnpRules()) {
                rule.setRuleId(++idx);
            }
        }
        /**
         * Reset all symbolic devs to system devs
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            intf.setSystemDev( intf.getPhysicalDev() );
            intf.setSymbolicDev( intf.getSystemDev() );
        }

        /**
         * Set all IMQ devices for wans
         */
        int wanIndex = 0;
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if ( intf.getIsWan() && intf.getConfigType() ==  InterfaceSettings.ConfigType.ADDRESSED ) {
                intf.setImqDev( "imq" + wanIndex );
                wanIndex++;
            } else {
                intf.setImqDev( null );
            }
        }
        
        /**
         * If hostname is null, set it to the default
         * Make sure hostname is not fully qualified
         */
        if (networkSettings.getHostName() == null)
            networkSettings.setHostName( UvmContextFactory.context().oemManager().getOemName().toLowerCase() );
        networkSettings.setHostName( networkSettings.getHostName().replaceAll("\\..*","") );
        
        /**
         * Handle VLAN interfaces
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if (!intf.getIsVlanInterface())
                continue;
            if ( intf.getVlanTag() == null )
                throw new RuntimeException("VLAN tag missing on VLAN interface");
            if ( intf.getVlanParent() == null )
                throw new RuntimeException("VLAN parent missing on VLAN interface");

            if ( intf.getInterfaceId() < 0 )
                intf.setInterfaceId( getNextFreeInterfaceId( networkSettings, 100 ) );
            
            InterfaceSettings parent = null;
            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( intf.getVlanParent() == intf2.getInterfaceId() )
                    parent = intf2;
            }

            if (parent == null)
                throw new RuntimeException( "Unable to find parent of VLAN: " + intf.getVlanParent() );

            intf.setPhysicalDev( parent.getPhysicalDev() );
            intf.setSystemDev( parent.getSystemDev() + "." + intf.getVlanTag() );
            intf.setSymbolicDev( intf.getSystemDev() );
        }

        /**
         * Handle PPPoE interfaces
         */
        int pppCount = 0;
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if ( !InterfaceSettings.ConfigType.DISABLED.equals( intf.getConfigType() ) &&
                 InterfaceSettings.V4ConfigType.PPPOE.equals( intf.getV4ConfigType() ) ) {
                // save the old system dev (usuallyy physdev or sometimse vlan dev as root dev)
                intf.setV4PPPoERootDev(intf.getSystemDev());
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

                    /**
                     * pick a decent bridge name
                     * We use br.systemDev - however we substitute periods for dashes
                     * so if you bridge a vlan it doesn't become br.eth0.3 because the O/S
                     * thinks that is a special vlan bridge.
                     */
                    String bridgeName = "br." + intf.getSystemDev().replace(".","-");
                    
                    intf.setSymbolicDev( bridgeName );
                    intf2.setSymbolicDev( bridgeName );
                }
            }
        }
        
        /**
         * Sanitize the interface Settings
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            sanitizeInterfaceSettings( intf );
        }

        /**
         * Sort Interfaces
         * We may have added new VLAN interfaces and set their interfaceId above
         * We need to position them in the correct place
         */
        List<InterfaceSettings> interfaceList = networkSettings.getInterfaces();
        Collections.sort(interfaceList, new Comparator<InterfaceSettings>() {
                /**
                 * compare sorts the interfaces by ID
                 * @param i1
                 * @param i2
                 * @return -1,0,1
                 */
                public int compare(InterfaceSettings i1, InterfaceSettings i2) {
                    int oi1 = i1.getInterfaceId();
                    int oi2 = i2.getInterfaceId();
                    if (oi1 == oi2) {
                        return 0;
                    } else if (oi1 < oi2) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });
        networkSettings.setInterfaces(interfaceList);
        
        /**
         *  Sanitize dynamic routing settings
         */
        if (networkSettings.getDynamicRoutingSettings() != null ){
            if( networkSettings.getDynamicRoutingSettings().getBgpNeighbors() != null) {
                idx = 0;
                for (DynamicRouteBgpNeighbor neighbor : networkSettings.getDynamicRoutingSettings().getBgpNeighbors()) {
                    neighbor.setRuleId(++idx);
                }
            }
            if( networkSettings.getDynamicRoutingSettings().getBgpNetworks() != null) {
                idx = 0;
                for (DynamicRouteNetwork network : networkSettings.getDynamicRoutingSettings().getBgpNetworks()) {
                    network.setRuleId(++idx);
                }
            }
            if( networkSettings.getDynamicRoutingSettings().getOspfNetworks() != null) {
                idx = 0;
                for (DynamicRouteNetwork network : networkSettings.getDynamicRoutingSettings().getOspfNetworks()) {
                    network.setRuleId(++idx);
                }
            }
            if( networkSettings.getDynamicRoutingSettings().getOspfAreas() != null) {
                idx = 0;
                for (DynamicRouteOspfArea area : networkSettings.getDynamicRoutingSettings().getOspfAreas()) {
                    area.setRuleId(++idx);
                }
            }
            if( networkSettings.getDynamicRoutingSettings().getOspfInterfaces() != null) {
                idx = 0;
                for (DynamicRouteOspfInterface intf : networkSettings.getDynamicRoutingSettings().getOspfInterfaces()) {
                    intf.setRuleId(++idx);
                }
            }
        }
        
        
    }

    /**
     * sanitizeInterfaceSettings
     * This will "sanitize" settings
     * It will do some sanity checks and change settings as necessary to avoid any possible
     * confusion on the backend
     * @param interfaceSettings
     */
    private void sanitizeInterfaceSettings( InterfaceSettings interfaceSettings )
    {
        /**
         * If DHCP settings are enabled, but settings arent picked, fill in reasonable defaults
         */
        if ( interfaceSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED &&
             interfaceSettings.getDhcpEnabled() != null &&
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
        if (interfaceSettings.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED)
            interfaceSettings.setIsWan( false );
    }

    /**
     * This chooses "reasonable" DHCP defaults if dhcp is enabled on the given interface
     * @param interfaceSettings
     */
    private void initializeDhcpDefaults( InterfaceSettings interfaceSettings )
    {
        if (! interfaceSettings.getDhcpEnabled())
            return;

        try {
            InetAddress addr = interfaceSettings.getV4StaticAddress();
            InetAddress mask = interfaceSettings.getV4StaticNetmask();
            if (addr == null || mask == null) {
                logger.warn("Missing interface[" + interfaceSettings.getName() + "] settings (" + addr + ", " + mask + "). Disabling DHCP.");
                interfaceSettings.setDhcpEnabled( false );
                return;
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
     * Create the default QoSSettings
     * @return QosSettings
     */
    private QosSettings defaultQosSettings()
    {
        QosSettings qosSettings = new QosSettings();

        qosSettings.setQosEnabled( false );
        qosSettings.setPingPriority( 1 );
        qosSettings.setDnsPriority( 1 );
        qosSettings.setSshPriority( 0 );
        qosSettings.setOpenvpnPriority( 0 );
        qosSettings.setQueueDiscipline( "fq_codel" );

        List<QosRule> qosRules = new LinkedList<QosRule>();

        /**
         * Add SIP rule
         */
        QosRule qosRule1 = new QosRule();
        qosRule1.setEnabled( true );
        qosRule1.setDescription( "VoIP (SIP) Traffic" );
        qosRule1.setPriority( 1 );
        
        List<QosRuleCondition> rule1Conditions = new LinkedList<QosRuleCondition>();
        QosRuleCondition rule1Matcher1 = new QosRuleCondition();
        rule1Matcher1.setConditionType(QosRuleCondition.ConditionType.DST_PORT);
        rule1Matcher1.setValue("5060,5061");
        QosRuleCondition rule1Matcher2 = new QosRuleCondition();
        rule1Matcher2.setConditionType(QosRuleCondition.ConditionType.PROTOCOL);
        rule1Matcher2.setValue("TCP,UDP");
        rule1Conditions.add(rule1Matcher1);
        rule1Conditions.add(rule1Matcher2);

        qosRule1.setConditions( rule1Conditions );


        /**
         * Add IAX rule
         */
        QosRule qosRule2 = new QosRule();
        qosRule2.setEnabled( true );
        qosRule2.setDescription( "VoIP (IAX) Traffic" );
        qosRule2.setPriority( 1 );
        
        List<QosRuleCondition> rule2Conditions = new LinkedList<QosRuleCondition>();
        QosRuleCondition rule2Matcher1 = new QosRuleCondition();
        rule2Matcher1.setConditionType(QosRuleCondition.ConditionType.DST_PORT);
        rule2Matcher1.setValue("4569");
        QosRuleCondition rule2Matcher2 = new QosRuleCondition();
        rule2Matcher2.setConditionType(QosRuleCondition.ConditionType.PROTOCOL);
        rule2Matcher2.setValue("TCP,UDP");
        rule2Conditions.add(rule2Matcher1);
        rule2Conditions.add(rule2Matcher2);

        qosRule2.setConditions( rule2Conditions );

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

        qosPriorities.add( new QosPriority( 1, "Very High",       50, 100, 50, 100) );
        qosPriorities.add( new QosPriority( 2, "High",            25, 100, 25, 100) );
        qosPriorities.add( new QosPriority( 3, "Medium",          12, 100, 12, 100) );
        qosPriorities.add( new QosPriority( 4, "Low",              6, 100,  6, 100) );
        qosPriorities.add( new QosPriority( 5, "Limited",          3,  75,  3, 75) );
        qosPriorities.add( new QosPriority( 6, "Limited More",     2,  50,  2, 50) );
        qosPriorities.add( new QosPriority( 7, "Limited Severely", 2,  10,  2, 10) );

        qosSettings.setQosPriorities( qosPriorities );
        
        
        return qosSettings;
    }

    /**
     * Get the default UPnP settings
     * @return UpnpSettings
     */
    private UpnpSettings defaultUpnpSettings()
    {
        UpnpSettings upnpSettings = new UpnpSettings();

        upnpSettings.setUpnpEnabled( false );
        upnpSettings.setSecureMode( true );

        List<UpnpRule> upnpRules = new LinkedList<UpnpRule>();

        /**
         * Allow all rule
         */
        UpnpRule upnpRule = new UpnpRule();
        upnpRule.setEnabled( true );
        upnpRule.setAllow( true );
        upnpRule.setDescription( "Allow all" );
        
        List<UpnpRuleCondition> ruleConditions = new LinkedList<UpnpRuleCondition>();
        UpnpRuleCondition ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.SRC_ADDR);
        ruleMatcher.setValue("0.0.0.0/0");
        ruleConditions.add(ruleMatcher);

        ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.DST_PORT);
        ruleMatcher.setValue("1024-65535");
        ruleConditions.add(ruleMatcher);

        ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.SRC_PORT);
        ruleMatcher.setValue("1024-65535");
        ruleConditions.add(ruleMatcher);

        upnpRule.setConditions(ruleConditions);
        upnpRules.add( upnpRule );

        /**
         * Deny all rule
         */
        upnpRule = new UpnpRule();
        upnpRule.setEnabled( true );
        upnpRule.setAllow( false );
        upnpRule.setDescription( "Deny all" );
        
        ruleConditions = new LinkedList<>();
        ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.SRC_ADDR);
        ruleMatcher.setValue("0.0.0.0/0");
        ruleConditions.add(ruleMatcher);

        ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.DST_PORT);
        ruleMatcher.setValue("0-65535");
        ruleConditions.add(ruleMatcher);

        ruleMatcher = new UpnpRuleCondition();
        ruleMatcher.setConditionType(UpnpRuleCondition.ConditionType.SRC_PORT);
        ruleMatcher.setValue("0-65535");
        ruleConditions.add(ruleMatcher);

        upnpRule.setConditions(ruleConditions);
        upnpRules.add( upnpRule );

        /*
         * Add rules
         */
        upnpSettings.setUpnpRules( upnpRules );

        return upnpSettings;
    }

    /**
     * get the default dynamic routing settings
     * @return DynamicRoutingSettings
     */
    private DynamicRoutingSettings defaultDynamicRoutingSettings()
    {
        DynamicRoutingSettings drSettings = new DynamicRoutingSettings();

        drSettings.setEnabled( false );
        List<DynamicRouteBgpNeighbor> bgpNeighbors = new LinkedList<>();
        drSettings.setBgpNeighbors(bgpNeighbors);

        List<DynamicRouteNetwork> bgpNetworks = new LinkedList<>();
        drSettings.setBgpNetworks(bgpNetworks);

        List<DynamicRouteNetwork> ospfNetworks = new LinkedList<>();
        drSettings.setOspfNetworks(ospfNetworks);

        List<DynamicRouteOspfInterface> ospfInterfaces = new LinkedList<>();
        drSettings.setOspfInterfaces(ospfInterfaces);

        List<DynamicRouteOspfArea> ospfAreas = new LinkedList<>();

        DynamicRouteOspfArea ospfArea = new DynamicRouteOspfArea();
        ospfArea.setRuleId(1);
        ospfArea.setDescription("Backbone");
        ospfArea.setArea("0.0.0.0");
        ospfArea.setType(0);
        ospfArea.setVirtualLinks(new LinkedList<InetAddress>());
        ospfAreas.add(ospfArea);

        drSettings.setOspfAreas(ospfAreas);

        return drSettings;
    }

    /**
     * Get a list of the default access rules
     * @return list
     */
    private List<FilterRule> defaultAccessRules()
    {
        List<FilterRule> rules = new LinkedList<>();
        List<FilterRuleCondition> conditions;

        // enabled in dev env
        // disabled but there in normal env
        FilterRule filterRuleSsh = new FilterRule();
        filterRuleSsh.setReadOnly( true );
        filterRuleSsh.setEnabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot() );
        filterRuleSsh.setIpv6Enabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot() );
        filterRuleSsh.setDescription( "Allow SSH" );
        filterRuleSsh.setBlocked( false );
        filterRuleSsh.setReadOnly( true );
        List<FilterRuleCondition> ruleSshConditions = new LinkedList<>();
        FilterRuleCondition ruleSshMatcher1 = new FilterRuleCondition();
        ruleSshMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleSshMatcher1.setValue("22");
        FilterRuleCondition ruleSshMatcher2 = new FilterRuleCondition();
        ruleSshMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleSshMatcher2.setValue("TCP");
        ruleSshConditions.add(ruleSshMatcher1);
        ruleSshConditions.add(ruleSshMatcher2);
        filterRuleSsh.setConditions( ruleSshConditions );

        FilterRule filterRuleHttpsWan = new FilterRule();
        filterRuleHttpsWan.setReadOnly( true );
        filterRuleHttpsWan.setEnabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot());
        filterRuleHttpsWan.setIpv6Enabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot());
        filterRuleHttpsWan.setDescription( "Allow HTTPS on WANs" );
        filterRuleHttpsWan.setBlocked( false );
        filterRuleHttpsWan.setReadOnly( true );
        List<FilterRuleCondition> ruleHttpsWanConditions = new LinkedList<>();
        FilterRuleCondition ruleHttpsWanMatcher1 = new FilterRuleCondition();
        ruleHttpsWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleHttpsWanMatcher1.setValue("443");
        FilterRuleCondition ruleHttpsWanMatcher2 = new FilterRuleCondition();
        ruleHttpsWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleHttpsWanMatcher2.setValue("TCP");
        FilterRuleCondition ruleHttpsWanMatcher3 = new FilterRuleCondition();
        ruleHttpsWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleHttpsWanMatcher3.setValue("wan");
        ruleHttpsWanConditions.add(ruleHttpsWanMatcher1);
        ruleHttpsWanConditions.add(ruleHttpsWanMatcher2);
        ruleHttpsWanConditions.add(ruleHttpsWanMatcher3);
        filterRuleHttpsWan.setConditions( ruleHttpsWanConditions );

        FilterRule filterRuleHttpsNonWan = new FilterRule();
        filterRuleHttpsNonWan.setReadOnly( true );
        filterRuleHttpsNonWan.setEnabled( true );
        filterRuleHttpsNonWan.setIpv6Enabled( true );
        filterRuleHttpsNonWan.setDescription( "Allow HTTPS on non-WANs" );
        filterRuleHttpsNonWan.setBlocked( false );
        filterRuleHttpsNonWan.setReadOnly( true );
        List<FilterRuleCondition> ruleHttpsNonWanConditions = new LinkedList<>();
        FilterRuleCondition ruleHttpsNonWanMatcher1 = new FilterRuleCondition();
        ruleHttpsNonWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleHttpsNonWanMatcher1.setValue("443");
        FilterRuleCondition ruleHttpsNonWanMatcher2 = new FilterRuleCondition();
        ruleHttpsNonWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleHttpsNonWanMatcher2.setValue("TCP");
        FilterRuleCondition ruleHttpsNonWanMatcher3 = new FilterRuleCondition();
        ruleHttpsNonWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleHttpsNonWanMatcher3.setValue("non_wan");
        ruleHttpsNonWanConditions.add(ruleHttpsNonWanMatcher1);
        ruleHttpsNonWanConditions.add(ruleHttpsNonWanMatcher2);
        ruleHttpsNonWanConditions.add(ruleHttpsNonWanMatcher3);
        filterRuleHttpsNonWan.setConditions( ruleHttpsNonWanConditions );

        FilterRule filterRulePing = new FilterRule();
        filterRulePing.setReadOnly( true );
        filterRulePing.setEnabled( true );
        filterRulePing.setIpv6Enabled( true );
        filterRulePing.setDescription( "Allow PING" );
        filterRulePing.setBlocked( false );
        filterRulePing.setReadOnly( true );
        List<FilterRuleCondition> rulePingConditions = new LinkedList<>();
        FilterRuleCondition rulePingMatcher1 = new FilterRuleCondition();
        rulePingMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        rulePingMatcher1.setValue("ICMP");
        rulePingConditions.add(rulePingMatcher1);
        filterRulePing.setConditions( rulePingConditions );

        FilterRule filterRuleDns = new FilterRule();
        filterRuleDns.setReadOnly( true );
        filterRuleDns.setEnabled( true );
        filterRuleDns.setIpv6Enabled( true );
        filterRuleDns.setDescription( "Allow DNS on non-WANs" );
        filterRuleDns.setBlocked( false );
        List<FilterRuleCondition> ruleDnsConditions = new LinkedList<>();
        FilterRuleCondition ruleDnsMatcher1 = new FilterRuleCondition();
        ruleDnsMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleDnsMatcher1.setValue("53");
        FilterRuleCondition ruleDnsMatcher2 = new FilterRuleCondition();
        ruleDnsMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleDnsMatcher2.setValue("TCP,UDP");
        FilterRuleCondition ruleDnsMatcher3 = new FilterRuleCondition();
        ruleDnsMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleDnsMatcher3.setValue("non_wan");
        ruleDnsConditions.add(ruleDnsMatcher1);
        ruleDnsConditions.add(ruleDnsMatcher2);
        ruleDnsConditions.add(ruleDnsMatcher3);
        filterRuleDns.setConditions( ruleDnsConditions );

        FilterRule filterRuleDhcp = new FilterRule();
        filterRuleDhcp.setReadOnly( true );
        filterRuleDhcp.setEnabled( true );
        filterRuleDhcp.setIpv6Enabled( true );
        filterRuleDhcp.setDescription( "Allow DHCP on non-WANs" );
        filterRuleDhcp.setBlocked( false );
        filterRuleDhcp.setReadOnly( true );
        List<FilterRuleCondition> ruleDhcpConditions = new LinkedList<>();
        FilterRuleCondition ruleDhcpMatcher1 = new FilterRuleCondition();
        ruleDhcpMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleDhcpMatcher1.setValue("67");
        FilterRuleCondition ruleDhcpMatcher2 = new FilterRuleCondition();
        ruleDhcpMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleDhcpMatcher2.setValue("UDP");
        FilterRuleCondition ruleDhcpMatcher3 = new FilterRuleCondition();
        ruleDhcpMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleDhcpMatcher3.setValue("non_wan");
        ruleDhcpConditions.add(ruleDhcpMatcher1);
        ruleDhcpConditions.add(ruleDhcpMatcher2);
        ruleDhcpConditions.add(ruleDhcpMatcher3);
        filterRuleDhcp.setConditions( ruleDhcpConditions );
        
        FilterRule filterRuleHttp = new FilterRule();
        filterRuleHttp.setReadOnly( true );
        filterRuleHttp.setEnabled( true );
        filterRuleHttp.setIpv6Enabled( true );
        filterRuleHttp.setDescription( "Allow HTTP on non-WANs" );
        filterRuleHttp.setBlocked( false );
        filterRuleHttp.setReadOnly( true );
        List<FilterRuleCondition> ruleHttpConditions = new LinkedList<>();
        FilterRuleCondition ruleHttpMatcher1 = new FilterRuleCondition();
        ruleHttpMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleHttpMatcher1.setValue("80");
        FilterRuleCondition ruleHttpMatcher2 = new FilterRuleCondition();
        ruleHttpMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleHttpMatcher2.setValue("TCP");
        FilterRuleCondition ruleHttpMatcher3 = new FilterRuleCondition();
        ruleHttpMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleHttpMatcher3.setValue("non_wan");
        ruleHttpConditions.add(ruleHttpMatcher1);
        ruleHttpConditions.add(ruleHttpMatcher2);
        ruleHttpConditions.add(ruleHttpMatcher3);
        filterRuleHttp.setConditions( ruleHttpConditions );

        FilterRule filterRuleSnmp = new FilterRule();
        filterRuleSnmp.setReadOnly( true );
        filterRuleSnmp.setEnabled( true );
        filterRuleSnmp.setIpv6Enabled( true );
        filterRuleSnmp.setDescription( "Allow SNMP on non-WANs" );
        filterRuleSnmp.setBlocked( false );
        filterRuleSnmp.setReadOnly( true );
        List<FilterRuleCondition> ruleSnmpConditions = new LinkedList<>();
        FilterRuleCondition ruleSnmpMatcher1 = new FilterRuleCondition();
        ruleSnmpMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleSnmpMatcher1.setValue("161");
        FilterRuleCondition ruleSnmpMatcher2 = new FilterRuleCondition();
        ruleSnmpMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleSnmpMatcher2.setValue("UDP");
        FilterRuleCondition ruleSnmpMatcher3 = new FilterRuleCondition();
        ruleSnmpMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleSnmpMatcher3.setValue("non_wan");
        ruleSnmpConditions.add(ruleSnmpMatcher1);
        ruleSnmpConditions.add(ruleSnmpMatcher2);
        ruleSnmpConditions.add(ruleSnmpMatcher3);
        filterRuleSnmp.setConditions( ruleSnmpConditions );

        FilterRule filterRuleUpnp = new FilterRule();
        filterRuleUpnp.setReadOnly( true );
        filterRuleUpnp.setEnabled( true );
        filterRuleUpnp.setIpv6Enabled( true );
        filterRuleUpnp.setDescription( "Allow UPnP (UDP/1900) on non-WANs" );
        filterRuleUpnp.setBlocked( false );
        filterRuleUpnp.setReadOnly( true );
        conditions = new LinkedList<>();
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.DST_PORT, "1900" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.PROTOCOL, "UDP" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.SRC_INTF, "non_wan" ));
        filterRuleUpnp.setConditions( conditions );

        FilterRule filterRuleUpnpB = new FilterRule();
        filterRuleUpnpB.setReadOnly( true );
        filterRuleUpnpB.setEnabled( true );
        filterRuleUpnpB.setIpv6Enabled( true );
        filterRuleUpnpB.setDescription( "Allow UPnP (TCP/5000) on non-WANs" );
        filterRuleUpnpB.setBlocked( false );
        filterRuleUpnpB.setReadOnly( true );
        conditions = new LinkedList<>();
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.DST_PORT, "5000" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.PROTOCOL, "TCP" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.SRC_INTF, "non_wan" ));
        filterRuleUpnpB.setConditions( conditions );

        FilterRule filterRuleUpnpC = new FilterRule();
        filterRuleUpnpC.setReadOnly( true );
        filterRuleUpnpC.setEnabled( true );
        filterRuleUpnpC.setIpv6Enabled( true );
        filterRuleUpnpC.setDescription( "Allow UPnP (UDP/5351) on non-WANs" );
        filterRuleUpnpC.setBlocked( false );
        filterRuleUpnpC.setReadOnly( true );
        conditions = new LinkedList<>();
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.DST_PORT, "5351" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.PROTOCOL, "UDP" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.SRC_INTF, "non_wan" ));
        filterRuleUpnpC.setConditions( conditions );


        FilterRule filterRuleBgp = new FilterRule();
        filterRuleBgp.setReadOnly( true );
        filterRuleBgp.setEnabled( true );
        filterRuleBgp.setIpv6Enabled( true );
        filterRuleBgp.setDescription( "Allow Dynamic Routing BGP (TCP/179)" );
        filterRuleBgp.setBlocked( false );
        conditions = new LinkedList<>();
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.DST_PORT, "179" ));
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.PROTOCOL, "TCP" ));
        filterRuleBgp.setConditions( conditions );

        FilterRule filterRuleOspf = new FilterRule();
        filterRuleOspf.setReadOnly( true );
        filterRuleOspf.setEnabled( true );
        filterRuleOspf.setIpv6Enabled( true );
        filterRuleOspf.setDescription( "Allow Dynamic Routing OSPF" );
        filterRuleOspf.setBlocked( false );
        conditions = new LinkedList<>();
        conditions.add(new FilterRuleCondition( FilterRuleCondition.ConditionType.PROTOCOL, "OSPF" ));
        filterRuleOspf.setConditions( conditions );
        
        FilterRule filterRuleAhEsp = new FilterRule();
        filterRuleAhEsp.setReadOnly( true );
        filterRuleAhEsp.setEnabled( true );
        filterRuleAhEsp.setIpv6Enabled( true );
        filterRuleAhEsp.setDescription( "Allow AH/ESP for IPsec" );
        filterRuleAhEsp.setBlocked( false );
        filterRuleAhEsp.setReadOnly( true );
        List<FilterRuleCondition> ruleAhEspConditions = new LinkedList<>();
        FilterRuleCondition ruleAhEspMatcher1 = new FilterRuleCondition();
        ruleAhEspMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleAhEspMatcher1.setValue("AH,ESP");
        ruleAhEspConditions.add(ruleAhEspMatcher1);
        filterRuleAhEsp.setConditions( ruleAhEspConditions );

        FilterRule filterRuleIke = new FilterRule();
        filterRuleIke.setReadOnly( true );
        filterRuleIke.setEnabled( true );
        filterRuleIke.setIpv6Enabled( true );
        filterRuleIke.setDescription( "Allow IKE for IPsec" );
        filterRuleIke.setBlocked( false );
        filterRuleIke.setReadOnly( true );
        List<FilterRuleCondition> ruleIkeConditions = new LinkedList<>();
        FilterRuleCondition ruleIkeMatcher1 = new FilterRuleCondition();
        ruleIkeMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleIkeMatcher1.setValue("UDP");
        FilterRuleCondition ruleIkeMatcher2 = new FilterRuleCondition();
        ruleIkeMatcher2.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleIkeMatcher2.setValue("500");
        ruleIkeConditions.add(ruleIkeMatcher1);
        ruleIkeConditions.add(ruleIkeMatcher2);
        filterRuleIke.setConditions( ruleIkeConditions );

        FilterRule filterRuleNatT = new FilterRule();
        filterRuleNatT.setReadOnly( true );
        filterRuleNatT.setEnabled( true );
        filterRuleNatT.setIpv6Enabled( true );
        filterRuleNatT.setDescription( "Allow NAT-T for IPsec" );
        filterRuleNatT.setBlocked( false );
        filterRuleNatT.setReadOnly( true );
        List<FilterRuleCondition> ruleNatTConditions = new LinkedList<>();
        FilterRuleCondition ruleNatTMatcher1 = new FilterRuleCondition();
        ruleNatTMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleNatTMatcher1.setValue("UDP");
        FilterRuleCondition ruleNatTMatcher2 = new FilterRuleCondition();
        ruleNatTMatcher2.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleNatTMatcher2.setValue("4500");
        ruleNatTConditions.add(ruleNatTMatcher1);
        ruleNatTConditions.add(ruleNatTMatcher2);
        filterRuleNatT.setConditions( ruleNatTConditions );

        FilterRule filterRuleL2tp = new FilterRule();
        filterRuleL2tp.setReadOnly( true );
        filterRuleL2tp.setEnabled( true );
        filterRuleL2tp.setIpv6Enabled( true );
        filterRuleL2tp.setDescription( "Allow L2TP" );
        filterRuleL2tp.setBlocked( false );
        filterRuleL2tp.setReadOnly( true );
        List<FilterRuleCondition> ruleL2tpConditions = new LinkedList<>();
        FilterRuleCondition ruleL2tpMatcher1 = new FilterRuleCondition();
        ruleL2tpMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleL2tpMatcher1.setValue("UDP");
        FilterRuleCondition ruleL2tpMatcher2 = new FilterRuleCondition();
        ruleL2tpMatcher2.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleL2tpMatcher2.setValue("1701");
        ruleL2tpConditions.add(ruleL2tpMatcher1);
        ruleL2tpConditions.add(ruleL2tpMatcher2);
        filterRuleL2tp.setConditions( ruleL2tpConditions );

        FilterRule filterRuleOpenVpn = new FilterRule();
        filterRuleOpenVpn.setReadOnly( true );
        filterRuleOpenVpn.setEnabled( true );
        filterRuleOpenVpn.setIpv6Enabled( true );
        filterRuleOpenVpn.setDescription( "Allow OpenVPN" );
        filterRuleOpenVpn.setBlocked( false );
        filterRuleOpenVpn.setReadOnly( true );
        List<FilterRuleCondition> ruleOpenVpnConditions = new LinkedList<>();
        FilterRuleCondition ruleOpenVpnMatcher1 = new FilterRuleCondition();
        ruleOpenVpnMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleOpenVpnMatcher1.setValue("UDP");
        FilterRuleCondition ruleOpenVpnMatcher2 = new FilterRuleCondition();
        ruleOpenVpnMatcher2.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleOpenVpnMatcher2.setValue("1194");
        FilterRuleCondition ruleOpenVpnMatcher3 = new FilterRuleCondition();
        ruleOpenVpnMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleOpenVpnMatcher3.setValue("wan");
        ruleOpenVpnConditions.add(ruleOpenVpnMatcher1);
        ruleOpenVpnConditions.add(ruleOpenVpnMatcher2);
        ruleOpenVpnConditions.add(ruleOpenVpnMatcher3);
        filterRuleOpenVpn.setConditions( ruleOpenVpnConditions );
        
        FilterRule filterRuleBlock = new FilterRule();
        filterRuleBlock.setReadOnly( true );
        filterRuleBlock.setEnabled( true );
        filterRuleBlock.setIpv6Enabled( true );
        filterRuleBlock.setDescription( "Block All" );
        filterRuleBlock.setBlocked( true );
        filterRuleBlock.setReadOnly( true );
        List<FilterRuleCondition> rule4Conditions = new LinkedList<>();
        filterRuleBlock.setConditions( rule4Conditions );

        rules.add( filterRuleSsh );
        rules.add( filterRuleHttpsWan );
        rules.add( filterRuleHttpsNonWan );
        rules.add( filterRulePing );
        rules.add( filterRuleDns );
        rules.add( filterRuleDhcp );
        rules.add( filterRuleHttp );
        rules.add( filterRuleSnmp );
        rules.add( filterRuleUpnp );
        rules.add( filterRuleUpnpB );
        rules.add( filterRuleUpnpC );
        rules.add(filterRuleBgp);
        rules.add(filterRuleOspf);
        rules.add( filterRuleAhEsp );
        rules.add( filterRuleIke );
        rules.add( filterRuleNatT );
        rules.add( filterRuleL2tp );
        rules.add( filterRuleOpenVpn );
        rules.add( filterRuleBlock );

        return rules;
    }

    /**
     * Get a list of the default bypass rules
     * @return list
     */
    private List<BypassRule> defaultBypassRules()
    {
        List<BypassRule> rules = new LinkedList<BypassRule>();

        BypassRule filterRuleDns = new BypassRule();
        filterRuleDns.setEnabled( false );
        filterRuleDns.setDescription( "Bypass DNS Sessions" );
        filterRuleDns.setBypass( true );
        List<BypassRuleCondition> ruleDnsConditions = new LinkedList<>();
        BypassRuleCondition ruleDnsMatcher1 = new BypassRuleCondition();
        ruleDnsMatcher1.setConditionType(BypassRuleCondition.ConditionType.DST_PORT);
        ruleDnsMatcher1.setValue("53");
        ruleDnsConditions.add(ruleDnsMatcher1);
        filterRuleDns.setConditions( ruleDnsConditions );
        
        BypassRule filterRuleSip = new BypassRule();
        filterRuleSip.setEnabled( true );
        filterRuleSip.setDescription( "Bypass VoIP (SIP) Sessions" );
        filterRuleSip.setBypass( true );
        List<BypassRuleCondition> ruleSipConditions = new LinkedList<>();
        BypassRuleCondition ruleSipMatcher1 = new BypassRuleCondition();
        ruleSipMatcher1.setConditionType(BypassRuleCondition.ConditionType.DST_PORT);
        ruleSipMatcher1.setValue("5060");
        ruleSipConditions.add(ruleSipMatcher1);
        filterRuleSip.setConditions( ruleSipConditions );

        BypassRule filterRuleIax = new BypassRule();
        filterRuleIax.setEnabled( true );
        filterRuleIax.setDescription( "Bypass VoIP (IAX2) Sessions" );
        filterRuleIax.setBypass( true );
        List<BypassRuleCondition> ruleIaxConditions = new LinkedList<>();
        BypassRuleCondition ruleIaxMatcher1 = new BypassRuleCondition();
        ruleIaxMatcher1.setConditionType(BypassRuleCondition.ConditionType.DST_PORT);
        ruleIaxMatcher1.setValue("4569");
        ruleIaxConditions.add(ruleIaxMatcher1);
        filterRuleIax.setConditions( ruleIaxConditions );

        BypassRule filterRulePptp = new BypassRule();
        filterRulePptp.setEnabled( true );
        filterRulePptp.setDescription( "Bypass PPTP Sessions" );
        filterRulePptp.setBypass( true );
        List<BypassRuleCondition> rulePptpConditions = new LinkedList<>();
        BypassRuleCondition rulePptpMatcher1 = new BypassRuleCondition();
        rulePptpMatcher1.setConditionType(BypassRuleCondition.ConditionType.DST_PORT);
        rulePptpMatcher1.setValue("1723");
        rulePptpConditions.add(rulePptpMatcher1);
        BypassRuleCondition rulePptpMatcher2 = new BypassRuleCondition();
        rulePptpMatcher2.setConditionType(BypassRuleCondition.ConditionType.PROTOCOL);
        rulePptpMatcher2.setValue("TCP");
        rulePptpConditions.add(rulePptpMatcher2);
        filterRulePptp.setConditions( rulePptpConditions );
        
        rules.add( filterRuleDns );
        rules.add( filterRuleSip );
        rules.add( filterRuleIax );
        rules.add( filterRulePptp );

        return rules;
    }

    /**
     * Get a list of all ethernet devices detected
     * @return list
     */
    private LinkedList<String> getEthernetDeviceNames()
    {
        ExecManagerResult result;
        LinkedList<String> deviceNames = new LinkedList<>( );

        // add all eth* devices
        result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort -n -k 1.4" );
        for ( String name : result.getOutput().split("\\r?\\n") ) {

            String devName = name.trim();
            
            // ignore vlan devices (ie eth0.3)
            if ( devName.matches(".*\\.[0-9]+$") )  
                continue;
            // ignore blanks
            if( "".equals( devName ) )
                continue;
            
            deviceNames.add( devName );
        }

        // add all wlan* devices
        result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'wlan*' | sed -e 's|/sys/class/net/||' | sort -n -k 1.5 " );
        for ( String name : result.getOutput().split("\\r?\\n") ) {

            String devName = name.trim();
            
            // ignore vlan devices (ie eth0.3)
            if ( devName.matches(".*\\.[0-9]+$") )  
                continue;
            // ignore blanks
            if( "".equals( devName ) )
                continue;
            
            deviceNames.add( devName );
        }
        
        return deviceNames;
    }

    /**
     * Query a wireless card for a list of supported channels
     * @param systemDev
     * @return a list of the support wifi channels (never null)
     */
    public List<Integer> getWirelessChannels( String systemDev )
    {
        List<Integer> channels = new LinkedList<>();

        String infoResult = UvmContextFactory.context().execManager().execOutput( "iw " + systemDev + " info" );

        String channelPattern = "^\\s+\\* (\\d)(\\d+) MHz \\[(\\d+)\\]";
        String channelDisabledPattern = "passive scanning|no IBSS|disabled|no IR";
        String wiphyPattern = ".*wiphy (\\d)";
        Pattern channelRegex = Pattern.compile(channelPattern);
        Pattern channelDisabledRegex = Pattern.compile(channelDisabledPattern);
        Pattern wiphyRegex = Pattern.compile(wiphyPattern);
        Integer maxChannel = 0;

        // Automatic 2.4 Ghz
        // disabled NGFW-11496
        // channels.add(new Integer(-1));

        String wiphyId = "";

        try {
            String lines[] = infoResult.split("\\n");
            for ( String line : lines ) {
                Matcher match = wiphyRegex.matcher(line);
                if ( match.find() ) {
                    wiphyId = match.group(1);
                }
            }
        } catch (Exception e) {
            logger.error( "Error parsing wiphy", e );
            return channels;
        }

        if ( wiphyId.length() == 0 ) {
            logger.error( "Error parsing wiphy for dev: " + systemDev );
            return channels;
        }

        logger.debug("Query supported channels for " + systemDev + " / phy" + wiphyId + " with: iw phy" + wiphyId + " info");
        String channelResult = UvmContextFactory.context().execManager().execOutput( "iw phy" + wiphyId + " info" );

        try {
            String lines[] = channelResult.split("\\n");
            for ( String line : lines ) {
                logger.debug("Parsing line: " + line);
                Matcher match = channelRegex.matcher(line);
                Matcher disabledMatch = channelDisabledRegex.matcher(line);
                if ( match.find() ) {
                    Integer channel = Integer.valueOf(match.group(3));
                    if ( disabledMatch.find() ) {
                        logger.debug("Ignoring channel (disabled): " + channel);
                    } else {
                        logger.debug("Adding channel: " + channel);
                        channels.add(channel);
                        if (channel > maxChannel) maxChannel = channel;
                    }
                }
            }
        } catch (Exception e) {
            logger.error( "Error parsing wireless channels", e );
            return channels;
        }
	
        // Automatic 5 Ghz
        // disabled NGFW-11496
        // if (maxChannel > 11) channels.add(1, new Integer( -2 ));

        return channels;
    }

    /**
     * getUpnpManager
     * runs the upnp manager script with the specified command and argument and returns the output
     * @param command
     * @param arguments
     * @return output
     */
    public String getUpnpManager(String command, String arguments)
    {
        return UvmContextFactory.context().execManager().execOutput(upnpManagerScript + " " + command + " " + arguments);
    }

    /**
     * Iterate through all wireless interfaces set the settings to the given values
     * Used by the setup wizard
     * @param ssid
     * @param encryption
     * @param password
     */
    public void setWirelessSettings( String ssid, InterfaceSettings.WirelessEncryption encryption, String password )
    {
        boolean changed = false;

        if ( ssid == null || password == null || encryption == null ) {
            logger.warn("Invalid arguments: " + ssid + " " + password + " " + encryption );
            return;
        }

        // Bridge to Internal if it exists and is addressed, otherwise external
        int intfToBridge = 1;
        InterfaceSettings bridgedInterfaceSettings = findInterfaceId(2);
        if ( bridgedInterfaceSettings != null && bridgedInterfaceSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED ) {
            intfToBridge = 2;
        }

        int i = 0;
        for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
            if (!intf.getIsWirelessInterface())
                continue;
            i++;

            List<Integer> channels = getWirelessChannels( intf.getSystemDev() );
            if ( channels == null ) {
                logger.warn("Unabled to determine supported channels for " + intf.getSystemDev());
                continue;
            }
            int maxChannel = 0;
            int minChannel = 99999;
            for ( Integer ch : channels ) {
                if ( ch > maxChannel ) maxChannel = ch;
                if ( ch < minChannel ) minChannel = ch;
            }
            // for 2.4 default to max (11)
            // for 5.0 default to min (36)
            int defaultChannel = maxChannel;
            if ( maxChannel > 100 )
                defaultChannel = minChannel;

            intf.setIsWan( false );
            intf.setConfigType( InterfaceSettings.ConfigType.BRIDGED );
            intf.setBridgedTo( intfToBridge );

            intf.setWirelessChannel(defaultChannel);
            intf.setWirelessMode( InterfaceSettings.WirelessMode.AP );
            if ( i > 1 ) {
                intf.setWirelessSsid( ssid + "-" + i );
            } else {
                intf.setWirelessSsid( ssid );
            }
            intf.setWirelessPassword( password );
            intf.setWirelessEncryption( encryption );
        }

        setNetworkSettings( this.networkSettings );
    }

    /**
     * Find the first (lowest ID) wireless interface.
     * 
     * @return the first wireless interface settings, or null if not found
     */
    public InterfaceSettings getFirstWirelessInterface()
    {
        for (InterfaceSettings intf : this.networkSettings.getInterfaces()) {
            if (intf.getIsWirelessInterface())
                return intf;
        }
        return null;
    }

    /**
     * Get the SSID from the first wireless interface
     * If no wireless interface exists returns null
     * Used by the setup wizard
     * @return String
     */
    public String getWirelessSsid()
    {
        InterfaceSettings intf = getFirstWirelessInterface();
        if ( intf != null )
            return intf.getWirelessSsid();
        else
            return null;
    }

    /**
     * Get the password from the first wireless interface
     * If no wireless interface exists returns null
     * Used by the setup wizard
     * @return String
     */
    public String getWirelessPassword()
    {
        InterfaceSettings intf = getFirstWirelessInterface();
        if ( intf != null )
            return intf.getWirelessPassword();
        else
            return null;
    }

    /**
     * Get the encryption from the first wireless interface
     * If no wireless interface exists returns null
     * Used by the setup wizard
     * @return WirelessEncryption
     */
    public InterfaceSettings.WirelessEncryption getWirelessEncryption()
    {
        InterfaceSettings intf = getFirstWirelessInterface();
        if ( intf != null )
            return intf.getWirelessEncryption();
        else
            return null;
    }

    /**
     * Return the FQDN according to the settings
     *
     * If domain name is null it just returns the hostname
     * @return String of the FQDN name of this server, never null
     */
    public String getFullyQualifiedHostname()
    {
        String primaryAddressStr = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        if ( primaryAddressStr == null )
            primaryAddressStr = "hostname";
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        if ( domainName != null )
            primaryAddressStr = primaryAddressStr + "." + domainName;
        return primaryAddressStr;
    }
    
    /**
     * @return the public url for the box, this is the address (may be hostname or ip address)
     */
    public String getPublicUrl()
    {
        String httpsPortStr = Integer.toString( UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort() );
        String primaryAddressStr = "unconfigured.example.com";

        if ( NetworkSettings.PUBLIC_URL_EXTERNAL_IP.equals( this.networkSettings.getPublicUrlMethod() ) ) {
            InetAddress primaryAddress = UvmContextFactory.context().networkManager().getFirstWanAddress();
            if ( primaryAddress == null ) {
                logger.warn("No WAN IP found");
            } else {
                primaryAddressStr = primaryAddress.getHostAddress();
            }
        } else if ( NetworkSettings.PUBLIC_URL_HOSTNAME.equals( this.networkSettings.getPublicUrlMethod() ) ) {
            if ( UvmContextFactory.context().networkManager().getNetworkSettings().getHostName() == null ) {
                logger.warn("No hostname is configured");
            } else {
                primaryAddressStr = getFullyQualifiedHostname();
            }
        } else if ( NetworkSettings.PUBLIC_URL_ADDRESS_AND_PORT.equals( this.networkSettings.getPublicUrlMethod() ) ) {
            if ( this.networkSettings.getPublicUrlAddress() == null ) {
                logger.warn("No public address configured");
            } else {
                primaryAddressStr = this.networkSettings.getPublicUrlAddress();
                httpsPortStr = Integer.toString( this.networkSettings.getPublicUrlPort() );
            }
        } else {
            logger.warn("Unknown public URL method: " + this.networkSettings.getPublicUrlMethod() );
        }
        
        return primaryAddressStr + ":" + httpsPortStr;
    }

    /**
     * Provide the first free interface ID higher than the specified minimum
     *
     * @param netSettings - the network settings to use to check for a free ID
     * @param minimum - the minimum ID. It will start looking for a free ID at this minimum
     * @return the next available ID greater than or equal to minimum, or -1 if not found.
     */
    public int getNextFreeInterfaceId(NetworkSettings netSettings, int minimum)
    {
        if (netSettings == null)
            return minimum;
        int freeId;
        for (freeId = minimum ; freeId < InterfaceSettings.MAX_INTERFACE_ID ; freeId++) {
            boolean found = false;
            if ( netSettings.getInterfaces() != null ) {
                for ( InterfaceSettings intfSettings : netSettings.getInterfaces() ) {
                    if ( freeId == intfSettings.getInterfaceId() ) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) continue;
            if ( netSettings.getVirtualInterfaces() != null ) {
                for (InterfaceSettings intfSettings : netSettings.getVirtualInterfaces()) {
                    if ( freeId == intfSettings.getInterfaceId() ) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) continue;
            return freeId;
        }
        logger.warn("Failed to find a free interface Id (min:" + minimum + " max:"  + freeId + ")");
        return -1;
    }

    /**
     * NetworkTestDownloadHandler
     * This is the download servlet helper to allow the tcpdump test in troubleshooting
     * to download the pcap file
     */
    private class NetworkTestDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";

        /**
         * getName
         * @return name
         */
        @Override
        public String getName()
        {
            return "NetworkTestExport";
        }
        
        /**
         * serveDownload
         * @param req
         * @param resp
         */
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {
            String name = req.getParameter("arg1");

            if (name == null ) {
                logger.warn("Invalid parameters: " + name );
                return;
            }

            FileInputStream fis = null;
            OutputStream out = null;
            try{
                resp.setCharacterEncoding(CHARACTER_ENCODING);
                resp.setHeader("Content-Type","application/vnd.tcpdump.pcap");
                resp.setHeader("Content-Disposition","attachment; filename="+name+".pcap");

                byte[] buffer = new byte[1024];
                int read;
                fis = new FileInputStream(name);
                out = resp.getOutputStream();
                
                while ( ( read = fis.read( buffer ) ) > 0 ) {
                    out.write( buffer, 0, read);
                }

                out.flush();
            } catch (Exception e) {
                logger.warn("Failed to export packet trace.",e);
            } finally{
                try{
                    if(fis != null){
                        fis.close();
                    }
                    if(out != null){
                        out.close();
                    }
                }catch(IOException ex){
                    logger.error("Unable to close formatter", ex);
                }

            }
        }
    }
}
