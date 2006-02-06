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

import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.DirectionMatcher;
import com.metavize.mvvm.tran.firewall.TrafficDirectionMatcher;

/**
 * A class for matching redirects
 *   This is cannot be squashed into a FirewallRule because all of its elements are final. 
 *   This is a property which is not possible in hibernate objects.
 */
class FirewallMatcher extends TrafficDirectionMatcher {
    private static final Logger logger = Logger.getLogger( FirewallMatcher.class );
    
    public static final FirewallMatcher MATCHER_DISABLED;

    /* Used for logging */
    private final FirewallRule rule;
    private final int ruleIndex;

    private final boolean isTrafficBlocker;

    public FirewallMatcher( boolean     isEnabled,  ProtocolMatcher protocol, 
                            DirectionMatcher direction,
                            IPMatcher   srcAddress, IPMatcher       dstAddress,
                            PortMatcher srcPort,    PortMatcher     dstPort,
                            boolean isTrafficBlocker )
    {
        super( isEnabled, protocol, direction, srcAddress, dstAddress, srcPort, dstPort );

        /* Attributes of the firewall rule */
        this.isTrafficBlocker = isTrafficBlocker;
        
        /* XXX probably want to set this to a more creative value, or just get rid of this constructor
         * it is never used */
        this.rule      = null;
        this.ruleIndex = 0;
    }

    FirewallMatcher( FirewallRule rule, int ruleIndex )
    {
        super( rule );
        
        this.rule      = rule;
        this.ruleIndex = ruleIndex;

        /* Attributes of the redirect */
        isTrafficBlocker = rule.isTrafficBlocker();
    }

    public boolean isTrafficBlocker()
    {
        return this.isTrafficBlocker;
    }

    public FirewallRule rule()
    {
        return this.rule;
    }

    public int ruleIndex()
    {
        return this.ruleIndex;
    }

    static
    {
        IPMatcherFactory ipmf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        
        MATCHER_DISABLED =
            new FirewallMatcher( false, ProtocolMatcher.MATCHER_NIL,
                                 DirectionMatcher.getInstance( false, false ),
                                 ipmf.getNilMatcher(), ipmf.getNilMatcher(),
                                 pmf.getNilMatcher(), pmf.getNilMatcher(), 
                                 false );
    }

}
