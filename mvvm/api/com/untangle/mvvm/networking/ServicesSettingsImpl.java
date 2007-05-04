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
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_services", schema="settings")
public class ServicesSettingsImpl implements ServicesSettings, Serializable
{

    // !!! serialver

    private Long id;

    /* Is dhcp enabled */
    private boolean dhcpEnabled = false;
    private IPaddr  dhcpStartAddress;
    private IPaddr  dhcpEndAddress;
    private int     dhcpLeaseTime = 0;

    /* Dhcp leasess */
    private List<DhcpLeaseRule> dhcpLeaseList = new LinkedList<DhcpLeaseRule>();

    /* DNS Masquerading settings */
    private boolean  dnsEnabled = false;
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
     * Get whether or not dhcp is enabled..
     *
     * @return is DHCP enabled.
     */
    @Column(name="is_dhcp_enabled", nullable=false)
    public boolean getDhcpEnabled()
    {
        return dhcpEnabled;
    }

    public void setDhcpEnabled( boolean b )
    {
        this.dhcpEnabled = b;
    }

    /**
     * DHCP server start address.
     *
     * @return DHCP server start address.
     */
    @Column(name="dhcp_start_address")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getDhcpStartAddress()
    {
        if ( this.dhcpStartAddress == null ) this.dhcpStartAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpStartAddress;
    }

    public void setDhcpStartAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
        this.dhcpStartAddress = address;
    }

    /**
     * DHCP server end address.
     *
     * @return DHCP server end address.
     */
    @Column(name="dhcp_end_address")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getDhcpEndAddress()
    {
        if ( this.dhcpEndAddress == null ) this.dhcpEndAddress = NetworkUtil.EMPTY_IPADDR;
        return dhcpEndAddress;
    }

    public void setDhcpEndAddress( IPaddr address )
    {
        if ( address == null ) address = NetworkUtil.EMPTY_IPADDR;
        this.dhcpEndAddress = address;
    }

    /** Set the starting and end address of the dns server */
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
     * Get the default DHCP lease time.
     *
     * @return default DHCP lease.
     */
    @Column(name="dhcp_lease_time", nullable=false)
    public int getDhcpLeaseTime()
    {
        return this.dhcpLeaseTime;
    }

    public void setDhcpLeaseTime( int time )
    {
        this.dhcpLeaseTime = time;
    }

    /**
     * List of the dhcp leases.
     *
     * @return the list of the dhcp leases.
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

    public void setDhcpLeaseList( List<DhcpLeaseRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<DhcpLeaseRule>();
        dhcpLeaseList = newValue;
    }


    /**
     * @return If DNS Masquerading is enabled.
     */
    @Column(name="dns_enabled", nullable=false)
    public boolean getDnsEnabled()
    {
        return dnsEnabled;
    }

    public void setDnsEnabled( boolean newValue )
    {
        this.dnsEnabled = newValue;
    }

    /**
     * Local Domain
     *
     * @return the local domain
     */
    @Column(name="dns_local_domain")
    @Type(type="com.untangle.mvvm.type.HostNameUserType")
    public HostName getDnsLocalDomain()
    {
        if ( this.dnsLocalDomain == null ) this.dnsLocalDomain = HostName.getEmptyHostName();
        return dnsLocalDomain;
    }

    public void setDnsLocalDomain( HostName newValue )
    {
        if ( newValue == null ) newValue = HostName.getEmptyHostName();
        this.dnsLocalDomain = newValue;
    }

    /**
     * List of the DNS Static Host rules.
     *
     * @return the list of the DNS Static Host rules.
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

    public void setDnsStaticHostList( List<DnsStaticHostRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<DnsStaticHostRule>();
        dnsStaticHostList = newValue;
    }
}
