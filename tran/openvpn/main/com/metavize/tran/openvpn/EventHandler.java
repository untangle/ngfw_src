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
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    /* Triggered when there is a VPN session that is blocked */
    /* XXXXXXX Probably want to log block events */
    private static final int BLOCK_COUNTER   = Transform.GENERIC_0_COUNTER;

    /* Triggered when there is a VPN session that is passed */
    private static final int PASS_COUNTER    = Transform.GENERIC_1_COUNTER;

    /* Triggered whenever a client connects to the VPN */
    private static final int CONNECT_COUNTER = Transform.GENERIC_2_COUNTER;

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
        logger.debug( "New session: [" + request.id() + "]" );

        if ( request.clientIntf() != IntfConverter.VPN && request.serverIntf() != IntfConverter.VPN ) {
            /* Nothing to do */
            request.release();
            return;
        }
        else if ( request.clientIntf() == IntfConverter.VPN && request.serverIntf() == IntfConverter.VPN ) {
            /* XXXXXXXXXX sort this out */
            request.release();
            return;
        }
        /* XXXX Handle bridging */
        else if ( request.clientIntf() == IntfConverter.VPN ) {
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
            logger.debug( "Testing " + vpn.getHostAddress() + " against " + matcher );
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
            logger.debug( "Testing " + local.getHostAddress() + " against " + matcher );
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
        logger.debug( "Accepted VPN session: [" + request.id() + "]" );
        transform.incrementCount( PASS_COUNTER );
        request.release();

        /* XXX Probably want to create an event */
        // transform.statisticManager.incrRequest( protocol, request, reject );
    }

    private void reject( IPNewSessionRequest request )
    {
        /* XXX Should this always reject silently */
        request.rejectSilently();

        transform.incrementCount( BLOCK_COUNTER );

        /* XXX Probably want to create an event */
        logger.debug( "Blocked VPN session: [" + request.id() + "]" );

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

        for ( VpnClient client : (List<VpnClient>)settings.getClientList()) {
            /* Continue if the client isn't live or the group the client is in isn't live */
            if ( !client.isLive() || ( null == client.getGroup()) || !client.getGroup().isLive()) continue;

            for ( SiteNetwork siteNetwork : (List<SiteNetwork>)client.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;
                IPMatcher matcher = new IPMatcher( siteNetwork.getNetwork(), siteNetwork.getNetmask(),
                                                   false );
                clientAddressList.add( matcher );
                logger.debug( "clientAddressList: [" + matcher + "]" );
            }
        }

        /* If requested add the matchers for internal */
        if ( settings.getIsInternalExported()) {
            exportedAddressList.add( IPMatcher.MATCHER_INTERNAL );
            logger.debug( "exportedAddressList: [internal]" );
        }

        /* If requested add the matchers for external, (may be the same as internal, but should have
         * no effect) */
        /* XXXXX This isn't going to work, the external matchers are jenky */
        if ( settings.getIsExternalExported()) {
            exportedAddressList.add( IPMatcher.MATCHER_EXTERNAL );
            logger.debug( "exportedAddressList: [external]" );
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
