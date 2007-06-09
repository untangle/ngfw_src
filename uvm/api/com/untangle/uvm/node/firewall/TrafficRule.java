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

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.Rule;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolDBMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;

/**
 * Rule for matching sessions based on session protocol, address and
 * port.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@MappedSuperclass
abstract class TrafficRule extends Rule
{
    private static final long serialVersionUID = 3300082570569262876L;

    private ProtocolDBMatcher protocol;

    private IPDBMatcher   srcAddress;
    private IPDBMatcher   dstAddress;

    private PortDBMatcher srcPort;
    private PortDBMatcher dstPort;

    // constructors -----------------------------------------------------------

    public TrafficRule() { }

    public TrafficRule( boolean       isLive,     ProtocolDBMatcher protocol,
                        IPDBMatcher   srcAddress, IPDBMatcher     dstAddress,
                        PortDBMatcher srcPort,    PortDBMatcher   dstPort )
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
    public void fixPing() throws ParseException
    {
        PortDBMatcher pingMatcher = PortMatcherFactory.getInstance().getPingMatcher();
        if ( this.protocol.equals( ProtocolMatcherFactory.getInstance().getPingMatcher())) {
            this.srcPort = pingMatcher;
            this.dstPort = pingMatcher;
        } else if ( this.srcPort.equals( pingMatcher ) || this.dstPort.equals( pingMatcher )) {
            throw new ParseException( "Invalid port for a non-ping traffic type" );
        }
    }

    /**
     * Protocol matcher
     *
     * @return the protocol matcher.
     */
    @Column(name="protocol_matcher")
    @Type(type="com.untangle.mvvm.type.firewall.ProtocolMatcherUserType")
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
    @Type(type="com.untangle.mvvm.type.firewall.IPMatcherUserType")
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
    @Type(type="com.untangle.mvvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getDstAddress()
    {
        return dstAddress;
    }

    public void setDstAddress( IPDBMatcher dstAddress )
    {
        this.dstAddress = dstAddress;
    }

    /**
     * source PortDBMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_port_matcher")
    @Type(type="com.untangle.mvvm.type.firewall.PortMatcherUserType")
    public PortDBMatcher getSrcPort()
    {
        return srcPort;
    }

    public void setSrcPort( PortDBMatcher srcPort )
    {
        this.srcPort = srcPort;
    }

    /**
     * destination PortDBMatcher
     *
     * @return the destination IP matcher.
     */
    @Column(name="dst_port_matcher")
    @Type(type="com.untangle.mvvm.type.firewall.PortMatcherUserType")
    public PortDBMatcher getDstPort()
    {
        return dstPort;
    }

    public void setDstPort( PortDBMatcher dstPort )
    {
        this.dstPort = dstPort;
    }
}
