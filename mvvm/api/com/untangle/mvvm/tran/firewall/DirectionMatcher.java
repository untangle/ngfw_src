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

package com.untangle.mvvm.tran.firewall;

public class DirectionMatcher
{
    private final boolean inbound;
    private final boolean outbound;

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

    public boolean isMatch( boolean isInbound )
    {
        if ( isInbound && this.inbound ) return true;
        if ( !isInbound && this.outbound ) return true;
        return false;
    }

    public static DirectionMatcher getInstance( boolean inbound, boolean outbound )
    {
        /* This is a little flimsy and magical, but it is simply enough that it doesn't
         * need to go overboard */
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
