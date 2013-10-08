/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/uvm/util/WorkerRunner.java $
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

package com.untangle.uvm.util;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;

public class WorkerRunner
{
    /* give the thread 3 seconds to die by default */
    private static final long DEFAULT_STOP_DELAY_MILLIS = 3000;

    /* At most 5 attempts at ending the thread */
    private static final int MAX_STOP_INTERRUPT_COUNT = 5;

    private final Worker worker;
    private final UvmContext localContext;
    private final long stopDelayMillis;

    private Thread thread;

    private final Logger logger = Logger.getLogger( this.getClass());

    public WorkerRunner( Worker worker, UvmContext localContext )
    {
        this( worker, localContext, DEFAULT_STOP_DELAY_MILLIS );
    }

    public WorkerRunner( Worker worker, UvmContext localContext, long stopDelayMillis )
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


        worker.start();

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

        worker.stop();
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
