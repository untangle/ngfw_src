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

package com.untangle.mvvm.tran.firewall;

import java.net.InetAddress;

 import org.apache.log4j.Logger;

import com.untangle.mvvm.tapi.IPNewSessionRequest;
import com.untangle.mvvm.tapi.IPSessionDesc;
import com.untangle.mvvm.tapi.Protocol;
import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;

/**
 * A class for matching Traffic
 *   This is cannot be squashed into a RedirectRule because all of its elements are final.
 *   This is a property which is not possible in hibernate objects.
 */
abstract class TrafficMatcher
{
    private final Logger logger = Logger.getLogger(getClass());

    private final boolean isEnabled;

    /* Package protected so the InterfaceRedirect has access to these variables */
    private final ProtocolMatcher protocol;

    private final IPMatcher   srcAddress;
    private final IPMatcher   dstAddress;

    private final PortMatcher srcPort;
    private final PortMatcher dstPort;

    // XXX For the future
    // TimeMatcher time;
    protected TrafficMatcher( boolean     isEnabled,  ProtocolMatcher protocol,
                              IPMatcher   srcAddress, IPMatcher       dstAddress,
                              PortMatcher srcPort,    PortMatcher     dstPort )
    {
        this.isEnabled  = isEnabled;
        this.protocol   = protocol;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;

        /* Ports are ignored for PING sessions */
        /* XXX ICMP Hack moved to the matching algorithm, before it was inside of the port matchers */
        this.srcPort       = srcPort;
        this.dstPort       = dstPort;
    }

    protected TrafficMatcher( TrafficRule rule )
    {
        this( rule.isLive(), rule.getProtocol(),
              rule.getSrcAddress(), rule.getDstAddress(), rule.getSrcPort(), rule.getDstPort());
    }

    public boolean isEnabled()
    {
        return isEnabled;
    }

    protected boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        boolean isMatch =
            isEnabled &&
            isMatchProtocol( protocol, request.clientPort(), request.serverPort()) &&
            isMatchAddress( request.clientAddr(), request.serverAddr()) &&
            isTimeMatch();

        if ( isMatch && logger.isDebugEnabled())
            logger.debug( "Matched: " + request + " the session " );

        return isMatch;

    }

    protected boolean isMatch( IPSessionDesc session, Protocol protocol )
    {
        return ( isEnabled &&
                 isMatchProtocol( protocol, session.clientPort(), session.serverPort()) &&
                 isMatchAddress( session.clientAddr(), session.serverAddr()) &&
                 isTimeMatch());
    }

    protected boolean isMatch( Protocol protocol, InetAddress srcAddress, InetAddress dstAddress,
                               int srcPort, int dstPort )
    {
        return ( isEnabled &&
                 isMatchProtocol( protocol, srcPort, dstPort ) &&
                 isMatchAddress( srcAddress, dstAddress) &&
                 isTimeMatch());
    }


    public boolean isMatchProtocol( Protocol protocol, int srcPort, int dstPort )
    {
        /* XXX ICMP Hack */
        if (( srcPort == 0 ) && ( dstPort == 0 ) && this.protocol.isMatch( Protocol.ICMP )) return true;

        /* Otherwise the port and protocol must match */
        return ( this.protocol.isMatch( protocol ) && isMatchPort( srcPort, dstPort ));
    }

    public boolean isMatchAddress( InetAddress src, InetAddress dst )
    {
        return ( this.srcAddress.isMatch( src ) && this.dstAddress.isMatch( dst ));
    }

    public boolean isMatchPort( int src, int dst )
    {
        return ( this.srcPort.isMatch( src ) && this.dstPort.isMatch( dst ));
    }

    /* Unused for now */
    public boolean isTimeMatch()
    {
        return true;
    }
}
