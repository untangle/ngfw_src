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

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.metavize.mvvm.tran.Rule;

import com.metavize.mvvm.tran.IPaddr;


/**
 * The configuration state for one network space.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_space"
 */
public class NetworkSpace extends Rule
{
    /* There should be at least one */
    private List networkList = Collections.emptyList();

    private static final int DEFAULT_MTU = 1500;

    private IPNetwork primaryAddress;
    
    private boolean isDhcpEnabled;
    private boolean isTrafficForwarded = true;
    
    private int mtu = DEFAULT_MTU;
    private int index = -1;

    /* This is the address that traffic is NATd to */
    private boolean isNatEnabled;
    private IPaddr  natAddress;
    private boolean isDmzHostEnabled;
    private boolean isDmzLoggingEnabled;
    private IPaddr  dmzHost;

    /* List of interfaces in this bridge, this is only used in 
     * this package for writing the configuration.  It is not stored in the database */
    private List interfaceList = Collections.emptyList();
    
    /* This is a property based on whether or not this network space is a bridge, it is 
     * only available to this package. */
    private String deviceName;

    /**
     * The list of networks in this network space.
     *
     * @return The list of networks in this network space.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="space_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.IPNetworkRule"
     */    
    public List getNetworkList()
    {
        if ( this.networkList == null ) this.networkList = Collections.emptyList();
            
        return this.networkList;
    }
    
    public void setNetworkList( List networkList )
    {
        /* This make a copy of the list involved */
        if ( networkList == null ) {
            networkList = Collections.emptyList();
        } else {
            networkList = new LinkedList( networkList );
        }

        this.networkList = networkList;

        /* Detect if this space has a primary address, a primary address is the
         * first valid unicast address */
        this.primaryAddress = null;

        NetworkUtil nu = NetworkUtil.getInstance();

        for ( Iterator iter = this.networkList.iterator() ; iter.hasNext() ; ) {
            IPNetworkRule networkRule = (IPNetworkRule)iter.next();
            IPNetwork network = networkRule.getNetwork();
            
            if ( nu.isUnicast( network )) {
                this.primaryAddress = network;
                break;
            }
        }
    }

    /**
     * Should traffic originiating from this network space be able to leave it.
     * Turning off traffic forwarding indicates that traffic cannot leave this network
     * space, but sessions from other spaces may be able to enter it.
     * @return Whether or not traffic is forwarded.
     * @hibernate.property
     * column="is_traffic_forwarded"
     */    
    public boolean getIsTrafficForwarded()
    {
        return isTrafficForwarded;
    }

    public void setIsTrafficForwarded( boolean isTrafficForwarded )
    {
        this.isTrafficForwarded = isTrafficForwarded;
    }

    /**
     * Does this network space get its address from a DHCP server.
     * @return Does this network space get its address from a DHCP server.
     * @hibernate.property
     * column="is_dhcp_enabled"
     */
    public boolean getIsDhcpEnabled()
    {
        return isDhcpEnabled;
    }

    public void setIsDhcpEnabled( boolean isDhcpEnabled )
    {
        this.isDhcpEnabled = isDhcpEnabled;
    }

    /**
     * Is this space running NAT.
     * @return is nat running on this space.
     * @hibernate.property
     * column="is_nat_enabled"
     */
    public boolean getIsNatEnabled()
    {
        return isNatEnabled;
    }

    public void setIsNatEnabled( boolean isNatEnabled )
    {
        this.isNatEnabled = isNatEnabled;
    }

    /**
     * Address to NAT connections to if, NAT is enabled.
     *
     * @return address to NAT connections to.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="nat_address"
     * sql-type="inet"
     */
    public IPaddr getNatAddress()
    {
        /* null tests */
        return this.natAddress;
    }
    
    public void setNatAddress( IPaddr address )
    {
        this.natAddress = address;
    }

    /**
     * Is the DMZ host enabled.
     * @return is this space using a DMZ host.
     * @hibernate.property
     * column="is_dmz_host_enabled"
     */
    public boolean getIsDmzHostEnabled()
    {
        return isDmzHostEnabled;
    }

    public void setIsDmzHostEnabled( boolean isDmzHostEnabled )
    {
        this.isDmzHostEnabled = isDmzHostEnabled;
    }

    /**
     * Address to send incoming requests to.
     *
     * @return address to the address to send all requests to.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dmz_host"
     * sql-type="inet"
     */
    public IPaddr getDmzHost()
    {
        /* null tests */
        return this.dmzHost;
    }
    
    public void setDmzHost( IPaddr dmzHost )
    {
        this.dmzHost = dmzHost;
    }

    /**
     * Is the DMZ host enabled.
     * @return is this space using a DMZ host.
     * @hibernate.property
     * column="is_dmz_logging_enabled"
     */
    public boolean getIsDmzLoggingEnabled()
    {
        return isDmzLoggingEnabled;
    }

    public void setIsDmzLoggingEnabled( boolean isDmzLoggingEnabled )
    {
        this.isDmzLoggingEnabled = isDmzLoggingEnabled;
    }

    /**
     * the mtu for this network space.
     * @return the mtu for this network space.
     * @hibernate.property
     * column="mtu"
     */
    public int getMtu()
    {
        if ( this.mtu <= 0 ) this.mtu = DEFAULT_MTU;

        return this.mtu;
    }
   
    public void setMtu( int mtu )
    {
        if ( mtu <= 0 ) mtu = DEFAULT_MTU;

        this.mtu = mtu;
    }

    /* The following are not stored inside of the database */
    
    /* May be null, should check hasPrimaryAddress first */
    public IPNetwork getPrimaryAddress()
    {
        return this.primaryAddress;
    }

    public boolean hasPrimaryAddress()
    {
        return ( !isDhcpEnabled && ( this.primaryAddress != null ));
    }

    /* Local property used to link the configuration between the /etc/network/interfaces
     * file and the routing and post configuration functions */
    int getIndex() throws NetworkException
    {
        if ( this.index < 0 ) {
            throw new NetworkException( "The index on this network space is not initialized" );
        }

        return this.index;
    }

    void setIndex( int index )
    {
        this.index = index;
    }

    String getDeviceName()
    {
        return this.deviceName;
    }
    
    void setDeviceName( String deviceName )
    {
        this.deviceName = deviceName;
    }

    /* A package level function used to get the list of interfaces belonging to this network space */
    List getInterfaceList()
    {
        return this.interfaceList;
    }
    
    void setInterfaceList( List interfaceList )
    {
        this.interfaceList = interfaceList;
    }

    boolean isBridge() throws NetworkException
    {
        if ( this.interfaceList == null ) {
            throw new NetworkException( "The interface list for '" + this + "' is not initialized" );
        }

        switch( this.interfaceList.size()) {
        case 0: throw new NetworkException( "The interface list for '" + this + "' is empty" );
        case 1: return false;
        default: return true;
        }
    }
}

