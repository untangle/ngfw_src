/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tran.firewall;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;


/**
 * The class <code>IPMatcher</code> represents an method for determining if an IP address
 * is a match.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public class IPMatcher
{
    public static final String MARKER_SUBNET    = "/";
    public static final String MARKER_RANGE     = "-";
    public static final String MARKER_WILDCARD  = "*";
    
    public static final IPMatcher MATCHER_ALL = new IPMatcher( 0, 0, false );

    static final int INADDRSZ = 4;

    /* Base address for the rule */
    private final long base;
    
    /**
     * second: 
     * Multipurpose second argument.
     * 1. base   : The matcher is just for one ip.
     * 2. <mask> : The matcher uses the subnet syntax.
     * 3. <end>  : The top of the range if the matcher uses the range syntax.
     */
    private final long second;
    
    /**
     * isRange: True if end is used, false if mask is used
     */
    private final boolean isRange;
        
    public IPMatcher( Inet4Address base, Inet4Address second, boolean isRange ) 
    {
        long tmpBase;
        long tmpSecond;

        this.isRange = isRange;
        tmpBase = toLong( base );
        
        if ( isRange ) {
            tmpSecond = toLong( second );
            

            /* If the range is reversed, swap the two */
            if (  tmpSecond < tmpBase ) {
                long tmp  = tmpSecond;
                tmpSecond = tmpBase;
                tmpBase   = tmp;
            }
        } else {
            tmpSecond = toLong( second );

            tmpBase   = tmpBase & tmpSecond;
        }

        this.base   = tmpBase;
        this.second = tmpSecond;
    }

    public IPMatcher( Inet4Address base )
    {
        this.second = this.base   = toLong( base );
        isRange     = true;
    }
    
    /* Internal for creating ip matchers */
    IPMatcher( long base, long second, boolean isRange )
    {
        this.base    = base;
        this.second  = second;
        this.isRange = isRange;
    }
    
    /**
     * An IPMatcher can be specified in one of the following formats:
     * 1. Just an IP address: (x.y.w.z)
     * 2. An IP address and a subnet (x.y.w.z/a.b.c.d)
     * 3. An IP address range (start to finish): (x.y.w.z - a.b.c.d)
     * 4. * : Wildcard matches everything.
     */
    public static IPMatcher parse( String str ) throws IllegalArgumentException
    {                                                       
        String marker;
        boolean isRange;
        long base;
        long second;

        str = str.trim();

        if ( str.indexOf( MARKER_SUBNET ) > 0 ) {
            marker = MARKER_SUBNET;
            isRange = false;
        } else if ( str.indexOf( MARKER_RANGE ) > 0 ) {
            marker = MARKER_RANGE;
            isRange = true;            
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD )) {
            return MATCHER_ALL;
        } else {
            base = toLong( str );
            /* Just an address a range where the start and end are the same */
            return new IPMatcher( base, base, true );
        }

        String strArray[] = str.split( marker, 3 );

        if ( strArray.length != 2 ) {
            throw new IllegalArgumentException( "Invalid IPMatcher, more than two components" + str );
        }
        
        base   = toLong( strArray[0] );

        /* XXX May need to guarantee a valid mask? */
        second = toLong( strArray[1] );
        
        return new IPMatcher( base, second, isRange );
    }

    public boolean isMatch( InetAddress addr)
    {
        long tmp = toLong((Inet4Address)addr );

        if ( isRange ) {
            if (( base <= tmp ) && ( tmp <= second )) return true;
        } else {
            /* Mask off the bits from the subnet */
            if (( tmp & second ) == base ) return true;
        }
        
        return false;
    }
    
    /** Convert a dot notation string to a long */    
    static long toLong( String dotNotation )
    {
        long val = 0;
        
        /* Trim any whitespace */
        dotNotation = dotNotation.trim();

        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = dotNotation.split( "\\.", INADDRSZ + 1 );

        if ( tmp.length != INADDRSZ ) {
            throw new IllegalArgumentException( "Invalid IPV4 dot-notation address" + dotNotation );
        }

        /* Validation */
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            int part = Integer.parseInt( tmp[c] );
            if (( part < 0 ) || ( part > 255 )) {
                throw new IllegalArgumentException( "Each component must be between 0 and 255 " + tmp);
            }
            
            val += (long)(part << ( 8 * c ));
        }

        return val;
    }

    /** Convert and address to a long */
    static long toLong( Inet4Address address )
    {
        long val = 0;
        
        byte valArray[] = address.getAddress();
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * c );
        }

        return val;
    }

    static int byteToInt ( byte val ) 
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }
}
