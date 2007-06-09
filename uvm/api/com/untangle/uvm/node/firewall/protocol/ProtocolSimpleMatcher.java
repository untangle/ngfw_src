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

package com.untangle.uvm.node.firewall.protocol;

import com.untangle.uvm.tapi.Protocol;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * ProtocolMatcher designed for simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class ProtocolSimpleMatcher extends ProtocolDBMatcher
{
    // XXX private static final long serialVersionUID = 6026959848409522258L;
    /* A protocol matcher that matches everything */
    private static final ProtocolDBMatcher MATCHER_ALL = new ProtocolSimpleMatcher( true );

    /* A protocol matcher that never matches */
    private static final ProtocolDBMatcher MATCHER_NOTHING = new ProtocolSimpleMatcher( false );

    /* true if this matches everyhthing */
    private final boolean isAll;
    
    private ProtocolSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if this is the all matcher, false otherwise.
     */
    public boolean isMatch( Protocol protocol )
    {
        return this.isAll;
    }

    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if this is the all matcher, false otherwise.
     */
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
    
    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches every protocol.
     */
    public static ProtocolDBMatcher getAllMatcher()
    {
        return MATCHER_ALL;
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches a protocol.
     */
    public static ProtocolDBMatcher getNilMatcher()
    {
        return MATCHER_NOTHING;
    }

    /* This is the parser for simple protocol matchers */
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
