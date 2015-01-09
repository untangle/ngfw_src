/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
import com.untangle.uvm.network.BypassRuleMatcher;
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
import com.untangle.uvm.network.DhcpStaticEntry;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.servlet.DownloadHandler;

/**
 * The Network Manager handles all the network configuration
 */
public class NetworkManagerImpl implements NetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private final String updateRulesScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-update-rules.sh";
    private final String deviceStatusScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-device-status.sh";

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network.js";
    private final String settingsFilenameBackup = "/etc/untangle-netd/network.js";
    
    private NetworkSettings networkSettings;

    /* networkListeners stores parties interested in being notified of network settings change */
    private LinkedList<NetworkSettingsListener> networkListeners = new LinkedList<NetworkSettingsListener>();
    
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
                
                if (readSettings == null) {
                    // check for "backup" settings in /usr/share/untangle/settings.backup/
                    String rootLocation = "/usr/share/untangle/settings.backup/untangle-vm/network.js";
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
            checkForNewDevices( readSettings );
            
            this.networkSettings = readSettings;
            logger.debug( "Loading Settings: " + this.networkSettings.toJSONString() );
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( this.settingsFilename );
            File interfacesFile = new File("/etc/network/interfaces");
            if (settingsFile.lastModified() > interfacesFile.lastModified() ) {
                logger.warn("Settings file newer than interfaces files, Syncing...");
                this.setNetworkSettings( this.networkSettings );
            }
        }
        
        logger.info( "Initialized NetworkManager" );
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
        String downCommand, upCommand;

        /**
         * validate settings
         * validate: routes can not route traffic to self
         */
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
            settingsManager.save(NetworkSettings.class, this.settingsFilename, newSettings);

            /**
             * If its the dev env also save to /etc
             */
            if ( UvmContextFactory.context().isDevel() ) {
                settingsManager.save(NetworkSettings.class, this.settingsFilenameBackup, newSettings, false);
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

        /**
         * Now that the settings have been successfully saved
         * Restart networking
         */
        ExecManagerResult result;
        boolean errorOccurred = false;
        String errorStr = null;

        String[] commands = getAppropriateNetworkRestartCommand( newSettings );
        downCommand = commands[0];
        upCommand = commands[1];
        
        // run down command (usually ifdown)
        result = UvmContextFactory.context().execManager().exec( downCommand );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            for ( String line : lines )
                logger.info( downCommand + ": " + line );
        } catch (Exception e) {}

        // Now sync those settings to the OS
        String cmd = "/usr/share/untangle-netd/bin/sync-settings.py -v -f " + settingsFilename;
        result = UvmContextFactory.context().execManager().exec( cmd );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("Syncing settings to O/S: ");
            for ( String line : lines )
                logger.info("sync-settings.py: " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            errorOccurred = true;
            errorStr = "sync-settings.py failed: returned " + result.getResult();
        }
        
        // run up command (usually ifup)
        result = UvmContextFactory.context().execManager().exec( upCommand );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            for ( String line : lines )
                logger.info( upCommand + ": " + line );
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            errorOccurred = true;
            errorStr = upCommand + " failed: returned " + result.getResult();
        }
        
        // notify interested parties that the settings have changed
        callNetworkListeners();

        if ( errorOccurred ) {
            throw new RuntimeException(errorStr);
        }
    }

    /**
     * Renew the DHCP lease for the provided interface
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
     * returns a list of networks already used locally
     * This can be used for verification of settings in app settings
     */
    public List<IPMaskedAddress> getCurrentlyUsedNetworks( boolean includeDynamic, boolean includeL2tp, boolean includeOpenvpn )
    {
        List<IPMaskedAddress> addresses = new LinkedList<IPMaskedAddress>();
        try {
        
            /**
             * Add static v4 addresses
             */
            for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
                if ( intf.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                    continue;
                if ( intf.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
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
                    if ( intf.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                        continue;
                    if ( intf.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
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
     */
    public InetAddress getInterfaceHttpAddress( int clientIntfId )
    {
        int intfId = clientIntfId;
        
        if ( this.networkSettings == null ) {
            logger.warn("Missing network configuration");
            return null;
        }

        /**
         * Special handling for OpenVPN
         */
        if ( intfId == 250 ) {
            InetAddress address = getInterfaceStatus( intfId ).getV4Address();
            return address;
        }
        
        /**
         * Special handling for L2TP
         */
        if ( intfId == 251 ) {
            InetAddress address = getInterfaceStatus( intfId ).getV4Address();
            return address;
        }

        /**
         * Xauth doesn't get an interface but there are port forwards in place
         * for L2TP clients so we'll just have Xauth clients use the same address
         */
        if ( intfId == 252 ) {
            InetAddress address = getInterfaceStatus( 251 ).getV4Address();
            return address;
        }
        
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
        String filename = "/var/lib/untangle-netd/interface-" + interfaceId + "-status.js";

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
     * Returns true if the specified interface is currently the master of its VRRP group
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
     * Remove the iptables rules for capturing traffic
     */
    protected void removeRules( )
    {
        int retCode = UvmContextFactory.context().execManager().execResult( "ln -fs " + this.updateRulesScript + " /etc/untangle-netd/iptables-rules.d/800-uvm" );
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
                int interfaceId = nextFreeInterfaceId( netSettings, 1 );
                interfaceSettings.setInterfaceId( interfaceId );
                interfaceSettings.setName( "Interface " + interfaceId );
                interfaceSettings.setPhysicalDev( deviceName );
                interfaceSettings.setSystemDev( deviceName );
                interfaceSettings.setSymbolicDev( deviceName );
                interfaceSettings.setIsWan( false );
                interfaceSettings.setConfigType( InterfaceSettings.ConfigType.DISABLED );

		// Check for wireless interfaces
		if (deviceName.startsWith("wlan")) {
		    interfaceSettings.setIsWirelessInterface(true);
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
    
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();
        
        try {
            newSettings.setVersion( 2 ); // Currently on v2 (as of v11.0)
            newSettings.setHostName( UvmContextFactory.context().oemManager().getOemName().toLowerCase() );
            newSettings.setDomainName( "example.com" );
            newSettings.setHttpPort( 80 );
            newSettings.setHttpsPort( 443 );
        
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
                interfaces.add(internal);
            }

            int i = 2;
            for ( devName = deviceNames.poll() ; devName != null ; devName = deviceNames.poll() ) {
                String[] greekNames = new String[]{"Alpha","Beta","Gamma","Delta","Epsilon","Zeta","Eta","Theta","Iota","Kappa","Lambda","Mu"};
                
                InterfaceSettings intf = new InterfaceSettings();
                intf.setInterfaceId( i + 1 );
                try {
                    intf.setName("Interface " + greekNames[i]);
                } catch (Exception e) {
                    intf.setName("Interface " + (i + 1));
                }

		// Check for wireless interfaces
		if (devName.startsWith("wlan")) {
		    intf.setIsWirelessInterface(true);
		}

                intf.setPhysicalDev( devName);
                intf.setSystemDev( devName );
                intf.setSymbolicDev( devName );
                intf.setConfigType( InterfaceSettings.ConfigType.DISABLED );
                interfaces.add( intf );
                i++;
            }

            newSettings.setInterfaces(interfaces);

            newSettings.setPortForwardRules( new LinkedList<PortForwardRule>() );
            newSettings.setNatRules( new LinkedList<NatRule>() );
            newSettings.setBypassRules( defaultBypassRules() );
            newSettings.setStaticRoutes( new LinkedList<StaticRoute>() );
            newSettings.setQosSettings( defaultQosSettings() );
            newSettings.setDnsSettings( new DnsSettings() );
            newSettings.setForwardFilterRules( new LinkedList<FilterRule>() );
            newSettings.setInputFilterRules( defaultInputFilterRules() );
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

    private void reconfigure()
    {
        logger.info("reconfigure()");
    }

    private void sanityCheckNetworkSettings( NetworkSettings networkSettings )
    {
        if ( networkSettings == null ) {
                throw new RuntimeException("null settings");
        }
        
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if (intf.getV4ConfigType() == null)
                throw new RuntimeException("Missing V4 Config Type");
        }

        /**
         * Check that no two statically configured interfaces have the same masked address.
         * For example, don't let people put 192.168.1.100/24 on external and 192.168.1.101/24 on internal
         * This never makes sense if the netmasks are equal
         */
        for ( InterfaceSettings intf1 : networkSettings.getInterfaces() ) {
            if ( intf1.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                continue;
            if ( intf1.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
                continue;
            if ( intf1.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                continue;
            if ( intf1.getV4StaticAddress() == null || intf1.getV4StaticPrefix() == null )
                continue;

            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( intf2.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                    continue;
                if ( intf2.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
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
            if ( intf1.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                continue;
            if ( intf1.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
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
                if ( route.getNetwork() == null )
                    continue;
                if ( route.getPrefix() == null )
                    continue;
                if ( route.getRuleId() == route2.getRuleId() )
                    continue;

                IPMaskedAddress maskedAddr1 = new IPMaskedAddress( route.getNetwork(), route.getPrefix() );
                IPMaskedAddress maskedAddr2 = new IPMaskedAddress( route2.getNetwork(), route2.getPrefix() );;
                
                if ( maskedAddr1.getMaskedAddress().equals( maskedAddr2.getMaskedAddress() ) ) {
                    throw new RuntimeException( route.getDescription() + " & " + route2.getDescription() + " route conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                }
                
            }
            
            /**
             * Check interface addresses and aliases
             */
            for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
                if ( intf.getConfigType() == InterfaceSettings.ConfigType.DISABLED )
                    continue;
                if ( intf.getConfigType() == InterfaceSettings.ConfigType.BRIDGED )
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
                if ( maskedAddr1.getMaskedAddress().equals( maskedAddr2.getMaskedAddress() ) ) {
                    throw new RuntimeException( route.getDescription() + " & " + intf.getName() + " route & address conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                }
                for ( InterfaceSettings.InterfaceAlias alias : intf.getV4Aliases() ) {
                    maskedAddr2 = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                    if ( maskedAddr1.getMaskedAddress().equals( maskedAddr2.getMaskedAddress() ) ) {
                        throw new RuntimeException( route.getDescription() + " & " + intf.getName() + " route & address conflict: " + maskedAddr1 + " = " + maskedAddr2 ); 
                    }
                }
            }
        }
    }

    private void sanitizeNetworkSettings( NetworkSettings networkSettings )
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
        idx = 0;
        for (QosRule rule : networkSettings.getQosSettings().getQosRules()) {
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
         * Handle VLAN alias interfaces
         */
        for ( InterfaceSettings intf : networkSettings.getInterfaces() ) {
            if ( ! intf.getIsVlanInterface() )
                continue;
            
            if ( intf.getInterfaceId() < 0 )
                intf.setInterfaceId( nextFreeInterfaceId( networkSettings, 100 ) );
            
            if ( intf.getVlanTag() == null )
                throw new RuntimeException("VLAN tag missing on VLAN interface");
            if ( intf.getVlanParent() == null )
                throw new RuntimeException("VLAN parent missing on VLAN interface");
            
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
        
    }

    private void sanitizeInterfaceSettings( InterfaceSettings interfaceSettings )
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
        rule1Matcher1.setValue("5060,5061");
        QosRuleMatcher rule1Matcher2 = new QosRuleMatcher();
        rule1Matcher2.setMatcherType(QosRuleMatcher.MatcherType.PROTOCOL);
        rule1Matcher2.setValue("TCP,UDP");
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
        rule2Matcher2.setValue("TCP,UDP");
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

    private List<FilterRule> defaultInputFilterRules()
    {
        List<FilterRule> rules = new LinkedList<FilterRule>();

        // enabled in dev env
        // disabled but there in normal env
        FilterRule filterRuleSsh = new FilterRule();
        filterRuleSsh.setReadOnly( true );
        filterRuleSsh.setEnabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot() );
        filterRuleSsh.setDescription( "Allow SSH" );
        filterRuleSsh.setBlocked( false );
        filterRuleSsh.setReadOnly( true );
        List<FilterRuleMatcher> ruleSshMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleSshMatcher1 = new FilterRuleMatcher();
        ruleSshMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleSshMatcher1.setValue("22");
        FilterRuleMatcher ruleSshMatcher2 = new FilterRuleMatcher();
        ruleSshMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleSshMatcher2.setValue("TCP");
        ruleSshMatchers.add(ruleSshMatcher1);
        ruleSshMatchers.add(ruleSshMatcher2);
        filterRuleSsh.setMatchers( ruleSshMatchers );

        FilterRule filterRuleHttpsWan = new FilterRule();
        filterRuleHttpsWan.setReadOnly( true );
        filterRuleHttpsWan.setEnabled( UvmContextFactory.context().isDevel() || UvmContextFactory.context().isNetBoot());
        filterRuleHttpsWan.setDescription( "Allow HTTPS on WANs" );
        filterRuleHttpsWan.setBlocked( false );
        filterRuleHttpsWan.setReadOnly( true );
        List<FilterRuleMatcher> ruleHttpsWanMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleHttpsWanMatcher1 = new FilterRuleMatcher();
        ruleHttpsWanMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleHttpsWanMatcher1.setValue("443");
        FilterRuleMatcher ruleHttpsWanMatcher2 = new FilterRuleMatcher();
        ruleHttpsWanMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleHttpsWanMatcher2.setValue("TCP");
        FilterRuleMatcher ruleHttpsWanMatcher3 = new FilterRuleMatcher();
        ruleHttpsWanMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleHttpsWanMatcher3.setValue("wan");
        ruleHttpsWanMatchers.add(ruleHttpsWanMatcher1);
        ruleHttpsWanMatchers.add(ruleHttpsWanMatcher2);
        ruleHttpsWanMatchers.add(ruleHttpsWanMatcher3);
        filterRuleHttpsWan.setMatchers( ruleHttpsWanMatchers );

        FilterRule filterRuleHttpsNonWan = new FilterRule();
        filterRuleHttpsNonWan.setReadOnly( true );
        filterRuleHttpsNonWan.setEnabled( true );
        filterRuleHttpsNonWan.setDescription( "Allow HTTPS on non-WANs" );
        filterRuleHttpsNonWan.setBlocked( false );
        filterRuleHttpsNonWan.setReadOnly( true );
        List<FilterRuleMatcher> ruleHttpsNonWanMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleHttpsNonWanMatcher1 = new FilterRuleMatcher();
        ruleHttpsNonWanMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleHttpsNonWanMatcher1.setValue("443");
        FilterRuleMatcher ruleHttpsNonWanMatcher2 = new FilterRuleMatcher();
        ruleHttpsNonWanMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleHttpsNonWanMatcher2.setValue("TCP");
        FilterRuleMatcher ruleHttpsNonWanMatcher3 = new FilterRuleMatcher();
        ruleHttpsNonWanMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleHttpsNonWanMatcher3.setValue("non_wan");
        ruleHttpsNonWanMatchers.add(ruleHttpsNonWanMatcher1);
        ruleHttpsNonWanMatchers.add(ruleHttpsNonWanMatcher2);
        ruleHttpsNonWanMatchers.add(ruleHttpsNonWanMatcher3);
        filterRuleHttpsNonWan.setMatchers( ruleHttpsNonWanMatchers );

        FilterRule filterRulePing = new FilterRule();
        filterRulePing.setReadOnly( true );
        filterRulePing.setEnabled( true );
        filterRulePing.setDescription( "Allow PING" );
        filterRulePing.setBlocked( false );
        filterRulePing.setReadOnly( true );
        List<FilterRuleMatcher> rulePingMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher rulePingMatcher1 = new FilterRuleMatcher();
        rulePingMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        rulePingMatcher1.setValue("ICMP");
        rulePingMatchers.add(rulePingMatcher1);
        filterRulePing.setMatchers( rulePingMatchers );

        FilterRule filterRuleDns = new FilterRule();
        filterRuleDns.setReadOnly( true );
        filterRuleDns.setEnabled( true );
        filterRuleDns.setDescription( "Allow DNS on non-WANs" );
        filterRuleDns.setBlocked( false );
        List<FilterRuleMatcher> ruleDnsMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleDnsMatcher1 = new FilterRuleMatcher();
        ruleDnsMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleDnsMatcher1.setValue("53");
        FilterRuleMatcher ruleDnsMatcher2 = new FilterRuleMatcher();
        ruleDnsMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleDnsMatcher2.setValue("TCP,UDP");
        FilterRuleMatcher ruleDnsMatcher3 = new FilterRuleMatcher();
        ruleDnsMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleDnsMatcher3.setValue("non_wan");
        ruleDnsMatchers.add(ruleDnsMatcher1);
        ruleDnsMatchers.add(ruleDnsMatcher2);
        ruleDnsMatchers.add(ruleDnsMatcher3);
        filterRuleDns.setMatchers( ruleDnsMatchers );

        FilterRule filterRuleDhcp = new FilterRule();
        filterRuleDhcp.setReadOnly( true );
        filterRuleDhcp.setEnabled( true );
        filterRuleDhcp.setDescription( "Allow DHCP on non-WANs" );
        filterRuleDhcp.setBlocked( false );
        filterRuleDhcp.setReadOnly( true );
        List<FilterRuleMatcher> ruleDhcpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleDhcpMatcher1 = new FilterRuleMatcher();
        ruleDhcpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleDhcpMatcher1.setValue("67");
        FilterRuleMatcher ruleDhcpMatcher2 = new FilterRuleMatcher();
        ruleDhcpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleDhcpMatcher2.setValue("UDP");
        FilterRuleMatcher ruleDhcpMatcher3 = new FilterRuleMatcher();
        ruleDhcpMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleDhcpMatcher3.setValue("non_wan");
        ruleDhcpMatchers.add(ruleDhcpMatcher1);
        ruleDhcpMatchers.add(ruleDhcpMatcher2);
        ruleDhcpMatchers.add(ruleDhcpMatcher3);
        filterRuleDhcp.setMatchers( ruleDhcpMatchers );
        
        FilterRule filterRuleHttp = new FilterRule();
        filterRuleHttp.setReadOnly( true );
        filterRuleHttp.setEnabled( true );
        filterRuleHttp.setDescription( "Allow HTTP on non-WANs" );
        filterRuleHttp.setBlocked( false );
        filterRuleHttp.setReadOnly( true );
        List<FilterRuleMatcher> ruleHttpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleHttpMatcher1 = new FilterRuleMatcher();
        ruleHttpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleHttpMatcher1.setValue("80");
        FilterRuleMatcher ruleHttpMatcher2 = new FilterRuleMatcher();
        ruleHttpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleHttpMatcher2.setValue("TCP");
        FilterRuleMatcher ruleHttpMatcher3 = new FilterRuleMatcher();
        ruleHttpMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleHttpMatcher3.setValue("non_wan");
        ruleHttpMatchers.add(ruleHttpMatcher1);
        ruleHttpMatchers.add(ruleHttpMatcher2);
        ruleHttpMatchers.add(ruleHttpMatcher3);
        filterRuleHttp.setMatchers( ruleHttpMatchers );

        FilterRule filterRuleSnmp = new FilterRule();
        filterRuleSnmp.setReadOnly( true );
        filterRuleSnmp.setEnabled( true );
        filterRuleSnmp.setDescription( "Allow SNMP on non-WANs" );
        filterRuleSnmp.setBlocked( false );
        filterRuleSnmp.setReadOnly( true );
        List<FilterRuleMatcher> ruleSnmpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleSnmpMatcher1 = new FilterRuleMatcher();
        ruleSnmpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleSnmpMatcher1.setValue("161");
        FilterRuleMatcher ruleSnmpMatcher2 = new FilterRuleMatcher();
        ruleSnmpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleSnmpMatcher2.setValue("UDP");
        FilterRuleMatcher ruleSnmpMatcher3 = new FilterRuleMatcher();
        ruleSnmpMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleSnmpMatcher3.setValue("non_wan");
        ruleSnmpMatchers.add(ruleSnmpMatcher1);
        ruleSnmpMatchers.add(ruleSnmpMatcher2);
        ruleSnmpMatchers.add(ruleSnmpMatcher3);
        filterRuleSnmp.setMatchers( ruleSnmpMatchers );

        FilterRule filterRuleAhEsp = new FilterRule();
        filterRuleAhEsp.setReadOnly( true );
        filterRuleAhEsp.setEnabled( true );
        filterRuleAhEsp.setDescription( "Allow AH/ESP for IPsec" );
        filterRuleAhEsp.setBlocked( false );
        filterRuleAhEsp.setReadOnly( true );
        List<FilterRuleMatcher> ruleAhEspMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleAhEspMatcher1 = new FilterRuleMatcher();
        ruleAhEspMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleAhEspMatcher1.setValue("AH,ESP");
        ruleAhEspMatchers.add(ruleAhEspMatcher1);
        filterRuleAhEsp.setMatchers( ruleAhEspMatchers );

        FilterRule filterRuleIke = new FilterRule();
        filterRuleIke.setReadOnly( true );
        filterRuleIke.setEnabled( true );
        filterRuleIke.setDescription( "Allow IKE for IPsec" );
        filterRuleIke.setBlocked( false );
        filterRuleIke.setReadOnly( true );
        List<FilterRuleMatcher> ruleIkeMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleIkeMatcher1 = new FilterRuleMatcher();
        ruleIkeMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleIkeMatcher1.setValue("UDP");
        FilterRuleMatcher ruleIkeMatcher2 = new FilterRuleMatcher();
        ruleIkeMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleIkeMatcher2.setValue("500");
        ruleIkeMatchers.add(ruleIkeMatcher1);
        ruleIkeMatchers.add(ruleIkeMatcher2);
        filterRuleIke.setMatchers( ruleIkeMatchers );

        FilterRule filterRuleNatT = new FilterRule();
        filterRuleNatT.setReadOnly( true );
        filterRuleNatT.setEnabled( true );
        filterRuleNatT.setDescription( "Allow NAT-T for IPsec" );
        filterRuleNatT.setBlocked( false );
        filterRuleNatT.setReadOnly( true );
        List<FilterRuleMatcher> ruleNatTMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleNatTMatcher1 = new FilterRuleMatcher();
        ruleNatTMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleNatTMatcher1.setValue("UDP");
        FilterRuleMatcher ruleNatTMatcher2 = new FilterRuleMatcher();
        ruleNatTMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleNatTMatcher2.setValue("4500");
        ruleNatTMatchers.add(ruleNatTMatcher1);
        ruleNatTMatchers.add(ruleNatTMatcher2);
        filterRuleNatT.setMatchers( ruleNatTMatchers );

        FilterRule filterRuleL2tp = new FilterRule();
        filterRuleL2tp.setReadOnly( true );
        filterRuleL2tp.setEnabled( true );
        filterRuleL2tp.setDescription( "Allow L2TP" );
        filterRuleL2tp.setBlocked( false );
        filterRuleL2tp.setReadOnly( true );
        List<FilterRuleMatcher> ruleL2tpMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleL2tpMatcher1 = new FilterRuleMatcher();
        ruleL2tpMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleL2tpMatcher1.setValue("UDP");
        FilterRuleMatcher ruleL2tpMatcher2 = new FilterRuleMatcher();
        ruleL2tpMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleL2tpMatcher2.setValue("1701");
        ruleL2tpMatchers.add(ruleL2tpMatcher1);
        ruleL2tpMatchers.add(ruleL2tpMatcher2);
        filterRuleL2tp.setMatchers( ruleL2tpMatchers );

        FilterRule filterRuleOpenVpn = new FilterRule();
        filterRuleOpenVpn.setReadOnly( true );
        filterRuleOpenVpn.setEnabled( true );
        filterRuleOpenVpn.setDescription( "Allow OpenVPN" );
        filterRuleOpenVpn.setBlocked( false );
        filterRuleOpenVpn.setReadOnly( true );
        List<FilterRuleMatcher> ruleOpenVpnMatchers = new LinkedList<FilterRuleMatcher>();
        FilterRuleMatcher ruleOpenVpnMatcher1 = new FilterRuleMatcher();
        ruleOpenVpnMatcher1.setMatcherType(FilterRuleMatcher.MatcherType.PROTOCOL);
        ruleOpenVpnMatcher1.setValue("UDP");
        FilterRuleMatcher ruleOpenVpnMatcher2 = new FilterRuleMatcher();
        ruleOpenVpnMatcher2.setMatcherType(FilterRuleMatcher.MatcherType.DST_PORT);
        ruleOpenVpnMatcher2.setValue("1194");
        FilterRuleMatcher ruleOpenVpnMatcher3 = new FilterRuleMatcher();
        ruleOpenVpnMatcher3.setMatcherType(FilterRuleMatcher.MatcherType.SRC_INTF);
        ruleOpenVpnMatcher3.setValue("wan");
        ruleOpenVpnMatchers.add(ruleOpenVpnMatcher1);
        ruleOpenVpnMatchers.add(ruleOpenVpnMatcher2);
        ruleOpenVpnMatchers.add(ruleOpenVpnMatcher3);
        filterRuleOpenVpn.setMatchers( ruleOpenVpnMatchers );
        
        FilterRule filterRuleBlock = new FilterRule();
        filterRuleBlock.setReadOnly( true );
        filterRuleBlock.setEnabled( true );
        filterRuleBlock.setDescription( "Block All" );
        filterRuleBlock.setBlocked( true );
        filterRuleBlock.setReadOnly( true );
        List<FilterRuleMatcher> rule4Matchers = new LinkedList<FilterRuleMatcher>();
        filterRuleBlock.setMatchers( rule4Matchers );
        
        rules.add( filterRuleSsh );
        rules.add( filterRuleHttpsWan );
        rules.add( filterRuleHttpsNonWan );
        rules.add( filterRulePing );
        rules.add( filterRuleDns );
        rules.add( filterRuleDhcp );
        rules.add( filterRuleHttp );
        rules.add( filterRuleSnmp );
        rules.add( filterRuleAhEsp );
        rules.add( filterRuleIke );
        rules.add( filterRuleNatT );
        rules.add( filterRuleL2tp );
        rules.add( filterRuleOpenVpn );
        rules.add( filterRuleBlock );

        return rules;
    }

    private List<BypassRule> defaultBypassRules()
    {
        List<BypassRule> rules = new LinkedList<BypassRule>();

        BypassRule filterRuleDns = new BypassRule();
        filterRuleDns.setEnabled( false );
        filterRuleDns.setDescription( "Bypass DNS Sessions" );
        filterRuleDns.setBypass( true );
        List<BypassRuleMatcher> ruleDnsMatchers = new LinkedList<BypassRuleMatcher>();
        BypassRuleMatcher ruleDnsMatcher1 = new BypassRuleMatcher();
        ruleDnsMatcher1.setMatcherType(BypassRuleMatcher.MatcherType.DST_PORT);
        ruleDnsMatcher1.setValue("53");
        ruleDnsMatchers.add(ruleDnsMatcher1);
        filterRuleDns.setMatchers( ruleDnsMatchers );
        
        BypassRule filterRuleSip = new BypassRule();
        filterRuleSip.setEnabled( true );
        filterRuleSip.setDescription( "Bypass VoIP (SIP) Sessions" );
        filterRuleSip.setBypass( true );
        List<BypassRuleMatcher> ruleSipMatchers = new LinkedList<BypassRuleMatcher>();
        BypassRuleMatcher ruleSipMatcher1 = new BypassRuleMatcher();
        ruleSipMatcher1.setMatcherType(BypassRuleMatcher.MatcherType.DST_PORT);
        ruleSipMatcher1.setValue("5060");
        ruleSipMatchers.add(ruleSipMatcher1);
        filterRuleSip.setMatchers( ruleSipMatchers );

        BypassRule filterRuleIax = new BypassRule();
        filterRuleIax.setEnabled( true );
        filterRuleIax.setDescription( "Bypass VoIP (IAX2) Sessions" );
        filterRuleIax.setBypass( true );
        List<BypassRuleMatcher> ruleIaxMatchers = new LinkedList<BypassRuleMatcher>();
        BypassRuleMatcher ruleIaxMatcher1 = new BypassRuleMatcher();
        ruleIaxMatcher1.setMatcherType(BypassRuleMatcher.MatcherType.DST_PORT);
        ruleIaxMatcher1.setValue("4569");
        ruleIaxMatchers.add(ruleIaxMatcher1);
        filterRuleIax.setMatchers( ruleIaxMatchers );

        BypassRule filterRulePptp = new BypassRule();
        filterRulePptp.setEnabled( true );
        filterRulePptp.setDescription( "Bypass PPTP Sessions" );
        filterRulePptp.setBypass( true );
        List<BypassRuleMatcher> rulePptpMatchers = new LinkedList<BypassRuleMatcher>();
        BypassRuleMatcher rulePptpMatcher1 = new BypassRuleMatcher();
        rulePptpMatcher1.setMatcherType(BypassRuleMatcher.MatcherType.DST_PORT);
        rulePptpMatcher1.setValue("1723");
        rulePptpMatchers.add(rulePptpMatcher1);
        BypassRuleMatcher rulePptpMatcher2 = new BypassRuleMatcher();
        rulePptpMatcher2.setMatcherType(BypassRuleMatcher.MatcherType.PROTOCOL);
        rulePptpMatcher2.setValue("TCP");
        rulePptpMatchers.add(rulePptpMatcher2);
        filterRulePptp.setMatchers( rulePptpMatchers );
        
        rules.add( filterRuleDns );
        rules.add( filterRuleSip );
        rules.add( filterRuleIax );
        rules.add( filterRulePptp );

        return rules;
    }

    private int nextFreeInterfaceId( NetworkSettings netSettings, int min)
    {
        if (netSettings == null)
            return min;
        int free = min;
        for ( InterfaceSettings intfSettings : netSettings.getInterfaces() ) {
            if ( free <= intfSettings.getInterfaceId() )
                free = intfSettings.getInterfaceId() + 1;
        }
        return free;
    }

    private LinkedList<String> getEthernetDeviceNames()
    {
        ExecManagerResult result;
        LinkedList<String> deviceNames = new LinkedList<String>( );

        // add all eth* devices
        result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort " );
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
        result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'wlan*' | sed -e 's|/sys/class/net/||' | sort " );
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
     * This method predicts the files that will change when syncing the new settings to the O/S
     * It returns a list of files that will change (in content)
     * If the prediction fails, null is returned
     */
    private LinkedList<String> predictUpdatedFiles( NetworkSettings newSettings )
    {
        ExecManagerResult result;
        int retCode;
        LinkedList<String> changedFiles = new LinkedList<String>();
        Path tmpDir = null;
        String cmd;
        
        try {
            tmpDir = Files.createTempDirectory( "tmp-sync-settings" );

            // apply settings in new dir
            cmd = "/usr/share/untangle-netd/bin/sync-settings.py -v -f " + settingsFilename + " -p " + tmpDir.getFileName();
            retCode = UvmContextFactory.context().execManager().execResult( cmd );

            if ( retCode != 0 ) {
                logger.warn( "sync-settings.py failed: returned " + retCode );
                return null;
            }

            cmd = "diff -rqP / " + tmpDir + " | grep -v '^Only in' | awk '{print $2}'";
            result = UvmContextFactory.context().execManager().exec( cmd );
            
            if ( result.getResult() != 0 ) {
                logger.warn( "diff failed: returned " + result.getResult() );
                return null;
            }
            try {
                String lines[] = result.getOutput().split("\\r?\\n");
                for ( String line : lines ) {
                    String filename = line.replaceAll("\\s","");
                    if ( ! "".equals( filename ) )
                        changedFiles.add( filename );
                }
            } catch (Exception e) {}
        } catch ( Exception e ) {
            logger.warn( "Failed to predict changed files", e );
            try { Files.delete( tmpDir ); } catch ( Exception exc ) { logger.warn("Failed to delete directory " + tmpDir, exc); }
        }

        return changedFiles;
    }

    /**
     * Usually when saving network settings we need to restart all of networking.
     * However, in a few cases we can get away with just restarting a daemon or two.
     *
     * This method computes what is necessary for the provided new NetworkSettings.
     * 
     * Returns a string array. The first entry is the command to run before
     * syncing settings, the second is the command to run after syncing settings
     */
    private String[] getAppropriateNetworkRestartCommand( NetworkSettings newSettings )
    {
        // default fullRestartCommands are full restart
        //String[] fullRestartCommands = {"ifdown -a --exclude=lo", "ifup -a --exclude=lo"};
        String[] fullRestartCommands = {"ifdown -a -v --exclude=lo", "ifup -a -v --exclude=lo"};

        try {
            LinkedList<String> changedFiles = predictUpdatedFiles( newSettings );

            /**
             * prediction failed, just do a full restart
             */
            if ( changedFiles == null )
                return fullRestartCommands;

            /**
             * If nothing is new, we still do a full networking restart
             * This is because conversions and the ability to resync by toggling settings
             * This is for safety, when in doubt, do a full sync.
             */
            if ( changedFiles.size() == 0 ) {
                logger.info("No config files changed. Syncing settings anyway...");
                return fullRestartCommands;
            }

            for ( String filename : changedFiles )
                logger.info("Changing file: " + filename);

            /**
             * If only /etc/hosts and /etc/hosts.dnsmasq have been written, nothing is needed!
             */
            if ( changedFiles.contains("/etc/hosts") && changedFiles.contains("/etc/hosts.dnsmasq") && changedFiles.size() == 2 ) {
                return new String[] {"/bin/true", "/etc/untangle-netd/post-network-hook.d/990-restart-dnsmasq"};
            }

            /**
             * If only /etc/dnsmasq.conf has been written, just restart dnsmasq
             */
            if ( changedFiles.contains("/etc/dnsmasq.conf") && changedFiles.size() == 1 ) {
                return new String[] {"/bin/true", "/etc/untangle-netd/post-network-hook.d/990-restart-dnsmasq"};
            }

            /**
             * If only /etc/untangle-netd/iptables-rules.d/* files are changed, just restart iptables rules
             */
            int count = 0;
            for ( String changedFile : changedFiles ) {
                if ( changedFile.startsWith("/etc/untangle-netd/iptables-rules.d/") )
                    count++;
            }
            if ( count == changedFiles.size() ) {
                return new String[] {"/bin/true", "/etc/untangle-netd/post-network-hook.d/960-iptables"};
            }
        }
        catch ( Exception e ) {
            logger.warn("Exception",e);
        }
        
        return fullRestartCommands;
    }

    /**
     * Query a wireless card for a list of supported channels
     */

    public List<Integer> getWirelessChannels( String systemDev )
    {
	List<Integer> channels = new LinkedList<Integer>();

	String infoResult = UvmContextFactory.context().execManager().execOutput( "iw " + systemDev + " info" );

	String channelPattern = "^\\s+\\* (\\d)(\\d+) MHz \\[(\\d+)\\]";
	String channelDisabledPattern = "passive scanning|no IBSS|disabled";
	String wiphyPattern = ".*wiphy (\\d)";
	Pattern channelRegex = Pattern.compile(channelPattern);
	Pattern channelDisabledRegex = Pattern.compile(channelDisabledPattern);
	Pattern wiphyRegex = Pattern.compile(wiphyPattern);
	Integer maxChannel = 0;

	channels.add(new Integer(-1));

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

	String channelResult = UvmContextFactory.context().execManager().execOutput( "iw wiphy" + wiphyId + " info" );

	try {
            String lines[] = channelResult.split("\\n");
            for ( String line : lines ) {
		Matcher match = channelRegex.matcher(line);
		Matcher disabledMatch = channelDisabledRegex.matcher(line);
		if ( match.find() && !disabledMatch.find() ) {
		    Integer channel = Integer.valueOf(match.group(3));
		    channels.add(channel);
		    if (channel > maxChannel) maxChannel = channel;
                }
            }
        } catch (Exception e) {
            logger.error( "Error parsing wireless channels", e );
            return channels;
        }
	
	if (maxChannel > 11) channels.add(1, new Integer( -2 ));

	return channels;
    }

    private class NetworkTestDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";

        @Override
        public String getName()
        {
            return "NetworkTestExport";
        }
        
        public void serveDownload( HttpServletRequest req, HttpServletResponse resp )
        {
            String name = req.getParameter("arg1");

            if (name == null ) {
                logger.warn("Invalid parameters: " + name );
                return;
            }

            try{
                resp.setCharacterEncoding(CHARACTER_ENCODING);
                resp.setHeader("Content-Type","application/vnd.tcpdump.pcap");
                resp.setHeader("Content-Disposition","attachment; filename="+name+".pcap");

                byte[] buffer = new byte[1024];
                int read;
                FileInputStream fis = new FileInputStream(name);
                OutputStream out = resp.getOutputStream();
                
                while ( ( read = fis.read( buffer ) ) > 0 ) {
                    out.write( buffer, 0, read);
                }

                fis.close();
                out.flush();
                out.close();

            } catch (Exception e) {
                logger.warn("Failed to export packet trace.",e);
            }
        }
    }
}
