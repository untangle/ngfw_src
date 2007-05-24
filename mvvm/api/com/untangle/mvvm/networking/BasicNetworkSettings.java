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

/**
 * These are stripped down settings used to represent the 
 * configuration of the internet connection.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class BasicNetworkSettings implements Serializable
{
    /* True iff the internet connection is configured using DHCP */
    boolean isDhcpEnabled;
    
    /* The current primary IP address of the internet connection */
    IPaddr host;

    /* The current netmask of the internet connection */
    IPaddr netmask;
    
    /* Gateway */
    IPaddr gateway;

    /* Primary DNS server */
    IPaddr dns1;

    /* Secondary DNS server */
    IPaddr dns2;
    
    /* List of secondary addresses for the primary interface */
    List<InterfaceAlias> aliasList;

    /* The configuration for PPPoE */
    PPPoEConnectionRule pppoe;
    
    /**
     * Get if DHCP is enabled
     *
     * @return True iff DHCP is enabled.
     */
    public boolean isDhcpEnabled()
    {
        return this.isDhcpEnabled;
    }

    /**
     * Set if DHCP is enabled
     *
     * @param newValue True iff DHCP is enabled.
     */
    public void isDhcpEnabled( boolean newValue )
    {
        this.isDhcpEnabled = newValue;
    }

    
    /**
     * Get the primary address of the internet connection.
     *
     * @return The primary address of the internet connection.
     */
    public IPaddr host()
    {
        if ( this.host == null ) this.host = NetworkUtil.EMPTY_IPADDR;
        return this.host;
    }
    
    /**
     * Set the primary address of the internet connection.
     *
     * @param newValue The primary address of the internet connection.
     */
    public void host( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.host = newValue;
    }

    /**
     * Get the netmask of the primary address on the internet connection.
     *
     * @return The netmask of the primary address on the internet
     * connection.
     */
    public IPaddr netmask()
    {
        if ( this.netmask == null ) this.netmask = NetworkUtil.EMPTY_IPADDR;
        return this.netmask;
    }
    
    /**
     * Set the netmask of the primary address on the internet connection.
     *
     * @param newValue The new netmask of the primary address on the
     * internet connection.
     */
    public void netmask( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.netmask = newValue;
    }

    /**
     * Get the gateway for the internet connection.
     *
     * @return The default gateway.
     */
    public IPaddr gateway()
    {
        if ( this.gateway == null ) this.gateway = NetworkUtil.EMPTY_IPADDR;
        return this.gateway;
    }
    
    /**
     * Set the gateway for the internet connection.
     *
     * @param newValue The new default gateway.
     */
    public void gateway( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.gateway = newValue;
    }


    /**
     * Get the primary dns server.
     *
     * @return The primary dns server.
     */
    public IPaddr dns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }
    
    /**
     * Set the primary dns server.
     *
     * @param newValue The new primary dns server.
     */
    public void dns1( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }
    
    /**
     * Get the secondary dns server.
     *
     * @return The secondary dns server.
     */
    public IPaddr dns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }
    
    /**
     * Set the secondary dns server.
     *
     * @param newValue The new secondary dns server.
     */
    public void dns2( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /**
     * Determine whether or not there is a secondary dns server.
     *
     * @return True iff there is a secondary dns server.
     */
    public boolean hasDns2()
    {
        return (( this.dns2 != null ) && !this.dns2.isEmpty());
    }

    /**
     * Retrieve a list of the interface aliases.
     *
     * @return A list of the interface aliases.
     */
    public List<InterfaceAlias> getAliasList()
    {
        if ( this.aliasList == null ) {
            this.aliasList = new LinkedList<InterfaceAlias>();
        }

        return this.aliasList;
    }

    /**
     * Set the list of the interface aliases.
     *
     * @param newValue A list of the interface aliases.
     */
    public void setAliasList( List<InterfaceAlias> newValue )
    {
        this.aliasList = newValue;
    }

    /**
     * Retrieve the current PPPoE settings.
     *
     * @return The current PPPoE configuration.
     */
    public PPPoEConnectionRule getPPPoESettings()
    {
        if ( this.pppoe == null ) {
            this.pppoe = new PPPoEConnectionRule();
            this.pppoe.setLive( false );
        }

        return this.pppoe;
    }
    
    /**
     * Set the PPPoE settings.
     *
     * @param newvalue The new PPPoE configuration.
     */
    public void setPPPoESettings( PPPoEConnectionRule newValue )
    {
        this.pppoe = newValue;
    }

    public String toString()
    {
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
