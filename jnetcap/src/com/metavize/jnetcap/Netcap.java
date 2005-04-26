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

package com.metavize.jnetcap;

import java.io.FileReader;
import java.io.BufferedReader;

import java.net.InetAddress;
import org.apache.log4j.Logger;

public final class Netcap {
    public static final short IPPROTO_UDP  = 17;
    public static final short IPPROTO_TCP  = 6;
    public static final short IPPROTO_ICMP = 1;

    private static final int JNETCAP_DEBUG = 1;
    private static final int NETCAP_DEBUG  = 2;

    /* The maximum number of netcap threads you can create */
    public static final int MAX_THREADS   = 20;

    /* The largest interface that netcap will return */
    public static final int MAX_INTERFACE = 32;

    /* The proc file containing the routing tables */
    private static final String ROUTE_PROC_FILE = "/proc/net/route";
    private static final String ROUTE_PREFIX    = "br0\t00000000\t";

    /* Maximum number of lines to read from the routing table */
    private static final int ROUTE_READ_LIM  = 50;

    protected static final Logger logger = Logger.getLogger( Netcap.class );

    /* Singleton, no point in instanciating this */
    private Netcap()
    {
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
     * @param enableShield Whether or not to enable to the shield.
     * @param netcapLevel Netcap debugging level.
     * @param jnetcapLevel JNetcap debugging level.
     */
    public static native int init( boolean enableShield, int netcapLevel, int jnetcapLevel );
    
    /** 
     * Initialize the JNetcap and Netcap library with the same debugging level for
     * JNetcap and Netcap.</p>
     * @param enableShield - Whether or not to enable the shield.
     * @param level - The debugging level for jnetcap and libnetcap.
     */
    public static int init( boolean enableShield, int level )
    {
        return init( enableShield, level, level );
    }

    /**
     * Retrieve the IP address of the box
     */
    public static InetAddress getHost()
    {
        return Inet4AddressConverter.toAddress( getHostLong());
    }

    /**
     * Retrieve the Netmask of the box 
     */
    public static InetAddress getNetmask()
    {
        return Inet4AddressConverter.toAddress( getNetmaskLong());
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
            String str;

            /* Limit the number of lines to read */
            for ( int c = 0 ; (( str = in.readLine()) != null ) && c < ROUTE_READ_LIM ; c++ ) {
                if ( str.startsWith( ROUTE_PREFIX )) {
                    str = str.substring( ROUTE_PREFIX.length(), 
                                         ROUTE_PREFIX.length() + Inet4AddressConverter.INADDRSZ * 2 );
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

    /* 
     * Block that traffic that would go to the box in the port range that came in on
     * interface intf
     */
    public static void blockIncomingTraffic( int protocol, int intf, Range ports )
    {
        blockIncomingTraffic( true, protocol, intf, ports.low(), ports.high());
    }
    
    public static void unblockIncomingTraffic( int protocol, int intf, Range ports )
    {
        blockIncomingTraffic( false, protocol, intf, ports.low(), ports.high());
    }

    private static native void blockIncomingTraffic( boolean isAdd, int protocol, int intf, 
                                                     int low, int high );

    /**
     * Limit traffic to the subnet of the bridge.</p>
     * @param inside - Name of the inside interface.
     * @param outside - Name of the inside interface.
     */
    public static native void limitSubnet( String inside, String outside );

    /* XXX the gates should probably be bytes, but stringToIntf returns an intf */
    // Inside of the wrapper risky to use.
    // public static native void stationGuard( int gate, String tcpPorts, String udpPorts );
    // public static native void relieveGuard( int gate );
    public static native void stationTcpGuard( int gate, String ports, String guests );
    public static native void stationUdpGuard( int gate, String ports, String guests );

    public static native void relieveTcpGuard( int gate, String ports, String guests );
    public static native void relieveUdpGuard( int gate, String ports, String guests );

    
    /**
     * Retrieve the IP address of the box (br0).
     */
    private static native long getHostLong();

    /**
     * Retrieve the Netmask of the box (br0).
     */
    private static native long getNetmaskLong();
    
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
     * Update an ICMP error packet to contain the data in a cached message.
     * @return - New length of the ICMP packet.
     */
    public static int updateIcmpPacket( byte[] data, int len, int icmpType, int icmpCode, 
                                        ICMPMailbox icmpMailbox )
    {
        return updateIcmpPacket( data, len, icmpType, icmpCode, icmpMailbox.pointer().value());
    }
    
    /**
     * Fix an ICMP packet
     * @param len - Length of the current data inside of the buffer
     * @param icmpType - Type of ICMP packet.
     * @param icmpCode - Code for the ICMP packet.
     * @param trafficPointer - Pointer to the traffic structure the packet will go out on
     */
    private static native int updateIcmpPacket( byte[] data, int len, int icmpType, int icmpCode, 
                                                long icmpMailboxPointer );
    
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
        System.loadLibrary( "alpine" );
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
    
    /* Specialty functions for NAT, DHCP and updating the address */
    public static native void updateAddress();
    
    public static native void enableLocalAntisubscribe();
    public static native void disableLocalAntisubscribe();
    
    public static native void enableDhcpForwarding();
    public static native void disableDhcpForwarding();
}
