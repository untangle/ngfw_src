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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;

import java.util.List;
import java.util.LinkedList;

import com.untangle.mvvm.networking.IPNetwork;

import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IPInternalMatcher implements IPMatcher
{
    private static final String MARKER_INTERNAL = "internal";
    private static final String MARKER_EXTERNAL = "external";
    
    private static final IPInternalMatcher MATCHER_INTERNAL = new IPInternalMatcher( true );
    private static final IPInternalMatcher MATCHER_EXTERNAL = new IPInternalMatcher( false );
    
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
    
    public synchronized void setInternalNetwork( InetAddress internalNetwork, InetAddress internalSubnet )
    {
        if (( internalNetwork == null ) || ( internalSubnet == null )) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSubnetMatcher.makeInstance( internalNetwork, internalSubnet );
        }
    }

    
    public synchronized void setInternalNetworks( List<IPNetwork> networkList )
    {
        switch ( networkList.size()) {
        case 0:
            this.matcher = IPSimpleMatcher.getNilMatcher();
            break;

        case 1: {
            IPNetwork network = networkList.get( 0 );
            setInternalNetwork( network.getNetwork().getAddr(), network.getNetmask().getAddr());
            break;
        }
            
        default:
            final List<IPMatcher> matcherList = new LinkedList<IPMatcher>();
            for ( IPNetwork network : networkList ) {
                matcherList.add( IPSubnetMatcher.makeInstance( network.getNetwork(), network.getNetmask()));
            }
            
            this.matcher = new IPMatcher() {
                    public boolean isMatch( InetAddress address ) {
                        /* iterate all of the matchers and check if any of them match */
                        for ( IPMatcher matcher : matcherList ) {
                            if ( matcher.isMatch( address )) return true;
                        }
                        /* otherwise return false */
                        return false;
                    }

                    public String toDatabaseString() {
                        return "never will i ever";
                    }
                };
        }
    }

    public static IPInternalMatcher getInternalMatcher()
    {
        return MATCHER_INTERNAL;
    }

    public static IPInternalMatcher getExternalMatcher()
    {
        return MATCHER_EXTERNAL;
    }
}
