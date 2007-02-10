/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.AddressRange;
import com.untangle.mvvm.tran.AddressValidator;
import com.untangle.mvvm.tran.ValidateException;
import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;

public class NetworkUtil 
{
    private static final NetworkUtil INSTANCE;
    
    public static final IPaddr  EMPTY_IPADDR;
    public static final IPaddr  DEF_OUTSIDE_NETWORK;
    public static final IPaddr  DEF_OUTSIDE_NETMASK;
    public static final HostName LOCAL_DOMAIN_DEFAULT;
    public static final IPaddr  BOGUS_DHCP_ADDRESS;
    public static final IPaddr  BOGUS_DHCP_NETMASK;

    public static final IPaddr DEFAULT_DHCP_START;
    public static final IPaddr DEFAULT_DHCP_END;

    public static final IPaddr DEFAULT_NAT_ADDRESS;
    public static final IPaddr DEFAULT_NAT_NETMASK;

    /* This is the address to use during setup */
    public static final IPaddr SETUP_ADDRESS;
    
    /* ??? which one */
    public static final HostName DEFAULT_HOSTNAME;
    
    public static int DEFAULT_LEASE_TIME_SEC = 4 * 60 * 60;

    public static final String DEFAULT_SPACE_NAME_PRIMARY = "public";
    public static final String DEFAULT_SPACE_NAME_NAT     = "private";

    public static final int    DEF_HTTPS_PORT = 443;

    public static final boolean DEF_IS_INSIDE_INSECURE_EN = true;
    public static final boolean DEF_IS_OUTSIDE_EN         = true;
    public static final boolean DEF_IS_OUTSIDE_RESTRICTED = false;
    public static final boolean DEF_IS_SSH_EN             = false;
    public static final boolean DEF_IS_EXCEPTION_REPORTING_EN = false;
    public static final boolean DEF_IS_TCP_WIN_EN         = false;

    public static final boolean DEF_OUTSIDE_ADMINISTRATION = false;
    public static final boolean DEF_OUTSIDE_QUARANTINE = true;
    public static final boolean DEF_OUTSIDE_REPORTING = false;
    
    private static final String PRIVATE_NETWORK_STRINGS[] = 
    {
        "192.168.0.0/16", "10.0.0.0/8", "172.16.0.0/12"
    };

    /* ports 9500 -> 9650 are redirect ports, 10000 -> 60000 are reserved for NAT */
    public static final int INTERNAL_OPEN_HTTPS_PORT = 64157;

    private static final String PUBLIC_ADDRESS_EXCEPTION = 
        "A public address is an ip address, optionally followed by a port.  (e.g. 1.2.3.4:445 or 1.2.3.4)";

    /* XXX This should be final, but there is a bug in javac that won't let it be. */
    private List<IPMatcher> privateNetworkList;

    /* Package protected so that NetworkUtilPriv can work */
    NetworkUtil()
    {
        List<IPMatcher> matchers = new LinkedList<IPMatcher>();
        IPMatcherFactory imf = IPMatcherFactory.getInstance();

        for ( String matcherString : PRIVATE_NETWORK_STRINGS ) {
            try {
                matchers.add( imf.parse( matcherString ));
            } catch ( Exception e ) {
                System.err.println( "Unable to parse: " + matcherString );
            }
        }
        
        privateNetworkList = Collections.unmodifiableList( matchers );
    }

    /* Get all of the interfaces in a particular network space */
    public List<Interface> getInterfaceList( NetworkSpacesSettings settings, NetworkSpace space )
    {
        List<Interface> list = new LinkedList<Interface>();
        
        long papers = space.getBusinessPapers();
        for ( Interface intf : settings.getInterfaceList()) {
            NetworkSpace intfSpace = intf.getNetworkSpace();
            if ( intfSpace.equals( space ) || ( intfSpace.getBusinessPapers() == papers )) list.add( intf );
        }
        
        return list;
    }

    /* Check if the network is private */
    public boolean isPrivateNetwork( IPaddr address, IPaddr netmask )
    {
        if (( address == null ) || ( netmask == null )) return false;

        IPaddr base = address.and( netmask );

        for ( IPMatcher matcher : privateNetworkList ) if ( matcher.isMatch( base.getAddr())) return true;

        return false;
    }

    /* Validate that a network configuration is okay */
    public void validate( NetworkSpacesSettings settings ) throws ValidateException
    {
        int index = 0;
        Set<String> nameSet = new HashSet<String>();

        /* Calculate if a space is live, a space is not alive if it is not mapped to. */
        for ( NetworkSpace space : settings.getNetworkSpaceList()) {
            space.setLive( true );
            /* The external interface is always mapped to the primary space */
            if ( space.getIsPrimary()) continue;
            if ( getInterfaceList( settings, space ).size() == 0 ) space.setLive( false );
        }
        
        for ( NetworkSpace space : settings.getNetworkSpaceList()) {
            if ( index == 0 && !space.isLive()) {
                throw new ValidateException( "The primary network space must be active." );
            }

            String name = space.getName().trim();
            space.setName( name );

            if ( !nameSet.add( name )) {
                throw new ValidateException( "Two network spaces cannot have the same name[" + name + "]" );
            }

            /* If dhcp is not enabled, there must be at least one address */
            validate( space );
                        
            index++;
        }
        
        if ( settings.getNetworkSpaceList().size() < 1 ) {
            throw new ValidateException( "There must be at least one network space" );
        }

        /* Have to validate that all of the next hops are reachable */
        for ( Route route : (List<Route>)settings.getRoutingTable()) validate( route );
        
        for ( Interface intf : settings.getInterfaceList()) {
            NetworkSpace space = intf.getNetworkSpace();
            if ( space == null ) {
                throw new ValidateException( "Interface " + intf.getName() + " has an empty network space" );
            }
        }

        /* XXX Check the reverse, make sure each interface is in one of the network spaces
         * in the list */
        // throw new ValidateException( "Implement me" );

        /* XXX !!!!!!!!!!! Check to see if the serviceSpace has a primary address */
    }

    public void validate( NetworkSpace space ) throws ValidateException
    {
        boolean isDhcpEnabled = space.getIsDhcpEnabled();
        List<IPNetworkRule> networkList = (List<IPNetworkRule>)space.getNetworkList();
        
        if (( space.getName() == null ) || ( space.getName().trim().length() == 0 )) {
            throw new ValidateException( "A network space should have a non-empty an empty name" );
        }
        
        if ( space.isLive()) {
            if ( !isDhcpEnabled && (( networkList == null ) || ( networkList.size() < 1 ))) {
                throw new ValidateException( "A network space should either have at least one address,"+
                                             " or use DHCP." );
            }
            
            AddressValidator av = AddressValidator.getInstance();
            int index = 0;
            for ( IPNetworkRule network : networkList ) {
                index++;
                InetAddress address = network.getNetwork().getAddr();
                if ( av.isIllegalAddress( address )) {
                    throw new ValidateException( "The network address: " + address.getHostAddress() + 
                                                 " in network space: " + space.getName() + " is not valid." );
                }
            }
                        
            if ( space.getIsNatEnabled()) {
                if ( isDhcpEnabled ) {
                    throw new ValidateException( "A network space running NAT should not get its address" +
                                                 " from a DHCP server." );
                }

                NetworkSpace natSpace = space.getNatSpace();
                if ( natSpace == null ) {
                    throw new ValidateException( "The network space '" + space.getName() +
                                                 "' running NAT must have a NAT space" );
                }

                if ( natSpace.getBusinessPapers() == space.getBusinessPapers()) {
                    throw new ValidateException( "The network space '" + space.getName() +
                                                 "' running NAT must mapped to a different space" );
                }

                if ( !natSpace.isLive() ) {
                    throw new ValidateException( "The network space '" + space.getName() +
                                                 "' running NAT must mapped to an enabled NAT space" );
                }
            }
        }

        IPaddr dmzHost = space.getDmzHost();
        if ( space.getIsDmzHostEnabled() && (( dmzHost == null ) || dmzHost.isEmpty())) {
            throw new ValidateException( "If DMZ is enabled, the DMZ host should also be set" );
        }
        
    }

    public void validate( Route route ) throws ValidateException
    {
        IPNetwork network = route.getDestination();
        /* Try to convert the netmask to CIDR notation, if it fails this isn't a valid route */
        network.getNetmask().toCidr();

        if ( route.getNextHop().isEmpty()) throw new ValidateException( "The next hop is empty" );
    }

    /** Functions for IPNetworks */
    public void validate( IPNetwork network ) throws ValidateException
    {
        /* implement me */
    }

    public boolean isUnicast( IPNetwork network )
    {
        byte[] address = network.getNetwork().getAddr().getAddress();
        
        /* Magic numbers, -127, because of unsigned bytes */
        return (( address[3] != 0 ) && ( address[3] != -127 ));
    }

    /* This is a string that is parseable by the ip command */
    public String toRouteString( IPNetwork network ) throws NetworkException
    {
        /* XXX This is kind of hokey and should be precalculated at creation time */
        IPaddr netmask = network.getNetmask();

        try {
            int cidr = netmask.toCidr();
            
            IPaddr networkAddress = network.getNetwork().and( netmask );
            /* Very important, the ip command barfs on spaces. */
            return networkAddress.toString() + "/" + cidr;
        } catch ( ParseException e ) {
            throw new NetworkException( "Unable to convert the netmask " + netmask + " into a cidr suffix" );
        }
    }
    
    public String generatePublicAddress( IPaddr publicAddress, int publicPort )
    {
        if ( publicAddress == null || publicAddress.isEmpty()) return "";
                
        if ( publicPort == DEF_HTTPS_PORT ) return publicAddress.toString();

        return publicAddress.toString() + ":" + publicPort;
    }

    /* Convert an IP Network to an AddressRange, used in validation */
    public AddressRange makeAddressRange( IPNetworkRule network )
    {
        return AddressRange.makeNetwork( network.getNetwork().getAddr(), network.getNetmask().getAddr());
    }

    // Used by UI at wizard time to provide an initial value for public hostname checkbox.
    public static boolean isHostnameLikelyPublic(String hostName)
    {
        if (!hostName.contains("."))
            return false;
        if (hostName.endsWith(".domain"))
            return false;
        return true;
    }

    public static NetworkUtil getInstance()
    {
        return INSTANCE;
    }
    
    static
    {
        IPaddr emptyAddr      = null;
        IPaddr outsideNetwork = null;
        IPaddr outsideNetmask = null;
        IPaddr bogusAddress   = null;
        IPaddr bogusNetmask   = null;
        IPaddr dhcpStart      = null;
        IPaddr dhcpEnd        = null;

        IPaddr natAddress     = null;
        IPaddr natNetmask     = null;
        
        IPaddr setupAddress = null;

        HostName h, l;

        try {
            emptyAddr      = IPaddr.parse( "0.0.0.0" );
            outsideNetwork = IPaddr.parse( "1.2.3.4" );
            outsideNetmask = IPaddr.parse( "255.255.255.0" );
            bogusAddress   = IPaddr.parse( "169.254.210.50" );
            bogusNetmask   = IPaddr.parse( "255.255.255.0" );

            dhcpStart      = IPaddr.parse( "192.168.1.100" );
            dhcpEnd        = IPaddr.parse( "192.168.1.200" );

            natAddress = IPaddr.parse( "192.168.1.1" );
            natNetmask = IPaddr.parse( "255.255.255.0" );

            setupAddress = IPaddr.parse( "192.168.1.1" );
        } catch( Exception e ) {
            System.err.println( "this should never happen: " + e );
            emptyAddr = null;
            dhcpStart = dhcpEnd = null;
            bogusAddress = bogusNetmask = null;
            natAddress = natNetmask = null;
            /* THIS SHOULD NEVER HAPPEN */
        }

        try {
            h = HostName.parse( "local.domain" );
            l = HostName.parse( "untangle.local.domain" );
        } catch ( ParseException e ) {
            /* This should never happen */
            System.err.println( "Unable to initialize LOCAL_DOMAIN_DEFAULT: " + e );
            h = null;
            l = null;
        }
        EMPTY_IPADDR        = emptyAddr;
        DEF_OUTSIDE_NETWORK = outsideNetwork;
        DEF_OUTSIDE_NETMASK = outsideNetmask;

        DEFAULT_DHCP_START  = dhcpStart;
        DEFAULT_DHCP_END    = dhcpEnd;

        BOGUS_DHCP_ADDRESS  = bogusAddress;
        BOGUS_DHCP_NETMASK  = bogusNetmask;

        DEFAULT_NAT_ADDRESS = natAddress;
        DEFAULT_NAT_NETMASK = natNetmask;
        
        SETUP_ADDRESS = setupAddress;

        LOCAL_DOMAIN_DEFAULT = h;
        DEFAULT_HOSTNAME = l;

	INSTANCE = new NetworkUtil();
    }
}
