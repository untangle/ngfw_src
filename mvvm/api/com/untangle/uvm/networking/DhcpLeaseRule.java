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

import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.IPNullAddr;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Rule;
import com.untangle.mvvm.tran.firewall.MACAddress;
import org.hibernate.annotations.Type;


/**
 * Rule for storing static and dynamic DHCP leases.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_dhcp_lease_rule", schema="settings")
public class DhcpLeaseRule extends Rule
{
    /* The MAC address associated with this lease */
    private MACAddress macAddress;

    /* The hostname (if any) used when the lease was requested */
    private String     hostname        = "";

    /* The current address that is assigned to the host with
     * <code>macAddress</code> */
    private IPNullAddr currentAddress  = IPNullAddr.getNullAddr();
    
    /* The desired address to be assigned to
     * <code>macAddress</code> */
    private IPNullAddr staticAddress   = IPNullAddr.getNullAddr();

    /* The time when the lease expires */
    private Date       endOfLease      = null;

    /* presently unused */
    private boolean    resolvedByMac   = true;

    // Constructors
    public DhcpLeaseRule() { }

    public DhcpLeaseRule(MACAddress macAddress, String hostname,
                         IPNullAddr currentAddress, IPNullAddr staticAddress,
                         Date endOfLease, boolean resolvedByMac )
    {
        this.macAddress     = macAddress;
        this.hostname       = hostname;
        this.currentAddress = currentAddress;
        this.staticAddress  = staticAddress;
        this.endOfLease     = endOfLease;
        this.resolvedByMac  = resolvedByMac;
    }

    /**
     * MAC address.
     *
     * @return the mac address.
     */
    @Column(name="mac_address")
    @Type(type="com.untangle.mvvm.type.firewall.MACAddressUserType")
    public MACAddress getMacAddress()
    {
        return macAddress;
    }

    /**
     * Set the MAC address.
     *
     * @param macAddress The new MACAddress for this lease.
     */
    public void setMacAddress( MACAddress macAddress )
    {
        this.macAddress = macAddress;
    }

    /**
     * Host name
     *
     * @return the the hostname this machine registered with.
     */
    public String getHostname()
    {
        if ( hostname == null )
            return "";

        return hostname;
    }

    /**
     * Set the hostname.
     *
     * @param hostname The the hostname this machine registered with.
     */
    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    /**
     * Retrieve the address that is currently assigned to this machine.
     *
     * @return The current assigned address.
     */
    @Transient
    public IPNullAddr getCurrentAddress()
    {
        if ( this.currentAddress == null ) return ( this.currentAddress = IPNullAddr.getNullAddr());

        return this.currentAddress;
    }

    /**
     * Set the address that is currently assigned to this machine.
     *
     * @param currentAddress The current assigned address.
     */
    public void setCurrentAddress( IPNullAddr currentAddress )
    {
        this.currentAddress = currentAddress;
    }

    /**
     * Get static IP address for this MAC address
     *
     * @return desired static address.
     */
    @Column(name="static_address")
    @Type(type="com.untangle.mvvm.type.IPNullAddrUserType")
    public IPNullAddr getStaticAddress()
    {
        if ( this.staticAddress == null ) return ( this.staticAddress = IPNullAddr.getNullAddr());

        return this.staticAddress;
    }

    /**
     * Set static IP address for this MAC address
     *
     * @param staticAddress The desired static address.
     */
    public void setStaticAddress( IPNullAddr staticAddress )
    {
        this.staticAddress = staticAddress;
    }

    /**
     * Get the time when the lease expires.
     *
     * @return when the lease expires.
     */
    @Transient
    public Date getEndOfLease()
    {
        return endOfLease;
    }

    /**
     * Set the time when the lease expires.
     *
     * @param endofLease When the lease expires.
     */
    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }

    /**
     * Resolve by MAC
     * This is presently unused.
     *
     * @return true if the MAC address is used to resolve this rule.
     * This is presently unused.
     */
    @Column(name="is_resolve_mac", nullable=false)
    public boolean getResolvedByMac()
    {
        return resolvedByMac;
    }

    /**
     * Resolve by MAC.
     * This is presently unused.
     *
     * @param resolvedByMac If the MAC address is used to resolve this rule.
     */
    public void setResolvedByMac( boolean resolvedByMac )
    {
        this.resolvedByMac = resolvedByMac;
    }
}
