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

package com.untangle.uvm.node.firewall.port;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.ParsingFactory;

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

    @SuppressWarnings("unchecked")
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
