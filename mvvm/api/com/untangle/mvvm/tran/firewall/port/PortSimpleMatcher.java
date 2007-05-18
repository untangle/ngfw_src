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

/**
 * PortMatchers designed for simple cases (all, nothing or ping sessions).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class PortSimpleMatcher
{
    /* A Port Matcher that matches everything */
    private static final PortDBMatcher ALL_MATCHER = new PortDBMatcher()
        {
            public boolean isMatch( int port )
            {
                return true;
            }

            public String toDatabaseString()
            {
                return toString();
            }
            
            public String toString()
            {
                return ParsingConstants.MARKER_ANY;
            }
        };

    /* A Port Matcher that doesn't match anything */
    private static final PortDBMatcher NOTHING_MATCHER     = new PortDBMatcher()
        {
            public boolean isMatch( int port )
            {
                return false;
            }

            public String toDatabaseString()
            {
                return toString();
            }
            
            public String toString()
            {
                return ParsingConstants.MARKER_NOTHING;
            }
        };

    /* A matcher that matches ping sessions */
    private static final PortDBMatcher PING_MATCHER     = new PortDBMatcher()
        {
            public boolean isMatch( int port )
            {
                return ( port == 0 );
            }

            public String toDatabaseString()
            {
                return toString();
            }
            
            public String toString()
            {
                return ParsingConstants.MARKER_NA;
            }
        };
        
    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches every port.
     */
    public static PortDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches a port.
     */
    public static PortDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /**
     * Retrieve the ping matcher.
     *
     * @return A matcher that matches ping sessions.
     */
    public static PortDBMatcher getPingMatcher()
    {
        return PING_MATCHER;
    }

    /* This is the parser for simple port matchers */
    static final Parser<PortDBMatcher> PARSER = new Parser<PortDBMatcher>() 
    {
        public int priority()
        {
            return 0;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) ||
                     value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                     value.equalsIgnoreCase( ParsingConstants.MARKER_ALL ) ||
                     value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING ) ||
                     value.equalsIgnoreCase( ParsingConstants.MARKER_NA ));            
        }
        
        public PortDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NA )) {
                     return PING_MATCHER;
                 }
            
            throw new ParseException( "Invalid port simple matcher '" + value + "'" );
        }
    };
}
