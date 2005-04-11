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

/**
 * The class <code>IPMatcher</code> represents an method for determining if an IP address
 * is a match.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public class PortMatcher implements Serializable
{
    public static final String MARKER_RANGE    = "-";
    public static final int    PORT_MASK       = 0xFFFF;
    public static final String MARKER_WILDCARD = "*";
    public static final PortMatcher MATCHER_ALL = new PortMatcher( 0, PORT_MASK );

    /* Base port of the range for the rule */
    private final int start;

    /**
     * end of the range: 
     * Multipurpose second argument.
     * 1. base   : The matcher is just for one ip.
     * 3. <end>  : The top of the range if the matcher uses the range syntax.
     */
    private final int end;

    public PortMatcher( int start )
    {
        this.start = this.end = start & PORT_MASK;
    }

    public PortMatcher( int start, int end )
    {
        if ( start > end ) {
            this.start = end   & PORT_MASK;
            this.end   = start & PORT_MASK;
        } else {
            this.start = start & PORT_MASK;
            this.end   = end   & PORT_MASK;
        }
    }

    public boolean isMatch( int port )
    {
        if (( this.start <= port ) && ( port <= this.end ))
            return true;
        
        return false;
    }
    
    public static PortMatcher parse( String str ) throws IllegalArgumentException
    {
        int start;
        int end;

        str = str.trim();
        
        if ( str.indexOf( MARKER_RANGE ) > 0 ) {
            String strArray[] = str.split( MARKER_RANGE, 3 );
            
            if ( strArray.length != 2 ) {
                throw new IllegalArgumentException( "Invalid PortMatcher, more than two components" + str );
            }

            start = Integer.parseInt( strArray[0] );
            
            /* XXX May need to guarantee a valid mask? */
            end = Integer.parseInt( strArray[1] );

            if ( start < 0 || start > PORT_MASK ) {
                throw new IllegalArgumentException( "Invalid PortMatcher, start ( " + start +
                                                    " ) must be between 0 and " + PORT_MASK );
            }

            if ( end < 0 || end > PORT_MASK ) {
                throw new IllegalArgumentException( "Invalid PortMatcher, end ( " + end +
                                                    " ) must be between 0 and " + PORT_MASK );
            }

            return new PortMatcher( start, end );
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD )) {
            return MATCHER_ALL;
        }

        start = Integer.parseInt( str );
        
        if ( start < 0 || start > PORT_MASK ) {
            throw new IllegalArgumentException( "Invalid PortMatcher, start ( " + start +
                                                " ) must be between 0 and " + PORT_MASK );
        }
        
        /* Just an address a range where the start and end are the same */
        return new PortMatcher( start );
    }    
}
