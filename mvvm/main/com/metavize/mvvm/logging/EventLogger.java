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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

public class EventLogger<E extends LogEvent> implements EventManager<E>
{
    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = QUEUE_SIZE;
    private static final int SYNC_TIME = 300000;

    private static final Object LOG_LOCK = new Object();

    private final List<EventCache<E>> caches = new LinkedList<EventCache<E>>();
    private final TransformContext transformContext;
    private final String tag;

    private final BlockingQueue<E> inputQueue
        = new LinkedBlockingQueue<E>(QUEUE_SIZE);
    private final Worker worker = new Worker();
    private final SyslogManager syslogManager = MvvmContextFactory.context()
        .syslogManager();

    private final Logger logger = Logger.getLogger(getClass());

    private volatile int limit = 100;

    // constructors -----------------------------------------------------------

    public EventLogger()
    {
        this.transformContext = null;
        this.tag = "mvvm[0]: ";
    }

    public EventLogger(TransformContext transformContext)
    {
        this.transformContext = transformContext;
        this.tag = transformContext.getTransformDesc().getName()
            + "[" + transformContext.getTid().getId() + "]: ";
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
        if (!inputQueue.offer(e)) {
            logger.warn("dropping logevent: " + e);
        }
    }

    public void start()
    {
        new Thread(worker).start();
    }

    public void stop()
    {
        worker.stop();
    }

    // package protected methods ----------------------------------------------

    TransformContext getTransformContext()
    {
        return transformContext;
    }

    // private classes --------------------------------------------------------

    private class Worker implements Runnable
    {
        private volatile Thread thread;

        List<E> logQueue = new LinkedList<E>();

        // Runnable methods ---------------------------------------------------

        public void run()
        {
            thread = Thread.currentThread();

            long lastSync = System.currentTimeMillis();
            long nextSync = lastSync + SYNC_TIME;

            while (null != thread) {
                long t = System.currentTimeMillis();

                if (t < nextSync) {
                    E e;
                    try {
                        e = inputQueue.poll(nextSync - t, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException exn) {
                        continue;
                    }

                    if (null == e) {
                        continue;
                    }

                    logQueue.add(e);

                    try {
                        syslogManager.sendSyslog(e, tag);
                    } catch (Exception exn) { // never say die
                        logger.warn("failed to send syslog", exn);
                    }

                    for (EventCache<E> ec : caches) {
                        ec.log(e);
                    }
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
                        for (Iterator<E> i = logQueue.iterator();
                             i.hasNext(); ) {
                            E e = i.next();
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

        // public methods -----------------------------------------------------

        public void stop()
        {
            Thread t = thread;
            thread = null;
            t.interrupt();
        }
    }
}
