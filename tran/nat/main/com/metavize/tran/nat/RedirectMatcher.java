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
import com.metavize.mvvm.tran.firewall.TrafficMatcher;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a RedirectRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
class RedirectMatcher extends TrafficMatcher {
    private static final Logger logger = Logger.getLogger( RedirectMatcher.class );
    
    public static final RedirectMatcher MATCHER_DISABLED = 
        new RedirectMatcher( false, ProtocolMatcher.MATCHER_NIL,
                             IntfMatcher.getNothing(), IntfMatcher.getNothing(),
                             IPMatcher.MATCHER_NIL,   IPMatcher.MATCHER_ALL, 
                             PortMatcher.MATCHER_NIL, PortMatcher.MATCHER_NIL,
                             false, null, -1 );
    
    /* True if this redirect applies to the source. 
     * false if this redirect applies to the destination.*/
    private final boolean isDstRedirect;
    
    /**
     * Address for the redirect. (null to not redirect)
     * XXX Will redirect rules allow for both the source and destination to be redirected
     * If so, there should be a 2 addresses and 2 ports.
     */
    private final InetAddress redirectAddress;
    
    /**
     * Port for the redirect. (0 to not redirect)
     */
    private final int redirectPort;

    /**
     * Index of the rule this matched
     */
    private final RedirectRule rule;
    private final int ruleIndex;
    
    private RedirectMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                             IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                             IPMatcher   srcAddress, IPMatcher       dstAddress,
                             PortMatcher srcPort,    PortMatcher     dstPort,
                             boolean isDstRedirect,  InetAddress     redirectAddress, int redirectPort,
                             RedirectRule rule,      int             ruleIndex )
    {
        super( isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );
        
        /* Attributes of the redirect */
        this.isDstRedirect   = isDstRedirect;
        this.redirectAddress = redirectAddress;

        if ( this.isPingMatcher ) {
            this.redirectPort = -1;
        } else {
            this.redirectPort = redirectPort;
        }
        
        this.rule      = rule;
        this.ruleIndex = ruleIndex;

        
    }

    RedirectMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                     IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                     IPMatcher   srcAddress, IPMatcher       dstAddress,
                     PortMatcher srcPort,    PortMatcher     dstPort,
                     boolean isDstRedirect,  InetAddress redirectAddress, int redirectPort )
    {
        this( isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort,
              isDstRedirect, redirectAddress, redirectPort, null, 0 );
    }

    RedirectMatcher( RedirectRule rule, int ruleIndex )
    {
        this( rule.isLive(), rule.getProtocol(), 
              rule.getSrcIntf(),    rule.getDstIntf(), 
              rule.getSrcAddress(), rule.getDstAddress(), 
              rule.getSrcPort(),    rule.getDstPort(),
              rule.isDstRedirect(), 
              ( rule.getRedirectAddress() == null ) ? null : rule.getRedirectAddress().getAddr(), 
              rule.getRedirectPort(), rule, ruleIndex );
    }
        
    InetAddress getRedirectAddress()
    {
        return redirectAddress;
    }


    int getRedirectPort()
    {
        return redirectPort;
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
            
            if ( redirectPort > 0 ) {
                request.serverPort( redirectPort );
            }
        } else {
            /* Modify the client */
            if ( redirectAddress != null ) {
                request.clientAddr( redirectAddress );
            }
            
            if ( redirectPort > 0 ) {
                request.clientPort( redirectPort );
            }
        }
    }

    public RedirectRule rule()
    {
        return this.rule;
    }

    public int ruleIndex()
    {
        return this.ruleIndex;
    }


}
