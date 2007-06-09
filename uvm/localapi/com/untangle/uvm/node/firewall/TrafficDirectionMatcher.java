/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.node.firewall;

import java.net.InetAddress;

import com.untangle.uvm.tapi.Protocol;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.IPSessionDesc;

import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;


public abstract class TrafficDirectionMatcher extends TrafficMatcher 
{
    private final DirectionMatcher direction;

    public TrafficDirectionMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                                    DirectionMatcher direction,
                                    IPMatcher   srcAddress, IPMatcher       dstAddress,
                                    PortMatcher srcPort,    PortMatcher     dstPort )
    {
        super( isEnabled, protocol, srcAddress, dstAddress, srcPort, dstPort );
        this.direction  = direction;
    }

    protected TrafficDirectionMatcher( TrafficDirectionRule rule )
    {
        super( rule );
        this.direction = DirectionMatcher.getInstance( rule.getInbound(), rule.getOutbound());
    }
    
    public boolean isMatch( IPSessionDesc session, Protocol protocol )
    {
        return ( isMatchDirection( session.isInbound()) && super.isMatch( session, protocol ));
    }

    public boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        return ( isMatchDirection( request.isInbound()) && super.isMatch( request, protocol ));
    }
    
    public boolean isMatch( Protocol protocol, boolean isInbound, InetAddress srcAddress, 
                            InetAddress dstAddress, int srcPort, int dstPort )
    {
        return ( isMatchDirection( isInbound ) && 
                 super.isMatch( protocol, srcAddress, dstAddress, srcPort, dstPort ));
    }

    public boolean isMatchDirection( boolean isInbound )
    {
        return this.direction.isMatch( isInbound );
    }
}
