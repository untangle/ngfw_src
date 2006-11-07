/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.tran.firewall.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class DayOfWeekSetMatcher extends DayOfWeekDBMatcher
{
    private static final long serialVersionUID = -5205089936763240676L;

    private final Set<String> dayOfWeekSet;
    private final String string;

    private DayOfWeekSetMatcher( Set<String> dayOfWeekSet, String string )
    {
        this.dayOfWeekSet = dayOfWeekSet;
        this.string  = string;
    }

    public boolean isMatch( Date when )
    {
        for (String dayString : dayOfWeekSet) {
            if (matchCanonicalDayString(dayString, when))
                return true;
        }
        return false;
    }

    public List<String> toDatabaseList()
    {
        return Collections.unmodifiableList( new ArrayList<String>( dayOfWeekSet ));
    }
    
    public String toString()
    {
        return this.string;
    }

    public static DayOfWeekDBMatcher makeInstance( String ... dayOfWeekArray )
    {
        Set<String> dayOfWeekSet = new HashSet<String>();

        for ( String dayOfWeek : dayOfWeekArray ) dayOfWeekSet.add( dayOfWeek );

        return makeInstance( dayOfWeekSet );
    }

    public static DayOfWeekDBMatcher makeInstance( Set<String> dayOfWeekSet ) 
    {
        if ( dayOfWeekSet == null ) return DayOfWeekSimpleMatcher.getNilMatcher();
                
        String value = "";

        for ( String dayOfWeek  : dayOfWeekSet ) {
            if ( value.length() != 0 ) value += " " + DayOfWeekMatcherConstants.MARKER_SEPERATOR + " ";
            value += dayOfWeek;
        }

        dayOfWeekSet = Collections.unmodifiableSet( dayOfWeekSet );
    
        return new DayOfWeekSetMatcher( dayOfWeekSet, value );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<DayOfWeekDBMatcher> PARSER = new Parser<DayOfWeekDBMatcher>() 
    {
        public int priority()
        {
            return 8;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( DayOfWeekMatcherConstants.MARKER_SEPERATOR ));
        }
        
        public DayOfWeekDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid dayOfWeek set matcher '" + value + "'" );
            }
            
            String dayOfWeekArray[] = value.split( DayOfWeekMatcherConstants.MARKER_SEPERATOR );
            Set<String> dayOfWeekSet = new HashSet<String>();
            
            for ( String dayOfWeekString : dayOfWeekArray ) {
                String val = DayOfWeekDBMatcher.canonicalizeDayName(dayOfWeekString.trim());
                dayOfWeekSet.add ( val );
            }

            return makeInstance( dayOfWeekSet );
        }
    };
}

