/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.net.Inet4Address;


import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.InterfaceData;
import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.JNetcapException;

import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.script.ScriptRunner;

/* Class to convert a user from pre network spaces, to network spaces */
class Converter
{
    private static final Logger logger = Logger.getLogger( Converter.class );

    private static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );

    /* Script to test whether or not DHCP is enabled */
    private static final String DHCP_TEST_SCRIPT    = BUNNICULA_BASE + "/networking/dhcp-check";
    private static final int    DHCP_ENABLED_CODE   = 1;
    
    Converter()
    {
    }

    /* Migrate the settings from pre network spaces to network spaces,
     * Netcap.updateAddress should have already been called. 
     * Have to check in order to verify that the return value from this 
     * function cannot fail validate.
     * Assuming one of two possibilities:
     *   1. all interfaces are in the same space (bridge).
     *   2. internal is in its own space, and everything else is another space.
     *
     * *** note *** These settings have not run through
     *              NetworkUtilPriv.getInstance().complete yet.
     *
     * Here are the steps:
     *   1. Get the list of internal addresses.
     *   2. Get the list of external addresses.
     *   3. If they match, create one network space for both of them.
     *   4. If there is a mismatch, create two network spaces.  (second one with nat enabled)
     *   5. Grab DNS settings
     *   6. Check if the external space is configured using DHCP (set DHCP, and settings to DHCPAddress)
     *   7. If not in DHCP, get the default route.
     *   fin. Set defaults 
     *     space.mtu=1500
     *     space.isTrafficForwarded=true
     *     space.isPingable=true
     *     interface.ethernetMedia=AUTO_NEGOTIATE
     *
     * @throws NetworkException - something catastrophic happened, use reasonable defaults.
     */
    NetworkSettings upgradeSettings() throws NetworkException, JNetcapException, ArgonException
    {
        IntfConverter ic = IntfConverter.getInstance();
        Netcap netcap = Netcap.getInstance();
        NetworkSettings settings = new NetworkSettings();
        
        /* Get the address for each interface */
        List<InterfaceData> internalAddressList = null;
        List<InterfaceData> externalAddressList = null;

        /** This won't work until netcap is initialized */
        internalAddressList = 
            netcap.getInterfaceData( ic.argonIntfToString( IntfConstants.INTERNAL_INTF ));
        
        externalAddressList = 
            netcap.getInterfaceData( ic.argonIntfToString( IntfConstants.EXTERNAL_INTF ));
        
        /* Create the primary network space for External and DMZ */
        NetworkSpace external = new NetworkSpace();
        NetworkSpace internal = external;

        List<NetworkSpace> networkSpaceList = new LinkedList<NetworkSpace>();

        external.setNetworkList( makeNetworkList( externalAddressList ));

        external.setIsTrafficForwarded( true );
        external.setIsNatEnabled( false );
        networkSpaceList.add( external );

        if ( isTwoNetworkSpaces( internalAddressList, externalAddressList )) {
            internal = new NetworkSpace();
                    
            internal.setNetworkList( makeNetworkList( internalAddressList ));
            internal.setIsNatEnabled( true );
            /* !!!!, has to check for DHCP for whether or not this address is valid, blah */
            internal.setNatAddress( external.getPrimaryAddress().getNetwork());
            internal.setIsDhcpEnabled( false );

            networkSpaceList.add( internal );
        }

        /* Setup the defaults for all of the network spaces */
        for ( NetworkSpace space : networkSpaceList ) {
            space.setIsTrafficForwarded( true );
            space.setMtu( NetworkSpace.DEFAULT_MTU );
        }

        settings.setInterfaceList( makeInterfaceList( external, internal ));
        settings.setNetworkSpaceList( networkSpaceList );

        setDnsServers( settings );

        setDefaultRoute( settings );
        
        /* Setup DHCP last, this can unset the settings for dns and default route. */
        setDhcp( settings, external );

        return settings;
    }
    
    private List<IPNetworkRule> makeNetworkList( List<InterfaceData> addressList )
    {
        List<IPNetworkRule> networkList = new LinkedList<IPNetworkRule>();
        
        for ( InterfaceData data : addressList ) {
            logger.debug( "Inserting ip network for [" + data + "]" );
            networkList.
                add( new IPNetworkRule( IPNetwork.makeIPNetwork( data.getAddress(), data.getNetmask())));
        }

        return networkList;
    }

    private List<Interface> makeInterfaceList( NetworkSpace external, NetworkSpace internal )
    {
        List<Interface> interfaceList = new LinkedList<Interface>();
        
        /* Add External and DMZ to the external space */
        Interface intf = 
            new Interface( IntfConstants.EXTERNAL_INTF, external, EthernetMedia.AUTO_NEGOTIATE, false );
                           
        interfaceList.add( intf );

        intf = new Interface( IntfConstants.DMZ_INTF, external, EthernetMedia.AUTO_NEGOTIATE, false );
        interfaceList.add( intf );
        
        /* Add to the internal interface to the internal space */
        intf = new Interface( IntfConstants.INTERNAL_INTF, internal, EthernetMedia.AUTO_NEGOTIATE, false );
        interfaceList.add( intf );
        
        return interfaceList;
    }

    private void setDnsServers( NetworkSettings settings )
    {
        List<IPaddr> dnsServers = NetworkUtilPriv.getPrivInstance().getDnsServers();
        
        switch ( dnsServers.size()) {
        case 0:
            break;
            
        case 1:
            settings.setDns1( dnsServers.get( 0 ));
            break;
            
        default:
            settings.setDns1( dnsServers.get( 0 ));
            settings.setDns2( dnsServers.get( 1 ));
        }
    }

    private void setDhcp( NetworkSettings settings, NetworkSpace external )
    {
        DhcpStatus dhcpStatus = null;

        boolean isDhcpEnabled;
        try {
            int code = 0;
            Process p = Runtime.getRuntime().exec( "sh " + DHCP_TEST_SCRIPT  );
            code = p.waitFor();
            
            isDhcpEnabled = ( code == DHCP_ENABLED_CODE );
        } catch ( Exception e ) { 
            logger.warn( "Error testing DHCP address, continuing with false.", e );
            isDhcpEnabled = false;
        }

        if ( isDhcpEnabled ) {
            external.setIsDhcpEnabled( true );
            List<IPNetworkRule> networkList = (List<IPNetworkRule>)external.getNetworkList();
            
            /* Remove the first address, because that was assigned by DHCP */
            IPaddr address = null;
            IPaddr netmask = null;
            
            if ( networkList.size() > 1 ) {
                IPNetworkRule rule = networkList.remove( 0 );
                address = rule.getNetwork();
                netmask = rule.getNetmask();
            }

            IPaddr dns1 = settings.getDns1();
            IPaddr dns2 = settings.getDns2();
            IPaddr defaultRoute = settings.getDefaultRoute();

            /* Create a new dhcp status with all of the necessary info */
            dhcpStatus = new DhcpStatus( address, netmask, defaultRoute, dns1, dns2 );

            /* Clear out everyhing else that was set by the dns server */
            settings.setDns1( null );
            settings.setDns2( null );
            settings.setDefaultRoute( null );
            
        } else {
            external.setIsDhcpEnabled( false );
        }
        
        external.setDhcpStatus( dhcpStatus );
    }

    private void setDefaultRoute( NetworkSettings settings )
    {
        IPaddr gateway = new IPaddr((Inet4Address)Netcap.getGateway());

        settings.setDefaultRoute( gateway );
    }
    
    private boolean isTwoNetworkSpaces( List<InterfaceData> external, List<InterfaceData> internal )
    {
        return !external.equals( internal );
    }
}
