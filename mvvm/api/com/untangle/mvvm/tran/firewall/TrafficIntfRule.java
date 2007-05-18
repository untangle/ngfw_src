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

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.untangle.mvvm.tran.firewall.intf.IntfDBMatcher;
import com.untangle.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.untangle.mvvm.tran.firewall.ip.IPDBMatcher;
import com.untangle.mvvm.tran.firewall.port.PortDBMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolDBMatcher;
import org.hibernate.annotations.Type;

/**
 * Rule for matching sessions based on interfaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class TrafficIntfRule extends TrafficRule
{
    private static final long serialVersionUID = 679729134715419983L;

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
     * source IntfDBMatcher
     *
     * @return the source IP matcher.
     */
    @Column(name="src_intf_matcher")
    @Type(type="com.untangle.mvvm.type.firewall.IntfMatcherUserType")
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
    @Type(type="com.untangle.mvvm.type.firewall.IntfMatcherUserType")
    public IntfDBMatcher getDstIntf()
    {
        return dstIntf;
    }

    public void setDstIntf( IntfDBMatcher dstIntf )
    {
        this.dstIntf = dstIntf;
    }
}
