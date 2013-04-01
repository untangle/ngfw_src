/* $HeadURL$ */
package com.untangle.uvm.argon;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jvector.Vector;
import com.untangle.uvm.ArgonException;
import com.untangle.uvm.util.JsonClient;

public class Argon
{
    /* Number of times to try and shutdown all vectoring machines cleanly before giving up */
    static final int SHUTDOWN_ATTEMPTS = 5;

    /* Amount of time between subsequent calls to shutdown all of the vectoring machines */
    static final int SHUTDOWN_PAUSE    = 2000;

    public static final int SCHED_NORMAL = 0;
    public static final int SCHED_SOFTREAL = 4;

    /* Singleton */
    private static final Argon INSTANCE = new Argon();

    int netcapDebugLevel    = 1;
    int jnetcapDebugLevel   = 1;
    int vectorDebugLevel    = 0;
    int jvectorDebugLevel   = 0;
    int mvutilDebugLevel    = 0;

    int sessionThreadLimit  = 10000;
    int newSessionSchedPolicy  = SCHED_NORMAL;
    int sessionSchedPolicy  = SCHED_NORMAL;

    /* Number of threads to donate to netcap */
    int numThreads        = 15;

    /* Debugging */
    private final Logger logger = Logger.getLogger( this.getClass());

    /* Singleton */
    private Argon() { }

    public void run()
    {
        /* Parse all of the properties */
        parseProperties();

        try {
            init();
        } catch ( ArgonException e ) {
            logger.fatal( "Error initializing argon", e );
            throw new IllegalStateException( "Unable to initialize argon", e );
        }

        registerHooks();
    }

    /**
     * Parse the user supplied properties
     */
    private void parseProperties()
    {
        String temp;

        if (( temp = System.getProperty( "argon.numthreads" )) != null ) {
            int count;
            count = Integer.parseInt( temp );
            if ( count < 0 ) {
                logger.error( "argon.numthreads must be > 0." + count + " continuing" );
            } else {
                numThreads = count;
            }
        }

        if (( temp = System.getProperty( "argon.debug.netcap" )) != null ) {
            netcapDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "argon.debug.jnetcap" )) != null ) {
            jnetcapDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "argon.debug.vector" )) != null ) {
            vectorDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "argon.debug.jvector" )) != null ) {
            jvectorDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "argon.debug.mvutil" )) != null ) {
            mvutilDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "argon.sessionlimit" )) != null ) {
            sessionThreadLimit  = Integer.parseInt( temp );
        }

        // Policy used for session threads (and new session threads if not specified below)
        if (( temp = System.getProperty( "argon.sessionSchedPolicy" )) != null ) {
            sessionSchedPolicy  = Integer.parseInt( temp );
            newSessionSchedPolicy  = sessionSchedPolicy;
        }

        // Policy used for newSession (Netcap Server) threads
        if (( temp = System.getProperty( "argon.newSessionSchedPolicy" )) != null ) {
            newSessionSchedPolicy  = Integer.parseInt( temp );
        }
    }

    /**
     * Register the TCP and UDP hooks
     */
    private void registerHooks()
    {
        Netcap.registerUDPHook( ArgonUDPHook.getInstance());

        Netcap.registerTCPHook( ArgonTCPHook.getInstance());
    }

    /**
     * Initialize Netcap and any other supporting libraries.
     */
    private void init() throws ArgonException
    {
        if ( Netcap.init( netcapDebugLevel, jnetcapDebugLevel ) < 0 ) {
            throw new ArgonException( "Unable to initialize netcap" );
        }

        /* Start the scheduler */
        Netcap.startScheduler();

        Vector.mvutilDebugLevel( mvutilDebugLevel );
        Vector.vectorDebugLevel( vectorDebugLevel );
        Vector.jvectorDebugLevel( jvectorDebugLevel );

        Netcap.getInstance().setNewSessionSchedPolicy( this.newSessionSchedPolicy );
        Netcap.getInstance().setSessionSchedPolicy( this.sessionSchedPolicy );

        /* Donate a few threads */
        Netcap.donateThreads( numThreads );
        Netcap.getInstance().setSessionLimit( this.sessionThreadLimit );
    }

    public void destroy()
    {
        logger.debug( "Shutting down" );

        /* Remove both of the hooks to guarantee that no new sessions are created */
        Netcap.unregisterTCPHook();
        Netcap.unregisterUDPHook();

        ArgonSessionTable activeSessions = ArgonSessionTable.getInstance();

        /* Close all of the vectoring machines */
        for ( int c = 0; c <  SHUTDOWN_ATTEMPTS ; c++ ) {
            if ( logger.isInfoEnabled()) {
                logger.info( "" + activeSessions.count() + " active sessions remaining" );
            }

            if ( !activeSessions.shutdownActive()) break;

            /* Sleep a little while vectrons shutdown. */
            try {
                Thread.sleep( SHUTDOWN_PAUSE );
            } catch ( InterruptedException e ) {
                logger.error( e.getMessage());
            }
        }

        Netcap.cleanup();
    }

    public static Argon getInstance()
    {
        return INSTANCE;
    }

}
