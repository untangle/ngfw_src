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

package com.untangle.mvvm.tran.firewall.intf;

import java.util.Map;
import java.util.HashMap;
import java.util.BitSet;

import com.untangle.mvvm.IntfConstants;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

public final class IntfSetMatcher extends IntfDBMatcher
{
    static Map<ImmutableBitSet,IntfSetMatcher> CACHE = new HashMap<ImmutableBitSet,IntfSetMatcher>();

    private final ImmutableBitSet intfSet;
    private final String databaseRepresentation;
    private final String userRepresentation;
    
    private IntfSetMatcher( ImmutableBitSet intfSet, String userRepresentation, 
                            String databaseRepresentation )
    {
        this.intfSet = intfSet;
        this.databaseRepresentation = databaseRepresentation;
        this.userRepresentation = userRepresentation;
    }

    public boolean isMatch( byte intf )
    {
        /* This test has to come before the second test */
        /* This always matches true */
        if ( IntfConstants.UNKNOWN_INTF == intf || IntfConstants.LOOPBACK_INTF == intf ) return true;

        if ( intf >= IntfConstants.MAX_INTF ) return false;

        return intfSet.get( intf );
    }

    public String toDatabaseString()
    {
        return this.databaseRepresentation;
    }
    
    public String toString()
    {
        return this.userRepresentation;
    }

    /** !!! Why throw an exception, why not just ignore the ones that are not needed */
    public static IntfDBMatcher makeInstance( byte ... intfArray ) throws ParseException
    {
        BitSet intfSet = new BitSet( IntfConstants.MAX_INTF );
        
        IntfMatcherUtil imu = IntfMatcherUtil.getInstance();
        
        /* The first pass is to just fill the bitset */
        for ( byte intf : intfArray ) {
            imu.intfToDatabase( intf );
            intfSet.set( intf );
        }

        
        String databaseRepresentation = "";
        String userRepresentation = "";

        for ( int intf = intfSet.nextSetBit( 0 ) ; intf >= 0 ; intf = intfSet.nextSetBit( intf + 1 )) {
            if ( databaseRepresentation.length() > 0 ) {
                databaseRepresentation += ",";
                userRepresentation += " & ";
            }
            databaseRepresentation += imu.intfToDatabase((byte)intf );
            userRepresentation += imu.intfToUser((byte)intf );
        }
        
        
        return makeInstance( intfSet, userRepresentation, databaseRepresentation );
    }

    /* This is just for matching a list of interfaces */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>() 
    {
        /* This has to be after the inverse matcher, because isParseable will
         * pass since anything with the seperator will pass */
        public int priority()
        {
            return 3;
        }
        
        public boolean isParseable( String value )
        {
            return ( value.contains( ParsingConstants.MARKER_SEPERATOR ));
        }
        
        public IntfDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid intf set matcher '" + value + "'" );
            }

            /* Split up the items */
            String databaseArray[] = value.split( ParsingConstants.MARKER_SEPERATOR );

            byte intfArray[] = new byte[databaseArray.length];
            
            int c = 0;
            IntfMatcherUtil imu = IntfMatcherUtil.getInstance();

            for ( String databaseString : databaseArray ) {
                intfArray[c++] = imu.databaseToIntf( databaseString );
            }
            
            return makeInstance( intfArray );
        }
    };


    private static IntfDBMatcher makeInstance( BitSet intfSet, String user, String database )
    {
        ImmutableBitSet i = new ImmutableBitSet( intfSet );
        IntfSetMatcher cache = CACHE.get( i );
        // if ( cache == null ) System.out.println( "creating new set" );
        if ( cache == null ) CACHE.put( i, cache = new IntfSetMatcher( i, user, database ));

        return cache;
    }
}
