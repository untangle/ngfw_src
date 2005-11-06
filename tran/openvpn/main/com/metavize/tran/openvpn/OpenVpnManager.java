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
import java.util.LinkedHashSet;

import java.net.InetAddress;
import java.net.Inet4Address;

import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ScriptWriter;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.IPMatcher;

import static com.metavize.tran.openvpn.Constants.*;

class OpenVpnManager
{
    private static final String VPN_CONF_DIR       = "/etc/openvpn";
    private static final String VPN_SERVER_FILE    = VPN_CONF_DIR + "/server.conf";
    
    /* Most likely want to bind to the outside address when using NAT */
    private static final String FLAG_LOCAL       = "local";
    private static final String FLAG_PORT        = "port";
    private static final int    DEFAULT_PORT     = 1194;
    private static final String FLAG_PROTOCOL    = "proto";
    private static final String DEFAULT_PROTOCOL = "udp";
    private static final String FLAG_DEVICE      = "dev";
    private static final String DEVICE_BRIDGE    = "tap";
    private static final String DEVICE_ROUTING   = "tun";
    
    private static final String FLAG_ROUTE_GROUP  = "server";
    private static final String FLAG_BRIDGE_GROUP = "server-bridge";

    private static final String FLAG_PUSH_PARAM  = "push";
    private static final String FLAG_EXPOSE_CLI  = "client-to-client";

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
        "ifconfig-pool-persist ipp.txt",
        "client-config-dir ccd",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        /* XXXXXXXXX Need to select a valid cipher to use */
        "cipher none",
        "user nobody",
        "group nogroup",
        
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
    
    void configure( VpnSettings settings, NetworkingConfiguration netConfig ) throws TransformException
    {
        writeSettings( settings, netConfig );
    }
    
    void writeSettings( VpnSettings settings, NetworkingConfiguration netConfig ) throws TransformException
    {
        ScriptWriter sw = new VpnScriptWriter();
        
        /* Insert all of the default parameters */
        sw.appendLines( DEFAULTS );
        
        String serverPoolFlag;

        /* Bridging or routing */
        if ( settings.isBridgeMode()) {
            sw.appendVariable( FLAG_DEVICE, DEVICE_BRIDGE );
            serverPoolFlag = FLAG_BRIDGE_GROUP;
        } else {
            sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );
            serverPoolFlag = FLAG_ROUTE_GROUP;
        }

        /* XXX Convert to address groups rather than single values */
        List groupList = settings.getGroupList();
        if (( groupList != null ) && ( groupList.size() > 0 )) {
            VpnGroup group = (VpnGroup)groupList.get( 0 );            
            /* XXX This won't work for a bridge */
            sw.appendVariable( serverPoolFlag, group.getAddress().toString() + " " + 
                               group.getNetmask().toString());
        } else {
            logger.error( "Unable to save settings, no vpn groups" );
            return;
        }
        
        /* Push the inside address range to the clients */
        /* XXX This is just incorrect */
        sw.appendVariable( FLAG_PUSH_PARAM, "\"route 192.168.1.0 255.255.255.0\""); 
                          
        if ( settings.getExposeClients()) sw.appendLine( FLAG_EXPOSE_CLI );

        int maxClients = settings.getMaxClients();
        if ( maxClients > 0 ) sw.appendVariable( FLAG_MAX_CLI, String.valueOf( maxClients ));

        
        
        /* XXX Insert the site to site parameters */

        
        
        sw.writeFile( VPN_SERVER_FILE );
    }

    /**
     * Assign addresses to all of the clients.
     * XXXX This function needs some serious whitebox testing
     * @throws TransformException - A group does not contain enough addresses for its clients.
     */
    void assignAddresses( VpnSettings settings ) throws TransformException
    {
        /* A mapping from a group to its list of clients */
        Map<VpnGroup,List<VpnClient>> groupToClientList = new HashMap<VpnGroup,List<VpnClient>>();
        
        List<VpnClient> clientList = (List<VpnClient>)settings.getClientList();

        for ( VpnClient client : clientList ) {
            VpnGroup group = client.getGroup();
            if ( group == null ) {
                logger.error( "NULL group for client: " + client.getName());
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
        for ( Map.Entry<VpnGroup,List<VpnClient>> entry  : groupToClientList.entrySet()) {
            VpnGroup group = entry.getKey();
            List<VpnClient> clients = entry.getValue();
            List addrs = new LinkedList();
            
            /* Create enough addresses for all of the clients */
            /* Get the base address */
            InetAddress base = IPaddr.and( group.getAddress(), group.getNetmask()).getAddr();
            
            /* Create a new ip matcher to detect when the edge has been reached */
            IPMatcher matcher = new IPMatcher( group.getAddress(), group.getNetmask(), false );
                                           
            byte[] addressData = base.getAddress();
            addressData[3] &= 0xFC;
            addressData[3] |= 1;

            Set<IPaddr> addressSet = new LinkedHashSet<IPaddr>();
            for ( int c = clients.size() ; c-- > 0 ; ) {
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

            /* Now remove all of the entries that are taken */
            for ( VpnClient client : clients ) {
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

            /* Now use the remaining addresses to configure the clients that don't have addresses */
            Iterator<IPaddr> iter = addressSet.iterator();
            for ( VpnClient client : clients ) {
                /* Nothing to do for this clients that current have addresses */
                if ( client.getAddress() != null ) continue;
                
                /* Once you use the node, you must remove it from the set so it is never used again */
                client.setAddress( iter.next());
                iter.remove();
            }
        }
    }

    void getNextAddress( byte[] current, boolean isBridge )
    {
        /* For a bridge each one increments by 1 */
        current[3] += ( !isBridge ) ? 4 : 1;
        /* Overflow  */
        if ( current[3] == 1 ) {
            current[2]++;
            if ( current[2] == 0 ) {
                current[1]++;
                if ( current[1] == 0 ) {
                    current[0]++;
                }
            }
        }        
    }

    IPaddr getByAddress( byte[] data )
    {
        try {
            return new IPaddr((Inet4Address)InetAddress.getByAddress( data ));
        } catch ( UnknownHostException e ) {
            logger.error( "This should never happen, array should be 4 bytes", e );
        }
        return null;
    }
}
