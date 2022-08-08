/**
 * $Id: IntrusionPreventionEventMonitor.java 38792 2014-10-09 19:49:00Z dmorris $
 */
package com.untangle.app.intrusion_prevention;

import java.util.Date;

import org.apache.log4j.Logger;

import com.untangle.app.intrusion_prevention.IntrusionPreventionStatisticsParser;
import com.untangle.app.intrusion_prevention.IntrusionPreventionSuricataFastParser;
import com.untangle.uvm.UvmContextFactory;

/**
 * Process that:
 * -    Runs suricata statistics
 * -    Adds new entries from suricata logs to event logs.
 */
class IntrusionPreventionEventMonitor implements Runnable
{
    /* Interval to run */
    public static final long SLEEP_TIME_MSEC = (long)30 * 1000;

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    protected static final Logger logger = Logger.getLogger( IntrusionPreventionEventMonitor.class );

    private final IntrusionPreventionApp app;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    public IntrusionPreventionSuricataFastParser fastParser = new IntrusionPreventionSuricataFastParser();
    public IntrusionPreventionStatisticsParser statisticsParser = new IntrusionPreventionStatisticsParser();

    /**
     * Initialize event monitor.
     *
     * @param app
     *  Intrusion Prevention application.
     */
    protected IntrusionPreventionEventMonitor( IntrusionPreventionApp app )
    {
        this.app = app;
    }

    /**
     * Loop looking for new files and/or last file size change.
     */
    public void run()
    {
        logger.debug( "Starting" );

        if ( !isAlive ) {
            logger.error( "died before starting" );
            return;
        }

        Date now = new Date();

        while ( true ) {
            /* Check if the app is still running */
            if ( !isAlive )
                break;

            /* Update the current time */
            now.setTime( System.currentTimeMillis() );

            /**
             * Parse statistics
             */
            statisticsParser.parse( app );
            /**
             * Parse logs
             */
            fastParser.parse( app );

            /* sleep */
            try {
                Thread.sleep( SLEEP_TIME_MSEC );
            } catch ( InterruptedException e ) {
                logger.info( "ips event monitor was interrupted" );
            }

            /* Check if the app is still running */
            if ( !isAlive )
                break;
        }

        logger.debug( "Finished" );
    }

    /**
     * Start the process
     */
    public synchronized void start()
    {
        isAlive = true;

        logger.debug( "Starting Intrusion Prevention Event monitor" );

        /* If thread is not-null, there is a running thread that thinks it is alive */
        if ( thread != null ) {
            logger.debug( "Intrusion Prevention Event monitor is already running" );
            return;
        }

        thread = UvmContextFactory.context().newThread( this );
        thread.start();
    }

    /**
     * Stop the process
     */
    public synchronized void stop()
    {
        if ( thread != null ) {
            logger.debug( "Stopping Intrusion Prevention Event monitor" );

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

}
