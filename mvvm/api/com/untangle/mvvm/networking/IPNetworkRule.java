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
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.Rule;
import com.untangle.mvvm.tran.firewall.ParsingConstants;
import org.hibernate.annotations.Type;



/**
 * An IPNetwork that is to go into a list, this is only to
 * allow lists of IPNetworks to be saved.  Normally, an IPNetwork is
 * just stored using the IPNetworkUserType.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_ip_network", schema="settings")
public class IPNetworkRule extends Rule
{
    private IPNetwork ipNetwork;

    public IPNetworkRule() { }

    public IPNetworkRule( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /**
     * The IPNetwork associated with this rule.
     * @return The IPNetwork associated with this rule.
     */
    @Column(name="network")
    @Type(type="com.untangle.mvvm.networking.IPNetworkUserType")
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
    @Transient
    public IPaddr getNetwork()
    {
        return this.ipNetwork.getNetwork();
    }

    @Transient
    public IPaddr getNetmask()
    {
        return this.ipNetwork.getNetmask();
    }

    @Transient
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
