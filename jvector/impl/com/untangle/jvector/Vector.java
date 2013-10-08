/**
 * $Id: Vector.java 35567 2013-08-08 07:47:12Z dmorris $
 */
package com.untangle.jvector;

import org.apache.log4j.Logger;

import java.util.ListIterator;
import java.util.LinkedList;

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

    /* Most of these should/could be static */
    private native long  vector_create (long listptr);
    private native int  vector_raze (long vecptr);
    private native int  vector_send_msg (long vecptr, int msg, long arg);
    private native void vector_set_timeout (long vecptr, int timeout_sec);
    private native int  vector (long vecptr);

    private native long  list_create (int flags);
    private native long  list_add_tail (long listptr, long val);
    private native int   list_raze (long listptr);

    private static native int cLoad();

    private static native void debugLevel( int type, int level );
    
    public static void jvectorDebugLevel( int level ) { debugLevel( JVECTOR_DEBUG, level ); }
    public static void mvutilDebugLevel( int level ) { debugLevel( MVUTIL_DEBUG, level ); }
    public static void vectorDebugLevel( int level ) { debugLevel( VECTOR_DEBUG, level ); }

    private long vec_ptr = 0;
    private long list_ptr;

    /* This list is always razed */

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

    public int vector()
    {
        return vector(vec_ptr);
    }

    /**
     * Set the vector timeout in msec
     */
    public void timeout(int msec)
    {
        vector_set_timeout(vec_ptr, msec);
    }

    public void raze()
    {
        if ( vec_ptr != 0 ) vector_raze( vec_ptr );
        vec_ptr = 0;

        /* Raze the associated list */
        if ( list_ptr != 0 ) list_raze( list_ptr );
        list_ptr = 0;
    }

    public void shutdown()
    {
        if ( vec_ptr == 0 ) return;
        vector_send_msg( vec_ptr, MSG_SHUTDOWN, 0L );
    }

    static
    {
        logInfo( "Loading Vector" );
        System.loadLibrary("uvmcore");
        cLoad();
    }

    /**
     * This doesn't do anything, but it will automatically call the static method 
     * once the first time Vector is initialized */
    public static void load() 
    {
    }

    /* Debugging and logging, setup this way so we could add functionality to register
     * a logger, rather than forcing the user to support log4j. */
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

    static boolean isDebugEnabled()
    {
        return logger.isDebugEnabled();
    }

    static boolean isInfoEnabled()
    {
        return logger.isInfoEnabled();
    }
}
