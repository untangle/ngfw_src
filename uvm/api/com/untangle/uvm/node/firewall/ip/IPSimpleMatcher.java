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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * IPMatcher designed for simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPSimpleMatcher extends IPDBMatcher
{
    /* An IP Matcher that matches everything */
    private static final IPDBMatcher ALL_MATCHER     = new IPSimpleMatcher( true );

    /* An IP Matcher that never matches */
    private static final IPDBMatcher NOTHING_MATCHER = new IPSimpleMatcher( false );
    
    private final boolean isAll;

    private IPSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if this is the all matcher, false otherwise.
     */
    public boolean isMatch( InetAddress address )
    {
        return isAll;
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        if ( isAll ) return ParsingConstants.MARKER_ANY;
        return ParsingConstants.MARKER_NOTHING;
    }

    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches every IP.
     */
    public static IPDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches an IP.
     */
    public static IPDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }
    
    /* This is the parser for simple ip matchers */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        /* This should always be checked first because it has the most
         * specific definition */
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
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid ip simple matcher '" + value + "'" );
        }
    };
}

