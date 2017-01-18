/**
 * $Id: ReportsManagerImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

public class AlertManagerImpl implements AlertManager
{

    private static final Logger logger = Logger.getLogger(AlertManagerImpl.class);

    private static AlertManagerImpl instance = null;

    private AlertEventWriter eventWriter = new AlertEventWriter();

    protected AlertManagerImpl()
    {
        eventWriter.start();
    }

    public void logEvent( LogEvent event )
    {
        eventWriter.inputQueue.offer(event);
    }

    /**
     * This thread periodically walks through the entries and removes expired entries
     * It also explicitly releases hosts from the penalty box and quotas after expiration
     */

    /**
      * The amount of time for the event write to sleep
       * if there is not a lot of work to be done
    */
    private static int SYNC_TIME = 30*1000; /* 30 seconds */

    /**
     * If the event queue length reaches the high water mark
     * Then the eventWriter is not able to keep up with demand
     * In this case the overloadedFlag is set to true
     */
    private static int HIGH_WATER_MARK = 1000000;

    /**
     * If overloadedFlag is set to true and the queue shrinks to this size
     * then overloadedFlag will be set to false
     */
    private static int LOW_WATER_MARK = 100000;

    private static boolean forceFlush = false;

    private class AlertEventWriter implements Runnable
    {

        private volatile Thread thread;

        /**
         * Maximum number of events to write per work cycle
         */
        private int maxEventsPerCycle = 20000; 

        /**
         * If true then the eventWriter is considered "overloaded" and can not keep up with demand
         * This is set if the event queue length reaches the high water mark
         * In this case we stop logging events entirely until we are no longer overloaded
         */
        private boolean overloadedFlag = false;
    
        /**
         * This stores the maximum queue delay for the last batch
         * That is difference between now() and the oldest event in the batch
         * This approximates the delay its taking for events to be written to the database
         * If the event writer falls behind this value can get large.
         * Typical values less than a minute. A value of one hour would mean its behind and writing events slower than they are being created
         * and that it is currently taking one hour before new events are written to the database
         */
        private long writeDelaySec = 0;

        /**
         * This is a queue of incoming events
         */
        private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

        public void run()
        {
            thread = Thread.currentThread();

            LinkedList<LogEvent> logQueue = new LinkedList<LogEvent>();
            LogEvent event = null;

            /**
             * Loop indefinitely and continue logging events
             */
            while (thread != null) {
                /**
                 * Sleep until next log time
                 * If force flush was called, don't sleep
                 * If there is already a full runs worth of events, don't sleep
                 * If events are significantly delayed (more than 2x SYNC_TIME), don't sleep
                 */
                if ( forceFlush ||
                     (inputQueue.size() > maxEventsPerCycle) ||
                    (writeDelaySec*1000 >  SYNC_TIME*2) ) {
                    logger.debug("persist(): skipping sleep");
                    // minor sleep to let other threads that my want to synchronize on this run
                    try {Thread.sleep(100);} catch (Exception e) {}
                } else {
                    try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}
                }

                synchronized( this ) {
                    try {
                        /**
                         * Copy all events out of the queue
                        */
                        while ((event = inputQueue.poll()) != null && logQueue.size() < maxEventsPerCycle) {
                            logQueue.add(event);
                        }

                        /**
                         * Check queue lengths
                         */
                        if (!this.overloadedFlag && inputQueue.size() > HIGH_WATER_MARK)  {
                            logger.warn("OVERLOAD: High Water Mark reached.");
                            this.overloadedFlag = true;
                        }
                        if (this.overloadedFlag && inputQueue.size() < LOW_WATER_MARK) {
                            logger.warn("OVERLOAD: Low Water Mark reached. Continuing normal operation.");
                            this.overloadedFlag = false;
                        }

                        /**
                         * Run alert rules
                         */
                        for ( LogEvent le : logQueue ) {
                            // runAlertRules( alertRules, event, reports );
                        }
                    
                        try {Thread.sleep(1000);} catch (Exception e) {}

                    } catch (Exception e) {
                        logger.warn("Failed to write alert events.", e);
                    } finally {
                        /**
                         * If the forceFlush flag was set, reset it and wake any interested parties
                         */
                        if (forceFlush) {
                            forceFlush = false; //reset global flag
                            notifyAll();  /* notify any waiting threads that the flush is done */ 
                        }
                    }
                }
            }
        }

        protected void start()
        {
            UvmContextFactory.context().newThread(this).start();
        }

        protected void stop()
        {
            // this is disabled because it causes boxes to hang on stopping the uvm
            // forceFlush(); /* flush last few events */

            Thread tmp = thread;
            thread = null; /* thread will exit if thread is null */
            if (tmp != null) {
                tmp.interrupt();
            }
        }
    }

}
