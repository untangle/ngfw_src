/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.script.ScriptWriter;
import com.metavize.mvvm.tran.IPaddr;

import static com.metavize.mvvm.tran.script.ScriptWriter.COMMENT;
import static com.metavize.mvvm.tran.script.ScriptWriter.METAVIZE_HEADER;

import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.RouteInternal;
import com.metavize.mvvm.networking.internal.InterfaceInternal;

class InterfacesScriptWriter extends ScriptWriter
{
    private static final Logger logger = Logger.getLogger( InterfacesScriptWriter.class );

    private final NetworkSpacesInternalSettings settings;

    private static final String INTERFACE_HEADER = 
        COMMENT + METAVIZE_HEADER +
        COMMENT + " network interfaces\n\n" +
        COMMENT + " Loopback network interface\n" + 
        "auto lo\n" +
        "iface lo inet loopback\n\n";
    
    private static final String BUNNICULA_BASE = System.getProperty( "bunnicula.home" );
    private static final String FLUSH_CONFIG = BUNNICULA_BASE + "/networking/flush-interfaces";

    private static final String DHCP_BOGUS_ADDRESS = "169.254.210.5";

    /* String used to indicate that pump should only update the address of the interface */
    static final String DHCP_FLAG_ADDRESS_ONLY = " --no-gateway --no-resolvconf ";

    InterfacesScriptWriter( NetworkSpacesInternalSettings settings )
    {
        super();
        this.settings = settings;
    }
    
    /* This function should only be called once */
    void addNetworkSettings() throws NetworkException, ArgonException
    {
        boolean isFirst = true;
        for ( NetworkSpaceInternal space : this.settings.getNetworkSpaceList()) { 
            /* If the space is not enabled, keep on trucking */
            if ( !space.getIsEnabled()) continue;
            
            /* Add a seperator betwixt each active space */
            if ( !isFirst ) appendLine( "" );

            addNetworkSpace( space, isFirst );
            isFirst = false;
            
            if ( !this.settings.getIsEnabled()) break;
        }

        /* Add this after the last interface, done this way so there isn't a space
         * in between the interface and the final commands */
        addRoutingTable();
    }

    /* Index is used to name the bridge */
    private void addNetworkSpace( NetworkSpaceInternal space, boolean isFirst )
        throws NetworkException, ArgonException
    {
        NetworkUtilPriv nu = NetworkUtilPriv.getPrivInstance();
        List<InterfaceInternal> interfaceList = (List<InterfaceInternal>)space.getInterfaceList();
        String name = space.getDeviceName();
        boolean isBridge = space.isBridge();
        int mtu = space.getMtu();
                
        appendLine( "auto " + name );

        List<IPNetwork> networkList = (List<IPNetwork>)space.getNetworkList();
        IPNetwork primaryAddress = null;
        
        appendLine( "iface " + name + " inet manual" );

        /* Insert the flush command for the first network space */
        if ( isFirst ) {
            appendCommands( "pre-up if [ -r " + FLUSH_CONFIG + " ]; then sh " + FLUSH_CONFIG + "; fi" );
        }

        /* If this is a bridge, then add the necessary settings for the bridge */
        if ( isBridge ) {
            appendCommands( "up brctl addbr " + name );

            /* Build a list of all of the ports inside of the bridge */
            for ( InterfaceInternal intf : interfaceList ) {
                String dev = intf.getIntfName();
                appendCommands( "up brctl addif " + name + " " + dev,
                                "up ifconfig " + dev + " 0.0.0.0 mtu " + mtu + " up" );
            }
        }
        
        /* Add the primary address */
        if ( space.getIsDhcpEnabled()) {
            /* This jibberish guarantees that if the device doesn't come up, it still
             * gets an address */
            String flags = "";
            
            /* If this is not the primary space, then the gateway and
             * /etc/resolv.conf should not be updated. */
            if ( !isFirst ) flags = DHCP_FLAG_ADDRESS_ONLY;

            String command = "up pump " + flags + " -i " + name + 
                " || ifconfig " + name + " " + DHCP_BOGUS_ADDRESS + space.getIndex() + 
                " netmask 255.255.255.0"; 
            
            appendCommands( command );
            appendCommands( "up ifconfig " + name + " mtu " + mtu );
        } else {
            primaryAddress = space.getPrimaryAddress();
            appendCommands( "up ifconfig " + name + " " + primaryAddress.getNetwork() +
                            " netmask " + primaryAddress.getNetmask() + " mtu " + mtu );
        }

        /* Add all of the aliases */
        int aliasIndex = 0;
        for ( IPNetwork network : networkList ) {
            /* Only add the primary address is DHCP is not enabled */
            if ( !space.getIsDhcpEnabled() && network.equals( primaryAddress )) continue;
            
            if ( nu.isUnicast( network )) {
                String aliasName = name + ":" + aliasIndex++;
                appendCommands( "up ifconfig " + aliasName + " " + network.getNetwork() +
                                " netmask " + network.getNetmask() + " mtu " + mtu );
            } else {
                appendCommands( "up ip route add to " + nu.toRouteString( network ) + " dev " + name );
            }
        }
    }

    private void addRoutingTable() throws NetworkException
    {
        NetworkUtilPriv nup = NetworkUtilPriv.getPrivInstance();

        /* Add the default route and all of the other routes after configuring the
         * last network space */
        IPaddr defaultRoute = this.settings.getDefaultRoute();
        
        /* Add the routing table */
        for ( RouteInternal route : (List<RouteInternal>)this.settings.getRoutingTable()) {
            /* Ignore disabled routes */
            if ( !route.getIsEnabled()) continue;

            IPNetwork destination = route.getDestination();
            IPaddr nextHop = route.getNextHop();

            /* Ignore empty routing entries */
            if ( destination.equals( IPNetwork.getEmptyNetwork()) ||
                 ( destination.getNetwork() == null || destination.getNetwork().isEmpty()) ||
                 ( destination.getNetmask() == null || destination.getNetmask().isEmpty()) ||
                 ( nextHop == null || nextHop.isEmpty())) {
                logger.warn( "Ignoring empty routing entry: " + route );
            }

            // !!! This is currently not supported.
            // if ( route.getNetworkSpace() != null ) {
            // logger.warn( "Custom routing rules with per network space are presently not supported" );
            //}
            
            appendCommands( "up ip route add to " + nup.toRouteString( destination ) + " via " + nextHop );
        }
        
        /* Add the default route last */
        if (( defaultRoute != null ) && ( !defaultRoute.isEmpty())) {
            appendCommands( "up ip route add to default via " + settings.getDefaultRoute());
        }
        
        appendCommands( "up ip route flush table cache" );
    }

    /* A helper to append commands that can't fail */
    private void appendCommands( String ... commandArray )
    {
        for ( String command : commandArray ) appendLine( "\t" + command + " || true" );
    }
        
    @Override
    protected String header()
    {
        return INTERFACE_HEADER;
    }
} 
