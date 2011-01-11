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
import java.net.UnknownHostException;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

/**
 * An IPMatcher that matches a range of addresses.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public final class IPRangeMatcher extends IPDBMatcher
{
    private static final String MARKER_RANGE = IPMatcherUtil.MARKER_RANGE;

    /* Start of the range, (inclusive) */
    private final long start;

    /* End of the range, (inclusive) */
    private final long end;

    /* Database/User representation of this matcher */
    private final String string;

    private IPRangeMatcher( long start, long end, String string )
    {
        this.start = start;
        this.end = end;
        this.string  = string;
    }

    /**
     * Test if <param>address<param> matches this matcher.
     *
     * @param address The address to test.
     * @return True if <param>address</param> in this range.
     */
    public boolean isMatch( InetAddress address )
    {
        long tmp = IPMatcherUtil.getInstance().toLong( address );

        return (( start <= tmp ) && ( tmp <= end ));
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
     * @param start The start of the range.
     * @param end The end of the range.
     * @return A RangeMatcher that matches IP address from
     * <param>start</param> to <param>end</param>, inclusive.
     */
    public static IPDBMatcher makeRangeInstance( IPAddress start, IPAddress end )
    {
        return makeRangeInstance( start.getAddr(), end.getAddr());
    }

    /**
     * Create a range matcher.
     *
     * @param start The start of the range.
     * @param end The end of the range.
     * @return A RangeMatcher that matches IP address from
     * <param>start</param> to <param>end</param>, inclusive.
     */
    public static IPDBMatcher makeRangeInstance( InetAddress start, InetAddress end )
    {
        if ( start == null || end == null ) throw new NullPointerException( "Null address" );

        IPMatcherUtil imu = IPMatcherUtil.getInstance();

        String user = start.getHostAddress() + " " + MARKER_RANGE +  " " + end.getHostAddress();

        return new IPRangeMatcher( imu.toLong( start ), imu.toLong( end ), user );
    }

    /**
     * The parser for the range matcher */
    static final Parser<IPDBMatcher> PARSER = new Parser<IPDBMatcher>()
    {
        public int priority()
        {
            return 10;
        }

        public boolean isParseable( String value )
        {
            return ( value.contains( MARKER_RANGE ));
        }

        public IPDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid ip range matcher '" + value + "'" );
            }

            String ipArray[] = value.split( MARKER_RANGE );
            if ( ipArray.length != 2 ) {
                throw new ParseException( "Range matcher contains two components: " + value );
            }

            try {
                return makeRangeInstance( IPAddress.parse( ipArray[0] ).getAddr(),
                                          IPAddress.parse( ipArray[1] ).getAddr());
            } catch ( UnknownHostException e ) {
                throw new ParseException( e );
            }
        }
    };
}

