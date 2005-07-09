/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TransformContextFactory;

import com.metavize.mvvm.tran.firewall.IntfMatcher;

class NatStatisticManager implements Runnable
{    
    private static NatStatisticManager INSTANCE = null;
    
    /* Log every five minutes */
    private static final long   SLEEP_TIME_MSEC = 5 * 60 * 1000;

    /* Delay a second while the thread is joining */
    private static final long   THREAD_JOIN_TIME_MSEC = 1000;
            
    /* The thread the monitor is running on */
    private Thread thread = null;
    
    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* Interface matcher to determine if the sessions is incoming or outgoing */
    final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;
    
    private final Logger logger = Logger.getLogger( this.getClass());
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();
    
    private NatStatisticEvent statisticEvent = new NatStatisticEvent();
    
    private NatStatisticManager()
    {
    }
    
    public void run()
    {
        logger.debug( "Starting" );

        waitForTransformContext();

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        while ( true ) {
            try { 
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "NatStatistic manager was interrupted" );
            }
            
            /* Only log the stats if there is something to log */
            if ( statisticEvent.hasStatistics()) {
                eventLogger.info( statisticEvent );
                statisticEvent = new NatStatisticEvent();
            }
            
            /* Check if the transform is still running */
            if ( !isAlive ) break;
        }

        logger.debug( "Finished" );
    }

    synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting thread" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Monitor is already running" );
            return;
        }

        thread = new Thread( this );
        thread.start();
    }

    synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping thread" );
            
            isAlive = false;
            try { 
                thread.interrupt();
                thread.join( THREAD_JOIN_TIME_MSEC );
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
            } catch ( InterruptedException e ) {
                logger.error( "interrupted while stopping", e );
            }
            thread = null;
        }
    }

    private void waitForTransformContext()
    {
        Tid tid  = TransformContextFactory.context().getTid();
        
        while ( true ) {
            if ( MvvmContextFactory.context().transformManager().transformContext( tid ) != null ) {
                break;
            }
            
            try {
                Thread.sleep( 1000 );
            } catch ( InterruptedException e ) {
                logger.info( "waitForTransformContext was interrupted" );
            }
            
            if ( !isAlive ) return;          
        }
    }

    void incrNatSessions()
    {
        this.statisticEvent.incrNatSessions();
    }

    void incrRedirect( Protocol protocol, IPNewSessionRequest request )
    {
        boolean isOutgoing = matcherIncoming.isMatch( request.clientIntf());

        if ( protocol == Protocol.TCP ) {
            if ( isOutgoing ) incrTcpOutgoingRedirects();
            else              incrTcpIncomingRedirects();
        } else {
            if (( request.clientPort() == 0 ) && ( request.serverPort() == 0 )) {
                /* Ping Sessions */
                if ( isOutgoing ) incrIcmpOutgoingRedirects();
                else              incrIcmpIncomingRedirects();
            } else {
                /* UDP Sessions */
                if ( isOutgoing ) incrUdpOutgoingRedirects();
                else              incrUdpIncomingRedirects();
            }
        }
    }

    void incrTcpIncomingRedirects()
    {
        this.statisticEvent.incrTcpIncomingRedirects();
    }

    void incrTcpOutgoingRedirects()
    {
        this.statisticEvent.incrTcpOutgoingRedirects();
    }

    void incrUdpIncomingRedirects()
    {
        this.statisticEvent.incrUdpIncomingRedirects();
    }

    void incrUdpOutgoingRedirects()
    {
        this.statisticEvent.incrUdpOutgoingRedirects();
    }

    void incrIcmpIncomingRedirects()
    {
        this.statisticEvent.incrIcmpIncomingRedirects();
    }

    void incrIcmpOutgoingRedirects()
    {
        this.statisticEvent.incrIcmpOutgoingRedirects();
    }

    void incrDmzSessions()
    {
        this.statisticEvent.incrDmzSessions();
    }

    static synchronized NatStatisticManager getInstance()
    {
        if ( INSTANCE == null ) 
            INSTANCE = new NatStatisticManager();

        return INSTANCE;
    }
}
