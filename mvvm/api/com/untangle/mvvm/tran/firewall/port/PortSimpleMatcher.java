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

public final class PortSimpleMatcher
{
    private static final PortDBMatcher ALL_MATCHER     = new PortDBMatcher()
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
        
    public static PortDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    public static PortDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    public static PortDBMatcher getPingMatcher()
    {
        return PING_MATCHER;
    }

    /* This is just for matching a list of interfaces */
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
