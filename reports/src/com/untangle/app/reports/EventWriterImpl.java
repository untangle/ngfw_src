/**
 * $Id$
 */
package com.untangle.app.reports;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private static final Logger logger = Logger.getLogger(EventWriterImpl.class);

    /**
     * The amount of time for the event write to sleep
     * if there is not a lot of work to be done
     */
    private static int SYNC_TIME = 10*1000; /* 10 seconds */

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

    /**
     * Maximum number of events to write per work cycle
     */
    private static int MAX_EVENTS_PER_CYCLE = 50000;

    private static boolean forceFlush = false;

    private volatile Thread thread;

    private ReportsApp app;

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
    private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<>();

    static {
        String temp;
        if ((temp = System.getProperty("reports.max_queue_len")) != null) {
            try {
                HIGH_WATER_MARK = Integer.parseInt(temp);
            } catch (Exception e) {
                logger.warn("Invalid value: " + System.getProperty( "reports.max_queue_len" ),e);
            }
        }
        if ((temp = System.getProperty("reports.events_per_cycle")) != null) {
            try {
                MAX_EVENTS_PER_CYCLE = Integer.parseInt(temp);
            } catch (Exception e) {
                logger.warn("Invalid value: " + System.getProperty( "reports.events_per_cycle" ),e);
            }
        }
        if ((temp = System.getProperty("reports.sync_time")) != null) {
            try {
                SYNC_TIME = Integer.parseInt( temp );
            } catch (Exception e) {
                logger.warn("Invalid value: " + System.getProperty( "reports.sync_time" ),e);
            }
        }
    }

    /**
     * Initialize event writer.
     *
     * @param app
     *  Reports application.
     */
    public EventWriterImpl( ReportsApp app )
    {
        this.app = app;
        this.dbConnection = null;
    }

    /**
     * Run main loop of writer.
     */
    public void run()
    {
        thread = Thread.currentThread();

        LinkedList<LogEvent> logQueue = new LinkedList<>();
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
                // minor sleep to let other threads that my want to synchronize on this run
                try {Thread.sleep(100);} catch (Exception e) {}
            } else {
                try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}
            }

            synchronized( this ) {
                try {
                    if ( dbConnection == null || dbConnection.isClosed() ) {
                        dbConnection = this.app.getDbConnection();
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
            logger.warn("forceFlush() called, but reports not running.");
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
    
    /**
     * Send log event to queue.
     *
     * @param event
     *  Event to log.
     */
    public void logEvent(LogEvent event)
    {
        if ( this.thread == null ) {
            if ( System.currentTimeMillis() - this.lastLoggedWarningTime > 10000 ) {
                logger.warn("Reports app not running, discarding event");
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
        
        /**
         * Send to queue for database logging
         */
        if (!inputQueue.offer(event)) {
            logger.warn("dropping logevent: " + event);
        }

    }

    /**
     * Get the average write time per event.
     *
     * @return
     *  Return timing value.
     */
    public double getAvgWriteTimePerEvent()
    {
        /**
         * If too few datapoints, just return 0.0
         */
        if (totalEventsWritten < 500)
            return 0.0;
                         
        return this.avgWriteTimePerEvent;
    }

    /**
     * Get the write delay in seconds.
     *
     * @return
     *  Return write delay values in seconds.
     */
    public long getWriteDelaySec()
    {
        /**
         * If too few datapoints, just return 0
         */
        if (totalEventsWritten < 500)
            return 0;
                         
        return this.writeDelaySec;
    }
    
    /**
     * Write the logQueue to the database
     *
     * @param logQueue
     *  Log queue to push to database.
     */
    private void persist( LinkedList<LogEvent> logQueue )
    {
        /**
         * These map stores the type of objects being written and stats purely for debugging output
         */
        Map<String,Integer> countMap = new HashMap<>(); // Map from Event type to count of this type of event
        Map<String,Long> timeMap     = new HashMap<>();    // Map from Event type to culumalite time to write these events
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

        HashMap<String,PreparedStatement> statementCache = new LinkedHashMap<>();
        
        logger.debug("Compiling PreparedStatement(s)... (event count: " + logQueue.size() + ")");
        for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
            LogEvent event = i.next();
            
            try {
                event.compileStatements( this.dbConnection, statementCache );
            } catch ( java.sql.BatchUpdateException e) {
                logger.warn("Failed SQL query(s) for event \"" + event.getClass() + "\" object: \"" + event.toJSONString(), e);
                if ( e.getNextException() != null )
                    logger.warn("Next Exception: ", e.getNextException());
                if ( e.getCause() != null )
                    logger.warn("Cause: ", e.getCause());
            } catch (Exception e) {
                logger.warn("Failed SQL query(s) for event \"" + event.getClass() + "\" object: \"" + event.toJSONString(), e);
                if ( e.getCause() != null )
                    logger.warn("Cause: ", e.getCause());
            }

            i.remove();
        }
        logger.debug("Compiling PreparedStatement(s)... Complete");

        int statementCount = statementCache.size();
        logger.debug("Executing PreparedStatement(s)... (statement count: " + statementCache.size() + ")");
        java.util.Set<Map.Entry<String,PreparedStatement>> entries = statementCache.entrySet();

        /**
         * Ideally we would write all events in the order that they are logged.
         * However, since all events of the same type grouped as a batch, we must decide what order to write events by their type.
         *
         * We could add a rank by each event type and sort, however the below hack is relatively simple
         * First write all INSERTS. Then write all UPDATES. We want to write all INSERT type events first because
         * the UPDATE type events rely on the data being present in the table to update it.
         * Lastly write SessionMinutesEvents, despite being an INSERT it relies on the data being present and current in sessions.
         *
         * We do three iterations on the list
         * 1) write inserts type events - we must write these first because updates will update existing entries
         * 2) write update type events - we write these second because they update the previously inserted values
         * 3) write SessionMinuteEvents - write these last because despite being inserts they refer to inserted and update values in the sessions table
         */
        for (int i=0;i<3;i++) {
            for (Iterator<Map.Entry<String,PreparedStatement>> j = entries.iterator(); j.hasNext(); ) {
                Map.Entry<String,PreparedStatement> entry = j.next();

                PreparedStatement statement = entry.getValue();
                String str = entry.getKey();
                String[] splits = str.split(",",2);
                String className = splits[0];
                String sql = splits[1];

                if (className == null)
                    logger.warn("Invalid Key: " + sql);

                // only handle INSERTS on first 
                if ( i == 0 ) {
                    if ( "com.untangle.uvm.app.SessionMinuteEvent".equals(className) ) {
                        continue;
                    }
                    if (!sql.substring(0,10).contains("INSERT") ) {
                        continue;
                    }
                }
                // only handle UPDATES on second 
                if ( i == 1 ) {
                    if ( !sql.substring(0,10).contains("UPDATE") )
                        continue;
                }
                // only handle SessionMinuteEvent on third
                if ( i == 2 ) {
                    if ( !"com.untangle.uvm.app.SessionMinuteEvent".equals(className) ) {
                        logger.warn("Unknown event in third run: " + className);
                    }
                }
                
                // remove it from the list
                j.remove();
            
                /**
                 * Write event to database using SQL
                 * If fails, just move on
                 */
                try {
                    long write_t0 = System.currentTimeMillis();
                    logger.debug("Writing " + className + " events...");
                    try {
                        //statement.execute();
                        statement.executeBatch();
                    } catch (Exception e) {
                        logger.warn("Failed SQL query for " + statement, e);
                        Throwable t = e;
                        while ( ( t = t.getCause() ) != null ) {
                            logger.warn("Cause: " + t, t);
                        }
                    } finally {
                        try {
                            statement.close();
                        } catch (Exception e) {
                            logger.warn("Failed to close statement", e);
                        }
                    }
                    long write_t1 = System.currentTimeMillis();

                    if (logger.isInfoEnabled() && className != null) {
                        /**
                         * Update the stats
                         */
                        String[] parts = className.split("\\.");
                        String eventTypeName = parts[parts.length-1];
                        //String eventTypeName = statements.get(statement).getClass().getSimpleName();
                        Long currentTime = timeMap.get(eventTypeName);
                        if (currentTime == null)
                            currentTime = 0L;
                        currentTime = currentTime+(write_t1-write_t0); //add time to write this instances
                        timeMap.put(eventTypeName, currentTime);
                    }
                } catch (Exception e) {
                    logger.warn("Failed SQL query for " + statement, e);
                }
            }
        }

        for (Iterator<Map.Entry<String,PreparedStatement>> j = entries.iterator(); j.hasNext(); ) {
            Map.Entry<String,PreparedStatement> entry = j.next();
            PreparedStatement statement = entry.getValue();
            logger.warn("Unhandled Event! : " + statement);
        }
        logger.debug("Executing PreparedStatement(s)... Complete");

        /**
         * This looks at the event queue to be written and builds a summary string of the type of objects about to be written
         * Example: "SessionEvent[45,10ms] WebFilterEvent[10,20ms]
         */
        if (logger.isInfoEnabled()) {
            /**
             * Sort the list
             */
            LinkedList<EventTypeMap> eventTypeMapList = new LinkedList<>();
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
                    mapOutput += " " + item.name + "[" + item.count + "," + totalTimeMs + "ms" + "," + String.format("%.2f",((float)totalTimeMs/((float)(item.count)))) + "avg]";
            }

            long t1 = System.currentTimeMillis();
            long elapsedTime = t1-t0;
            double avgTime = ((double)elapsedTime/((double)count));
            double ratePerSec = ((double)1000)/avgTime;
            logger.info("persist(): EventStats " +
                        String.format("%5d",count) + " events, " +
                        String.format("%5d",statementCount) + " statements " +
                        "[" + String.format("%5d",elapsedTime) + " ms] " +
                        "[" + String.format("%5.0f",ratePerSec) + " event/s] " + 
                        "[" + String.format("%5.2f",avgTime) + " avg] " +
                        "[" + String.format("%5d",writeDelaySec) + "s delay] " + 
                        "[" + String.format("%5d",inputQueue.size()) + " pending]");
            logger.info("persist(): EventMap   " + mapOutput);

            /**
             * update avgWriteTimePerEvent and totalEventsWritten
             */
            this.totalEventsWritten += count;
            if ( count > 10000 ) {
                this.avgWriteTimePerEvent = avgTime;
            } else {
                this.avgWriteTimePerEvent = (this.avgWriteTimePerEvent * ((10000.0d-(double)count)/10000.0d)) + (avgTime * ((double)count)/10000.0d);
            }
        }
    }

    /**
     * Return the number of events that are pending in the queue.
     *
     * @return
     *  Number of events in queue.
     */
    protected int getEventsPendingCount()
    {
        return inputQueue.size();
    }

    /**
     * Start the writer.
     *
     * @param app
     *  Reports application.
     */
    protected void start( ReportsApp app )
    {
        this.app = app;
        UvmContextFactory.context().newThread(this).start();
    }

    /**
     * Stop the writer.
     */
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

    /**
     * Event type map.
     */
    class EventTypeMap
    {
        public String name;
        public int count;
        
        /**
         * Initialize EventTypeMap.
         *
         * @param name
         *  Name
         * @param count
         *  Count
         */
        public EventTypeMap(String name, int count)
        {
            this.name = name;
            this.count = count;
        }
    }
    
    /**
     * Event type comparator.
     */
    class EventTypeMapComparator implements Comparator<EventTypeMap>
    {
        /**
         * Compare two event maps based on counts.
         *
         * @param a
         *  First EventTypeMap to compare.
         * @param b
         *  Second EventTypeMap to compare.
         * @return
         *  Results of compare.
         */
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
