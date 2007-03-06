/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.argon;

import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.Netcap;
import com.untangle.jnetcap.Shield;
import com.untangle.jvector.Vector;

import com.untangle.mvvm.ArgonException;
import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.localapi.LocalShieldManager;
import com.untangle.mvvm.localapi.LocalIntfManager;
import com.untangle.mvvm.engine.PolicyManagerPriv;
import com.untangle.mvvm.shield.ShieldMonitor;

import com.untangle.mvvm.networking.NetworkManagerImpl;
import com.untangle.mvvm.networking.NetworkException;

public class Argon
{
    /* Number of times to try and shutdown all vectoring machines cleanly before giving up */
    static final int SHUTDOWN_ATTEMPTS = 5;

    /* Amount of time between subsequent calls to shutdown all of the vectoring machines */
    static final int SHUTDOWN_PAUSE    = 2000;

    /* Maximum number of threads allowed at any time */
    private static int MAX_THREADS = 10000;

    public static final int SCHED_NORMAL = 0;
    public static final int SCHED_SOFTREAL = 4;

    /* The networking manager impl is passed in at init time */
    private NetworkManagerImpl networkManager = null;

    /* Singleton */
    private static final Argon INSTANCE = new Argon();

    private int sleepingThreads;
    private int totalThreads;
    private int activeThreads;

    int netcapDebugLevel    = 1;
    int jnetcapDebugLevel   = 1;
    int vectorDebugLevel    = 0;
    int jvectorDebugLevel   = 0;
    int mvutilDebugLevel    = 0;

    private LocalIntfManagerImpl intfManager;
    int sessionThreadLimit  = 10000;
    int newSessionSchedPolicy  = SCHED_SOFTREAL;
    int sessionSchedPolicy  = SCHED_SOFTREAL;
    boolean isShieldEnabled = true;
    String shieldFile       = null;
    Shield shield;
    
    /* Number of threads to donate to netcap */
    int numThreads        = 15;

    /* Debugging */
    private final Logger logger = Logger.getLogger( this.getClass());

    /* Default device values */
    String external = "eth0";
    String internal  = "eth1";

    /* If there is a DMZ interface, it is passed in using the system property */
    String dmz     = "";

    /* The NAT Checker */
    private final NatChecker natChecker = new NatChecker();
    
    /* Singleton */
    private Argon()
    {
    }

    public void run( PolicyManagerPriv policyManager, NetworkManagerImpl networkManager )
    {
        this.networkManager = networkManager;

        /* Get an instance of the shield */
        shield = Shield.getInstance();

        /* Parse all of the properties */
        parseProperties();

        try {
            init( policyManager );
        } catch ( ArgonException e ) {
            logger.fatal( "Error initializing argon", e );
            throw new IllegalStateException( "no", e );
        }

        registerHooks();
    }

    NatChecker getNatChecker()
    {
        return this.natChecker;
    }

    /**
     * Parse the user supplied properties
     */
    private void parseProperties()
    {
        String temp;
        if (( temp = System.getProperty( "argon.internal" )) != null ) {
            internal = temp;
        }

        if (( temp = System.getenv( "MVVM_INTERNAL_INTF" )) != null ) {
            if ( !temp.equals( internal )) logger.warn( "argon.internal,environ mismatch" );
            internal = temp;
        }

        if (( temp = System.getProperty( "argon.external" )) != null ) {
            external = temp;
        }

        if (( temp = System.getenv( "MVVM_EXTERNAL_INTF" )) != null ) {
            if ( !temp.equals( external )) logger.warn( "argon.external,environ mismatch" );
            external = temp;
        }

        if (( temp = System.getProperty( "argon.dmz" )) != null ) {
            dmz = temp;
        }

        if (( temp = System.getenv( "MVVM_DMZ_INTF" )) != null ) {
            if ( !temp.equals( external )) logger.warn( "argon.dmz,environ mismatch" );
            dmz = temp;
        }

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

        if (( temp = System.getProperty( "argon.shield.enabled" )) != null ) {
            isShieldEnabled = Boolean.parseBoolean( temp );
        }

        if (( temp = System.getProperty( "argon.shield.cfg_file" )) != null ) {
            shieldFile = temp;
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
        Netcap.registerUDPHook( UDPHook.getInstance());

        Netcap.registerTCPHook( TCPHook.getInstance());
    }

    /**
     * Initialize Netcap and any other supporting libraries.
     */
    private void init( PolicyManagerPriv policyManager ) throws ArgonException
    {
        Netcap.init( isShieldEnabled, netcapDebugLevel, jnetcapDebugLevel );

        /* Start the scheduler */
        Netcap.startScheduler();

        this.intfManager = new LocalIntfManagerImpl( policyManager );
        this.intfManager.initializeIntfArray( internal, external, dmz );

        /* Register the NatChecker */
        networkManager.registerListener( this.natChecker );

        /* Initialize the network manager, this has to be done after netcap init. */
        networkManager.init();

        if ( isShieldEnabled ) {
            shield.registerEventListener( ShieldMonitor.getInstance());
        }
        
        /* Initialize the shield configuration */
        LocalShieldManager lsm = MvvmContextFactory.context().localShieldManager();
        lsm.setIsShieldEnabled( isShieldEnabled );
        lsm.setShieldConfigurationFile( shieldFile );


        Vector.mvutilDebugLevel( mvutilDebugLevel );
        Vector.vectorDebugLevel( vectorDebugLevel );
        Vector.jvectorDebugLevel( jvectorDebugLevel );

        Netcap.getInstance().setNewSessionSchedPolicy( this.newSessionSchedPolicy );
        Netcap.getInstance().setSessionSchedPolicy( this.sessionSchedPolicy );

        /* Donate a few threads */
        Netcap.donateThreads( numThreads );
        sleepingThreads = numThreads;
        totalThreads    = numThreads;
        activeThreads   = 0;
        Netcap.getInstance().setSessionLimit( this.sessionThreadLimit );

        /* Initialize the InterfaceOverride table, this is just so the logger doesn't get into the NAT
         * transform context */
        InterfaceOverride.getInstance().clearOverrideList();
    }

    public void destroy()
    {
        logger.debug( "Shutting down" );
        ArgonManagerImpl argonManager = ArgonManagerImpl.getInstance();
        
        argonManager.isShutdown();
        networkManager.isShutdown();

        shield.unregisterEventListener();

        /* Remove both of the hooks to guarantee that no new sessions are created */
        Netcap.unregisterTCPHook();
        Netcap.unregisterUDPHook();

        VectronTable activeVectrons = VectronTable.getInstance();

        /* Close all of the vectoring machines */
        for ( int c = 0; c <  SHUTDOWN_ATTEMPTS ; c++ ) {
            if ( logger.isInfoEnabled()) {
                logger.info( "" + activeVectrons.count() + " active sessions remaining" );
            }

            if ( !activeVectrons.shutdownActive()) break;

            /* Sleep a little while vectrons shutdown. */
            try {
                Thread.sleep( SHUTDOWN_PAUSE );
            } catch ( InterruptedException e ) {
                logger.error( e.getMessage());
            }
        }

        Netcap.cleanup();

        try {
            networkManager.flushIPTables();
        } catch ( NetworkException e ) {
            logger.error( "Unable to flush iptables rules!!!!", e );
        }
    }

    public static Argon getInstance()
    {
        return INSTANCE;
    }


    /* ----------------- Package ----------------- */
    LocalIntfManager getIntfManager()
    {
        return this.intfManager;
    }
}
