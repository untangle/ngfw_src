/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.io.Serializable;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.ParseException;

import com.metavize.mvvm.tran.firewall.ip.IPDBMatcher;
import com.metavize.mvvm.tran.firewall.port.PortDBMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
abstract class TrafficRule extends Rule
{
    // !!! private static final long serialVersionUID = -3950973798403822835L;

    private ProtocolMatcher protocol;

    private IPDBMatcher   srcAddress;
    private IPDBMatcher   dstAddress;

    private PortDBMatcher srcPort;
    private PortDBMatcher dstPort;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public TrafficRule()
    {
    }

    public TrafficRule( boolean       isLive,     ProtocolMatcher protocol, 
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
        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.ProtocolMatcherUserType"
     * @hibernate.column
     * name="PROTOCOL_MATCHER"
     */
    public ProtocolMatcher getProtocol()
    {
        return protocol;
    }

    public void setProtocol( ProtocolMatcher protocol )
    {
        this.protocol = protocol;
    }
        
    /**
     * source IPDBMatcher
     *
     * @return the source IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="SRC_IP_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="DST_IP_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="SRC_PORT_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="DST_PORT_MATCHER"
     */
    public PortDBMatcher getDstPort()
    {
        return dstPort;
    }

    public void setDstPort( PortDBMatcher dstPort )
    {
        this.dstPort = dstPort;
    }
}
