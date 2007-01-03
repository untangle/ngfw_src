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

package com.untangle.mvvm.tran.firewall.user;

import java.util.Collections;
import java.util.List;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class UserSimpleMatcher
{
    private static final UserDBMatcher ALL_MATCHER     = new UserDBMatcher()
        {
            public boolean isMatch( String user )
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

    private static final UserDBMatcher NOTHING_MATCHER     = new UserDBMatcher()
        {
            public boolean isMatch( String user )
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

    private static final UserDBMatcher KNOWN_MATCHER     = new UserDBMatcher()
        {
            public boolean isMatch( String user )
            {
                /******** This has to look up inside of the user manager *****/
                return true;
            }

            public List<String> toDatabaseList()
            {
                return Collections.nCopies( 1, toString());
            }
            
            public String toString()
            {
                return UserMatcherConstants.MARKER_KNOWN;
            }
        };

    private static final UserDBMatcher UNKNOWN_MATCHER     = new UserDBMatcher()
        {
            public boolean isMatch( String user )
            {
                /******** This has to look up inside of the user manager *****/
                return true;
            }

            public List<String> toDatabaseList()
            {
                return Collections.nCopies( 1, toString());
            }
            
            public String toString()
            {
                return UserMatcherConstants.MARKER_UNKNOWN;
            }
        };
        
    public static UserDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    public static UserDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* This is just for matching a list of interfaces */
    static final Parser<UserDBMatcher> PARSER = new Parser<UserDBMatcher>() 
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
        
        public UserDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid user simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid user simple matcher '" + value + "'" );
        }
    };
}
