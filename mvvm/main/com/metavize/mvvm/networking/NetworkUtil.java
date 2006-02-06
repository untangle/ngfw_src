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

import java.net.InetAddress;
import java.net.Inet4Address;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.ParseException;

public class NetworkUtil 
{
    private static final NetworkUtil INSTANCE = new NetworkUtil();
    
    public static final IPaddr  EMPTY_IPADDR;

    /* Package protected */
    NetworkUtil()
    {
    }

    /* Get all of the interfaces in a particular network space */
    public List getInterfaceList( NetworkSettings config, NetworkSpace space )
    {
        List list = new LinkedList();
        
        for ( Interface intf : (List<Interface>)config.getInterfaceList()) {
            if ( intf.getNetworkSpace().equals( space )) list.add( intf );
        }
        
        return list;
    }

    /* Validate that a network configuration is okay */
    public void validate( NetworkSettings config ) throws ValidateException
    {
        int index = 0;
        for ( NetworkSpace space : (List<NetworkSpace>)config.getNetworkSpaceList()) {
            index++;
            if ( getInterfaceList( config, space ).size() == 0 ) {
                throw new ValidateException( "Each network space["+ index +"] must have at least one" + 
                                             " interface" );
            }

            /* If dhcp is not enabled, there must be at least one address */
            validate( space );
        }

        for ( Route route : (List<Route>)config.getRoutingTable()) validate( route );

        /* XXX Check the reverse, make sure each interface is in one of the network spaces
         * in the list */
        // throw new ValidateException( "Implement me" );

        /* XXX !!!!!!!!!!! Check to see if the serviceSpace has a primary address */
    }

    public void validate( NetworkSpace space ) throws ValidateException
    {
        boolean isDhcpEnabled = space.getIsDhcpEnabled();
        List<IPNetworkRule> networkList = (List<IPNetworkRule>)space.getNetworkList();
        
        if ( !isDhcpEnabled && (( networkList == null ) || ( networkList.size() < 1 ))) {
            throw new ValidateException( "A network space should either have at least one address,"+
                                         " or use DHCP." );
        }

        if ( isDhcpEnabled && space.getIsNatEnabled()) {
            throw new ValidateException( "A network space running NAT should not get its address" +
                                         " from a DHCP server." );
        }

        if ( space.getIsNatEnabled()) {
            if ( !space.hasPrimaryAddress()) {
                throw new ValidateException( "If NAT is enabled, A network space should have at " +
                                             "least one static unicast address " );
            }
            
            if ( space.getNatSpace() == space ) {
                throw new ValidateException( "A Network space cannot NAT to itself" );
            }

            if (( space.getNatSpace() == null ) && 
                ((space.getNatAddress() == null ) || ( space.getNatAddress().isEmpty()))) {
                throw new ValidateException( " A Network space that is running NAT must have a NAT Address" );
            }
        }

        IPaddr dmzHost = space.getDmzHost();
        if ( space.getIsDmzHostEnabled() && (( dmzHost == null ) || dmzHost.isEmpty())) {
            throw new ValidateException( "If DMZ is enabled, the DMZ host should also be set" );
        }
    }

    public void validate( Route route ) throws ValidateException
    {
        /* implement me */
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

        try {
            emptyAddr = (Inet4Address)InetAddress.getByName( "0.0.0.0" );
        } catch( Exception e ) {
            System.err.println( "this should never happen: " + e );
            emptyAddr = null;
            /* THIS SHOULD NEVER HAPPEN */
        }
        
        EMPTY_IPADDR = new IPaddr( emptyAddr );
    }

}
