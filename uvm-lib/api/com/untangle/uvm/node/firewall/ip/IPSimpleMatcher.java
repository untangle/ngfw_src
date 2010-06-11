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

package com.untangle.uvm.node.firewall.ip;

import java.net.InetAddress;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;
import com.untangle.uvm.node.firewall.ParsingConstants;

/**
 * IPMatcher designed for simple cases (all or nothing).
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IPSimpleMatcher extends IPDBMatcher
{

    /* An IP Matcher that matches everything */
    private static final IPDBMatcher ALL_MATCHER     = new IPSimpleMatcher( true );

    /* An IP Matcher that never matches */
    private static final IPDBMatcher NOTHING_MATCHER = new IPSimpleMatcher( false );
    
    private final boolean isAll;

    private IPSimpleMatcher( boolean isAll )
    {
        this.isAll = isAll;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if this is the all matcher, false otherwise.
     */
    public boolean isMatch( InetAddress address )
    {
        return isAll;
    }

    public String toDatabaseString()
    {
        return toString();
    }
    
    public String toString()
    {
        if ( isAll ) return ParsingConstants.MARKER_ANY;
        return ParsingConstants.MARKER_NOTHING;
    }

    /**
     * Retrieve the all matcher.
     *
     * @return A matcher that matches every IP.
     */
    public static IPDBMatcher getAllMatcher()
    {
        return ALL_MATCHER;
    }

    /**
     * Retrieve the nil matcher.
     *
     * @return A matcher that never matches an IP.
     */
    public static IPDBMatcher getNilMatcher()
    {
        return NOTHING_MATCHER;
    }
    
    /* This is the parser for simple ip matchers */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>() 
    {
        /* This should always be checked first because it has the most
         * specific definition */
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
        
        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip simple matcher '" + value + "'" );
            }
            
            if ( value.equalsIgnoreCase( ParsingConstants.MARKER_ANY ) || 
                 value.equalsIgnoreCase( ParsingConstants.MARKER_WILDCARD ) ||
                 value.equalsIgnoreCase( ParsingConstants.MARKER_ALL )) {
                     return ALL_MATCHER;
                 } else if ( value.equalsIgnoreCase( ParsingConstants.MARKER_NOTHING )) {
                     return NOTHING_MATCHER;
                 }
            
            throw new ParseException( "Invalid ip simple matcher '" + value + "'" );
        }
    };
}

