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

package com.metavize.tran.nat;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.IPSessionDesc;

import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;

import com.metavize.mvvm.tran.firewall.RedirectRule;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a RedirectRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
class RedirectMatcher {
    private static final Logger logger = Logger.getLogger( RedirectMatcher.class );
    
    public static final RedirectMatcher MATCHER_DISABLED = 
        new RedirectMatcher( false, ProtocolMatcher.MATCHER_NIL,
                             IntfMatcher.MATCHER_NIL, IntfMatcher.MATCHER_NIL,
                             IPMatcher.MATCHER_NIL,   IPMatcher.MATCHER_ALL, 
                             PortMatcher.MATCHER_NIL, PortMatcher.MATCHER_NIL,
                             false, null, -1 );

    private final boolean isEnabled;
    
    /* True if this redirect applies to the source. 
     * false if this redirect applies to the destination.*/
    private final boolean isDstRedirect;
    
    private final ProtocolMatcher protocol;

    private final IntfMatcher srcIntf;
    private final IntfMatcher dstIntf;

    private final IPMatcher   srcAddress;
    private final IPMatcher   dstAddress;

    private final PortMatcher srcPort;
    private final PortMatcher dstPort;

    /**
     * Address for the redirect.
     * XXX Will redirect rules allow for both the source and destination to be redirected
     * If so, there should be a 2 addresses and 2 ports.
     */
    private final InetAddress redirectAddress;

    /**
     * Port for the redirect.
     */
    private final int redirectPort;

    // XXX For the future
    // TimeMatcher time;
    public RedirectMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                            IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                            IPMatcher   srcAddress, IPMatcher       dstAddress,
                            PortMatcher srcPort,    PortMatcher     dstPort,
                            boolean isDstRedirect,  InetAddress redirectAddress, int redirectPort )
    {
        this.isEnabled  = isEnabled;
        this.protocol   = protocol;
        this.srcIntf    = srcIntf;
        this.dstIntf    = dstIntf;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.srcPort    = srcPort;
        this.dstPort    = dstPort;

        /* Attributes of the redirect */
        this.isDstRedirect   = isDstRedirect;
        this.redirectAddress = redirectAddress;
        this.redirectPort    = redirectPort;
    }

    RedirectMatcher( RedirectRule rule )
    {
        this.isEnabled  = rule.isLive();
        this.protocol   = rule.getProtocol();
        this.srcIntf    = rule.getSrcIntf();
        this.dstIntf    = rule.getDstIntf();
        this.srcAddress = rule.getSrcAddress();
        this.dstAddress = rule.getDstAddress();
        this.srcPort    = rule.getSrcPort();
        this.dstPort    = rule.getDstPort();

        /* Attributes of the redirect */
        this.isDstRedirect   = rule.isDstRedirect();
        this.redirectAddress = rule.getRedirectAddress().getAddr();
        this.redirectPort    = rule.getRedirectPort();
    }
    
    public boolean isEnabled()
    {
        return isEnabled;
    }
    
    InetAddress getRedirectAddress()
    {
        return redirectAddress;
    }


    int getRedirectPort()
    {
        return redirectPort;
    }

    boolean isMatch( IPNewSessionRequest request, Protocol protocol )
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

    boolean isMatch( IPSessionDesc session, Protocol protocol )
    {
        return ( isEnabled && 
                 isMatchProtocol( protocol ) &&
                 isMatchIntf( session.clientIntf(), session.serverIntf()) &&
                 isMatchAddress( session.clientAddr(), session.serverAddr()) &&
                 isMatchPort( session.clientPort(), session.serverPort()) &&
                 isTimeMatch());
    }

    boolean isMatchProtocol( Protocol protocol ) 
    {
        return this.protocol.isMatch( protocol );
    }
    
    boolean isMatchIntf( byte src, byte dst )
    {
        return ( this.srcIntf.isMatch( src ) && this.dstIntf.isMatch( dst ));
    }
    
    boolean isMatchAddress( InetAddress src, InetAddress dst ) 
    {
        return ( this.srcAddress.isMatch( src ) && this.dstAddress.isMatch( dst ));
    }
    
    boolean isMatchPort( int src, int dst )
    {
        return ( this.srcPort.isMatch( src ) && this.dstPort.isMatch( dst ));
    }
    
    /* Unused for now */
    boolean isTimeMatch()
    {
        return true;
    }

    /** Redirect an IP Session request */
    void redirect( IPNewSessionRequest request )
    {
        logger.debug( "Redirecting: (" + isDstRedirect +")" + request + " to  " + redirectAddress + 
                      ":" + redirectPort );

        
        if ( isDstRedirect ) {
            /* Modify the server */            
            if ( redirectAddress != null ) {
                request.serverAddr( redirectAddress );
            }
            
            if ( redirectPort >= 0 ) {
                request.serverPort( redirectPort );
            }
        } else {
            /* Modify the client */
            if ( redirectAddress != null ) {
                request.clientAddr( redirectAddress );
            }
            
            if ( redirectPort >= 0 ) {
                request.clientPort( redirectPort );
            }
        }
    }
}
