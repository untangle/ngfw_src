/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.untangle.uvm.networking.BasicNetworkSettings;
import com.untangle.uvm.networking.DhcpLeaseRule;
import com.untangle.uvm.networking.DnsStaticHostRule;
import com.untangle.uvm.networking.Interface;
import com.untangle.uvm.networking.NetworkSpace;
import com.untangle.uvm.networking.NetworkSpacesSettings;
import com.untangle.uvm.networking.NetworkSpacesSettingsImpl;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.networking.RedirectRule;
import com.untangle.uvm.networking.Route;
import com.untangle.uvm.networking.ServicesSettings;
import com.untangle.uvm.networking.ServicesSettingsImpl;
import com.untangle.uvm.networking.SetupState;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;

public class RouterAdvancedSettingsImpl implements RouterAdvancedSettings, Serializable
{
    private static final long serialVersionUID = -5344795278019860714L;
    private final NetworkSpacesSettings networkSpacesSettings;
    private final ServicesSettings servicesSettings;

    /* Network settings to used in validation for the DHCP settings */
    private BasicNetworkSettings networkSettings;

    private final List<IPDBMatcher> localMatcherList;

    private boolean isEnabled;

    /* Use with caution */
    RouterAdvancedSettingsImpl()
    {
        networkSpacesSettings = new NetworkSpacesSettingsImpl();

        /* Not the perfect fit, but it implements services settings */
        servicesSettings = new ServicesSettingsImpl();

        /* As null, this object will not be able to generate a complete getLocalMatcherList */
        this.localMatcherList = RouterUtil.getInstance().getEmptyLocalMatcherList();
    }

    RouterAdvancedSettingsImpl( NetworkSpacesSettings networkSpaces, ServicesSettings services,
                             List<IPDBMatcher> localMatcherList )
    {
        this.networkSpacesSettings = networkSpaces;
        this.servicesSettings = services;

        if ( localMatcherList == null ) localMatcherList = RouterUtil.getInstance().getEmptyLocalMatcherList();
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
        return RouterUtil.getInstance().getGlobalRedirectList( getRedirectList());
    }

    public void setGlobalRedirectList( List<RedirectRule> newValue )
    {
        setRedirectList( RouterUtil.getInstance().setGlobalRedirectList( getRedirectList(), newValue ));
    }

    /* All of the local redirects go at the bottom of the list */
    public List<RedirectRule> getLocalRedirectList()
    {
        return RouterUtil.getInstance().getLocalRedirectList( getRedirectList());
    }

    public void setLocalRedirectList( List<RedirectRule> newValue )
    {
        setRedirectList( RouterUtil.getInstance().setLocalRedirectList( getRedirectList(), newValue ));
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

    /**
     * Property indicating whether the user has completed the setup wizard.
     * Only false if the wizard has never finished the wizard.  If the user
     * ever hits save from inside of the standard gui, this is set to true,
     * and should never return to false;
     *
     */
    public boolean getHasCompletedSetup()
    {
        /* XXX This should really do nothing, because this should never change from true.
         * It doesn't make sense for the user to not have completed setup if they are in advanced mode. */
        return networkSpacesSettings.getHasCompletedSetup();
    }

    public void setHasCompletedSetup( boolean newValue )
    {
        /* XXX This should really do nothing, because this should never change from true.
         * It doesn't make sense for the user to not have completed setup if they are in advanced mode. */
        networkSpacesSettings.setHasCompletedSetup( newValue );
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

    /** Methods used to update the current basic network settings object.
     *  this object is only used in validation */
    public BasicNetworkSettings getNetworkSettings()
    {
        return this.networkSettings;
    }

    public void setNetworkSettings( BasicNetworkSettings networkSettings )
    {
        this.networkSettings = networkSettings;
    }
}
