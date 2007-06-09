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

package com.untangle.uvm.networking;

import java.util.List;

import com.untangle.uvm.node.IPaddr;

/**
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface NetworkSpacesSettings
{
    /**
     * Retrieve the setup state of network spaces.
     * (deprecated, unconfigured, basic, advanced).
     *
     * @return The current setup state.
     */
    public SetupState getSetupState();

    /**
     * Retrieve whether or not the Untangle Platform has completed the
     * setup wizard.
     *
     * @return True if the untangle has completed setup.
     */
    public boolean getHasCompletedSetup();

    /**
     * Set whether this Untangle Server has completed setup.
     *
     * @param newValue whether this Untangle Server has completed
     * setup.
     */
    public void setHasCompletedSetup( boolean newValue );

    /**
     * Retrieve whether or not the network spaces are enabled.  If
     * not, all of the interfaces are reassigned to the primary
     * network space.
     *
     * @return True iff network spaces are enabled.
     */
    public boolean getIsEnabled();    

    /**
     * Set whether or not network spaces are enabled.
     *
     * @param newValue True iff network spaces are enabled.
     */
    public void setIsEnabled( boolean newValue );

    /**
     * Retrieve a list of interfaces
     *
     * @return A list of the interfaces.
     */
    public List<Interface> getInterfaceList();

    /**
     * Set the list of interfaces
     *
     * @param newValue A list of the interfaces.
     */
    public void setInterfaceList( List<Interface> newValue );

    /**
     * Retrieve the list of network spaces for the box.
     *
     * @return The lists of network spaces.
     */
    public List<NetworkSpace> getNetworkSpaceList();

    /**
     * Set The list of network spaces for the box.
     *
     * @param newValue The lists of network spaces.
     */
    public void setNetworkSpaceList( List<NetworkSpace> newValue );

    /**
     * Retrieve the routing table for the box.
     *
     * @return The routing table.
     */
    public List<Route> getRoutingTable();

    /**
     * Set the routing table for the box.
     *
     * @param newValue The routing table.
     */
    public void setRoutingTable( List<Route> newValue );
    
    /**
     * Retrieve the list of redirects for the box
     *
     * @return Get the current list of redirects.
     */
    public List<RedirectRule> getRedirectList();

    /**
     * Set the list of redirects for the box
     *
     * @param newValue The new list of redirects.
     */
    public void setRedirectList( List<RedirectRule> newValue );

    /**
     * Get the IP address of the default route.
     *
     * @return The current default route for the untangle.
     */
    public IPaddr getDefaultRoute();

    /**
     * Set the IP address of the default route.
     *
     * @param newValue The new default route for the untangle.
     */
    public void setDefaultRoute( IPaddr newValue );

    /**
     * Get IP address of the primary dns server, may be empty (dhcp is
     * enabled)
     *
     * @return The primay DNS server.
     */
    public IPaddr getDns1();

    /**
     * Set IP address of the primary dns server, may be empty (dhcp is
     * enabled)
     *
     * @param newValue The primay DNS server.
     */
    public void setDns1( IPaddr newValue );

    /**
     * IP address of the secondary DNS server, may be empty
     *
     * @return The IP address of the secondary DNS server.
     */
    public IPaddr getDns2();

    /**
     * Set the IP address of the secondary DNS server, may be empty.
     *
     * @param newValue The IP address of the secondary DNS server.
     */
    public void setDns2( IPaddr newValue );

    /**
     * Check if the secondary DNS entry is empty. 
     *
     * @return True iff the is a secondary DNS entry
     */
    public boolean hasDns2();
}
