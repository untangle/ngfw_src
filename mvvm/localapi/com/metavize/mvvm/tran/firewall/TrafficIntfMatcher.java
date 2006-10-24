/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.net.InetAddress;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.IPSessionDesc;

import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcher;


public abstract class TrafficIntfMatcher extends TrafficMatcher 
{    
    final IntfMatcher srcIntf;
    final IntfMatcher dstIntf;
    
    public TrafficIntfMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                               IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                               IPMatcher   srcAddress, IPMatcher       dstAddress,
                               PortMatcher srcPort,    PortMatcher     dstPort )
    {
        super( isEnabled, protocol, srcAddress, dstAddress, srcPort, dstPort );
        this.srcIntf  = srcIntf;
        this.dstIntf  = dstIntf;
    }

    protected TrafficIntfMatcher( TrafficIntfRule rule )
    {
        super( rule );
        this.srcIntf = rule.getSrcIntf();
        this.dstIntf = rule.getDstIntf();
    }
    
    public boolean isMatch( IPSessionDesc session, Protocol protocol )
    {
        return ( isMatchIntf( session.clientIntf(), session.serverIntf()) && 
                 super.isMatch( session, protocol )); 
    }

    public boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        return ( isMatchIntf( request.clientIntf(), request.originalServerIntf()) && 
                 super.isMatch( request, protocol ));     
    }
    
    public boolean isMatch( Protocol protocol, byte srcIntf, byte dstIntf, 
                            InetAddress srcAddress, InetAddress dstAddress, 
                            int srcPort, int dstPort )                            
    {
        return ( isMatchIntf( srcIntf, dstIntf ) && 
                 super.isMatch( protocol, srcAddress, dstAddress, srcPort, dstPort ));
    }

    public boolean isMatchIntf( byte src, byte dst )
    {
        return this.srcIntf.isMatch( src ) && this.dstIntf.isMatch( dst );
    }
}
