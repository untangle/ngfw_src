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

import java.util.List;

import com.untangle.mvvm.tran.IPaddr;

public interface NetworkSpacesSettings
{
    /** Retrieve the setup state of network spaces. */
    public SetupState getSetupState();

    /** Retrieve whether or not the settings are enabled */
    public boolean getIsEnabled();

    /** Retrieve whether or not the Untangle Platform has completed the setup wizard */
    public boolean getHasCompletedSetup();

    /** Set whether this Untangle Server has completed setup */
    public void setHasCompletedSetup( boolean newValue );

    public void setIsEnabled( boolean newValue );

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
}
