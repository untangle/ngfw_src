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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.Session;
import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;


class EventHandler extends AbstractEventHandler
{
    private static final int BLOCK_COUNTER = Transform.GENERIC_0_COUNTER;
    private static final int PASS_COUNTER  = Transform.GENERIC_1_COUNTER;

    private final Logger logger = Logger.getLogger( EventHandler.class );

    private List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

    private boolean isQuickExit = true;
    private boolean rejectSilently = true;

    /* True to accept by default, false to block by default */
    private boolean isDefaultAccept = true;

    /* Firewall Transform */
    private final FirewallImpl transform;

    EventHandler( FirewallImpl transform )
    {
        super(transform);

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
            request.attach(new FirewallEvent(request.id(), rule, reject, ruleIndex));
        }

        /* Track the statistics */
        transform.statisticManager.incrRequest( protocol, request, reject, rule == null );
    }

    // XXX move to new callback
    @Override
    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            fe.setPipelineEndpoints(s.id());
            transform.log(fe);
        }
    }

    // XXX move to new callback
    @Override
    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            fe.setPipelineEndpoints(s.id());
            transform.log(fe);
        }
    }

    void configure( FirewallSettings settings )
    {
        this.isQuickExit = settings.isQuickExit();
        this.rejectSilently = settings.isRejectSilently();
        this.isDefaultAccept = settings.isDefaultAccept();

        /* Create a new list in tmp to avoid sessions that are iterating the current list */
        List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

        List<FirewallRule> list = (List<FirewallRule>)settings.getFirewallRuleList();

        if ( list == null ) {
            logger.error( "Settings contain null firewall list" );
        } else {
            int index = 1;

            /* Update all of the rules */
            for ( Iterator<FirewallRule> iter = list.iterator() ; iter.hasNext() ; index++ ) {
                FirewallRule rule = iter.next();
                /* Don't insert inactive rules */
                if ( !rule.isLive()) continue;
                firewallRuleList.add( new FirewallMatcher( rule, index ));
            }
        }

        this.firewallRuleList = firewallRuleList;
    }
}
