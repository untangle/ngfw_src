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

package com.untangle.node.firewall;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.Protocol;
import com.untangle.uvm.tapi.Session;
import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.TCPSessionEvent;
import com.untangle.uvm.tapi.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.UDPSessionEvent;
import com.untangle.uvm.node.Node;
import org.apache.log4j.Logger;

class EventHandler extends AbstractEventHandler
{
    private static final int BLOCK_COUNTER = Node.GENERIC_0_COUNTER;
    private static final int PASS_COUNTER  = Node.GENERIC_1_COUNTER;

    private final Logger logger = Logger.getLogger( EventHandler.class );

    private List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

    private boolean isQuickExit = true;
    private boolean rejectSilently = true;

    /* True to accept by default, false to block by default */
    private boolean isDefaultAccept = true;

    /* Firewall Node */
    private final FirewallImpl node;

    EventHandler( FirewallImpl node )
    {
        super(node);

        this.node = node;
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

        boolean log = ( rule != null && rule.getLog()) ? true : false;
        
        if ( reject ) {            
            if (logger.isDebugEnabled()) {
                logger.debug( "Rejecting session: " + request );
            }
            
            if ( rejectSilently ) {
                /* use finalization only if logging */
                request.rejectSilently( log );
            } else {
                if ( protocol == Protocol.UDP ) {
                    request.rejectReturnUnreachable( IPNewSessionRequest.PORT_UNREACHABLE, log );
                } else {
                    ((TCPNewSessionRequest)request).rejectReturnRst( log );
                }
            }
            
            /* Increment the block counter */
            node.incrementCount( BLOCK_COUNTER ); // BLOCK COUNTER
            
            /* If necessary log the event */
            if ( log ) {
                request.attach(new FirewallEvent(request.pipelineEndpoints(), rule, reject, ruleIndex));
            }
            
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug( "Releasing session: " + request );
            }
            
            /* only finalize if logging */
            request.release( log );
            
            /* Increment the pass counter */
            node.incrementCount( PASS_COUNTER ); // PASS COUNTER
            
            /* If necessary log the event */
            if ( log ) {
                request.attach(new FirewallEvent(request.pipelineEndpoints(), rule, reject, ruleIndex));
            }
        }

        /* Track the statistics */
        node.statisticManager.incrRequest( protocol, request, reject, rule == null );
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.log(fe);
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.log(fe);
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
