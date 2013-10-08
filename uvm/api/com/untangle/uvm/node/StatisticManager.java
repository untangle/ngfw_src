/*
 * $Id: StatisticManager.java 30512 2011-12-18 21:48:44Z dmorris $
 */
package com.untangle.uvm.node;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.logging.LogEvent;

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

    private final Logger logger = Logger.getLogger( this.getClass());

    protected StatisticManager() { }

    public void run()
    {
        logger.debug( "Starting" );

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        StatisticEvent statisticEvent = getInitialStatisticEvent();

        while ( true ) {
            try {
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.debug( "Statistics manager was interrupted" );
            }

            /* Only log the stats if there is something to log */
            if ( statisticEvent.hasStatistics()) {
                /* Pre-cache the statistics to insure that no stats are lost */
                StatisticEvent currentStatistics = statisticEvent;
                statisticEvent = getNewStatisticEvent();
                UvmContextFactory.context().logEvent( currentStatistics );
            } else {
                logger.debug( "No statistics available" );
            }

            /* Check if the node is still running */
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

        thread = UvmContextFactory.context().newThread( this );
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

    protected abstract StatisticEvent getInitialStatisticEvent();

    protected abstract StatisticEvent getNewStatisticEvent();
}
