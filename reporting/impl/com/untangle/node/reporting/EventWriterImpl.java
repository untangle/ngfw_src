/**
 * $Id: EventWriter.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;


/**
 * Worker that batches and flushes events to the database.
 */
public class EventWriterImpl implements Runnable
{
    /**
     * The amount of time for the event write to sleep
     * if there is not a lot of work to be done
     */
    private static final int SYNC_TIME = 30*1000; /* 30 seconds */

    /**
     * Maximum number of events to write per work cycle
     */
    private static final int MAX_EVENTS_PER_CYCLE = 10000; 

    /**
     * If the event queue length reaches the high water mark
     * Then the eventWriter is not able to keep up with demand
     * In this case the overloadedFlag is set to true
     */
    private static final int HIGH_WATER_MARK = 1000000;

    /**
     * If overloadedFlag is set to true and the queue shrinks to this size
     * then overloadedFlag will be set to false
     */
    private static final int LOW_WATER_MARK = 100000;

    private final Logger logger = Logger.getLogger(getClass());

    private static boolean forceFlush = false;

    private volatile Thread thread;

    private ReportingNodeImpl node;

    private Connection dbConnection;

    private long lastLoggedWarningTime = System.currentTimeMillis();

    /**
     * If true then the eventWriter is considered "overloaded" and can not keep up with demand
     * This is set if the event queue length reaches the high water mark
     * In this case we stop logging events entirely until we are no longer overloaded
     */
    private boolean overloadedFlag = false;
    
    /**
     * This stores the approximate write times of events
     * It is updated each time the events are flushed
     * using a weighted mixture of the current value with the old value
     */
    private double avgWriteTimePerEvent = 0.0;

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
     * Stores the total number of events written
     */
    private long totalEventsWritten = 0;
    
    /**
     * This is a queue of incoming events
     */
    private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

    public EventWriterImpl( ReportingNodeImpl node )
    {
        this.node = node;
        this.dbConnection = null;
    }

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
                 (inputQueue.size() > MAX_EVENTS_PER_CYCLE) ||
                 (writeDelaySec*1000 >  SYNC_TIME*2) ) {
                logger.debug("persist(): skipping sleep");
            } else {
                try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}
            }

            synchronized( this ) {
                try {
                    if ( dbConnection == null || dbConnection.isClosed() ) {
                        dbConnection = this.node.getDbConnection();
                    }
                    if ( dbConnection == null || dbConnection.isClosed() ) {
                        logger.warn("Unable to get connection to DB, dropping events...");
                        while ((event = inputQueue.poll()) != null) {}
                        continue; 
                    }

                    /**
                     * Copy all events out of the queue
                     */
                    while ((event = inputQueue.poll()) != null && logQueue.size() < MAX_EVENTS_PER_CYCLE) {
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
                     * If there is anything to log, log it to the database
                     */
                    if (logQueue.size() > 0)
                        persist(logQueue);
                    else
                        try {Thread.sleep(1000);} catch (Exception e) {}

                
                } catch (Exception e) {
                    logger.warn("Failed to write events.", e);
                } finally {
                    /**
                     * If the forceFlush flag was set, reset it and wake any interested parties
                     */
                    if (forceFlush) {
                        forceFlush = false; //reset global flag
                        notifyAll(); /* notify any waiting threads that the flush is done */
                    }
                }
            }
        }

        if (dbConnection != null) {
            try {dbConnection.close();} catch (SQLException e) { logger.warn("Exception during dbConnection close",e); }
        }
        
    }

    /**
     * Force currently queued events to the DB
     */
    public void forceFlush()
    {
        if ( thread == null ) {
            logger.warn("forceFlush() called, but reporting not running.");
            return;
        }

        /**
         * Wait on the flush to finish - we will get notified)
         */
        synchronized( this ) {
            forceFlush = true;
            logger.info("forceFlush() ...");
            thread.interrupt();

            while (true) {
                try {wait();} catch (java.lang.InterruptedException e) {}

                if (!forceFlush) {
                    logger.info("forceFlush() complete.");
                    return;
                }
            }
        }
    }
    
    public void logEvent(LogEvent event)
    {
        if ( this.thread == null ) {
            if ( System.currentTimeMillis() - this.lastLoggedWarningTime > 10000 ) {
                logger.warn("Reporting node not running, discarding event");
                this.lastLoggedWarningTime = System.currentTimeMillis();
            }
            return;
        }
        if ( this.overloadedFlag ) {
            if ( System.currentTimeMillis() - this.lastLoggedWarningTime > 10000 ) {
                logger.warn("Event Writer overloaded, discarding event");
                this.lastLoggedWarningTime = System.currentTimeMillis();
            }
            return;
        }
        
        String tag = "uvm[0]: ";
        event.setTag(tag);
        
        /**
         * Send to queue for database logging
         */
        if (!inputQueue.offer(event)) {
            logger.warn("dropping logevent: " + event);
        }

        /**
         * Send it to syslog (make best attempt - ignore errors)
         */
        try {
            SyslogManagerImpl.manager().sendSyslog(event, event.getTag());
        } catch (Exception exn) { 
            logger.warn("failed to send syslog", exn);
        }
    }

    public double getAvgWriteTimePerEvent()
    {
        /**
         * If too few datapoints, just return 0.0
         */
        if (totalEventsWritten < 50)
            return 0.0;
                         
        return this.avgWriteTimePerEvent;
    }

    public long getWriteDelaySec()
    {
        /**
         * If too few datapoints, just return 0
         */
        if (totalEventsWritten < 100)
            return 0;
                         
        return this.writeDelaySec;
    }
    
    /**
     * write the logQueue to the database
     */
    private void persist( LinkedList<LogEvent> logQueue )
    {
        /**
         * These map stores the type of objects being written and stats purely for debugging output
         */
        Map<String,Integer> countMap = new HashMap<String, Integer>(); // Map from Event type to count of this type of event
        Map<String,Long> timeMap     = new HashMap<String, Long>();    // Map from Event type to culumalite time to write these events
        if (logger.isInfoEnabled()) {
            for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
                LogEvent event = i.next();

                /**
                 * Update the stats
                 */
                String eventTypeName = event.getClass().getSimpleName();
                Integer currentCount = countMap.get(eventTypeName);
                if (currentCount == null)
                    currentCount = 1;
                else
                    currentCount = currentCount+1;
                countMap.put(eventTypeName, currentCount);
            }
        }

        /**
         * Calculate the write delay
         */
        LogEvent first = null;
        try {first = logQueue.getFirst();} catch (Exception e) {}
        if (first != null && first.getTimeStamp() != null) {
            this.writeDelaySec = (System.currentTimeMillis() - first.getTimeStamp().getTime())/1000L;
        }
        
        int count = logQueue.size();
        long t0 = System.currentTimeMillis();

        logger.debug("Writing events to database... (size: " + logQueue.size() + ")");
        for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
            LogEvent event = i.next();

            /**
             * Write event to database using SQL
             * If fails, just move on
             */
            try {
                List<PreparedStatement> pstmts = event.getDirectEventSqls( this.dbConnection );
                if (pstmts != null) {
                    long write_t0 = System.currentTimeMillis();
                    for (PreparedStatement pstmt : pstmts) {
                        logger.debug("Write direct event: " + pstmt);
                        try {
                            pstmt.execute();
                        } catch (SQLException e) {
                            logger.warn("Failed SQL query for " + event.getClass() + ": \"" + pstmt + "\"", e);
                        }
                    }
                    long write_t1 = System.currentTimeMillis();

                    if (logger.isInfoEnabled()) {
                        /**
                         * Update the stats
                         */
                        String eventTypeName = event.getClass().getSimpleName();
                        Long currentTime = timeMap.get(eventTypeName);
                        if (currentTime == null)
                            currentTime = 0L;
                        currentTime = currentTime+(write_t1-write_t0); //add time to write this instances
                        timeMap.put(eventTypeName, currentTime);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed SQL query(s) for event \"" + event.getClass() + "\" object: \"" + event.toJSONString() + "\"", e);
            }


            i.remove();
        }

        logger.debug("Writing events to database... Complete");

        /**
         * This looks at the event queue to be written and builds a summary string of the type of objects about to be written
         * Example: "SessionEvent[45,10ms] WebFilterEvent[10,20ms]
         */
        if (logger.isInfoEnabled()) {
            /**
             * Sort the list
             */
            LinkedList<EventTypeMap> eventTypeMapList = new LinkedList<EventTypeMap>();
            for ( String key : countMap.keySet() ) {
                eventTypeMapList.add(new EventTypeMap(key, countMap.get(key)));
            }
            Collections.sort(eventTypeMapList, new EventTypeMapComparator());

            /**
             * Build the output string
             */
            String mapOutput = "";
            for ( EventTypeMap item : eventTypeMapList ) {
                Long totalTimeMs = timeMap.get(item.name);
                if (totalTimeMs == null)
                    mapOutput += " " + item.name + "[" + item.count + "]";
                else
                    mapOutput += " " + item.name + "[" + item.count + "," + totalTimeMs + "ms" + "," + String.format("%.1f",((float)totalTimeMs/((float)(item.count)))) + "avg]";
            }

            long t1 = System.currentTimeMillis();
            long elapsedTime = t1-t0;
            double avgTime = ((double)elapsedTime/((double)count));
            logger.info("persist(): EventStats " +
                        String.format("%5d",count) +
                        " events [" + String.format("%5d",elapsedTime) +
                        " ms] [" + String.format("%4.1f",avgTime) +
                        " avg] [" + String.format("%5d",writeDelaySec) +
                        "s delay] [" + String.format("%5d",inputQueue.size()) + " pending]");
            logger.info("persist(): EventMap   " + mapOutput);

            /**
             * update avgWriteTimePerEvent and totalEventsWritten
             */
            this.totalEventsWritten += count;
            this.avgWriteTimePerEvent = (this.avgWriteTimePerEvent * .8) + (avgTime * .2);
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

    class EventTypeMap
    {
        public String name;
        public int count;
        
        public EventTypeMap(String name, int count)
        {
            this.name = name;
            this.count = count;
        }
    }
    
    class EventTypeMapComparator implements Comparator<EventTypeMap>
    {
        public int compare( EventTypeMap a, EventTypeMap b )
        {
            if (a.count < b.count) {
                return 1;
            } else if (a.count == b.count) {
                return 0;
            } else {
                return -1;
            }
        }
    }

}
