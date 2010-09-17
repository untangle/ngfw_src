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

import com.untangle.uvm.node.InterfaceComparator;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.VnetSessionDesc;
import com.untangle.uvm.vnet.Protocol;


public abstract class TrafficIntfMatcher extends TrafficMatcher
{
    final IntfMatcher srcIntf;
    final IntfMatcher dstIntf;

    public TrafficIntfMatcher(boolean isEnabled, ProtocolMatcher protocol,
                              IntfMatcher srcIntf, IntfMatcher dstIntf,
                              IPMatcher srcAddress, IPMatcher dstAddress,
                              PortMatcher srcPort, PortMatcher dstPort)
    {
        super(isEnabled, protocol, srcAddress, dstAddress, srcPort, dstPort);
        this.srcIntf  = srcIntf;
        this.dstIntf  = dstIntf;
    }

    protected TrafficIntfMatcher(TrafficIntfRule rule)
    {
        super(rule);
        this.srcIntf = rule.getSrcIntf();
        this.dstIntf = rule.getDstIntf();
    }

    public boolean isMatch(VnetSessionDesc session, Protocol protocol,
                           InterfaceComparator c)
    {
        return (isMatchIntf(session.clientIntf(), session.serverIntf(), c) &&
                super.isMatch(session, protocol));
    }

    public boolean isMatch(IPNewSessionRequest request, Protocol protocol,
                           InterfaceComparator c)
    {
        return isMatchIntf(request.clientIntf(), request.serverIntf(), c)
            && super.isMatch(request, protocol);
    }

    public boolean isMatch(Protocol protocol, byte srcIntf, byte dstIntf,
                           InetAddress srcAddress, InetAddress dstAddress,
                           int srcPort, int dstPort, InterfaceComparator c)
    {
        return (isMatchIntf(srcIntf, dstIntf, c) && super.isMatch(protocol, srcAddress, dstAddress, srcPort, dstPort));
    }

    public boolean isMatchIntf(byte src, byte dst, InterfaceComparator c)
    {
        return this.srcIntf.isMatch(src, dst, c) && this.dstIntf.isMatch(dst, src, c);
    }
}
