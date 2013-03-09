/**
 * $Id: NetworkManagerImpl.java,v 1.00 2013/01/07 12:15:03 dmorris Exp $
 */
package com.untangle.uvm.engine;

import java.util.LinkedList;
import java.util.List;
import java.net.InetAddress;

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
import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.StaticRoute;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.PortForwardRule;
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
                    // XXX this doesn't work because we re-link settings to our local settings
                    // check for "backup" settings in /usr/share/untangle/settings/
                    String rootLocation = "/usr/share/untangle/settings/untangle-vm/network";
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
            //FIXME can remove me later - for testing
            if (readSettings.getQosSettings() == null)
                readSettings.setQosSettings( defaultQosSettings() );
            //FIXME can remove me later - for testing
            if ( false ) {
                DnsSettings dnsSettings = new DnsSettings();
                LinkedList<DnsStaticEntry> staticEntries = new LinkedList<DnsStaticEntry>();
                LinkedList<DnsLocalServer> localServers = new LinkedList<DnsLocalServer>();
                try {
                    staticEntries.add( new DnsStaticEntry( "chef" , InetAddress.getByName("10.0.0.10")) ); // XXX for testing
                    staticEntries.add( new DnsStaticEntry( "chef.metaloft.com" , InetAddress.getByName("10.0.0.10")) ); // XXX for testing
                    localServers.add( new DnsLocalServer( "metaloft.com", InetAddress.getByName("10.0.0.1")

                                                          ) );
                } catch (Exception e) {}
                dnsSettings.setStaticEntries( staticEntries );
                dnsSettings.setLocalServers( localServers );
                
                readSettings.setDnsSettings( dnsSettings );
            }

            
            this.networkSettings = readSettings;
            logger.debug( "Loading Settings: " + this.networkSettings.toJSONString() );
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
        boolean isWan = intfSettings.getIsWan();
        if ( isWan ) {
            //this is normal no error logged
            return null;
        }
        
        /**
         * If this interface is bridged with another, use the addr from the other
         */
        if ( InterfaceSettings.ConfigType.BRIDGED == intfSettings.getConfigType() ) {
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

        if (status == null)
            return new InterfaceStatus(); // never return null
        else
            return status;
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

        newSettings.setHostName( UvmContextFactory.context().oemManager().getOemName().toLowerCase() );
        newSettings.setDomainName( "example.com" );
                                
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( "find /sys/class/net -type l -name 'eth*' | sed -e 's|/sys/class/net/||' | sort " );
        String devices[] = result.getOutput().split("\\r?\\n");
        
        try {
            LinkedList<InterfaceSettings> interfaces = new LinkedList<InterfaceSettings>();

            if (devices.length > 0) {
                InterfaceSettings external = new InterfaceSettings();
                external.setInterfaceId( 1 );
                external.setName( "Extern\u00e1l" );
                external.setIsWan( true );
                external.setPhysicalDev( devices[0] );
                external.setSystemDev( devices[0] );
                external.setSymbolicDev( devices[0] );
                external.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                external.setV4ConfigType( InterfaceSettings.V4ConfigType.AUTO );
                external.setV6ConfigType( InterfaceSettings.V6ConfigType.AUTO );
                external.setV4NatEgressTraffic( true );
                interfaces.add( external );
            }
        
            if (devices.length > 1) {
                InterfaceSettings internal = new InterfaceSettings();
                internal.setInterfaceId( 2 );
                internal.setName( "Intern\u00e1l" );
                internal.setIsWan( false );
                internal.setPhysicalDev( devices[1] );
                internal.setSystemDev( devices[1] );
                internal.setSymbolicDev( devices[1] );
                internal.setConfigType( InterfaceSettings.ConfigType.ADDRESSED );
                internal.setV4ConfigType( InterfaceSettings.V4ConfigType.STATIC );
                internal.setV4StaticAddress( InetAddress.getByName("192.168.2.1") );
                internal.setV4StaticPrefix( 24 );
                internal.setDhcpEnabled( true );
                // FIXME what to set IPv6 to?
                internal.setV6ConfigType( InterfaceSettings.V6ConfigType.STATIC );
                internal.setV6StaticAddress( InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7334") );
                internal.setV6StaticPrefixLength( 64 );
                internal.setBridgedTo( 1 );

                // InterfaceSettings.InterfaceAlias alias = new InterfaceSettings.InterfaceAlias();
                // alias.setV4StaticAddress( InetAddress.getByName("192.168.3.1") );
                // alias.setV4StaticPrefix( 24 );
                // List<InterfaceSettings.InterfaceAlias> aliases = new LinkedList<InterfaceSettings.InterfaceAlias>();
                // aliases.add(alias);
                // internal.setV4Aliases( aliases );

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
            interfaceSettings.setDhcpLeaseDuration( 60*60 ); // 1 hours (dnsmasq doc suggested value)
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

    private QosSettings defaultQosSettings()
    {
        QosSettings qosSettings = new QosSettings();

        qosSettings.setQosEnabled( false );
        qosSettings.setPingPriority( 1 );
        qosSettings.setDnsPriority( 1 );
        qosSettings.setSshPriority( 0 );
        qosSettings.setOpenvpnPriority( 0 );
        qosSettings.setGamingPriority( 0 );

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
        qosPriorities.add( new QosPriority( 5, "Limited More",    2,  50,  2, 50) );
        qosPriorities.add( new QosPriority( 5, "Limited Severly", 2,  10,  2, 10) );

        qosSettings.setQosPriorities( qosPriorities );
        
        
        return qosSettings;
    }

}
