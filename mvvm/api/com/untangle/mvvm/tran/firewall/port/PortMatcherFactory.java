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

import com.untangle.mvvm.tran.ParseException;

import com.untangle.mvvm.tran.firewall.ParsingFactory;

public class PortMatcherFactory
{
    private static final PortMatcherFactory INSTANCE = new PortMatcherFactory();

    private final ParsingFactory<PortDBMatcher> parser;

    private PortMatcherFactory()
    {
        this.parser = new ParsingFactory<PortDBMatcher>( "port matcher" );
        this.parser.registerParsers( PortSimpleMatcher.PARSER, PortSingleMatcher.PARSER,
                                     PortSetMatcher.PARSER, PortRangeMatcher.PARSER );
    }

    public PortDBMatcher getAllMatcher() 
    {
        return PortSimpleMatcher.getAllMatcher();
    }
    
    public PortDBMatcher getNilMatcher() 
    {
        return PortSimpleMatcher.getNilMatcher();
    }

    public PortDBMatcher getPingMatcher() 
    {
        return PortSimpleMatcher.getPingMatcher();
    }

    public PortDBMatcher makeSingleMatcher( int port )
    {
        return PortSingleMatcher.makeInstance( port );
    }

    public PortDBMatcher makeSetMatcher( int ... portArray )
    {
        switch ( portArray.length ) {
        case 0: return PortSimpleMatcher.getNilMatcher();
        case 1: return makeSingleMatcher( portArray[0] );
        default: return PortSetMatcher.makeInstance( portArray );
        }
    }

    public PortDBMatcher makeRangeMatcher( int start, int end )
    {
        return PortRangeMatcher.makeInstance( start, end );
    }
    
    public static final PortDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.parser.parse( value );
    }

    public static final PortMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}
