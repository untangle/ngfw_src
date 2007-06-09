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

package com.untangle.uvm.node.firewall;

/**
 * A matcher for matching traffic direction.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class DirectionMatcher
{
    /* True if this triggers for inbound traffic */
    private final boolean inbound;
    
    /* True if this triggers for outbound traffic */
    private final boolean outbound;

    /* Enumeration of all of the possible direction matchers */
    static final DirectionMatcher ENUMERATION[] =  {
        new DirectionMatcher( false, false ),
        new DirectionMatcher( true, false ),
        new DirectionMatcher( false, true ),
        new DirectionMatcher( true, true )
    };

    private DirectionMatcher( boolean inbound, boolean outbound )
    {
        this.inbound  = inbound;
        this.outbound = outbound;
    }

    /**
     * Test if the direction <param>isInbound</param> matches this matcher.
     *
     * @param inbound The direction to test.
     */
    public boolean isMatch( boolean isInbound )
    {
        if ( isInbound && this.inbound ) return true;
        if ( !isInbound && this.outbound ) return true;
        return false;
    }

    /**
     * Retrieve a direction matcher.
     *
     * @param inbound True if the matcher should match inbound traffic.
     * @param outbound True if the matcher should match outbound traffic.
     */
    public static DirectionMatcher getInstance( boolean inbound, boolean outbound )
    {
        if ( inbound && outbound ) {
            return ENUMERATION[3];
        } else if ( inbound ) {
            return ENUMERATION[1];
        } else if ( outbound ) {
            return ENUMERATION[2];
        }

        return ENUMERATION[0];
    }
}
