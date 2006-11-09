/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.ListEventFilter;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.logging.SyslogManager;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

class EventLoggerImpl<E extends LogEvent> extends EventLogger<E>
{
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int DEFAULT_SYNC_TIME = 120000; /* 2 minutes */
    private static final int SYNC_TIME;

    private static final List<String> INIT_QUEUE = new LinkedList<String>();

    private static final Map<Tid, Worker> WORKERS = new HashMap<Tid, Worker>();
    private static final Object LOG_LOCK = new Object();

    private static volatile boolean conversionComplete = true;

    private final List<EventCache<E>> caches = new LinkedList<EventCache<E>>();
    private final TransformContext transformContext;

    private final Logger logger = Logger.getLogger(getClass());

    private volatile int limit = 100;
    private BlockingQueue<EventDesc> inputQueue;

    // constructors -----------------------------------------------------------

    EventLoggerImpl()
    {
        this.transformContext = null;
    }

    EventLoggerImpl(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // static methods ---------------------------------------------------------

    static void initSchema(final String name)
    {
        synchronized (INIT_QUEUE) {
            conversionComplete = false;

            INIT_QUEUE.add(name);
        }

        // XXX not using newThread, called from MvvmContextImpl constructor
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

                            conversionComplete = true;
                            INIT_QUEUE.notifyAll();
                        }
                }
            }).start();
    }

    static boolean isConversionComplete()
    {
        return conversionComplete;
    }

    // EventManager methods ---------------------------------------------------

    public List<RepositoryDesc> getRepositoryDescs()
    {
        List<RepositoryDesc> l = new ArrayList<RepositoryDesc>(caches.size());
        for (EventCache<E> ec : caches) {
            l.add(ec.getRepositoryDesc());
        }

        return l;
    }

    public EventRepository<E> getRepository(String repositoryName)
    {
        for (EventCache<E> ec : caches) {
            if (ec.getRepositoryDesc().getName().equals(repositoryName)) {
                return ec;
            }
        }

        return null;
    }

    public List<EventRepository<E>> getRepositories()
    {
        return new LinkedList<EventRepository<E>>(caches);
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

    public EventRepository<E> addSimpleEventFilter(SimpleEventFilter<E> simpleFilter)
    {
        ListEventFilter lef = new SimpleEventFilterAdaptor(simpleFilter);
        EventCache<E> ec = new SimpleEventCache<E>(lef);
        ec.setEventLogger(this);
        caches.add(ec);
        return ec;
    }

    public EventRepository<E> addListEventFilter(ListEventFilter<E> listFilter)
    {
        EventCache<E> ec = new SimpleEventCache<E>(listFilter);
        ec.setEventLogger(this);
        caches.add(ec);
        return ec;
    }

    public EventRepository<E> addEventRepository(EventRepository<E> er) {
        EventCache<E> ec = new EventRepositoryCache(er);
        caches.add(ec);
        return ec;
    }

    public void log(E e)
    {
        if (null == inputQueue || !inputQueue.offer(new EventDesc(this, e))) {
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
        private boolean interruptable;

        // constructors -------------------------------------------------------

        Worker(TransformContext transformContext)
        {
            this.transformContext = transformContext;
            if (null == transformContext) {
                this.tag = "mvvm[0]: ";
            } else {
                String name = transformContext.getTransformDesc().getSyslogName();
                this.tag = name + "[" + transformContext.getTid().getId() + "]: ";
            }
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
                    EventDesc ed;

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
                    if (EventLoggerImpl.isConversionComplete()) {
                        try {
                            persist();
                        } catch (Exception exn) { // never say die
                            logger.error("something bad happened", exn);
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

        private boolean accept(EventDesc ed)
        {
            if (null == ed) { return false; }

            LogEvent e = ed.getLogEvent();

            if (e.isPersistent()) {
                logQueue.add(e);

                try {
                    syslogManager.sendSyslog(e, tag);
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

            synchronized (LOG_LOCK) {
                if (null == transformContext) {
                    boolean s = MvvmContextFactory.context().runTransaction(tw);
                    if (!s) {
                        logger.error("could not log events for MVVM");
                    }
                } else {
                    boolean s = transformContext.runTransaction(tw);
                    if (!s) {
                        logger.error("could not log events for: "
                                    + transformContext.getTid()
                                    + "("
                                    + transformContext.getMackageDesc().getName()
                                    + ")");
                    }
                }
            }
        }

        // package protected methods ------------------------------------------

        void start()
        {
            if (0 == clientCount) {
                MvvmContextFactory.context().newThread(this).start();
            }

            clientCount++;
        }

        void stop()
        {
            clientCount--;

            if (0 == clientCount) {
                Thread t = thread;
                thread = null;
                synchronized (this) {
                    if (interruptable) {
                        t.interrupt();
                    }
                }
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
        private final EventLoggerImpl eventLogger;
        private final LogEvent logEvent;

        EventDesc(EventLoggerImpl eventLogger, LogEvent logEvent)
        {
            this.eventLogger = eventLogger;
            this.logEvent = logEvent;
        }

        EventLoggerImpl getEventLogger()
        {
            return eventLogger;
        }

        LogEvent getLogEvent()
        {
            return logEvent;
        }
    }

    private static class EventRepositoryCache<E extends LogEvent>
        extends EventCache<E>
    {
        private final EventRepository eventRepository;

        EventRepositoryCache(EventRepository eventRepository)
        {
            this.eventRepository = eventRepository;
        }


        // EventRepository methods --------------------------------------------
        public RepositoryDesc getRepositoryDesc() {
            return eventRepository.getRepositoryDesc();
        }

        public List<E> getEvents()
        {
            return eventRepository.getEvents();
        }

        // EventCache methods ------------------------------------------------
        public void log(E e) { }
        public void checkCold() { }
        public void setEventLogger(EventLoggerImpl<E> el) { }
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
