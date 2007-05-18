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

import com.untangle.mvvm.IntfConstants;

import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.firewall.Parser;
import com.untangle.mvvm.tran.firewall.ParsingConstants;

/**
 * An IntfMatcher that matches a single interface.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public final class IntfSingleMatcher extends IntfDBMatcher
{
    /* Interface matcher for the external interface */
    private static final IntfDBMatcher EXTERNAL_MATCHER;

    /* Interface matcher for the internal interface */
    private static final IntfDBMatcher INTERNAL_MATCHER;

    /* Interface matcher for the dmz interface */
    private static final IntfDBMatcher DMZ_MATCHER;

    /* Interface matcher for the vpn interface */
    private static final IntfDBMatcher VPN_MATCHER;

    /* Map of interfaces to their matcher */
    static Map<Byte,IntfSingleMatcher> CACHE = new HashMap<Byte,IntfSingleMatcher>();

    /* The interface this matcher matches */
    private final byte intf;

    /* Database representation of this interface matcher */
    private final String databaseRepresentation;

    /* User representation of this interface matcher */
    private final String userRepresentation;

    private IntfSingleMatcher( byte intf, String userRepresentation, String databaseRepresentation )
    {
        this.intf = intf;
        this.databaseRepresentation = databaseRepresentation;
        this.userRepresentation = userRepresentation;
    }

    /**
     * Test if <param>intf<param> matches this matcher.
     *
     * @param intf Interface to test.
     * @return True if the interface matches.
     */
    public boolean isMatch( byte intf )
    {        
        /* These interfaces always match true */
        if ( IntfConstants.UNKNOWN_INTF == intf || IntfConstants.LOOPBACK_INTF == intf ) return true;

        if ( intf >= IntfConstants.MAX_INTF ) return false;
        
        return ( this.intf == intf );
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
     * Retrieve the External matcher */
    public static IntfDBMatcher getExternalMatcher()
    {
        return EXTERNAL_MATCHER;
    }

    /**
     * Retrieve the Internal matcher */
    public static IntfDBMatcher getInternalMatcher()
    {
        return INTERNAL_MATCHER;
    }

    /**
     * Retrieve the DMZ matcher */
    public static IntfDBMatcher getDmzMatcher()
    {
        return DMZ_MATCHER;
    }

    /**
     * Retrieve the VPN matcher */
    public static IntfDBMatcher getVpnMatcher()
    {
        return VPN_MATCHER;
    }

    public static IntfDBMatcher makeInstance( byte intf ) throws ParseException
    {
        IntfMatcherUtil imu = IntfMatcherUtil.getInstance();
        return makeInstance( intf, imu.intfToUser( intf ), imu.intfToDatabase( intf ));
    }

    private static IntfDBMatcher makeInstance( byte intf, String user, String database )
    {
        IntfSingleMatcher cache = CACHE.get( intf );
        if ( cache == null ) CACHE.put( intf, cache = new IntfSingleMatcher( intf, user, database ));
        return cache;
    }

    /* The parser for the single matcher */
    static final Parser<IntfDBMatcher> PARSER = new Parser<IntfDBMatcher>() 
    {
        public int priority()
        {
            return 1;
        }
        
        public boolean isParseable( String value )
        {
            return ( !value.contains( ParsingConstants.MARKER_SEPERATOR ));
        }
        
        public IntfDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid intf single matcher '" + value + "'" );
            }
            
            return makeInstance( IntfMatcherUtil.getInstance().databaseToIntf( value ));
        }
    };

    static
    {
        IntfDBMatcher external, internal, dmz, vpn;

        try {
            external = makeInstance( IntfConstants.EXTERNAL_INTF );
            internal = makeInstance( IntfConstants.INTERNAL_INTF );
            dmz      = makeInstance( IntfConstants.DMZ_INTF );
            vpn      = makeInstance( IntfConstants.VPN_INTF );
        } catch ( ParseException e ) {
            System.err.println( "This should never occur" );
            e.printStackTrace();
            /* Just so the compiler doesn't complain about unitialized values */
            vpn = dmz = internal = external = null;
        }

        EXTERNAL_MATCHER = external;
        INTERNAL_MATCHER = internal;
        DMZ_MATCHER      = dmz;
        VPN_MATCHER      = vpn;
    }
}

