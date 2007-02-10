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
package com.untangle.tran.openvpn;

import java.net.Inet4Address;
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

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.TransformException;
import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;
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
     * @throws TransformException - A group does not contain enough addresses for its clients.
     */
    void assignAddresses( VpnSettings settings ) throws TransformException
    {
        /* A mapping from a group to its list of clients */
        Map<VpnGroup,List<VpnClientBase>> groupToClientList = new HashMap<VpnGroup,List<VpnClientBase>>();

        /* Create a new list that combines the clients and the sites */
        List<VpnClientBase> clientList = new LinkedList<VpnClientBase>( settings.getClientList());
        clientList.addAll(settings.getSiteList());

        if ( settings.getGroupList().size() == 0 ) throw new TransformException( "No groups" );

        VpnGroup serverGroup = settings.getGroupList().get( 0 );

        /* Create a mapping from the group to its list of clients */

        /* Always add the first group, even if there aren't any clients in it,
         * this is where the server pulls its address from */
        groupToClientList.put( serverGroup, new LinkedList<VpnClientBase>());

        for ( VpnClientBase client : clientList ) {
            VpnGroup group = client.getGroup();
            if ( group == null ) {
                logger.error( "NULL group for client [" + client.getName() + "]" );
                continue;
            }

            /* Retrieve the group list this client belongs on */
            List<VpnClientBase> groupClientList = groupToClientList.get( group );

            /* If a list hasn't been created yet, then create one */
            if ( groupClientList == null ) {
                groupClientList = new LinkedList();
                groupToClientList.put( group, groupClientList );
            }

            /* Add this client to the list */
            groupClientList.add( client );
        }

        /* Iterate each group list assigning all of the clients IP addresses */
        final boolean isBridge = settings.isBridgeMode();

        for ( Map.Entry<VpnGroup,List<VpnClientBase>> entry  : groupToClientList.entrySet()) {
            VpnGroup group = entry.getKey();
            List<VpnClientBase> clients = entry.getValue();

            List addrs = new LinkedList();
            boolean isServerGroup = group.equals( serverGroup );

            /* Create a new ip matcher to validate all of the created addresses */
            IPMatcher matcher =
                IPMatcherFactory.getInstance().makeSubnetMatcher( group.getAddress(), group.getNetmask());

            /* Create enough addresses for all of the clients, and possible the server */
            Set<IPaddr> addressSet = createAddressSet( clients.size() + ( isServerGroup ? 1 : 0 ),
                                                       group, matcher, isBridge );

            /* Remove any duplicates in the current list */
            removeDuplicateAddresses( settings, matcher, clients, isServerGroup );

            /* Now remove all of the entries that are taken */
            removeTakenAddresses( settings, matcher, clients, addressSet, isServerGroup );

            /* Now assign the remaining address to clients that don't have addresses */
            assignRemainingClients( settings, clients, addressSet, isServerGroup );
        }

        if ( null == settings.getServerAddress()) {
            throw new TransformException( "Unable to set the server address" );
        }
    }

    private Set<IPaddr> createAddressSet( int size, VpnGroup group, IPMatcher matcher, boolean isBridge )
        throws TransformException
    {
        /* Get the base address */
        InetAddress base = IPaddr.and( group.getAddress(), group.getNetmask()).getAddr();

        byte[] addressData = base.getAddress();
        addressData[3] &= 0xFC;
        addressData[3] |= 1;

        Set<IPaddr> addressSet = new LinkedHashSet<IPaddr>();

        for (  ; size-- > 0 ; ) {
            /* Create the inet address */
            IPaddr address = getByAddress( addressData );

            /* Check to see if it is in the range */
            if ( !matcher.isMatch( address.getAddr())) {
                /* This is a configuration problem */
                logger.info( "Unable to configure clients, not enough client addresses for " +
                             group.getName());
                // throw new TransformException( "Not enough addresses to assign all clients in group " +
                // group.getName());
                break;
            }

            addressSet.add( address );

            getNextAddress( addressData, isBridge );
        }

        return addressSet;
    }

    /** These are addresses that are already assigned upon starting */
    void removeDuplicateAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClientBase> clientList,
                                   boolean assignServer )
    {
        Set<IPaddr> addressSet = new HashSet<IPaddr>();

        /* If necessary add the server address */
        if ( assignServer ) {
            IPaddr serverAddress = settings.getServerAddress();
            if (( null != serverAddress ) && matcher.isMatch( serverAddress.getAddr())) {
                addressSet.add( serverAddress );
            } else {
                settings.setServerAddress( null );
            }
        }

        /* Check to see if each client has a unique address */
        for ( VpnClientBase client : clientList ) {
            IPaddr address = client.getAddress();
            /* If the address is already isn't set, it isn't in the current address group
             * or it is already taken, then clear the address */
            if (( null == address ) || !matcher.isMatch( address.getAddr()) || !addressSet.add( address )) {
                client.setAddress( null );
            }
        }
    }

    /* Remove the addresses that are taken in the address pool to be distributed to clients */
    void removeTakenAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClientBase> clientList,
                               Set<IPaddr> addressSet, boolean assignServer )
    {
        /* First check the server address */
        if ( assignServer ) {
            IPaddr serverAddress = settings.getServerAddress();
            if (( null !=  serverAddress ) && matcher.isMatch( serverAddress.getAddr())) {
                addressSet.remove( serverAddress );
            } else {
                settings.setServerAddress( null );
            }
        }

        for ( VpnClientBase client : clientList ) {
            IPaddr address = client.getAddress();
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

    void assignRemainingClients( VpnSettings settings, List<VpnClientBase> clientList,
                                 Set<IPaddr> addressSet, boolean assignServer )
    {
        Iterator<IPaddr> iter = addressSet.iterator();

        /* If necessary assign the server an address */
        if ( assignServer && ( null == settings.getServerAddress())) {
            settings.setServerAddress( iter.next());
            iter.remove();
        }

        /* Assign each client an address */
        for ( VpnClientBase client : clientList ) {
            /* Nothing to do for this clients that current have addresses or there aren't more
             * more available addresses */
            if ( client.getAddress() != null || !iter.hasNext()) continue;

            /* Once you use the node, you must remove it from the set so it is never used again */
            client.setAddress( iter.next());
            iter.remove();
        }
    }

    void getNextAddress( byte[] current, boolean isBridge )
    {
        /* For a bridge each one increments by 1 */
        boolean overflow = false;
        if ( isBridge ) {
            current[3] += 1;

            if (( current[3] == 0 ) || ( current[3] == -127 )) {
                overflow = true;
                current[3] = 1;
            }
        } else {
            current[3] += 4;
            if ( current[3] == 1 ) overflow = true;
        }

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
    private IPaddr getByAddress( byte[] data )
    {
        try {
            return new IPaddr((Inet4Address)InetAddress.getByAddress( data ));
        } catch ( UnknownHostException e ) {
            logger.error( "Something happened, array should be 4 actually " + data.length + " bytes", e );
        }
        return null;
    }
}
