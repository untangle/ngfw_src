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

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostName;

import com.metavize.mvvm.networking.NetworkSettings;
import com.metavize.mvvm.networking.Route;
import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.NetworkSpace;

public class NatSettingsWrapper
{
    private final NatSettings nat;
    private final NetworkSettings network;

    NatSettingsWrapper( NatSettings nat, NetworkSettings network )
    {
        this.nat = nat;
        this.network = network;
    }

    public NatSettings getNatSettings()
    {
        return this.nat;
    }

    public NetworkSettings getNetworkSettings()
    {
        return this.network;
    }

    /** Functions for accessing through to the nat settings */

    public List<RedirectRule> getRedirectList()
    {
        return (List<RedirectRule>)nat.getRedirectList();
    }

    public void setRedirectList( List<RedirectRule> redirectList )
    {
        nat.setRedirectList( redirectList );
    }

    public boolean getDhcpEnabled()
    {
        return nat.getDhcpEnabled();
    }

    public void setDhcpEnabled( boolean b )
    {
        nat.setDhcpEnabled( b );
    }

    public IPaddr getDhcpStartAddress()
    {
        return nat.getDhcpStartAddress();
    }

    public void setDhcpStartAddress( IPaddr address )
    {
        nat.setDhcpStartAddress( address );
    }

    public IPaddr getDhcpEndAddress()
    {
        return nat.getDhcpEndAddress();
    }

    public void setDhcpEndAddress( IPaddr address )
    {
        nat.setDhcpEndAddress( address );
    }

    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end )
    {
        nat.setDhcpStartAndEndAddress( start, end );
    }

    public int getDhcpLeaseTime()
    {
        return nat.getDhcpLeaseTime();
    }

    public void setDhcpLeaseTime( int time )
    {
        nat.setDhcpLeaseTime( time );
    }

    public List<DhcpLeaseRule> getDhcpLeaseList()
    {
        return nat.getDhcpLeaseList();
    }

    public void setDhcpLeaseList( List<DhcpLeaseRule> dhcpLeaseList )
    {
        nat.setDhcpLeaseList( dhcpLeaseList );
    }

    public boolean getDnsEnabled()
    {
        return nat.getDnsEnabled();
    }

    public void setDnsEnabled( boolean b )
    {
        nat.setDnsEnabled( b );
    }

    public HostName getDnsLocalDomain()
    {
        return nat.getDnsLocalDomain();
    }

    public void setDnsLocalDomain( HostName s )
    {
        nat.setDnsLocalDomain( s );
    }

    public List<DnsStaticHostRule> getDnsStaticHostList()
    {
        return (List<DnsStaticHostRule>)nat.getDnsStaticHostList();
    }

    public void setDnsStaticHostList( List<DnsStaticHostRule> s )
    {
        nat.setDnsStaticHostList( s );
    }

    /* Functions for accessing through to the network settings */    
    public List<Interface> getInterfaceList()
    {
        return (List<Interface>)network.getInterfaceList();
    }

    public void setInterfaceList( List<Interface> interfaceList )
    {
        network.setInterfaceList( interfaceList );
    }

    public List<NetworkSpace> getNetworkSpaceList()
    {
        return (List<NetworkSpace>)network.getNetworkSpaceList();
    }
    
    public void setNetworkSpaceList( List<NetworkSpace> networkSpaceList )
    {
        network.setNetworkSpaceList( networkSpaceList );
    }

    public List<Route> getRoutingTable()
    {
        return (List<Route>)network.getRoutingTable();
    }

    public void setRoutingTable( List<Route> routingTable )
    {
        network.setRoutingTable( routingTable );
    }

    public IPaddr getDefaultRoute()
    {
        return network.getDefaultRoute();
    }
    
    public void setDefaultRoute( IPaddr defaultRoute )
    {
        network.setDefaultRoute( defaultRoute );
    }

    public IPaddr getDns1() 
    {
        return network.getDns1();
    }
        
    public void setDns1( IPaddr dns1 ) 
    {
        network.setDns1( dns1 );
    }

    public IPaddr getDns2()
    {
        return network.getDns2();
    }

    public void setDns2( IPaddr dns2 )
    {
        network.setDns2( dns2 );
    }

    public String getHostname()
    {
        return network.getHostname();
    }

    public void setHostname( String hostname )
    {
        network.setHostname( hostname );
    }

    public String getPublicAddress()
    {
        return network.getPublicAddress();
    }

    public void setPublicAddress( String publicAddress )
    {
        network.setPublicAddress( publicAddress );
    }
    
    /****************** Non-hibernate utility functions */
    public boolean hasPublicAddress()
    {
        return network.hasPublicAddress();
    }

    public boolean hasDns2() 
    {
        return network.hasDns2();
    }
}
