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

package com.untangle.uvm.node.firewall;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.port.PortDBMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolDBMatcher;

/**
 * Rule for matching sessions based on direction and IP addresses, ports
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class TrafficDirectionRule extends TrafficRule
{
    private static final long serialVersionUID = -153406452136543289L;

    /* User string for both directions */
    private static final String DIRECTION_BOTH = "Inbound & Outbound";

    /* User string for inbound traffic. */
    private static final String DIRECTION_IN   = "Inbound";

    /* User string for outbout traffic. */
    private static final String DIRECTION_OUT  = "Outbound";

    /* Enumeration of all of the possible user directions */
    private static final String[] DIRECTION_ENUMERATION
        = { DIRECTION_BOTH, DIRECTION_IN, DIRECTION_OUT };

    /* True if this matches inbound sessions */
    private boolean inbound = true;

    /* True if this matches outbound sessions */
    private boolean outbound = true;

    /**
     * Hibernate constructor.
     */
    public TrafficDirectionRule() { }

    public TrafficDirectionRule( boolean       isLive,     ProtocolDBMatcher protocol,
                                 boolean       inbound,    boolean         outbound,
                                 IPDBMatcher   srcAddress, IPDBMatcher     dstAddress,
                                 PortDBMatcher srcPort,    PortDBMatcher   dstPort )
    {
        super( isLive, protocol, srcAddress, dstAddress, srcPort, dstPort );
        this.inbound  = inbound;
        this.outbound = outbound;
    }

    /**
     * match inbound sessions
     *
     * @return true if this matches inbound sessions.
     */
    @Column(nullable=false)
    public boolean getInbound()
    {
        return inbound;
    }

    public void setInbound( boolean inbound )
    {
        this.inbound = inbound;
    }

    /**
     * match outbound sessions.
     *
     * @return the destination IP matcher.
     */
    @Column(nullable=false)
    public boolean getOutbound()
    {
        return this.outbound;
    }

    public void setOutbound( boolean outbound )
    {
        this.outbound = outbound;
    }

    /**
     * Utility method for the user interface to retrieve the user
     * string for the direction.
     *
     * @return the user string for the direction.
     */
    @Transient
    public String getDirection()
    {
        if ( outbound && inbound ) {
            return DIRECTION_BOTH;
        } else if ( outbound ) {
            return DIRECTION_OUT;
        } else if ( inbound ) {
            return DIRECTION_IN;
        }

        /* Go back to the default */
        inbound = true;
        outbound = true;
        return DIRECTION_BOTH;
    }

    /**
     * Set the direction using the user string.
     *
     * @param direction User string for the direction.
     */
    public void setDirection( String direction ) throws ParseException
    {
        if ( direction.equalsIgnoreCase( DIRECTION_BOTH )) {
            setOutbound( true );
            setInbound( true );
        } else if ( direction.equalsIgnoreCase( DIRECTION_IN )) {
            setOutbound( false );
            setInbound( true );
        } else if ( direction.equalsIgnoreCase( DIRECTION_OUT )) {
            setOutbound( true );
            setInbound( false );
        } else {
            throw new ParseException( "Invalid direction: " + direction );
        }
    }

    /**
     * Get all of the possible user string for direction matcher.
     */
    @Transient
    public static String[] getDirectionEnumeration()
    {
        return DIRECTION_ENUMERATION;
    }

    /**
     * Get the user string for the default direction manager.
     */
    @Transient
    public static String getDirectionDefault()
    {
        return DIRECTION_ENUMERATION[0];
    }
}
