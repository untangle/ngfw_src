/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.metavize.mvvm.tran.firewall.intf.IntfDBMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.ip.IPDBMatcher;
import com.metavize.mvvm.tran.firewall.port.PortDBMatcher;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolDBMatcher;
import org.hibernate.annotations.Type;

/**
 * Rule for matching sessions based on direction and IP addresses,
 * ports.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class TrafficIntfRule extends TrafficRule
{
    // XXXX private static final long serialVersionUID = -3414932048560925028L;

    /* True if this matches source interface */
    private IntfDBMatcher srcIntf = IntfMatcherFactory.getInstance()
        .getAllMatcher();

    /* True if this matches the destination interface */
    private IntfDBMatcher dstIntf = IntfMatcherFactory.getInstance()
        .getAllMatcher();

    public TrafficIntfRule() { }

    public TrafficIntfRule(boolean isLive, ProtocolDBMatcher protocol,
                           IntfDBMatcher srcIntf, IntfDBMatcher dstIntf,
                           IPDBMatcher srcAddress, IPDBMatcher dstAddress,
                           PortDBMatcher srcPort, PortDBMatcher dstPort)
    {
        super(isLive, protocol, srcAddress, dstAddress, srcPort, dstPort);
        this.srcIntf = srcIntf;
        this.dstIntf = dstIntf;
    }

    /**
     * source IntfMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_intf_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IntfMatcherUserType")
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
     */
    @Column(name="dst_intf_matcher")
    @Type(type="com.metavize.mvvm.type.firewall.IntfMatcherUserType")
    public IntfDBMatcher getDstIntf()
    {
        return dstIntf;
    }

    public void setDstIntf( IntfDBMatcher dstIntf )
    {
        this.dstIntf = dstIntf;
    }
}
