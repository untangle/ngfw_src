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

package com.untangle.node.firewall;




import org.apache.log4j.Logger;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.DirectionMatcher;
import com.untangle.uvm.node.firewall.TrafficDirectionMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;


/**
 * A class for matching redirects
 *   This is cannot be squashed into a FirewallRule because all of its elements are final.
 *   This is a property which is not possible in hibernate objects.
 */
class FirewallMatcher extends TrafficDirectionMatcher {
    private final Logger logger = Logger.getLogger(getClass());

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
            new FirewallMatcher( false, ProtocolMatcherFactory.getInstance().getNilMatcher(),
                                 DirectionMatcher.getInstance( false, false ),
                                 ipmf.getNilMatcher(), ipmf.getNilMatcher(),
                                 pmf.getNilMatcher(), pmf.getNilMatcher(),
                                 false );
    }

}
