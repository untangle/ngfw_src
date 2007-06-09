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

package com.untangle.uvm.node.firewall.intf;

import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * An IntfMatcher that matches the simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IntfSimpleMatcher extends IntfDBMatcher
{
    /* An interface matcher that matches everything */
    private static final IntfDBMatcher ALL_MATCHER     = new IntfSimpleMatcher( true );

    /* An interface matcher that doesn't match anything */
    private static final IntfDBMatcher NOTHING_MATCHER = new IntfSimpleMatcher( false );
    
    /* true if this is the all matcher */
    private final boolean isAll;

    private IntfSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     */
    public boolean isMatch( byte intf )
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
     * Retrieve the all matcher
     *
     * @return An interface matcher that matches everything
     */
    public static IntfDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    /**
     * Retrieve the nil matcher
     *
     * @return An interface matcher that doesn't match anything.
     */
    public static IntfDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* The parse for simple matchers */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>() 
    {
        /* This has the most specific syntax and should alyways first */
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
        
        public IntfDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid intf simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid intf simple matcher '" + value + "'" );
        }
    };
}

