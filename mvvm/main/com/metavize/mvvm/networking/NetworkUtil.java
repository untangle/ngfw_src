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

import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;

import java.net.InetAddress;
import java.net.Inet4Address;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.ParseException;

public class NetworkUtil 
{
    private static final NetworkUtil INSTANCE = new NetworkUtil();
    
    public static final IPaddr  EMPTY_IPADDR;
    public static final IPaddr  DEF_OUTSIDE_NETWORK;
    public static final IPaddr  DEF_OUTSIDE_NETMASK;
    public static final HostName LOCAL_DOMAIN_DEFAULT;

    public static final IPaddr DEFAULT_DHCP_START;
    public static final IPaddr DEFAULT_DHCP_END;

    public static final String DEFAULT_HOSTNAME      = "mv-edgeguard";
    
    public static int DEFAULT_LEASE_TIME_SEC = 4 * 60 * 60;

    public static final String DEFAULT_SPACE_NAME_PRIMARY = "public";
    public static final String DEFAULT_SPACE_NAME_NAT     = "private";

    public static final int     DEF_HTTPS_PORT = 443;
    

    /* Package protected so that NetworkUtilPriv can work */
    NetworkUtil()
    {
    }

    /* Get all of the interfaces in a particular network space */
    public List getInterfaceList( NetworkSpacesSettings settings, NetworkSpace space )
    {
        List list = new LinkedList();
        
        long papers = space.getBusinessPapers();
        for ( Interface intf : (List<Interface>)settings.getInterfaceList()) {
            NetworkSpace intfSpace = intf.getNetworkSpace();
            if ( intfSpace.equals( space ) || ( intfSpace.getBusinessPapers() == papers )) list.add( intf );
        }
        
        return list;
    }

    /* Validate that a network configuration is okay */
    public void validate( NetworkSpacesSettings settings ) throws ValidateException
    {
        int index = 0;
        Set<String> nameSet = new HashSet<String>();

        for ( NetworkSpace space : (List<NetworkSpace>)settings.getNetworkSpaceList()) {
            if ( index == 0 && !space.isLive()) {
                throw new ValidateException( "The primary network space must be active." );
            }
            
            /* Could also check if the external interface is in the first network space */
            if ( space.isLive() && getInterfaceList( settings, space ).size() == 0 ) {
                throw new ValidateException( "The space "+ space.getName() + " must have at least one" + 
                                             " interface" );
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
        
        if ( !isDhcpEnabled && (( networkList == null ) || ( networkList.size() < 1 ))) {
            throw new ValidateException( "A network space should either have at least one address,"+
                                         " or use DHCP." );
        }
        
        if ( space.isLive() && space.getIsNatEnabled()) {
             if ( isDhcpEnabled ) {
                 throw new ValidateException( "A network space running NAT should not get its address" +
                                              " from a DHCP server." );
             }

             if ( space.getNatSpace() == null ) {
                 throw new ValidateException( "The network space " + space.getName() +
                                              " running NAT must have a NAT space" );
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

    public static NetworkUtil getInstance()
    {
        return INSTANCE;
    }
    
    static
    {
        Inet4Address emptyAddr = null;
        Inet4Address outsideNetwork = null;
        Inet4Address outsideNetmask = null;

        IPaddr dhcpStart, dhcpEnd;

        HostName h;

        try {
            emptyAddr = (Inet4Address)InetAddress.getByName( "0.0.0.0" );
            outsideNetwork = (Inet4Address)InetAddress.getByName( "1.2.3.4" );
            outsideNetmask = (Inet4Address)InetAddress.getByName( "255.255.255.0" );

            dhcpStart = IPaddr.parse( "192.168.1.100" );
            dhcpEnd   = IPaddr.parse( "192.168.1.200" );
        } catch( Exception e ) {
            System.err.println( "this should never happen: " + e );
            emptyAddr = null;
            dhcpStart = dhcpEnd = null;
            /* THIS SHOULD NEVER HAPPEN */
        }

        try {
            h = HostName.parse( "local.domain" );
        } catch ( ParseException e ) {
            /* This should never happen */
            System.err.println( "Unable to initialize LOCAL_DOMAIN_DEFAULT: " + e );
            h = null;
        }
        EMPTY_IPADDR = new IPaddr( emptyAddr );
        DEF_OUTSIDE_NETWORK = new IPaddr( outsideNetwork );
        DEF_OUTSIDE_NETMASK = new IPaddr( outsideNetmask );

        DEFAULT_DHCP_START = dhcpStart;
        DEFAULT_DHCP_END = dhcpEnd;

        LOCAL_DOMAIN_DEFAULT = h;
    }
}
