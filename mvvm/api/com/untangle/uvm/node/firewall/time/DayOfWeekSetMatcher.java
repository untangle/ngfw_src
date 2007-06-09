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

package com.untangle.mvvm.tran.firewall.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

        // Unsorted, so we must sort here.
        StringBuilder sb = new StringBuilder();
        boolean doneOne = false;
        boolean hasEmAll = true;
        // Locale prob. XXX
        String[] sortedDays = { "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday" };

        for ( String dayOfWeek  : sortedDays ) {
            if (dayOfWeekSet.contains(dayOfWeek)) {
                if (doneOne)
                    sb.append(" ").append(DayOfWeekMatcherConstants.MARKER_SEPERATOR).append(" ");
                else
                    doneOne = true;
                sb.append(dayOfWeek);
            } else {
                hasEmAll = false;
            }
        }

        dayOfWeekSet = Collections.unmodifiableSet( dayOfWeekSet );

        if (!hasEmAll)
            return new DayOfWeekSetMatcher( dayOfWeekSet, sb.toString() );
        else
            return new DayOfWeekSetMatcher( dayOfWeekSet, ParsingConstants.MARKER_ANY );

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

