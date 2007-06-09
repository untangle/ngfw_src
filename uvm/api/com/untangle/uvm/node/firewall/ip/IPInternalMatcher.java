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

/**
 * An IPMatcher that matches all of the addresses assigned to the
 * internal interface network space.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPInternalMatcher implements IPMatcher
{
    /* String representation for the internal matcher */
    private static final String MARKER_INTERNAL = "internal";

    /* String representation for the external matcher */
    private static final String MARKER_EXTERNAL = "external";
    
    /* The internal matcher */
    private static final IPInternalMatcher MATCHER_INTERNAL = new IPInternalMatcher( true );

    /* The external matcher (This is just the inverse of the internal
     * matcher */
    private static final IPInternalMatcher MATCHER_EXTERNAL = new IPInternalMatcher( false );
    
    /* The matcher to use when testing for match, this is updated when the address changes. */
    private static IPMatcher matcher = IPSimpleMatcher.getNilMatcher();

    /* True if this is the internal matcher, false if this is the external matcher */
    private final boolean isInternal;

    private IPInternalMatcher( boolean isInternal )
    {
        this.isInternal = isInternal;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> matches this matcher.
     */
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

    /* This actually cannot be stored into a datbase, it is not a DBMatcher */
    public String toDatabaseString()
    {
        return (( this.isInternal ) ? MARKER_INTERNAL : MARKER_EXTERNAL );
    }
    
    /**
     * Update the internal network with a single value.
     * 
     * @param internalNetwork Address of internal interface.
     * @param internalSubnet subnet of the internal network.
     */
    public synchronized void setInternalNetwork( InetAddress internalNetwork, InetAddress internalSubnet )
    {
        if (( internalNetwork == null ) || ( internalSubnet == null )) {
            matcher = IPSimpleMatcher.getNilMatcher();
        } else {
            matcher = IPSubnetMatcher.makeInstance( internalNetwork, internalSubnet );
        }
    }

    /**
     * Update the internal network with a list of networks.
     * 
     * @param networkList The list of networks that are on the internal interface.
     */    
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
            /* There presently isn't a generic way of matching or'ing several matchers
             * together, right now this is hacked together with a simple matcher that
             * just iterates a list */
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
