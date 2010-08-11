/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node.firewall;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.IPSessionDesc;
import com.untangle.uvm.vnet.Protocol;

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
        this( rule.isLive(), rule.getProtocol(), rule.getSrcAddress(), rule.getDstAddress(), rule.getSrcPort(), rule.getDstPort());
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

    protected boolean isMatch( Protocol protocol, InetAddress srcAddress, InetAddress dstAddress, int srcPort, int dstPort )
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
