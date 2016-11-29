/* $HeadURL$ */
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
    private static final Logger logger = Logger.getLogger( Netcap.class );

    public static final short IPPROTO_UDP  = 17;
    public static final short IPPROTO_TCP  = 6;

    private static final int JNETCAP_DEBUG = 1;
    private static final int NETCAP_DEBUG  = 2;

    private static final Netcap INSTANCE = new Netcap();

    private static final int longArrayLength = 1024*256;
    private final long[] longArray = new long[longArrayLength];
    
    /* Singleton */
    private Netcap()
    {
        /* Zero out array so valgrind won't complain */
        for ( int i = 0 ; i < longArrayLength ; i++ ) {
            longArray[i] = 0;
        }
    }

    /**
     * Initialzie the JNetcap and Netcap library. </p>
     *
     * @param netcapLevel Netcap debugging level.
     * @param jnetcapLevel JNetcap debugging level.
     */
    public static native int init( int netcapLevel, int jnetcapLevel );

    /**
     * Return the next available session ID
     */
    public static native long nextSessionId();

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
     * Set the hardlimit on the number of sessions to process at a time
     */
    public native void setSessionLimit( int limit );

    public static void setJnetcapDebugLevel( int level )
    {
        debugLevel( JNETCAP_DEBUG, level );
    }

    public static void setNetcapDebugLevel( int level )
    {
        debugLevel( NETCAP_DEBUG, level );
    }

    /**
     * Set both the jnetcap and Netcap debug level to the same level
     */
    public static void setDebugLevel( int level )
    {
        setJnetcapDebugLevel( level );
        setNetcapDebugLevel( level );
    }

    /**
     * Return a list of conntrack entries that represent the current
     * conntrack table
     */
    public synchronized List<Conntrack> getConntrackDump()
    {
        LinkedList<Conntrack> entries = new LinkedList<Conntrack>();
        int num = conntrackDump( longArray, longArrayLength );
        for ( int i = 0 ; i < num ; i++ ) {
            if ( longArray[i] == 0 ) {
                logger.warn("conntrackDump() returned a NULL value. " + num); 
            } else {
                entries.add( new Conntrack( new CPointer(longArray[i]) ) );
            }
        }
        return entries;
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
    public static native int registerUDPHook( NetcapCallback udpHook );
    
    /**
     * Setup a TCP hook 
     */
    public static native int registerTCPHook( NetcapCallback tcpHook );

    /**
     * Setup a Conntrack hook 
     */
    public static native int registerConntrackHook( NetcapCallback udpHook );
    
    /**
     * Clear out the UDP hook 
     */
    public static native int unregisterUDPHook();
    
    /**
     * Clear out the TCP hook 
     */
    public static native int unregisterTCPHook();

    /**
     * Clear out the Conntrack hook 
     */
    public static native int unregisterConntrackHook( );
    
    /**
     * Lookup MAC address for IP in ARP table
     */
    public static native String arpLookup( String ipAddress );

    /**
     * Destroy a conntrack entry
     */
    public static native int conntrackDestroy( int protocol, String cClientAddr, int cClientPort, String cServerAddr, int cServerPort );
    
    /**
     * Get a dump of the conntrack
     */
    public static native int conntrackDump( long[] arr, int arrLength );
    
    /**
     * Change the debugging level.
     * 
     * @param type The type debugging to change, this must be either JNETCAP_DEBUG or NETCAP_DEBUG
     * @param level Amount of debugging requested.  Higher is more debugging information.
     */
    private static native void debugLevel( int type, int level );

    static
    {
        System.loadLibrary( "uvmcore" );
    }

    public static Netcap getInstance()
    {
        return INSTANCE;
    }
}
