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

import org.apache.log4j.Logger;

import com.metavize.mvvm.NetworkingConfiguration;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ScriptWriter;

import static com.metavize.tran.openvpn.Constants.*;

class OpenVpnManager
{
    private static final String VPN_CONF_DIR    = "/etc/openvpn";
    private static final String VPN_SERVER_FILE = VPN_CONF_DIR + "/server.conf";
    
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
}
