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

import java.io.Serializable;
import java.util.List;
import java.util.Collections;

import com.metavize.mvvm.networking.NetworkUtil;
import com.metavize.mvvm.networking.NetworkSpacesSettings;
import com.metavize.mvvm.networking.NetworkSpacesSettingsImpl;
import com.metavize.mvvm.networking.ServicesSettings;
import com.metavize.mvvm.networking.ServicesSettingsImpl;
import com.metavize.mvvm.networking.NetworkSpace;
import com.metavize.mvvm.networking.Interface;
import com.metavize.mvvm.networking.Route;
import com.metavize.mvvm.networking.DhcpLeaseRule;
import com.metavize.mvvm.networking.DnsStaticHostRule;
import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.SetupState;

import com.metavize.mvvm.tran.HostName;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.firewall.ip.IPDBMatcher;

public class NatAdvancedSettingsImpl implements NatAdvancedSettings, Serializable
{
    // !!!! private static final long serialVersionUID = 4349679825783697834L;
    private final NetworkSpacesSettings networkSpacesSettings;
    private final ServicesSettings servicesSettings;

    private final List<IPDBMatcher> localMatcherList;

    private boolean isEnabled;

    /* Use with caution */
    NatAdvancedSettingsImpl()
    {
        networkSpacesSettings = new NetworkSpacesSettingsImpl();

        /* Not the perfect fit, but it implements services settings */
        servicesSettings = new ServicesSettingsImpl();

        /* As null, this object will not be able to generate a complete getLocalMatcherList */
        this.localMatcherList = NatUtil.getInstance().getEmptyLocalMatcherList();
    }
    
    NatAdvancedSettingsImpl( NetworkSpacesSettings networkSpaces, ServicesSettings services, 
                             List<IPDBMatcher> localMatcherList )
    {
        this.networkSpacesSettings = networkSpaces;
        this.servicesSettings = services;

        if ( localMatcherList == null ) localMatcherList = NatUtil.getInstance().getEmptyLocalMatcherList();
        this.localMatcherList = Collections.unmodifiableList( localMatcherList );
    }

    public SetupState getSetupState()
    {
        return SetupState.ADVANCED;
    }
    
    /** True if network spaces is enabled */
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }
    
    /** Retrieve a list of interfaces */
    public List<Interface> getInterfaceList()
    {
        return networkSpacesSettings.getInterfaceList();
    }
    
    public void setInterfaceList( List<Interface> newValue )
    {
        networkSpacesSettings.setInterfaceList( newValue );
    }

    
    /** The list of network spaces for the box. */
    public List<NetworkSpace> getNetworkSpaceList()
    {
        return networkSpacesSettings.getNetworkSpaceList();
    }
    
    public void setNetworkSpaceList( List<NetworkSpace> newValue )
    {
        networkSpacesSettings.setNetworkSpaceList( newValue );
    }

    /** The routing table for the box. */
    public List<Route> getRoutingTable()
    {
        return networkSpacesSettings.getRoutingTable();
    }

    public void setRoutingTable( List<Route> newValue )
    {
        networkSpacesSettings.setRoutingTable( newValue );
    }

    /** List of redirects for the box */
    public List<RedirectRule> getRedirectList()
    {
        return networkSpacesSettings.getRedirectList();
    }

    public void setRedirectList( List<RedirectRule> newValue )
    {
        networkSpacesSettings.setRedirectList( newValue );
    }

    public List<RedirectRule> getGlobalRedirectList()
    {
        return NatUtil.getInstance().getGlobalRedirectList( getRedirectList());
    }
    
    public void setGlobalRedirectList( List<RedirectRule> newValue )
    {
        setRedirectList( NatUtil.getInstance().setGlobalRedirectList( getRedirectList(), newValue ));
    }

    /* All of the local redirects go at the bottom of the list */
    public List<RedirectRule> getLocalRedirectList()
    {
        return NatUtil.getInstance().getLocalRedirectList( getRedirectList());
    }
    
    public void setLocalRedirectList( List<RedirectRule> newValue )
    {
        setRedirectList( NatUtil.getInstance().setLocalRedirectList( getRedirectList(), newValue ));
    }

    /**
     * List of all of the matchers available for local redirects
     */
    public List<IPDBMatcher> getLocalMatcherList()
    {
        return this.localMatcherList;
    }

    /** IP address of the default route. */
    public IPaddr getDefaultRoute()
    {
        return networkSpacesSettings.getDefaultRoute();
    }
    
    public void setDefaultRoute( IPaddr newValue )
    {
        networkSpacesSettings.setDefaultRoute( newValue );
    }
    
    
    /** IP address of the primary dns server, may be empty (dhcp is enabled) */
    public IPaddr getDns1()
    {
        return networkSpacesSettings.getDns1();
    }

    public void setDns1( IPaddr newValue )
    {
        networkSpacesSettings.setDns1( newValue );
    }

    /** IP address of the secondary dns server, may be empty */
    public IPaddr getDns2()
    {
        return networkSpacesSettings.getDns2();
    }

    public void setDns2( IPaddr newValue )
    {
        networkSpacesSettings.setDns2( newValue );
    }

    /* Return true if there is a secondary DNS entry */
    public boolean hasDns2()
    {
        return networkSpacesSettings.hasDns2();
    }

    /* DHCP Settings */
    /**
     * Returns If DHCP is enabled.
     */
    public boolean getDhcpEnabled()
    {
        return servicesSettings.getDhcpEnabled();
    }

    public void setDhcpEnabled( boolean b )
    {
        servicesSettings.setDhcpEnabled( b );
    }

    /**
     * Get the start address of the range of addresses to server.
     */
    public IPaddr getDhcpStartAddress()
    {
        return servicesSettings.getDhcpStartAddress();
    }

    public void setDhcpStartAddress( IPaddr newValue )
    {
        servicesSettings.setDhcpStartAddress( newValue );
    }

    /**
     * Get the end address of the range of addresses to server.
     */
    public IPaddr getDhcpEndAddress()
    {
        return servicesSettings.getDhcpEndAddress();
    }

    public void setDhcpEndAddress( IPaddr newValue )
    {
        servicesSettings.setDhcpEndAddress( newValue );
    }

    /** Set the starting and end address of the dns server */
    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end )
    {
        servicesSettings.setDhcpStartAndEndAddress( start, end );
    }

    /**
     * Get the default length of the DHCP lease in seconds.
     */
    public int getDhcpLeaseTime()
    {
        return servicesSettings.getDhcpLeaseTime();
    }

    public void setDhcpLeaseTime( int newValue )
    {
        servicesSettings.setDhcpLeaseTime( newValue );
    }

    /**
     * List of the dhcp leases.
     */
    public List<DhcpLeaseRule> getDhcpLeaseList()
    {
        return servicesSettings.getDhcpLeaseList();
    }

    public void setDhcpLeaseList( List<DhcpLeaseRule> newValue )
    {
        servicesSettings.setDhcpLeaseList( newValue );
    }


    /* DNS Settings */
        /**
     * If DNS Masquerading is enabled.
     */
    public boolean getDnsEnabled()
    {
        return servicesSettings.getDnsEnabled();
    }

    public void setDnsEnabled( boolean newValue )
    {
        servicesSettings.setDnsEnabled( newValue );
    }

    /**
     * Local Domain
     */
    public HostName getDnsLocalDomain()
    {
        return servicesSettings.getDnsLocalDomain();
    }

    public void setDnsLocalDomain( HostName newValue )
    {
        servicesSettings.setDnsLocalDomain( newValue );
    }

    /**
     * List of the DNS Static Host rules.
     */
    public List<DnsStaticHostRule> getDnsStaticHostList()
    {
        return servicesSettings.getDnsStaticHostList();
    }

    public void setDnsStaticHostList( List<DnsStaticHostRule> newValue )
    {
        servicesSettings.setDnsStaticHostList( newValue );
    }

    /* Validate method */
    public void validate() throws ValidateException
    {
        /* implement me */
        NetworkUtil.getInstance().validate( this.networkSpacesSettings );
    }

    
}
