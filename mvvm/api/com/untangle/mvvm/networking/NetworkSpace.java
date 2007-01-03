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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Rule;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

/**
 * The configuration state for one network space.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_space", schema="settings")
public class NetworkSpace extends Rule
{
    /* There should be at least one */
    private List<IPNetworkRule> networkList = new LinkedList();

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

    @Transient
    public boolean getIsPrimary()
    {
        return this.isPrimary;
    }

    public void setIsPrimary( boolean newValue )
    {
        this.isPrimary = newValue;
    }

    /**
     * Get the business papers for the space.
     *
     * @return The business papers for the space.
     */
    @Column(name="papers", nullable=false)
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
            org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="space_id")
    @IndexColumn(name="position")
    public List<IPNetworkRule> getNetworkList()
    {
        if ( this.networkList == null ) this.networkList = new LinkedList<IPNetworkRule>();

        return this.networkList;
    }

    public void setNetworkList( List<IPNetworkRule> networkList )
    {
        /* This make a copy of the list involved */
        if ( networkList == null ) {
            networkList = new LinkedList<IPNetworkRule>();
        }

        this.networkList = networkList;
    }

    /**
     * Should traffic originiating from this network space be able to leave it.
     * Turning off traffic forwarding indicates that traffic cannot leave this network
     * space, but sessions from other spaces may be able to enter it.
     * @return Whether or not traffic is forwarded.
     */
    @Column(name="is_traffic_forwarded", nullable=false)
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
     */
    @Column(name="is_dhcp_enabled", nullable=false)
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
     */
    @Column(name="is_nat_enabled", nullable=false)
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
     */
    @Column(name="nat_address")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getNatAddress()
    {
        /* null tests */
        return this.natAddress;
    }

    public void setNatAddress( IPaddr address )
    {
        this.natAddress = address;
    }

    /** XXX Should this have cascade="all" because this is typically
     * inside of the NetworkSettings object, which also saves the list
     * of network spaces.  The network space to NAT to.  If this is
     * non-null, then NAT will use the primary address of the selected
     * network space as the NAT address.  This cannot point to itself.
     *
     * @return The network space to nat traffic to.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="nat_space")
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
     */
    @Column(name="dmz_host_enabled", nullable=false)
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
     */
    @Column(name="dmz_host")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
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
     */
    @Column(name="dmz_host_logging", nullable=false)
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
     */
    @Column(nullable=false)
    public int getMtu()
    {
        if ( this.mtu <= MIN_MTU || this.mtu >= MAX_MTU ) this.mtu = DEFAULT_MTU;

        return this.mtu;
    }

    public void setMtu( int mtu )
    {
        if ( mtu <= MIN_MTU || mtu >= MAX_MTU ) mtu = DEFAULT_MTU;
        this.mtu = mtu;
    }

    /* The following are not stored inside of the database */
    /* Retrieve the address that a DHCP server assigned to this network space, null
     * if dhcp is disabled. */
    @Transient
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

