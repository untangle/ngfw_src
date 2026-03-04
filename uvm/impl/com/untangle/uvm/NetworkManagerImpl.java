/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.RuleCondition;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.BypassRuleCondition;
import com.untangle.uvm.network.DeviceSettings;
import com.untangle.uvm.network.DeviceStatus;
import com.untangle.uvm.network.DeviceStatus.ConnectedStatus;
import com.untangle.uvm.network.DeviceStatus.DuplexStatus;
import com.untangle.uvm.network.DhcpRelay;
import com.untangle.uvm.network.DhcpStaticEntry;
import com.untangle.uvm.network.DnsSettings;
import com.untangle.uvm.network.DynamicRouteBgpNeighbor;
import com.untangle.uvm.network.DynamicRouteNetwork;
import com.untangle.uvm.network.DynamicRouteOspfArea;
import com.untangle.uvm.network.DynamicRouteOspfInterface;
import com.untangle.uvm.network.DynamicRoutingSettings;
import com.untangle.uvm.network.FilterRule;
import com.untangle.uvm.network.FilterRuleCondition;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceSettings.ConfigType;
import com.untangle.uvm.network.InterfaceSettings.V4ConfigType;
import com.untangle.uvm.network.InterfaceSettings.V6ConfigType;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.NatRuleCondition;
import com.untangle.uvm.network.NetflowSettings;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.PortForwardRuleCondition;
import com.untangle.uvm.network.QosPriority;
import com.untangle.uvm.network.QosRule;
import com.untangle.uvm.network.QosRuleCondition;
import com.untangle.uvm.network.QosSettings;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.UpnpRule;
import com.untangle.uvm.network.UpnpRuleCondition;
import com.untangle.uvm.network.UpnpSettings;
import com.untangle.uvm.network.generic.InterfaceSettingsGeneric;
import com.untangle.uvm.network.generic.InterfaceStatusGeneric;
import com.untangle.uvm.network.generic.NetworkSettingsGeneric;
import com.untangle.uvm.servlet.DownloadHandler;
import com.untangle.uvm.util.ObjectMatcher;
import com.untangle.uvm.util.StringUtil;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jabsorb.serializer.UnmarshallException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Network Manager handles all the network configuration
 */
public class NetworkManagerImpl implements NetworkManager
{
    public static final String MAC = "MAC";
    public static final String ORGANIZATION = "Organization";
    private static final String INET = "inet";
    private static final String INET6 = "inet6";
    private static final String DHCP = "dhcp";
    private static final String DHCPV6 = "dhcpv6";
    public static final String COMMA = ",";
    private final Logger logger = LogManager.getLogger(this.getClass());

    private final String updateRulesScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-update-rules.sh";
    private final String deviceStatusScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-device-status.sh";
    private final String upnpManagerScript = System.getProperty("uvm.bin.dir") + "/ut-upnp-manager";
    private final String wirelessInterfaceScript = System.getProperty("uvm.bin.dir") + "/wireless-interface.py";
    private final String statusScript = System.getProperty("uvm.bin.dir") + "/network-status.sh";
    private final String troubleshootingScript = System.getProperty("uvm.bin.dir") + "/network-troubleshooting.sh";

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network.js";
    private final String settingsFilenameBackup = "/etc/untangle/network.js";

    private static String NETSPACE_OWNER = "networking";
    private static String NETSPACE_STATIC_ADDRESS = "static-address";
    private static String NETSPACE_STATIC_ALIAS = "static-alias";
    private static String NETSPACE_DYNAMIC_ADDRESS = "dynamic-address";
    private final static String GET_LOGFILE_SCRIPT = System.getProperty("uvm.home") + "/bin/hostapd-logfile";
    private static String INTERFACE_NAME_PATTERN = "[a-zA-Z0-9._:-]{1,32}";
    private static String REGION_NAME_PATTERN = "[A-Z]{2}";

    // creating a cache for the lookedup mac addresses and vendors
    private static ConcurrentMap<String,String> cachedMacAddrVendorList = new ConcurrentHashMap<>();

    /**
     * The current network settings
     */
    private NetworkSettings networkSettings;
    private Integer currentVersion = 12;

    /**
     * This array holds the current interface Settings indexed by the interface ID.
     * This enabled fast lookups with iterating the list in findInterfaceId()
     */
    private InterfaceSettings[] interfaceSettingsById = new InterfaceSettings[InterfaceSettings.MAX_INTERFACE_ID + 1];

    /**
     * This flag indicates if the physical interfaces have been overloaded.
     * It is set when a physical interface cannot get a free interfaceId. This likely means the user has:
     * 1. Filled up all of the virtual interfaces
     * 2. Attempted to add a physical interface
     */
    private boolean interfacesOverloadedFlag = false;

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
            logger.warn("Failed to load settings:", e );
        }

        /**
         * If its the development environment, try loading settings from /etc
         * We do this because we frequently nuke dist/ in the development environment
         * and this assures we keep the networking settings by saving them outside dist/
         */
        if ( readSettings == null && UvmContextFactory.context().isDevel() ) {
            try {
                // check for "backup" settings in /etc
                logger.info("Reading Network Settings from {}", this.settingsFilenameBackup);
                readSettings = settingsManager.load( NetworkSettings.class, this.settingsFilenameBackup );
                logger.info("Reading Network Settings from {} = {}", this.settingsFilenameBackup , readSettings);
                
                if (readSettings != null)
                    settingsManager.save( this.settingsFilename, readSettings );
                    
            } catch ( SettingsManager.SettingsException e ) {
                logger.warn("Failed to load settings:", e );
            }
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setNetworkSettings( defaultSettings() );

            // apply oem settings
	    // some oems require bridged interfaces, and we're not able to sanityCheck bridged
	    // interfaces without the interfaceSettingsById array set, which happens
	    // after defaultSettings are set to this.networkSettings, after a sanityCheck should be performed. 
	    // So we run set network settings twice, once for defaultSettings so the 
	    // interfaceSettingsById array is set, and a second time with oem settings set
            if (UvmContextFactory.context().oemManager().hasOemOverrideFile()) {
                this.applyOemSettings();
            }
        }
        else {
            checkForNewDevices( readSettings );
            checkForRemovedDevices( readSettings );
            
            this.networkSettings = readSettings;
            updateNetworkReservations(readSettings);
            configureInterfaceSettingsArray();

            if ( this.networkSettings.getVersion() < currentVersion ) {
                convertSettings();
            }
            if (logger.isDebugEnabled())
                logger.debug("Loading Settings: {}", this.networkSettings.toJSONString() );
        }

        /**
         * Fix missing access rules NGFW-11503
         */
        if (this.networkSettings.getAccessRules() == null) {
            this.networkSettings.setAccessRules( defaultAccessRules() );
            this.setNetworkSettings( this.networkSettings, false );
        }

        /**
         * Check for missing RADIUS rules NGFW-13560
         */
        checkForRadiusRules();

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
        
        logger.info("Initialized NetworkManager");
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
     * Get the v2 network settings
     * @return NetworkSettingsV2
     */
    public NetworkSettingsGeneric getNetworkSettingsV2() {
        return this.networkSettings.transformNetworkSettingsToGeneric();
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
     * Set the network settings V2
     * @param newSettings
     */
    public void setNetworkSettingsV2( NetworkSettingsGeneric newSettings )
    {
        // Deep clone current Network Settings to transform in New Network Settings
        NetworkSettings clonedNetworkSettings = SerializationUtils.clone(this.networkSettings);
        newSettings.transformGenericToNetworkSettings(clonedNetworkSettings);
        setNetworkSettings( clonedNetworkSettings, true );
    }

    /**
     * Set the network settings
     * @param newSettings
     * @param runSanityChecks - if true sanityCheckNetworkSettings is called
     */
    public void setNetworkSettings( NetworkSettings newSettings, boolean runSanityChecks )
    {
        String downCommand, upCommand;

        // notify interested parties that the network settings are about to change (send old settings in args)
        UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.PRE_NETWORK_SETTINGS_CHANGE, this.networkSettings );

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
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.networkSettings = newSettings;
        updateNetworkReservations(newSettings);
        configureInterfaceSettingsArray();
        try {
            if (logger.isDebugEnabled())
                logger.debug("New Settings: \n{}", new org.json.JSONObject(this.networkSettings).toString(2));
        } catch (Exception e) {}

        UvmContextFactory.context().syncSettings().run(this.settingsFilename);
        
        // notify interested parties that the settings have changed
        UvmContextFactory.context().hookManager().callCallbacksSynchronous( HookManager.NETWORK_SETTINGS_CHANGE, this.networkSettings );
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
            logger.warn("Interface not found. Unable to renew DHCP lease on interface {}", interfaceId);
            return;
        }
        String devName = intfSettings.getSymbolicDev();
        if ( devName == null ) {
            logger.warn("Interface missing systemDev. Unable to renew DHCP lease on interface {}", interfaceId);
            return;
        }
        if ( intfSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.AUTO ) {
            logger.warn("Interface not type AUTO. Unable to renew DHCP lease on interface {}", interfaceId);
            return;
        }
        
        // just bring the interface up and down 
        result = UvmContextFactory.context().execManager().exec( "ifdown " + devName );
        try {
            String[] lines = result.getOutput().split("\\r?\\n");
            logger.info("ifdown {}: ", devName );
            for ( String line : lines )
                logger.info("ifdown: {}", line);
        } catch (Exception e) {}

        result = UvmContextFactory.context().execManager().exec( "ifup " + devName );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info("ifup {}: ", devName );
            for ( String line : lines )
                logger.info("ifup: {}", line);
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
            logger.warn("Missing network settings.");
            return null;
        }

        if ( interfaceId < 0 || interfaceId > 255 ) {
            logger.warn("Invalid interface ID: {}", interfaceId );
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
            logger.warn("Unknown interface: {}", interfaceId, new Exception());
            return false;
        }

        return intfSettings.getIsWan();
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
            logger.warn("Failed to find interface {}", intfId);
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
                logger.warn("No Interface found for name: {}", bridgedTo );
                return null;
            } 

            intfId = intfSettings.getInterfaceId();
        }

        InetAddress address = getInterfaceStatus( intfId ).getV4Address();
        return address;
    }

    /**
     * Search interfaces and get the first non-WAN DNS resolver.
     * 
     * First look at all interfaces that have DHCP enabled and get the first DNS resolver.
     * If not found, get first non-WAN IP address.
     @return InetAddress of address.
     */
    public InetAddress getFirstDnsResolverAddress()
    {
        InetAddress address = null;
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null ) {
            return address;
        }

        try{
            for ( InterfaceSettings intfSettings : this.networkSettings.getInterfaces() ) {
                if ( !intfSettings.igetDisabled() && !intfSettings.getIsWan() ) {
                    if(address == null){
                        address = getInterfaceStatus( intfSettings.getInterfaceId() ).getV4Address();
                    }
                    if(intfSettings.getDhcpType() == InterfaceSettings.DhcpType.SERVER &&
                        intfSettings.getDhcpDnsOverride() != null &&
                        !intfSettings.getDhcpDnsOverride().equals("")){
                            InetAddress dhcpOverride = InetAddress.getByName(intfSettings.getDhcpDnsOverride());
                            if(dhcpOverride != null){
                                address = dhcpOverride;
                                break;
                            }
                    }
                }
            }
        }catch(Exception e){
            logger.warn("Unable to parse interfaces for dhcpDnsOverride:", e);
        }

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
            logger.warn("Failed to load settings:", e);
            return null;
        }

        if (status == null) {
            status = new InterfaceStatus(); // never return null
        }
        status.setInterfaceId(interfaceId); //Interface id must be set in all cases. It is not stored in interface-<interfaceId>-status.js file
        return status;
    }

    /**
     * Method to get all interfaces' status.
     * @return List of InterfaceStatusGeneric
     */
    public List<InterfaceStatusGeneric> getAllInterfacesStatusV2() {
        List<DeviceStatus> deviceStatusList = getDeviceStatus();

        return networkSettings.getInterfaces().stream()
                .map(intf -> buildInterfaceStatus(intf.resolveDeviceName(), intf, deviceStatusList))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Method to get status for a specific interface by device name.
     * @param device the resolved device name (e.g., eth0, eth1.12, wlan0)
     * @return InterfaceStatusGeneric for the device, or null if not found
     */
    public InterfaceStatusGeneric getInterfaceStatusV2(String device) {
        List<DeviceStatus> deviceStatusList = getDeviceStatus();

        return networkSettings.getInterfaces().stream()
                .filter(intf -> device.equals(intf.resolveDeviceName()))
                .findFirst()
                .map(intf -> buildInterfaceStatus(device, intf, deviceStatusList))
                .orElse(null);
    }

    /**
     * Helper: Build populated InterfaceStatusGeneric
     * @param device device name
     * @param intf InterfaceSettings
     * @param deviceStatusList List<DeviceStatus>
     * @return InterfaceStatusGeneric
     */
    private InterfaceStatusGeneric buildInterfaceStatus(String device, InterfaceSettings intf, List<DeviceStatus> deviceStatusList) {
        InterfaceStatusGeneric status = new InterfaceStatusGeneric();

        status.setDevice(device);
        status.setWan(intf.getIsWan());
        status.setInterfaceId(intf.getInterfaceId());
        populateTransferStats(status, intf);
        populateMacVendor(status);
        populateIpAddresses(status, intf);
        populateConnectionStatus(status, intf, deviceStatusList);
        populateGatewayAndDns(status, intf);
        populateAddressSources(status, intf);

        return status;
    }

    /** 
     * Populates transfer statistics such as rx/tx bytes, packets, errors, and drops. 
     * @param status InterfaceStatusGeneric
     * @param intf InterfaceSettings
     */
    private void populateTransferStats(InterfaceStatusGeneric status, InterfaceSettings intf) {
        String intfTransfer = getStatus(StatusCommands.INTERFACE_TRANSFER, intf.getSymbolicDev());
        String[] stats = intfTransfer.trim().split("\\s+");

        if (stats.length < 12) return;

        status.setMacAddress(stats[1]);
        status.setRxbytes(parseLongSafe(stats[2]));
        status.setRxpkts(parseLongSafe(stats[3]));
        status.setRxerr(parseLongSafe(stats[4]));
        status.setRxdrop(parseLongSafe(stats[5]));
        status.setTxbytes(parseLongSafe(stats[8]));
        status.setTxpkts(parseLongSafe(stats[9]));
        status.setTxerr(parseLongSafe(stats[10]));
        status.setTxdrop(parseLongSafe(stats[11]));
    }

    /** 
     * Adds vendor name based on MAC address to the InterfaceStatusGeneric object. 
     * @param status InterfaceStatusGeneric
     */
    private void populateMacVendor(InterfaceStatusGeneric status) {
        String vendor = null;
        if(status.getMacAddress() != null) {
            if (cachedMacAddrVendorList.containsKey(status.getMacAddress())) {
                vendor = cachedMacAddrVendorList.get(status.getMacAddress());
            } else {
                vendor = UvmContextFactory.context()
                            .deviceTable()
                            .getMacVendorFromMacAddress(status.getMacAddress());
                if (!StringUtil.isEmpty(vendor)) {
                    cachedMacAddrVendorList.put(status.getMacAddress(), vendor);
                }
            }
        }
        status.setMacVendor(vendor);
    }

    /** 
     * Populates IPv4 and IPv6 addresses for the interface 
     * in InterfaceStatusGeneric using Status - INTERFACE_IP_ADDRESSES. 
     * @param status InterfaceStatusGeneric
     * @param intf InterfaceSettings
     */
    private void populateIpAddresses(InterfaceStatusGeneric status, InterfaceSettings intf) {
        String ipStatus = getStatus(StatusCommands.INTERFACE_IP_ADDRESSES, intf.getSymbolicDev());
        String[] tokens = ipStatus.trim().split("\\s+");

        String nextType = "";
        for (String token : tokens) {
            if (INET.equals(nextType)) {
                if(status.getIp4Addr() == null)
                    status.setIp4Addr(new LinkedList<>());
                status.getIp4Addr().add(token);
            } else if (INET6.equals(nextType)) {
                if(status.getIp6Addr() == null)
                    status.setIp6Addr(new LinkedList<>());
                status.getIp6Addr().add(token);
            }
            nextType = "";
            if (INET.equals(token)) nextType = INET;
            else if (INET6.equals(token)) nextType = INET6;
        }
    }

    /** 
     * Populates connection state (connected/offline), duplex, and speed. 
     * @param status InterfaceStatusGeneric
     * @param intf InterfaceSettings
     * @param deviceStatusList List<DeviceStatus>
     */
    private void populateConnectionStatus(InterfaceStatusGeneric status, InterfaceSettings intf, List<DeviceStatus> deviceStatusList) {
        for (DeviceStatus ds : deviceStatusList) {
            if (ds.getDeviceName().equals(intf.getPhysicalDev())) {
                boolean isConnected = ConnectedStatus.CONNECTED.equals(ds.getConnected());
                DuplexStatus duplex = ds.getDuplex();

                status.setConnected(isConnected);
                status.setOffline(!isConnected);
                status.setEthSpeed(ds.getMbit());

                if (duplex == DuplexStatus.FULL_DUPLEX) status.setEthDuplex("full");
                else if(duplex == DuplexStatus.HALF_DUPLEX) status.setEthDuplex("half");
                else status.setEthDuplex(DuplexStatus.UNKNOWN.toString().toLowerCase());
                
                return;
            }
        }
    }

    /** 
     * Sets DNS and gateway information from interface status. 
     * @param status InterfaceStatusGeneric
     * @param intf InterfaceSettings
     */
    private void populateGatewayAndDns(InterfaceStatusGeneric status, InterfaceSettings intf) {
        InterfaceStatus intfStatus = getInterfaceStatus(intf.getInterfaceId());
        List<InetAddress> dns = Stream.of(intfStatus.getV4Dns1(), intfStatus.getV4Dns2())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
        if (!dns.isEmpty()) {
            if (status.getDnsServers() == null)
                status.setDnsServers(new LinkedList<>());
            status.getDnsServers().addAll(dns);
        }
        status.setV4Address(intfStatus.getV4Address());
        status.setIp4Gateway(intfStatus.getV4Gateway());
        status.setIp6Gateway(intfStatus.getV6Gateway());
    }

    /** Populates IPv4/IPv6 address source (dhcp/static/pppoe
    * @param status InterfaceStatusGeneric
    * @param intf InterfaceSettings
    */
    private void populateAddressSources(InterfaceStatusGeneric status, InterfaceSettings intf) {
        if (intf.getConfigType() != ConfigType.ADDRESSED) return;

        // Ensure lists are initialized
        if(status.getAddressSource() == null)
            status.setAddressSource(new LinkedList<>());
        if(status.getIp6addressSource() == null && intf.getV6ConfigType() != V6ConfigType.DISABLED)
            status.setIp6addressSource(new LinkedList<>());
        
        // Handle IPv4 address source
        V4ConfigType v4Type = intf.getV4ConfigType();
        switch (v4Type) {
            case AUTO: 
                status.getAddressSource().add(DHCP); break;
            case PPPOE: 
            case STATIC: {
                status.getAddressSource().add(v4Type.name().toLowerCase()); break;
            }
        }

        // Handle IPv6 address source
        V6ConfigType v6Type = intf.getV6ConfigType();
        switch (v6Type) {
            case AUTO: 
                status.getIp6addressSource().add(DHCPV6); break;
            case STATIC: 
                status.getIp6addressSource().add(v6Type.name().toLowerCase()); break;
        }
    }

    /** 
     * Safely parses integer, returns 0 if invalid. 
     * @param str String
     * @return int 
     */
    private long parseLongSafe(String str) {
        return str.matches("-?\\d+") ? Long.parseLong(str) : 0;
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
            logger.warn("Unable to find interface settings for interface {}", interfaceId );
            return false;
        }
        // check if vrrp is enabled on the interface to avoid console errors
        // if not enabled, return false
        if (!Boolean.TRUE.equals(intfSettings.getVrrpEnabled())) {
            logger.warn("VRRP not enabled on interface {}", interfaceId );
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
            logger.warn("VRRP alias not found on interface {}", interfaceId );
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
     * Return the status of non-WAN type interfaces
     * @return a list of InterfaceStatus for non-WAN type interfaces
     */
    public List<InterfaceStatus> getLocalInterfaceStatuses()
    {
        List<InterfaceStatus> allStatuses = getInterfaceStatus();
        List<InterfaceStatus> returnStatuses = new LinkedList<InterfaceStatus>();

        for(InterfaceStatus intfStatus : allStatuses) {
            InterfaceSettings intfSettings = findInterfaceId(intfStatus.getInterfaceId());
            if(intfSettings != null && intfSettings.getConfigType().equals(InterfaceSettings.ConfigType.ADDRESSED) && !intfSettings.getIsWan()) {
                returnStatuses.add(intfStatus);
            }
        }

        return returnStatuses;
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
            //expected Java class type
            Class<List<DeviceStatus>> ListOfDeviceStatusClass = (Class<List<DeviceStatus>>) (Class<?>) List.class;
            entryList = ObjectMatcher.parseJson(output, ListOfDeviceStatusClass); 
            } catch (JSONException | UnmarshallException e) {
                logger.warn("Unable to parse device status: ", e);
                logger.warn("Unable to parse device status: {}", output);
                return null;
            } catch (Exception e) {
                logger.error("Unexpected exception while getting device status: ", e);
                return null; 
            }
        return entryList;
    }
    
    /**
     * Get locally routed networks.
     * @return List of IPMaskedAddrress
     */
    public List<IPMaskedAddress> getLocalNetworks()
    {
        boolean match;
        IPMaskedAddress maskedAddress;
        List<IPMaskedAddress> addresses = new LinkedList<>();

        /*
         * Pull static addresses from non-WAN interfaces.
         */
        for( InterfaceSettings interfaceSettings : networkSettings.getInterfaces() ){
            if ( interfaceSettings.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED ){
                continue;
            }
            if ( interfaceSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC ){
                continue;
            }
            if(interfaceSettings.getIsWan()){
                continue;
            }

            maskedAddress = new IPMaskedAddress( interfaceSettings.getV4StaticAddress(), interfaceSettings.getV4StaticPrefix());
            addresses.add(new IPMaskedAddress( maskedAddress.getMaskedAddress(), interfaceSettings.getV4StaticPrefix()));
            for ( InterfaceSettings.InterfaceAlias alias : interfaceSettings.getV4Aliases() ) {
                /*
                 * Don't add if already in list
                 */
                match = false;
                maskedAddress = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                for( IPMaskedAddress ma : addresses ){
                    if( ma.getMaskedAddress().getHostAddress().equals( maskedAddress.getMaskedAddress().getHostAddress() ) &&
                        ( ma.getPrefixLength() == maskedAddress.getPrefixLength() ) ){
                        match = true;
                    }
                }
                if( match == false ){
                    addresses.add( new IPMaskedAddress( maskedAddress.getMaskedAddress(), alias.getStaticPrefix()) );
                }
            }
        }

        /**
         * Pull static routes that route locally
         */
        for (StaticRoute route : UvmContextFactory.context().networkManager().getNetworkSettings().getStaticRoutes()) {
            match = false;
            maskedAddress = new IPMaskedAddress( route.getNetwork(), route.getPrefix());
            for( IPMaskedAddress ma : addresses ){
                if( ma.getMaskedAddress().getHostAddress().equals( maskedAddress.getMaskedAddress().getHostAddress() ) &&
                    ( ma.getPrefixLength() == route.getPrefix() ) ){
                    match = true;
                }
            }
            if( match == false ){
                addresses.add( new IPMaskedAddress( maskedAddress.getMaskedAddress(), route.getPrefix()) );
            }
        }

        return addresses;

    }

    /**
     * Look through all interfaces for matching network and return address/alias.
     *
     * @param network    String of network.
     * @param prefixLength Integer of network prefix.
     * @return InetAddress of matching interface IP address or null if not found.
     */
    public InetAddress getInterfaceAddressForNetwork(String network, int prefixLength)
    {
        InetAddress matchingAddress = null;
        IPMaskedAddress currentMaskedAddress, lookupMaskedAddress;
        /*
         * Pull static addresses from non-WAN interfaces.
         */
        for( InterfaceSettings interfaceSettings : networkSettings.getInterfaces() ){
            if ( interfaceSettings.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED ){
                continue;
            }
            if ( interfaceSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC ){
                continue;
            }

            // The interface masked network.
            currentMaskedAddress = new IPMaskedAddress( interfaceSettings.getV4StaticAddress(), interfaceSettings.getV4StaticPrefix());
            // The lookup network, using the larger network (smaller prefix) of itself or this interface.
            lookupMaskedAddress = new IPMaskedAddress( network, interfaceSettings.getV4StaticPrefix() < prefixLength ? interfaceSettings.getV4StaticPrefix() : prefixLength);
            if(lookupMaskedAddress.getMaskedAddress().equals(currentMaskedAddress.getMaskedAddress())){
                matchingAddress = interfaceSettings.getV4StaticAddress();
                break;
            }
            for ( InterfaceSettings.InterfaceAlias alias : interfaceSettings.getV4Aliases() ) {
                /**
                 * Look at aliases.
                 */
                // The interface masked alias network.
                currentMaskedAddress = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                // The lookup network, using the larger network (smaller prefix) of itself or this alias.
                lookupMaskedAddress = new IPMaskedAddress( network, alias.getStaticPrefix() < prefixLength? alias.getStaticPrefix() : prefixLength);
                if(lookupMaskedAddress.getMaskedAddress().equals(currentMaskedAddress.getMaskedAddress())){
                    matchingAddress = interfaceSettings.getV4StaticAddress();
                    break;
                }
            }
            if(matchingAddress != null){
                break;
            }
        }
        return matchingAddress;
    }


    /**
     * Modify reserved access rules by changing old port to new port.
     * This is used by some services where it is common to change the listening port, like WireGuard.
     *
     * @param oldPort Existing port to match.
     * @param newPort New port number.
     */
    public void updateReservedAccessRulePort(String oldPort, String newPort)
    {
        boolean changed = false;
        for ( FilterRule rule : this.networkSettings.getAccessRules() ) {
            if(rule.getReadOnly()){
                List<FilterRuleCondition> conditions = rule.getConditions();
                if ( conditions != null )
                    for ( RuleCondition condition : conditions ) {
                        if(condition.getConditionType() == RuleCondition.ConditionType.DST_PORT &&
                           condition.getValue().equals(oldPort) ){
                            condition.setValue(newPort);
                            changed = true;
                        }
                    }
            }
        }
        if(changed){
            setNetworkSettings( this.networkSettings );
        }
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
                logger.info("insert rules: {}", line);
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
                logger.info("remove rules: {}", line);
        } catch (Exception e) {}
    }

    /**
     * Return the current network settings file for use in sync-settings calls.
     * @return String of network settings filename. 
     */
    public String getNetworkSettingsFilename()
    {
        return settingsFilename;
    }

    /**
     * sets values in the interfaceSettingsById map for quick lookups
     */
    private void configureInterfaceSettingsArray()
    {
        /**
         * Set interfaceSettingsById array for fast lookups
         */
        for ( int i = 0 ; i < interfaceSettingsById.length ; i++ ) {
            interfaceSettingsById[i] = null;
        }
        // Users cannot add interfaces greater than 253 through the UI, but 
        // could still add them manually into network.js. This skips any 
        // leftover interfaces, preventing a nasty error message
        if ( this.networkSettings.getInterfaces() != null ) {
            for ( InterfaceSettings intf : this.networkSettings.getInterfaces() ) {
                try {
                    interfaceSettingsById[intf.getInterfaceId()] = intf;
            }
                catch (ArrayIndexOutOfBoundsException e) {
                    logger.warn("Skipping out-of-bounds physical interface: {}", intf.getInterfaceId());
                    continue;
                }
            }
        }
        if ( this.networkSettings.getVirtualInterfaces() != null ) {
            for ( InterfaceSettings intf : this.networkSettings.getVirtualInterfaces() ) {
                try {
                    interfaceSettingsById[intf.getInterfaceId()] = intf;
                }
                catch (ArrayIndexOutOfBoundsException e) {
                    logger.warn("Skipping out-of-bounds virtual interface: {}", intf.getInterfaceId());
                    continue;
                }
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
                logger.warn("Found unmapped new physical device: {}", deviceName);
                logger.warn("Creating new InterfaceSettings for {}", deviceName);
                int interfaceId = this.getNextFreeInterfaceId( netSettings );
                if (interfaceId == -1) {
                    // note: we never need to explicitly set this flag to false. The user needs to 
                    // restart to solve the problem (which is told to them in NotificationManagerImpl). 
                    // On restart, the flag will always be false initially.
                    this.setInterfacesOverloadedFlag(true);
                    logger.warn("No space for added physical interface '{}'", deviceName );
                    continue;
                }
                InterfaceSettings interfaceSettings = new InterfaceSettings();
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
                logger.warn("Found unmapped new physical device: {}", deviceName);
                logger.warn("Creating new DeviceSettings for {}.", deviceName );

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
     * Remove any devices without a corresponding interface
     * @param netSettings
     */
    private void checkForRemovedDevices( NetworkSettings netSettings )
    {
        List<DeviceSettings> deviceSettings = netSettings.getDevices();
        Predicate<DeviceSettings> predicateTest = d -> {
            boolean foundMatchingInterface = false;

            for ( InterfaceSettings interfaceSettings : netSettings.getInterfaces() ) {
                if ( d.getDeviceName().equals( interfaceSettings.getPhysicalDev() ) )
                    foundMatchingInterface = true;
            }

            if ( ! foundMatchingInterface ) {
                return true;
            } else {
                return false;
            }
        };

        deviceSettings.removeIf(predicateTest);

        netSettings.setDevices(deviceSettings);
    }

    /**
     * Check to make sure the RADIUS rules exist. To be absolutely sure we
     * only add them when missing, I created this function which does not
     * use the settings version. It looks for the rules by name+read_only
     * flag to make sure they are the protected ones we create. If they
     * do not exist, we create them and save the updated settings.
     */
    private void checkForRadiusRules()
    {
        int addLocation = 0;
        int lanLocation = 0;
        int wanLocation = 0;

        List<FilterRule> accessRules = this.networkSettings.getAccessRules();

        for( FilterRule rule : accessRules ) {
            int pos = 1;
            // look for the rule where we want to insert the RADIUS rules
            if (rule.getDescription() != null && ("Allow HTTPS on non-WANs".equals(rule.getDescription()))) {
                addLocation = pos;
            }
            // see if the LAN rule already exists
            if (rule.getDescription() != null && ("Allow RADIUS on non-WANs".equals(rule.getDescription()))) {
                lanLocation = pos;
            }
            // see if the WAN rule already exists
            if (rule.getDescription() != null && ("Allow RADIUS on WANs".equals(rule.getDescription()))) {
                wanLocation = pos;
            }
            pos++;
        }

        // if we didn't find the expected add location just return
        if (addLocation == 0) return;

        // if both of the rules we need to create already exist just bail
        if ((lanLocation != 0) && (wanLocation != 0)) return;

        // if the LAN rule doesn't exist add it
        if (lanLocation == 0) {
            // create and insert the LAN rule
            FilterRule filterRuleRadiusNonWan = new FilterRule();
            filterRuleRadiusNonWan.setReadOnly( true );
            filterRuleRadiusNonWan.setEnabled( true );
            filterRuleRadiusNonWan.setIpv6Enabled( true );
            filterRuleRadiusNonWan.setDescription( "Allow RADIUS on non-WANs" );
            filterRuleRadiusNonWan.setBlocked( false );
            List<FilterRuleCondition> ruleRadiusNonWanConditions = new LinkedList<>();
            FilterRuleCondition ruleRadiusNonWanMatcher1 = new FilterRuleCondition();
            ruleRadiusNonWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
            ruleRadiusNonWanMatcher1.setValue("1812, 1813");
            FilterRuleCondition ruleRadiusNonWanMatcher2 = new FilterRuleCondition();
            ruleRadiusNonWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
            ruleRadiusNonWanMatcher2.setValue("UDP");
            FilterRuleCondition ruleRadiusNonWanMatcher3 = new FilterRuleCondition();
            ruleRadiusNonWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
            ruleRadiusNonWanMatcher3.setValue("non_wan");
            ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher1);
            ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher2);
            ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher3);
            filterRuleRadiusNonWan.setConditions( ruleRadiusNonWanConditions );
            accessRules.add( addLocation, filterRuleRadiusNonWan );
        }

        // if the WAN rule doesn't exist add it
        if (wanLocation == 0) {
            // create and insert the WAN rule
            FilterRule filterRuleRadiusWan = new FilterRule();
            filterRuleRadiusWan.setReadOnly( true );
            filterRuleRadiusWan.setEnabled( false );
            filterRuleRadiusWan.setIpv6Enabled( false );
            filterRuleRadiusWan.setDescription( "Allow RADIUS on WANs" );
            filterRuleRadiusWan.setBlocked( false );
            List<FilterRuleCondition> ruleRadiusWanConditions = new LinkedList<>();
            FilterRuleCondition ruleRadiusWanMatcher1 = new FilterRuleCondition();
            ruleRadiusWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
            ruleRadiusWanMatcher1.setValue("1812, 1813");
            FilterRuleCondition ruleRadiusWanMatcher2 = new FilterRuleCondition();
            ruleRadiusWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
            ruleRadiusWanMatcher2.setValue("UDP");
            FilterRuleCondition ruleRadiusWanMatcher3 = new FilterRuleCondition();
            ruleRadiusWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
            ruleRadiusWanMatcher3.setValue("wan");
            ruleRadiusWanConditions.add(ruleRadiusWanMatcher1);
            ruleRadiusWanConditions.add(ruleRadiusWanMatcher2);
            ruleRadiusWanConditions.add(ruleRadiusWanMatcher3);
            filterRuleRadiusWan.setConditions( ruleRadiusWanConditions );
            accessRules.add( addLocation, filterRuleRadiusWan );
        }

        // save the updated settings
        this.setNetworkSettings( this.networkSettings, false );
    }
    
    /**
     * Create the default NetworkSettings
     * @return NetworkSettings
     */
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();
        
        try {
            newSettings.setVersion( currentVersion );

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
                internal.setDhcpType(InterfaceSettings.DhcpType.SERVER);
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

            virtualIntf = new InterfaceSettings(InterfaceSettings.WIREGUARD_INTERFACE_ID,"WireGuard VPN");
            virtualIntf.setIsVirtualInterface(true);
            virtualIntf.setConfigType(null);
            virtualIntf.setV4ConfigType(null);
            virtualIntf.setV4Aliases(null);
            virtualIntf.setV6ConfigType(null);
            virtualIntf.setV6Aliases(null);
            virtualIntf.setVrrpAliases(null);
            virtualInterfaces.add(virtualIntf);

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
            newSettings.setDhcpRelays( new LinkedList<DhcpRelay>() );

            /**
             * If this is a netboot (untangle local installation)
             * Copy the authorized keys to root's ssh
             */
            if ( UvmContextFactory.context().isNetBoot() ) {
                UvmContextFactory.context().execManager().exec(System.getProperty("uvm.bin.dir") + "/ut-networking-helpers.sh copyAuthorizedKeys");
            }
        }
        catch (Exception e) {
            logger.error("Error creating Network Settings", e);
        }

        return newSettings;
    }

    /**
     * Apply oem overrides to settings
     */
    private void applyOemSettings() 
    {
        logger.info("Apply oem settings");
        
        // pass the settings to the OEM override function and return the override settings
        NetworkSettings overrideSettings = (NetworkSettings)UvmContextFactory.context().oemManager().applyOemOverrides(this.networkSettings);
    
        this.setNetworkSettings( overrideSettings );
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
            if(intf1.getConfigType() == InterfaceSettings.ConfigType.DISABLED && intf1.getName().equals(networkSettings.getDynamicDnsServiceWan())){
                throw new RuntimeException("This WAN is used by the DDNS service. Please change the WAN from the DDNS configuration before disabling this WAN.");
            }
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
         * Prevent users from choosing edge.arista.com (#7574)
         */
        if ( "edge.arista.com".equals(networkSettings.getDomainName()) ) {
            throw new RuntimeException( "edge.arista.com is not an allowed domain name." );
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
            if ( intf.getDhcpType() == InterfaceSettings.DhcpType.DISABLED )
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
        
        List<InterfaceSettings> interfacesSettings =  networkSettings.getInterfaces();
        List<InterfaceSettings> interfacesToMove = interfacesSettings.stream()
                                                                        .filter(i-> i.getInterfaceId() == -1).collect(Collectors.toList());
        
        // For each interface with id == -1, remove it and add it to the end
        if(!interfacesToMove.isEmpty()){
            for (InterfaceSettings interfaceToMove : interfacesToMove) {
                interfacesSettings.remove(interfaceToMove);  // Remove the interface from its current position
                interfacesSettings.add(interfaceToMove);  // Add it to the last position
            }
        }  
        
        /**
         * Handle VLAN interfaces
         */
        for ( InterfaceSettings intf : interfacesSettings ) {
            if (!intf.getIsVlanInterface())
                continue;
            if ( intf.getVlanTag() == null )
                throw new RuntimeException("VLAN tag missing on VLAN interface");
            if ( intf.getVlanParent() == null )
                throw new RuntimeException("VLAN parent missing on VLAN interface");
                
            if (intf.getInterfaceId() < 0) {
                intf.setInterfaceId(this.getNextFreeInterfaceId(networkSettings));
            }

            InterfaceSettings parent = null;
            for ( InterfaceSettings intf2 : interfacesSettings ) {
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
        for ( InterfaceSettings intf : interfacesSettings ) {
            if (!InterfaceSettings.ConfigType.DISABLED.equals( intf.getConfigType()) && InterfaceSettings.V4ConfigType.PPPOE.equals( intf.getV4ConfigType())){
                // save the old system dev (usuallyy physdev or sometimse vlan dev as root dev)
                intf.setV4PPPoERootDev(intf.getSystemDev());
                intf.setSystemDev("ppp" + pppCount);
                intf.setSymbolicDev("ppp" + pppCount);
                String plainPassword = intf.getV4PPPoEPassword();
                String encryptedPassword = intf.getV4PPPoEPasswordEncrypted();
                // Handle empty password by setting encrypted password to null, ensuring the secret file contains "None" as per current behavior.
                if(plainPassword == null && encryptedPassword != null && PasswordUtil.getDecryptPassword(encryptedPassword).isEmpty()){
                    intf.setV4PPPoEPasswordEncrypted(null);
                }
                // Set encrypted password for non-empty password value
                if(plainPassword != null){ 
                    intf.setV4PPPoEPasswordEncrypted(PasswordUtil.getEncryptPassword(plainPassword));
                    intf.setV4PPPoEPassword(null);
                }
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
         * Sanitize the device settings
         * If an interface was deleted, delete the corresponding device
         * from the device list
         */
        checkForRemovedDevices( networkSettings );

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
         * Check if all interfaceIds are unique
         */
        for ( InterfaceSettings intf1 : networkSettings.getInterfaces() ) {
            for ( InterfaceSettings intf2 : networkSettings.getInterfaces() ) {
                if ( intf1.getInterfaceId() == intf2.getInterfaceId() &&
                     intf1 != intf2 )
                    throw new RuntimeException( intf1.getName() + " & " + intf2.getName() + " interfaceId conflict." );
            }
        }

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
             interfaceSettings.getDhcpType() == InterfaceSettings.DhcpType.SERVER &&
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
        if (interfaceSettings.getDhcpType() == InterfaceSettings.DhcpType.DISABLED){
            return;
        }

        try {
            InetAddress addr = interfaceSettings.getV4StaticAddress();
            InetAddress mask = interfaceSettings.getV4StaticNetmask();
            if (addr == null || mask == null) {
                logger.warn("Missing interface[{}] settings ({}, {}). Disabling DHCP.", interfaceSettings.getName() , addr , mask );
                interfaceSettings.setDhcpType(InterfaceSettings.DhcpType.DISABLED);
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
            logger.warn("Exception initializing DHCP Address: ", e);
            interfaceSettings.setDhcpType(InterfaceSettings.DhcpType.DISABLED);
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

        FilterRule filterRuleRadiusWan = new FilterRule();
        filterRuleRadiusWan.setReadOnly( true );
        filterRuleRadiusWan.setEnabled( false );
        filterRuleRadiusWan.setIpv6Enabled( false );
        filterRuleRadiusWan.setDescription( "Allow RADIUS on WANs" );
        filterRuleRadiusWan.setBlocked( false );
        List<FilterRuleCondition> ruleRadiusWanConditions = new LinkedList<>();
        FilterRuleCondition ruleRadiusWanMatcher1 = new FilterRuleCondition();
        ruleRadiusWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleRadiusWanMatcher1.setValue("1812, 1813");
        FilterRuleCondition ruleRadiusWanMatcher2 = new FilterRuleCondition();
        ruleRadiusWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleRadiusWanMatcher2.setValue("UDP");
        FilterRuleCondition ruleRadiusWanMatcher3 = new FilterRuleCondition();
        ruleRadiusWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleRadiusWanMatcher3.setValue("wan");
        ruleRadiusWanConditions.add(ruleRadiusWanMatcher1);
        ruleRadiusWanConditions.add(ruleRadiusWanMatcher2);
        ruleRadiusWanConditions.add(ruleRadiusWanMatcher3);
        filterRuleRadiusWan.setConditions( ruleRadiusWanConditions );

        FilterRule filterRuleRadiusNonWan = new FilterRule();
        filterRuleRadiusNonWan.setReadOnly( true );
        filterRuleRadiusNonWan.setEnabled( true );
        filterRuleRadiusNonWan.setIpv6Enabled( true );
        filterRuleRadiusNonWan.setDescription( "Allow RADIUS on non-WANs" );
        filterRuleRadiusNonWan.setBlocked( false );
        List<FilterRuleCondition> ruleRadiusNonWanConditions = new LinkedList<>();
        FilterRuleCondition ruleRadiusNonWanMatcher1 = new FilterRuleCondition();
        ruleRadiusNonWanMatcher1.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleRadiusNonWanMatcher1.setValue("1812, 1813");
        FilterRuleCondition ruleRadiusNonWanMatcher2 = new FilterRuleCondition();
        ruleRadiusNonWanMatcher2.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleRadiusNonWanMatcher2.setValue("UDP");
        FilterRuleCondition ruleRadiusNonWanMatcher3 = new FilterRuleCondition();
        ruleRadiusNonWanMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleRadiusNonWanMatcher3.setValue("non_wan");
        ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher1);
        ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher2);
        ruleRadiusNonWanConditions.add(ruleRadiusNonWanMatcher3);
        filterRuleRadiusNonWan.setConditions( ruleRadiusNonWanConditions );

        FilterRule filterRulePing = new FilterRule();
        filterRulePing.setReadOnly( true );
        filterRulePing.setEnabled( true );
        filterRulePing.setIpv6Enabled( true );
        filterRulePing.setDescription( "Allow PING" );
        filterRulePing.setBlocked( false );
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

        FilterRule filterRuleWireGuard = new FilterRule();
        filterRuleWireGuard.setReadOnly( true );
        filterRuleWireGuard.setEnabled( true );
        filterRuleWireGuard.setIpv6Enabled( true );
        filterRuleWireGuard.setDescription( "Allow WireGuard" );
        filterRuleWireGuard.setBlocked( false );
        List<FilterRuleCondition> ruleWireGuardConditions = new LinkedList<>();
        FilterRuleCondition ruleWireGuardMatcher1 = new FilterRuleCondition();
        ruleWireGuardMatcher1.setConditionType(FilterRuleCondition.ConditionType.PROTOCOL);
        ruleWireGuardMatcher1.setValue("UDP");
        FilterRuleCondition ruleWireGuardMatcher2 = new FilterRuleCondition();
        ruleWireGuardMatcher2.setConditionType(FilterRuleCondition.ConditionType.DST_PORT);
        ruleWireGuardMatcher2.setValue("51820");
        FilterRuleCondition ruleWireGuardMatcher3 = new FilterRuleCondition();
        ruleWireGuardMatcher3.setConditionType(FilterRuleCondition.ConditionType.SRC_INTF);
        ruleWireGuardMatcher3.setValue("wan");
        ruleWireGuardConditions.add(ruleWireGuardMatcher1);
        ruleWireGuardConditions.add(ruleWireGuardMatcher2);
        ruleWireGuardConditions.add(ruleWireGuardMatcher3);
        filterRuleWireGuard.setConditions( ruleWireGuardConditions );

        FilterRule filterRuleBlock = new FilterRule();
        filterRuleBlock.setReadOnly( true );
        filterRuleBlock.setEnabled( true );
        filterRuleBlock.setIpv6Enabled( true );
        filterRuleBlock.setDescription( "Block All" );
        filterRuleBlock.setBlocked( true );
        List<FilterRuleCondition> rule4Conditions = new LinkedList<>();
        filterRuleBlock.setConditions( rule4Conditions );

        rules.add( filterRuleSsh );
        rules.add( filterRuleHttpsWan );
        rules.add( filterRuleHttpsNonWan );
        rules.add( filterRuleRadiusWan );
        rules.add( filterRuleRadiusNonWan );
        rules.add( filterRulePing );
        rules.add( filterRuleDns );
        rules.add( filterRuleDhcp );
        rules.add( filterRuleHttp );
        rules.add( filterRuleSnmp );
        rules.add(filterRuleBgp);
        rules.add(filterRuleOspf);
        rules.add( filterRuleAhEsp );
        rules.add( filterRuleIke );
        rules.add( filterRuleNatT );
        rules.add( filterRuleL2tp );
        rules.add( filterRuleOpenVpn );
        rules.add( filterRuleWireGuard );
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
        result = UvmContextFactory.context().execManager().exec( System.getProperty("uvm.bin.dir") + "/ut-get-interfaces.sh eth");
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
        result = UvmContextFactory.context().execManager().exec( System.getProperty("uvm.bin.dir") + "/ut-get-interfaces.sh wlan");
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
     *  Helper method to execute shell commands with a wireless device for fetching information
     * @param scriptPath executable path
     * @param arguments list of strings
     * @param interfaceName
     * @return String
     */
    private String execInterfaceCommand( String scriptPath, List<String> arguments, String interfaceName) {

        // Validate interface syntax
        if (interfaceName == null ||
            !interfaceName.matches(INTERFACE_NAME_PATTERN)) {
            throw new RuntimeException("Invalid interface name");
        }

        // Validate interface exists (sysfs)
        Path iface = Paths.get("/sys/class/net", interfaceName);
        if (!Files.exists(iface)) {
            throw new RuntimeException("Unknown interface: " + interfaceName);
        }

        ExecManager execManager = UvmContextFactory.context().execManager();

        ExecManagerResult result =
            execManager.execCommand(scriptPath, arguments);

        return result.getOutput();
    }



    /**
     * Query a wireless device for valid regulatory country codes
     * @param systemDev
     * @return a JSON array containing a list of strings of two letter country codes like US, JP, etc.
     */
    public JSONArray getWirelessValidRegulatoryCountryCodes( String systemDev )
    {
        String result = execInterfaceCommand(
            wirelessInterfaceScript,
            List.of(
                "--interface=" + systemDev,
                "--query=get_valid_country_codes"
            ),
            systemDev
        );
    
        try {
            JSONObject jo = new JSONObject(result);
            return jo.getJSONArray("get_valid_country_codes");
        } catch (JSONException e) {
            logger.warn(
                "Unable to parse wireless regulatory country codes: " + result,
                e
            );
            return null;
        }
    }

    /**
     * Query a wireless device to determine if it complies with wireless regions.
     * @param systemDev
     * @return a boolean of whether wireless driver complies with regional settings
     */
    public boolean isWirelessRegulatoryCompliant( String systemDev )
    {
        String result = execInterfaceCommand(
            wirelessInterfaceScript,
            List.of(
                "--interface=" + systemDev,
                "--query=is_regulatory_compliant"
            ),
            systemDev
        );

        try{
            JSONObject jo = new JSONObject(result);
            return jo.getBoolean("is_regulatory_compliant");
        }catch( JSONException e ){
            logger.warn("Unable to parse wireless driver compliance: {}", result);
        }
        return false;
    }

    /**
     * Query a wireless device to determine if it complies with wireless regions.
     * @param systemDev
     * @return string of wireless driver regulaatory country code
     */
    public String getWirelessRegulatoryCountryCode( String systemDev )
    {
        String result = execInterfaceCommand(
            wirelessInterfaceScript,
            List.of(
                "--interface=" + systemDev,
                "--query=get_regulatory_country_code"
            ),
            systemDev
        );

        try{
            JSONObject jo = new JSONObject(result);
             return  jo.getString("get_regulatory_country_code");
        }catch( JSONException e ){
            logger.warn("Unable to parse wireless driver country code: {}", result);
        }
        return "";
    }

    /**
     * Query a wireless device for a list of supported channels
     * @param systemDev
     * @param region
     * @return a JSON array containing a list of the support wifi channels where each entry is a JSONobject containing the keys:
     *      channel     Numeric channel identifier.
     *      frequency   String frequency in GHz for display
     */
    public JSONArray getWirelessChannels( String systemDev, String region )
    {
        // Validate region (ISO 3166-1 alpha-2, e.g., US, IN, DE)
        if (region == null || !region.matches(REGION_NAME_PATTERN)) {
            throw new RuntimeException("Invalid wireless regulatory region");
        }
        String result = execInterfaceCommand(
            wirelessInterfaceScript,
            List.of(
                "--interface=" + systemDev,
                "--query=get_channels," + region
            ),
            systemDev
        );
        try{
            JSONObject jo = new JSONObject(result);
            return jo.getJSONArray("get_channels");
        }catch(JSONException e ){
            logger.warn("Unable to parse wireless channels: {}", result);
        }
        return null;
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
        throw new RuntimeException("UpnpManager not supported Exception");

    }

    /**
     * Lookup the hardware vendors for a MAC addresses
     *
     * @param macAddressList
     *        The MAC address List
     * @return The MAC addresses with hardware vendors, or null
     */
    public ConcurrentMap<String, String> lookupMacVendorList(List<String> macAddressList) {
        // find MAC addresses that are missing in cache
        List<String> missingMacAddressList = macAddressList.stream()
                .filter(macAddress -> !cachedMacAddrVendorList.containsKey(macAddress))
                .toList();

        if (!missingMacAddressList.isEmpty()) {
            try {
                String macAddresses = String.join(COMMA, missingMacAddressList);
                logger.info("Cloud MAC lookup: {}", macAddresses);
                // fetch the vendors for the mac addresses
                JSONArray macAddrVendorArr = UvmContextFactory.context().deviceTable().lookupMacVendor(macAddresses);
                if (macAddrVendorArr != null) {
                    for (int i = 0; i < macAddrVendorArr.length(); i++) {
                        JSONObject macAddrVendor = macAddrVendorArr.getJSONObject(i);
                        if (macAddrVendor.has(MAC) && macAddrVendor.has(ORGANIZATION)) {
                            String macAddr = macAddrVendor.getString(MAC), vendorName = macAddrVendor.getString(ORGANIZATION);
                            if (logger.isDebugEnabled())
                                logger.debug("Cloud MAC lookup: {} ,Cloud Vendor lookup: {}", macAddr, vendorName);
                            // add the fetched mac address with vendor in our cache
                            cachedMacAddrVendorList.put(macAddr, vendorName);
                        }
                    }
                } else {
                    logger.info("Vendors not found for MAC addresses: {}", macAddresses);
                }
            } catch (Exception exn) {
                logger.warn("Exception looking up MAC address vendor:", exn);
            }
        }
        return cachedMacAddrVendorList;
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
            logger.warn("Invalid arguments: {} {} {}", ssid , password , encryption );
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

            JSONArray channels = getWirelessChannels( intf.getSystemDev(), intf.getWirelessCountryCode() );
            if ( channels == null ) {
                logger.warn("Unabled to determine supported channels for {}", intf.getSystemDev());
                continue;
            }
            int maxChannel = 0;
            int minChannel = 99999;
            int ch = 0;
            JSONObject channelFrequency = null;
            for (int j = 0; j < channels.length(); j++) {
                try{
                    channelFrequency = channels.getJSONObject(j);
                    ch = channelFrequency.getInt("channel");
                }catch(JSONException e){
                    logger.warn("Unable to determine supported channels for {}", intf.getSystemDev());
                    continue;
                }
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
            logger.warn("Unknown public URL method: {}", this.networkSettings.getPublicUrlMethod() );
        }
        
        return primaryAddressStr + ":" + httpsPortStr;
    }

    /**
     * Provide the first free interface ID
     *
     * @param netSettings - the network settings to use to check for a free ID
     * @return the next available ID, or -1 if not found.
     */
    public int getNextFreeInterfaceId(NetworkSettings netSettings)
    {
        if (netSettings == null)
            return 1;
        int freeId;
        for (freeId = 1 ; freeId < InterfaceSettings.MAX_INTERFACE_ID ; freeId++) {
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
        logger.warn("Failed to find a free interface Id ({})", freeId );
        return -1;
    }

    /**
     * Get network status from statusScript.
     *
     * Each command is expected to be a function in the script of the format:
     * get_<command in lowercase> such as get_interface_transfer
     *
     * @param command - the network settings to use to check for a free ID
     * @param argument - String of argument to pass to script
     * @return string of status
     */
    public String getStatus(StatusCommands command, String argument) {

        ExecManager execManager = UvmContextFactory.context().execManager();
    
        List<String> args = new ArrayList<>();
        args.add(command.getScriptCommand());
    
        if (command.requiresInterface()) {
            Path iface = Paths.get("/sys/class/net", argument);
    
            if (argument == null ||
                !argument.matches(INTERFACE_NAME_PATTERN) || (!argument.startsWith("ppp") && !Files.exists(iface))) {
                throw new RuntimeException("Invalid interface name");
            }
    
            args.add(argument);
    
        } else if (argument != null) {
            throw new RuntimeException(
                "Argument not supported for command: " + command
            );
        }
    
        return execManager.execCommand(statusScript, args).getOutput();
    }
    
    /**
     * Run network troubleshooting script
     *
     * @param command - the network settings to use to check for a free ID
     * @param arguments - JSONObject of arguments to pass as environment variables to command
     * @return string of status
     */
    public ExecManagerResultReader runTroubleshooting(TroubleshootingCommands command, JSONObject arguments)
    {
        List<String> environment_variables = new ArrayList<>();
        List<String> suspiciousEntries = new ArrayList<>();

        // Allowed args
        Map<TroubleshootingCommands, Set<String>> allowedArgs =
            Map.of(
                TroubleshootingCommands.CONNECTIVITY, Set.of("DNS_TEST_HOST", "TCP_TEST_HOST"),
                TroubleshootingCommands.REACHABLE,    Set.of("HOST"),
                TroubleshootingCommands.DNS,          Set.of("HOST"),
                TroubleshootingCommands.CONNECTION,   Set.of("HOST", "HOST_PORT"),
                TroubleshootingCommands.PATH,         Set.of("HOST", "PROTOCOL"),
                TroubleshootingCommands.DOWNLOAD,     Set.of("URL"),
                TroubleshootingCommands.TRACE,
                    Set.of("TIMEOUT","MODE","TRACE_ARGUMENTS","HOST","HOST_PORT","INTERFACE","FILENAME")
            );

        TroubleshootingValidator validator = new TroubleshootingValidator(this.networkSettings);

        try {
            if (arguments != null) {
                Iterator<?> keys = arguments.keys();
                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    String value = String.valueOf(arguments.get(key)).trim();

                    validator.validate(command, key, value, allowedArgs, suspiciousEntries);

                    environment_variables.add(key + "=" + value);
                }
            }

            if (!suspiciousEntries.isEmpty()) {
                throw new RuntimeException("Blocked due to suspicious entries: " + suspiciousEntries);
            }

            return UvmContextFactory.context().execManager().execEvil(
                    new String[]{troubleshootingScript, "run_" + command.toString().toLowerCase()},
                    environment_variables.toArray(new String[0])
            );

        } catch (Exception ex) {
            logger.warn("runTroubleshooting failed: ", ex);
            return null;
        }
    }

    /**
     * convertSettings
     * Convert settings to latest version
     */
    private void convertSettings()
    {
        // For 17.5 Vue Migration Changes
        boolean globalQosEnabled = this.networkSettings.getQosSettings() != null && this.networkSettings.getQosSettings().getQosEnabled();
        for(InterfaceSettings intfSettings: this.networkSettings.getInterfaces()) {
            // Set generic config type for Vue response config type.
            InterfaceSettingsGeneric.ConfigType configTypeGeneric =
                    (intfSettings.getConfigType() != ConfigType.DISABLED)
                            ? InterfaceSettingsGeneric.ConfigType.valueOf(intfSettings.getConfigType().name())
                            : InterfaceSettingsGeneric.ConfigType.ADDRESSED;
            intfSettings.setConfigTypeGeneric(configTypeGeneric);

            // If global QOS is enabled and interface have non zero band width set qosEnabled true for that interface
            if(intfSettings.getIsWan() && globalQosEnabled) {
                boolean qosEnabled = intfSettings.getDownloadBandwidthKbps() != null && intfSettings.getDownloadBandwidthKbps() != 0
                        && intfSettings.getUploadBandwidthKbps() != null && intfSettings.getUploadBandwidthKbps() != 0;
                intfSettings.setQosEnabled(qosEnabled);
            }
        }
        // Set new version
        this.networkSettings.setVersion( currentVersion );
        this.setNetworkSettings( this.networkSettings, false );
    }

    /**
     * Gets the contents of the hostapd log file
     * @param device - the device name to filter the logs
     *
     * @return The contents of the IPsec log file
     */
    public String getLogFile(String device)
    {
        logger.debug("hostapd.log getLogFile()");
        return UvmContextFactory.context().execManager().execOutput(String.format("%s %s", GET_LOGFILE_SCRIPT, device));
    }

    /**
     * NetworkTestDownloadHandler
     * This is the download servlet helper to allow the tcpdump test in troubleshooting
     * to download the pcap file
     */
    private class NetworkTestDownloadHandler implements DownloadHandler
    {
        private static final String CHARACTER_ENCODING = "utf-8";
        private static final String PATH = "/tmp/network-tests/";
        private static final String FORWARD_SLASH = "/";
        private static final Path BASE_PATH = Paths.get(PATH).toAbsolutePath().normalize();

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
                logger.warn("Invalid parameters: {}", name );
                return;
            }
            // Ensuring that filename can only be downloaded from under our path and is valid
            if (name.startsWith(FORWARD_SLASH)) name = name.substring(1);

            Path resolvedPath = BASE_PATH.resolve(name).normalize();
            if (!resolvedPath.startsWith(BASE_PATH) || !resolvedPath.toFile().exists()) {
                logger.warn("Invalid parameter: {}, won't download the file", name );
                return;
            }

            FileInputStream fis = null;
            OutputStream out = null;
            try{
                resp.setCharacterEncoding(CHARACTER_ENCODING);
                resp.setHeader("Content-Type","application/vnd.tcpdump.pcap");
                resp.setHeader("Content-Disposition","attachment; filename="+name);

                byte[] buffer = new byte[1024];
                int read;

                fis = new FileInputStream(PATH + name);
                out = resp.getOutputStream();
                
                while ( ( read = fis.read( buffer ) ) > 0 ) {
                    out.write( buffer, 0, read);
                }

                out.flush();
            } catch (Exception e) {
                logger.warn("Failed to export packet trace.", e);
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

    /**
     * Validates troubleshooting command arguments.
     * This class contains all validation logic for network troubleshooting parameters.
     */
    private static class TroubleshootingValidator {

        private final NetworkSettings networkSettings;

        private final Pattern hostnamePat = Pattern.compile("^(?=.{1,253}$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,}$");
        private final Pattern ipv4Pat = Pattern.compile("^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)(\\.|$)){4}$");
        private final Pattern ipv6Pat = Pattern.compile("^(?:[\\da-fA-F]{1,4}:){1,7}[\\da-fA-F]{1,4}$");
        private final Pattern urlPat = Pattern.compile("^(https?://)([\\w.-]+)(:\\d+)?(/\\S*)?$");
        private final Pattern pcapFilePat = Pattern.compile("^[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*\\.(pcap|pcapng)$");
        private final Pattern traceSafeChars = Pattern.compile("^[A-Za-z0-9 ._:/\\-]+$");
        private final Pattern safeChars = Pattern.compile("^[A-Za-z0-9:/._\\-]+$");

        private final Set<String> simpleFlags = Set.of(
                "-A","-b","-d","-D","-e","-f","-h","-H","-I","-J","-K","-l","-L",
                "-n","-N","-O","-p","-q","-S","-t","-u","-U","-v","-x","-X"
        );
        private final Set<String> numericFlags = Set.of("-B","-c","-C","-G","-s","-W");
        private final Set<String> stringFlags = Set.of("-i","-j","-Q","-T","-y");
        private final Set<String> forbiddenFlags = Set.of("-z","-Z","-E","-M","-F","-V","-r");
        private final Set<String> outputFlags = Set.of("-w");

        /**
         * Creates a new validator for troubleshooting arguments.
         *
         * @param networkSettings network interface settings used to validate interface names
         */
        TroubleshootingValidator(NetworkSettings networkSettings) {
            this.networkSettings = networkSettings;
        }

        /**
         * Validates a single troubleshooting argument, performing character checks,
         * type validation, and rule-based restrictions depending on the command.
         *
         * @param command          the troubleshooting command being executed
         * @param key              the argument key (e.g., HOST, URL, TIMEOUT)
         * @param value            the argument value provided by the user
         * @param allowedArgs      mapping of valid keys permitted for the given command
         * @param suspiciousEntries list to accumulate validation failures or suspicious values
         */
        public void validate(
                TroubleshootingCommands command,
                String key,
                String value,
                Map<TroubleshootingCommands, Set<String>> allowedArgs,
                List<String> suspiciousEntries
        ) {

            // Reject unexpected args
            if (!allowedArgs.get(command).contains(key)) {
                throw new RuntimeException("Unexpected argument for " + command + ": " + key);
            }

            // Reject dangerous characters
            if (key.equals("TRACE_ARGUMENTS")) {
                if (!traceSafeChars.matcher(value).matches()) {
                    suspiciousEntries.add("Illegal characters in TRACE_ARGUMENTS");
                }
            } else if (!safeChars.matcher(value).matches() && !key.equals("HOST_PORT") && !key.equals("HOST")) {
                suspiciousEntries.add("Illegal characters in " + key + "=" + value);
            }

            switch (key) {
                case "HOST":
                case "DNS_TEST_HOST":
                case "TCP_TEST_HOST":
                    validateHost(command, value, suspiciousEntries);
                    break;
                case "URL":
                    if (!urlPat.matcher(value).matches()) {
                        suspiciousEntries.add("Invalid URL: " + value);
                    }
                    break;
                case "HOST_PORT":
                    validatePort(command, value, suspiciousEntries);
                    break;
                case "TIMEOUT":
                    validateTimeout(value, suspiciousEntries);
                    break;
                case "MODE":
                    if (!Set.of("basic", "advanced")
                            .contains(value.toLowerCase(Locale.ROOT))) {
                        suspiciousEntries.add("Invalid MODE: " + value);
                    }
                    break;
                case "PROTOCOL":
                    if (!Set.of("tcp", "udp", "icmp", "t", "u", "i")
                            .contains(value.toLowerCase(Locale.ROOT))) {
                        suspiciousEntries.add("Invalid PROTOCOL: " + value);
                    }
                    break;
                case "INTERFACE":
                    validateInterface(value, suspiciousEntries);
                    break;
                case "FILENAME":
                    if (!pcapFilePat.matcher(value).matches()) {
                        suspiciousEntries.add("Invalid .pcap filename: " + value);
                    }
                    break;
                case "TRACE_ARGUMENTS":
                    validateTraceArguments(value, suspiciousEntries);
                    break;
            }
        }

        /**
         * Validates the host value based on hostname, IPv4, or IPv6 rules.
         *
         * @param command          the troubleshooting command (TRACE allows blank/any host)
         * @param value            the host value to validate
         * @param suspiciousEntries list to collect validation errors
         */
        private void validateHost(TroubleshootingCommands command, String value, List<String> suspiciousEntries) {
            if (command == TroubleshootingCommands.TRACE) {
                if (StringUtils.isBlank(value) || value.equalsIgnoreCase("any")) return;
            }

            boolean valid = hostnamePat.matcher(value).matches()
                    || ipv4Pat.matcher(value).matches()
                    || ipv6Pat.matcher(value).matches();

            if (!valid) {
                suspiciousEntries.add("Invalid host: " + value);
            }
        }

        /**
         * Validates a port value ensuring numeric bounds and command-specific rules.
         *
         * @param command           troubleshooting command being executed
         * @param value             the port string to validate
         * @param suspiciousEntries list to collect validation errors
         */
        private void validatePort(TroubleshootingCommands command, String value, List<String> suspiciousEntries) {
            if (command == TroubleshootingCommands.TRACE && StringUtils.isBlank(value)) return;

            try {
                int p = Integer.parseInt(value);
                if (p < 1 || p > 65535) {
                    suspiciousEntries.add("Invalid port: " + value);
                }
            } catch (Exception ex) {
                suspiciousEntries.add("Non-numeric port: " + value);
            }
        }

        /**
         * Validates timeout ensuring it is numeric and within the allowed range (1 to 60).
         *
         * @param value            the timeout value as string
         * @param suspiciousEntries list to collect validation errors
         */
        private void validateTimeout(String value, List<String> suspiciousEntries) {
            try {
                int t = Integer.parseInt(value);
                if (t < 1 || t > 120) {
                    suspiciousEntries.add("Invalid timeout: " + value);
                }
            } catch (Exception ex) {
                suspiciousEntries.add("Non-numeric timeout: " + value);
            }
        }

        /**
         * Validates the provided network interface name against existing interfaces.
         *
         * @param value            interface name supplied by user
         * @param suspiciousEntries list to collect validation errors
         */
        private void validateInterface(String value, List<String> suspiciousEntries) {
            boolean found = false;

            for (InterfaceSettings ni : networkSettings.getInterfaces()) {
                if (ni.getSystemDev().equals(value)) {
                    found = true;
                    break;
                }
            }

            if (value.equals("tun0")) found = true;

            if (!found) {
                suspiciousEntries.add("Invalid interface: " + value);
            }
        }

        /**
         * Validates TRACE_ARGUMENTS by parsing tcpdump flags and ensuring
         * numeric/string parameters, forbidlist flags, and permitted BPF filters.
         *
         * @param value            full argument string for tcpdump
         * @param suspiciousEntries list to collect validation errors or unknown flags
         */
        private void validateTraceArguments(String value, List<String> suspiciousEntries) {
            String[] parts = value.split("\\s+");

            for (int i = 0; i < parts.length; i++) {
                String arg = parts[i];

                if (forbiddenFlags.contains(arg)) {
                    suspiciousEntries.add("Forbidden tcpdump flag: " + arg);
                    continue;
                }

                if (simpleFlags.contains(arg)) continue;

                if (numericFlags.contains(arg)) {
                    if (i + 1 >= parts.length || !parts[i + 1].matches("^\\d+$")) {
                        suspiciousEntries.add("Invalid numeric param for " + arg);
                    }
                    i++;
                    continue;
                }

                if (stringFlags.contains(arg)) {
                    if (i + 1 >= parts.length) {
                        suspiciousEntries.add("Missing string param for " + arg);
                    }
                    i++;
                    continue;
                }

                if (outputFlags.contains(arg)) {
                    if (i + 1 >= parts.length || !pcapFilePat.matcher(parts[i + 1]).matches()) {
                        suspiciousEntries.add("Invalid filename for -w");
                    }
                    i++;
                    continue;
                }

                // BPF filter allowlist
                if (!arg.startsWith("-")) {
                    if (!arg.matches("^(host|net|port)\\s*[A-Za-z0-9./]+$")) {
                        suspiciousEntries.add("Invalid BPF filter: " + arg);
                    }
                    continue;
                }

                suspiciousEntries.add("Unknown flag: " + arg);
            }
        }
    }

    /**
     * Function to register all network address blocks configured in this
     * application
     *
     * @param argSettings
     *        - The application settings
     */
    private void updateNetworkReservations(NetworkSettings argSettings)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        // start by clearing all existing registrations
        nsmgr.clearOwnerRegistrationAll(NETSPACE_OWNER);

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
            nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_STATIC_ADDRESS, intfma);

            for ( InterfaceSettings.InterfaceAlias alias : intf.getV4Aliases() ) {
                IPMaskedAddress aliasma = new IPMaskedAddress( alias.getStaticAddress(), alias.getStaticNetmask() );
                nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_STATIC_ALIAS, intfma);
            }
        }

        /**
         * Add dynamic v4 addresses
         */
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
            nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_DYNAMIC_ADDRESS, intfma);
        }
    }

    /**
     * Sets the interfacesOverloadedFlag, to handle cases where the user must remove interfaces and restart the NGFW box
     * 
     * @param value
     *      - The new flag value
     */
    public void setInterfacesOverloadedFlag(boolean value) {
        this.interfacesOverloadedFlag = value;
    }

    /**
     * Gets the interfacesOverloadedFlag, to handle cases where the user must remove interfaces and restart the NGFW box
     * 
     * @return the current flag value
     */
    public boolean getInterfacesOverloadedFlag() {
        return this.interfacesOverloadedFlag;
    }
}
