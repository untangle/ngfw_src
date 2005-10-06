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
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.tran.ParseException;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.IntfEnum;

/**
 * The class <code>IntfMatcher</code> represents a class for filtering on one of the interfaces
 * for a session. This matches MVVM interfaces(zero indexed) and NOT netcap interfaces(1 indexed).
 * This should be turned into a factory
 *
 * @author <a href="mailto:rbscott@metavize.com">rbscott</a>
 * @version 1.0
 */
public final class IntfMatcher implements Serializable
{
    private static final long serialVersionUID = 6995354637973561046L;

    public static IntfEnum INTF_ENUM = null;

    /* The maximum number of interfaces */
    public static final int    INTERFACE_MAX   = 5;
    public static final int    BITSET_MASK     = ( 1 << INTERFACE_MAX ) - 1;
    public static final String MARKER_INSIDE   = "I";
    public static final String MARKER_OUTSIDE  = "O";
    public static final String MARKER_DMZ      = "D";

    public static final String MARKER_WILDCARD  = MatcherStringConstants.WILDCARD;
    public static final String MARKER_SEP       = MatcherStringConstants.SEPERATOR;

    private static final String MARKER_NOTHING  = MatcherStringConstants.NOTHING;

    private static final int BITSET_ALL     = -1;
    private static final int BITSET_NOTHING = 0;
    private static final int BITSET_OUTSIDE = 1;
    private static final int BITSET_INSIDE  = 2;
    private static final int BITSET_DMZ     = 4;

    /* Using a map for the off chance that the number of interfaces may need to change
     * on the fly?, If there are more interface, this could be a caching structure
     * that just creates a new IntfMatcher only when necessary */
    public static final Map<Integer,IntfMatcher> MATCHER_MAP = new HashMap<Integer,IntfMatcher>();

    public static final Map<Byte,String> INTF_MARKER_MAP = new HashMap<Byte,String>();
    public static final Map<String,Byte> MARKER_INTF_MAP = new HashMap<String,Byte>();

    private static IntfMatcher ENUMERATION[] = new IntfMatcher[0];

    /**
     * A bit set of interfaces. (This only uses the first 8 bits, so it is safe that it is
     * signed).
     */
    public final int interfaceBitSet;
    final String databaseRepresentation;
    String userRepresentation = null;

    private IntfMatcher( int interfaceBitSet, String databaseRepresentation )
    {
        this.interfaceBitSet        = interfaceBitSet;
        this.databaseRepresentation = databaseRepresentation;
    }

    public boolean isMatch( byte intf )
    {
        /* This matches everything */
        if ( BITSET_ALL == this.interfaceBitSet ) return true;
        
        if ( BITSET_NOTHING == this.interfaceBitSet ) return false;

        /* Unknown and loopback always matches on outside */
        if ( IntfConverter.ARGON_UNKNOWN == intf || IntfConverter.ARGON_LOOPBACK == intf ) return true;
                
        /* XXX Possibly throw an exception */
        if ( intf >= INTERFACE_MAX ) return false;

        int mask = ( 1 << intf );

        /* Return true if the bit for the interface is set */
        return (( mask & this.interfaceBitSet ) == mask );
    }

    public String toString()
    {
        /* XXX This function is horrendous XXX */
        if ( this.userRepresentation == null ) {
            if ( this.interfaceBitSet == BITSET_ALL ) this.userRepresentation = "any";
            else {
                String s = null;
                if ( isMatch((byte)0 )) s = "Internal";
                if ( isMatch((byte)1 )) s = (( s == null ) ? "" : s + " & " ) + "External";
                if ( isMatch((byte)2 )) s = (( s == null ) ? "" : s + " & " ) + "DMZ";
                this.userRepresentation = s;
            }
        }
        
        return userRepresentation;
    }
    
    public String toDatabaseString()
    {
        return this.databaseRepresentation;
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof IntfMatcher )) return false;

        IntfMatcher i = (IntfMatcher)o;
        return ( i.interfaceBitSet == this.interfaceBitSet );
    }

    /**
     * An IntfMatcher can be specified in one of the following formats:
     * 1. (I|O)[,(I|O)]* Inside or outside (one of each) eg "I,O" or "I" or "O"
     * 2. * : Wildcard matches everything.
     * 3. ! : Nothing matches nothing.
     */
    public static IntfMatcher parse( String str ) throws ParseException
    {
        str = str.trim();
        int interfaceBitSet = 0;

        if ( str.indexOf( MARKER_SEP ) > 0 ) {
            String strArray[] = str.split( MARKER_SEP );
            for ( int c = 0 ; c < strArray.length ; c++ ) {
                if ( strArray[c].equalsIgnoreCase( MARKER_INSIDE )) {
                    interfaceBitSet |= ( 1 << IntfConverter.INSIDE );
                } else if ( strArray[c].equalsIgnoreCase( MARKER_OUTSIDE )) {
                    interfaceBitSet |= ( 1 << IntfConverter.OUTSIDE );
                } else if ( strArray[c].equalsIgnoreCase( MARKER_DMZ )) {
                    interfaceBitSet |= ( 1 << IntfConverter.DMZ );
                } else {
                    throw new ParseException( "Invalid IntfMatcher at \"" + strArray[c] + "\"" );
                }
            }
        } else if ( str.equalsIgnoreCase( MARKER_WILDCARD )) {
            return getAll();
        } else if ( str.equalsIgnoreCase( MARKER_NOTHING )) {
            return getNothing();
        } else if ( str.equalsIgnoreCase( MARKER_OUTSIDE ))  {
            interfaceBitSet = ( 1 << IntfConverter.OUTSIDE );
        } else if ( str.equalsIgnoreCase( MARKER_INSIDE )) {
            interfaceBitSet = ( 1 << IntfConverter.INSIDE );
        } else if ( str.equalsIgnoreCase( MARKER_DMZ ))  {
            interfaceBitSet = ( 1 << IntfConverter.DMZ );
        } else {
            throw new ParseException( "Invalid IntfMatcher at \"" + str + "\"" );
        }

        IntfMatcher matcher = MATCHER_MAP.get( interfaceBitSet );
        if ( matcher == null ) throw new ParseException( "Invalid IntfMatcher at \"" + str + "\"" );
        return matcher;
    }

    public static IntfMatcher getAll()
    {
        return MATCHER_MAP.get( BITSET_ALL );
    }

    public static IntfMatcher getNothing()
    {
        return MATCHER_MAP.get( BITSET_NOTHING );
    }

    public static IntfMatcher getOutside()
    {
        return MATCHER_MAP.get( BITSET_OUTSIDE );
    }

    public static IntfMatcher getNotInside()
    {
        int bitSet = ( ~BITSET_INSIDE ) & BITSET_MASK;
        IntfMatcher matcher = MATCHER_MAP.get( bitSet );
        return matcher;
    }

    public static IntfMatcher getInside()
    {
        return MATCHER_MAP.get( BITSET_INSIDE );
    }

    public static IntfMatcher getMatcher( int c )
    {
        return MATCHER_MAP.get( c );
    }

    /**
     * I love java?
     */
    private static void addMarker( int intf, String marker )
    {
        addMarker((byte)intf, marker );
    }

    private static void addMarker( byte intf, String marker )
    {
        MARKER_INTF_MAP.put( marker, intf );
        INTF_MARKER_MAP.put( intf, marker );
    }

    public static synchronized void updateEnumeration( IntfEnum intfEnum )
    {
        if ( INTF_ENUM == intfEnum ) return;

        IntfMatcher user[] = ( intfEnum.getIntfNums().length == 3 ) ? new IntfMatcher[5] : new IntfMatcher[3];
        int c=0;

        /* XXX This is just for DMZ */
        user[c++] = getInside();
        user[c++] = getOutside();

        if ( intfEnum.getIntfNums().length == 3 ) {
            user[c++] = getMatcher( BITSET_DMZ );
            user[c++] = getMatcher( BITSET_DMZ | BITSET_OUTSIDE );
        }

        user[c++] = getAll();
        
        /* Convert to an immutable list */
        ENUMERATION = user;
        
        /* Set the cache */
        INTF_ENUM = intfEnum;
    }

    public static IntfMatcher[] getEnumeration()
    {
        return ENUMERATION;
    }

    public static IntfMatcher getDefault()
    {
        return getEnumeration()[0];
    }
    
    static
    {
        addMarker( IntfConverter.OUTSIDE, MARKER_OUTSIDE );    // 0
        addMarker( IntfConverter.INSIDE, MARKER_INSIDE );      // 1
        addMarker( IntfConverter.DMZ, MARKER_DMZ );            // 2
        addMarker( 3, "U1" );
        addMarker( 4, "U2" );
        addMarker( 5, "U3" );
        addMarker( 6, "U4" );
        addMarker( 7, "U5" );

        for ( int c = 0 ; c < ( 1 << INTERFACE_MAX ) ; c++ ) {
            String databaseRepresentation = null;
            /* XXX This will have to change if the number of interfaces can change */
            if ( BITSET_NOTHING == c ) {
                databaseRepresentation = MARKER_NOTHING;
            } else {
                /* Cycle through each bit of checking for each interface */
                for ( int d = 0 ; d < INTERFACE_MAX ; d++ ) {
                    int mask = ( 1 << d );
                    if ( mask > c ) break;

                    if (( c & mask ) == mask ) {
                        String intfString = INTF_MARKER_MAP.get((byte)d );
                        if ( databaseRepresentation == null ) {
                            databaseRepresentation = intfString;
                        } else {
                            databaseRepresentation = databaseRepresentation + MARKER_SEP + intfString;
                        }
                    }
                }
            }
            MATCHER_MAP.put( c, new IntfMatcher( c, databaseRepresentation ));
        }

        MATCHER_MAP.put( BITSET_ALL, new IntfMatcher( BITSET_ALL, MARKER_WILDCARD ));
    }
}
