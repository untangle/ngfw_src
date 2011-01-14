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
package com.untangle.node.router;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.StatisticManager;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.Protocol;

class RouterStatisticManager extends StatisticManager
{
    /* Interface matcher to determine if the sessions is incoming or outgoing */
    /* !!! This is not legit */
    final IntfMatcher matcherIncoming = IntfMatcherFactory.getInstance().getInternalMatcher();
    final IntfMatcher matcherOutgoing = IntfMatcherFactory.getInstance().getExternalMatcher();

    private RouterStatisticEvent statisticEvent = new RouterStatisticEvent();

    RouterStatisticManager(NodeContext tctx)
    {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return (this.statisticEvent = new RouterStatisticEvent());
    }

    void incrRouterSessions()
    {
        this.statisticEvent.incrRouterSessions();
    }

    void incrRedirect(Protocol protocol, IPNewSessionRequest request)
    {
        LocalUvmContext uc = LocalUvmContextFactory.context();

        /* XXX Incoming/outgoing is all wrong */
        boolean isOutgoing = matcherIncoming.isMatch(request.clientIntf(), request.serverIntf());

        if (protocol == Protocol.TCP) {
            if (isOutgoing) incrTcpOutgoingRedirects();
            else              incrTcpIncomingRedirects();
        } else {
            /* XXX ICMP Hack */
            if ((request.clientPort() == 0) && (request.serverPort() == 0)) {
                /* Ping Sessions */
                if (isOutgoing) incrIcmpOutgoingRedirects();
                else              incrIcmpIncomingRedirects();
            } else {
                /* UDP Sessions */
                if (isOutgoing) incrUdpOutgoingRedirects();
                else              incrUdpIncomingRedirects();
            }
        }
    }

    void incrTcpIncomingRedirects()
    {
        this.statisticEvent.incrTcpIncomingRedirects();
    }

    void incrTcpOutgoingRedirects()
    {
        this.statisticEvent.incrTcpOutgoingRedirects();
    }

    void incrUdpIncomingRedirects()
    {
        this.statisticEvent.incrUdpIncomingRedirects();
    }

    void incrUdpOutgoingRedirects()
    {
        this.statisticEvent.incrUdpOutgoingRedirects();
    }

    void incrIcmpIncomingRedirects()
    {
        this.statisticEvent.incrIcmpIncomingRedirects();
    }

    void incrIcmpOutgoingRedirects()
    {
        this.statisticEvent.incrIcmpOutgoingRedirects();
    }

    void incrDmzSessions()
    {
        this.statisticEvent.incrDmzSessions();
    }
}
