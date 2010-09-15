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

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.TCPSessionEvent;
import com.untangle.uvm.vnet.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.vnet.event.UDPSessionEvent;

class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);

    private List<FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

    private boolean isQuickExit = true;
    private boolean rejectSilently = true;

    /* True to accept by default, false to block by default */
    private boolean isDefaultAccept = true;

    /* Firewall Node */
    private final FirewallImpl node;

    public EventHandler(FirewallImpl node)
    {
        super(node);

        this.node = node;
    }

    public void handleTCPNewSessionRequest(TCPNewSessionRequestEvent event) 
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.TCP);
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event) 
    {
        handleNewSessionRequest(event.sessionRequest(), Protocol.UDP);
    }

    private void handleNewSessionRequest(IPNewSessionRequest request, Protocol protocol)
    {
        boolean reject    = !isDefaultAccept; /* By default, do whatever the first rule is */
        boolean log = false;
        FirewallRule rule = null;
        int ruleIndex     = 0;

        /**
         * Find the matching rule compute block/log verdicts
         */
        FirewallMatcher matcher = findMatchingRule(protocol, request);
        if (matcher != null) {
            rule      = matcher.rule();
            ruleIndex = matcher.ruleIndex();
            if (rule != null) {
                reject = matcher.isTrafficBlocker();
                log = rule.getLog();
            }
        }

        /**
         * Take the appropriate actions
         */
        if (reject) {
            if (logger.isDebugEnabled()) {
                logger.debug("Rejecting session: " + request);
            }

            if (rejectSilently) {
                /* use finalization only if logging */
                request.rejectSilently(log);
            } else {
                if (protocol == Protocol.UDP) {
                    request.rejectReturnUnreachable(IPNewSessionRequest.PORT_UNREACHABLE, log);
                } else {
                    ((TCPNewSessionRequest)request).rejectReturnRst(log);
                }
            }

            /* Increment the block counter */
            node.incrementBlockCount(); 

            /* We just blocked, so we have to log too, regardless of what the rule actually says */
            FirewallEvent fwe = new FirewallEvent(request.pipelineEndpoints(), reject, ruleIndex);
            fwe.setRuleId(rule.getId());
            request.attach(fwe);
            node.incrementLogCount(); 
        } else { /* not rejected */
            if (logger.isDebugEnabled()) {
                logger.debug("Releasing session: " + request);
            }

            /* only finalize if logging */
            request.release(log);

            /* Increment the pass counter */
            node.incrementPassCount();

            /* If necessary log the event */
            if (log) {
                FirewallEvent fwe = new FirewallEvent(request.pipelineEndpoints(), 
                                                      reject, 
                                                      ruleIndex);
                fwe.setRuleId(rule.getId());
                request.attach(fwe);
                node.incrementLogCount();
            }
        }

        /* Track the statistics */
        node.statisticManager.incrRequest(protocol, request, reject, rule == null);
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event) 
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.log(fe);
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event) 
    {
        Session s = event.session();
        FirewallEvent fe = (FirewallEvent)s.attachment();
        if (null != fe) {
            node.log(fe);
        }
    }

    public void configure(FirewallSettings settings)
    {
        this.isQuickExit = settings.getBaseSettings().isQuickExit();
        this.rejectSilently = settings.getBaseSettings().isRejectSilently();
        this.isDefaultAccept = settings.getBaseSettings().isDefaultAccept();

        /* Create a new list in tmp to avoid sessions that are
         * iterating the current list */
        List <FirewallMatcher> firewallRuleList = new LinkedList<FirewallMatcher>();

        List<FirewallRule> list = settings.getFirewallRuleList();

        if (list == null) {
            logger.error("Settings contain null firewall list");
        } else {
            int index = 1;

            /* Update all of the rules */
            for (Iterator<FirewallRule> iter = list.iterator() ; iter.hasNext() ; index++) {
                FirewallRule rule = iter.next();
                /* Don't insert inactive rules */
                if (!rule.isLive()) continue;
                firewallRuleList.add(new FirewallMatcher(rule, index));
            }
        }

        this.firewallRuleList = firewallRuleList;
    }

    protected FirewallMatcher findMatchingRule( Protocol protocol,
                                                byte clientIntf, InetAddress clientAddr, int clientPort,
                                                byte serverIntf, InetAddress serverAddr, int serverPort)
    {
        InterfaceComparator ifCompare = LocalUvmContextFactory.context().localIntfManager().getInterfaceComparator();

        for (FirewallMatcher matcher : firewallRuleList) {

            if (matcher.isMatch(protocol,
                                clientIntf, serverIntf,
                                clientAddr, serverAddr,
                                clientPort, serverPort, ifCompare)) {

                return matcher;
            }
        }

        return null;
    }
    
    protected FirewallMatcher findMatchingRule( Protocol protocol, IPNewSessionRequest request )
    {
        return findMatchingRule( protocol,
                                 request.clientIntf(), request.clientAddr(), request.clientPort(),
                                 request.serverIntf(), request.getNatToHost(), request.getNatToPort());
    }
    

}
