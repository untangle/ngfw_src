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
import java.util.Collections;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;

/**
 * Network Configuration for the box.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_settings"
 */
public class NetworkSettings
{
    public static final String  DEF_HOSTNAME   = "edgeguard";
    public static final String  DEF_PUBLIC_ADDRESS = "";

    private Long id;
    
    private List interfaceList = Collections.emptyList();
    private List networkSpaceList = Collections.emptyList();
    private List routingTable = Collections.emptyList();
    
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns2 = NetworkUtil.EMPTY_IPADDR;
    private IPaddr defaultRoute = NetworkUtil.EMPTY_IPADDR;
    
    private String hostname = DEF_HOSTNAME;
    private String publicAddress = DEF_PUBLIC_ADDRESS;

    public NetworkSettings()
    {
    }

    /**
     * @hibernate.id
     * column="settings_id"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * The list of interfaces for the box.
     *
     * @return The list of networks in this network space.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.Interface"
     */    
    public List getInterfaceList()
    {
        if ( this.interfaceList == null ) this.interfaceList = Collections.emptyList();
        return this.interfaceList;
    }

    public void setInterfaceList( List interfaceList )
    {
        if ( this.interfaceList == null ) this.interfaceList = Collections.emptyList();
        this.interfaceList = interfaceList;
    }

    /**
     * The list of network spaces for the box.
     *
     * @return The list of network spaces for the box.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.NetworkSpace"
     */    
    public List getNetworkSpaceList()
    {
        if ( this.networkSpaceList == null ) this.networkSpaceList = Collections.emptyList();
        return this.networkSpaceList;
    }
    
    public void setNetworkSpaceList( List networkSpaceList )
    {
        if ( networkSpaceList == null ) networkSpaceList = Collections.emptyList();
        this.networkSpaceList = networkSpaceList;
    }

    /**
     * The routing table for the box.
     *
     * @return The routing table for the box.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.Route"
     */
    public List getRoutingTable()
    {
        if ( this.routingTable == null ) this.routingTable = Collections.emptyList();
        return this.routingTable;
    }

    public void setRoutingTable( List routingTable )
    {
        if ( routingTable == null ) routingTable = Collections.emptyList();
        this.routingTable = routingTable;
    }

    /**
     * IP address of the default route.
     *
     * @return address of the default route.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="default_route"
     * sql-type="inet"
     */    
    public IPaddr getDefaultRoute()
    {
        return this.defaultRoute;
    }
    
    public void setDefaultRoute( IPaddr defaultRoute )
    {
        if ( defaultRoute == null ) defaultRoute = NetworkUtil.EMPTY_IPADDR;

        this.defaultRoute = defaultRoute;
    }
    
    
    /**
     * IP address of the primary dns server.
     *
     * @return IP address of the primary dns server.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dns_1"
     * sql-type="inet"
     */ 
    public IPaddr getDns1() 
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;

        return this.dns1;
    }

    public void setDns1( IPaddr dns1 ) 
    {
        if ( dns1 == null ) dns1 = NetworkUtil.EMPTY_IPADDR;

        this.dns1 = dns1;
    }

    /**
     * IP address of the secondary dns server.
     *
     * @return IP address of the secondary dns server.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dns_2"
     * sql-type="inet"
     */ 
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;

        return this.dns2;
    }

    public void setDns2( IPaddr dns2 )
    {
        if ( dns2 == null ) dns2 = NetworkUtil.EMPTY_IPADDR;
            
        this.dns2 = dns2;
    }

    /**
     * @return the hostname for the box(this is the hostname that goes into certificates).
     *
     * @hibernate.property
     * column="hostname"
     */
    public String getHostname()
    {
        return this.hostname;
    }

    public void setHostname( String hostname )
    {
        if ( hostname == null ) hostname = DEF_HOSTNAME;
        this.hostname = hostname;
    }

    /**
     * @return the public url for the box, this is the address (may be hostname or ip address)
     * where the the box is reachable from the outside.
     *
     * @hibernate.property
     * column="public_address"
     */
    public String getPublicAddress()
    {
        return this.publicAddress;
    }

    public void setPublicAddress( String publicAddress )
    {
        if ( publicAddress == null ) publicAddress = DEF_PUBLIC_ADDRESS;
        this.publicAddress = publicAddress;
    }

    /****************** Non-hibernate utility functions */
    public boolean hasPublicAddress()
    {
        /* ??? */
        return (( this.publicAddress != null ) && ( this.publicAddress.length() > 0 ));
    }

    public boolean hasDns2() 
    {
        return ( this.dns2 != null && !this.dns2.equals( NetworkUtil.EMPTY_IPADDR ));
    }

    BasicNetworkSettings toBasicConfiguration()
    {
        return new BasicNetworkSettings( this );
    }
}
