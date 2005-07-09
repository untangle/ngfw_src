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

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.IPSessionDesc;

/**
 * A class for matching Traffic
 *   This is cannot be squashed into a RedirectRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
public class TrafficMatcher {
    private static final Logger logger = Logger.getLogger( TrafficMatcher.class );
        
    private final boolean isEnabled;
        
    private final ProtocolMatcher protocol;

    private final IntfMatcher srcIntf;
    private final IntfMatcher dstIntf;

    private final IPMatcher   srcAddress;
    private final IPMatcher   dstAddress;

    private final PortMatcher srcPort;
    private final PortMatcher dstPort;

    protected final boolean isPingMatcher;

    // XXX For the future
    // TimeMatcher time;
    public TrafficMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                           IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                           IPMatcher   srcAddress, IPMatcher       dstAddress,
                           PortMatcher srcPort,    PortMatcher     dstPort )
    {
        this.isEnabled  = isEnabled;
        this.protocol   = protocol;
        this.srcIntf    = srcIntf;
        this.dstIntf    = dstIntf;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;

        /* Ports are ignored for PING sessions */
        if ( this.protocol.equals( ProtocolMatcher.MATCHER_PING )) {
            this.srcPort       = PortMatcher.MATCHER_PING;
            this.dstPort       = PortMatcher.MATCHER_PING;
            this.isPingMatcher = true;
        } else {
            this.srcPort       = srcPort;
            this.dstPort       = dstPort;
            this.isPingMatcher = false;
        }
    }

    protected TrafficMatcher( TrafficRule rule )
    {
        this( rule.isLive(), rule.getProtocol(), rule.getSrcIntf(), rule.getDstIntf(),
              rule.getSrcAddress(), rule.getDstAddress(), rule.getSrcPort(), rule.getDstPort());
    }
    
    public boolean isEnabled()
    {
        return isEnabled;
    }
    
    public boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        boolean isMatch = 
            isEnabled && 
            isMatchProtocol( protocol ) &&
            isMatchIntf( request.clientIntf(), request.serverIntf()) &&
            isMatchAddress( request.clientAddr(), request.serverAddr()) &&
            isMatchPort( request.clientPort(), request.serverPort()) &&
            isTimeMatch();

        if ( isMatch )
            logger.debug( "Matched: " + request + " the session " );

        return isMatch;
    
    }

    public boolean isMatch( IPSessionDesc session, Protocol protocol )
    {
        return ( isEnabled && 
                 isMatchProtocol( protocol ) &&
                 isMatchIntf( session.clientIntf(), session.serverIntf()) &&
                 isMatchAddress( session.clientAddr(), session.serverAddr()) &&
                 isMatchPort( session.clientPort(), session.serverPort()) &&
                 isTimeMatch());
    }

    public boolean isMatchProtocol( Protocol protocol ) 
    {
        return this.protocol.isMatch( protocol );
    }
    
    public boolean isMatchIntf( byte src, byte dst )
    {
        return ( this.srcIntf.isMatch( src ) && this.dstIntf.isMatch( dst ));
    }
    
    public boolean isMatchAddress( InetAddress src, InetAddress dst ) 
    {
        return ( this.srcAddress.isMatch( src ) && this.dstAddress.isMatch( dst ));
    }
    
    public boolean isMatchPort( int src, int dst )
    {
        return ( this.srcPort.isMatch( src ) && this.dstPort.isMatch( dst ));
    }
    
    /* Unused for now */
    public boolean isTimeMatch()
    {
        return true;
    }
}
