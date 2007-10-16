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
