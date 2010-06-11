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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

@SuppressWarnings("serial")
public final class DayOfWeekSimpleMatcher
{
    private static final DayOfWeekDBMatcher ALL_MATCHER     = new DayOfWeekDBMatcher()
        {
            public boolean isMatch( Date when )
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

    private static final DayOfWeekDBMatcher NOTHING_MATCHER     = new DayOfWeekDBMatcher()
        {
            public boolean isMatch( Date when )
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

    public static DayOfWeekDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    public static DayOfWeekDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }

    /* This is just for matching a list of interfaces */
    static final Parser<DayOfWeekDBMatcher> PARSER = new Parser<DayOfWeekDBMatcher>() 
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
        
        public DayOfWeekDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid dayOfWeek simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid dayOfWeek simple matcher '" + value + "'" );
        }
    };
}
