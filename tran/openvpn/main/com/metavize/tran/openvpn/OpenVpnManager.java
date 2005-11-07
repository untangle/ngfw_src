/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.openvpn;

import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Iterator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.HashSet;
import java.util.LinkedHashSet;

import java.net.InetAddress;
import java.net.Inet4Address;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ScriptWriter;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.IPMatcher;

import static com.metavize.tran.openvpn.Constants.*;

class OpenVpnManager
{
    private static final String VPN_CONF_DIR     = "/etc/openvpn";
    private static final String VPN_SERVER_FILE  = VPN_CONF_DIR + "/server.conf";
    private static final String VPN_CCD_DIR      = VPN_CONF_DIR + "/ccd";
    
    /* Most likely want to bind to the outside address when using NAT */
    private static final String FLAG_LOCAL       = "local";
    private static final String FLAG_PORT        = "port";
    private static final int    DEFAULT_PORT     = 1194;
    private static final String FLAG_PROTOCOL    = "proto";
    private static final String DEFAULT_PROTOCOL = "udp";
    private static final String FLAG_DEVICE      = "dev";
    private static final String DEVICE_BRIDGE    = "tap";
    private static final String DEVICE_ROUTING   = "tun";
    
    private static final String FLAG_ROUTE        = "route";
    private static final String FLAG_IFCONFIG     = "ifconfig";
    private static final String FLAG_CLI_IFCONFIG = "ifconfig-push";
    private static final String FLAG_CLI_ROUTE    = "iroute";
    private static final String FLAG_BRIDGE_GROUP = "server-bridge";

    private static final String FLAG_PUSH         = "push";
    private static final String FLAG_EXPOSE_CLI   = "client-to-client";

    private static final String FLAG_MAX_CLI     = "max-clients";
    
    /* Ping every x seconds */
    private static final int DEFAULT_PING_TIME      = 10;
    
    /* If a ping response isn't received in this amount time, assume the connection is dead */
    private static final int DEFAULT_PING_TIMEOUT   = 120;

    /* Default verbosity in the log messages(0-9) *
     * 0 -- No output except fatal errors.
     * 1 to 4 -- Normal usage range. 
     * 5  --  Output  R  and W characters to the console for each packet read and write, uppercase is
     * used for TCP/UDP packets and lowercase is used for TUN/TAP packets.
     * 6 to 11 -- Debug info range (see errlevel.h for additional information on debug levels). */
    private static final int DEFAULT_VERBOSITY   = 3;

    /* XXX Just pick one that is unused (this is openvpn + 1) */
    private static final int MANAGEMENT_PORT     = 1195;

    /* Key management directives */
    private static final String DEFAULTS[] = new String[] {
        "mode server",
        "ca   keys/ca.crt",
        "cert keys/server.crt",
        "key  keys/server.key",
        "dh   keys/dh.pem",
        // XXX This is only valid if you specify a pool
        // "ifconfig-pool-persist ipp.txt",
        "client-config-dir ccd",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        /* XXXXXXXXX Need to select a valid cipher to use */
        "cipher none",
        "user nobody",
        "group nogroup",
        
        /* Only talk to clients with a client configuration file */
        /* XXX This may need to go away to support pools */
        "ccd-exclusive",
        "tls-server",
        "comp-lzo",

        /* XXX Be careful, restarts that change the key will not take this into account */
        "persist-key",
        "persist-tun",

        "status openvpn-status.log",
        "verb " + DEFAULT_VERBOSITY,

        /* Stop logging repeated messages (after 20). */
        "mute 20",

        /* Allow management from localhost */
        "management 127.0.0.1 " + MANAGEMENT_PORT
    };

    private final Logger logger = Logger.getLogger( this.getClass());

    OpenVpnManager()
    {
    }
       
    void start()
    {
        
    }
    
    void restart() throws TransformException
    {
        stop();
        start();
    }

    void stop()
    {
        
    }
    
    void configure( VpnSettings settings ) throws TransformException
    {
        writeSettings( settings );

        writeClientFiles( settings );
    }
    
    void writeSettings( VpnSettings settings ) throws TransformException
    {
        ScriptWriter sw = new VpnScriptWriter();
        
        /* Insert all of the default parameters */
        sw.appendLines( DEFAULTS );

        /* Bridging or routing */
        if ( settings.isBridgeMode()) {            
            sw.appendVariable( FLAG_DEVICE, DEVICE_BRIDGE );
            
        } else {
            sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );
            IPaddr localEndpoint  = settings.getServerAddress();
            IPaddr remoteEndpoint = getRemoteEndpoint( localEndpoint );
            
            sw.appendVariable( FLAG_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );
            writePushRoute( sw, localEndpoint, null );

            /* Get all of the routes for all of the different groups */
            writeGroups( sw, settings );
            
            /* Export the inside address if necessary */
            
            /* VPN configuratoins needs information from the networking settings. */
            ArgonManager argonManager = MvvmContextFactory.context().argonManager();
            
            writeExports( sw, settings,
                          argonManager.getInsideAddress(), argonManager.getInsideNetmask(),
                          argonManager.getOutsideAddress(), argonManager.getOutsideNetmask());
        }
        
        int maxClients = settings.getMaxClients();
        if ( maxClients > 0 ) sw.appendVariable( FLAG_MAX_CLI, String.valueOf( maxClients ));       
        
        
        sw.writeFile( VPN_SERVER_FILE );
    }

    private void writeExports( ScriptWriter sw, VpnSettings settings, 
                               InetAddress internalAddress, InetAddress internalNetmask,
                               InetAddress externalAddress, InetAddress externalNetmask )
    {
        if ( internalAddress.equals( externalAddress )) {
            /* If either is exported, export it */
            if ( settings.getIsInternalExported() || settings.getIsInternalExported()) {
                writePushRoute( sw, internalAddress, internalNetmask );
            }
        } else {
            /* Export the inside if requested */
            if ( settings.getIsInternalExported()) writePushRoute( sw, internalAddress, internalNetmask );
            
            /* Export the outside address if necessary */
            if ( settings.getIsExternalExported()) writePushRoute( sw, externalAddress, externalNetmask );
        }
        
        /* XXX This needs additional entries in the routing table,
         * because the edgeguard must also know how to route this
         * traffic */
        for ( SiteNetwork siteNetwork : (List<SiteNetwork>)settings.getExportedAddressList()) {
            writePushRoute( sw, siteNetwork.getNetwork(), siteNetwork.getNetmask());
        }
        
        /* Each client configuration is written at a separate time */
        for ( VpnClient client : (List<VpnClient>)settings.getClientList()) {
            for ( SiteNetwork siteNetwork : (List<SiteNetwork>)client.getExportedAddressList()) {
                IPaddr network = siteNetwork.getNetwork();
                IPaddr netmask = siteNetwork.getNetmask();
                
                if ( netmask != null ) {
                    network = IPaddr.and( network, netmask );
                    sw.appendVariable( FLAG_ROUTE, "" + network + " " + netmask );
                } else {
                    sw.appendVariable( FLAG_ROUTE, "" + network );
                }
                
                writePushRoute( sw, network, netmask );
            }
        }
    }

    private void writeGroups( ScriptWriter sw, VpnSettings settings )
    {
        /* XXX Need some group consolidation */
        /* XXX Need some checking for overlapping groups */
        /* XXX Do not exports groups that are not used */

        for ( VpnGroup group : (List<VpnGroup>)settings.getGroupList()) {
            writePushRoute( sw, group.getAddress(), group.getNetmask());
        }
    }

    private void writeClientFiles( VpnSettings settings )
    {
        for ( VpnClient client : (List<VpnClient>)settings.getClientList()) {
            ScriptWriter sw = new VpnScriptWriter();
            
            IPaddr localEndpoint  = client.getAddress();
            IPaddr remoteEndpoint = getRemoteEndpoint( localEndpoint );

            /* XXXX This won't work for a bridge configuration */
            sw.appendVariable( FLAG_CLI_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );
            
            for ( SiteNetwork siteNetwork : (List<SiteNetwork>)client.getExportedAddressList()) {
                sw.appendVariable( FLAG_CLI_ROUTE, "" + siteNetwork.getNetwork() + " " + 
                                   siteNetwork.getNetmask());
            }

            sw.writeFile( VPN_CCD_DIR + "/" + client.getInternalName());
        }
    }
    
    private void writePushRoute( ScriptWriter sw, IPaddr address, IPaddr netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write route with null address" );
            return;
        }

        writePushRoute( sw, address.getAddr(), ( netmask == null ) ? null : netmask.getAddr());
    }

    private void writePushRoute( ScriptWriter sw, InetAddress address )
    {
        writePushRoute( sw, address, null );
    }

    private void writePushRoute( ScriptWriter sw, InetAddress address, InetAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write route with null address" );
            return;
        }

        String value = "\"route ";
        if ( netmask != null ) {
            /* the route command complains you do not pass in the base address */
            value += IPaddr.and( new IPaddr((Inet4Address)address ), new IPaddr((Inet4Address)netmask ));
            value += " " + netmask.getHostAddress();
        } else {
            value += address.getHostAddress();
        }
        
        value += "\"";
        
        sw.appendVariable( FLAG_PUSH,  value );
    }

    /**
     * Assign addresses to the server and all of the clients.
     * XXXX This function needs some serious whitebox testing
     * @throws TransformException - A group does not contain enough addresses for its clients.
     */
    void assignAddresses( VpnSettings settings ) throws TransformException
    {
        /* A mapping from a group to its list of clients */
        Map<VpnGroup,List<VpnClient>> groupToClientList = new HashMap<VpnGroup,List<VpnClient>>();
        
        List<VpnClient> clientList = (List<VpnClient>)settings.getClientList();

        if ( settings.getGroupList().size() == 0 ) throw new TransformException( "No groups" );

        VpnGroup serverGroup   = (VpnGroup)settings.getGroupList().get( 0 );

        /* Always add the first group, even if there aren't any clients in it, 
         * this is where the server pulls its address from */
        groupToClientList.put( serverGroup, new LinkedList());
        
        for ( VpnClient client : clientList ) {
            VpnGroup group = client.getGroup();
            if ( group == null ) {
                logger.error( "NULL group for client [" + client.getName() + "]" );
                continue;
            }

            /* Retrieve the group list this client belongs on */
            List<VpnClient> groupClientList = groupToClientList.get( group );

            /* If a list hasn't been created yet, then create one */
            if ( groupClientList == null ) {
                groupClientList = new LinkedList();
                groupToClientList.put( group, groupClientList );
            }
            
            /* Add this client to the list */
            groupClientList.add( client );
        }

        /* Iterate each group assigning all of the clients IP addresses */
        final boolean isBridge = settings.isBridgeMode();

        /* Test to make sure this gets set */
        boolean setServerAddress = false;

        for ( Map.Entry<VpnGroup,List<VpnClient>> entry  : groupToClientList.entrySet()) {
            VpnGroup group = entry.getKey();
            List<VpnClient> clients = entry.getValue();

            List addrs = new LinkedList();
            boolean isServerGroup = group.equals( serverGroup );
            
            /* Create a new ip matcher to validate all of the created addresses */
            IPMatcher matcher = new IPMatcher( group.getAddress(), group.getNetmask(), false );
                                           
            /* Create enough addresses for all of the clients, and possible the server */
            Set<IPaddr> addressSet = createAddressSet( clients.size() + ( isServerGroup ? 1 : 0 ), 
                                                       group, matcher, isBridge );
            
            /* Remove any duplicates */
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
                logger.warn( "Unable to configure clients" );
                throw new TransformException( "Not enough addresses to assign all clients in group " + 
                                              group.getName());
            }
            
            addressSet.add( address );
            
            getNextAddress( addressData, isBridge );
        }
        
        return addressSet;
    }

    void removeDuplicateAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClient> clientList, 
                                   boolean assignServer )
    {
        Set<IPaddr> addressSet = new HashSet<IPaddr>();

        /* If necessary add the server address */
        if ( assignServer ) {
            IPaddr serverAddress = settings.getServerAddress();
            if (( null != serverAddress ) && matcher.isMatch( serverAddress.getAddr())) {
                addressSet.add( serverAddress );
            }
        }

        /* Check to see if each client has a unique address */
        for ( VpnClient client : clientList ) {
            IPaddr address = client.getAddress();
            /* If the address is already in the set, then unset it from the client since it is
             * already taken */
            if (( null != address ) && matcher.isMatch( address.getAddr()) && !addressSet.add( address )) {
                client.setAddress( null );
            }
        }
    }
    
    void removeTakenAddresses( VpnSettings settings, IPMatcher matcher, List<VpnClient> clientList, 
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

        for ( VpnClient client : clientList ) {
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

    void assignRemainingClients( VpnSettings settings, List<VpnClient> clientList,
                                 Set<IPaddr> addressSet, boolean assignServer )
    {
        Iterator<IPaddr> iter = addressSet.iterator();
        
        /* If necessary assign the server an address */
        if ( assignServer && ( null == settings.getServerAddress())) {
            settings.setServerAddress( iter.next());
            iter.remove();
        }

        /* Assign each client an address */
        for ( VpnClient client : clientList ) {
            /* Nothing to do for this clients that current have addresses */
            if ( client.getAddress() != null ) continue;
            
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

    /* A safe function (exceptionless) for InetAddress.getByAddress */
    IPaddr getByAddress( byte[] data )
    {
        try {
            return new IPaddr((Inet4Address)InetAddress.getByAddress( data ));
        } catch ( UnknownHostException e ) {
            logger.error( "Something happened, array should be 4 actually " + data.length + " bytes", e );
        }
        return null;
    }

    /* For Tunnel nodes, this gets the corresponding remote endpoint, see the openvpn howto for 
     * the definition of a remote and local endpoint */
    IPaddr getRemoteEndpoint( IPaddr localEndpoint )
    {
        byte[] data = localEndpoint.getAddr().getAddress();
        data[3] += 1;
        return getByAddress( data );
    }
}
