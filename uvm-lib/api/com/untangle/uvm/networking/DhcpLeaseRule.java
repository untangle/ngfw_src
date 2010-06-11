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

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPNullAddr;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.MACAddress;


/**
 * Rule for storing static and dynamic DHCP leases.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_dhcp_lease_rule", schema="settings")
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
    @Type(type="com.untangle.uvm.type.firewall.MACAddressUserType")
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
    @Type(type="com.untangle.uvm.type.IPNullAddrUserType")
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
