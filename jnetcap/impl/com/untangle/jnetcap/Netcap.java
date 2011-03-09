/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.jnetcap;

import java.io.FileReader;
import java.io.BufferedReader;

import java.util.regex.Pattern;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import java.net.InetAddress;
import org.apache.log4j.Logger;

public final class Netcap
{
    public static final short IPPROTO_UDP  = 17;
    public static final short IPPROTO_TCP  = 6;
    public static final short IPPROTO_ICMP = 1;

    private static final int JNETCAP_DEBUG = 1;
    private static final int NETCAP_DEBUG  = 2;

    /* The maximum number of netcap threads you can create at a time */
    public static final int MAX_THREADS    = 50;

    /* The largest interface that netcap will return */
    public static final int MAX_INTERFACE = 256;

    /* The proc file containing the routing tables */
    private static final String ROUTE_PROC_FILE = "/proc/net/route";

    private static final String GATEWAY_PATTERN = "^[^\t]*\t00000000\t([^\t]*)\t.*";
    private static final String GATEWAY_REPLACE = "$1";

    /* Maximum number of lines to read from the routing table */
    private static final int ROUTE_READ_LIM  = 50;

    private static final Netcap INSTANCE = new Netcap();
    
    private static final List<InterfaceData> EMPTY_INTERFACE_DATA_LIST = Collections.emptyList();

    protected static final Logger logger = Logger.getLogger( Netcap.class );

    /* Singleton */
    private Netcap()
    {
    }

    /*  Get the Redirect port range */
    public PortRange tcpRedirectPortRange() throws JNetcapException {
        PortRange portRange = null;

        try {
            int[] ports = cTcpRedirectPorts();
            portRange = new PortRange( ports[0], ports[1] );
        } catch ( Exception e ) {
            throw new JNetcapException( e );
        }
        
        return portRange;
    }
    
    /**
     * A common place for processing netcap errors
     * @param msg - The message associated with the error.
     */
    static void error( String msg ) {
        throw new IllegalStateException( "Netcap.error: " + msg );
    }

    static void error() {
        error( "" );
    }

    /**
     * Verify that a protocol is a valid value.</p>
     * @param protocol - An integer containing the protocol to verify.
     * @return <code>protocol</code> if the number is value.
     */
    public static int verifyProtocol( int protocol ) 
    {
        switch ( protocol ) {
        case IPPROTO_TCP: 
        case IPPROTO_UDP: 
        case IPPROTO_ICMP: break;
            
        default: error( "Invalid protocol: " + protocol );
        }

        return protocol;
    }

    /**
     * Determine if an address is a broadcast 
     * @param host - The address to check.
     * @return <code>true</code> if the
     */
    public static boolean isBroadcast( InetAddress address )
    {
        return isBroadcast( Inet4AddressConverter.toLong( address ));
    }

    /**
     * Determine if an address is a multicast
     * @param host - The address to check.
     * @return <code>true</code> if the
     */
    public static boolean isMulticast( InetAddress address )
    {
        return isMulticast( Inet4AddressConverter.toLong( address ));
    }

    public static boolean isMulticastOrBroadcast( InetAddress address )
    {
        long temp = Inet4AddressConverter.toLong( address );
        return isMulticast( temp ) || isBroadcast( temp );
    }
    
    private static native boolean isBroadcast( long address );
    public static  native boolean isBridgeAlive();

    private static native boolean isMulticast( long address );

    /**
     * Initialzie the JNetcap and Netcap library. </p>
     *
     * @param netcapLevel Netcap debugging level.
     * @param jnetcapLevel JNetcap debugging level.
     */
    public static native int init( int netcapLevel, int jnetcapLevel );
    
    /** 
     * Initialize the JNetcap and Netcap library with the same debugging level for
     * JNetcap and Netcap.</p>
     * @param level - The debugging level for jnetcap and libnetcap.
     */
    public static int init( int level )
    {
        return init( level, level );
    }
    
    /**
     * Retrieve the gateway of the box.
     */
    public static InetAddress getGateway()
    {
        BufferedReader in = null;
        InetAddress  addr = null;

        /* Open up the default route file */
        try { 
            in = new BufferedReader(new FileReader( ROUTE_PROC_FILE ));
            Pattern pattern = Pattern.compile( GATEWAY_PATTERN );

            String str;

            /* Limit the number of lines to read */
            for ( int c = 0 ; (( str = in.readLine()) != null ) && c < ROUTE_READ_LIM ; c++ ) {
                if ( str.matches( GATEWAY_PATTERN )) {
                    str = pattern.matcher( str ).replaceAll( GATEWAY_REPLACE );
                    addr = Inet4AddressConverter.getByHexAddress( str, true );
                    break;
                }
            }
        } catch ( Exception ex ) {
            System.out.println( "Error reading file: " + ex );
            logger.error( "Error reading file: ", ex );
        }

        try {
            if ( in != null ) 
                in.close();
        } catch ( Exception ex ) {
            System.out.println( "Unable to close file: " + ex );
            logger.error( "Unable to close file", ex );
        }

        return addr;
    }

    /**
     * Retrieve the address of an interface
     */
    public List<InterfaceData> getInterfaceData( String interfaceString ) throws JNetcapException
    {
        try {
            /* XXXX 3 is the magic number, this magic number and this comment makes no sense */
            long input[] = new long[MAX_INTERFACE*3];
            int numIntf = getInterfaceDataArray( interfaceString, input );
            if ( numIntf <= 0 ) return EMPTY_INTERFACE_DATA_LIST;
            List<InterfaceData> dataList = new LinkedList<InterfaceData>();

            for ( int c = 0 ; c < numIntf ; c++ ) {
                dataList.add( new InterfaceData( input[(3 * c) + 0], 
                                                 input[(3 * c) + 1], 
                                                 input[(3 * c) + 2] ));
            }
            return dataList;
        } catch ( Exception e ) {
            throw new JNetcapException( "Error retrieving interface address", e );
        }
    }

    /**
     * Configure the netcap interface array
     */
    public void configureInterfaceArray( int intfIndexArray[], String interfaceArray[] )
        throws JNetcapException
    {
        try {
            cConfigureInterfaceArray( intfIndexArray, interfaceArray );
        } catch ( Exception e ) {
            throw new JNetcapException( "Error configuring interface array", e );
        }

    }

    /**
     * Set the hardlimit on the number of sessions to process at a time
     */
    public native void setSessionLimit( int limit );

    /**
     * Set the scheduling policy to use for Netcap Server threads
     */
    public native void setNewSessionSchedPolicy( int policy );

    /**
     * Set the scheduling policy to use for Session threads
     */
    public native void setSessionSchedPolicy( int policy );

    /**
     * Retrieve the IP address of an interface
     */
    private native int getInterfaceDataArray( String interfaceString, long input[] );
    
    public static void jnetcapDebugLevel( int level ) { debugLevel( JNETCAP_DEBUG, level ); }
    public static void netcapDebugLevel( int level ) { debugLevel( NETCAP_DEBUG, level ); }

    /**
     * Set both the jnetcap and Netcap debug level to the same level
     */
    public static void debugLevel( int level )
    {
        jnetcapDebugLevel( level );
        netcapDebugLevel( level );
    }

    /**
     * Cleanup the netcap library
     */
    public static native void cleanup();

    /**
     * Donate some threads to the netcap server.</p>
     * @param numThreads - The number of threads to donate.
     */
    public static native int donateThreads( int numThreads );
    
    /**
     * Start the netcap scheduler.
     */
    public static native int startScheduler();

    /**
     * Setup a UDP hook 
     */
    public static native int registerUDPHook( NetcapHook udpHook );
    
    /**
     * Setup a TCP hook 
     */
    public static native int registerTCPHook( NetcapHook tcpHook );

    /**
     * Clear out the UDP hook 
     */
    public static native int unregisterUDPHook();
    
    /**
     * Clear out the TCP hook 
     */
    public static native int unregisterTCPHook();
    
    /**
     * Convert a string interface to unique identifer that netcap uses to represent interfaces.</p>
     * @param intf - String containing the interface to convert. (eg. eth0).
     * @return A unique identifier between 1 and MAX_INTERFACES(Inclusive).
     */
    public static native byte convertStringToIntf( String intf );

    /**
     * Convert a netcap representation of an interface to a string.
     * @param intf - Numeric representation of the interface to convert.
     */
    public static native String convertIntfToString( int intf );

    /**
     * Change the debugging level. <p/>
     * 
     * @param type The type debugging to change, this must be either JNETCAP_DEBUG or NETCAP_DEBUG
     * @param level Amount of debugging requested.  Higher is more debugging information.
     */
    private static native void debugLevel( int type, int level );

    /**
     * An empty function that when executed will automatically call the static initializer 
     */
    public static void load()
    {
    }

    static
    {
        System.loadLibrary( "uvmcore" );
    }

    /* Debugging and logging */
    static void logDebug( Object o )
    {
        logger.debug( o );
    }

    static void logInfo( Object o )
    {
        logger.info( o );
    }

    static void logWarn( Object o )
    {
        logger.warn( o );
    }

    static void logError( Object o )
    {
        logger.error( o );
    }

    static void logFatal( Object o )
    {
        logger.fatal( o );
    }

    public static Netcap getInstance()
    {
        return INSTANCE;
    }
    
    /**
     * Specialty functions for NAT and DHCP events to update the address.
     */
    public static native void refreshNetworkConfig();

    /**
     * Function to retrieve the TCP redirect ports
     */
    private native int[] cTcpRedirectPorts();
    
    /**
     * Function to configure the netcap interface array 
     */
    private native void cConfigureInterfaceArray( int intfIndexArray[], String interfaceArray[] );
}
