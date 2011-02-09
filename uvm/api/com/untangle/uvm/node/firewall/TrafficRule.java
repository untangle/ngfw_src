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

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

/**
 * Rule for matching sessions based on session protocol, address and
 * port.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
@MappedSuperclass
abstract class TrafficRule extends Rule
{

    private ProtocolDBMatcher protocol;

    private IPDBMatcher   srcAddress;
    private IPDBMatcher   dstAddress;

    private PortMatcher srcPort;
    private PortMatcher dstPort;

    // constructors -----------------------------------------------------------

    public TrafficRule() { }

    public TrafficRule( boolean       isLive,     ProtocolDBMatcher protocol,
                        IPDBMatcher   srcAddress, IPDBMatcher     dstAddress,
                        PortMatcher srcPort,      PortMatcher   dstPort )
    {
        setLive( isLive );
        this.protocol   = protocol;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.srcPort    = srcPort;
        this.dstPort    = dstPort;
    }

    // accessors --------------------------------------------------------------

    /* Hack that sets the ports to zero for Ping sessions */

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
     * source IPDBMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getSrcAddress()
    {
        return srcAddress;
    }

    public void setSrcAddress( IPDBMatcher srcAddress )
    {
        this.srcAddress = srcAddress;
    }

    /**
     * destination IPDBMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="dst_ip_matcher")
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getDstAddress()
    {
        return dstAddress;
    }

    public void setDstAddress( IPDBMatcher dstAddress )
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
