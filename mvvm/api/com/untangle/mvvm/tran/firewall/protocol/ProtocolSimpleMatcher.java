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

package com.untangle.mvvm.tran.firewall.protocol;

import com.untangle.mvvm.tapi.Protocol;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

/**
 * The class <code>ProtocolMatcher</code> represents a class for filtering on the Protocol of a
 * session.
 *
 * @author <a href="mailto:rbscott@untangle.com">rbscott</a>
 * @version 1.0
 */
public final class ProtocolSimpleMatcher extends ProtocolDBMatcher
{
    // XXX private static final long serialVersionUID = 6026959848409522258L;
    private static final ProtocolDBMatcher MATCHER_ALL = new ProtocolSimpleMatcher( true );
    private static final ProtocolDBMatcher MATCHER_NOTHING = new ProtocolSimpleMatcher( false );

    private final boolean isAll;
    
    private ProtocolSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    public boolean isMatch( Protocol protocol )
    {
        return this.isAll;
    }

    public boolean isMatch( short protocol )
    {
        return this.isAll;
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        String name = ( isAll ) ? ProtocolParsingConstants.MARKER_ANY : ParsingConstants.MARKER_NOTHING;
        return name.toUpperCase();
    }
    
    public static ProtocolDBMatcher getAllMatcher()
    {
        return MATCHER_ALL;
    }

    public static ProtocolDBMatcher getNilMatcher()
    {
        return MATCHER_NOTHING;
    }

    static final Parser<ProtocolDBMatcher> PARSER = new Parser<ProtocolDBMatcher>() 
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
                     value.equalsIgnoreCase( ProtocolParsingConstants.MARKER_ANY ));
        }
        
        public ProtocolDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid protocol simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return MATCHER_ALL;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return MATCHER_NOTHING;
                 }
            
            throw new ParseException( "Invalid protocol simple matcher '" + value + "'" );
        }
    };
}
