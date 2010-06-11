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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.TrafficIntfRule;
import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.port.PortDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolDBMatcher;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_firewall_rule", schema="settings")
@SuppressWarnings("serial")
public class FirewallRule extends TrafficIntfRule
{
    
    private static final String ACTION_BLOCK     = "Block";
    private static final String ACTION_PASS      = "Pass";

    private static final String[] ACTION_ENUMERATION = { ACTION_BLOCK, ACTION_PASS };

    private boolean isTrafficBlocker;

    // constructors -----------------------------------------------------------

    public FirewallRule() { }

    public FirewallRule(boolean isLive, ProtocolDBMatcher protocol,
                        IntfDBMatcher clientIface, IntfDBMatcher serverIface,
                        IPDBMatcher srcAddress, IPDBMatcher dstAddress,
                        PortDBMatcher srcPort, PortDBMatcher dstPort,
                        boolean isTrafficBlocker)
    {
        super(isLive, protocol, clientIface, serverIface,
              srcAddress, dstAddress, srcPort, dstPort);

        /* Attributes of the firewall */
        this.isTrafficBlocker = isTrafficBlocker;
    }


    // accessors --------------------------------------------------------------

    /**
     * Does this rule block traffic or let it pass.
     *
     * @return if this rule blocks traffic.
     */
    @Column(name="is_traffic_blocker", nullable=false)
    public boolean isTrafficBlocker()
    {
        return isTrafficBlocker;
    }

    public void setTrafficBlocker( boolean isTrafficBlocker )
    {
        this.isTrafficBlocker = isTrafficBlocker;
    }

    @Transient
    public  String getAction()
    {
        return ( isTrafficBlocker ) ? ACTION_BLOCK : ACTION_PASS;
    }

    public  void setAction( String action ) throws ParseException
    {
        if ( action.equalsIgnoreCase( ACTION_BLOCK )) {
            isTrafficBlocker = true;
        } else if ( action.equalsIgnoreCase( ACTION_PASS )) {
            isTrafficBlocker = false;
        } else {
            throw new ParseException( "Invalid action: " + action );
        }
    }

    @Transient
    public static String[] getActionEnumeration()
    {
        return ACTION_ENUMERATION;
    }

    public static String getActionDefault()
    {
        return ACTION_ENUMERATION[0];
    }
}
