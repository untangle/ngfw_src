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

package com.untangle.node.router;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.networking.RedirectRule;
import com.untangle.uvm.networking.internal.RedirectInternal;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.node.firewall.TrafficIntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a RedirectRule because all of its elements are final.
 *   This is a property which is not possible in hibernate objects.
 */
class RedirectMatcher extends TrafficIntfMatcher {
    private final Logger logger = Logger.getLogger(getClass());

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

        /* XXX Possible ICMP hack since ICMP sessions can't be redirected */
        this.redirectPort = redirectPort;

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

            /* If the redirect port is greater than zero, and this is not an Ping session, then
             * redirect it */
            /* XXXXX ICMP HACK */
            if (( redirectPort > 0 ) && ( request.serverPort() != 0 ) && ( request.clientPort() != 0 )) {
                request.serverPort( redirectPort );
            }
        } else {
            /* Modify the client */
            if ( redirectAddress != null ) {
                request.clientAddr( redirectAddress );
            }

            /* If the redirect port is greater than zero, and this is not an Ping session, then
             * redirect it */
            /* XXXXX ICMP HACK */
            if (( redirectPort > 0 ) && ( request.serverPort() != 0 ) && ( request.clientPort() != 0 )) {
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

    static RedirectMatcher makeLocalRedirect( RedirectRule rule )
    {
        return makeLocalRedirect( rule, 0 );
    }

    static RedirectMatcher makeLocalRedirect( RedirectRule rule, int ruleIndex )
    {
        IntfMatcher intfAllMatcher = IntfMatcherFactory.getInstance().getAllMatcher();
        IPMatcher   ipAllMatcher   = IPMatcherFactory.getInstance().getAllMatcher();
        PortMatcher portAllMatcher = PortMatcherFactory.getInstance().getAllMatcher();

        InetAddress redirectAddress = null;
        if ( rule.getRedirectAddress() != null ) redirectAddress = rule.getRedirectAddress().getAddr();

        return
            new RedirectMatcher( rule.isLive(), rule.getLog(), rule.getProtocol(),
                                 intfAllMatcher,       intfAllMatcher,
                                 ipAllMatcher,         rule.getDstAddress(),
                                 rule.getSrcPort(),    rule.getDstPort(),
                                 rule.isDstRedirect(),
                                 redirectAddress,
                                 rule.getRedirectPort(), ruleIndex );
    }

    static
    {
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();

        MATCHER_DISABLED =
            new RedirectMatcher( false, false, ProtocolMatcherFactory.getInstance().getNilMatcher(),
                                 imf.getNilMatcher(), imf.getNilMatcher(),
                                 ipmf.getNilMatcher(), ipmf.getNilMatcher(),
                                 pmf.getNilMatcher(), pmf.getNilMatcher(),
                                 false, null, -1, -1 );
    }


}
