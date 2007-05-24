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
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

/**
 * Combined settings for the DHCP and DNS servers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_services", schema="settings")
public class ServicesSettingsImpl implements ServicesSettings, Serializable
{
    private static final long serialVersionUID = 7074952180633919139L;

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
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
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
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
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
    @JoinTable(name="mvvm_dhcp_lease_list",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<DhcpLeaseRule> getDhcpLeaseList()
    {
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
    @Type(type="com.untangle.mvvm.type.HostNameUserType")
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
    @JoinTable(name="mvvm_dns_host_list",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<DnsStaticHostRule> getDnsStaticHostList()
    {
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
