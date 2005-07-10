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

package com.metavize.tran.firewall;

import java.util.List;

import java.util.Iterator;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.metavize.mvvm.tapi.Protocol;

import com.metavize.mvvm.NetworkingManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;

import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;

import com.metavize.mvvm.tapi.IPSession;

import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

class EventHandler extends AbstractEventHandler
{
    private static final int BLOCK_COUNTER = Transform.GENERIC_0_COUNTER;
    private static final int PASS_COUNTER  = Transform.GENERIC_1_COUNTER;

    private final Logger logger = Logger.getLogger( EventHandler.class );
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private final FirewallStatisticManager statisticManager = FirewallStatisticManager.getInstance();

    private List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

    private boolean isQuickExit = true;
    private boolean rejectSilently = true;

    /* True to accept by default, false to block by default */
    private boolean isDefaultAccept = true;

    /* Firewall Transform */
    private final FirewallImpl transform;
    
    EventHandler( FirewallImpl transform ) 
    {
        this.transform = transform;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest(), Protocol.TCP );
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest(), Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        /* By default, do whatever the first rule is */
        boolean reject    = !isDefaultAccept;
        FirewallRule rule = null;
        int ruleIndex     = 0;
        
        for ( Iterator<FirewallMatcher> iter = firewallRuleList.iterator() ; iter.hasNext() ; ) {
            FirewallMatcher matcher = iter.next();

            if ( matcher.isMatch( request, protocol )) {
                reject = matcher.isTrafficBlocker();
                
                if ( isQuickExit ) {
                    rule      = matcher.rule();
                    ruleIndex = matcher.ruleIndex();
                    break;
                }
            }
        }

        if ( reject ) {
            logger.debug( "Rejecting session: " + request );

            if ( rejectSilently ) {
                request.rejectSilently();
            } else {
                if ( protocol == Protocol.UDP ) {
                    request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE );
                } else {
                    ((TCPNewSessionRequest)request).rejectReturnRst();
                }
            }
            
            /* Increment the block counter */
            transform.incrementCount( BLOCK_COUNTER ); // BLOCK COUNTER

        } else {
            logger.debug( "Releasing session: " + request );
            request.release( false );

            /* Increment the pass counter */
            transform.incrementCount( PASS_COUNTER ); // PASS COUNTER
        }

        /* If necessary log the event */
        if ( rule != null && rule.getLog()) {
            eventLogger.info( new FirewallEvent( request.id(), rule, ruleIndex ));
        }

        /* Track the statistics */
        statisticManager.incrRequest( protocol, request, reject, rule == null );
    }
    
    void configure( FirewallSettings settings )
    {
        this.isQuickExit = settings.isQuickExit();
        this.rejectSilently = settings.isRejectSilently();
        this.isDefaultAccept = settings.isDefaultAccept();

        /* Empty out the list */
        firewallRuleList.clear();

        List<FirewallRule> list = (List<FirewallRule>)settings.getFirewallRuleList();

        if ( list == null ) {
            logger.error( "Settings contain null firewall list" );
        } else {
            int index = 1;

            /* Update all of the rules */
            for ( Iterator<FirewallRule> iter = list.iterator() ; iter.hasNext() ; index++ ) {
                logger.debug( "Inserting rule" );
                firewallRuleList.add( new FirewallMatcher( iter.next(), index ));
            }
        }
    }
}
