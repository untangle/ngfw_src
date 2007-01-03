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

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class PortSetMatcher extends PortDBMatcher
{
    private static final String MARKER_RANGE = "-";

    private final Set<Integer> portSet;
    private final String string;

    private PortSetMatcher( Set<Integer> portSet, String string )
    {
        this.portSet = portSet;
        this.string  = string;
    }

    public boolean isMatch( int port )
    {
        return ( this.portSet.contains( port ));
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return this.string;
    }

    public static PortDBMatcher makeInstance( int ... portArray )
    {
        Set<Integer> portSet = new HashSet<Integer>();

        for ( int port : portArray ) portSet.add( port );

        return makeInstance( portSet );
    }

    public static PortDBMatcher makeInstance( Set<Integer> portSet ) 
    {
        if ( portSet == null ) return PortSimpleMatcher.getNilMatcher();
        
        PortMatcherUtil pmu = PortMatcherUtil.getInstance();
        
        String user = "";

        for ( Integer port : portSet ) {
            if ( user.length() != 0 ) user += " " + ParsingConstants.MARKER_SEPERATOR + " ";
            user += port;
        }

        portSet = Collections.unmodifiableSet( portSet );
    
        return new PortSetMatcher( portSet, user );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<PortDBMatcher> PARSER = new Parser<PortDBMatcher>() 
    {
        public int priority()
        {
            return 8;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( ParsingConstants.MARKER_SEPERATOR ));
        }
        
        public PortDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port set matcher '" + value + "'" );
            }
            
            String portArray[] = value.split( ParsingConstants.MARKER_SEPERATOR );
            Set<Integer> portSet = new HashSet<Integer>();
            
            PortMatcherUtil pmu = PortMatcherUtil.getInstance();

            for ( String portString : portArray ) {
                portString = portString.trim();
                
                int port = pmu.fixPort( portString );
                portSet.add ( port );
            }

            return makeInstance( portSet );
        }
    };
}

