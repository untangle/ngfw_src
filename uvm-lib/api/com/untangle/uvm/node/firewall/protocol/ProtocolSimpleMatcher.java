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

package com.untangle.uvm.node.firewall.protocol;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;
import com.untangle.uvm.vnet.Protocol;

/**
 * ProtocolMatcher designed for simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class ProtocolSimpleMatcher extends ProtocolDBMatcher
{
    /* A protocol matcher that matches everything */
    private static final ProtocolDBMatcher MATCHER_ALL = new ProtocolSimpleMatcher( true );

    /* A protocol matcher that never matches */
    private static final ProtocolDBMatcher MATCHER_NOTHING = new ProtocolSimpleMatcher( false );

    /* true if this matches everyhthing */
    private final boolean isAll;
    
    private ProtocolSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if this is the all matcher, false otherwise.
     */
    public boolean isMatch( Protocol protocol )
    {
        return this.isAll;
    }

    /**
     * Test if <param>protocol<param> matches this matcher.
     *
     * @param protocol The protocol to test.
     * @return True if this is the all matcher, false otherwise.
     */
    public boolean isMatch( short protocol )
    {
        return this.isAll;
    }

    public String toDatabaseString()
    {
        return toString();
    }

    public String toString()
    {
        String name = ( isAll ) ? ProtocolParsingConstants.MARKER_ANY : ParsingConstants.MARKER_NOTHING;
        return name.toUpperCase();
    }
    
    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches every protocol.
     */
    public static ProtocolDBMatcher getAllMatcher()
    {
        return MATCHER_ALL;
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches a protocol.
     */
    public static ProtocolDBMatcher getNilMatcher()
    {
        return MATCHER_NOTHING;
    }

    /* This is the parser for simple protocol matchers */
    static final Parser<ProtocolDBMatcher> PARSER = new Parser<ProtocolDBMatcher>() 
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
                     value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING ) ||
                     value.equalsIgnoreCase( ProtocolParsingConstants.MARKER_ANY ));
        }
        
        public ProtocolDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid protocol simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return MATCHER_ALL;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return MATCHER_NOTHING;
                 }
            
            throw new ParseException( "Invalid protocol simple matcher '" + value + "'" );
        }
    };
}
