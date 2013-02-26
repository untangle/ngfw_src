/**
 * $Id: NewNetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.net.InetAddress;

import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NewNetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.NetworkSettingsListener;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.node.IPMaskedAddress;

/**
 * The Network Manager handles all the network configuration
 */
public class NewNetworkManagerImpl implements NewNetworkManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private final String updateRulesScript = System.getProperty("uvm.bin.dir") + "/ut-uvm-update-rules.sh";

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "network";
    private final String settingsFilenameBackup = "/etc/untangle-netd/network";
    
    private NetworkSettings networkSettings;

    /* networkListeners stores parties interested in being notified of network settings change */
    private LinkedList<NetworkSettingsListener> networkListeners = new LinkedList<NetworkSettingsListener>();
    
    protected NewNetworkManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        NetworkSettings readSettings = null;

        try {
            readSettings = settingsManager.load( NetworkSettings.class, this.settingsFilename );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If its the dev env, try loading from /etc
         */
        if ( readSettings == null && UvmContextFactory.context().isDevel() ) {
            try {
                // check for "backup" settings in /etc
                readSettings = settingsManager.load( NetworkSettings.class, this.settingsFilenameBackup );
                
                // check for "backup" settings in /usr/share/untangle/settings/
                if (readSettings == null)
                    readSettings = settingsManager.load( NetworkSettings.class, "/usr/share/untangle/settings/untangle-vm/network" );
                    
                if (readSettings != null)
                    settingsManager.save(NetworkSettings.class, this.settingsFilename, readSettings);
                    
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
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

        // notify interested parties that the settings have changed
        callNetworkListeners();
    }

    /**
     * Insert the iptables rules for capturing traffic
     */
    public void insertRules( )
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
    
    public void registerListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.add( networkListener );
    }

    public void unregisterListener( NetworkSettingsListener networkListener )
    {
        this.networkListeners.remove( networkListener );
    }

   public InetAddress getFirstWanAddress()
    {
        if ( this.networkSettings == null || this.networkSettings.getInterfaces() == null ) {
            return null;
        }
        
        for ( InterfaceSettings intfSettings : this.networkSettings.getInterfaces() ) {
            if ( intfSettings.getIsWan() ) {
                //FIXME what if pppoe or dhcp ?
                return intfSettings.getV4StaticAddress();
            }
                
        }

        return null;
    }

    /*
     * This returns an address where the host should be able to access
     * HTTP.  if HTTP is not reachable, this returns NULL
     */
    public InetAddress getInternalHttpAddress( int clientIntf )
    {
        /* Retrieve the network settings */
        NetworkSettings netSettings = this.networkSettings;
        if ( netSettings == null ) {
            logger.warn("Failed to fetch network configuration");
            return null;
        }

        InterfaceSettings intfSettings = netSettings.findInterfaceId( clientIntf );
        if ( intfSettings == null ) {
            logger.warn("Failed to fetch interface configuration");
            return null;
        }

        /* WAN ports never have HTTP open */
        boolean isWan = intfSettings.getIsWan();
        if ( isWan ) {
            //this is normal no error logged
            return null;
        }
        
        /**
         * If this interface is bridged with another, use the addr from the other
         */
        if (InterfaceSettings.CONFIG_BRIDGED.equals(intfSettings.getV4ConfigType())) {
            Integer bridgedTo = intfSettings.getBridgedTo();
            intfSettings = netSettings.findInterfaceId( bridgedTo );

            if ( intfSettings == null ) {
                logger.warn("No Interface found for name: " + bridgedTo );
                return null;
            }
        }

        /**
         * The primary IP of OpenVPN interface is not in the config
         * Must query the openVPN node
         */
        if (intfSettings.getInterfaceId() == 250) {
            // FIXME how to handle OpenVPN?
            //             OpenVpn openvpn = (OpenVpn) UvmContextFactory.context().nodeManager().node("untangle-node-openvpn");
//             if (openvpn == null) {
//                 logger.warn("OpenVPN node not found");
//                 return null;
//             }
            
//             InetAddress addr = openvpn.getVpnServerAddress().getIp();
//             if (addr == null) {
//                 logger.warn("VPN Server address not found");
//                 return null;
//             }

//             return addr;
        }

        //FIXME must support dhcp and pppoe
        InetAddress address = intfSettings.getV4StaticAddress();
        return address;
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
    
    private NetworkSettings defaultSettings()
    {
        NetworkSettings newSettings = new NetworkSettings();

        newSettings.setHostName("hostname.example.com");
        newSettings.setDomainName("example.com");
                                
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort " );
        String devices[] = result.getOutput().split("\\r?\\n");
        
        try {
            LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();

            if (devices.length > 0) {
                InterfaceSettings external = new InterfaceSettings();
                external.setInterfaceId(1);
                external.setName("Externál");
                external.setIsWan(true);
                external.setPhysicalDev(devices[0]);
                external.setSystemDev(devices[0]);
                external.setSymbolicDev(devices[0]);
                external.setConfig("addressed");
                external.setV4ConfigType("auto");
                //external.setV4StaticAddress(InetAddress.getByName("172.16.2.60"));
                //external.setV4StaticNetmask(InetAddress.getByName("255.255.0.0"));
                //external.setV4StaticGateway(InetAddress.getByName("172.16.2.1"));
                //external.setV4StaticDns1(InetAddress.getByName("172.16.2.1"));
                //external.setV4StaticAddress(InetAddress.getByName("10.0.0.60"));
                //external.setV4StaticNetmask(InetAddress.getByName("255.0.0.0"));
                //external.setV4StaticGateway(InetAddress.getByName("10.0.0.1"));
                //external.setV4StaticDns1(InetAddress.getByName("10.0.0.1"));
                external.setV6ConfigType("auto");
                interfaces.add(external);
            }
        
            if (devices.length > 1) {
                InterfaceSettings internal = new InterfaceSettings();
                internal.setInterfaceId(2);
                internal.setName("Internál");
                internal.setPhysicalDev(devices[1]);
                internal.setSystemDev(devices[1]);
                internal.setSymbolicDev(devices[1]);
                internal.setConfig("addressed");
                internal.setV4ConfigType("static");
                internal.setV4StaticAddress(InetAddress.getByName("192.168.2.1"));
                internal.setV4StaticNetmask(InetAddress.getByName("255.255.255.0"));
                internal.setIsWan(false);
                internal.setV6ConfigType("static");
                internal.setV6StaticAddress(InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7334"));
                internal.setV6StaticPrefixLength(64);
                internal.setBridgedTo(1);
                interfaces.add(internal);
            }

            for (int i = 2 ; i < devices.length ; i++ ) {
                String[] greekNames = new String[]{"Alpha","Beta","Gamma","Delta","Epsilon","Zeta","Eta","Theta","Iota","Kappa","Lambda","Mu"};
                
                InterfaceSettings intf = new InterfaceSettings();
                intf.setInterfaceId( i + 1 );
                try {
                    intf.setName("Interface " + greekNames[i + 1]);
                } catch (Exception e) {
                    intf.setName("Interface " + (i + 1));
                }
                intf.setPhysicalDev(devices[i]);
                intf.setSystemDev(devices[i]);
                intf.setSymbolicDev(devices[i]);
                intf.setConfig("disabled");
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

        /**
         * Reset all symbolic devs to system devs
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
            if (maddr.maskNumBits() <= 24) {
                start = InetAddress.getByName("0.0.0.100");
                end   = InetAddress.getByName("0.0.0.200");
            } else {
                start = InetAddress.getByName("0.0.0.16");
                end   = InetAddress.getByName("0.0.0.99");
            }

            // bitwise OR the selected start and end with the base address
            InetAddress baseAddr = maddr.getMaskedAddr();
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
            interfaceSettings.setDhcpAuthoritative( true );
            interfaceSettings.setDhcpLeaseDuration( 24*60*60 ); // 24 hours
            interfaceSettings.setDhcpLeaseLimit( 100 ); // 100
        }
        catch (Exception e) {
            logger.warn("Exception initializing DHCP Address: ",e);
            interfaceSettings.setDhcpEnabled( false );
        }
    }

    /* Listener functions */
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
    
}
