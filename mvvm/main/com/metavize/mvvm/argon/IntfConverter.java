/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: IntfConverter.java,v 1.6 2005/01/25 01:20:19 rbscott Exp $
 */

package com.metavize.mvvm.argon;

import org.apache.log4j.Logger;
import com.metavize.jnetcap.*;

public final class IntfConverter
{
    /** This class is a singleton that must be initialized */

    /* Inside and outside interface constants */
    public static final byte    OUTSIDE = 0;
    public static final byte    INSIDE  = 1;

    public static final byte UNKNOWN_INTERFACE = -2;

    /* The constants for the DMZ zones should be looked up with the function
     * dmz( zone ), this will throw an error if that zone doesn't exist */
    private static final byte DMZ_1   = 2;
    
    /**
     * Greatest number of possible interfaces
     */
    private static final byte MAX_INTERFACE = Netcap.MAX_INTERFACE;
    private static final byte DMZ_MAX       = MAX_INTERFACE - DMZ_1;

    private static final byte INVALID_INTERFACE = -1;

    /**
     * An array for converting netcap interfaces to argon interfaces
     */
    private final byte toArgon[] = new byte[MAX_INTERFACE];

    /**
     * An array for converting argon interfaces to netcap interfaces
     */
    private final byte toNetcap[] = new byte[MAX_INTERFACE];

    private static IntfConverter INSTANCE = null;

    private Logger logger;

    private IntfConverter( String inside, String outside, String dmz[] )
    {        
        byte intf;

        logger = Logger.getLogger(IntfConverter.class.getName());

        /* Invalidate all of the interfaces */
        for ( int c = 0 ; c < MAX_INTERFACE; c++ ) {
            toArgon[c] = INVALID_INTERFACE;
            toNetcap[c] = INVALID_INTERFACE;
        }
        setInterface( outside, OUTSIDE );
        setInterface( inside, INSIDE );
        
        int length = ( dmz == null ) ? 0 : dmz.length;
        
        if ( length == 0 ) return;
        
        for ( int c = 0 ; c < length ; c++ ) {
            setInterface( dmz[c], (byte)( DMZ_1 + c ));
        }

    }

    /**
     * Set an interface.
     */
    private void setInterface( String intfName, byte argonIntf )
    {
        byte netcapIntf = Netcap.convertStringToIntf( intfName );
        
        if ( toNetcap[argonIntf] != INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Cannot set the toNetcap argument twice: " + argonIntf );
        }
        
        if ( toArgon[netcapIntf] != INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Cannot place the same interface in two locations: " + 
                                                intfName );
        }

        toArgon[netcapIntf] = argonIntf;
        toNetcap[argonIntf] = netcapIntf;        
    }

    /**
     * Lookup the netcap interface identifier for the inside interface
     */
    public static byte inside()
    {
        return INSTANCE.toNetcap[INSIDE];
    }

    /**
     * Lookup the netcap interface identifier for the outside interface
     */
    public static byte outside()
    {
        return INSTANCE.toNetcap[OUTSIDE];
    }

    /**
     * Convert an interface using the argon standard (0 = outside, 1 = inside, 2 = DMZ 1, etc)
     * to an interface that uses that netcap unique identifiers 
     */
    public static byte toNetcap( byte argonIntf )
    {
        if ( argonIntf > INSTANCE.toNetcap.length ) {
            throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
        }

        byte netcapIntf = INSTANCE.toNetcap[argonIntf];

        if ( netcapIntf == INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
        }
        
        return netcapIntf;
    }


    /**
     * Convert an interface from a netcap interface to the argon standard
     */
    public static byte toArgon( byte netcapIntf )
    {
        if ( netcapIntf > INSTANCE.toArgon.length ) {
            throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
        }

        byte argonIntf = INSTANCE.toArgon[netcapIntf];

        if ( argonIntf == INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
            
        }
        
        return argonIntf;
    }
    /**
     * Lookup the argon interface identifier for a DMZ zone.
     * @param zone - Number from 1 to DMZ_MAX which indicates which zone to lookup.
     */
    public static byte dmz( int zone )
    {
        byte argonIntf = (byte)( zone + DMZ_1 - 1 );
        if (( argonIntf < DMZ_1 ) || ( argonIntf >= MAX_INTERFACE )) {
            throw new IllegalArgumentException( "Zone must be between 1 and " + INSTANCE.DMZ_MAX +
                                                "\nZone: " + zone );
        }

        if ( INSTANCE.toNetcap[argonIntf] == INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid Argon Interface: " + argonIntf );
        }
        
        return argonIntf;
    }

    public static void validateNetcapIntf( byte netcapIntf )
    {
        if ( netcapIntf < 0 || netcapIntf > MAX_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
        }

        if ( INSTANCE.toArgon[netcapIntf] == INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
        }
    }

    public static void validateArgonIntf( byte argonIntf )
    {
        if ( argonIntf < 0 || argonIntf > MAX_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
        }

        if ( INSTANCE.toNetcap[argonIntf] == INVALID_INTERFACE ) {
            throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
        }
    }

    /* Initialize the INSTANCE */
    public synchronized static void init( String inside, String outside, String dmz[] )
    {
        if ( INSTANCE == null ) {
            INSTANCE = new IntfConverter( inside, outside, dmz );
            INSTANCE.logger.info("IntfConverted init: inside = " + inside + ", outside =  " + outside);
        } else {
            throw new IllegalStateException( "Attempt to initialize the IntfConverter Twice" );
        }
    }
}
