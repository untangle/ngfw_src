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

package com.untangle.uvm.node.firewall.time;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

public final class DayOfWeekSimpleMatcher
{
    private static final DayOfWeekDBMatcher ALL_MATCHER     = new DayOfWeekDBMatcher()
        {
            public boolean isMatch( Date when )
            {
                return true;
            }

            public List<String> toDatabaseList()
            {
                return Collections.nCopies( 1, toString());
            }
            
            public String toString()
            {
                return ParsingConstants.MARKER_ANY;
            }
        };

    private static final DayOfWeekDBMatcher NOTHING_MATCHER     = new DayOfWeekDBMatcher()
        {
            public boolean isMatch( Date when )
            {
                return false;
            }

            public List<String> toDatabaseList()
            {
                return Collections.nCopies( 1, toString());
            }
            
            public String toString()
            {
                return ParsingConstants.MARKER_NOTHING;
            }
        };

    public static DayOfWeekDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    public static DayOfWeekDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* This is just for matching a list of interfaces */
    static final Parser<DayOfWeekDBMatcher> PARSER = new Parser<DayOfWeekDBMatcher>() 
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
                     value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING ));
        }
        
        public DayOfWeekDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid dayOfWeek simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid dayOfWeek simple matcher '" + value + "'" );
        }
    };
}
