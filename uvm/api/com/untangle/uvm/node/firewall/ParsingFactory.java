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

package com.untangle.uvm.node.firewall;

import com.untangle.uvm.node.ParseException;

import java.util.List;
import java.util.LinkedList;

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
