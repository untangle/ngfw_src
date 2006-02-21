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

import java.io.Serializable;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.networking.internal.ServicesInternalSettings;
import com.metavize.mvvm.networking.internal.ServicesInternalSettings;
import com.metavize.mvvm.networking.internal.ServicesInternalSettings;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;

/**
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_services"
 */
public class ServicesSettingsImpl implements ServicesSettings, Serializable
{
    private Long id;

    /* Is dhcp enabled */
    private boolean dhcpEnabled = false;
    private IPaddr  dhcpStartAddress;
    private IPaddr  dhcpEndAddress;
    private int     dhcpLeaseTime = 0;

    /* Dhcp leasess */
    private List dhcpLeaseList = new LinkedList();

    /* DNS Masquerading settings */
    private boolean  dnsEnabled = false;
    private HostName dnsLocalDomain = HostName.getEmptyHostName();

    /* DNS Static Hosts */
    private List dnsStaticHostList = new LinkedList();

    public ServicesSettingsImpl()
    {
    }

    public ServicesSettingsImpl( ServicesInternalSettings internal )
    {
        this.dhcpEnabled       = internal.getIsDhcpEnabled();
        this.dhcpStartAddress  = internal.getDhcpStartAddress();
        this.dhcpEndAddress    = internal.getDhcpEndAddress();
        this.dhcpLeaseTime     = internal.getDhcpLeaseTime();        
        this.dhcpLeaseList     = internal.getDhcpLeaseRuleList();

        this.dnsEnabled        = internal.getIsDnsEnabled();
        this.dnsLocalDomain    = internal.getDnsLocalDomain();
        this.dnsStaticHostList = internal.getDnsStaticHostRuleList();
    }

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
    
    /**
     * @hibernate.id
     * column="settings_id"
     * generator-class="native"
     */
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
     * @hibernate.property
     * column="is_dhcp_enabled"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dhcp_start_address"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dhcp_end_address"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * column="dhcp_lease_time"
     */
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
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="mvvm_dhcp_lease_list"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.DhcpLeaseRule"
     * column="rule_id"
     */
    public List getDhcpLeaseList()
    {
        return dhcpLeaseList;
    }

    public void setDhcpLeaseList( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        dhcpLeaseList = newValue;
    }

    
    /**
     * @return If DNS Masquerading is enabled.
     *
     * @hibernate.property
     * column="dns_enabled"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.HostNameUserType"
     * @hibernate.column
     * name="dns_local_domain"
     */
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
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="mvvm_dns_host_list"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.DnsStaticHostRule"
     * column="rule_id"
     */
    public List getDnsStaticHostList()
    {
        return dnsStaticHostList;
    }

    public void setDnsStaticHostList( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        dnsStaticHostList = newValue;
    }
}
