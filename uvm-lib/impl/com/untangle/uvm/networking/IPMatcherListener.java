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
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;

class IPMatcherListener implements NetworkSettingsListener
{
    private final Logger logger = Logger.getLogger(getClass());

    IPMatcherListener()
    {
    }

    public void event( NetworkSpacesInternalSettings settings )
    {
        NetworkSpaceInternal internal = settings.getNetworkSpace( IntfConstants.INTERNAL_INTF );
        NetworkSpaceInternal external = settings.getNetworkSpace( IntfConstants.EXTERNAL_INTF );

        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();

        /* Load the list of networks for the internal interface */
        List<IPNetwork> networkList = new LinkedList<IPNetwork>();
        if ( internal == null ) {
            logger.warn( "Internal interface doesn't have a network space." );
        } else {
            networkList.addAll( internal.getNetworkList());
        }

        if ( networkList.size() == 0 ) {
            logger.warn( "no networks for the internal network space: " + internal );
        }
        
        for ( IPNetwork network : networkList ) logger.debug( "internal network: " + network );

        ipmf.setInternalNetworks( networkList );
        
        /* Load the list of networks for the external interface. */
        networkList.clear();
        if ( external == null ) {
            logger.warn( "External interface doesn't have a network space." );
        } else {
            networkList.addAll( external.getNetworkList());
        }
        
        InetAddress primaryAddress = NetworkUtil.BOGUS_DHCP_ADDRESS.getAddr();

        InetAddress addressArray[] = new InetAddress[networkList.size()];
                    
        for ( int c = 0 ; c < addressArray.length ; c++ ) {
            InetAddress address  =  networkList.get( c ).getNetwork().getAddr();
            addressArray[c] = address;
            logger.debug( "external network: " + address.getHostAddress());
        }

        if ( addressArray.length == 0 ) {
            logger.warn( "no networks for the external network space: " + external );
        } else {
            primaryAddress = addressArray[0];
        }


        ipmf.setLocalAddresses( primaryAddress, addressArray );
    }
}
