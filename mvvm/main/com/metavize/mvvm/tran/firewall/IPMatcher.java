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

import java.io.Serializable;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;


/**
 * The class <code>IPMatcher</code> represents an method for determining if an IP address
 * is a match.
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public final class IPMatcher implements Serializable
{
    public static final String MARKER_SUBNET    = "/";
    public static final String MARKER_ANY       = "any";
    public static final String MARKER_ALL       = "all";
    public static final String MARKER_LOCAL     = "local";
    public static final String MARKER_EG        = "edgeguard";
    public static final String MARKER_RANGE     = MatcherStringConstants.RANGE;
    public static final String MARKER_WILDCARD  = MatcherStringConstants.WILDCARD;

    private static final String MARKER_NOTHING  = MatcherStringConstants.NOTHING;
    
    /* A matcher for all traffic */
    public static final IPMatcher MATCHER_ALL   = new IPMatcher( 0, 0, false );

    /* Use a range that goes backwards for the nil matcher */
    public static final IPMatcher MATCHER_NIL   = new IPMatcher( 1, 0, true );

    /* A range for matching local traffic */
    public static final IPMatcher MATCHER_LOCAL = new IPMatcher( true );

    static InetAddress localAddress = null;
    static long        localLong = -1L;

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

    /**
     * isLocal: True if this is a matcher for local traffic 
     */
    private final boolean isLocal;
        
    public IPMatcher( Inet4Address base, Inet4Address second, boolean isRange ) 
    {
        long tmpBase;
        long tmpSecond;

        this.isRange = isRange;
        tmpBase = toLong( base );
        
        isLocal = false;
        
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

    public IPMatcher( IPaddr base, IPaddr second, boolean isRange )
    {
        this((Inet4Address)base.getAddr(), (Inet4Address)second.getAddr(), isRange );
    }


    public IPMatcher( Inet4Address base )
    {
        this.second = this.base = toLong( base );
        isRange     = true;
        isLocal     = false;
    }

    public IPMatcher( IPaddr base )
    {
        this((Inet4Address)base.getAddr());
    }
    
    /* Internal for creating ip matchers */
    IPMatcher( long base, long second, boolean isRange )
    {
        this.second  = second;
        this.isRange = isRange;
        
        if ( isRange ) {
            this.base    = base;
        } else {
            /* Must mask off the unused bits for the subnet */
            this.base    = base & second;
        }

        this.isLocal = false;
    }
    
    /* Internal for creating local ip matchers */
    private IPMatcher( boolean isLocal )
    {
        this.base    = 0;
        this.second  = 0;
        this.isRange = true;
        this.isLocal = true;
    }

    /**
     * An IPMatcher can be specified in one of the following formats:
     * 1. Just an IP address: (x.y.w.z)
     * 2. An IP address and a subnet (x.y.w.z/a.b.c.d)
     * 3. An IP address range (start to finish): (x.y.w.z - a.b.c.d)
     * 4. * : Wildcard matches everything.
     */
    public static IPMatcher parse( String str ) throws ParseException
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
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD ) || str.equalsIgnoreCase( MARKER_ANY ) ||
                    str.equalsIgnoreCase( MARKER_ALL )) {
            return MATCHER_ALL;
        } else if ( str.equalsIgnoreCase( MARKER_NOTHING )) {
            return MATCHER_NIL;
        } else if ( str.equalsIgnoreCase( MARKER_EG ) || str.equalsIgnoreCase( MARKER_LOCAL )) {
            return MATCHER_LOCAL;
        } else {
            base = toLong( str );
            /* Just an address a range where the start and end are the same */
            return new IPMatcher( base, base, true );
        }

        String strArray[] = str.split( marker, 3 );

        if ( strArray.length != 2 ) {
            throw new ParseException( "Invalid IPMatcher, more than two components" + str );
        }
        
        base   = toLong( strArray[0].trim());

        /* XXX May need to guarantee a valid mask? */
        second = toLong( strArray[1].trim());
        
        /* Swap the values around */
        if ( isRange && ( second < base )) {
            long tmp = second;
            second = base;
            base = tmp;
        }
        
        return new IPMatcher( base, second, isRange );
    }

    public boolean isMatch( InetAddress addr)
    {
        long tmp = toLong((Inet4Address)addr );
        
        // Just for testing
        // System.out.println( "matcher[ base: " + base + " second: " + second + " tmp: " + tmp + " isRange: " + isRange + "]" );

        if ( isLocal ) {
            return ( tmp == localLong );
        } else if ( isRange ) {
            if (( base <= tmp ) && ( tmp <= second )) return true;
        } else {
            /* Mask off the bits from the subnet */
            if (( tmp & second ) == base ) return true;
        }
        
        return false;
    }

    public String toString()
    {
        if ( isLocal ) {
            return MARKER_LOCAL;
        }

        /* Check for the wildcard matcher */
        if ( this.equals( MATCHER_ALL ) || ( !isRange && ( base == 0 ) && ( second == 0 ))) {
            return MARKER_ANY;
        } else if ( this.equals( MATCHER_NIL ) || ( isRange && ( base > second ))) {
            return MARKER_NOTHING;
        }
        
        if ( base == second ) {
            return baseToString();
        }
        
        /* Otherwise, convert to one of the notations */
        String marker = ( isRange ) ? MARKER_RANGE : MARKER_SUBNET;
        
        return baseToString() + " " + marker + " " + secondToString();
    }
    
    public boolean equals( Object o )
    {
        if (!( o instanceof IPMatcher )) return false;
        
        IPMatcher i = (IPMatcher)o;
        return (( i.base == this.base ) && ( i.second == this.second ) &&
                ( i.isRange == this.isRange ) && ( i.isLocal == this.isLocal ));
    }
    
    private String baseToString()
    {
        return toString( base );
    }
    
    private String secondToString()
    {
        return toString( second );
    }
    
    /** Convert a dot notation string to a long */    
    static long toLong( String dotNotation ) throws ParseException
    {
        long val = 0;
        
        /* Trim any whitespace */
        dotNotation = dotNotation.trim();

        /* Use five to guarantee it doesn't converted from x.x.x.x.x to { x, x, x, x.x } */
        String tmp[] = dotNotation.split( "\\.", INADDRSZ + 1 );

        if ( tmp.length != INADDRSZ ) {
            throw new ParseException( "Invalid IPV4 dot-notation address" + dotNotation );
        }

        /* Validation */
        for ( int c = 0 ; c < tmp.length ; c++ ) {
            int part = Integer.parseInt( tmp[c] );
            if (( part < 0 ) || ( part > 255 )) {
                throw new ParseException( "Each component must be between 0 and 255 " + tmp);
            }
            
            val += (((long)part) << ( 8 * ( INADDRSZ - c - 1 )));
        }
        
        if ( val < 0 ) val += 0x100000000L;

        return val;
    }

    public static void setLocalAddress( IPaddr address )
    {
        setLocalAddress( address.getAddr());
    }

    public static void setLocalAddress( InetAddress address )
    {
        localAddress = address;

        /* If the input address is empty, do not match anything */
        localLong = ( address == null ) ? -1L : toLong((Inet4Address)address );

        if ( localLong == 0 ) {
            localLong = -1L;
        }
    }

    public static InetAddress getLocalAddress()
    {
        return localAddress;
    }

    /** Convert and address to a long */
    static long toLong( Inet4Address address )
    {
        long val = 0;
        
        byte valArray[] = address.getAddress();
        
        for ( int c = 0 ; c < INADDRSZ ; c++ ) {
            val += ((long)byteToInt(valArray[c])) << ( 8 * ( INADDRSZ - c - 1 ));
        }

        return val;
    }

    static int byteToInt ( byte val ) 
    {
        int num = val;
        if ( num < 0 ) num = num & 0x7F + 0x80;
        return num;
    }

    private static String toString( long addr )
    {
        String addrString = "";

        for ( int c = INADDRSZ ; --c >= 0  ; ) {
            addrString += (int)((addr >> ( 8 * c )) & 0xFF);
            if ( c > 0 )
                addrString += ".";
        }
        
        return addrString;
    }
}
