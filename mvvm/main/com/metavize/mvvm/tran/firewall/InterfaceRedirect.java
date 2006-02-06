/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;

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

    public abstract byte netcapIntf( byte argonDstIntf );

    public abstract byte argonIntf( byte argonDstIntf );
}
