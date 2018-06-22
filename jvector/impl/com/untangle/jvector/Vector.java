/**
 * $Id$
 */
package com.untangle.jvector;

import org.apache.log4j.Logger;

import java.util.ListIterator;
import java.util.LinkedList;

/**
 * Vector is a instance of a vector machine
 */
public class Vector
{
    /* These are the C return codes for the vectoring machine */
    public static final int ACTION_ERROR    = -1;
    public static final int ACTION_NOTHING  = 0;
    public static final int ACTION_DEQUEUE  = 1;
    public static final int ACTION_SHUTDOWN = 2;

    /* For setting the debug level, type should be one of the following constants */
    private static final int JVECTOR_DEBUG  = 1;
    private static final int MVUTIL_DEBUG   = 2;
    private static final int VECTOR_DEBUG   = 3;

    /* For telling the vectoring machine to shutdown */
    private static final int MSG_SHUTDOWN = 1;
    
    /* Poll flags */
    protected final static int MVPOLLIN  = 0x001;
    protected final static int MVPOLLOUT = 0x004;
    protected final static int MVPOLLERR = 0x008;
    protected final static int MVPOLLHUP = 0x010;

    protected static final Logger logger = Logger.getLogger( Vector.class );

    private long vec_ptr = 0;
    private long list_ptr;
    
    /**
     * vector_create
     * @param listptr
     * @return
     */
    private static native long  vector_create ( long listptr );

    /**
     * vector_raze
     * @param vecptr
     * @return
     */
    private static native int   vector_raze ( long vecptr );

    /**
     * vector_print
     * @param vecptr
     */
    private static native void  vector_print ( long vecptr );

    /**
     * vector_compress
     * @param vecptr
     * @param sink
     * @param source
     */
    private static native void  vector_compress ( long vecptr, long sink, long source );

    /**
     * vector_length
     * @param vecptr
     * @return
     */
    private static native int   vector_length ( long vecptr );

    /**
     * vector_send_msg
     * @param vecptr
     * @param msg
     * @param arg
     * @return
     */
    private static native int   vector_send_msg ( long vecptr, int msg, long arg );

    /**
     * vector_set_timeout
     * @param vecptr
     * @param timeout_sec
     */
    private static native void  vector_set_timeout ( long vecptr, int timeout_sec );

    /**
     * vector
     * @param vecptr
     * @return
     */
    private static native int   vector ( long vecptr );

    /**
     * list_create
     * @param flags
     * @return
     */
    private native long  list_create (int flags);

    /**
     * list_add_tail
     * @param listptr
     * @param val
     * @return
     */
    private native long  list_add_tail (long listptr, long val);

    /**
     * list_raze
     * @param listptr
     * @return
     */
    private native int   list_raze (long listptr);

    /**
     * cLoad
     * @return
     */
    private static native int cLoad();

    /**
     * debugLevel
     * @param type
     * @param level
     */
    private static native void debugLevel( int type, int level );
    
    /**
     * jvectorDebugLevel
     * @param level
     */
    public static void jvectorDebugLevel( int level ) { debugLevel( JVECTOR_DEBUG, level ); }

    /**
     * mvutilDebugLevel
     * @param level
     */
    public static void mvutilDebugLevel( int level ) { debugLevel( MVUTIL_DEBUG, level ); }

    /**
     * vectorDebugLevel
     * @param level
     */
    public static void vectorDebugLevel( int level ) { debugLevel( VECTOR_DEBUG, level ); }

    /**
     * Vector - create a vector machine
     * @param list - a list of the relays
     */
    public Vector (LinkedList<Relay> list)
    {
        list_ptr = list_create(0);
        
        for ( ListIterator<Relay>iter = list.listIterator() ; iter.hasNext() ;) {
            Relay relay = iter.next();
            if ( list_add_tail(list_ptr,relay.get_relay()) == 0 ) {
                logError( "list_add_tail: failed" );
                throw new IllegalStateException( "Failed to add to tail of the relay list" );
            }
        }

        vec_ptr = vector_create(list_ptr);
    }

    /**
     * vector - initiate the vectoring of data
     * @return 0 if success
     */
    public int vector()
    {
        return vector(vec_ptr);
    }

    /**
     * print the vector description to stdout
     * used for debugging
     */
    public synchronized void print()
    {
        vector_print(vec_ptr);
    }

    /**
     * compress reorganizes the sinks and sources to remove this relay
     * The source corresponding to the specified sink and the sink
     * corresponding to the specfied source will be connected in a new relay,
     * effectively removing a link from the chain.
     * @param sink
     * @param source
     */
    public synchronized void compress( Sink sink, Source source )
    {
        vector_compress( vec_ptr, sink.snk_ptr(), source.src_ptr() );
    }

    /**
     * length - gets the length of the vector (number of relays)
     * @return vector_length
     */
    public synchronized int length()
    {
        return vector_length( vec_ptr );
    }
    
    /**
     * Set the vector timeout in msec
     * @param msec
     */
    public synchronized void timeout(int msec)
    {
        vector_set_timeout(vec_ptr, msec);
    }

    /**
     * raze all vector resources
     */
    public synchronized void raze()
    {
        if ( vec_ptr != 0 ) vector_raze( vec_ptr );
        vec_ptr = 0;

        /* Raze the associated list */
        if ( list_ptr != 0 ) list_raze( list_ptr );
        list_ptr = 0;
    }

    /**
     * isRazed 
     * @return true if razed, false otherwise
     */
    public synchronized boolean isRazed()
    {
        if ( vec_ptr == 0L )
            return true;
        else
            return false;
    }

    /**
     * shutdown jvector
     */
    public synchronized void shutdown()
    {
        if ( vec_ptr == 0 ) return;
        vector_send_msg( vec_ptr, MSG_SHUTDOWN, 0L );
    }

    /**
     * This doesn't do anything, but it will automatically call the static method 
     * once the first time Vector is initialized
     */
    public static void load() 
    {
    }

    /**
     * Debugging and logging, setup this way so we could add
     * functionality to register a logger, rather than forcing the
     * user to support log4j.
     * @param o
     */
    static void logDebug( Object o )
    {
        logger.debug( o );
    }

    /**
     * logInfo
     * @param o
     */
    static void logInfo( Object o )
    {
        logger.info( o );
    }

    /**
     * logWarn
     * @param o
     */
    static void logWarn( Object o )
    {
        logger.warn( o );
    }

    /**
     * logError
     * @param o
     */
    static void logError( Object o )
    {
        logger.error( o );
    }

    /**
     * logFatal
     * @param o
     */
    static void logFatal( Object o )
    {
        logger.fatal( o );
    }

    /**
     * isDebugEnabled
     * @return
     */
    static boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    /**
     * isInfoEnabled
     * @return
     */
    static boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }

    static
    {
        logInfo( "Loading Vector" );
        System.loadLibrary("uvmcore");
        cLoad();
    }
}
