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

package com.untangle.mvvm.tran.firewall;

import com.untangle.mvvm.tran.ParseException;

import java.util.List;
import java.util.LinkedList;

public class ParsingFactory<T>
{
    final List<Parser<T>> parserList = new LinkedList<Parser<T>>();

    private final String type;

    public ParsingFactory( String type )
    {
        this.type = type;
    }
    
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

    public void registerParsers( Parser<T> ... parsers )
    {
        for ( Parser<T> parser : parsers ) registerParser( parser );
    }

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


    public String type()
    {
        return this.type;
    }
}
