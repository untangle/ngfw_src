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

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.ParsingFactory;

public class DayOfWeekMatcherFactory
{
    private static final DayOfWeekMatcherFactory INSTANCE = new DayOfWeekMatcherFactory();

    private final ParsingFactory<DayOfWeekDBMatcher> parser;

    @SuppressWarnings("unchecked") //varargs
    private DayOfWeekMatcherFactory()
    {
        this.parser = new ParsingFactory<DayOfWeekDBMatcher>( "dayOfWeek matcher" );
        this.parser.registerParsers( DayOfWeekSimpleMatcher.PARSER, DayOfWeekSingleMatcher.PARSER,
                                     DayOfWeekSetMatcher.PARSER );
    }

    public DayOfWeekDBMatcher getAllMatcher() 
    {
        return DayOfWeekSimpleMatcher.getAllMatcher();
    }
    
    public DayOfWeekDBMatcher getNilMatcher() 
    {
        return DayOfWeekSimpleMatcher.getNilMatcher();
    }

    public DayOfWeekDBMatcher makeSingleMatcher( String dayOfWeek )
    {
        return DayOfWeekSingleMatcher.makeInstance( dayOfWeek );
    }

    public DayOfWeekDBMatcher makeSetMatcher( String ... dayOfWeekArray )
    {
        switch ( dayOfWeekArray.length ) {
        case 0: return DayOfWeekSimpleMatcher.getNilMatcher();
        case 1: return makeSingleMatcher( dayOfWeekArray[0] );
        default: return DayOfWeekSetMatcher.makeInstance( dayOfWeekArray );
        }
    }
    
    public static final DayOfWeekDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.parser.parse( value );
    }

    public static final DayOfWeekMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}
