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

package com.untangle.mvvm.tran.firewall.ip;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;

/**
 * An IPMatcher that matches a range of addresses.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
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
    public static IPDBMatcher makeRangeInstance( IPaddr start, IPaddr end )
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
                return makeRangeInstance( IPaddr.parse( ipArray[0] ).getAddr(),
                                          IPaddr.parse( ipArray[1] ).getAddr());
            } catch ( UnknownHostException e ) {
                throw new ParseException( e );
            }
        }
    };
}

