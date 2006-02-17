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

import java.util.List;
import java.util.LinkedList;

import java.io.Serializable;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable; /* perhaps */

public class NetworkSpacesSettingsImpl implements NetworkSpacesSettings, Serializable
{
    private SetupState setupState = SetupState.BASIC;
    private boolean isEnabled = false;;

    private List<Interface> interfaceList = new LinkedList<Interface>();
    private List<NetworkSpace> networkSpaceList = new LinkedList<NetworkSpace>();
    private List<Route> routingTable = new LinkedList<Route>();
    private List<RedirectRule> redirectList = new LinkedList<RedirectRule>();
    
    private IPaddr defaultRoute = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns2 = NetworkUtil.EMPTY_IPADDR;

    private String hostname = "";
    private String publicAddress;

    /* This is a data class */
    public NetworkSpacesSettingsImpl()
    {
    }

    /** Get the setup state */
    public SetupState getSetupState()
    {
        return this.setupState;
    }

    public void setSetupState( SetupState newValue )
    {
        this.setupState = newValue;
    }

    /** Retrieve whether or not the settings are enabled */
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
        if ( this.interfaceList == null ) this.interfaceList = new LinkedList<Interface>();
        return this.interfaceList;
    }
    
    public void setInterfaceList( List<Interface> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<Interface>();
        this.interfaceList = newValue;
    }

    /** The list of network spaces for the box. */
    public List<NetworkSpace> getNetworkSpaceList()
    {
        if ( this.networkSpaceList == null ) this.networkSpaceList = new LinkedList<NetworkSpace>();
        return this.networkSpaceList;        
    }
    
    public void setNetworkSpaceList( List<NetworkSpace> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<NetworkSpace>();
        this.networkSpaceList = newValue;
    }

    /** The routing table for the box. */
    public List<Route> getRoutingTable()
    {
        if ( this.routingTable == null ) this.routingTable = new LinkedList<Route>();
        return this.routingTable;
    }

    public void setRoutingTable( List<Route> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<Route>();
        this.routingTable = newValue;
    }

    /** IP address of the default route. */
    public IPaddr getDefaultRoute()
    {
        if ( this.defaultRoute == null ) this.defaultRoute = NetworkUtil.EMPTY_IPADDR;
        return this.defaultRoute;
    }
    
    public void setDefaultRoute( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.defaultRoute = newValue;
    }

    /** The redirects for the box. */
    public List<RedirectRule> getRedirectList()
    {
        if ( this.redirectList == null ) this.redirectList = new LinkedList<RedirectRule>();
        return this.redirectList;
    }

    public void setRedirectList( List<RedirectRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<RedirectRule>();
        this.redirectList = newValue;
    }
    
    /** IP address of the primary dns server, may be empty (dhcp is enabled) */
    public IPaddr getDns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }

    public void setDns1( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }

    /** IP address of the secondary dns server, may be empty */
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }

    public void setDns2( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /* Return true if there is a secondary DNS entry */
    public boolean hasDns2()
    {
        return (( this.dns2 == null ) || this.dns2.isEmpty());
    }

    /** The hostname for the box(this is the hostname that goes into certificates). */
    public String getHostname()
    {
        return this.hostname;
    }

    public void setHostname( String newValue )
    {
        /* ??? empty strings, null, etc */
        this.hostname = newValue;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress()
    {
        return this.publicAddress;
    }

    public void setPublicAddress( String newValue )
    {
        this.publicAddress = newValue;
    }

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress()
    {
        return (( this.publicAddress == null ) || ( this.publicAddress.length() == 0 ));
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Network Settings\n" );
        sb.append( "setup-state: " + getSetupState());
        
        sb.append( "\nInterfaces:\n" );
        for ( Interface intf : getInterfaceList()) sb.append( intf + "\n" );
        
        sb.append( "Network Spaces:\n" );
        for ( NetworkSpace space : getNetworkSpaceList()) sb.append( space + "\n" );
    
        sb.append( "Routing table:\n" );
        for ( Route route : getRoutingTable()) sb.append( route + "\n" );
        
        sb.append( "dns1:     " + getDns1());
        sb.append( "\ndns2:     " + getDns2());
        sb.append( "\ngateway:  " + getDefaultRoute());
        sb.append( "\nhostname: " + getHostname());
        sb.append( "\npublic:   " + getPublicAddress());

        return sb.toString();
    }
}