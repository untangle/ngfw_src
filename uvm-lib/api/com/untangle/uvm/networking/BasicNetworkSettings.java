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

package com.untangle.uvm.networking;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.node.IPaddr;

/**
 * These are stripped down settings used to represent the 
 * configuration of the internet connection.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
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

    /* True if single nic mode is enabled. */
    boolean isSingleNicEnabled = false;

    /* The configuration for PPPoE */
    PPPoEConnectionRule pppoe;
    
    /**
     * Get if DHCP is enabled
     *
     * @return True iff DHCP is enabled.
     */
    public boolean getDhcpEnabled()
    {
        return this.isDhcpEnabled;
    }

    /**
     * Set if DHCP is enabled
     *
     * @param newValue True iff DHCP is enabled.
     */
    public void setDhcpEnabled( boolean newValue )
    {
        this.isDhcpEnabled = newValue;
    }

    
    /**
     * Get the primary address of the internet connection.
     *
     * @return The primary address of the internet connection.
     */
    public IPaddr getHost()
    {
        if ( this.host == null ) this.host = NetworkUtil.EMPTY_IPADDR;
        return this.host;
    }
    
    /**
     * Set the primary address of the internet connection.
     *
     * @param newValue The primary address of the internet connection.
     */
    public void setHost( IPaddr newValue )
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
    public IPaddr getNetmask()
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
    public void setNetmask( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.netmask = newValue;
    }

    /**
     * Get the gateway for the internet connection.
     *
     * @return The default gateway.
     */
    public IPaddr getGateway()
    {
        if ( this.gateway == null ) this.gateway = NetworkUtil.EMPTY_IPADDR;
        return this.gateway;
    }
    
    /**
     * Set the gateway for the internet connection.
     *
     * @param newValue The new default gateway.
     */
    public void setGateway( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.gateway = newValue;
    }


    /**
     * Get the primary dns server.
     *
     * @return The primary dns server.
     */
    public IPaddr getDns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }
    
    /**
     * Set the primary dns server.
     *
     * @param newValue The new primary dns server.
     */
    public void setDns1( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }
    
    /**
     * Get the secondary dns server.
     *
     * @return The secondary dns server.
     */
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }
    
    /**
     * Set the secondary dns server.
     *
     * @param newValue The new secondary dns server.
     */
    public void setDns2( IPaddr newValue )
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

    public boolean isSingleNicEnabled()
    {
        return this.isSingleNicEnabled;
    }

    public void setSingleNicEnabled( boolean newValue )
    {
        this.isSingleNicEnabled = newValue;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "dhcp:          "   + getDhcpEnabled());
        sb.append( "\nhost:          " + getHost());
        sb.append( "\nnetmask:       " + getNetmask());
        sb.append( "\ngateway:       " + getGateway());
        sb.append( "\ndns 1:         " + getDns1());
        sb.append( "\ndns 2:         " + getDns2());
        for ( InterfaceAlias alias : getAliasList()) sb.append( "\n alias:      " + alias );
        sb.append( "\n pppoe:        " + getPPPoESettings());
        sb.append( "\nsingle-nic-en: " + isSingleNicEnabled());

        return sb.toString();
    }
        
}
