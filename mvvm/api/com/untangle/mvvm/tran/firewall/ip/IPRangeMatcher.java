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

public final class IPRangeMatcher extends IPDBMatcher
{
    private static final String MARKER_RANGE = IPMatcherUtil.MARKER_RANGE;

    private final long start, end;
    private final String string;

    private IPRangeMatcher( long start, long end, String string )
    {
        this.start = start;
        this.end = end;
        this.string  = string;
    }

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

    public static IPDBMatcher makeRangeInstance( IPaddr network, IPaddr netmask )
    {
        return makeRangeInstance( network.getAddr(), netmask.getAddr());
    }

    public static IPDBMatcher makeRangeInstance( InetAddress start, InetAddress end )
    {
        if ( start == null || end == null ) throw new NullPointerException( "Null address" );

        IPMatcherUtil imu = IPMatcherUtil.getInstance();

        String user = start.getHostAddress() + " " + MARKER_RANGE +  " " + end.getHostAddress();

        return new IPRangeMatcher( imu.toLong( start ), imu.toLong( end ), user );
    }

    /* This is just for matching a list of interfaces */
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

