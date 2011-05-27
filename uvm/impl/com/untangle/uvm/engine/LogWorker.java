/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Freeoftware Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.util.TransactionWork;

/**
 * Worker that batches and flushes events to the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class LogWorker implements Runnable
{
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int DEFAULT_SYNC_TIME = 120000; /* 2 minutes */
    private static final int SYNC_TIME;

    private final RemoteLoggingManagerImpl loggingManager;

    /**
     * The queue of events waiting to be written to the database
     */
    private final List<LogEvent> logQueue = new LinkedList<LogEvent>();

    /**
     * This is a queue of incoming events
     */
    private final BlockingQueue<LogEventDesc> inputQueue = new LinkedBlockingQueue<LogEventDesc>();

    private final SyslogManager syslogManager = LocalUvmContextFactory.context().syslogManager();

    private final Logger logger = Logger.getLogger(getClass());

    private volatile Thread thread;
    private boolean interruptable;

    // constructors -------------------------------------------------------

    LogWorker(RemoteLoggingManagerImpl loggingManager)
    {
        this.loggingManager = loggingManager;
    }

    // Runnable methods ---------------------------------------------------

    public void run()
    {
        thread = Thread.currentThread();

        long lastSync = System.currentTimeMillis();
        long nextSync = lastSync + SYNC_TIME;

        while (thread != null) {
            long t = System.currentTimeMillis();

            if (t < nextSync) {
                LogEventDesc event;

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
                } catch (InterruptedException exn) {
                    continue;
                }

                synchronized (this) {
                    interruptable = false;
                }

                accept(event);
            }

            if (logQueue.size() >= BATCH_SIZE || t >= nextSync) {
                if (loggingManager.isConversionComplete()) {
                    try {
                        persist();
                    } catch (Exception exn) { // never say die
                        logger.error("Hibernate error, see nested exception below", exn);
                    }
                }

                lastSync = System.currentTimeMillis();
                nextSync = lastSync + SYNC_TIME;
            }
        }

        while (accept(inputQueue.poll()));

        if (0 < logQueue.size()) {
            persist();
        }
    }

    private boolean accept(LogEventDesc event)
    {
        if (null == event) { return false; }

        LogEvent e = event.getLogEvent();

        if (e.isPersistent()) {
            /**
             * Add it to the queue to be written to database
             */
            logQueue.add(e);

            /**
             * Send it to syslog (make best attempt - ignore errors)
             */
            try {
                syslogManager.sendSyslog(e, event.getTag());
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

        boolean s = LocalUvmContextFactory.context().runTransaction(tw);
        if (!s) {
            logger.error("could not log events");
        }
    }

    // package protected methods ------------------------------------------

    void start()
    {
        if (!RemoteLoggingManagerImpl.isLoggingDisabled()) {
            LocalUvmContextFactory.context().newThread(this).start();
        }
    }

    void stop()
    {
        Thread t = thread;
        thread = null;
        synchronized (this) {
            if (interruptable && null != t) {
                t.interrupt();
            }
        }
    }

    BlockingQueue<LogEventDesc> getInputQueue()
    {
        return inputQueue;
    }

    static {
        String p = System.getProperty("uvm.logging.synctime");
        if (null == p) {
            SYNC_TIME = DEFAULT_SYNC_TIME;
        } else {
            int i = -1;
            try {
                i = Integer.parseInt(p);
            } catch (NumberFormatException exn) {
                Logger.getLogger(LogWorker.class).warn("ignoring invalid sync time: " + p);
            }

            SYNC_TIME = i < 0 ? DEFAULT_SYNC_TIME : i;
        }
    }
}
