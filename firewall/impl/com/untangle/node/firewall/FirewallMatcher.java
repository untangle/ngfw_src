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

import org.apache.log4j.Logger;

import com.untangle.uvm.node.IntfMatcher;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.node.PortMatcher;
import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.vnet.Protocol;

/**
 * A class for matching redirects. This is cannot be squashed into a
 * FirewallRule because all of its elements are final.  This is a
 * property which is not possible in hibernate objects.
 */
public class FirewallMatcher
{
    /* Used for logging */
    private FirewallRule rule;
    private int ruleIndex;

    private final IntfMatcher srcIntf;
    private final IntfMatcher dstIntf;

    private final boolean isTrafficBlocker;

    private final Logger logger = Logger.getLogger(getClass());

    private final boolean isEnabled;

    /* Package protected so the InterfaceRedirect has access to these variables */
    private final ProtocolMatcher protocol;

    private final IPMatcher   srcAddress;
    private final IPMatcher   dstAddress;

    private final PortMatcher srcPort;
    private final PortMatcher dstPort;

    
    public FirewallMatcher(boolean isEnabled, ProtocolMatcher protocol,
                           IntfMatcher srcIntf, IntfMatcher dstIntf,
                           IPMatcher srcAddress, IPMatcher dstAddress,
                           PortMatcher srcPort, PortMatcher dstPort,
                           boolean isTrafficBlocker )
    {
        /* Attributes of the firewall rule */
        this.isTrafficBlocker = isTrafficBlocker;

        this.srcIntf = srcIntf;
        this.dstIntf = dstIntf;

        /* XXX probably want to set this to a more creative value, or
         * just get rid of this constructor it is never used */
        this.rule      = null;
        this.ruleIndex = 0;

        this.isEnabled  = isEnabled;
        this.protocol   = protocol;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;

        /* Ports are ignored for PING sessions */
        /* XXX ICMP Hack moved to the matching algorithm, before it was inside of the port matchers */
        this.srcPort       = srcPort;
        this.dstPort       = dstPort;
    }

    FirewallMatcher(FirewallRule rule, int ruleIndex)
    {
        this( rule.isLive(), rule.getProtocol(), rule.getSrcIntf(), rule.getDstIntf(), rule.getSrcAddress(), rule.getDstAddress(), rule.getSrcPort(), rule.getDstPort(), rule.isTrafficBlocker());
        
        this.rule      = rule;
        this.ruleIndex = ruleIndex;
    }

    public boolean isTrafficBlocker()
    {
        return this.isTrafficBlocker;
    }

    public FirewallRule rule()
    {
        return this.rule;
    }

    public int ruleIndex()
    {
        return this.ruleIndex;
    }


    public boolean isMatch( Protocol protocol,
                            int srcIntf, int dstIntf,
                            InetAddress srcAddress, InetAddress dstAddress,
                            int srcPort, int dstPort)
    {
        return (isEnabled &&
                isMatchProtocol( protocol ) &&
                isMatchIntf(srcIntf, dstIntf) && 
                isMatchPort( srcPort, dstPort ) &&
                isMatchAddress( srcAddress, dstAddress));

    }

    public boolean isMatchIntf(int src, int dst)
    {
        return this.srcIntf.isMatch(src) && this.dstIntf.isMatch(dst);
    }

    public boolean isEnabled()
    {
        return isEnabled;
    }

    protected boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        boolean isMatch =
            ( isEnabled &&
              isMatchProtocol( protocol ) &&
              isMatchPort( request.clientPort(), request.serverPort() ) &&
              isMatchAddress( request.clientAddr(), request.serverAddr()) &&
              isMatchIntf(request.clientIntf(), request.serverIntf()) );
        
        if ( isMatch && logger.isDebugEnabled())
            logger.debug( "Matched: " + request + " the session " );

        return isMatch;

    }

    protected boolean isMatch( VnetSessionDesc session, Protocol protocol )
    {
        return ( isEnabled &&
                 isMatchProtocol( protocol ) &&
                 isMatchPort( session.clientPort(), session.serverPort() ) &&
                 isMatchAddress( session.clientAddr(), session.serverAddr()) &&
                 isMatchIntf(session.clientIntf(), session.serverIntf()) );
    }

    public boolean isMatchProtocol( Protocol protocol )
    {
        return ( this.protocol.isMatch( protocol ) );
    }

    public boolean isMatchAddress( InetAddress src, InetAddress dst )
    {
        return ( this.srcAddress.isMatch( src ) && this.dstAddress.isMatch( dst ));
    }

    public boolean isMatchPort( int src, int dst )
    {
        return ( this.srcPort.isMatch( src ) && this.dstPort.isMatch( dst ));
    }
}
