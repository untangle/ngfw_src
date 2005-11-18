/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.logging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.engine.SchemaUtil;
import com.metavize.mvvm.security.Tid;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class EventLogger<E extends LogEvent> implements EventManager<E>
{
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int DEFAULT_SYNC_TIME = 600000; /* 10 minutes */
    private static final int SYNC_TIME;

    private static final List<String> INIT_QUEUE = new LinkedList<String>();

    private static final Map<Tid, Worker> WORKERS = new HashMap<Tid, Worker>();
    private static final Object LOG_LOCK = new Object();

    private final List<EventCache<E>> caches = new LinkedList<EventCache<E>>();
    private final TransformContext transformContext;

    private final Logger logger = Logger.getLogger(getClass());

    private volatile int limit = 100;
    private BlockingQueue<EventDesc> inputQueue;

    // constructors -----------------------------------------------------------

    public EventLogger()
    {
        this.transformContext = null;
    }

    public EventLogger(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // static methods ----------------------------------------------------------

    public static void initSchema(final String name)
    {
        synchronized (INIT_QUEUE) {
            INIT_QUEUE.add(name);
        }

        new Thread(new Runnable()
            {
                public void run()
                {
                        MvvmContextFactory.context().waitForStartup();

                        synchronized (INIT_QUEUE) {
                            for (Iterator<String> i = INIT_QUEUE.iterator(); i.hasNext(); ) {
                                String n = i.next();
                                i.remove();
                                SchemaUtil.initSchema("events", n);
                            }

                            INIT_QUEUE.notifyAll();
                        }
                }
            }).start();
    }

    // EventManager methods ---------------------------------------------------

    public List<FilterDesc> getFilterDescs()
    {
        List<FilterDesc> l = new ArrayList<FilterDesc>(caches.size());
        for (EventCache<E> ec : caches) {
            l.add(ec.getFilterDesc());
        }

        return l;
    }

    public EventFilter<E> getFilter(String filterName)
    {
        for (EventCache<E> ec : caches) {
            if (ec.getFilterDesc().getName().equals(filterName)) {
                return ec;
            }
        }

        return null;
    }

    public List<EventFilter<E>> getFilters()
    {
        return new LinkedList<EventFilter<E>>(caches);
    }

    public void setLimit(int limit)
    {
        boolean checkCold = limit > this.limit;

        this.limit = limit;

        if (checkCold) {
            for (EventCache<E> c : caches) {
                c.checkCold();
            }
        }
    }

    public int getLimit()
    {
        return limit;
    }

    // public methods --------------------------------------------------------

    public EventCache addEventHandler(EventHandler<E> eventHandler)
    {
        EventCache<E> ec = new EventCache<E>(this, eventHandler);
        caches.add(ec);

        return ec;
    }

    public void log(E e)
    {
        if (!inputQueue.offer(new EventDesc(this, e))) {
            logger.warn("dropping logevent: " + e);
        }
    }

    public void start()
    {
        Tid tid = null == transformContext ? null : transformContext.getTid();
        synchronized (WORKERS) {
            Worker w = WORKERS.get(tid);
            if (null == w) {
                w = new Worker(transformContext);
                WORKERS.put(tid, w);
                w.start();
            }

            this.inputQueue = w.getInputQueue();
        }
    }

    public void stop()
    {
        Tid tid = null == transformContext ? null : transformContext.getTid();
        synchronized (WORKERS) {
            inputQueue = null;
            Worker w = WORKERS.get(tid);
            if (null != w) {
                w.stop();
                if (!w.isLive()) {
                    WORKERS.remove(tid);
                }
            }
        }
    }

    // package protected methods ----------------------------------------------

    TransformContext getTransformContext()
    {
        return transformContext;
    }

    private void doLog(LogEvent e)
    {
        for (EventCache<E> ec : caches) {
            ec.log((E)e);
        }
    }

    // private classes --------------------------------------------------------

    private static class Worker implements Runnable
    {
        private final List<LogEvent> logQueue = new LinkedList<LogEvent>();
        private final TransformContext transformContext;
        private final BlockingQueue<EventDesc> inputQueue
            = new LinkedBlockingQueue<EventDesc>();
        private final String tag;

        private final SyslogManager syslogManager = MvvmContextFactory
            .context().syslogManager();

        private final Logger logger = Logger.getLogger(getClass());

        private int clientCount = 0;

        private volatile Thread thread;

        // constructors -------------------------------------------------------

        Worker(TransformContext transformContext)
        {
            this.transformContext = transformContext;
            if (null == transformContext) {
                this.tag = "mvvm[0]: ";
            } else {
                String name = transformContext.getTransformDesc().getName();
                this.tag = name + "[" + transformContext.getTid().getId() + "]: ";
            }
        }

        // Runnable methods ---------------------------------------------------

        public void run()
        {
            thread = Thread.currentThread();

            synchronized (INIT_QUEUE) {
                while (0 < INIT_QUEUE.size() && null != thread) {
                    try {
                        INIT_QUEUE.wait();
                    } catch (InterruptedException exn) {
                        // reevaluate loop condition
                    }
                }
            }

            long lastSync = System.currentTimeMillis();
            long nextSync = lastSync + SYNC_TIME;

            while (null != thread) {
                long t = System.currentTimeMillis();

                if (t < nextSync) {
                    EventDesc ed;
                    try {
                        if (0 < logQueue.size()) {
                            ed = inputQueue.poll(nextSync - t, TimeUnit.MILLISECONDS);
                        } else {
                            ed = inputQueue.take();
                        }
                    } catch (InterruptedException exn) {
                        continue;
                    }

                    if (null == ed) {
                        continue;
                    }

                    LogEvent e = ed.getLogEvent();

                    logQueue.add(e);

                    try {
                        syslogManager.sendSyslog(e, tag);
                    } catch (Exception exn) { // never say die
                        logger.warn("failed to send syslog", exn);
                    }

                    ed.getEventLogger().doLog(e);
                }

                if (logQueue.size() >= BATCH_SIZE || t >= nextSync) {
                    try {
                        persist();
                    } catch (Exception exn) { // never say die
                        logger.error("something bad happened", exn);
                    }

                    lastSync = System.currentTimeMillis();
                    nextSync = lastSync + SYNC_TIME;
                }
            }

            if (0 < logQueue.size()) {
                persist();
            }
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
                            s.save(e);
                            i.remove();
                        }

                        return true;
                    }
                };

            synchronized (LOG_LOCK) {
                if (null == transformContext) {
                    MvvmContextFactory.context().runTransaction(tw);
                } else {
                    transformContext.runTransaction(tw);
                }
            }
        }

        // package protected methods ------------------------------------------

        void start()
        {
            if (0 == clientCount) {
                new Thread(this).start();
            }

            clientCount++;
        }

        void stop()
        {
            clientCount--;

            if (0 == clientCount) {
                Thread t = thread;
                thread = null;
                t.interrupt();
            }
        }

        public boolean isLive()
        {
            return null != thread;
        }

        public BlockingQueue<EventDesc> getInputQueue()
        {
            return inputQueue;
        }
    }

    private static class EventDesc
    {
        private final EventLogger eventLogger;
        private final LogEvent logEvent;

        EventDesc(EventLogger eventLogger, LogEvent logEvent)
        {
            this.eventLogger = eventLogger;
            this.logEvent = logEvent;
        }

        EventLogger getEventLogger()
        {
            return eventLogger;
        }

        LogEvent getLogEvent()
        {
            return logEvent;
        }
    }

    // static initialization --------------------------------------------------

    static {
        String p = System.getProperty("mvvm.logging.synctime");
        if (null == p) {
            SYNC_TIME = DEFAULT_SYNC_TIME;
        } else {
            int i = -1;
            try {
                i = Integer.parseInt(p);
            } catch (NumberFormatException exn) {
                Logger l = Logger.getLogger(EventLogger.class);
                l.warn("ignoring invalid sync time: " + p);
            }

            SYNC_TIME = 0 > i ? DEFAULT_SYNC_TIME : i;
        }
    }
}
