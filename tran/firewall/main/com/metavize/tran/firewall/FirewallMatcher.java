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

package com.metavize.tran.firewall;

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
import com.metavize.mvvm.tran.firewall.FirewallRule;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a FirewallRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
class FirewallMatcher extends TrafficMatcher {
    private static final Logger logger = Logger.getLogger( FirewallMatcher.class );
    
    public static final FirewallMatcher MATCHER_DISABLED = 
        new FirewallMatcher( false, ProtocolMatcher.MATCHER_NIL,
                             IntfMatcher.MATCHER_NIL, IntfMatcher.MATCHER_NIL,
                             IPMatcher.MATCHER_NIL,   IPMatcher.MATCHER_ALL, 
                             PortMatcher.MATCHER_NIL, PortMatcher.MATCHER_NIL,
                             false );

    
    private final boolean isTrafficBlocker;


    // XXX For the future
    // TimeMatcher time;
    public FirewallMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                            IntfMatcher srcIntf,    IntfMatcher     dstIntf, 
                            IPMatcher   srcAddress, IPMatcher       dstAddress,
                            PortMatcher srcPort,    PortMatcher     dstPort,
                            boolean isTrafficBlocker )
    {
        super( isEnabled, protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );

        /* Attributes of the firewall rule */
        this.isTrafficBlocker = isTrafficBlocker;
    }

    FirewallMatcher( FirewallRule rule )
    {
        super( rule );

        /* Attributes of the redirect */
        isTrafficBlocker = rule.isTrafficBlocker();
    }

    public boolean isTrafficBlocker()
    {
        return this.isTrafficBlocker;
    }
}
