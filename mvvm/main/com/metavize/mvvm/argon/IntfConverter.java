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

import java.util.Iterator;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import com.metavize.jnetcap.*;

import com.metavize.mvvm.IntfConstants;

public final class IntfConverter
{
    /** This class is a singleton that must be initialized */

    /* Inside and outside interface argon constants */
    static final byte  OUTSIDE        = IntfConstants.EXTERNAL_INTF;
    static final byte  NETCAP_OUTSIDE = OUTSIDE + 1;
    static final byte  INSIDE         = IntfConstants.INTERNAL_INTF;
    static final byte  NETCAP_INSIDE  = INSIDE + 1;
    static final byte  DMZ            = IntfConstants.DMZ_INTF;
    static final byte  NETCAP_DMZ     = DMZ + 1;
    static final byte  VPN            = IntfConstants.VPN_INTF;
    static final byte  NETCAP_VPN     = VPN + 1;

    /* Special argon interfaces */
    public static final byte  ARGON_MIN      = 0;
    public static final byte  ARGON_MAX      = IntfConstants.MAX_INTF;

    public static final byte  ARGON_ERROR    = ARGON_MIN - 1;
    public static final byte  ARGON_LOOPBACK = IntfConstants.MAX_INTF;
    public static final byte  ARGON_UNKNOWN  = IntfConstants.UNKNOWN_INTF;

    public static final byte  NETCAP_MIN      = 1;
    public static final byte  NETCAP_MAX      = 9;

    /* Magic numbers, all of them */
    public static final byte  NETCAP_ERROR    = 0;
    public static final byte  NETCAP_LOOPBACK = 17;
    public static final byte  NETCAP_UNKNOWN  = 18;

    private static final String TRANSFORM_INTF_SEPERATOR     = "_";
    private static final String TRANSFORM_INTF_VAL_SEPERATOR = ",";
    
    
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
    
    private byte[]   argonIntfArray  = new byte[0];
    private byte[]   netcapIntfArray = new byte[0];
    private String[] deviceNameArray = new String[0];
    
    /* Always update the policy manager on the first pass */
    private boolean updatePolicyManager = true;
    
    private List<TransformInterface> transformInterfaceList = new LinkedList<TransformInterface>();

    private static IntfConverter INSTANCE = new IntfConverter();

    private Logger logger = Logger.getLogger( this.getClass());

    private IntfConverter()
    {
        this.outsideString = "";
        this.insideString  = "";
        this.dmzString     = "";
    }

    private IntfConverter( String outside, String inside, String dmz )
    {
        this.outsideString = outside;
        this.insideString  = inside;
        this.dmzString     = dmz;
    }

    /**
     * Lookup the netcap interface identifier for the inside interface
     */
    public static byte inside()
    {
        return NETCAP_INSIDE;
    }

    /**
     * Lookup the netcap interface identifier for the outside interface
     */
    public static byte outside()
    {
        return NETCAP_OUTSIDE;
    }

    /**
     * Lookup the netcap interface identifier for the dmz
     */
    public byte dmz()
    {
        return NETCAP_DMZ;
    }

    public byte[] argonIntfArray()
    {
        return this.argonIntfArray;
    }

    public byte[] netcapIntfArray()
    {
        return this.netcapIntfArray;
    }

    public String[] deviceNameArray()
    {
        return this.deviceNameArray;
    }

    List<TransformInterface> transformInterfaceList()
    {
        return this.transformInterfaceList;
    }

    public String argonIntfToString( byte argonIntf ) throws ArgonException
    {
        switch ( argonIntf ) {
        case OUTSIDE:
            return outsideString;

        case INSIDE:
            return insideString;
            
        case DMZ:
            return dmzString;
            
        default:
            for ( TransformInterface ti : this.transformInterfaceList ) {
                if ( ti.argonIntf() == argonIntf ) return ti.deviceName();
            }
            
            throw new ArgonException( "Unable to convert " + argonIntf + " to a string" );
        }
    }

    boolean clearUpdatePolicyManager()
    {
        boolean status = this.updatePolicyManager;
        this.updatePolicyManager = false;
        return status;
    }

    boolean registerIntf( byte argonIntf, String deviceName ) throws ArgonException
    {
        if (( 0 > argonIntf ) || ( argonIntf > ARGON_MAX ) || 
            ( argonIntf < DMZ )) {
            throw new ArgonException( "Unable to register argon interface: " + argonIntf );
        }
        
        /* Iterate all of the transform interfaces and check if this index is already taken */
        for ( Iterator<TransformInterface> iter = transformInterfaceList.iterator() ; iter.hasNext() ; ) {
            TransformInterface ti = iter.next();

            if ( argonIntf == ti.argonIntf()) {
                /* Interface is already registered, nothing to do */
                if ( ti.deviceName().equals( deviceName )) {
                    logger.info( "Interface '" + argonIntf + "' '" + deviceName + "' already registered" );
                    return false;
                }
                
                logger.info( "Replacing interface '" + ti.argonIntf() + "' '" + ti.deviceName() + "'" );
                iter.remove();
            }
        }
        
        logger.info( "Inserting transform interface '" + argonIntf + "' '" + deviceName + "'" );
        TransformInterface ti = new TransformInterface( argonIntf, deviceName );
        transformInterfaceList.add( ti );

        updateArrays();        
        return true;
    }

    boolean deregisterIntf( byte argonIntf ) throws ArgonException
    {
        if (( 0 > argonIntf ) || ( argonIntf > ARGON_MAX ) || 
            ( argonIntf < DMZ )) {
            throw new ArgonException( "Unable to register argon interface: " + argonIntf );
        }
        
        /* Iterate all of the transform interfaces and check if this index is already taken */
        for ( Iterator<TransformInterface> iter = transformInterfaceList.iterator() ; iter.hasNext() ; ) {
            TransformInterface ti = iter.next();
            
            if ( argonIntf == ti.argonIntf()) {
                logger.info( "Interface '" + argonIntf + "' '" + ti.deviceName() + "' deregistered" );
                iter.remove();
                
                updateArrays();
                return true;
            }
        }

        logger.debug( "Interface " + argonIntf + " is not registered" );
        
        return false;
    }

    private void updateArrays()
    {
        List<Byte> argonList = new LinkedList<Byte>();
        List<Byte> netcapList = new LinkedList<Byte>();
        List<String> deviceNameList = new LinkedList<String>();

        argonList.add( OUTSIDE );
        netcapList.add( NETCAP_OUTSIDE );
        deviceNameList.add( this.outsideString );

        argonList.add( INSIDE );
        netcapList.add( NETCAP_INSIDE );
        deviceNameList.add( this.insideString );

        if ( dmzString != null && this.dmzString.length() > 0 ) {
            argonList.add( DMZ );
            netcapList.add( NETCAP_DMZ );
            deviceNameList.add( this.dmzString );
        }
        
        /* No need to validate here, if it is on the interface list it is valid */
        for ( TransformInterface ti : transformInterfaceList ) {
            argonList.add( ti.argonIntf());
            netcapList.add( (byte)(ti.argonIntf() + 1 ));
            deviceNameList.add( ti.deviceName());
        }
        
        
        byte[] argonArray = new byte[argonList.size()];
        int c = 0;
        for ( Byte b : argonList ) argonArray[c++] = b;

        /* Check if the policy manager needs to be updated */
        if ( this.updatePolicyManager == false && !Arrays.equals( argonArray, this.argonIntfArray )) {
            this.updatePolicyManager = true;
        }
                 
        this.argonIntfArray  = argonArray;

        byte[] netcapArray = new byte[netcapList.size()];
        c = 0;
        for ( Byte b : netcapList ) netcapArray[c++] = b;

        this.netcapIntfArray = netcapArray;
        this.deviceNameArray = deviceNameList.toArray( new String[0] );

        logger.debug( "New netcapIntfArray: " + Arrays.toString( this.netcapIntfArray ));
        logger.debug( "New argonIntfArray: " + Arrays.toString( this.argonIntfArray ));
        logger.debug( "New deviceNameArray: " + Arrays.toString( this.deviceNameArray ));
    }
    
    /* Initialize the INSTANCE */
    synchronized void init( String outside, String inside, String dmz, String transformIntfs ) 
        throws ArgonException
    {
        
        inside = inside.trim();
        outside = outside.trim();
        if ( dmz == null ) dmz = "";
        
        IntfConverter newInstance = new IntfConverter( outside, inside, dmz );
        List<TransformInterface> transformInterfaceList = newInstance.transformInterfaceList;

        for ( String transformIntf : transformIntfs.split( TRANSFORM_INTF_SEPERATOR )) {
            /* Ignore the empty string which may be returned if there isn't anything there */
            if ( transformIntf.length() == 0 ) continue;
            String[] values = transformIntf.split( TRANSFORM_INTF_VAL_SEPERATOR );
            if ( values.length != 2 ) {
                logger.warn( "Invalid transform intf: '" + transformIntf + "'" );
                continue;
            }

            String deviceName = values[0];
            byte   argonIntf;
            try {
                argonIntf = Byte.parseByte( values[1] );
            } catch ( NumberFormatException e ) {
                logger.error( "Invalid argon interface index: '" + values[1] + "'" );
                continue;
            }

            if (( 0 > argonIntf ) || ( argonIntf > ARGON_MAX ) || 
                ( argonIntf < DMZ )) {
                logger.error(  "Invalid argon intf: " + argonIntf + " '" + deviceName + "', continuing" );
                continue;
            }
            
            transformInterfaceList.add( new TransformInterface( argonIntf, deviceName ));
        }

        /* Tell the new instance to generate new arrays */
        newInstance.updateArrays();
        
        INSTANCE = newInstance;               
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

    public synchronized static IntfConverter getInstance()
    {
        return INSTANCE;
    }
}
