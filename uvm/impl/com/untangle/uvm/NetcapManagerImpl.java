/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jvector.Vector;
import com.untangle.uvm.NetcapManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * Manager for netcap
 */
public class NetcapManagerImpl implements NetcapManager
{
    /* Number of times to try and shutdown all vectoring machines cleanly before giving up */
    static final int SHUTDOWN_ATTEMPTS = 5;

    /* Amount of time between subsequent calls to shutdown all of the vectoring machines */
    static final int SHUTDOWN_PAUSE    = 2000;

    /* Singleton */
    private static NetcapManagerImpl INSTANCE = null;

    int netcapDebugLevel    = 1;
    int jnetcapDebugLevel   = 1;
    int vectorDebugLevel    = 0;
    int jvectorDebugLevel   = 0;
    int mvutilDebugLevel    = 0;

    int sessionThreadLimit  = 0;

    /* Number of threads to donate to netcap */
    int numThreads        = 15;

    /* Debugging */
    private final Logger logger = Logger.getLogger( this.getClass());

    /**
     * Singleton
     */
    private NetcapManagerImpl()
    {
        /* Parse all of the properties */
        parseProperties();

        try {
            init();
        } catch ( Exception e ) {
            logger.fatal( "Error initializing netcap", e );
            throw new IllegalStateException( "Unable to initialize netcap", e );
        }
    }

    /**
     * run (start netcap)
     */
    public void run()
    {
        registerHooks();
    }

    /**
     * Parse the user supplied properties
     */
    private void parseProperties()
    {
        String temp;

        if (( temp = System.getProperty( "netcap.numthreads" )) != null ) {
            int count;
            count = Integer.parseInt( temp );
            if ( count < 0 ) {
                logger.error( "netcap.numthreads must be > 0." + count + " continuing" );
            } else {
                numThreads = count;
            }
        }

        if (( temp = System.getProperty( "netcap.debug.netcap" )) != null ) {
            netcapDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "netcap.debug.jnetcap" )) != null ) {
            jnetcapDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "netcap.debug.vector" )) != null ) {
            vectorDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "netcap.debug.jvector" )) != null ) {
            jvectorDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "netcap.debug.mvutil" )) != null ) {
            mvutilDebugLevel = Integer.parseInt( temp );
        }

        if (( temp = System.getProperty( "netcap.sessionlimit" )) != null ) {
            sessionThreadLimit  = Integer.parseInt( temp );
        }
    }

    /**
     * Register the TCP and UDP hooks
     */
    private void registerHooks()
    {
        Netcap.registerUDPHook( NetcapUDPHook.getInstance());

        Netcap.registerTCPHook( NetcapTCPHook.getInstance());

        Netcap.registerConntrackHook( NetcapConntrackHook.getInstance());
    }

    /**
     * Initialize Netcap and any other supporting libraries.
     * @throws Exception
     */
    private void init() throws Exception
    {
        if ( Netcap.init( netcapDebugLevel, jnetcapDebugLevel ) < 0 ) {
            throw new Exception( "Unable to initialize netcap" );
        }

        Vector.mvutilDebugLevel( mvutilDebugLevel );
        Vector.vectorDebugLevel( vectorDebugLevel );
        Vector.jvectorDebugLevel( jvectorDebugLevel );

        if ( this.sessionThreadLimit > 0 )
            Netcap.getInstance().setSessionLimit( this.sessionThreadLimit );

        /* Donate a few threads */
        Netcap.donateThreads( numThreads );
    }

    /**
     * destroy - shutdown netcap
     */
    public void destroy()
    {
        logger.debug( "Shutting down" );

        /* Remove both of the hooks to guarantee that no new sessions are created */
        Netcap.unregisterTCPHook();
        Netcap.unregisterUDPHook();
        Netcap.unregisterConntrackHook();

        SessionTableImpl sessionTable = SessionTableImpl.getInstance();

        /* Close all of the vectoring machines */
        for ( int c = 0; c <  SHUTDOWN_ATTEMPTS ; c++ ) {
            if ( logger.isInfoEnabled()) {
                logger.info( "" + sessionTable.count() + " active sessions remaining" );
            }

            if ( !sessionTable.shutdownActive()) break;

            /* Sleep a little while vectrons shutdown. */
            try {
                Thread.sleep( SHUTDOWN_PAUSE );
            } catch ( InterruptedException e ) {
                logger.error( e.getMessage());
            }
        }

        Netcap.cleanup();
    }

    /**
     * getInstance - get the singleton
     * @return NetcapManagerImpl
     */
    public static NetcapManagerImpl getInstance()
    {
        if ( INSTANCE == null )
            INSTANCE = new NetcapManagerImpl();

        return INSTANCE;
    }

    /**
     * arpLookup - call arp lookup in netcap
     * @param ipAddress
     * @return macAddress
     */
    public String arpLookup( String ipAddress )
    {
        return Netcap.arpLookup( ipAddress );
    }

    /**
     * Get the number of sessions from the SessionTable
     * @return int
     */
    public int getSessionCount()
    {
        return SessionTableImpl.getInstance().count();
    }

    /**
     * Get the number of sessions from the SessionTable with the specified protocol
     * @param protocol
     * @return int
     */
    public int getSessionCount(short protocol)
    {
        return SessionTableImpl.getInstance().count(protocol);
    }
    
    /**
     * Shutdown all of the sessions that match <code>matcher</code>
     * @param matcher
     */
    public void shutdownMatches( SessionMatcher matcher )
    {
        SessionTableImpl.getInstance().shutdownMatches( matcher );
    }

    /**
     * Shutdown all of the sessions that have been touch by the PipelineConnector that match <code>matcher</code>
     * @param matcher
     * @param connector
     */
    public void shutdownMatches( SessionMatcher matcher, PipelineConnector connector )
    {
        SessionTableImpl.getInstance().shutdownMatches( matcher, connector );
    }

    /**
     * See if a addr:port binding is already in use by an existing session
     * @param addr
     * @param port
     * @return bool
     */
    public boolean isTcpPortUsed( InetAddress addr, int port )
    {
        return SessionTableImpl.getInstance().isTcpPortUsed( addr, port );
    }

    /**
     * setNetcapDebugLevel - sets the netcap debug level
     * @param level
     */
    public void setNetcapDebugLevel( int level )
    {
        Netcap.setNetcapDebugLevel( level );
    }

    /**
     * setJNetcapDebugLevel - set the jnetcap debug level
     * @param level
     */
    public void setJNetcapDebugLevel( int level )
    {
        Netcap.setJnetcapDebugLevel( level );
    }
    
}
