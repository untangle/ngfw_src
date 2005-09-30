/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import com.metavize.jnetcap.*;

public final class IntfConverter
{
    /** This class is a singleton that must be initialized */

    /* Inside and outside interface argon constants */
    public static final byte  OUTSIDE   = 0;
    public static final byte  INSIDE    = 1;
    public static final byte  DMZ       = 2;
    
    /* Special argon interfaces */
    public static final byte  ARGON_MIN      = 0;
    public static final byte  ARGON_MAX      = 8;

    public static final byte  ARGON_ERROR    = ARGON_MIN - 1;
    public static final byte  ARGON_LOOPBACK = ARGON_MAX + 1;
    public static final byte  ARGON_UNKNOWN  = ARGON_MAX + 2;

    public static final byte  NETCAP_MIN      = 1;
    public static final byte  NETCAP_MAX      = 9;

    /* Magic numbers, all of them */
    public static final byte  NETCAP_ERROR    = 0;
    public static final byte  NETCAP_LOOPBACK = 17;
    public static final byte  NETCAP_UNKNOWN  = 18;
    
    /* The constants for the DMZ zones should be looked up with the function
     * dmz( zone ), this will throw an error if that zone doesn't exist */
    private static final byte USER_BASE = 3;
    
    /**
     * Greatest number of possible interfaces
     */
    private static final byte MAX_INTERFACE = Netcap.MAX_INTERFACE;
    private static final byte USER_MAX      = MAX_INTERFACE - USER_BASE;

    private static final byte INVALID_INTERFACE = -1;

    private final String insideString;
    private final String outsideString;
    private final String dmzString;
    private final String userString[];

    private static IntfConverter INSTANCE = null;

    private Logger logger;

    private IntfConverter( String inside, String outside, String dmz, String user[] )
    {        
        byte intf;

        logger = Logger.getLogger( this.getClass());

        this.insideString  = inside;
        this.outsideString = outside;
        this.dmzString     = dmz;
        this.userString    = user;
    }

    /**
     * Lookup the netcap interface identifier for the inside interface
     */
    public static byte inside()
    {
        return 2;
    }

    /**
     * Lookup the netcap interface identifier for the outside interface
     */
    public static byte outside()
    {
        return 1;
    }

    /**
     * Lookup the netcap interface identifier for the dmz
     */
    public byte dmz()
    {
        return 3;
    }

    public boolean hasDmz()
    {
        return ( this.dmzString != null && this.dmzString.length() > 0 );
    }

    public byte[] getArgonIntfArray()
    {
        /* XXX User interfaces, how to do VPN, etc. */
        if ( hasDmz()) {
            return new byte[]{ 0, 1, 2 };
        } else {
            return new byte[]{ 0, 1 };
        }
    }

    

    public String argonIntfToString( byte argonInterface ) throws ArgonException
    {
        switch ( argonInterface ) {
        case OUTSIDE:
            return outsideString;

        case INSIDE:
            return insideString;
            
        case DMZ:
            return dmzString;
            
            /* XXX Insert the user interfaces */
            
        default:
            throw new ArgonException( "Unable to convert " + argonInterface + " to a string" );
        }
    }

    /**
     * Convert an interface using the argon standard (0 = outside, 1 = inside, 2 = DMZ 1, etc)
     * to an interface that uses that netcap unique identifiers 
     */
    public static byte toNetcap( byte argonIntf )
    {
        switch ( argonIntf ) {
        case ARGON_ERROR:
            throw new IllegalArgumentException( "Invalid argon interface[" + argonIntf + "]" );
        case ARGON_UNKNOWN:  return NETCAP_UNKNOWN;
        case ARGON_LOOPBACK: return NETCAP_LOOPBACK;
        }
        
        /* May actually want to check if interfaces exists */
        if ( argonIntf < ARGON_MIN  || argonIntf > ARGON_MAX ) {
            throw new IllegalArgumentException( "Invalid argon interface[" + argonIntf + "]" );
        }
        
        return (byte)(argonIntf + 1);
    }


    /**
     * Convert an interface from a netcap interface to the argon standard
     */
    public static byte toArgon( byte netcapIntf )
    {
        switch ( netcapIntf ) {
        case NETCAP_ERROR:
            throw new IllegalArgumentException( "Invalid netcap interface[" + netcapIntf + "]" );
        case NETCAP_UNKNOWN:  return ARGON_UNKNOWN;
        case NETCAP_LOOPBACK: return ARGON_LOOPBACK;
        }
        
        /* May actually want to check if interfaces exists */
        if ( netcapIntf < NETCAP_MIN  || netcapIntf > NETCAP_MAX ) {
            throw new IllegalArgumentException( "Invalid netcap interface[" + netcapIntf + "]" );
        }
        
        return (byte)(netcapIntf - 1);
    }

    /**
     * Lookup the argon interface identifier for a USER zone.
     */
    public static byte user( byte zone )
    {
//         byte argonIntf = (byte)( zone + DMZ_1 - 1 );
//         if (( argonIntf < DMZ_1 ) || ( argonIntf >= MAX_INTERFACE )) {
//             throw new IllegalArgumentException( "Zone must be between 1 and " + INSTANCE.DMZ_MAX +
//                                                 "\nZone: " + zone );
//         }

//         if ( INSTANCE.toNetcap[argonIntf] == INVALID_INTERFACE ) {
//             throw new IllegalArgumentException( "Invalid Argon Interface: " + argonIntf );
//         }
        
//         return argonIntf;
        return 3;
    }

    /* XXXX 
     * This needs something.
     */
    public static void validateNetcapIntf( byte netcapIntf )
    {
//         if ( netcapIntf < 0 || netcapIntf > MAX_INTERFACE ) {
//             throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
//         }

//         if ( INSTANCE.toArgon[netcapIntf] == INVALID_INTERFACE ) {
//             throw new IllegalArgumentException( "Invalid netcap interface: " + netcapIntf );
//         }
    }

    public static void validateArgonIntf( byte argonIntf )
    {
//         if ( argonIntf < 0 || argonIntf > MAX_INTERFACE ) {
//             throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
//         }

//         if ( INSTANCE.toNetcap[argonIntf] == INVALID_INTERFACE ) {
//             throw new IllegalArgumentException( "Invalid argon interface: " + argonIntf );
//         }
    }

    /* Initialize the INSTANCE */
    public synchronized static void init( String inside, String outside, String dmz, String user[] ) 
        throws ArgonException
    {
        if ( INSTANCE == null ) {
            inside = inside.trim();
            outside = outside.trim();
            if ( dmz == null ) dmz = "";
            else dmz = dmz.trim();

            INSTANCE = new IntfConverter( inside, outside, dmz, user );
            INSTANCE.logger.info( "IntfConverted init: inside = '" + inside + "', outside = '" + 
                                  outside + "'" );
            INSTANCE.logger.info( "IntfConverted init: dmz '" + dmz + "'" );
                                    
            /* Create a new array large enough to hold all of the elements */
            List tmp = new LinkedList<String>();
            tmp.add( outside );
            tmp.add( inside );
            tmp.add( dmz );
            for ( String userInterface : user ) {
                INSTANCE.logger.info( "IntfConverted init: user interface '" + userInterface + "'" );
                tmp.add( userInterface );
            }
            
            try {
                /* use this method to typecast the array */
                Netcap.getInstance().configureInterfaceArray((String[])tmp.toArray( new String[0] ));
            } catch ( JNetcapException e ) {
                throw new ArgonException( "Error initialized string -> netcap interface map", e );
            }
         } else {
             throw new ArgonException( "Attempt to initialize the IntfConverter Twice" );
         }
    }

    public synchronized static IntfConverter getInstance()
    {
        return INSTANCE;
    }
}
