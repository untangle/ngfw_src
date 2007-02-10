/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.networking;

import java.io.Serializable;

import java.util.List;
import java.util.LinkedList;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.InterfaceAlias;

public class BasicNetworkSettings implements Serializable
{
    boolean isDhcpEnabled;
    IPaddr host;
    IPaddr netmask;
    IPaddr gateway;
    IPaddr dns1;
    IPaddr dns2;
    List<InterfaceAlias> aliasList;
    PPPoEConnectionRule pppoe;
    
    /* Get if DHCP is enabled */
    public boolean isDhcpEnabled()
    {
        return this.isDhcpEnabled;
    }
    
    /* Set if DHCP is enabled */
    public void isDhcpEnabled( boolean newValue )
    {
        this.isDhcpEnabled = newValue;
    }

    /* Get the address of the box */
    public IPaddr host()
    {
        if ( this.host == null ) this.host = NetworkUtil.EMPTY_IPADDR;
        return this.host;
    }
    
    /* Set the address of the box */
    public void host( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.host = newValue;
    }

    /* Get the netmask of the box */
    public IPaddr netmask()
    {
        if ( this.netmask == null ) this.netmask = NetworkUtil.EMPTY_IPADDR;
        return this.netmask;
    }
    
    /* Set the netmask of the box */
    public void netmask( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.netmask = newValue;
    }


    /* Get the gateway of the box */
    public IPaddr gateway()
    {
        if ( this.gateway == null ) this.gateway = NetworkUtil.EMPTY_IPADDR;
        return this.gateway;
    }
    
    /* Set the gateway of the box */
    public void gateway( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.gateway = newValue;
    }

    /* Get the dns1 of the box */
    public IPaddr dns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }
    
    /* Set the dns1 of the box */
    public void dns1( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }
    
    /* Get the dns2 of the box */
    public IPaddr dns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }
    
    /* Set the dns2 of the box */
    public void dns2( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /* Get whether or not the configuration includes a second DNS setting */
    public boolean hasDns2()
    {
        return (( this.dns2 != null ) && !this.dns2.isEmpty());
    }

    /* Get the list of aliases */
    public List<InterfaceAlias> getAliasList()
    {
        if ( this.aliasList == null ) {
            this.aliasList = new LinkedList<InterfaceAlias>();
        }

        return this.aliasList;
    }

    /* Set the list of aliases */
    public void setAliasList( List<InterfaceAlias> newValue )
    {
        this.aliasList = newValue;
    }

    /* Get the settings PPPoE settings for the external interface. */
    public PPPoEConnectionRule getPPPoESettings()
    {
        if ( this.pppoe == null ) {
            this.pppoe = new PPPoEConnectionRule();
            this.pppoe.setLive( false );
        }

        return this.pppoe;
    }
    
    /* Set the settings PPPoE settings for the external interface. */
    public void setPPPoESettings( PPPoEConnectionRule newValue )
    {
        this.pppoe = newValue;
    }

    public String toString()
    {
        /* The networking configuration could be rewritten as the composition of
         * a BasicNetworkSettingsImpl(doesn't exist yet) and a RemoteSettingsImpl.
         * this would make this function a lot shorter */
        StringBuilder sb = new StringBuilder();
        
        sb.append( "dhcp:        "   + isDhcpEnabled());
        sb.append( "\nhost:        " + host());
        sb.append( "\nnetmask:     " + netmask());
        sb.append( "\ngateway:     " + gateway());
        sb.append( "\ndns 1:       " + dns1());
        sb.append( "\ndns 2:       " + dns2());
        for ( InterfaceAlias alias : getAliasList()) sb.append( "\n alias:    " + alias );
        sb.append( "\n pppoe:      " + getPPPoESettings());
        return sb.toString();
    }
        
}
