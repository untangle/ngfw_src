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
 * Rule for storing DHCP leases.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_dhcp_lease_rule", schema="settings")
public class DhcpLeaseRule extends Rule
{
    private MACAddress macAddress;
    private String     hostname        = "";
    private IPNullAddr currentAddress  = IPNullAddr.getNullAddr();
    private IPNullAddr staticAddress   = IPNullAddr.getNullAddr();
    private Date       endOfLease      = null;
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
     * MAC address
     *
     * @return the mac address.
     */
    @Column(name="mac_address")
    @Type(type="com.untangle.mvvm.type.firewall.MACAddressUserType")
    public MACAddress getMacAddress()
    {
        return macAddress;
    }

    public void setMacAddress( MACAddress macAddress )
    {
        this.macAddress = macAddress;
    }

    /**
     * Host name
     *
     * @return the desired/assigned host name for this machine.
     */
    public String getHostname()
    {
        if ( hostname == null )
            return "";

        return hostname;
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }

    @Transient
    public IPNullAddr getCurrentAddress()
    {
        if ( this.currentAddress == null ) return ( this.currentAddress = IPNullAddr.getNullAddr());

        return this.currentAddress;
    }

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

    public void setStaticAddress( IPNullAddr staticAddress )
    {
        this.staticAddress = staticAddress;
    }

    @Transient
    public Date getEndOfLease()
    {
        return endOfLease;
    }

    public void setEndOfLease( Date endOfLease )
    {
        this.endOfLease = endOfLease;
    }

    /**
     * Resolve by MAC
     *
     * @return true if the MAC address is used to resolve this rule.
     */
    @Column(name="is_resolve_mac", nullable=false)
    public boolean getResolvedByMac()
    {
        return resolvedByMac;
    }

    public void setResolvedByMac( boolean resolvedByMac )
    {
        this.resolvedByMac = resolvedByMac;
    }
}
