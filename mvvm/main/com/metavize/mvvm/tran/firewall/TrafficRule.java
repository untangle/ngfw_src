/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeRule.java 229 2005-04-07 22:25:00Z amread $
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
public abstract class TrafficRule extends Rule
{
    private static final String DIRECTION_BOTH = "Inbound & Outbound";
    private static final String DIRECTION_IN   = "Inbound";
    private static final String DIRECTION_OUT  = "Outbound";

    private static final String[] DIRECTION_ENUMERATION = { DIRECTION_BOTH, DIRECTION_IN, DIRECTION_OUT };

    private static final long serialVersionUID   =  337184355738935251L;

    private ProtocolMatcher protocol;
    private IntfMatcher srcIntf;
    private IntfMatcher dstIntf;

    private IPMatcher   srcAddress;
    private IPMatcher   dstAddress;

    private PortMatcher srcPort;
    private PortMatcher dstPort;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public TrafficRule() { }

    public TrafficRule( boolean     isLive,  ProtocolMatcher protocol, 
                        IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                        IPMatcher   srcAddress, IPMatcher       dstAddress,
                        PortMatcher srcPort,    PortMatcher     dstPort )
    {
        setLive( isLive );
        this.protocol   = protocol;
        this.srcIntf    = srcIntf;
        this.dstIntf    = dstIntf;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.srcPort    = srcPort;
        this.dstPort    = dstPort;
    }

    // accessors --------------------------------------------------------------

    /* Hack that sets the ports to zero for Ping sessions */
    public final void fixPing() throws ParseException
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

    /**
     * source IntfMatcher
     *
     * @return the source IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IntfMatcherUserType"
     * @hibernate.column
     * name="SRC_INTF_MATCHER"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IntfMatcherUserType"
     * @hibernate.column
     * name="DST_INTF_MATCHER"
     */
    public IntfMatcher getDstIntf()
    {
        return dstIntf;
    }

    public void setDstIntf( IntfMatcher dstIntf )
    {
        this.dstIntf = dstIntf;
    }

    /* ----- */
       
    public void setDirection( String direction ) throws ParseException
    {
        if ( direction.equalsIgnoreCase( DIRECTION_BOTH )) {
            setSrcIntf( IntfMatcher.MATCHER_ALL );
            setDstIntf( IntfMatcher.MATCHER_ALL );
        } else if ( direction.equalsIgnoreCase( DIRECTION_IN )) {
            setSrcIntf( IntfMatcher.MATCHER_OUT );
            setDstIntf( IntfMatcher.MATCHER_ALL );
        } else if ( direction.equalsIgnoreCase( DIRECTION_OUT )) {
            setSrcIntf( IntfMatcher.MATCHER_IN );
            setDstIntf( IntfMatcher.MATCHER_ALL );
        } else {
            throw new ParseException( "Invalid direction: " + direction );
        }
    }

    public String getDirection()
    {
        /* XXX For now just use the src interface */
        if ( srcIntf == null ) {
            srcIntf = IntfMatcher.MATCHER_ALL;
            dstIntf = IntfMatcher.MATCHER_ALL;
            return DIRECTION_BOTH;
        } else {
            if ( srcIntf.isInsideEnabled && srcIntf.isOutsideEnabled ) {
                return DIRECTION_BOTH;
            } else if ( srcIntf.isInsideEnabled ) {
                return DIRECTION_OUT;
            } else if ( srcIntf.isOutsideEnabled ) {
                return DIRECTION_IN;
            }
        }
        /* Restore to default, something has gone wrong */
        srcIntf = IntfMatcher.MATCHER_ALL;
        dstIntf = IntfMatcher.MATCHER_ALL;
        return DIRECTION_BOTH;
    }
    
    public static String[] getDirectionEnumeration()
    {
        return DIRECTION_ENUMERATION;
    }

    public static String getDirectionDefault()
    {
        return DIRECTION_ENUMERATION[0];
    }

}
