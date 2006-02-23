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

package com.metavize.mvvm.networking.internal;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.networking.NetworkUtil;
import com.metavize.mvvm.networking.DhcpServerSettings;
import com.metavize.mvvm.networking.DnsServerSettings;
import com.metavize.mvvm.networking.DnsStaticHostRule;
import com.metavize.mvvm.networking.DhcpLeaseRule;



/** These are the settings for the DNS/DHCP server */
public class ServicesInternalSettings
{
    // !!!!! private static final long serialVersionUID = 4349679825783697834L;
    
    /* The global flag for whether all services are enabled */
    private final boolean isEnabled;

    /* This is the address where you can definitely reach services */
    private final IPaddr serviceAddress;

    private final boolean isDhcpEnabled;
    private final IPaddr dhcpStartAddress;
    private final IPaddr dhcpEndAddress;
    private final int dhcpLeaseTime;
    private final List<DhcpLeaseInternal> leaseList;

    /* DNS settings */
    private final boolean isDnsEnabled;
    private final HostName dnsLocalDomain;
    private final List<DnsStaticHostInternal> staticHostList;

    private final IPaddr defaultRoute;
    private final IPaddr netmask;
    private final List<IPaddr> dnsServerList;
    private final String interfaceName;


    public ServicesInternalSettings( boolean isEnabled,
                                     boolean isDhcpEnabled, IPaddr dhcpStartAddress, IPaddr dhcpEndAddress,
                                     int dhcpLeaseTime, List<DhcpLeaseInternal> leaseList, 
                                     boolean isDnsEnabled, HostName dnsLocalDomain, 
                                     List<DnsStaticHostInternal> hostList,
                                     IPaddr defaultRoute, IPaddr netmask, List<IPaddr> dnsServerList,
                                     String interfaceName, IPaddr serviceAddress )
    {
        /* Indicator for whether or not services are enabled */
        this.isEnabled         = isEnabled;
        this.serviceAddress    = serviceAddress;

        /* dhcp settings */
        this.isDhcpEnabled     = isDhcpEnabled;
        this.dhcpStartAddress  = dhcpStartAddress;
        this.dhcpEndAddress    = dhcpEndAddress;
        this.dhcpLeaseTime     = dhcpLeaseTime;
        this.leaseList         = leaseList;

        /* dns settings */
        this.isDnsEnabled      = isDnsEnabled;
        this.dnsLocalDomain    = dnsLocalDomain;
        this.staticHostList    = hostList;
        
        /* peripheral settings */
        this.defaultRoute      = defaultRoute;
        this.netmask           = netmask;
        this.dnsServerList     = Collections.unmodifiableList( new LinkedList( dnsServerList ));
        this.interfaceName     = interfaceName;
    }

    /** Return whether or not services are enabled */
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    /**
     * Returns If DHCP is enabled.
     */
    public boolean getIsDhcpEnabled()
    {
        return this.isDhcpEnabled;
    }

    /**
     * Get the start address of the range of addresses to server.
     */
    public IPaddr getDhcpStartAddress()
    {
        return this.dhcpStartAddress;
    }

    /**
     * Get the end address of the range of addresses to server.
     */
    public IPaddr getDhcpEndAddress()
    {
        return this.dhcpEndAddress;
    }

    /**
     * Get the default length of the DHCP lease in seconds.
     */
    public int getDhcpLeaseTime()
    {
        return this.dhcpLeaseTime;
    }

    /**
     * List of the dhcp leases.
     */
    public List<DhcpLeaseInternal> getDhcpLeaseList()
    {
        return this.leaseList;
    }

    /**
     * A new list of the dhcp lease rules.
     */
    public List<DhcpLeaseRule> getDhcpLeaseRuleList()
    {
        List<DhcpLeaseRule> list = new LinkedList<DhcpLeaseRule>();
        
        for ( DhcpLeaseInternal internal : getDhcpLeaseList()) list.add( internal.toRule());

        return list;
    }

    /* The following settings are calculated at save time */

    /* The default route to tell clients */
    public IPaddr getDefaultRoute()
    {
        return this.defaultRoute;
    }

    /* The netmask to tell clients */
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    /* The list of nameservers to advertise */
    public List<IPaddr> getDnsServerList()
    {
        return this.dnsServerList;
    }

    /************* DNS server settings */
    /**
     * If DNS Masquerading is enabled.
     */
    public boolean getIsDnsEnabled()
    {
        return this.isDnsEnabled;
    }

    /**
     * Local Domain
     */
    public HostName getDnsLocalDomain()
    {
        return this.dnsLocalDomain;
    }

    /**
     * List of the DNS Static Host rules.
     */
    public List<DnsStaticHostInternal> getDnsStaticHostList()
    {
        return this.staticHostList;
    }

    /**
     * A new list of the dns static hosts.
     */
    public List<DnsStaticHostRule> getDnsStaticHostRuleList()
    {
        List<DnsStaticHostRule> list = new LinkedList<DnsStaticHostRule>();
        
        for ( DnsStaticHostInternal internal : getDnsStaticHostList()) list.add( internal.toRule());

        return list;
    }

    /* The name of interface to bind to, may be null if the server shouldn't bind */
    public String getInterfaceName()
    {
        return this.interfaceName;
    }
    
    public IPaddr getServiceAddress()
    {
        return this.serviceAddress;
    }

    public static ServicesInternalSettings 
        makeInstance( boolean isEnabled, DhcpServerSettings dhcp, DnsServerSettings dns,
                      IPaddr defaultRoute, IPaddr netmask, List<IPaddr> dnsServerList,
                      String interfaceName, IPaddr serviceAddress )
    {
        boolean isDhcpEnabled   = dhcp.getDhcpEnabled();
        IPaddr dhcpStartAddress = dhcp.getDhcpStartAddress();
        IPaddr dhcpEndAddress   = dhcp.getDhcpEndAddress();
        int leaseTime           = dhcp.getDhcpLeaseTime();
        
        /* Build a new list of the internal leases */
        List<DhcpLeaseInternal> leaseList = new LinkedList<DhcpLeaseInternal>();
        for ( DhcpLeaseRule rule : dhcp.getDhcpLeaseList()) leaseList.add( new DhcpLeaseInternal( rule ));
        
        /* Build a new list of the internal host entries */
        boolean isDnsEnabled     = dns.getDnsEnabled();
        HostName local = dns.getDnsLocalDomain();
        if ( local == null || local.isEmpty()) {
            local = NetworkUtil.LOCAL_DOMAIN_DEFAULT;
        }
        
        /* Build a new list of static host mapping entries */
        List<DnsStaticHostInternal> hostList = new LinkedList<DnsStaticHostInternal>();
        for ( DnsStaticHostRule rule : dns.getDnsStaticHostList()) {
            hostList.add( new DnsStaticHostInternal( rule ));
        }
        
        if ( serviceAddress == null ) serviceAddress = NetworkUtil.BOGUS_DHCP_ADDRESS;

        return new ServicesInternalSettings( isEnabled, isDhcpEnabled, dhcpStartAddress, dhcpEndAddress,
                                             leaseTime, leaseList, isDnsEnabled, local, hostList,
                                             defaultRoute, netmask, dnsServerList, interfaceName, 
                                             serviceAddress );
    }

    public static ServicesInternalSettings 
        makeInstance( ServicesInternalSettings server,
                      IPaddr defaultRoute, IPaddr netmask, List<IPaddr> dnsServerList,
                      String interfaceName, IPaddr serviceAddress )
    {
        /* If the previous server settings are null, then return new null settings */
        if ( server == null ) return null;

        boolean isDhcpEnabled   = server.getIsDhcpEnabled();
        IPaddr dhcpStartAddress = server.getDhcpStartAddress();
        IPaddr dhcpEndAddress   = server.getDhcpEndAddress();
        int leaseTime           = server.getDhcpLeaseTime();
        
        /* Build a new list of the internal leases */
        List<DhcpLeaseInternal> leaseList = server.getDhcpLeaseList();
        
        /* Build a new list of the internal host entries */
        boolean isDnsEnabled     = server.getIsDnsEnabled();
        HostName local           = server.getDnsLocalDomain();
        if ( local == null || local.isEmpty()) {
            local = NetworkUtil.LOCAL_DOMAIN_DEFAULT;
        }
        
        /* Build a new list of static host mapping entries */
        List<DnsStaticHostInternal> hostList = server.getDnsStaticHostList();

        if ( serviceAddress == null ) serviceAddress = NetworkUtil.BOGUS_DHCP_ADDRESS;

        return new ServicesInternalSettings( server.isEnabled, 
                                             isDhcpEnabled, dhcpStartAddress, dhcpEndAddress,
                                             leaseTime, leaseList, isDnsEnabled, local, hostList,
                                             defaultRoute, netmask, dnsServerList, interfaceName,
                                             serviceAddress );
    }
}
