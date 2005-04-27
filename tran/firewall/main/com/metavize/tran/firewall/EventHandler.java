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
import com.metavize.mvvm.tran.firewall.FirewallRule;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger( EventHandler.class );
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

    private boolean isQuickExit = true;
    private boolean rejectSilently = true;

    /* True to accept by default, false to block by default */
    private boolean isDefaultAccept = true;
    
    EventHandler() 
    {
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
        boolean reject = !isDefaultAccept;

        for ( Iterator<FirewallMatcher> iter = firewallRuleList.iterator() ; iter.hasNext() ; ) {
            FirewallMatcher matcher = iter.next();

            if ( matcher.isMatch( request, protocol )) {
                reject = matcher.isTrafficBlocker();
                
                if ( isQuickExit )
                    break;
            }
        }

        if ( reject ) {
            logger.debug( "Rejecting session: " + request );

            if ( rejectSilently ) {
                request.rejectSilently();
            } else {
                /* XXX How to reject non-silently */
                request.rejectSilently();
            }
            
            /* Increment the block counter */
            incrementCount( Transform.GENERIC_0_COUNTER ); // BLOCK COUNTER

        } else {
            logger.debug( "Releasing session: " + request );
            request.release();

            /* Increment the pass counter */
            incrementCount( Transform.GENERIC_1_COUNTER ); // PASS COUNTER
        }                
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
            /* Update all of the rules */
            for ( Iterator<FirewallRule> iter = list.iterator() ; iter.hasNext() ; ) {
                logger.debug( "Inserting rule" );
                firewallRuleList.add( new FirewallMatcher( iter.next()));
            }
        }
    }
}
