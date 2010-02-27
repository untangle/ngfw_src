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

import com.untangle.uvm.node.firewall.TrafficIntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;

/**
 * A class for matching redirects. This is cannot be squashed into a
 * FirewallRule because all of its elements are final.  This is a
 * property which is not possible in hibernate objects.
 */
class FirewallMatcher extends TrafficIntfMatcher {
    /* Used for logging */
    private final FirewallRule rule;
    private final int ruleIndex;

    private final boolean isTrafficBlocker;

    public FirewallMatcher(boolean isEnabled, ProtocolMatcher protocol,
                           IntfMatcher srcIntf, IntfMatcher dstIntf,
                           IPMatcher srcAddress, IPMatcher dstAddress,
                           PortMatcher srcPort, PortMatcher dstPort,
                           boolean isTrafficBlocker )
    {
        super(isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress,
              srcPort, dstPort);

        /* Attributes of the firewall rule */
        this.isTrafficBlocker = isTrafficBlocker;

        /* XXX probably want to set this to a more creative value, or
         * just get rid of this constructor it is never used */
        this.rule      = null;
        this.ruleIndex = 0;
    }

    FirewallMatcher(FirewallRule rule, int ruleIndex)
    {
        super(rule);

        this.rule      = rule;
        this.ruleIndex = ruleIndex;

        /* Attributes of the redirect */
        isTrafficBlocker = rule.isTrafficBlocker();
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
}
