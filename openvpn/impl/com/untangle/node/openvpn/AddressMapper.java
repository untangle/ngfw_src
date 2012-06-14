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
package com.untangle.node.openvpn;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPMatcher;
import org.apache.log4j.Logger;

/* Class used to assign addresses to clients */
class AddressMapper
{
    private final Logger logger = Logger.getLogger( this.getClass());

    AddressMapper()
    {
    }

    /**
     * Assign addresses to the server and all of the clients.
     * XXXX This function needs some serious whitebox testing
     * @throws Exception - A group does not contain enough addresses for its clients.
     */
    void assignAddresses( VpnSettings settings ) throws Exception
    {
        /* A mapping from a group to its list of clients */
        Map<VpnGroup,List<VpnClient>> groupToClientList = new HashMap<VpnGroup,List<VpnClient>>();

        /* Create a new list that combines the clients and the sites */
        List<VpnClient> clientList = new LinkedList<VpnClient>( settings.getClientList());
        clientList.addAll(settings.getSiteList());

        if ( settings.getGroupList().size() == 0 ) throw new Exception( "No groups" );

        VpnGroup serverGroup = settings.getGroupList().get( 0 );

        /* Create a mapping from the group to its list of clients */

        /* Always add the first group, even if there aren't any clients in it,
         * this is where the server pulls its address from */
        groupToClientList.put( serverGroup, new LinkedList<VpnClient>());
        
        Map<String,VpnGroup> groupMap = OpenVpnManager.buildGroupMap(settings);

        for ( VpnClient client : clientList ) {
            VpnGroup group = groupMap.get(client.getGroupName());
            if ( group == null ) {
                logger.error( "NULL group for client [" + client.getName() + "]" );
                continue;
            }

            /* Retrieve the group list this client belongs on */
            List<VpnClient> groupClientList = groupToClientList.get( group );

            /* If a list hasn't been created yet, then create one */
            if ( groupClientList == null ) {
                groupClientList = new LinkedList<VpnClient>();
                groupToClientList.put( group, groupClientList );
            }

            /* Add this client to the list */
            groupClientList.add( client );
        }

        for ( Map.Entry<VpnGroup,List<VpnClient>> entry  : groupToClientList.entrySet()) {
            VpnGroup group = entry.getKey();
            List<VpnClient> clients = entry.getValue();

            boolean isServerGroup = group.equals( serverGroup );

            /* Create a new ip matcher to validate all of the created addresses */
            IPMatcher matcher = IPMatcher.makeSubnetMatcher( group.getAddress(), group.getNetmask());

            /* Create enough addresses for all of the clients, and possible the server */
            Set<IPAddress> addressSet = createAddressSet( clients.size() + ( isServerGroup ? 1 : 0 ), group, matcher );

            /* Remove any duplicates in the current list */
            removeDuplicateAddresses( settings, matcher, clients, isServerGroup );

            /* Now remove all of the entries that are taken */
            removeTakenAddresses( settings, matcher, clients, addressSet, isServerGroup );

            /* Now assign the remaining address to clients that don't have addresses */
            assignRemainingClients( settings, clients, addressSet, isServerGroup );
        }

        if ( null == settings.getServerAddress()) {
            throw new Exception( "Unable to set the server address" );
        }
    }

    private Set<IPAddress> createAddressSet( int size, VpnGroup group, IPMatcher matcher )
        throws Exception
    {
        /* Get the base address */
        InetAddress base = IPAddress.and( group.getAddress(), group.getNetmask()).getAddr();

        byte[] addressData = base.getAddress();
        addressData[3] &= 0xFC;
        addressData[3] |= 1;

        Set<IPAddress> addressSet = new LinkedHashSet<IPAddress>();

        for (  ; size-- > 0 ; ) {
            /* Create the inet address */
            IPAddress address = getByAddress( addressData );

            /* Check to see if it is in the range */
            if ( !matcher.isMatch( address.getAddr())) {
                /* This is a configuration problem */
                logger.info( "Unable to configure clients, not enough client addresses for " +
                             group.getName());
                // throw new Exception( "Not enough addresses to assign all clients in group " +
                // group.getName());
                break;
            }

            addressSet.add( address );

            getNextAddress( addressData );
        }

        return addressSet;
    }

    /** These are addresses that are already assigned upon starting */
    void removeDuplicateAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClient> clientList, boolean assignServer )
    {
        Set<IPAddress> addressSet = new HashSet<IPAddress>();

        /* If necessary add the server address */
        if ( assignServer ) {
            IPAddress address = null;
            HostAddress serverAddress = settings.getServerAddress();
            if ( serverAddress != null ) address = serverAddress.getIp();
            
            if (( null != address ) && matcher.isMatch( address.getAddr())) {
                addressSet.add( address );
            } else {
                settings.setServerAddress( null );
            }
        }

        /* Check to see if each client has a unique address */
        for ( VpnClient client : clientList ) {
            IPAddress address = client.getAddress();
            /* If the address is already isn't set, it isn't in the current address group
             * or it is already taken, then clear the address */
            if (( null == address ) || !matcher.isMatch( address.getAddr()) || !addressSet.add( address )) {
                client.setAddress( null );
            }
        }
    }

    /* Remove the addresses that are taken in the address pool to be distributed to clients */
    void removeTakenAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClient> clientList, Set<IPAddress> addressSet, boolean assignServer )
    {
        /* First check the server address */
        if ( assignServer ) {
            IPAddress address = null;
            HostAddress serverAddress = settings.getServerAddress();
            if ( serverAddress != null ) address = serverAddress.getIp();

            if (( null != address ) && matcher.isMatch( address.getAddr())) {
                addressSet.remove( serverAddress.getIp());
            } else {
                settings.setServerAddress( null );
            }
        }

        for ( VpnClient client : clientList ) {
            IPAddress address = client.getAddress();
            if (( null != address ) && matcher.isMatch( address.getAddr())) {
                /* The return code doesn't really matter */
                addressSet.remove( address );
            } else {
                /* This will clear clients that currently have addresses are not in
                 * this address space */
                client.setAddress( null );
            }
        }
    }

    void assignRemainingClients( VpnSettings settings, List<VpnClient> clientList, Set<IPAddress> addressSet, boolean assignServer )
    {
        Iterator<IPAddress> iter = addressSet.iterator();

        /* If necessary assign the server an address */
        if ( assignServer && ( null == settings.getServerAddress())) {
            settings.setServerAddress( new HostAddress( iter.next()));
            iter.remove();
        }

        /* Assign each client an address */
        for ( VpnClient client : clientList ) {
            /* Nothing to do for this clients that current have addresses or there aren't more
             * more available addresses */
            if ( client.getAddress() != null || !iter.hasNext()) continue;

            /* Once you use the node, you must remove it from the set so it is never used again */
            client.setAddress( iter.next());
            iter.remove();
        }
    }

    void getNextAddress( byte[] current )
    {
        boolean overflow = false;

        current[3] += 4;
        if ( current[3] == 1 ) overflow = true;

        /* Overflow  */
        if ( overflow ) {
            current[2]++;
            if ( current[2] == 0 ) {
                current[1]++;
                if ( current[1] == 0 ) {
                    current[0]++;
                }
            }
        }
    }

    /* A safe function (exceptionless) for InetAddress.getByAddress  */
    private IPAddress getByAddress( byte[] data )
    {
        try {
            return new IPAddress(InetAddress.getByAddress( data ));
        } catch ( UnknownHostException e ) {
            logger.error( "Something happened, array should be 4 actually " + data.length + " bytes", e );
        }
        return null;
    }
}
