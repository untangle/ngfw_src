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


public class ServicesSettingsImpl implements ServicesSettings, Serializable
{
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
    
    public boolean getDhcpEnabled()
    {
        return dhcpEnabled;
    }

    public void setDhcpEnabled( boolean b )
    {
        this.dhcpEnabled = b;
    }

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

    public int getDhcpLeaseTime()
    {
        return this.dhcpLeaseTime;
    }

    public void setDhcpLeaseTime( int time )
    {
        this.dhcpLeaseTime = time;
    }

    public List<DhcpLeaseRule> getDhcpLeaseList()
    {
        return dhcpLeaseList;
    }

    public void setDhcpLeaseList( List<DhcpLeaseRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<DhcpLeaseRule>();
        dhcpLeaseList = newValue;
    }

    public boolean getDnsEnabled()
    {
        return dnsEnabled;
    }

    public void setDnsEnabled( boolean newValue )
    {
        this.dnsEnabled = newValue;
    }

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

    public List getDnsStaticHostList()
    {
        return dnsStaticHostList;
    }

    public void setDnsStaticHostList( List<DnsStaticHostRule> newValue )
    {
        dnsStaticHostList = newValue;
    }
}
