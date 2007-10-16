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

package com.untangle.uvm.networking.internal;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.networking.DhcpServerSettings;
import com.untangle.uvm.networking.DnsServerSettings;
import com.untangle.uvm.networking.DnsStaticHostRule;
import com.untangle.uvm.networking.DhcpLeaseRule;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.ServicesSettingsImpl;

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


    private ServicesInternalSettings( boolean isEnabled,
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

    public ServicesSettingsImpl toSettings()
    {
        ServicesSettingsImpl settings = new ServicesSettingsImpl();

        /* Dhcp settings */
        settings.setDhcpEnabled( getIsDhcpEnabled());
        settings.setDhcpStartAddress( getDhcpStartAddress());
        settings.setDhcpEndAddress( getDhcpEndAddress());
        settings.setDhcpLeaseTime( getDhcpLeaseTime());
        settings.setDhcpLeaseList( getDhcpLeaseRuleList()); /* This returns a mutable copy */

        /* dns settings */
        settings.setDnsEnabled( getIsDnsEnabled());
        settings.setDnsLocalDomain( getDnsLocalDomain());
        settings.setDnsStaticHostList( getDnsStaticHostRuleList()); /* This returns a copy */

        return settings;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "is-enabled: " + getIsEnabled());
        sb.append( "\ndhcp: " + getIsDhcpEnabled());
        sb.append( "\ndhcp-range: " + getDhcpStartAddress() + "-" + getDhcpEndAddress());
        sb.append( "\ndhcp-params: " + getDefaultRoute() + "/" + getNetmask());
        for ( IPaddr dnsServer : getDnsServerList()) sb.append( "\ndns-server: " + dnsServer );
        sb.append( "\ndns: " + getIsDnsEnabled());
        sb.append( "\ndns-domain: " + getDnsLocalDomain());

        return sb.toString();
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
        makeInstance( boolean isEnabled, ServicesInternalSettings server,
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

        return new ServicesInternalSettings( isEnabled, 
                                             isDhcpEnabled, dhcpStartAddress, dhcpEndAddress,
                                             leaseTime, leaseList, isDnsEnabled, local, hostList,
                                             defaultRoute, netmask, dnsServerList, interfaceName,
                                             serviceAddress );
    }
}
