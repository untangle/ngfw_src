/**
 * $Id: LogWorker.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.node.reporting;

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogWorker;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.util.TransactionWork;

/**
 * Worker that batches and flushes events to the database.
 */
public class LogWorkerImpl implements Runnable, LogWorker
{
    private static final int MAX_LOAD = 2;

    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int LONG_SYNC_TIME = (int)Integer.getInteger("uvm.events.long_sync");
    private static final int SHORT_SYNC_TIME = (int)Integer.getInteger("uvm.events.short_sync");
    private static int RUNTIME_SYNC_TIME;
    private static boolean forceFlush = false;
    private static boolean running = false;
    
    static { // initialize RUNTIME_SYNC_TIME
        String p = System.getProperty("uvm.logging.synctime");
        int i = -1;
        if (null != p) {
            try {
                i = Integer.parseInt(p);
            } catch (NumberFormatException exn) {
                Logger.getLogger(LogWorker.class).warn("ignoring invalid sync time: " + p);
            }
        }
        RUNTIME_SYNC_TIME = i < 0 ? 0 : i;
    }

    /**
     * The queue of events waiting to be written to the database
     */
    private final List<LogEvent> logQueue = new LinkedList<LogEvent>();

    /**
     * This is a queue of incoming events
     */
    private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

    private final SyslogManager syslogManager = UvmContextFactory.context().syslogManager();

    private final Logger logger = Logger.getLogger(getClass());

    private final ReportingNode node;
    
    private volatile Thread thread;
    private boolean interruptable;

    private Double lastLoad;
    private long lastLoadGet;
    private long syncTime;

    // constructors -------------------------------------------------------

    public LogWorkerImpl(ReportingNode node)
    {
        this.lastLoadGet = 0;
        this.syncTime = 0;
        this.node = node;
    }

    // Runnable methods ---------------------------------------------------

    public void run()
    {
        thread = Thread.currentThread();

        long lastSync = System.currentTimeMillis();
        long nextSync = lastSync + getSyncTime();

        while (thread != null) {
            long t = System.currentTimeMillis();

            if (t < nextSync) {
                LogEvent event = null;

                synchronized (this) {
                    interruptable = true;
                }

                try {
                    /**
                     * If there are events waiting to be written only wait a certain amount of time
                     * Otherwise wait indefinitely because no reason to wake up
                     */
                    if (logQueue.size() > 0) {
                        event = inputQueue.poll(nextSync - t, TimeUnit.MILLISECONDS);
                    } else {
                        event = inputQueue.take();
                    }
                } catch (InterruptedException exn) {}

                synchronized (this) {
                    interruptable = false;
                }

                if (event != null)
                    accept(event);
            }

            if (logQueue.size() >= BATCH_SIZE || t >= nextSync || forceFlush) {
                if (UvmContextFactory.context().loggingManager().isConversionComplete()) {
                    try {
                        persist();
                    } catch (Exception exn) { // never say die
                        logger.error("Hibernate error, see nested exception below", exn);
                    }
                }

                lastSync = System.currentTimeMillis();
                nextSync = lastSync + getSyncTime();
                forceFlush = false;
            }
        }

        while (accept(inputQueue.poll()));

        if (0 < logQueue.size()) {
            persist();
        }
    }

    /**
     * Force currently queued events to the DB
     */
    public void forceFlush()
    {
        forceFlush = true;
        logger.info("forceFlush()");
        thread.interrupt();
    }
    
    public void logEvent(LogEvent evt)
    {
        if (!running) {
            logger.warn("Reports not running, discarding event");
        }
        
        String tag = "uvm[0]: ";
        evt.setTag(tag);
        
        if (!inputQueue.offer(evt)) {
            logger.warn("dropping logevent: " + evt);
        }
    }

    /*
     * Only calculate the load every LONG_SYNC_TIME
     */
    private Double getLoad()
    {
        if ((lastLoadGet == 0)
            || ((System.currentTimeMillis() - lastLoadGet) > LONG_SYNC_TIME)) {
            Double load =  Double.parseDouble("0");

            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader("/proc/loadavg"));
                String l = br.readLine();
                if (null != l)
                    load = Double.parseDouble(l.split(" ")[0]);
            } catch (Exception e) {
                logger.warn("could not get loadavg", e);
            }
            lastLoadGet = System.currentTimeMillis();
            lastLoad = load;
            return load;
        } else {
            return lastLoad;
        }
    }

    private long getSyncTime()
    {
        if (RUNTIME_SYNC_TIME != 0)
            return RUNTIME_SYNC_TIME;

        long oldSyncTime = syncTime;

        Double load = getLoad();
        if (load > MAX_LOAD) {
            syncTime = LONG_SYNC_TIME;
            if (syncTime != oldSyncTime)
                logger.info("Current load (" + load + ") is higher than " +
                            MAX_LOAD + ", logging events only every " +
                            syncTime/1000 + " seconds");
        } else {
            syncTime = SHORT_SYNC_TIME;
            if (syncTime != oldSyncTime)
                logger.info("Current load (" + load + ") is lower than " +
                            MAX_LOAD + ", now logging events every " +
                            syncTime/1000 + " seconds");
        }
        
        return syncTime;
    }

    private boolean accept(LogEvent event)
    {
        if (null == event) { return false; }

        if (event.isPersistent()) {
            /**
             * Add it to the queue to be written to database
             */
            logQueue.add(event);

            /**
             * Send it to syslog (make best attempt - ignore errors)
             */
            try {
                syslogManager.sendSyslog(event, event.getTag());
            } catch (Exception exn) { 
                logger.warn("failed to send syslog", exn);
            }
        }

        return true;
    }

    /**
     * write the logQueue to the database
     */
    private void persist()
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    logger.debug("Writing events to database... (size: " + logQueue.size() + ")");

                    for (Iterator<LogEvent> i = logQueue.iterator(); i.hasNext(); ) {
                        LogEvent event = i.next();

                        /**
                         * Write event to database
                         * If fails, just move on
                         */
                        try {
                            s.saveOrUpdate(event);
                        } catch (Exception exc) {
                            logger.error("could not log event: ", exc);
                        }

                        i.remove();
                    }

                    logger.debug("Writing events to database... Complete");

                    return true;
                }
            };

        int count = logQueue.size();
        long t0 = System.currentTimeMillis();

        boolean s = UvmContextFactory.context().runTransaction(tw);

        long t1 = System.currentTimeMillis();

        logger.info("persist(): wrote " + count + " events to DB (" + (t1-t0) + " msec)");
        
        if (!s) {
            logger.error("could not log events");
        }
    }

    // package protected methods ------------------------------------------

    protected void start()
    {
        this.running = true;
        UvmContextFactory.context().newThread(this).start();
    }

    protected void stop()
    {
        this.running = false;
        Thread t = thread;
        thread = null;
        synchronized (this) {
            if (interruptable && null != t) {
                t.interrupt();
            }
        }
    }
}
