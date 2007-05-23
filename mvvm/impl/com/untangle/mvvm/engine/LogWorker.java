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

package com.untangle.mvvm.engine;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.SyslogManager;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

class LogWorker implements Runnable
{
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int DEFAULT_SYNC_TIME = 120000; /* 2 minutes */
    private static final int SYNC_TIME;

    private final LoggingManagerImpl loggingManager;
    private final List<LogEvent> logQueue = new LinkedList<LogEvent>();
    private final BlockingQueue<LogEventDesc> inputQueue
        = new LinkedBlockingQueue<LogEventDesc>();

    private final SyslogManager syslogManager = MvvmContextFactory
        .context().syslogManager();

    private final Logger logger = Logger.getLogger(getClass());

    private volatile Thread thread;
    private boolean interruptable;

    // constructors -------------------------------------------------------

    LogWorker(LoggingManagerImpl loggingManager)
    {
        this.loggingManager = loggingManager;
    }

    // Runnable methods ---------------------------------------------------

    public void run()
    {
        thread = Thread.currentThread();

        long lastSync = System.currentTimeMillis();
        long nextSync = lastSync + SYNC_TIME;

        while (null != thread) {
            long t = System.currentTimeMillis();

            if (t < nextSync) {
                LogEventDesc ed;

                synchronized (this) {
                    interruptable = true;
                }

                try {
                    if (0 < logQueue.size()) {
                        ed = inputQueue.poll(nextSync - t, TimeUnit.MILLISECONDS);
                    } else {
                        ed = inputQueue.take();
                    }
                } catch (InterruptedException exn) {
                    continue;
                }

                synchronized (this) {
                    interruptable = false;
                }

                accept(ed);
            }

            if (logQueue.size() >= BATCH_SIZE || t >= nextSync) {
                if (loggingManager.isConversionComplete()) {
                    try {
                        persist();
                    } catch (Exception exn) { // never say die
                        logger.error("fire on the poop deck", exn);
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

    private boolean accept(LogEventDesc ed)
    {
        if (null == ed) { return false; }

        LogEvent e = ed.getLogEvent();

        if (e.isPersistent()) {
            logQueue.add(e);

            try {
                syslogManager.sendSyslog(e, ed.getTag());
            } catch (Exception exn) { // never say die
                logger.warn("failed to send syslog", exn);
            }
        }

        ed.getEventLogger().doLog(e);

        return true;
    }

    private void persist()
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    for (Iterator<LogEvent> i = logQueue.iterator();
                         i.hasNext(); ) {
                        LogEvent e = i.next();
                        s.saveOrUpdate(e);
                        i.remove();
                    }

                    return true;
                }
            };

        boolean s = MvvmContextFactory.context().runTransaction(tw);
        if (!s) {
            logger.error("could not log events");
        }
    }

    // package protected methods ------------------------------------------

    void start()
    {
        if (!loggingManager.isLoggingDisabled()) {
            MvvmContextFactory.context().newThread(this).start();
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
        String p = System.getProperty("mvvm.logging.synctime");
        if (null == p) {
            SYNC_TIME = DEFAULT_SYNC_TIME;
        } else {
            int i = -1;
            try {
                i = Integer.parseInt(p);
            } catch (NumberFormatException exn) {
                Logger.getLogger(LogWorker.class)
                    .warn("ignoring invalid sync time: " + p);
            }

            SYNC_TIME = 0 > i ? DEFAULT_SYNC_TIME : i;
        }
    }
}
