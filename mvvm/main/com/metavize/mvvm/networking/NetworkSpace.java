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
import java.util.Iterator;
import java.util.Random;

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
    private List networkList = new LinkedList();

    public static final int DEFAULT_MTU = 1500;
    public static final int MIN_MTU = 100;
    public static final int MAX_MTU = 3000;
    public static final String DEFAULT_SPACE_NAME = "space";
    
    private boolean isPrimary;

    /* This is a special piece, it should stay the same for the life of a network space.  */
    private long businessPapers;
    private boolean isDhcpEnabled;
    private boolean isTrafficForwarded = true;
    
    private int mtu = DEFAULT_MTU;

    /* This is the address that traffic is NATd to */
    private boolean isNatEnabled;
    private IPaddr  natAddress;

    /* If non-null, NAT to whatever the primary address of this space is */
    private NetworkSpace natSpace;
    
    private boolean isDmzHostEnabled = false;
    private boolean isDmzHostLoggingEnabled = false;
    private IPaddr  dmzHost;

    /* The current status of the interface, only valid if DHCP is enabled */
    private DhcpStatus dhcpStatus = DhcpStatus.EMPTY_STATUS;
    
    /** Helper constructor */
    public NetworkSpace( boolean isEnabled, 
                         List networkList, boolean isDhcpEnabled, boolean isTrafficForwarded, int mtu,
                         boolean isNatEnabled, IPaddr natAddress,
                         boolean isDmzHostEnabled, boolean isDmzHostLoggingEnabled, IPaddr dmzHost )
    {
        setLive( isEnabled );
        this.setName( DEFAULT_SPACE_NAME );
        this.businessPapers          = ( new Random()).nextLong();
        this.networkList             = networkList;
        this.isDhcpEnabled           = isDhcpEnabled;
        this.isTrafficForwarded      = isTrafficForwarded;
        this.mtu                     = mtu;
        this.isNatEnabled            = isNatEnabled;
        this.natAddress              = natAddress;
        this.isDmzHostEnabled        = isDmzHostEnabled;
        this.isDmzHostLoggingEnabled = isDmzHostLoggingEnabled;
        this.dmzHost                 = dmzHost;
    }

    public NetworkSpace()
    {
        this.businessPapers = ( new Random()).nextLong();
        this.setName( DEFAULT_SPACE_NAME );
    }

    public boolean getIsPrimary()
    {
        return this.isPrimary;
    }

    public void setIsPrimary( boolean newValue )
    {
        this.isPrimary = newValue;
    }

    public long getBusinessPapers()
    {
        return this.businessPapers;
    }

    /* This should only be done once per the life of the network
     * space.  It should remain the same throughout restarts, saves,
     * etc. */
    public void setBusinessPapers( long newValue )
    {
        this.businessPapers = newValue;
    }

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
        if ( this.networkList == null ) this.networkList = new LinkedList();
            
        return this.networkList;
    }
    
    public void setNetworkList( List networkList )
    {
        /* This make a copy of the list involved */
        if ( networkList == null ) {
            networkList = new LinkedList();
        }

        this.networkList = networkList;
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
     * The network space to NAT to.  If this is non-null, then NAT will use the primary
     * address of the selected network space as the NAT address.  This cannot point
     * to itself.
     * @return The network space to nat traffic to. 
     * @hibernate.many-to-one
     * cascade="none"
     * class="com.metavize.mvvm.networking.NetworkSpace"
     * column="nat_space"
     */
    /** XXX Should this have cascade="all" because this is typically inside of the NetworkSettings object, 
     * which also saves the list of network spaces. */
    public NetworkSpace getNatSpace()
    {
        return this.natSpace;
    }
    
    public void setNatSpace( NetworkSpace newValue )
    {
        this.natSpace = newValue;
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
    public boolean getIsDmzHostLoggingEnabled()
    {
        return isDmzHostLoggingEnabled;
    }

    public void setIsDmzHostLoggingEnabled( boolean newValue )
    {
        this.isDmzHostLoggingEnabled = isDmzHostLoggingEnabled;
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
    /* Retrieve the address that a DHCP server assigned to this network space, null
     * if dhcp is disabled. */
    public DhcpStatus getDhcpStatus()
    {
        if ( this.dhcpStatus == null ) this.dhcpStatus = DhcpStatus.EMPTY_STATUS;
        return this.dhcpStatus;
    }
 
    /* Set the address a DHCP assigned to this network space */
    void setDhcpStatus( DhcpStatus dhcpStatus )
    {
        if ( dhcpStatus == null ) dhcpStatus = DhcpStatus.EMPTY_STATUS;
 
        this.dhcpStatus = dhcpStatus;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "name:        "   + getName());
        sb.append( "\nisEnabled:   " + isLive());
        sb.append( "\nnetworks:    " + getNetworkList());
        sb.append( "\ndhcp:        " + getIsDhcpEnabled());
        sb.append( "\nforwarded:   " + getIsTrafficForwarded());
        sb.append( "\nmtu:         " + getMtu()); 
        sb.append( "\nnat:         " + getIsNatEnabled());
        sb.append( "\nnat-address: " + getNatAddress());
                
        sb.append( "\ndmz-host:    " + getIsDmzHostEnabled() + " logging " + getIsDmzHostLoggingEnabled() + 
                   "["  + getDmzHost() + "]" );

        return sb.toString();
    }
}

