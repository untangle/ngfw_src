/*
 * Copyright (c) 2004, 2005 Metavize Inc.
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
import java.util.Collections;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;

import com.metavize.mvvm.networking.NetworkSettings;
import com.metavize.mvvm.networking.Route;
import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.NetworkSpace;


/**
 * This is a classed used internally by network spaces.  It is used to
 * combine the settings from the NetworkSettings and NatSettings into
 * one object
 */
class NetworkSpacesSettingsPriv
{
    /* List of spaces that are NATd */
    private final List<NetworkSpace> natdNetworkSpaceList;

    /* The space where dhcp and dns should live. */
    private final NetworkSpace serviceSpace;

    /* The network settings object */    
    private final NetworkSettings networkSettings;

    /* The NAT settings object */
    private final NatSettings natSettings;
    
    /* The nat settings object */
    NetworkSpacesSettingsPriv( List<NetworkSpace> natdNetworkSpaceList,
                               NetworkSpace serviceSpace,
                               NetworkSettings networkSettings,
                               NatSettings natSettings )
    {
        this.natdNetworkSpaceList = natdNetworkSpaceList;
        this.serviceSpace = serviceSpace;
        this.networkSettings = networkSettings;
        this.natSettings = natSettings;
    }

    NetworkSpace getServiceSpace()
    {
        return this.serviceSpace;
    }

    NetworkSettings getNetworkSettings()
    {
        return this.networkSettings;
    }

    /* XXX kind of want to make this private */
    NatSettings getNatSettings()
    {
        return this.natSettings;
    }

    List<NetworkSpace> getNatdNetworkSpaceList()
    {
        return this.natdNetworkSpaceList;
    }

    /** Functions for accessing through to the nat settings */

    List<RedirectRule> getRedirectList()
    {
        return (List<RedirectRule>)natSettings.getRedirectList();
    }

    void setRedirectList( List<RedirectRule> redirectList )
    {
        natSettings.setRedirectList( redirectList );
    }

    boolean getDhcpEnabled()
    {
        return natSettings.getDhcpEnabled();
    }

    IPaddr getDhcpStartAddress()
    {
        return natSettings.getDhcpStartAddress();
    }

    void setDhcpStartAddress( IPaddr address )
    {
        natSettings.setDhcpStartAddress( address );
    }

    IPaddr getDhcpEndAddress()
    {
        return natSettings.getDhcpEndAddress();
    }

    void setDhcpEndAddress( IPaddr address )
    {
        natSettings.setDhcpEndAddress( address );
    }

    void setDhcpStartAndEndAddress( IPaddr start, IPaddr end )
    {
        natSettings.setDhcpStartAndEndAddress( start, end );
    }

    int getDhcpLeaseTime()
    {
        return natSettings.getDhcpLeaseTime();
    }

    void setDhcpLeaseTime( int time )
    {
        natSettings.setDhcpLeaseTime( time );
    }

    List<DhcpLeaseRule> getDhcpLeaseList()
    {
        return natSettings.getDhcpLeaseList();
    }

    void setDhcpLeaseList( List<DhcpLeaseRule> dhcpLeaseList )
    {
        natSettings.setDhcpLeaseList( dhcpLeaseList );
    }

    boolean getDnsEnabled()
    {
        return natSettings.getDnsEnabled();
    }

    void setDnsEnabled( boolean b )
    {
        natSettings.setDnsEnabled( b );
    }

    HostName getDnsLocalDomain()
    {
        return natSettings.getDnsLocalDomain();
    }

    void setDnsLocalDomain( HostName s )
    {
        natSettings.setDnsLocalDomain( s );
    }

    List<DnsStaticHostRule> getDnsStaticHostList()
    {
        return (List<DnsStaticHostRule>)natSettings.getDnsStaticHostList();
    }

    void setDnsStaticHostList( List<DnsStaticHostRule> s )
    {
        natSettings.setDnsStaticHostList( s );
    }

    /* Functions for accessing through to the network settings */    
    List<Interface> getInterfaceList()
    {
        return (List<Interface>)networkSettings.getInterfaceList();
    }

    List<NetworkSpace> getNetworkSpaceList()
    {
        return (List<NetworkSpace>)networkSettings.getNetworkSpaceList();
    }

    List<Route> getRoutingTable()
    {
        return (List<Route>)networkSettings.getRoutingTable();
    }

    IPaddr getDefaultRoute()
    {
        return networkSettings.getDefaultRoute();
    }

    IPaddr getDns1() 
    {
        return networkSettings.getDns1();
    }

    IPaddr getDns2()
    {
        return networkSettings.getDns2();
    }

    String getHostname()
    {
        return networkSettings.getHostname();
    }

    void setHostname( String hostname )
    {
        networkSettings.setHostname( hostname );
    }

    String getPublicAddress()
    {
        return networkSettings.getPublicAddress();
    }

    void setPublicAddress( String publicAddress )
    {
        networkSettings.setPublicAddress( publicAddress );
    }

    /* Once it gets here, there is no setting the setup state */
    SetupState getSetupState()
    {
        return this.natSettings.getSetupState();
    }
}