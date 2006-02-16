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

package com.metavize.tran.nat;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.internal.RedirectInternal;

import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.TrafficIntfMatcher;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a RedirectRule because all of its elements are final.
 *   This is a property which is not possible in hibernate objects.
 */
class RedirectMatcher extends TrafficIntfMatcher {
    private static final Logger logger = Logger.getLogger( RedirectMatcher.class );

    public static final RedirectMatcher MATCHER_DISABLED;

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
    private final int ruleIndex;


    /** Whether or not to log */
    private final boolean isLoggingEnabled;
     


    private RedirectMatcher( boolean     isEnabled, boolean isLoggingEnabled,
                             ProtocolMatcher protocol,
                             IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                             IPMatcher   srcAddress, IPMatcher       dstAddress,
                             PortMatcher srcPort,    PortMatcher     dstPort,
                             boolean isDstRedirect,  InetAddress     redirectAddress, int redirectPort,
                             int ruleIndex )
    {
        /* XXX logging should be pushed into a higher class, but for now just leave it in here */
        super( isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, 
               srcPort, dstPort );

        this.isLoggingEnabled = isLoggingEnabled;

        /* Attributes of the redirect */
        this.isDstRedirect   = isDstRedirect;
        this.redirectAddress = redirectAddress;

        if ( this.isPingMatcher ) {
            this.redirectPort = -1;
        } else {
            this.redirectPort = redirectPort;
        }

        this.ruleIndex = ruleIndex;
    }

    RedirectMatcher( boolean     isEnabled,  boolean isLoggingEnabled,
                     ProtocolMatcher protocol,
                     IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                     IPMatcher   srcAddress, IPMatcher       dstAddress,
                     PortMatcher srcPort,    PortMatcher     dstPort,
                     boolean isDstRedirect,  InetAddress redirectAddress, int redirectPort )
    {
        this( isEnabled, isLoggingEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, 
              srcPort, dstPort, isDstRedirect, redirectAddress, redirectPort, 0 );
    }

    RedirectMatcher( RedirectRule rule, int ruleIndex )
    {
        this( rule.isLive(), rule.getLog(), rule.getProtocol(),
              rule.getSrcIntf(),    rule.getDstIntf(),
              rule.getSrcAddress(), rule.getDstAddress(),
              rule.getSrcPort(),    rule.getDstPort(),
              rule.isDstRedirect(),
              ( rule.getRedirectAddress() == null ) ? null : rule.getRedirectAddress().getAddr(),
              rule.getRedirectPort(), ruleIndex );
    }

    RedirectMatcher( RedirectInternal redirect )
    {
        this( redirect.getIsEnabled(), redirect.getIsLoggingEnabled(), redirect.getProtocol(),
              redirect.getSrcIntf(),    redirect.getDstIntf(),
              redirect.getSrcAddress(), redirect.getDstAddress(),
              redirect.getSrcPort(),    redirect.getDstPort(),
              redirect.getIsDstRedirect(),
              ( redirect.getRedirectAddress() == null ) ? null : redirect.getRedirectAddress().getAddr(),
              redirect.getRedirectPort(), redirect.getIndex());
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
        if (logger.isDebugEnabled()) {
            logger.debug( "Redirecting: (" + isDstRedirect +")" + request
                          + " to  " + redirectAddress + ":" + redirectPort );
        }

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

    public int ruleIndex()
    {
        return this.ruleIndex;
    }

    public boolean getIsLoggingEnabled()
    {
        return this.isLoggingEnabled;
    }

    static
    {
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        
        MATCHER_DISABLED =
            new RedirectMatcher( false, false, ProtocolMatcher.MATCHER_NIL,
                                 imf.getNilMatcher(), imf.getNilMatcher(),
                                 ipmf.getNilMatcher(), ipmf.getNilMatcher(),
                                 pmf.getNilMatcher(), pmf.getNilMatcher(), 
                                 false, null, -1, -1 );
    }


}
