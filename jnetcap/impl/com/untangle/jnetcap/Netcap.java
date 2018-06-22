/**
 * $Id: Netcap.java,v 1.00 2018/06/21 17:00:24 dmorris Exp $
 */
package com.untangle.jnetcap;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * Netcap ss the main JNetcap interface
 */
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
    
    /**
     * Netcap is a Singleton
     * so this is private
     */
    private Netcap()
    {
        /* Zero out array so valgrind won't complain */
        for ( int i = 0 ; i < longArrayLength ; i++ ) {
            longArray[i] = 0;
        }
    }

    /**
     * init
     * Initialzie the JNetcap and Netcap library.
     * @param netcapLevel Netcap debugging level.
     * @param jnetcapLevel JNetcap debugging level.
     * @return 0 if success
     */
    public static native int init( int netcapLevel, int jnetcapLevel );

    /**
     * nextSessionId
     * Return the next available session ID
     * @return - the next session ID
     */
    public static native long nextSessionId();

    /**
     * init
     * Initialize the JNetcap and Netcap library with the same
     * debugging level for JNetcap and Netcap.
     * @param level - The debugging level for jnetcap and libnetcap.
     * @return 0 if success
     */
    public static int init(int level)
    {
        return init( level, level );
    }
    
    /**
     * Set the hardlimit on the number of sessions to process at a time
     * @param limit
     */
    public native void setSessionLimit(int limit);

    /**
     * setJnetcapDebugLevel - sets the jnetcap debug level
     * @param level
     */
    public static void setJnetcapDebugLevel(int level)
    {
        debugLevel( JNETCAP_DEBUG, level );
    }

    /**
     * setNetcapDebugLevel - sets the netcap debug level
     * @param level
     */
    public static void setNetcapDebugLevel(int level)
    {
        debugLevel( NETCAP_DEBUG, level );
    }

    /**
     * Set both the jnetcap and Netcap debug level to the same level
     * @param level
     */
    public static void setDebugLevel(int level)
    {
        setJnetcapDebugLevel( level );
        setNetcapDebugLevel( level );
    }

    /**
     * Return a list of conntrack entries that represent the current
     * conntrack table
     * @return the list of conntrack entries
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
     * donateThreads
     * Donate some threads to the netcap server.</p>
     * @param numThreads - The number of threads to donate.
     * @return
     */
    public static native int donateThreads( int numThreads );
    
    /**
     * startScheduler
     * Start the netcap scheduler.
     * @return
     */
    public static native int startScheduler();

    /**
     * registerUDPHook
     * Setup a UDP hook
     * @param udpHook
     * @return
     */
    public static native int registerUDPHook( NetcapCallback udpHook );
    
    /**
     * registerTCPHook
     * Setup a TCP hook
     * @param tcpHook
     * @return
     */
    public static native int registerTCPHook( NetcapCallback tcpHook );

    /**
     * registerConntrackHook
     * Setup a Conntrack hook
     * @param udpHook
     * @return
     */
    public static native int registerConntrackHook( NetcapCallback udpHook );
    
    /**
     * unregisterUDPHook
     * Clear out the UDP hook
     * @return
     */
    public static native int unregisterUDPHook();
    
    /**
     * unregisterTCPHook
     * Clear out the TCP hook
     * @return
     */
    public static native int unregisterTCPHook();

    /**
     * unregisterConntrackHook
     * Clear out the Conntrack hook
     * @return
     */
    public static native int unregisterConntrackHook( );
    
    /**
     * arpLookup
     * Lookup MAC address for IP in ARP table
     * @param ipAddress
     * @return
     */
    public static native String arpLookup( String ipAddress );

    /**
     * conntrackDestroy
     * Destroy a conntrack entry
     * @param protocol
     * @param cClientAddr
     * @param cClientPort
     * @param cServerAddr
     * @param cServerPort
     * @return
     */
    public static native int conntrackDestroy( int protocol, String cClientAddr, int cClientPort, String cServerAddr, int cServerPort );
    
    /**
     * conntrackDump
     * Get a dump of the conntrack
     * @param arr
     * @param arrLength
     * @return
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

    /**
     * getInstance - gets the main singleton Netcap instance
     * @return Netcap
     */
    public static Netcap getInstance()
    {
        return INSTANCE;
    }
}
