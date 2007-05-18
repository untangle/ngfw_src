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

/**
 * Interface matcher that matches more than one interfaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IntfSetMatcher extends IntfDBMatcher
{
    /* Cache of the created interface matchers */
    static Map<ImmutableBitSet,IntfSetMatcher> CACHE = new HashMap<ImmutableBitSet,IntfSetMatcher>();

    /* Set of bits that shouldn't match */
    private final ImmutableBitSet intfSet;

    /* String representation for the database */
    private final String databaseRepresentation;

    /* String representation for the user */
    private final String userRepresentation;
    
    private IntfSetMatcher( ImmutableBitSet intfSet, String userRepresentation, 
                            String databaseRepresentation )
    {
        this.intfSet = intfSet;
        this.databaseRepresentation = databaseRepresentation;
        this.userRepresentation = userRepresentation;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     */
    public boolean isMatch( byte intf )
    {
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

    /**
     * Create an Set Matcher.
     * 
     * @param intfArray array of interfaces that should match.
     */
    public static IntfDBMatcher makeInstance( byte ... intfArray ) throws ParseException
    {
        BitSet intfSet = new BitSet( IntfConstants.MAX_INTF );
        
        IntfMatcherUtil imu = IntfMatcherUtil.getInstance();
        
        /* The first pass is to just fill the bitset */
        for ( byte intf : intfArray ) intfSet.set( intf );
        
        String databaseRepresentation = "";
        String userRepresentation = "";

        /* Second pass, build up the user and database string */
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

    /**
     * Parser for the set matcher. */
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

    /**
     * Create or retrieve the Interface matcher for this bit set.
     * Only create a new bitset if there isn't one in the cache.
     * 
     * @param intfSet BitSet to use.
     * @param user User string for this interface matcher.
     * @param database Database representation for this bitset.
     */
    private static IntfDBMatcher makeInstance( BitSet intfSet, String user, String database )
    {
        ImmutableBitSet i = new ImmutableBitSet( intfSet );
        IntfSetMatcher cache = CACHE.get( i );
        if ( cache == null ) CACHE.put( i, cache = new IntfSetMatcher( i, user, database ));

        return cache;
    }
}
