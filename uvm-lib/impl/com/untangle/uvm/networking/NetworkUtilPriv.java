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
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.untangle.jnetcap.InterfaceData;
import com.untangle.jnetcap.Netcap;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.RedirectInternal;
import com.untangle.uvm.networking.internal.RouteInternal;
import com.untangle.uvm.networking.internal.ServicesInternalSettings;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.util.ConfigFileUtil;
import org.apache.log4j.Logger;

/* Utilities that are only required inside of this package */
class NetworkUtilPriv extends NetworkUtil
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final NetworkUtilPriv INSTANCE = new NetworkUtilPriv();

    private static final String HOST_NAME_FILE        = "/etc/hostname";

    /* Prefix for the bridge devices */
    private static final String BRIDGE_PREFIX  = "br";

    /* Index of the first network space */
    public static final int SPACE_INDEX_BASE = 0;

    /* Size of an ip addr byte array */
    public static final int IP_ADDR_SIZE_BYTES = 4;

    private NetworkUtilPriv()
    {
    }

    BasicNetworkSettings toBasic( NetworkSpacesInternalSettings settings )
    {
        BasicNetworkSettings basic = new BasicNetworkSettings();

        NetworkSpaceInternal primary = settings.getNetworkSpaceList().get( 0 );
        
        basic.isDhcpEnabled( false );
        IPNetwork primaryNetwork = primary.getPrimaryAddress();

        basic.host( primaryNetwork.getNetwork());
        basic.netmask( primaryNetwork.getNetmask());

        /* interface aliases */
        List<InterfaceAlias> aliasList = new LinkedList<InterfaceAlias>();
        for ( IPNetwork network : primary.getNetworkList()) {
            if ( network.equals( primaryNetwork )) continue;
            aliasList.add( new InterfaceAlias( network.getNetwork(), network.getNetmask()));
        }
        basic.setAliasList( aliasList );
        
        basic.dns1( settings.getDns1());
        basic.dns2( settings.getDns2());
        basic.gateway( settings.getDefaultRoute());

        logger.debug( "created: " + basic );

        return basic;
    }

    /* Load the network configuration */
    NetworkSpacesInternalSettings loadConfiguration()
    {
        try {
            String cmd = System.getProperty( "bunnicula.bin.dir" ) + "/net-properties";
            Process process = LocalUvmContextFactory.context().exec( cmd );
            Properties properties = new Properties();
            properties.load( process.getInputStream());
            String networkConfiguration = properties.getProperty( "com.untangle.networking.net-conf" );
            String bridgeConfiguration = properties.getProperty( "com.untangle.networking.bridge-conf" );

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

            IPaddr dns1 = parseIPaddr( properties.getProperty( "com.untangle.networking.dns-1" ));
            IPaddr dns2 = parseIPaddr( properties.getProperty( "com.untangle.networking.dns-2" ));
            IPaddr defaultGateway = parseIPaddr( properties.getProperty( "com.untangle.networking.default-gateway" ));

            /* just in case dns2 is specified and dns1 is not */
            if ( dns1 == null ) {
                dns1 = dns2;
                dns2 = null;
            }
            
            return new NetworkSpacesInternalSettings( interfaceList, networkSpaceList, 
                                                      dns1, dns2, defaultGateway );
        } catch ( Exception e ) {
            logger.warn( "unable to load network configuration.", e );
            return null;
        } 
    }

    /* Return the impl so this can go into a database */
    NetworkSpacesSettingsImpl toSettings( NetworkSpacesInternalSettings internalSettings )
    {
        NetworkSpacesSettingsImpl settings = new NetworkSpacesSettingsImpl();

        /* Generate the list network spaces and a map from internal -> normal. */
        List<NetworkSpace> networkSpaceList = new LinkedList<NetworkSpace>();

        settings.setIsEnabled( true );

        Map<NetworkSpaceInternal,NetworkSpace> networkSpaceMap =
            new HashMap<NetworkSpaceInternal,NetworkSpace>();

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

    /************* PRIVATE **********/
    private IPNetwork getPrimaryAddress( NetworkSpace networkSpace, int index )
    {
        IPNetwork primaryAddress = IPNetwork.getEmptyNetwork();

        if ( networkSpace.getIsDhcpEnabled()) {
            DhcpStatus status = networkSpace.getDhcpStatus();
            primaryAddress = IPNetwork.makeInstance( status.getAddress(), status.getNetmask());
        } else {
            for ( IPNetworkRule rule : (List<IPNetworkRule>)networkSpace.getNetworkList()) {
                if ( rule.isUnicast()) {
                    primaryAddress = rule.getIPNetwork();
                    break;
                }
            }
        }

        if ( primaryAddress == null || primaryAddress.equals( IPNetwork.getEmptyNetwork())) {
            /* XXX This is where it would handle the empty ip network */
            logger.warn( "Network space " + index + " does not have a primary address" );
        }

        return ( primaryAddress == null ) ? IPNetwork.getEmptyNetwork() : primaryAddress;
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
                logger.warn( "Unable to find a network space for: '" + name + "'" );
                /* this is not all that legit */
                networkSpace = parseConfig( name + ";192.0.2.1/32" );
            }
            Interface i = new Interface();
            i.setArgonIntf( argonIntf.getArgon());
            
            try {
                interfaceList.add( InterfaceInternal.makeInterfaceInternal( i, networkSpace ));
            } catch ( ValidateException e ) {
                logger.warn( "Unable to build the interface", e );
            }
        }

        return interfaceList;
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
}
