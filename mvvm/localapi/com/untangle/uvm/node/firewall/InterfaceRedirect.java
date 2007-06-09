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

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.mvvm.tapi.Protocol;

import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.intf.IntfMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcher;

public abstract class InterfaceRedirect extends TrafficIntfMatcher
{
    protected InterfaceRedirect( ProtocolMatcher protocol, 
                                 IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                                 IPMatcher   srcAddress, IPMatcher       dstAddress,
                                 PortMatcher srcPort,    PortMatcher     dstPort )
    {
        /* InterfaceRedirects are always active */
        super( true, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
    }
    
    protected InterfaceRedirect( TrafficIntfRule trafficRule )
    {
        super( trafficRule );
    }

    public abstract byte argonIntf( byte argonDstIntf );
}
