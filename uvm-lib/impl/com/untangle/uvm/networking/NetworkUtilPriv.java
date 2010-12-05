/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.networking;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.HostNameList;
import com.untangle.uvm.node.IPNullAddr;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.firewall.MACAddress;

/* Utilities that are only required inside of this package */
class NetworkUtilPriv extends NetworkUtil
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final NetworkUtilPriv INSTANCE = new NetworkUtilPriv();

    private static final String HOST_NAME_FILE        = "/etc/hostname";

    /* Index of the first network space */
    public static final int SPACE_INDEX_BASE = 0;

    /* Size of an ip addr byte array */
    public static final int IP_ADDR_SIZE_BYTES = 4;

    private NetworkUtilPriv()
    {
    }

    BasicNetworkSettings toBasic( NetworkSpacesInternalSettings settings, boolean isSingleNicEnabled )
    {
        BasicNetworkSettings basic = new BasicNetworkSettings();

        NetworkSpaceInternal primary = settings.getNetworkSpace( IntfConstants.EXTERNAL_INTF );

        if ( primary == null ) {
            logger.warn( "The external interface doesn't have a network space, using first network space." );
            primary = settings.getNetworkSpaceList().get( 0 );
        }
        
        basic.setDhcpEnabled( false );
        IPNetwork primaryNetwork = primary.getPrimaryAddress();

        basic.setHost( primaryNetwork.getNetwork());
        basic.setNetmask( primaryNetwork.getNetmask());

        /* interface aliases */
        List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();
        for ( IPNetwork network : primary.getNetworkList()) {
            if ( network.equals( primaryNetwork )) continue;
            aliasList.add( new InterfaceAlias( network.getNetwork(), network.getNetmask()));
        }
        basic.setAliasList( aliasList );
        
        basic.setDns1( settings.getDns1());
        basic.setDns2( settings.getDns2());
        basic.setGateway( settings.getDefaultRoute());
        basic.setSingleNicEnabled( isSingleNicEnabled );

        logger.debug( "created: " + basic );
        

        return basic;
    }

    /* Load the network properties, these are used for some of the settings. */
    Properties loadProperties() throws IOException
    {
        String cmd = System.getProperty( "uvm.bin.dir" ) + "/ut-net-properties";
        Process process = LocalUvmContextFactory.context().exec( cmd );
        Properties properties = new Properties();
        properties.load( process.getInputStream());
        return properties;
    }

    /* Load the network configuration */
    NetworkSpacesInternalSettings loadNetworkSettings( Properties properties )
    {
        String networkConfiguration = properties.getProperty( "com.untangle.networking.net-conf" );
        String bridgeConfiguration = properties.getProperty( "com.untangle.networking.bridge-conf" );
        
        if ( networkConfiguration == null ) {
            logger.warn( "The network configuration property is missing" );
            networkConfiguration = "";
        }
        if ( bridgeConfiguration == null ) bridgeConfiguration = "";
        
        /* Load the network configuration */
        Map<String,NetworkSpaceInternal> networkSpaceMap = new HashMap<String,NetworkSpaceInternal>();
        
        for ( String config : networkConfiguration.split( ":::" )) {
            NetworkSpaceInternal networkSpace = parseConfig( config );
            if ( networkSpace == null ) continue;
            networkSpaceMap.put( networkSpace.getName(), networkSpace );
        }
        
        /* Convert the hash to a list */
        List<NetworkSpaceInternal> networkSpaceList = 
            new LinkedList<NetworkSpaceInternal>( networkSpaceMap.values());


        List<InterfaceInternal> interfaceList = 
            buildInterfaceList( bridgeConfiguration, networkSpaceMap );

        IPaddr dns1 = parseIPaddr( properties, "com.untangle.networking.dns-1" );
        IPaddr dns2 = parseIPaddr( properties, "com.untangle.networking.dns-2" );
        IPaddr defaultGateway = parseIPaddr( properties, "com.untangle.networking.default-gateway" );

        /* just in case dns2 is specified and dns1 is not */
        if ( dns1 == null ) {
            dns1 = dns2;
            dns2 = null;
        }
            
        return new NetworkSpacesInternalSettings( interfaceList, networkSpaceList, 
                                                  dns1, dns2, defaultGateway );
    }

    /* Load the services settings based on the values inside of properties,
     * and in the dnsmasq.conf file
     * @param properties properties file from net-properties script
     * @param serviceAddress Address that dnsmasq is bound to on the internal interface.
     */
    ServicesInternalSettings loadServicesSettings( Properties properties, IPaddr serviceAddress )
    {
        ServicesSettingsImpl servicesSettings = new ServicesSettingsImpl();

        servicesSettings.
            setDhcpEnabled( parseBoolean( properties, "com.untangle.networking.dhcp-enabled" ));

        servicesSettings.
            setDhcpStartAddress( parseIPaddr( properties, "com.untangle.networking.dhcp-start" ));
        
        servicesSettings.
            setDhcpEndAddress( parseIPaddr( properties, "com.untangle.networking.dhcp-end" ));

        IPaddr defaultGateway = parseIPaddr( properties, "com.untangle.networking.dhcp-default-gateway" );

        if ( defaultGateway == null ) defaultGateway = serviceAddress;

        List<IPaddr> dnsServerList = new LinkedList<IPaddr>();
        String dnsServers = properties.getProperty( "com.untangle.networking.dhcp-dns-servers" );
        if ( dnsServers != null && ( dnsServers.trim().length() > 0 )) {
            for ( String dnsServer : dnsServers.split( "," )) {
                IPaddr ds = parseIPaddr( dnsServer );
                if ( ds != null ) dnsServerList.add( ds );
            }
        } else {
            /* Otherwise just use the address that is hosting services */
            dnsServerList.add( serviceAddress );
        }

        try {
            loadDhcpLeaseList( servicesSettings );
        } catch ( IOException e ) {
            logger.warn( "Error loading the DHCP Lease List", e );
        }

        servicesSettings.
            setDnsEnabled(  parseBoolean( properties, "com.untangle.networking.dns-enabled" ));

        servicesSettings.
            setDnsLocalDomain( parseHostname( properties, "com.untangle.networking.dns-local-domain" ));

        try {
            loadDnsStaticHostList( servicesSettings );
        } catch ( IOException e ) {
            logger.warn( "Error loading the DHCP Lease List", e );
        }
        
        return ServicesInternalSettings.makeInstance( servicesSettings, servicesSettings,
                                                      defaultGateway, dnsServerList );
    }
    
    /* Return the impl so this can go into a database */
    NetworkSpacesSettingsImpl toSettings( NetworkSpacesInternalSettings internalSettings )
    {
        NetworkSpacesSettingsImpl settings = new NetworkSpacesSettingsImpl();

        /* Generate the list network spaces and a map from internal -> normal. */
        List<NetworkSpace> networkSpaceList = new LinkedList<NetworkSpace>();

        settings.setIsEnabled( true );

        Map<NetworkSpaceInternal,NetworkSpace> networkSpaceMap = new HashMap<NetworkSpaceInternal,NetworkSpace>();

        NetworkSpace primary = null;

        for ( NetworkSpaceInternal si : internalSettings.getNetworkSpaceList()) {
            NetworkSpace space = si.toNetworkSpace();

            if ( primary == null ) primary = space;

            /* Placed into both in order to maintain the order of the items */
            networkSpaceMap.put( si, space );
            networkSpaceList.add( space );
            space.setIsPrimary( false );
        }

        /* Assuming there is at least one space, otherwise primary would be null. */
        primary.setIsPrimary( true );

        /* Generate the interfaces, wire them up to the correct network space */
        List<Interface> intfList = new LinkedList<Interface>();
        for ( InterfaceInternal intfInternal : internalSettings.getInterfaceList()) {
            Interface i = intfInternal.toInterface();
            NetworkSpace space = networkSpaceMap.get( intfInternal.getNetworkSpace());
            i.setNetworkSpace(( space == null ) ? primary : space );
            intfList.add( i );
        }

        /* Generate the routing table. */
        List<Route> routingTable = new LinkedList<Route>();

        /* Set all of the simple settings (eg defaultRoute) */
        settings.setInterfaceList( intfList );
        settings.setNetworkSpaceList( networkSpaceList );
        settings.setRoutingTable( routingTable );
        settings.setDefaultRoute( internalSettings.getDefaultRoute());
        settings.setDns1( internalSettings.getDns1());
        settings.setDns2( internalSettings.getDns2());
        settings.setHasCompletedSetup( true );

        settings.setRedirectList( new LinkedList<RedirectRule>());

        return settings;
    }

    List<IPaddr> getDnsServers()
    {
        List<IPaddr> dnsServers = new LinkedList<IPaddr>();

        BufferedReader in = null;

        /* Open up the interfaces file */
        try {
            in = new BufferedReader( new FileReader( NetworkManagerImpl.ETC_RESOLV_FILE ));
            String str;
            while (( str = in.readLine()) != null ) {
                str = str.trim();
                if ( str.startsWith( ResolvScriptWriter.NS_PARAM )) {
                    String server = str.substring( ResolvScriptWriter.NS_PARAM.length()).trim();
                    
                    /* ignore anything that uses the localhost */
                    if ( "127.0.0.1".equals( server )) continue;
                    dnsServers.add( IPaddr.parse( server ));
                }
            }
        } catch ( Exception ex ) {
            logger.error( "Error reading file: ", ex );
        }

        try {
            if ( in != null ) in.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file", ex );
        }

        return dnsServers;
    }

    /* Check if the address refers to the edgeguard box itself */
    public boolean isAddressLocal( NetworkSpacesInternalSettings settings, IPaddr address )
    {
        InetAddress addr = address.getAddr();
        if (addr.isLoopbackAddress() || addr.isLinkLocalAddress())
            return true;

        List<NetworkSpaceInternal> spaces = settings.getNetworkSpaceList();

        for (NetworkSpaceInternal space : spaces) {
            /* Check the primary address */
            if ( address.equals( space.getPrimaryAddress().getNetwork())) return true;
            
            for( IPNetwork network : space.getNetworkList()) {
                IPaddr ip = network.getNetwork();
                if ( ip.equals( address )) return true;
            }
        }

        return false;
    }

    byte[] getArgonIntfArray()
    {
        return LocalUvmContextFactory.context().localIntfManager().getArgonIntfArray();
    }

    /* Get the hostname of the box from the /etc/hostname file */
    HostName loadHostname()
    {
        HostName hostname = NetworkUtil.DEFAULT_HOSTNAME;

        BufferedReader in = null;

        /* Open up the interfaces file */
        try {
            in = new BufferedReader(new FileReader( HOST_NAME_FILE ));
            String str;
            str = in.readLine().trim();

            /* Try to parse the hostname, throws an exception if it fails */
            hostname = HostName.parse( str );
        } catch ( Exception ex ) {
            /* Go to the default */
            hostname = NetworkUtil.DEFAULT_HOSTNAME;
        }

        try {
            if ( in != null ) in.close();
        } catch ( Exception e ) {
            logger.error( "Error closing file: " + e );
        }

        return hostname;
    }

    static NetworkUtilPriv getPrivInstance()
    {
        return INSTANCE;
    }

    private NetworkSpaceInternal parseConfig( String config )
    {
        String configArray[] = config.split( ";" );
        if ( configArray.length < 2 ) return null;

        NetworkSpace space = new NetworkSpace();
        space.setName( configArray[0] );
        List<IPNetworkRule> networkList = new LinkedList<IPNetworkRule>();

        try {
            for ( int c = 1 ; c < configArray.length ; c++ ) {
                networkList.add( IPNetworkRule.parse( configArray[c] ));
            }
            
            space.setNetworkList( networkList );

            IPNetwork primary = networkList.get( 0 ).getIPNetwork();
            return  new NetworkSpaceInternal( space, primary );
        } catch ( ValidateException e ) {
            logger.warn( "Unable to parse configuration: '" + config + "'", e );
            return null;
        }
    }

    private List<InterfaceInternal> buildInterfaceList( String bridgeConfiguration, 
                                                        Map<String,NetworkSpaceInternal> networkSpaceMap )
    {
        List<ArgonInterface> argonIntfList = 
            LocalUvmContextFactory.context().localIntfManager().getIntfList();
        
        /* First build a map of the interface -> bridge-name */
        Map<String,String> map = new HashMap<String,String>();
        
        for ( String bridge : bridgeConfiguration.split( ":::" )) {
            if ( bridge.length() == 0 ) continue;
            String bridgeArray[] = bridge.split( ";" );

            /* Something is wrong here */
            if ( bridgeArray.length < 2 ) continue;
            
            for ( int c = 1 ; c< bridgeArray.length; c++ ) {
                if ( bridgeArray[c].length() == 0 ) continue;
                String prev = map.put( bridgeArray[c], bridgeArray[0] );
                if ( prev != null ) {
                    logger.warn( "the interface '" + bridgeArray[c] + "' is in two bridges: " + prev );
                }
            }
        }

        /* Now create all of the interfaces */
        List<InterfaceInternal> interfaceList = new LinkedList<InterfaceInternal>();

        for ( ArgonInterface argonIntf : argonIntfList ) {
            String name = argonIntf.getName();
            String bridge = map.get( name );
            NetworkSpaceInternal networkSpace = networkSpaceMap.get(( bridge == null ) ? name : bridge );
            
            if ( networkSpace == null ) {
                if (!"tun0".equals(name)) /* don't show warning for tun0 */
                    logger.warn( "Unable to find a network space for: '" + name + "'" );
                /* this is not all that legit */
                networkSpace = parseConfig( name + ";" + NetworkUtil.BOGUS_ADDRESS + "/32" );
            }
            boolean isPhysicalInterface = true;
            if ( argonIntf.getArgon() == IntfConstants.VPN_INTF ) isPhysicalInterface = false;
            Interface i = new Interface( isPhysicalInterface );
            i.setArgonIntf( argonIntf.getArgon());
            i.setName( argonIntf.getUserName());
            i.setSystemName( argonIntf.getPhysicalName());
            
            try {
                interfaceList.add( InterfaceInternal.makeInterfaceInternal( i, networkSpace ));
            } catch ( ValidateException e ) {
                logger.warn( "Unable to build the interface", e );
            }
        }

        return interfaceList;
    }

    private void loadDhcpLeaseList( ServicesSettings servicesSettings ) throws IOException
    {
        String cmd = System.getProperty( "uvm.bin.dir" ) + "/ut-dhcp-lease-list";
        Process process = LocalUvmContextFactory.context().exec( cmd );
        
        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream()));
        
        List<DhcpLeaseRule> dhcpLeaseList = new LinkedList<DhcpLeaseRule>();

        String line;

        while (( line = reader.readLine()) != null ) {
            DhcpLeaseRule rule = new DhcpLeaseRule();
            String pieces[] = line.split( ";" );
            if ( pieces.length != 2 ) continue;
            try {
                rule.setMacAddress( MACAddress.parse( pieces[0] ));
                rule.setStaticAddress( IPNullAddr.parse( pieces[1] ));

                dhcpLeaseList.add( rule );
            } catch ( Exception e ) {
                /* silently ignoring exceptions. */
                logger.warn( "Unable to parse '" + line + "'", e );
            }
        }

        try {
            reader.close();
        } catch ( IOException e ) {
            logger.warn( "Unable to close the process reader", e );
        }

        servicesSettings.setDhcpLeaseList( dhcpLeaseList );
    }

    private void loadDnsStaticHostList( ServicesSettings servicesSettings ) throws IOException
    {
        String cmd = System.getProperty( "uvm.bin.dir" ) + "/ut-dns-static-host-list";
        Process process = LocalUvmContextFactory.context().exec( cmd );
        
        BufferedReader reader = new BufferedReader( new InputStreamReader( process.getInputStream()));
        
        List<DnsStaticHostRule> dnsStaticHostList = new LinkedList<DnsStaticHostRule>();

        String line;

        while (( line = reader.readLine()) != null ) {
            DnsStaticHostRule rule = new DnsStaticHostRule();
            String pieces[] = line.split( ";" );
            if ( pieces.length != 2 ) continue;
            try {
                rule.setStaticAddress( IPaddr.parse( pieces[0] ));
                rule.setHostNameList( HostNameList.parse( pieces[1] ));

                dnsStaticHostList.add( rule );
            } catch ( Exception e ) {
                /* silently ignoring exceptions. */
                logger.warn( "Unable to parse '" + line + "'", e );
            }
        }

        try {
            reader.close();
        } catch ( IOException e ) {
            logger.warn( "Unable to close the process reader", e );
        }

        servicesSettings.setDnsStaticHostList( dnsStaticHostList );
    }
    
    private IPaddr parseIPaddr( Properties properties, String property )
    {
        return parseIPaddr( properties.getProperty( property ));
    }

    private IPaddr parseIPaddr( String value )
    {
        if ( value == null ) return null;
        
        value = value.trim();
        if ( value.length() == 0 ) return null;
        try {
            return IPaddr.parse( value );
        } catch ( ParseException e ) {
            logger.debug( "Unable to parse: '" + value + "'" );
        } catch ( UnknownHostException e ) {
            logger.debug( "Unable to parse: '" + value + "'" );
        }
        
        return null;
    }

    private boolean parseBoolean( Properties properties, String property )
    {
        return Boolean.parseBoolean( properties.getProperty( property ));
    }

    private HostName parseHostname( Properties properties, String property )
    {
        String value = properties.getProperty( property );
        if ( value == null ) return null;

        try {
            return HostName.parse( value );
        } catch ( ParseException e ) {
            logger.warn( "Unable to parse hostname: '" + value + "'", e );
        }
        
        return null;
    }
}
