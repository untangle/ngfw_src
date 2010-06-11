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

import static com.untangle.uvm.node.firewall.port.PortMatcherUtil.MARKER_RANGE;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

/**
 * A PortMatcher that matches a range of ports.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class PortRangeMatcher extends PortDBMatcher
{    

    /* Start of the range that should match (inclusive) */
    private final int start;

    /* End of the range that should match (inclusive) */
    private final int end;

    /* Database representation of this port matcher */
    private final String string;

    private PortRangeMatcher( int start, int end, String string )
    {
        this.start = start;
        this.end = end;
        this.string  = string;
    }

    /**
     * Test if <param>port<param> matches this matcher.
     *
     * @param port The port to test.
     * @return True if <param>port</param> is in this range.
     */
    public boolean isMatch( int port )
    {
        return (( start <= port ) && ( port <= end ));
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        return this.string;
    }

    /**
     * Create a range matcher.
     * 
     * @param start The start of the range (inclusive)
     * @param end The end of the range (inclusive)
     * @return A new port matcher from matches from
     * <param>start</param> to <param>end</param>
     */
    public static PortDBMatcher makeInstance( int start, int end )
    {
        PortMatcherUtil pmu = PortMatcherUtil.getInstance();
        
        start = pmu.fixPort( start );
        end = pmu.fixPort( end );
        
        /* These have to be swapped around */
        if ( start > end ) {
            // start = start ^ end;    // ( start ^ end )         = ( start ^ end )
            // end   = start ^ end;    // ( start ^ end ) ^ end   = start
            // start = start ^ end;    // ( start ^ end ) ^ start = end

            int tmp = start;
            start = end;
            end = tmp;
        }
        
        String user = String.valueOf( start ) + " " + MARKER_RANGE +  " " + String.valueOf( end );
    
        return new PortRangeMatcher( start, end, user );
    }

    /* This is the parser for range matchers. */
    static final Parser<PortDBMatcher> PARSER = new Parser<PortDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( MARKER_RANGE ));
        }
        
        public PortDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid port range matcher '" + value + "'" );
            }

            PortMatcherUtil pmu = PortMatcherUtil.getInstance();
            
            String portArray[] = value.split( MARKER_RANGE );

            if ( portArray.length != 2 ) {
                throw new ParseException( "Range matcher contains two components: " + value );
            }

            return makeInstance( pmu.fixPort( portArray[0] ), pmu.fixPort( portArray[1] ));
        }
    };
}

