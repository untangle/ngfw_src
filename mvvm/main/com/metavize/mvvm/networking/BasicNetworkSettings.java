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

import com.metavize.mvvm.tran.IPaddr;

public class BasicNetworkSettings implements Serializable
{
    private final NetworkSettings completeSettings;

    /* This is the space to associate with the basic configuration
     * (typically the first space) */
    private final NetworkSpace space;

    /* This is the list of network addresses to display for this
     * configuration (must be the list from space */
    private List<IPNetworkRule> aliasList;

    private IPNetwork primaryAddress;
    
    /* Going back and forth is only allowed inside this package */
    /* space and network list are passed in so that the empty network space *
     * list can be handled by the calling function. */
    BasicNetworkSettings( NetworkSettings settings, NetworkSpace space, IPNetwork primaryAddress,
                          List<IPNetworkRule> aliasList )
    {
        this.completeSettings = settings;
        this.space = space;
        this.aliasList = aliasList;
        this.primaryAddress = primaryAddress;
    }

    /* Is dhcp enabled */
    public boolean getIsDhcpEnabled()
    {
        return space.getIsDhcpEnabled();
    }

    public void setIsDhcpEnabled( boolean isEnabled )
    {
        space.setIsDhcpEnabled( isEnabled );
    }

    public IPNetwork getPrimaryAddress()
    {
        return this.primaryAddress;
    }

    public void setPrimaryAddress( IPNetwork address )
    {
        this.primaryAddress = address;
    }

    public IPaddr getDefaultRoute() 
    {
        return this.completeSettings.getDefaultRoute();
    }

    public void setDefaultRoute( IPaddr defaultRoute ) 
    {
        this.completeSettings.setDefaultRoute( defaultRoute );
    }
    
    public IPaddr getDns1() 
    {
        return this.completeSettings.getDns1();
    }
    
    public void setDns1( IPaddr dns1 ) 
    {
        this.completeSettings.setDns1( dns1 );
    }

    public IPaddr getDns2() 
    {
        return this.completeSettings.getDns2();
    }

    public void setDns2( IPaddr dns2 ) 
    {
        this.completeSettings.setDns2( dns2 );
    }

    public boolean hasDns2() 
    {
        return this.completeSettings.hasDns2();
    }

    public DhcpStatus getDhcpStatus()
    {
        return this.space.getDhcpStatus();
    }

    public String getHostname()
    {
        return this.completeSettings.getHostname();
    }

    public void setHostname( String hostname )
    {
        this.completeSettings.setHostname( hostname );
    }

    public String getPublicAddress()
    {
        return this.completeSettings.getPublicAddress();
    }

    public void setPublicAddress( String publicAddress )
    {
        this.completeSettings.setPublicAddress( publicAddress );
    }

    public boolean hasPublicAddress()
    {
        return this.completeSettings.hasPublicAddress();
    }

    public List<IPNetworkRule> getAliasList()
    {
        return this.aliasList;
    }

    public void setAliasList( List<IPNetworkRule> aliasList )
    {
        this.aliasList = aliasList;
    }

    /* Going back and forth is only allowed inside this package */
    NetworkSettings getNetworkSettings()
    {
        return this.completeSettings;
    }

    NetworkSpace getNetworkSpace()
    {
        return this.space;
    }
}
