/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.IPaddr;

/**
 * Settings for the Nat transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_SETTINGS"
 */
public class NatSettings implements java.io.Serializable
{
    private Long id;
    private Tid tid;

    /* XXX Must be updated */
    private static final long serialVersionUID = 2664348127860496780L;

    /* Nat Settings */
    private boolean natEnabled = false;
    private IPaddr  natInternalAddress;
    private IPaddr  natInternalSubnet;

    /* DMZ settings */
    private boolean dmzEnabled;
    private IPaddr  dmzAddress;

    /* Redirect rules */
    private List    redirectList = new LinkedList();
    
    /* Is dhcp enabled */
    private boolean dhcpEnabled = false;
    private IPaddr  dhcpStartAddress;
    private IPaddr  dhcpEndAddress;
    private IPaddr  dhcpGateway;
    private IPaddr  dhcpSubnet;
    private int     dhcpLeaseTime    = 0;
    private IPaddr  dhcpNameserver1;
    private IPaddr  dhcpNameserver2;
    /* Dhcp leasess */
    private List    dhcpLeaseList = new LinkedList();
    
    /* DNS Masquerading settings */
    private boolean dnsEnabled = false;
    private String  dnsLocalDomain = "";
    
    /**
     * Hibernate constructor.
     */
    private NatSettings()
    {
    }

    /**
     * Real constructor
     */
    public NatSettings( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * unique="true"
     * not-null="true"
     */
    public Tid getTid() 
    {
        return tid;
    }
    
    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * Get whether or not nat is enabled.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="NAT_ENABLED"
     */
    public boolean getNatEnabled()
    {
	return natEnabled;
    }

    public void setNatEnabled( boolean enabled )
    {
	natEnabled = enabled;
    }
    
    /**
     * Get the base of the internal address.
     *
     * @return internal Address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="NAT_INTERNAL_ADDR"
     * sql-type="inet"
     */
    public IPaddr getNatInternalAddress()
    {
        return natInternalAddress;
    }
    
    public void setNatInternalAddress( IPaddr addr ) 
    {
        natInternalAddress = addr;
    }

    /**
     * Get the subnet of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="NAT_INTERNAL_SUBNET"
     * sql-type="inet"
     */
    public IPaddr getNatInternalSubnet()
    {
        return natInternalSubnet;
    }
    
    public void setNatInternalSubnet( IPaddr addr ) 
    {
        natInternalSubnet = addr;
    }

    /**
     * Get whether or not DMZ is being used.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="DMZ_ENABLED"
     */
    public boolean getDmzEnabled()
    {
	return dmzEnabled;
    }

    public void setDmzEnabled( boolean enabled )
    {
	dmzEnabled = enabled;
    }


    /**
     * Get the address of the dmz host
     *
     * @return dmz address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DMZ_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDmzAddress()
    {
        return dmzAddress;
    }

    public void setDmzAddress( IPaddr dmzAddress )
    {
        this.dmzAddress = dmzAddress;
    }

    /**
     * List of the redirect rules.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_NAT_REDIRECTS"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.tran.firewall.RedirectRule"
     * column="RULE_ID"
     */
    public List getRedirectList() 
    {
        return redirectList;
    }
    
    public void setRedirectList( List s ) 
    { 
        redirectList = s;
    }

    /**
     * @return If DHCP is enabled.
     *
     * @hibernate.property
     * column="DHCP_ENABLED"
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
     * Get the start address of the range of addresses to server.
     *
     * @return DHCP start address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_S_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpStartAddress()
    {
        return dhcpStartAddress;
    }

    public void setDhcpStartAddress( IPaddr address )
    {
        this.dhcpStartAddress = address;
    }

    /**
     * Get the end address of the range of addresses to server.
     *
     * @return DHCP end address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_E_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpEndAddress()
    {
        return dhcpEndAddress;
    }

    public void setDhcpEndAddress( IPaddr address )
    {
        this.dhcpEndAddress = address;
    }

    /**
     * Get the gateway address for the DHCP server, this is ignored, if NAT is enabled.
     *
     * @return DHCP gateway address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_GATEWAY_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpGateway()
    {
        return dhcpGateway;
    }

    public void setDhcpGateway( IPaddr address )
    {
        this.dhcpGateway = address;
    }

    /**
     * Get the subnet address for the DHCP server, this is ignored, if NAT is enabled.
     *
     * @return DHCP subnet address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_SUBNET_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpSubnet()
    {
        return dhcpSubnet;
    }

    public void setDhcpSubnet( IPaddr address )
    {
        this.dhcpSubnet = address;
    }
    
    /**
     * Get the default length of the DHCP lease in seconds.
     *
     * @return the length of the DHCP lease in seconds.
     * @hibernate.property
     * column="DHCP_LEASE_TIME"
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
     * Get the nameserver 1 address for the DHCP server, this is ignored, if NAT is enabled.
     *
     * @return DHCP nameserver 1 address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_NAMESERVER1_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpNameserver1()
    {
        return dhcpNameserver1;
    }

    public void setDhcpNameserver1( IPaddr address )
    {
        this.dhcpNameserver1 = address;
    }

    /**
     * Get the nameserver2 address for the DHCP server.
     *
     * @return DHCP nameserver 2 address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="DHCP_NAMESERVER2_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getDhcpNameserver2()
    {
        return dhcpNameserver2;
    }

    public void setDhcpNameserver2( IPaddr address )
    {
        this.dhcpNameserver2 = address;
    }

    /**
     * List of the dhcp leases.
     *
     * @return the list of the dhcp leases.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_DHCP_LEASES"
     * @hibernate.collection-key
     * column="SETTING_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.DhcpLeaseRule"
     * column="RULE_ID"
     */
    public List getDhcpLeaseList() 
    {
        return dhcpLeaseList;
    }
    
    public void setDhcpLeaseList( List s ) 
    { 
        dhcpLeaseList = s;
    }

    /**
     * @return If DNS Masquerading is enabled.
     *
     * @hibernate.property
     * column="DNS_ENABLED"
     */
    public boolean getDnsEnabled()
    {
        return dnsEnabled;
    }

    public void setDnsEnabled( boolean b ) 
    {
        this.dnsEnabled = b;
    }

    /**
     * @return A string for the local domain, this most likely has to be validated.
     *
     * @hibernate.property
     * column="DNS_LOCAL_DOMAIN"
     */
    public String getDnsLocalDomain()
    {
        return dnsLocalDomain;
    }

    public void setDnsLocalDomain( String s ) 
    {
        this.dnsLocalDomain = s;
    }
}
