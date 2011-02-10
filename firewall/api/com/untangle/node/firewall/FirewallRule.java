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
import javax.persistence.MappedSuperclass;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_firewall_rule", schema="settings")
@SuppressWarnings("serial")
public class FirewallRule extends Rule
{
    
    private static final String ACTION_BLOCK     = "Block";
    private static final String ACTION_PASS      = "Pass";

    private static final String[] ACTION_ENUMERATION = { ACTION_BLOCK, ACTION_PASS };

    private boolean isTrafficBlocker;


    /* True if this matches source interface */
    private IntfMatcher srcIntf = IntfMatcher.getAnyMatcher();

    /* True if this matches the destination interface */
    private IntfMatcher dstIntf = IntfMatcher.getAnyMatcher();

    private ProtocolDBMatcher protocol;

    private IPMatcher   srcAddress;
    private IPMatcher   dstAddress;

    private PortMatcher srcPort;
    private PortMatcher dstPort;
    
    // constructors -----------------------------------------------------------

    public FirewallRule() { }

    public FirewallRule(boolean isLive, ProtocolDBMatcher protocol,
                        IntfMatcher clientIface, IntfMatcher serverIface,
                        IPMatcher srcAddress, IPMatcher dstAddress,
                        PortMatcher srcPort, PortMatcher dstPort,
                        boolean isTrafficBlocker)
    {
        /* Attributes of the firewall */
        this.isTrafficBlocker = isTrafficBlocker;

        this.srcIntf = srcIntf;
        this.dstIntf = dstIntf;

        setLive( isLive );
        this.protocol   = protocol;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.srcPort    = srcPort;
        this.dstPort    = dstPort;
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

    /**
     * source IntfMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_intf_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getSrcIntf()
    {
        return srcIntf;
    }

    public void setSrcIntf( IntfMatcher srcIntf )
    {
        this.srcIntf = srcIntf;
    }

    /**
     * destination IntfMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="dst_intf_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IntfMatcherUserType")
    public IntfMatcher getDstIntf()
    {
        return dstIntf;
    }

    public void setDstIntf( IntfMatcher dstIntf )
    {
        this.dstIntf = dstIntf;
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     */
    @Column(name="protocol_matcher")
    @Type(type="com.untangle.uvm.type.firewall.ProtocolMatcherUserType")
    public ProtocolDBMatcher getProtocol()
    {
        return protocol;
    }

    public void setProtocol( ProtocolDBMatcher protocol )
    {
        this.protocol = protocol;
    }

    /**
     * source IPMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getSrcAddress()
    {
        return srcAddress;
    }

    public void setSrcAddress( IPMatcher srcAddress )
    {
        this.srcAddress = srcAddress;
    }

    /**
     * destination IPMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="dst_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPMatcher getDstAddress()
    {
        return dstAddress;
    }

    public void setDstAddress( IPMatcher dstAddress )
    {
        this.dstAddress = dstAddress;
    }

    /**
     * source PortMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_port_matcher")
    @Type(type="com.untangle.uvm.type.firewall.PortMatcherUserType")
    public PortMatcher getSrcPort()
    {
        return srcPort;
    }

    public void setSrcPort( PortMatcher srcPort )
    {
        this.srcPort = srcPort;
    }

    /**
     * destination PortMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="dst_port_matcher")
    @Type(type="com.untangle.uvm.type.firewall.PortMatcherUserType")
    public PortMatcher getDstPort()
    {
        return dstPort;
    }

    public void setDstPort( PortMatcher dstPort )
    {
        this.dstPort = dstPort;
    }
}
