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
package com.metavize.mvvm.tran;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;

import com.metavize.mvvm.logging.StatisticEvent;

import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TransformContextFactory;

import com.metavize.mvvm.tran.firewall.IntfMatcher;

public abstract class StatisticManager implements Runnable
{    
    /* Log every five minutes */
    private static final long   SLEEP_TIME_MSEC = 5 * 60 * 1000;

    /* Delay a second while the thread is joining */
    private static final long   THREAD_JOIN_TIME_MSEC = 1000;
            
    /* The thread the monitor is running on */
    private Thread thread = null;
    
    /* Status of the monitor */
    private volatile boolean isAlive = true;
    
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();
    private final Logger logger = Logger.getLogger( this.getClass());
        
    protected StatisticManager()
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
            StatisticEvent statisticEvent = getNewStatisticEvent();
            
            try { 
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "Statistics manager was interrupted" );
            }
            
            /* Only log the stats if there is something to log */
            if ( statisticEvent.hasStatistics()) {
                eventLogger.info( statisticEvent );
                statisticEvent = getNewStatisticEvent();
            } else {
                logger.debug( "No statistics available" );
            }
            
            /* Check if the transform is still running */
            if ( !isAlive ) break;
        }

        logger.debug( "Finished" );
    }

    public synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting Statistic Manager" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Statistic Manager is already running" );
            return;
        }

        thread = new Thread( this );
        thread.start();
    }

    public synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping Statistic Manager" );
            
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

    private final void waitForTransformContext()
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

    protected abstract StatisticEvent getNewStatisticEvent();
}
