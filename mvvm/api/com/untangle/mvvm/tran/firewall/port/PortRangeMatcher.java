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

package com.untangle.mvvm.tran.firewall.port;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class PortRangeMatcher extends PortDBMatcher
{
    private static final String MARKER_RANGE = PortMatcherUtil.MARKER_RANGE;

    private final int start, end;
    private final String string;

    private PortRangeMatcher( int start, int end, String string )
    {
        this.start = start;
        this.end = end;
        this.string  = string;
    }

    public boolean isMatch( int port )
    {
        return (( start <= port ) && ( port <= end ));
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return this.string;
    }

    public static PortDBMatcher makeInstance( int start, int end )
    {
        PortMatcherUtil pmu = PortMatcherUtil.getInstance();
        
        start = pmu.fixPort( start );
        end = pmu.fixPort( end );
        
        /* These have to be swapped around */
        if ( start > end ) {
            // start = start ^ end;    // ( start ^ end )         = ( start ^ end )
            // end   = start ^ end;    // ( start ^ end ) ^ end   = start
            // start = start ^ end;    // ( start ^ end ) ^ start = end

            int tmp = start;
            start = end;
            end = tmp;
        }
        
        String user = String.valueOf( start ) + " " + MARKER_RANGE +  " " + String.valueOf( end );
    
        return new PortRangeMatcher( start, end, user );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<PortDBMatcher> PARSER = new Parser<PortDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( MARKER_RANGE ));
        }
        
        public PortDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port range matcher '" + value + "'" );
            }

            PortMatcherUtil pmu = PortMatcherUtil.getInstance();
            
            String portArray[] = value.split( MARKER_RANGE );

            if ( portArray.length != 2 ) {
                throw new ParseException( "Range matcher contains two components: " + value );
            }

            return makeInstance( pmu.fixPort( portArray[0] ), pmu.fixPort( portArray[1] ));
        }
    };
}

