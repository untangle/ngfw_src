/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */
package com.untangle.uvm.node;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.StatisticEvent;

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

    protected final EventLogger eventLogger;
    private final Logger logger = Logger.getLogger( this.getClass());
    private final LocalUvmContext localContext;

    protected StatisticManager(EventLogger eventLogger)
    {
        this.localContext = LocalUvmContextFactory.context();
        this.eventLogger = eventLogger;
    }

    protected StatisticManager(LocalUvmContext localContext,
                               EventLogger eventLogger)
    {
        this.localContext = localContext;
        this.eventLogger = eventLogger;
    }

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
                logger.info( "Statistics manager was interrupted" );
            }

            /* Only log the stats if there is something to log */
            if ( statisticEvent.hasStatistics()) {
                /* Pre-cache the statistics to insure that no stats are lost */
                StatisticEvent currentStatistics = statisticEvent;
                statisticEvent = getNewStatisticEvent();
                eventLogger.log( currentStatistics );
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

        thread = this.localContext.newThread( this );
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
