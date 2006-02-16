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

import com.metavize.mvvm.tran.IPaddr;

public interface NetworkSpacesSettings
{
    /** Retrieve the setup state of network spaces. */
    public SetupState getSetupState();

    /** Retrieve a list of interfaces */
    public List<Interface> getInterfaceList();
    
    public void setInterfaceList( List<Interface> newValue );

    /** The list of network spaces for the box. */
    public List<NetworkSpace> getNetworkSpaceList();
    
    public void setNetworkSpaceList( List<NetworkSpace> newValue );

    /** The routing table for the box. */
    public List<Route> getRoutingTable();

    public void setRoutingTable( List<Route> newValue );

    /** List of redirects for the box */
    public List<RedirectRule> getRedirectList();

    public void setRedirectList( List<RedirectRule> newValue );

    /** IP address of the default route. */
    public IPaddr getDefaultRoute();
    
    public void setDefaultRoute( IPaddr newValue );
    
    
    /** IP address of the primary dns server, may be empty (dhcp is enabled) */
    public IPaddr getDns1();

    public void setDns1( IPaddr newValue );

    /** IP address of the secondary dns server, may be empty */
    public IPaddr getDns2();

    public void setDns2( IPaddr newValue );

    /* Return true if there is a secondary DNS entry */
    public boolean hasDns2();

    /** The hostname for the box(this is the hostname that goes into certificates). */
    public String getHostname();

    public void setHostname( String newValue );

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress();

    public void setPublicAddress( String newValue );

    /* Return true if the current settings have a public address */
    public boolean hasPublicAddress();
}