/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.untangle.node.util.UvmUtil;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Rule;

/**
 * The configuration state for one network space.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_network_space", schema="settings")
public class NetworkSpace extends Rule
{
    /* There should be at least one */
    private List<IPNetworkRule> networkList = new LinkedList();

    /* The default MTU for a network space */
    public static final int DEFAULT_MTU = 1500;

    /* The minimum MTU */
    public static final int MIN_MTU = 100;

    /* The maximum MTU */
    public static final int MAX_MTU = 3000;

    /* The default name for a network space */
    public static final String DEFAULT_SPACE_NAME = "space";

    /* True if this is the primary network space.  The primary network
     * space is the first network space.  The external interface is
     * always in the primary network space. */
    private boolean isPrimary;

    /* This is a special piece, it should stay the same for the life
     * of a network space.  */
    private long businessPapers;

    /* True if DHCP is enabled for this network space. */
    private boolean isDhcpEnabled;

    /* True if traffic is allowed to leave or enter this network space */
    private boolean isTrafficForwarded = true;

    /* The MTU for all of the interfaces in this network space. */
    private int mtu = DEFAULT_MTU;

    /* True if traffic should be NATd */
    private boolean isNatEnabled;

    /* If non-null, NAT to whatever the primary address of this space is */
    private NetworkSpace natSpace;

    /* The address to nat traffic to */
    private IPaddr  natAddress;

    /* DMZ Host is a deprecated concept that is going away. */
    /* True if there is a DMZ Host for this space.  DMZ Host is used
     * to redirect all traffic to a specific address */
    private boolean isDmzHostEnabled = false;

    /* True to log all redirects from the DMZ Host */
    private boolean isDmzHostLoggingEnabled = false;

    /* The Host to redirect all traffic destined to this space */
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

    /* This should be set once per the life of the network space.  It
     * should remain the same throughout restarts, saves, etc.
     */
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

        return UvmUtil.eliminateNulls(this.networkList);
    }

    /**
     * Set the list of networks for this space.
     *
     * @param networkList The list of networks for this space.
     */
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

    /**
     * Set whether or not traffic is allowed to leave this network space.
     *
     * @param isTrafficForwarded whether or not traffic is allowed to
     * leave this network space.
     */
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

    /**
     * Set whether or not this network space uses DHCP.
     *
     * @param isDhcpEnabled True if DHCP should be enabled.
     */
    public void setIsDhcpEnabled( boolean isDhcpEnabled )
    {
        this.isDhcpEnabled = isDhcpEnabled;
    }

    /**
     * Is this space running NAT.
     *
     * @return True if this space should do NAT.
     */
    @Column(name="is_nat_enabled", nullable=false)
    public boolean getIsNatEnabled()
    {
        return isNatEnabled;
    }

    /**
     * Set whether or not the traffic on this space is NATd.
     *
     * @param isNatEnabled True if the traffic on this space should be
     * NATd.
     */
    public void setIsNatEnabled( boolean isNatEnabled )
    {
        this.isNatEnabled = isNatEnabled;
    }

    /**
     * Address to NAT connections to if, NAT is enabled.
     *
     * @return The address to NAT connections to.
     */
    @Column(name="nat_address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getNatAddress()
    {
        /* null tests */
        return this.natAddress;
    }

    /**
     * Address to NAT connections to if, NAT is enabled.
     *
     * @return The address to NAT connections to.
     */
    public void setNatAddress( IPaddr address )
    {
        this.natAddress = address;
    }

    /** XXX Should this have cascade="all" because this is typically
     * inside of the NetworkSettings object, which also saves the list
     * of network spaces.
     */

    /**
     * The network space to NAT to.  If this is non-null, then NAT
     * will use the primary address of the selected network space as
     * the NAT address.  This cannot point to itself.
     *
     * @return The network space to nat traffic to.
     */
    @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
    @JoinColumn(name="nat_space")
    public NetworkSpace getNatSpace()
    {
        return this.natSpace;
    }

    /**
     * Set the network space to NAT to.  If this is non-null, then NAT
     * will use the primary address of the selected network space as
     * the NAT address.  This cannot point to itself.
     *
     * @param newValue The network space to nat traffic to.
     */
    public void setNatSpace( NetworkSpace newValue )
    {
        this.natSpace = newValue;
    }

    /**
     * Is the DMZ host enabled.
     *
     * @return is this space using a DMZ host.
     */
    @Column(name="dmz_host_enabled", nullable=false)
    public boolean getIsDmzHostEnabled()
    {
        return isDmzHostEnabled;
    }

    /**
     * Set if DMZ host enabled.
     *
     * @param isDmzHostEnabled True if is this space using a DMZ host.
     */
    public void setIsDmzHostEnabled( boolean isDmzHostEnabled )
    {
        this.isDmzHostEnabled = isDmzHostEnabled;
    }

    /**
     * Address to send incoming requests to.
     *
     * @return The address to redirect all requests to.
     */
    @Column(name="dmz_host")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDmzHost()
    {
        /* null tests */
        return this.dmzHost;
    }

    /**
     * Set the address to send incoming requests to.
     *
     * @param dmzHost The address to redirect all requests to.
     */
    public void setDmzHost( IPaddr dmzHost )
    {
        this.dmzHost = dmzHost;
    }

    /**
     * Get whether to log redirects triggered by the DMZ Host rule.
     *
     * @return True if this space using a DMZ host.
     */
    @Column(name="dmz_host_logging", nullable=false)
    public boolean getIsDmzHostLoggingEnabled()
    {
        return isDmzHostLoggingEnabled;
    }

    /**
     * Set whether to log redirects triggered by the DMZ Host rule.
     *
     * @param newValue True if this space using a DMZ host.
     */
    public void setIsDmzHostLoggingEnabled( boolean newValue )
    {
        this.isDmzHostLoggingEnabled = newValue;
    }

    /**
     * Get the mtu for this network space.
     *
     * @return the mtu for this network space.
     */
    @Column(nullable=false)
    public int getMtu()
    {
        if ( this.mtu <= MIN_MTU || this.mtu >= MAX_MTU ) this.mtu = DEFAULT_MTU;

        return this.mtu;
    }

    /**
     * Set the mtu for this network space.
     *
     * @param mtu The mtu for this network space.
     */
    public void setMtu( int mtu )
    {
        if ( mtu <= MIN_MTU || mtu >= MAX_MTU ) mtu = DEFAULT_MTU;
        this.mtu = mtu;
    }

    /* The following are not stored inside of the database */

    /**
     * Retrieve the address that a DHCP server assigned to this
     * network space, null if dhcp is disabled.
     *
     * @return The current DHCP status.
     */
    @Transient
    public DhcpStatus getDhcpStatus()
    {
        if ( this.dhcpStatus == null ) this.dhcpStatus = DhcpStatus.EMPTY_STATUS;
        return this.dhcpStatus;
    }


    /**
     * Set the address a DHCP assigned to this network space
     *
     * @param dhcpStatus The DHCP status associated with this network
     * space.
     */
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

