/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.node.firewall.time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

public final class DayOfWeekSetMatcher extends DayOfWeekDBMatcher
{

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

