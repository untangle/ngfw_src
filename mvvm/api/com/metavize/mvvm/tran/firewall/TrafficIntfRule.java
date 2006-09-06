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

import com.metavize.mvvm.tran.firewall.ip.IPDBMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfDBMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.port.PortDBMatcher;

/**
 * Rule for matching sessions based on direction and IP addresses, ports
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
public abstract class TrafficIntfRule extends TrafficRule
{
    private static final long serialVersionUID = -3414932048560925028L;

    /* True if this matches source interface */
    private IntfDBMatcher srcIntf = IntfMatcherFactory.getInstance().getAllMatcher();
    
    /* True if this matches the destination interface */
    private IntfDBMatcher dstIntf = IntfMatcherFactory.getInstance().getAllMatcher();

    /**
     * Hibernate constructor.
     */
    public TrafficIntfRule()
    {
    }

    public TrafficIntfRule( boolean       isLive,     ProtocolMatcher protocol,
                            IntfDBMatcher srcIntf,    IntfDBMatcher     dstIntf,
                            IPDBMatcher   srcAddress, IPDBMatcher       dstAddress,
                            PortDBMatcher srcPort,    PortDBMatcher     dstPort )
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
    public IntfDBMatcher getSrcIntf()
    {
        return srcIntf;
    }

    public void setSrcIntf( IntfDBMatcher srcIntf )
    {
        this.srcIntf = srcIntf;
    }
    
    /**
     * destination IntfDBMatcher
     *
     * @return the destination IP matcher.
     * @hibernate.property
     * type="com.metavize.mvvm.type.firewall.IntfMatcherUserType"
     * @hibernate.column
     * name="DST_INTF_MATCHER"
     */
    public IntfDBMatcher getDstIntf()
    {
        return dstIntf;
    }

    public void setDstIntf( IntfDBMatcher dstIntf )
    {
        this.dstIntf = dstIntf;
    }
}
