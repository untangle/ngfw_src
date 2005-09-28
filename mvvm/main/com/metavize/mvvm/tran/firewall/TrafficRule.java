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

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
abstract class TrafficRule extends Rule
{
    private static final long serialVersionUID = -3950973798403822835L;

    private ProtocolMatcher protocol;

    private IPMatcher   srcAddress;
    private IPMatcher   dstAddress;

    private PortMatcher srcPort;
    private PortMatcher dstPort;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public TrafficRule()
    {
    }

    public TrafficRule( boolean     isLive,  ProtocolMatcher protocol, 
                        IPMatcher   srcAddress, IPMatcher       dstAddress,
                        PortMatcher srcPort,    PortMatcher     dstPort )
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
        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
            this.srcPort = PortMatcher.MATCHER_PING;
            this.dstPort = PortMatcher.MATCHER_PING;
        } else if ( this.srcPort.equals( PortMatcher.MATCHER_PING ) || 
                    this.dstPort.equals( PortMatcher.MATCHER_PING )) {
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
     * source IPMatcher
     *
     * @return the source IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="SRC_IP_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IPMatcherUserType"
     * @hibernate.column
     * name="DST_IP_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="SRC_PORT_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.PortMatcherUserType"
     * @hibernate.column
     * name="DST_PORT_MATCHER"
     */
    public PortMatcher getDstPort()
    {
        return dstPort;
    }

    public void setDstPort( PortMatcher dstPort )
    {
        this.dstPort = dstPort;
    }
}
