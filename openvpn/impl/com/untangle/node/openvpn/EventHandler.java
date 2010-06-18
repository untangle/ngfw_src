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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.MPipeException;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );

    /* Is this a VPN client, a VPN client passes all traffic */
    private boolean isUntanglePlatformClient = false;

    /* Any client can connect to any exported address and vice versa */
    private List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
    private List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

    /* Firewall Node */
    private final VpnNodeImpl node;

    EventHandler( VpnNodeImpl node )
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest());
    }

    private void handleNewSessionRequest( IPNewSessionRequest request )
    {
        if ( logger.isDebugEnabled()) logger.debug( "New session: [" + request.id() + "]" );

        if ( request.clientIntf() != IntfConstants.VPN_INTF &&
             request.serverIntf() != IntfConstants.VPN_INTF ) {
            /* Nothing to do */
            request.release();
            return;
        }
        else if ( request.clientIntf() == IntfConstants.VPN_INTF &&
                  request.serverIntf() == IntfConstants.VPN_INTF ) {
            /* XXXXXXXXXX sort this out */
            request.release();
            return;
        }
        /* XXXX Handle bridging */
        else if ( request.clientIntf() == IntfConstants.VPN_INTF ) {
            /* VPN client going to another interface */
            checkAddress( request, request.clientAddr(), request.serverAddr());
        }
        else {
            /* Local user trying to reach a VPN client */
            checkAddress( request, request.serverAddr(), request.clientAddr());
        }
    }

    /**
     * Check whether vpn is allowed to talk to local
     */
    private void checkAddress( IPNewSessionRequest request, InetAddress vpn, InetAddress local )
    {
        boolean isValid = false;

        /* Clients pass all traffic */
        if ( this.isUntanglePlatformClient ) {
            node.incrementPassCount();
            request.release();

            return;
        }

        for ( IPMatcher matcher : this.clientAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing vpn " + vpn.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( vpn )) {
                isValid = true;
                break;
            }
        }

        if ( !isValid ) {
            reject( request );
            return;
        }

        /* The local address must be one of the exported addresses */
        isValid = false;

        for ( IPMatcher matcher : this.exportedAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing local " + local.getHostAddress() + " against " + matcher );
            }
            if ( matcher.isMatch( local )) {
                isValid = true;
                break;
            }
        }

        if ( !isValid ) {
            reject( request );
            return;
        }

        /* Accept the request */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Accepted VPN session: [" + request.id() + "]" );
        }
        node.incrementPassCount();
        request.release();

        /* XXX Probably want to create an event */
        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    private void reject( IPNewSessionRequest request )
    {
        /* XXX Should this always reject silently */
        request.rejectSilently();

        node.incrementBlockCount();

        /* XXX Probably want to create an event */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Blocked VPN session: [" + request.id() + "]" );
        }

        // node.statisticManager.incrRequest( protocol, request, reject );
    }

    void configure( VpnSettings settings )
    {
        logger.debug( "Configuring handler" );

        if ( settings.isUntanglePlatformClient()) {
            isUntanglePlatformClient = settings.isUntanglePlatformClient();
            return;
        }

        /* Create temporary lists */
        List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
        List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

        IPMatcherFactory imf = IPMatcherFactory.getInstance();

        for ( VpnGroup group : (List<VpnGroup>)settings.getGroupList()) {
            /* Don't insert inactive groups */
            if ( !group.isLive()) continue;
            IPMatcher matcher = imf.makeSubnetMatcher( group.getAddress(), group.getNetmask());

            clientAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "clientAddressList: [" + matcher + "]" );
            }
        }
        
        Map<String,VpnGroup> groupMap = OpenVpnManager.buildGroupMap(settings);

        for ( VpnSite site : (List<VpnSite>)settings.getSiteList()) {
            VpnGroup group = groupMap.get(site.getGroupName());
            /* Continue if the client isn't live or the group the client is in isn't live */
            if ( !site.isEnabled() || ( group == null ) || !group.isLive()) {
                continue;
            }

            for ( ClientSiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;
                IPMatcher matcher =
                    imf.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

                clientAddressList.add( matcher );
                if (logger.isDebugEnabled()) {
                    logger.debug( "clientAddressList: [" + matcher + "]" );
                }
            }
        }

        for ( ServerSiteNetwork siteNetwork : settings.getExportedAddressList()) {
            if ( !siteNetwork.isLive()) continue;
            IPMatcher matcher =
                imf.makeSubnetMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask());

            exportedAddressList.add( matcher );
            if (logger.isDebugEnabled()) {
                logger.debug( "exportedAddressList: [" + matcher + "]" );
            }
        }

        this.clientAddressList   = clientAddressList;
        this.exportedAddressList = exportedAddressList;

        logger.debug( "" );
    }
}
