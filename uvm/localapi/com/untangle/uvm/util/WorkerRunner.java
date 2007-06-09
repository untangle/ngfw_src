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

package com.untangle.mvvm.util;

import com.untangle.mvvm.MvvmLocalContext;
import org.apache.log4j.Logger;

public class WorkerRunner
{
    /* give the thread 3 seconds to die by default */
    private static final long DEFAULT_STOP_DELAY_MILLIS = 3000;

    /* At most 5 attempts at ending the thread */
    private static final int MAX_STOP_INTERRUPT_COUNT = 5;

    private final Worker worker;
    private final MvvmLocalContext localContext;
    private final long stopDelayMillis;

    private Thread thread;

    private final Logger logger = Logger.getLogger( this.getClass());

    public WorkerRunner( Worker worker, MvvmLocalContext localContext )
    {
        this( worker, localContext, DEFAULT_STOP_DELAY_MILLIS );
    }

    public WorkerRunner( Worker worker, MvvmLocalContext localContext, long stopDelayMillis )
    {
        this.worker = worker;
        this.localContext = localContext;
        this.stopDelayMillis = stopDelayMillis;
    }

    public synchronized void start()
    {
        if ( this.thread != null ) {
            logger.info( "The worker: " + worker + " is already started." );
            return;
        }

        logger.debug( "Starting the worker: " + this.worker );

        this.thread = localContext.newThread( new RunnerThread());
        thread.start();
    }

    public synchronized void stop()
    {
        Thread currentThread = this.thread;
        this.thread = null;

        logger.debug( "Stopping the worker: " + this.worker );

        int count = 0;

        /* calcuate the end time */
        long endTime = System.currentTimeMillis() + this.stopDelayMillis;

        while ( true ) {
            try {
                if ( currentThread == null ) break;
                currentThread.interrupt();

                /* Calculate much more time to wait for the helper thread to die */
                long delay = endTime - System.currentTimeMillis();
                if ( delay < 0 ) {
                    logger.warn( "timeout trying to kill a worker thread" );
                    break;
                }
                currentThread.join( delay );

                /* stop after successfully joining */
                break;
            } catch ( SecurityException e ) {
                logger.error( "security exception, impossible", e );
                break;
            } catch ( InterruptedException e ) {
                if ( count > MAX_STOP_INTERRUPT_COUNT ) {
                    logger.warn( "interrupted too many times while stopping the thread, cancelling.", e );
                }

                logger.info( "interrupted while stopping the thread, attempting again." );
                count++;
                continue;
            }
        }
    }

    private class RunnerThread implements Runnable
    {
        public void run()
        {
            logger.debug( "Starting" );
            while( thread == Thread.currentThread()) {
                try {
                    worker.work();
                } catch( InterruptedException e ) {
                    continue;
                }
            }
        }
    }
}
