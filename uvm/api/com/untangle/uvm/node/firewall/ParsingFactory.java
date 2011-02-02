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

package com.untangle.uvm.node.firewall;

import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.node.ParseException;

/**
 * A ParsingFactory combines a number of parsers into a single unified
 * parser.  Each of the parsers must return the same common object (T).
 * 
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class ParsingFactory<T>
{
    /* List of currently registered parsers */
    private final List<Parser<T>> parserList = new LinkedList<Parser<T>>();

    /* String used mainly for debugging to describe the type of parser this is */
    private final String type;

    /**
     * @param type Type of parser this is.
     */
    public ParsingFactory( String type )
    {
        this.type = type;
    }
    
    /**
     * Parse a string and create a new object.
     *
     * This iterates all of the registered parsers by their priority
     * trying to parse <param>value</value>.  This then attempts to
     * parse the first one that returns true for isParseable.  If none
     * of can parse value, this throws a ParseException.
     *
     * @param value The value to parse.
     * @return Object represented by <param>value</param>.
     * @exception ParseException if <param>value</param> cannot be parsed.
     */
    public final T parse( String value ) throws ParseException
    {
        value = value.trim();
        for ( Parser<T> parser : parserList ) {
            if ( !parser.isParseable( value )) continue;
            
            return parser.parse( value );
        }
        
        /* XXX This probably needs to be customizable */
        throw new ParseException( "Unable to parse the type " + this.type + " [" + value + "]" );
    }

    /**
     * Register some parsers. 
     *
     * @param parsers Array of parsers to add.
     */    
    public void registerParsers( Parser<T> ... parsers )
    {
        for ( Parser<T> parser : parsers ) registerParser( parser );
    }

    /**
     * Register a parser.
     *
     * @param parser The parser to register.
     */
    public void registerParser( Parser<T> parser )
    {
        int priority = parser.priority();
        
        /* This is a silly method, but it is simple and it works */
        int c = 0;

        for ( Parser<T> p : parserList ) {
            if ( priority < p.priority()) break;
            c++;
        }
        
        parserList.add( c, parser );
    }

    /**
     * Retrieve the type of parser.
     *
     * @return The type of parser.
     */
    public String type()
    {
        return this.type;
    }
}
