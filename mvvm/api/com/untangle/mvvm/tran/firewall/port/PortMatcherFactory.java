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

/**
 * A factory for Port matchers.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public class PortMatcherFactory
{
    private static final PortMatcherFactory INSTANCE = new PortMatcherFactory();

    /** This is the parser used to convert strings to PortDBMatchers */
   private final ParsingFactory<PortDBMatcher> parser;

    private PortMatcherFactory()
    {
        this.parser = new ParsingFactory<PortDBMatcher>( "port matcher" );
        this.parser.registerParsers( PortSimpleMatcher.PARSER, PortSingleMatcher.PARSER,
                                     PortSetMatcher.PARSER, PortRangeMatcher.PARSER );
    }

    /**
     * Retrieve the all matcher.
     *
     * @return The all matcher
     */
    public PortDBMatcher getAllMatcher() 
    {
        return PortSimpleMatcher.getAllMatcher();
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return The nil matcher
     */    
    public PortDBMatcher getNilMatcher() 
    {
        return PortSimpleMatcher.getNilMatcher();
    }

    /**
     * Retrieve a matcher that matches ping sessions.
     *
     * @return The ping matcher
     */    
    public PortDBMatcher getPingMatcher() 
    {
        return PortSimpleMatcher.getPingMatcher();
    }

    /**
     * Create a matcher that matches <param>port</param>.
     *
     * @param port The port to match
     * @return A port matcher that matches <param>port</param>
     */    
    public PortDBMatcher makeSingleMatcher( int port )
    {
        return PortSingleMatcher.makeInstance( port );
    }

    /**
     * Create a matcher that matches an array of ports.
     *
     * @param portArray The array of ports that should match.
     * @return A matcher that matches an array of ports.
     */    
    public PortDBMatcher makeSetMatcher( int ... portArray )
    {
        switch ( portArray.length ) {
        case 0: return PortSimpleMatcher.getNilMatcher();
        case 1: return makeSingleMatcher( portArray[0] );
        default: return PortSetMatcher.makeInstance( portArray );
        }
    }

    /**
     * Create a matcher that matches a range of ports.
     *
     * @param start The start of the range
     * @param end The end of the range
     * @return A matcher that matches any port from
     * <param>start</param> to </param>end</param>.
     */
    public PortDBMatcher makeRangeMatcher( int start, int end )
    {
        return PortRangeMatcher.makeInstance( start, end );
    }
    
    /**
     * Convert <param>value</param> to an PortDBMatcher.
     *
     * @param value The string to parse.
     * @return The port matcher that corresponds to
     * <param>value</param>
     */
    public static final PortDBMatcher parse( String value ) throws ParseException
    {
        return INSTANCE.parser.parse( value );
    }

    public static final PortMatcherFactory getInstance()
    {
        return INSTANCE;
    }
}
