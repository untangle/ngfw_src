/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;

import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.Parser;
import com.metavize.mvvm.tran.firewall.ParsingConstants;

public final class IPInternalMatcher implements IPMatcher
{
    private static final String MARKER_INTERNAL = "internal";
    private static final String MARKER_EXTERNAL = "external";
    
    private static final IPMatcher MATCHER_INTERNAL = new IPInternalMatcher( true );
    private static final IPMatcher MATCHER_EXTERNAL = new IPInternalMatcher( false );
    
    private static IPMatcher matcher = IPSimpleMatcher.getNilMatcher();

    private final boolean isInternal;

    private IPInternalMatcher( boolean isInternal )
    {
        this.isInternal = isInternal;
    }

    public boolean isMatch( InetAddress address )
    {
        /* If internal and it is a match, it is true,        */
        /* If not internal and it is not a match, it is true */
        /* Equivalent to: if ( isInternal ) return matcher.isMatch() ; else return !matcher.isMatch() */
        return ( this.isInternal == matcher.isMatch( address ));
    }
    
    public String toString()
    {
        return toDatabaseString();
    }

    /* This actually cannot be stored into a datbase, this is just for fun */
    public String toDatabaseString()
    {
        return (( this.isInternal ) ? MARKER_INTERNAL : MARKER_EXTERNAL );
    }
    
    public synchronized void setAddress( InetAddress internalNetwork, InetAddress internalSubnet )
    {
        if (( internalNetwork == null ) || ( internalSubnet == null )) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSubnetMatcher.makeInstance( internalNetwork, internalSubnet );
        }
    }

    public static IPMatcher getInternalMatcher()
    {
        return MATCHER_INTERNAL;
    }

    public static IPMatcher getExternalMatcher()
    {
        return MATCHER_EXTERNAL;
    }
}
