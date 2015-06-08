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
    protected static final Logger logger = Logger.getLogger( Netcap.class );

    public static final short IPPROTO_UDP  = 17;
    public static final short IPPROTO_TCP  = 6;

    private static final int JNETCAP_DEBUG = 1;
    private static final int NETCAP_DEBUG  = 2;

    private static final Netcap INSTANCE = new Netcap();
    
    /* Singleton */
    private Netcap() {}

    /**
     * A common place for processing netcap errors
     * @param msg - The message associated with the error.
     */
    static void error( String msg )
    {
        throw new IllegalStateException( "Netcap.error: " + msg );
    }

    static void error()
    {
        error( "" );
    }

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

    public static void jnetcapDebugLevel( int level )
    {
        debugLevel( JNETCAP_DEBUG, level );
    }

    public static void netcapDebugLevel( int level )
    {
        debugLevel( NETCAP_DEBUG, level );
    }

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
}
