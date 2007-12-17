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

package com.untangle.uvm.networking;

import java.net.InetAddress;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;

import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;

import com.untangle.uvm.IntfConstants;

import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;

class IPMatcherListener implements NetworkSettingsListener
{
    private final Logger logger = Logger.getLogger(getClass());

    IPMatcherListener()
    {
    }

    public void event( NetworkSpacesInternalSettings settings )
    {
        List<NetworkSpaceInternal> networkSpaceList = settings.getNetworkSpaceList();
        NetworkSpaceInternal primary = networkSpaceList.get( 0 );
        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();

        List<IPNetwork> networkList = primary.getNetworkList();

        InetAddress addressArray[] = new InetAddress[networkList.size()];

        /* Add all of the addresses to the address array */
        int c = 0;
        for ( IPNetwork network : networkList ) addressArray[c++] = network.getNetwork().getAddr();

        InetAddress primaryAddress = primary.getPrimaryAddress().getNetwork().getAddr();

        logger.debug( "Setting local address: " + primaryAddress );
        logger.debug( "Setting public address array to: " + Arrays.toString( addressArray ));

        ipmf.setLocalAddresses( primaryAddress, addressArray );

        /* Set the IP address(es) for the private matcher */
        NetworkSpaceInternal internal = primary;
        boolean isFound = false;
        for ( InterfaceInternal intf : settings.getInterfaceList()) {
            if ( intf.getArgonIntf().getArgon() == IntfConstants.INTERNAL_INTF ) {
                internal = intf.getNetworkSpace();
                isFound = true;
                break;
            }
        }

        if ( !isFound ) logger.warn( "unable to find internal interface, using primary interface" );

        List<IPNetwork> internalNetworkList = new LinkedList<IPNetwork>( internal.getNetworkList());

        /* add the private network, if it is in there twice, it doesn't matter */
        IPNetwork n =  internal.getPrimaryAddress();

        if ( n != null && n.getNetwork() != null && !n.getNetwork().isEmpty() &&
             n.getNetmask() != null && !n.getNetmask().isEmpty()) {
            internalNetworkList.add( n );
        }

        if ( internalNetworkList.size() == 0 ) {
            logger.warn( "no networks for the internal network space: " + internal );
        }

        for ( IPNetwork network : internalNetworkList ) logger.debug( "internal network: " + network );

        ipmf.setInternalNetworks( internalNetworkList );
    }
}
