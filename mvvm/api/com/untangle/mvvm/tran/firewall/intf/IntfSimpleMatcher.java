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

package com.untangle.mvvm.tran.firewall.intf;

import java.net.InetAddress;

import com.untangle.mvvm.tran.IPaddr;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IntfSimpleMatcher extends IntfDBMatcher
{
    private static final IntfDBMatcher ALL_MATCHER     = new IntfSimpleMatcher( true );
    private static final IntfDBMatcher NOTHING_MATCHER = new IntfSimpleMatcher( false );
    
    private final boolean isAll;

    private IntfSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

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
    
    public static IntfDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    public static IntfDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* This is just for matching a list of interfaces */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>() 
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

