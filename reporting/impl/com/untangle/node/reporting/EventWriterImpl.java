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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogManager;

/**
 * Worker that batches and flushes events to the database.
 */
public class EventWriterImpl implements Runnable
{
    private static final int SYNC_TIME = 60*1000; /* 60 seconds */

    private final Logger logger = Logger.getLogger(getClass());

    private static boolean forceFlush = false;

    private volatile Thread thread;

    private Connection dbConnection;

    private long lastLoggedWarningTime = System.currentTimeMillis();

    /**
     * This is a queue of incoming events
     */
    private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

    public EventWriterImpl( )
    {
        this.dbConnection = null;
    }

    public void run()
    {
        thread = Thread.currentThread();

        List<LogEvent> logQueue = new LinkedList<LogEvent>();
        LogEvent event = null;

        /**
         * Loop indefinitely and continue logging events
         */
        while (thread != null) {
            /**
             * Sleep until next log time
             */
            if (!forceFlush)
                try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}

            if ( dbConnection == null ) {
                try {
                    dbConnection = ReportingNodeImpl.getDBConnection();
                } catch (Exception e) {
                    logger.warn("Unable to create connection to DB",e);
                }
            }
            if ( dbConnection == null) {
                logger.warn("Unable to get connection to DB, dropping events...");
                while ((event = inputQueue.poll()) != null) {}
                continue;
            }
            
            synchronized( this ) {
                /**
                 * Copy all events out of the queue
                 */
                while ((event = inputQueue.poll()) != null) {
                    logQueue.add(event);
                }
                
                /**
                 * If there is anything to log, log it to the database
                 */
                if (logQueue.size() > 0)
                    persist(logQueue);
                else
                    logger.info("persist(): 0 events");
                
                /**
                 * If the forceFlush flag was set, reset it and wake any interested parties
                 */
                if (forceFlush) {
                    forceFlush = false; //reset global flag
                    notifyAll(); /* notify any waiting threads that the flush is done */
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
            UvmContextFactory.context().syslogManager().sendSyslog(event, event.getTag());
        } catch (Exception exn) { 
            logger.warn("failed to send syslog", exn);
        }
    }

    /**
     * write the logQueue to the database
     */
    private void persist( List<LogEvent> logQueue )
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

        int count = logQueue.size();
        long t0 = System.currentTimeMillis();

        Statement statement = null;
        try {
            statement = this.dbConnection.createStatement();
        } catch (Exception e) {
            logger.warn("Unable to create statement.",e);
            this.dbConnection = null;
            return;
        }
        if ( statement == null ) {
            logger.warn("Unable to create statement: null");
            this.dbConnection = null;
            return;
        }
        
        logger.debug("Writing events to database... (size: " + logQueue.size() + ")");
        for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
            LogEvent event = i.next();

            /**
             * Write event to database using SQL
             * If fails, just move on
             */
            List<String> sqls = event.getDirectEventSqls();
            if (sqls != null) {
                long write_t0 = System.currentTimeMillis();
                for (String sqlStr : sqls) {
                    logger.debug("Write direct event: " + sqlStr);
                    try {
                        statement.execute(sqlStr);
                    } catch (SQLException e) {
                        logger.warn("Failed SQL query: \"" + sqlStr + "\": " + e.getMessage());
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
            logger.info("persist(): " + String.format("%5d",count) + " events [" + String.format("%5d",(t1-t0)) + " ms]" + mapOutput);
        }
    }

    protected void start()
    {
        UvmContextFactory.context().newThread(this).start();
    }

    protected void stop()
    {
        forceFlush(); /* flush last few events */

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
