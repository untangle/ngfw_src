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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

/**
 * Combined settings for the DHCP and DNS servers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_network_services", schema="settings")
@SuppressWarnings("serial")
public class ServicesSettingsImpl implements ServicesSettings, Serializable
{

    private Long id;

    /* Is dhcp enabled */
    private boolean dhcpEnabled = false;

    /* Start of the DHCP servers dynamic range */
    private IPaddr  dhcpStartAddress;

    /* End of the DHCP servers dynamic range */
    private IPaddr  dhcpEndAddress;

    /* Length of the default DHCP lease */
    private int     dhcpLeaseTime = 0;

    /* List of DHCP leases */
    private List<DhcpLeaseRule> dhcpLeaseList = new LinkedList<DhcpLeaseRule>();

    /* Is the DNS server enabled. */
    private boolean  dnsEnabled = false;

    /* The local domain for the DNS server */
    private HostName dnsLocalDomain = HostName.getEmptyHostName();

    /* DNS Static Hosts */
    private List<DnsStaticHostRule> dnsStaticHostList = new LinkedList<DnsStaticHostRule>();

    public ServicesSettingsImpl() { }

    public ServicesSettingsImpl( DhcpServerSettings dhcp, DnsServerSettings dns )
    {
        this.dhcpEnabled       = dhcp.getDhcpEnabled();
        this.dhcpStartAddress  = dhcp.getDhcpStartAddress();
        this.dhcpEndAddress    = dhcp.getDhcpEndAddress();
        this.dhcpLeaseTime     = dhcp.getDhcpLeaseTime();
        this.dhcpLeaseList     = dhcp.getDhcpLeaseList();

        this.dnsEnabled        = dns.getDnsEnabled();
        this.dnsLocalDomain    = dns.getDnsLocalDomain();
        this.dnsStaticHostList = dns.getDnsStaticHostList();
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Returns whether or not the DHCP server is enabled.
     *
     * @return True iff the DHCP server is enabled.
     */
    @Column(name="is_dhcp_enabled", nullable=false)
    public boolean getDhcpEnabled()
    {
        return dhcpEnabled;
    }

    /**
     * Set whether or not the DHCP server is enabled.
     *
     * @param newValue True iff the DHCP server is enabled.
     */
    public void setDhcpEnabled( boolean b )
    {
        this.dhcpEnabled = b;
    }

    /**
     * Retrieve the start of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @return The start of the DHCP dynamic range.
     */
    @Column(name="dhcp_start_address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDhcpStartAddress()
    {
        if ( this.dhcpStartAddress == null ) this.dhcpStartAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpStartAddress;
    }

    /**
     * Set the start of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @param newValue The start of the DHCP dynamic range.
     */
    public void setDhcpStartAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
        this.dhcpStartAddress = address;
    }

    /**
     * Retrieve the end of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @return The end of the DHCP dynamic range.
     */
    @Column(name="dhcp_end_address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDhcpEndAddress()
    {
        if ( this.dhcpEndAddress == null ) this.dhcpEndAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpEndAddress;
    }

    /**
     * Set the end of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @param newValue The end of the DHCP dynamic range.
     */
    public void setDhcpEndAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
        this.dhcpEndAddress = address;
    }

    /**
     * Set the range of addresses the DHCP server can distribute
     * dynamically.  This should automatically swap start and end if
     * necessary.
     *
     * @param start The start of the DHCP dynamic range.
     * @param end The end of the DHCP dynamic range.
     */
    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end )
    {
        if ( start == null ) {
            setDhcpStartAddress( end );
            setDhcpEndAddress( end );
        } else if ( end == null )  {
            setDhcpStartAddress( start );
            setDhcpEndAddress( start );
        } else {
            if ( start.isGreaterThan( end )) {
                setDhcpStartAddress( end );
                setDhcpEndAddress( start );
            } else {
                setDhcpStartAddress( start );
                setDhcpEndAddress( end );
            }
        }
    }

    /**
     * Retrieve the number of seconds that a dynamic DHCP lease should
     * be valid for.
     *
     * @return The length of the lease in seconds.
     */
    @Column(name="dhcp_lease_time", nullable=false)
    public int getDhcpLeaseTime()
    {
        return this.dhcpLeaseTime;
    }


    /**
     * Set the number of seconds that a dynamic DHCP lease should be
     * valid for.
     *
     * @param newValue The length of the lease in seconds.
     */
    public void setDhcpLeaseTime( int time )
    {
        this.dhcpLeaseTime = time;
    }

    /**
     * Retrieve the current list of DHCP leases.  This includes both
     * static and dynamic leases.
     *
     * @return The current DHCP leases.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="u_dhcp_lease_list",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<DhcpLeaseRule> getDhcpLeaseList()
    {
        if (dhcpLeaseList != null) dhcpLeaseList.removeAll(java.util.Collections.singleton(null));
        return dhcpLeaseList;
    }


    /**
     * Set the current list of DHCP leases.  Dynamic DHCP leases are
     * not saved.
     *
     * @param newValue The new list of leases.
     */
    public void setDhcpLeaseList( List<DhcpLeaseRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<DhcpLeaseRule>();
        dhcpLeaseList = newValue;
    }

    /**
     * Retrieve the on/off switch for the DNS server.
     *
     * @return True iff the dns server is enabled.
     */
    @Column(name="dns_enabled", nullable=false)
    public boolean getDnsEnabled()
    {
        return dnsEnabled;
    }


    /**
     * Set if the DNS server is enabled.
     *
     * @param newValue True iff the dns server is enabled.
     */
    public void setDnsEnabled( boolean newValue )
    {
        this.dnsEnabled = newValue;
    }

    /**
     * Get the local DNS domain, this is the domain for the internal
     * private network.
     *
     * @return The local DNS domain.
     */
    @Column(name="dns_local_domain")
    @Type(type="com.untangle.uvm.type.HostNameUserType")
    public HostName getDnsLocalDomain()
    {
        if ( this.dnsLocalDomain == null ) this.dnsLocalDomain = HostName.getEmptyHostName();
        return dnsLocalDomain;
    }

    /**
     * Set the local DNS domain, this is the domain for the internal
     * private network.
     *
     * @param newValue The new local DNS domain.
     */
    public void setDnsLocalDomain( HostName newValue )
    {
        if ( newValue == null ) newValue = HostName.getEmptyHostName();
        this.dnsLocalDomain = newValue;
    }

    /**
     * Set the additional list of dns entries that should resolve.
     * The DNS server will serve entries for machines that register
     * with DHCP (The Dynamic Host List) as well as the list of
     * addresses that are in this list.
     *
     * @return The list of static DNS entries.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="u_dns_host_list",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<DnsStaticHostRule> getDnsStaticHostList()
    {
        if (dnsStaticHostList != null) dnsStaticHostList.removeAll(java.util.Collections.singleton(null));
        return dnsStaticHostList;
    }

    /**
     * Set the static list of DNS entries.
     *
     * @param newValue The new list of static DNS entries.
     */
    public void setDnsStaticHostList( List<DnsStaticHostRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<DnsStaticHostRule>();
        dnsStaticHostList = newValue;
    }
}
