/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.metavize.mvvm.tran.ParseException;

/**
 * The class <code>IPMatcher</code> represents an method for determining if an IP address
 * is a match.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public final class PortMatcher implements Serializable
{
    private static final long serialVersionUID = -7392124742130751354L;

    public static  final String MARKER_RANGE     = MatcherStringConstants.RANGE;
    public static  final String MARKER_WILDCARD  = MatcherStringConstants.WILDCARD;
    public static  final String MARKER_ANY       = "any";
    public static  final String MARKER_ALL       = "all";
    public static  final String MARKER_PING      = "n/a";
    private static final String MARKER_NOTHING   = MatcherStringConstants.NOTHING;

    public static final int    PORT_MASK         = 0xFFFF;
    public static final PortMatcher MATCHER_ALL  = new PortMatcher( 0, PORT_MASK );
    public static final PortMatcher MATCHER_NIL  = new PortMatcher();
    public static final PortMatcher MATCHER_PING = new PortMatcher( 0 );

    /* Base port of the range for the rule */
    private final int start;

    /**
     * end of the range: 
     * <end>  : The top of the range for the matcher.
     */
    private final int end;

    /**
     * Private constructor to create the nil matcher
     */
    private PortMatcher()
    {
        start = -1;
        end   = -2;
    }

    public PortMatcher( int start )
    {
        this( start, start );
    }

    public PortMatcher( int start, int end )
    {
        start = fixPort( start );
        end   = fixPort( end );
        
        if ( start > end ) {
            this.start = end;
            this.end   = start;
        } else {
            this.start = start;
            this.end   = end;
        }
    }

    public boolean isMatch( int port )
    {
        if (( this.start <= port ) && ( port <= this.end ))
            return true;
        
        return false;
    }

    public String toString()
    {
        if ( this.equals( MATCHER_ALL ) || (( start == 0 ) && ( end == PORT_MASK ))) {
            return MARKER_ANY;
        } else if ( this.equals( MATCHER_NIL ) || ( start > end )) {
            return MARKER_NOTHING;
        } else if ( this.equals( MATCHER_PING ) || (( start == 0 ) && ( end == 0 ))) {
            return MARKER_PING;
        }
        
        if ( start == end ) {
            return "" + start;
        }
        
        return "" + start + " " + MARKER_RANGE + " " + end;
    }
    
    public static PortMatcher parse( String str ) throws ParseException
    {
        int start;
        int end;

        str = str.trim();
        
        if ( str.indexOf( MARKER_RANGE ) > 0 ) {
            String strArray[] = str.split( MARKER_RANGE, 3 );
            
            if ( strArray.length != 2 ) {
                throw new ParseException( "Invalid PortMatcher, more than two components" + str );
            }

            start = fixPort( strArray[0] );            
            end   = fixPort( strArray[1] );
            
            if (( start == MATCHER_ALL.start ) && ( end == MATCHER_ALL.end )) {
                return MATCHER_ALL;
            } else if (( start == MATCHER_PING.start ) && ( end == MATCHER_PING.end )) {
                return MATCHER_PING;
            }

            return new PortMatcher( start, end );
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD ) || str.equalsIgnoreCase( MARKER_ANY ) ||
                    str.equalsIgnoreCase( MARKER_ALL )) {
            return MATCHER_ALL;
        } else if ( str.equalsIgnoreCase( MARKER_NOTHING )) { 
            return MATCHER_NIL;
        } else if ( str.equalsIgnoreCase( MARKER_PING )) { 
            return MATCHER_PING;
        }
        
        start = fixPort( str );
        
        /* Just an address a range where the start and end are the same */
        return new PortMatcher( start );
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof PortMatcher )) return false;
        
        PortMatcher p = (PortMatcher)o;
        return (( p.start == this.start ) && ( p.end == this.end ));
    }

    static final int fixPort( String port )
    {        
        return fixPort( Integer.parseInt( port.trim()));
    }

    static int fixPort( int port )
    {
        port = ( port < 0 ) ? 0 : port;
        port = ( port > PORT_MASK ) ? PORT_MASK : port;
        return port;
    }
}
