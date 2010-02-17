/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.networking.IPNetwork;

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
            IPInternalMatcher.matcher = IPSimpleMatcher.getNilMatcher();
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
            
            IPInternalMatcher.matcher = new IPMatcher() {
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
