/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import java.net.InetAddress;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.ParsingConstants;;

/**
 * An IPNetwork that is to go into a list, this is only to 
 * allow lists of IPNetworks to be saved.  Normally, an IPNetwork is
 * just stored using the IPNetworkUserType.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_ip_network"
 */
public class IPNetworkRule extends Rule
{
    private IPNetwork ipNetwork;

    public IPNetworkRule()
    {
    }

    public IPNetworkRule( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /**
     * The IPNetwork associated with this rule.
     * @return The IPNetwork associated with this rule.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.IPNetworkUserType"
     * @hibernate.column
     * name="network"
     */
    public IPNetwork getIPNetwork()
    {
        return this.ipNetwork;
    }

    public void setIPNetwork( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /** The following are convenience methods, an IPNetwork is immutable, so the
     *  corresponding setters do not exist */
    public IPaddr getNetwork()
    {
        return this.ipNetwork.getNetwork();
    }

    public IPaddr getNetmask()
    {
        return this.ipNetwork.getNetmask();
    }

    public boolean isUnicast()
    {
        return this.ipNetwork.isUnicast();
    }

    public String toString()
    {
        if ( this.ipNetwork == null ) return "null";
        else return this.ipNetwork.toString();
    }

    public static IPNetworkRule parse( String value ) throws ParseException
    {
        return new IPNetworkRule( IPNetwork.parse( value ));
    }

    public static List parseList( String value ) throws ParseException
    {
        List networkList = new LinkedList();
        
        /* empty list, null or throw parse exception */
        if ( value == null ) throw new ParseException( "Null list" );

        value = value.trim();

        String networkArray[] = value.split( ParsingConstants.MARKER_SEPERATOR );
        
        for ( int c = 0 ; c < networkArray.length ; c++ ) networkList.add( parse( networkArray[c] ));
        return networkList;
    }

    public static IPNetworkRule makeInstance( InetAddress network, InetAddress netmask )
    {
        return new IPNetworkRule( IPNetwork.makeInstance( network, netmask ));
    }

    public static IPNetworkRule makeInstance( IPaddr network, IPaddr netmask )
    {
        return new IPNetworkRule( IPNetwork.makeInstance( network, netmask ));
    }
}
