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

/**
 * Rule for matching sessions based on direction and IP addresses, ports
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
public abstract class TrafficIntfRule extends TrafficRule
{
    private static final long serialVersionUID = -3414932048560925028L;

    /* True if this matches inbound sessions */
    private IntfMatcher srcIntf = IntfMatcher.getAll();
    
    /* True if this matches outbound sessions */
    private IntfMatcher dstIntf = IntfMatcher.getAll();

    /**
     * Hibernate constructor.
     */
    public TrafficIntfRule()
    {
    }

    public TrafficIntfRule( boolean     isLive,     ProtocolMatcher protocol,
                            IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                            IPMatcher   srcAddress, IPMatcher       dstAddress,
                            PortMatcher srcPort,    PortMatcher     dstPort )
    {
        super( isLive, protocol, srcAddress, dstAddress, srcPort, dstPort );
        this.srcIntf = srcIntf;
        this.dstIntf = dstIntf;
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
}
