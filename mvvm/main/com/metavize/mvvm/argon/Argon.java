/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.Shield;
import com.metavize.jnetcap.JNetcapException;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.jvector.Vector;

import com.metavize.mvvm.shield.ShieldMonitor;

public class Argon
{    
    /* Number of times to try and shutdown all vectoring machines cleanly before giving up */
    protected static final int SHUTDOWN_ATTEMPTS = 5;

    /* Amount of time between subsequent calls to shutdown all of the vectoring machines */
    protected static final int SHUTDOWN_PAUSE    = 2000;

    /* Maximum number of threads allowed at any time */
    private static int MAX_THREADS = 10000;
    
    /* Singleton */
    private static final Argon INSTANCE = new Argon();
        
    private int sleepingThreads;
    private int totalThreads;
    private int activeThreads;
    
    protected int netcapDebugLevel    = 1;
    protected int jnetcapDebugLevel   = 1;
    protected int vectorDebugLevel    = 0;
    protected int jvectorDebugLevel   = 0;
    protected int mvutilDebugLevel    = 0;
    protected boolean isShieldEnabled = true;
    protected String shieldFile       = null;
    protected Shield shield;
    
    /* Number of threads to donate to netcap */
    protected int numThreads        = 15;

    /* Debugging */
    private final Logger logger = Logger.getLogger( this.getClass());

    /* Inside device */
    protected String inside  = "eth1";
    protected String outside = "eth0";
    protected String dmz[]   = null;

    /* Singleton */
    private Argon()
    {
    }
        
    /* XXX
     * This should be able to throw an exception
     */
    public static void main( String args[] ) // throws ArgonException
    {
        INSTANCE.run( args );
    }
    
    /* For some reason this also can't be called main, even though one is static and one is not */
    public void run( String args[] )
    {
        /* Get an instance of the shield */
        shield = Shield.getInstance();

        /* Parse all of the properties */
        parseProperties();

        init();

        registerHooks();
    }

    /**
     * Parse the user supplied properties
     */
    private void parseProperties()
    {
        String temp;
        if (( temp = System.getProperty( "argon.inside" )) != null ) {
            inside = temp;
        }

        if (( temp = System.getProperty( "argon.outside" )) != null ) {
            outside = temp;
        }

        if (( temp = System.getProperty( "argon.numthreads" )) != null ) {
            int count;
            count = Integer.parseInt( temp );
            if ( count < 0 ) {
                throw new IllegalArgumentException( "argon.numthreads must be > 0. " + count ) ;
            }
            numThreads = count;
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
    private void init()
    {
        Netcap.init( isShieldEnabled, netcapDebugLevel, jnetcapDebugLevel );

        if ( isShieldEnabled ) { 
            shield.registerEventListener( ShieldMonitor.getInstance());
        }
        
        Vector.mvutilDebugLevel( mvutilDebugLevel );
        Vector.vectorDebugLevel( vectorDebugLevel );
        Vector.jvectorDebugLevel( jvectorDebugLevel );

        /* Donate a few threads */
        Netcap.donateThreads( numThreads );
        sleepingThreads = numThreads;
        totalThreads    = numThreads;
        activeThreads   = 0;

        /* Start the scheduler */
        Netcap.startScheduler();

        /* Convert all of the interface names from strings to bytes */
        try {
            IntfConverter.init( inside, outside, dmz );
        } catch ( ArgonException e ) {
            logger.error( "Error initializing IntfConverter, continuing", e );
        }

        if ( isShieldEnabled && shieldFile != null )
            shield.config( shieldFile );

        try {
            ArgonManagerImpl.getInstance().updateAddress();
        } catch ( ArgonException e ) {
            logger.error( "Unable to initialize iptables rules!!!!", e );
        }
    }

    public void destroy() 
    {
        logger.debug( "Shutting down" );
        ArgonManagerImpl argonManager = ArgonManagerImpl.getInstance();
        
        RuleManager.getInstance().isShutdown();
        argonManager.isShutdown();
        
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
            argonManager.argonRestoreBridge( MvvmContextFactory.context().networkingManager().get());
        } catch ( ArgonException e ) {
            logger.error( "Unable to remove restore bridge!!!!", e );
        }
        
        try {
            RuleManager.getInstance().destroyIptablesRules();
        } catch ( ArgonException e ) {
            logger.error( "Unable to remove iptables rules!!!!", e );
        }

    }

    public static Argon getInstance()
    {
        return INSTANCE;
    }
}
