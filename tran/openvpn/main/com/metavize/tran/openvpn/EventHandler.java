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

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;

import com.metavize.mvvm.IntfConstants;

import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );

    /* Are the VPNs bridged with the other networks */
    private boolean isBridge = false;

    /* Any client can connect to any exported address and vice versa */
    private List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
    private List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

    /* Firewall Transform */
    private final VpnTransformImpl transform;

    EventHandler( VpnTransformImpl transform )
    {
        super(transform);

        this.transform = transform;
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
        for ( IPMatcher matcher : this.clientAddressList ) {
            if ( logger.isDebugEnabled()) {
                logger.debug( "Testing " + vpn.getHostAddress() + " against " + matcher );
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
                logger.debug( "Testing " + local.getHostAddress() + " against " + matcher );
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
        transform.incrementCount( Constants.PASS_COUNTER );
        request.release();

        /* XXX Probably want to create an event */
        // transform.statisticManager.incrRequest( protocol, request, reject );
    }

    private void reject( IPNewSessionRequest request )
    {
        /* XXX Should this always reject silently */
        request.rejectSilently();

        transform.incrementCount( Constants.BLOCK_COUNTER );

        /* XXX Probably want to create an event */
        if ( logger.isDebugEnabled()) {
            logger.debug( "Blocked VPN session: [" + request.id() + "]" );
        }

        // transform.statisticManager.incrRequest( protocol, request, reject );
    }

    void configure( VpnSettings settings )
    {
        /* Create temporary lists */
        List <IPMatcher> clientAddressList = new LinkedList<IPMatcher>();
        List <IPMatcher> exportedAddressList = new LinkedList<IPMatcher>();

        logger.debug( "Configuring handler" );

        for ( VpnGroup group : (List<VpnGroup>)settings.getGroupList()) {
            /* Don't insert inactive groups */
            if ( !group.isLive()) continue;
            IPMatcher matcher = new IPMatcher( group.getAddress(), group.getNetmask(), false );
            clientAddressList.add( matcher );
            logger.debug( "clientAddressList: [" + matcher + "]" );
        }

        for ( VpnSite site : (List<VpnSite>)settings.getSiteList()) {
            /* Continue if the client isn't live or the group the client is in isn't live */
            if ( !site.isEnabled()) continue;

            for ( SiteNetwork siteNetwork : (List<SiteNetwork>)site.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;
                IPMatcher matcher = new IPMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask(),
                                                   false );
                clientAddressList.add( matcher );
                logger.debug( "clientAddressList: [" + matcher + "]" );
            }
        }

        for ( SiteNetwork siteNetwork : (List<SiteNetwork>)settings.getExportedAddressList()) {
            if ( !siteNetwork.isLive()) continue;
            IPMatcher matcher = new IPMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask(), false );
            exportedAddressList.add( matcher );
            logger.debug( "exportedAddressList: [" + matcher + "]" );
        }

        this.clientAddressList   = clientAddressList;
        this.exportedAddressList = exportedAddressList;

        logger.debug( "" );
    }
}
